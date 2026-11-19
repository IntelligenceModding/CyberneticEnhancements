package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.QuestStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.HashSet;

public final class NpcIncomingCalls {
    private static final String TAG = "cyberneticenhancements.incoming_calls";
    public static final long COOLDOWN = 30 * 60 * 20L;
    public static final long OVERDUE_AFTER = 2 * 60 * 60 * 20L;

    private NpcIncomingCalls() {}

    public static long playTicks(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
    }

    public static boolean evaluate(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.isSleeping() || NpcCallManager.hasCall(player.getUUID())
                || player.containerMenu != player.inventoryMenu) return false;
        long now = playTicks(player);
        var state = state(player);
        var quests = PlayerQuestManager.activeContracts(player);
        var activeIds = new HashSet<String>();
        var reminders = state.getCompound("Reminders");
        for (var quest : quests) {
            activeIds.add(quest.id());
            if (quest.acceptedPlayTicks() == Long.MIN_VALUE) PlayerQuestManager.setAcceptedPlayTicks(player, quest.issuerId(), now);
        }
        for (String id : new HashSet<>(reminders.getAllKeys())) if (!activeIds.contains(id)) reminders.remove(id);
        state.put("Reminders", reminders);
        if (!state.contains("LastIncoming")) {
            state.putLong("LastIncoming", now);
            save(player, state);
            return false;
        }
        save(player, state);
        if (cooldownRemaining(player) > 0) return false;
        var contacts = AbstractCityNpcEntity.archiveContacts(player).stream().filter(contact -> !contact.hidden() && !contact.refusing()).toList();
        for (var quest : quests) {
            if (quest.status() != QuestStatus.ACTIVE || quest.acceptedPlayTicks() == Long.MIN_VALUE || now - quest.acceptedPlayTicks() < OVERDUE_AFTER
                    || (reminders.contains(quest.id()) && now - reminders.getLong(quest.id()) < OVERDUE_AFTER)
                    || contacts.stream().noneMatch(contact -> contact.npcId().equals(quest.issuerId()))) continue;
            var npc = AbstractCityNpcEntity.resolveCallContact(player, quest.issuerId());
            if (npc != null && NpcCallManager.startIncoming(player, npc, NpcCallReason.OVERDUE, false)) {
                record(player, npc, NpcCallReason.OVERDUE);
                return true;
            }
        }
        if (now >= COOLDOWN && player.getRandom().nextInt(8) == 0) {
            var requests = state.getCompound("UrgentRequests");
            var candidates = new java.util.ArrayList<>(contacts);
            java.util.Collections.shuffle(candidates, new java.util.Random(player.getRandom().nextLong()));
            for (var contact : candidates) {
                String id = contact.npcId().toString();
                if (contact.deceased() || contact.surchargeActive() || (requests.contains(id) && now - requests.getLong(id) < OVERDUE_AFTER)) continue;
                var npc = AbstractCityNpcEntity.resolveCallContact(player, contact.npcId());
                if (npc != null && NpcCallManager.startIncoming(player, npc, NpcCallReason.URGENT_QUEST, false)) {
                    record(player, npc, NpcCallReason.URGENT_QUEST);
                    return true;
                }
            }
        }
        if (now < 60 * 60 * 20L || PlayerEurodollarManager.balance(player) < 20 || player.getRandom().nextInt(6) != 0) return false;
        var candidates = contacts.stream().filter(contact -> contact.trustLevel() >= 2 && !contact.surchargeActive()).toList();
        if (candidates.isEmpty()) return false;
        var contact = candidates.get(player.getRandom().nextInt(candidates.size()));
        var requests = state.getCompound("MoneyRequests");
        String id = contact.npcId().toString();
        if (requests.contains(id) && now - requests.getLong(id) < OVERDUE_AFTER) return false;
        var npc = AbstractCityNpcEntity.resolveCallContact(player, contact.npcId());
        if (npc == null || !NpcCallManager.startIncoming(player, npc, NpcCallReason.MONEY, false)) return false;
        record(player, npc, NpcCallReason.MONEY);
        return true;
    }

    public static void record(ServerPlayer player, AbstractCityNpcEntity npc, NpcCallReason reason) {
        var state = state(player);
        long now = playTicks(player);
        state.putLong("LastIncoming", now);
        if (reason == NpcCallReason.OVERDUE) {
            var active = PlayerQuestManager.snapshotForIssuer(player, npc).active();
            if (active != null) {
                var reminders = state.getCompound("Reminders");
                reminders.putLong(active.id(), now);
                state.put("Reminders", reminders);
            }
        } else if (reason == NpcCallReason.URGENT_QUEST) {
            var requests = state.getCompound("UrgentRequests");
            requests.putLong(npc.getStringUUID(), now);
            state.put("UrgentRequests", requests);
        } else if (reason == NpcCallReason.MONEY) {
            var requests = state.getCompound("MoneyRequests");
            requests.putLong(npc.getStringUUID(), now);
            state.put("MoneyRequests", requests);
        }
        save(player, state);
    }

    public static long cooldownRemaining(ServerPlayer player) {
        var state = state(player);
        return state.contains("LastIncoming") ? Math.max(0, COOLDOWN - (playTicks(player) - state.getLong("LastIncoming"))) : COOLDOWN;
    }

    public static void resetCooldowns(ServerPlayer player) {
        var state = new CompoundTag();
        state.putLong("LastIncoming", playTicks(player) - COOLDOWN);
        save(player, state);
    }

    public static void copyState(net.minecraft.world.entity.player.Player original, net.minecraft.world.entity.player.Player clone) {
        if (original.getPersistentData().contains(TAG)) clone.getPersistentData().put(TAG, original.getPersistentData().getCompound(TAG).copy());
    }

    private static CompoundTag state(ServerPlayer player) { return player.getPersistentData().getCompound(TAG); }
    private static void save(ServerPlayer player, CompoundTag state) { player.getPersistentData().put(TAG, state); }
}
