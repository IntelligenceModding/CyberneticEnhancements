package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload;
import de.artemis.cyberneticenhancements.common.network.NpcCallStatePayload;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import de.artemis.cyberneticenhancements.common.quest.QuestStatus;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static de.artemis.cyberneticenhancements.common.dialogue.NpcCallGameTests.*;
import static de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NpcIncomingCallGameTests {
    @GameTest(template = "sandevistan_test", batch = "failed_cache_angry_call", timeoutTicks = 100)
    public static void failedQuestCacheQueuesAnAngryCallUntilThePlayerIsFreeAndOnlyChargesTrustOnce(GameTestHelper h) {
        var owner = player(h, "cache-failure-call");
        var clone = player(h, "cache-failure-clone");
        var player = owner.player();
        var issuer = ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        var other = ModEntityTypes.MERC.get().create(h.getLevel());
        issuer.setPos(player.position().add(0, 0, 4));
        other.setPos(player.position().add(4, 0, 0));
        issuer.setNoAi(true); other.setNoAi(true);
        h.getLevel().addFreshEntity(issuer); h.getLevel().addFreshEntity(other);
        issuer.ensureIdentity(); other.ensureIdentity();
        issuer.markMet(player); other.markMet(player);
        issuer.raiseTrust(player, 100);
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(6, 2, 6));
        var quest = callQuest(issuer, QuestType.BREACH, QuestStatus.ACTIVE);
        quest.setTargetLocation(h.getLevel().dimension().location().toString(), pos, 8);
        seedCallBoard(player, issuer, List.of(), quest);
        var otherToken = java.util.UUID.randomUUID();
        try {
            NpcCallManager.handle(player, new NpcCallRequestPayload(otherToken, other.getUUID(), START));
            NpcCallManager.answerPending(player.server, player.server.overworld().getGameTime() + 200);
            drain(owner);
            int before = issuer.trustPoints(player);
            PlayerQuestManager.onQuestCacheFailed(player, pos);
            int after = issuer.trustPoints(player);
            h.assertTrue(after < before && PlayerQuestManager.activeContracts(player).isEmpty(), "Cache failure did not immediately fail the job and charge its normal penalty");
            h.assertTrue(de.artemis.cyberneticenhancements.common.quest.QuestFailureCalls.pending(player, issuer.getUUID()) != null,
                    "Cache failure did not queue an angry call");
            PlayerQuestManager.copyState(player, clone.player());
            h.assertTrue(de.artemis.cyberneticenhancements.common.quest.QuestFailureCalls.pending(clone.player(), issuer.getUUID()) != null,
                    "Pending cache-failure call did not survive player-state copying");
            for (int tick = 0; tick < 20; tick++) {
                player.tickCount++;
                PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            h.assertTrue(NpcCallManager.hasCall(player.getUUID()) && issuer.trustPoints(player) == after, "Failure call interrupted an existing call or repeated the penalty");
            NpcCallManager.handle(player, new NpcCallRequestPayload(otherToken, other.getUUID(), END));
            drain(owner);
            for (int tick = 0; tick < 20; tick++) {
                player.tickCount++;
                PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            var ringing = state(drain(owner));
            h.assertTrue(ringing.incoming() && ringing.npcId().equals(issuer.getUUID()), "Queued angry call did not come from the failed quest's issuer");
            NpcCallManager.handle(player, new NpcCallRequestPayload(ringing.callId(), issuer.getUUID(), ANSWER_INCOMING));
            var answered = state(drain(owner));
            h.assertTrue(answered.dialogue().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents line
                    && line.getKey().endsWith("incoming.cache_failed"), "Issuer did not use the angry cache-failure dialogue");
            NpcCallManager.handle(player, new NpcCallRequestPayload(ringing.callId(), issuer.getUUID(), ACKNOWLEDGE_REMINDER));
            NpcCallManager.handle(player, new NpcCallRequestPayload(ringing.callId(), issuer.getUUID(), END));
            for (int tick = 0; tick < 40; tick++) {
                player.tickCount++;
                PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            h.assertTrue(!NpcCallManager.hasCall(player.getUUID()) && issuer.trustPoints(player) == after
                    && de.artemis.cyberneticenhancements.common.quest.QuestFailureCalls.pending(player, issuer.getUUID()) == null,
                    "Cache-failure call repeated after acknowledgement or charged trust twice");
        } finally {
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
            PlayerQuestManager.clearAllQuestData(player);
            PlayerQuestManager.clearAllQuestData(clone.player());
            issuer.discard(); other.discard();
            player.server.getPlayerList().remove(player);
            clone.player().server.getPlayerList().remove(clone.player());
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_incoming", timeoutTicks = 200)
    public static void incomingMoneyCallsRequireAcceptanceOwnershipAndExplicitPayment(GameTestHelper h) {
        var owner = player(h, "incoming-owner");
        var other = player(h, "incoming-other");
        var npc = ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        npc.setPos(owner.player().position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.markMet(owner.player()); npc.markMet(other.player());
        PlayerEurodollarManager.setBalance(owner.player(), 100);
        try {
            drain(owner); drain(other);
            h.assertTrue(NpcCallManager.startIncoming(owner.player(), npc, NpcCallReason.MONEY, true), "NPC did not initiate a call");
            var incoming = state(drain(owner));
            h.assertTrue(incoming.incoming() && incoming.choices().size() == 2 && npc.isOnCall() && drain(other).isEmpty(), "Incoming call was not private or did not ring with a phone");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                NpcCallStatePayload.STREAM_CODEC.encode(buffer, incoming);
                h.assertTrue(incoming.equals(NpcCallStatePayload.STREAM_CODEC.decode(buffer)), "Incoming flag failed its network round trip");
            } finally { buffer.release(); }
            h.assertTrue(!NpcCallManager.startIncoming(other.player(), npc, NpcCallReason.CHECK_IN, true), "NPC initiated two simultaneous calls");
            NpcCallManager.handle(other.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), SEND_MONEY));
            h.assertTrue(drain(other).isEmpty() && drain(owner).isEmpty() && PlayerEurodollarManager.balance(owner.player()) == 100, "Unanswered call accepted money or another player's answer");
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), END));
            h.assertTrue(!npc.isOnCall() && PlayerEurodollarManager.balance(owner.player()) == 100, "Declining a call charged the player or left the phone");
            NpcCallManager.startIncoming(owner.player(), npc, NpcCallReason.MONEY, true);
            incoming = state(drain(owner));
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            var answered = state(drain(owner));
            h.assertTrue(!answered.incoming() && answered.choices().stream().anyMatch(choice -> choice.action() == SEND_MONEY), "Answered call did not explain the money request");
            PlayerEurodollarManager.setBalance(owner.player(), 0);
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), SEND_MONEY));
            drain(owner);
            h.assertTrue(PlayerEurodollarManager.balance(owner.player()) == 0, "Insufficient funds produced a negative balance");
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), END));
            PlayerEurodollarManager.setBalance(owner.player(), 100);
            NpcCallManager.startIncoming(owner.player(), npc, NpcCallReason.MONEY, true);
            incoming = state(drain(owner));
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            drain(owner);
            int trust = npc.trustPoints(owner.player());
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), SEND_MONEY));
            drain(owner);
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), SEND_MONEY));
            h.assertTrue(PlayerEurodollarManager.balance(owner.player()) == 80 && npc.trustPoints(owner.player()) == trust + 2,
                    "Money was charged twice or trust was granted twice");
            h.assertTrue(drain(other).isEmpty(), "Another player received money-call dialogue");
        } finally {
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(owner.player()));
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(other.player()));
            npc.discard();
            owner.player().server.getPlayerList().remove(owner.player());
            other.player().server.getPlayerList().remove(other.player());
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_incoming", timeoutTicks = 200)
    public static void overdueCallsUsePlayTimePersistCooldownsAndExpireUnanswered(GameTestHelper h) {
        var owner = player(h, "overdue-owner");
        var clone = player(h, "overdue-clone");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(owner.player().position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.markMet(owner.player());
        long now = NpcIncomingCalls.playTicks(owner.player());
        var quest = callQuest(npc, QuestType.RECOVERY, QuestStatus.ACTIVE);
        npc.ensureIdentity();
        quest.setAcceptedPlayTicks(now - NpcIncomingCalls.OVERDUE_AFTER + 1);
        seedCallBoard(owner.player(), npc, List.of(), quest);
        try {
            NpcIncomingCalls.resetCooldowns(owner.player());
            h.assertTrue(!NpcIncomingCalls.evaluate(owner.player()), "Quest was overdue before two hours played");
            PlayerQuestManager.setAcceptedPlayTicks(owner.player(), npc, now - NpcIncomingCalls.OVERDUE_AFTER);
            h.assertTrue(QuestContract.fromTag(PlayerQuestManager.activeContracts(owner.player()).getFirst().toTag()).acceptedPlayTicks()
                    == now - NpcIncomingCalls.OVERDUE_AFTER, "Acceptance time did not survive saving");
            drain(owner); drain(clone);
            h.assertTrue(NpcIncomingCalls.evaluate(owner.player()), "Overdue issuer did not call");
            var incoming = state(drain(owner));
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            var reminder = state(drain(owner));
            h.assertTrue(callKey(reminder).contains("incoming.overdue"), "NPC called for the wrong reason");
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), END));
            h.assertTrue(!NpcIncomingCalls.evaluate(owner.player()), "Ignored cooldown immediately repeated the call");
            NpcIncomingCalls.copyState(owner.player(), clone.player());
            h.assertTrue(NpcIncomingCalls.cooldownRemaining(clone.player()) == NpcIncomingCalls.COOLDOWN, "Respawn lost persistent call cooldowns");
            owner.player().awardStat(net.minecraft.stats.Stats.PLAY_TIME, (int) NpcIncomingCalls.COOLDOWN + 1);
            h.assertTrue(!NpcIncomingCalls.evaluate(owner.player()), "Same contract called again before its two-hour reminder cooldown");
            owner.player().awardStat(net.minecraft.stats.Stats.PLAY_TIME, (int) NpcIncomingCalls.OVERDUE_AFTER);
            h.assertTrue(NpcIncomingCalls.evaluate(owner.player()), "Long-overdue contract never called again");
            incoming = state(drain(owner));
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), KEEPALIVE));
            NpcCallManager.expire(owner.player().server, owner.player().server.overworld().getGameTime() + 601);
            var missed = state(drain(owner));
            h.assertTrue(missed.ended() && !missed.incoming() && !npc.isOnCall() && !NpcCallManager.hasCall(owner.player().getUUID()), "Unanswered incoming call kept ringing beyond thirty seconds");
            h.assertTrue(drain(clone).isEmpty(), "Other player received reminder dialogue");
        } finally {
            PlayerQuestManager.clearAllQuestData(owner.player());
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(owner.player()));
            npc.discard();
            owner.player().server.getPlayerList().remove(owner.player());
            clone.player().server.getPlayerList().remove(clone.player());
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_incoming", timeoutTicks = 200)
    public static void developerCommandsRequirePermissionAndForceTheSelectedContact(GameTestHelper h) {
        var owner = player(h, "call-command");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(owner.player().position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        var dispatcher = owner.player().server.getCommands().getDispatcher();
        var source = owner.player().createCommandSourceStack().withPermission(2);
        try {
            var node = dispatcher.getRoot().getChild("cyberneticenhancements").getChild("dev").getChild("calls");
            h.assertTrue(!node.canUse(source.withPermission(0)) && node.canUse(source), "Normal server players can run call-development commands");
            String forcePrefix = "cyberneticenhancements dev calls force ";
            var unknownSuggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(forcePrefix, source)).join().getList();
            h.assertTrue(unknownSuggestions.stream().noneMatch(suggestion -> suggestion.getText().equals(npc.getStringUUID())), "Unknown NPCs appeared in contact suggestions");
            drain(owner);
            int result = dispatcher.execute("cyberneticenhancements dev calls force " + npc.getStringUUID() + " money", source);
            var incoming = state(drain(owner));
            h.assertTrue(result == 1 && incoming.incoming() && incoming.npcId().equals(npc.getUUID()), "Force command called the wrong NPC");
            NpcCallManager.handle(owner.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), END));
            for (String command : List.of("force", "agequest")) {
                String prefix = "cyberneticenhancements dev calls " + command + " ";
                var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(prefix, source)).join().getList();
                h.assertTrue(suggestions.stream().anyMatch(suggestion -> suggestion.getText().equals(npc.getStringUUID())
                        && suggestion.getTooltip() != null && suggestion.getTooltip().getString().equals(npc.getName().getString())), "Call command did not suggest the known contact with its name");
                var filtered = dispatcher.getCompletionSuggestions(dispatcher.parse(prefix + npc.getStringUUID().substring(0, 8), source)).join().getList();
                h.assertTrue(filtered.stream().anyMatch(suggestion -> suggestion.getText().equals(npc.getStringUUID())), "Contact UUID completion stopped working after typing a prefix");
            }
            seedCallBoard(owner.player(), npc, List.of(), callQuest(npc, QuestType.RECOVERY, QuestStatus.ACTIVE));
            dispatcher.execute("cyberneticenhancements dev calls agequest " + npc.getStringUUID() + " 120", source);
            dispatcher.execute("cyberneticenhancements dev calls reset", source);
            result = dispatcher.execute("cyberneticenhancements dev calls check", source);
            h.assertTrue(result == 1 && state(drain(owner)).incoming(), "Commands could not test natural overdue eligibility without waiting");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new AssertionError("Call commands did not parse", exception);
        } finally {
            PlayerQuestManager.clearAllQuestData(owner.player());
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(owner.player()));
            npc.discard();
            owner.player().server.getPlayerList().remove(owner.player());
        }
        h.succeed();
    }

    private static String callKey(NpcCallStatePayload payload) {
        return ((net.minecraft.network.chat.contents.TranslatableContents) payload.dialogue().getContents()).getKey();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_urgent_schedule", timeoutTicks = 200)
    public static void urgentCallsRunNaturallyRespectCapacityAndCooldownsAndCanBeForcedForTesting(GameTestHelper h) {
        var owner = player(h, "urgent-schedule");
        var player = owner.player();
        var npcs = new java.util.ArrayList<de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity>();
        try {
            for (int index = 0; index < 4; index++) {
                var npc = ModEntityTypes.NETRUNNER.get().create(h.getLevel());
                npc.setNoAi(true);
                npc.setPos(player.position().add(index * 2, 0, 12));
                h.getLevel().addFreshEntity(npc);
                npc.ensureIdentity();
                npcs.add(npc);
                if (index == 0) {
                    npc.markMet(player);
                    seedCallBoard(player, npc, List.of(callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED)), null);
                } else seedCallBoard(player, npc, List.of(), callQuest(npc, QuestType.RECOVERY, QuestStatus.ACTIVE));
            }
            var npc = npcs.getFirst();
            h.assertTrue(NpcCallManager.startIncoming(player, npc, NpcCallReason.URGENT_QUEST, true), "Other contacts' active quests blocked an urgent offer");
            NpcCallManager.endForInteraction(player, npc);
            seedCallBoard(player, npc, List.of(callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED)),
                    callQuest(npc, QuestType.RECOVERY, QuestStatus.ACTIVE));
            h.assertTrue(!NpcCallManager.startIncoming(player, npc, NpcCallReason.URGENT_QUEST, true), "Urgent offer bypassed the per-contact active quest limit");
            PlayerQuestManager.clearAllQuestData(player);
            seedCallBoard(player, npc, List.of(callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED)), null);
            PlayerEurodollarManager.setBalance(player, 0);
            player.awardStat(net.minecraft.stats.Stats.PLAY_TIME, (int) NpcIncomingCalls.COOLDOWN);
            NpcIncomingCalls.resetCooldowns(player);
            drain(owner);
            boolean started = false;
            for (int attempt = 0; attempt < 128 && !started; attempt++) {
                player.getRandom().setSeed(attempt * 341873128712L + 13);
                started = NpcIncomingCalls.evaluate(player);
            }
            h.assertTrue(started, "Eligible urgent contracts never initiated a natural call");
            var incoming = state(drain(owner));
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            h.assertTrue(callKey(state(drain(owner))).contains("incoming.urgent."), "Automatic call used the wrong reason");
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), END));
            player.awardStat(net.minecraft.stats.Stats.PLAY_TIME, (int) NpcIncomingCalls.COOLDOWN + 1);
            for (int attempt = 0; attempt < 128; attempt++) {
                player.getRandom().setSeed(attempt * 341873128712L + 13);
                h.assertTrue(!NpcIncomingCalls.evaluate(player), "Same issuer ignored its urgent-offer cooldown");
            }
            drain(owner);
            var source = player.createCommandSourceStack().withPermission(2);
            int result = player.server.getCommands().getDispatcher().execute("cyberneticenhancements dev calls force " + npc.getStringUUID() + " urgent", source);
            h.assertTrue(result == 1 && state(drain(owner)).incoming(), "Urgent debug call command did not bypass automatic timing");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new AssertionError("Urgent debug command did not parse", exception);
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
            npcs.forEach(net.minecraft.world.entity.Entity::discard);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_urgent", timeoutTicks = 200)
    public static void urgentCallPinsItsSpecificOfferAndPaysItsSavedBonusOnlyOnCompletion(GameTestHelper h) {
        var owner = player(h, "urgent-owner");
        var other = player(h, "urgent-other");
        var player = owner.player();
        var npc = ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        npc.setNoAi(true);
        npc.setPos(player.position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(player);
        var offered = callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED);
        var unrelated = callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED);
        seedCallBoard(player, npc, List.of(offered), null);
        PlayerEurodollarManager.setBalance(player, 0);
        try {
            drain(owner); drain(other);
            h.assertTrue(NpcCallManager.startIncoming(player, npc, NpcCallReason.URGENT_QUEST, true), "Urgent offer did not ring");
            var incoming = state(drain(owner));
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ACCEPT_URGENT));
            NpcCallManager.handle(other.player(), new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            h.assertTrue(PlayerQuestManager.activeContracts(player).isEmpty(), "Unanswered or foreign call could accept urgent work");
            seedCallBoard(player, npc, List.of(unrelated, offered), null);
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            var briefing = state(drain(owner));
            h.assertTrue(callKey(briefing).contains("incoming.urgent.") && briefing.choices().stream().anyMatch(choice -> choice.action() == DECLINE_URGENT),
                    "Urgent call had no explicit briefing or decline choice");
            int trust = npc.trustPoints(player);
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ACCEPT_URGENT));
            drain(owner);
            var accepted = PlayerQuestManager.activeContracts(player).getFirst();
            h.assertTrue(accepted.id().equals(offered.id()) && accepted.isUrgent() && accepted.rewardMoney() == 7 && accepted.rewardTrust() == 2,
                    "Urgent call accepted a different offer or lost its bonus");
            accepted = QuestContract.fromTag(accepted.toTag());
            h.assertTrue(accepted.isUrgent() && accepted.rewardMoney() == 7 && accepted.rewardTrust() == 2, "Urgency bonus was lost or stacked after reload");
            h.assertTrue(PlayerEurodollarManager.balance(player) == 0 && npc.trustPoints(player) == trust, "Urgent rewards were paid before completion");
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ACCEPT_URGENT));
            h.assertTrue(PlayerQuestManager.activeContracts(player).size() == 1, "Replayed urgent acceptance created a second job");
            player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE, 2));
            npc.setPos(player.position().add(0, 0, 2));
            h.assertTrue(PlayerQuestManager.turnInIssuerActive(player, npc) == PlayerQuestManager.TurnInResult.OK, "Urgent recovery could not be completed");
            h.assertTrue(PlayerEurodollarManager.balance(player) == 7 && npc.trustPoints(player) == trust + 2, "Actual payout did not include the urgency bonus");
            h.assertTrue(!PlayerQuestManager.snapshotForIssuer(player, npc).offers().getFirst().isUrgent(), "Other offers inherited the call bonus");
            h.assertTrue(drain(other).isEmpty(), "Urgent offer details leaked to another player");
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
            npc.discard();
            player.server.getPlayerList().remove(player);
            other.player().server.getPlayerList().remove(other.player());
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_urgent_decline", timeoutTicks = 200)
    public static void urgentRefusalsCostOneTrustOnceButMissedAndStaleOffersDoNot(GameTestHelper h) {
        var owner = player(h, "urgent-decline");
        var player = owner.player();
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setNoAi(true);
        npc.setPos(player.position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(player);
        npc.raiseTrust(player, 10);
        var offered = callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED);
        seedCallBoard(player, npc, List.of(offered), null);
        try {
            int trust = npc.trustPoints(player);
            drain(owner);
            NpcCallManager.startIncoming(player, npc, NpcCallReason.URGENT_QUEST, true);
            var incoming = state(drain(owner));
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), END));
            h.assertTrue(npc.trustPoints(player) == trust && !PlayerQuestManager.snapshotForIssuer(player, npc).offers().getFirst().isUrgent(),
                    "Ignoring an urgent call charged trust or boosted the ordinary board offer");
            NpcCallManager.startIncoming(player, npc, NpcCallReason.URGENT_QUEST, true);
            incoming = state(drain(owner));
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), DECLINE_URGENT));
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), DECLINE_URGENT));
            h.assertTrue(npc.trustPoints(player) == trust - 1 && PlayerQuestManager.snapshotForIssuer(player, npc).offers().isEmpty()
                    && PlayerQuestManager.activeContracts(player).isEmpty(), "Explicit refusal did not withdraw the offer and charge exactly one trust");
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), END));
            seedCallBoard(player, npc, List.of(offered), null);
            NpcCallManager.startIncoming(player, npc, NpcCallReason.URGENT_QUEST, true);
            incoming = state(drain(owner));
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), ANSWER_INCOMING));
            seedCallBoard(player, npc, List.of(), null);
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), DECLINE_URGENT));
            h.assertTrue(npc.trustPoints(player) == trust - 1, "Stale offer caused a refusal penalty");
            NpcCallManager.handle(player, new NpcCallRequestPayload(incoming.callId(), npc.getUUID(), END));
            h.assertTrue(!NpcCallManager.startIncoming(player, npc, NpcCallReason.URGENT_QUEST, true), "Empty board generated a fake urgent offer");
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }
}
