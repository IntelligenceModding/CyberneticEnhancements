package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public final class SchematicKnowledge {
    public static final int EXTRACTION_COST = 25;
    private static final String KEY = "cyberneticenhancements.schematics";
    private static final String DETAILS = "cyberneticenhancements.schematic_history";
    private static volatile Map<String, Unlock> client = Map.of();
    private static volatile int revision;

    public record Unlock(String id, long epochMillis, long gameTime, String dimension, BlockPos position,
                         String source) {
        public static Unlock legacy(String id) {
            return new Unlock(id, 0, -1, "", BlockPos.ZERO, "Previously learned; discovery details unavailable");
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("date", epochMillis);
            tag.putLong("game_time", gameTime);
            tag.putString("dimension", dimension);
            tag.putInt("x", position.getX());
            tag.putInt("y", position.getY());
            tag.putInt("z", position.getZ());
            tag.putString("source", source);
            return tag;
        }

        public static Unlock load(String id, CompoundTag tag) {
            return new Unlock(id, tag.getLong("date"), tag.getLong("game_time"), tag.getString("dimension"),
                    new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")), tag.getString("source"));
        }
    }

    public static Set<String> known(Player player) {
        Set<String> result = new HashSet<>();
        ListTag list = player.getPersistentData().getList(KEY, Tag.TAG_STRING);
        for (int n = 0; n < list.size(); n++) result.add(list.getString(n));
        return Set.copyOf(result);
    }

    public static Unlock history(Player player, String id) {
        CompoundTag records = player.getPersistentData().getCompound(DETAILS);
        return records.contains(id, Tag.TAG_COMPOUND) ? Unlock.load(id, records.getCompound(id)) : Unlock.legacy(id);
    }

    public static boolean knows(Player player, String id) {
        return id.isEmpty() || known(player).contains(id);
    }

    public static boolean clientKnows(String id) {
        return id.isEmpty() || client.containsKey(id);
    }

    public static Unlock clientHistory(String id) {
        return client.get(id);
    }

    public static void receive(List<String> ids) {
        receiveDetails(ids.stream().map(Unlock::legacy).toList());
    }

    public static void receiveDetails(List<Unlock> records) {
        Map<String, Unlock> next = new HashMap<>();
        for (Unlock record : records) next.put(record.id(), record);
        client = Map.copyOf(next);
        revision++;
    }

    public static void clearClient() {
        client = Map.of();
        revision++;
    }

    public static int revision() {
        return revision;
    }

    public static boolean learn(ServerPlayer player, String id) {
        return learn(player, id, "Studied schematic", player.blockPosition());
    }

    public static boolean learn(ServerPlayer player, String id, String source, BlockPos location) {
        if (knows(player, id) || !FabricationContent.SCHEMATICS.containsKey(id)) return false;
        Set<String> ids = new HashSet<>(known(player));
        ids.add(id);
        saveKnown(player, ids);
        CompoundTag records = player.getPersistentData().getCompound(DETAILS);
        if (!records.contains(id, Tag.TAG_COMPOUND)) {
            Unlock unlock = new Unlock(id, System.currentTimeMillis(), player.level().getDayTime(),
                    player.level().dimension().location().toString(), location.immutable(), source);
            records.put(id, unlock.save());
            player.getPersistentData().put(DETAILS, records);
            player.giveExperiencePoints(de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.SCHEMATIC);
            de.artemis.cyberneticenhancements.common.streetcred.StreetCred.gain(player, de.artemis.cyberneticenhancements.common.streetcred.StreetCredRewards.SCHEMATIC);
            de.artemis.cyberneticenhancements.common.journal.PlayerJournal.recordAt(player, location, "workshop", "schematic", FabricationContent.schematicName(id));
        }
        sync(player);
        player.displayClientMessage(Component.literal("Schematic learned: ").append(FabricationContent.schematicName(id)), true);
        player.playNotifySound(net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.2F);
        player.serverLevel().sendParticles(player, net.minecraft.core.particles.ParticleTypes.ENCHANT, false,
                player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.35, 0.3, 0.35, 0.1);
        return true;
    }

    public static void extract(ServerPlayer player, String id) {
        var item = FabricationContent.SCHEMATICS.get(id);
        var ids = new HashSet<>(known(player));
        if (item == null || !ids.remove(id) || !player.isAlive() || player.isSpectator()) return;
        int slot = player.getInventory().getFreeSlot();
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.schematic_extract.inventory_full"), true);
            PlayerActionFeedback.DENIED.play(player);
            return;
        }
        if (!PlayerEurodollarManager.tryWithdraw(player, EXTRACTION_COST, "expense", "Schematic extraction", null, "")) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.schematic_extract.insufficient", EXTRACTION_COST), true);
            PlayerActionFeedback.DENIED.play(player);
            return;
        }

        // A legacy unlock has no discovery record, but still counts as previously learned.
        CompoundTag records = player.getPersistentData().getCompound(DETAILS);
        if (!records.contains(id, Tag.TAG_COMPOUND)) {
            records.put(id, Unlock.legacy(id).save());
            player.getPersistentData().put(DETAILS, records);
        }
        saveKnown(player, ids);
        player.getInventory().setItem(slot, new ItemStack(item.get()));
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        sync(player);
        player.displayClientMessage(Component.translatable("message.cyberneticenhancements.schematic_extract.success",
                FabricationContent.schematicName(id), EXTRACTION_COST), true);
        PlayerActionFeedback.SERVICE.play(player);
    }

    private static void saveKnown(ServerPlayer player, Set<String> ids) {
        ListTag list = new ListTag();
        ids.stream().sorted().forEach(id -> list.add(StringTag.valueOf(id)));
        player.getPersistentData().put(KEY, list);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SchematicKnowledgePayload(known(player).stream().sorted().map(id -> history(player, id)).toList()));
    }

    public static void onSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) sync(event.getPlayer());
        else event.getPlayerList().getPlayers().forEach(SchematicKnowledge::sync);
    }

    public static void onClone(PlayerEvent.Clone event) {
        for (String key : List.of(KEY, DETAILS))
            if (event.getOriginal().getPersistentData().contains(key))
                event.getEntity().getPersistentData().put(key, event.getOriginal().getPersistentData().get(key).copy());
    }
}
