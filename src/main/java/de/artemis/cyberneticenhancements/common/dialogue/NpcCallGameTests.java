package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload;
import de.artemis.cyberneticenhancements.common.network.NpcCallStatePayload;
import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import de.artemis.cyberneticenhancements.common.quest.QuestStatus;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import static de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NpcCallGameTests {
    @GameTest(template = "sandevistan_test", batch = "npc_idle_timeout", timeoutTicks = 260)
    public static void idleNpcCallsEndAfterTenSecondsDespiteHeartbeatsAndRepeatedStartPackets(GameTestHelper h) {
        var caller = player(h, "idle-call");
        var npc = ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        npc.setPos(caller.player.position().add(0, 0, 4));
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity(); npc.markMet(caller.player); npc.raiseTrust(caller.player, 100);
        h.assertTrue(NpcCallManager.startIncoming(caller.player, npc, NpcCallReason.CHECK_IN, true), "Could not start idle call fixture");
        var ringing = state(drain(caller));
        NpcCallManager.handle(caller.player, new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), ANSWER_INCOMING));
        drain(caller);
        int trust = npc.trustPoints(caller.player);
        int money = de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(caller.player);
        h.runAfterDelay(100, () -> {
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), KEEPALIVE));
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), START));
            drain(caller);
        });
        h.runAfterDelay(199, () -> {
            NpcCallManager.expire(caller.player.server, caller.player.server.overworld().getGameTime());
            h.assertTrue(NpcCallManager.hasCall(caller.player.getUUID()), "NPC hung up before ten seconds");
        });
        h.runAfterDelay(201, () -> {
            try {
                NpcCallManager.expire(caller.player.server, caller.player.server.overworld().getGameTime());
                var ended = drain(caller).stream().filter(NpcCallStatePayload.class::isInstance).map(NpcCallStatePayload.class::cast)
                        .filter(NpcCallStatePayload::ended).findFirst().orElse(null);
                if (ended == null) h.fail("Idle call did not send its terminal state");
                h.assertTrue(ended.ended() && ended.dialogue().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents line
                        && line.getKey().endsWith("idle_hangup"), "Idle call did not end with the NPC's time-wasting message: ended="
                        + ended.ended() + ", line=" + ended.dialogue().getContents() + ", still connected=" + NpcCallManager.hasCall(caller.player.getUUID()));
                h.assertTrue(!NpcCallManager.hasCall(caller.player.getUUID()) && !npc.isOnCall(), "Idle hang-up kept the contact occupied");
                h.assertTrue(npc.trustPoints(caller.player) == trust
                        && de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(caller.player) == money,
                        "Idle hang-up charged trust or money");
            } finally {
                NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
                npc.discard(); caller.player.server.getPlayerList().remove(caller.player);
            }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "npc_idle_reply_reset", timeoutTicks = 400)
    public static void anActualConversationReplyStartsANewTenSecondIdleWindow(GameTestHelper h) {
        var caller = player(h, "idle-reply");
        var npc = ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        npc.setPos(caller.player.position().add(0, 0, 4)); npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc); npc.ensureIdentity(); npc.markMet(caller.player);
        h.assertTrue(NpcCallManager.startIncoming(caller.player, npc, NpcCallReason.CHECK_IN, true), "Could not start reply timer fixture");
        var ringing = state(drain(caller));
        NpcCallManager.handle(caller.player, new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), ANSWER_INCOMING));
        drain(caller);
        h.runAfterDelay(150, () -> {
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(ringing.callId(), npc.getUUID(), LOCATE));
            drain(caller);
        });
        h.runAfterDelay(201, () -> {
            NpcCallManager.expire(caller.player.server, caller.player.server.overworld().getGameTime());
            h.assertTrue(NpcCallManager.hasCall(caller.player.getUUID()), "Real reply did not reset the idle timer");
        });
        h.runAfterDelay(351, () -> {
            try {
                NpcCallManager.expire(caller.player.server, caller.player.server.overworld().getGameTime());
                h.assertTrue(!NpcCallManager.hasCall(caller.player.getUUID()), "NPC did not hang up ten seconds after the last real reply");
            } finally {
                NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
                npc.discard(); caller.player.server.getPlayerList().remove(caller.player);
            }
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "npc_call_interruption", timeoutTicks = 100)
    public static void damageHangsUpAndDeathReportsAnErrorOnlyToTheCaller(GameTestHelper h) {
        var caller = player(h, "call-damage");
        var observer = player(h, "call-witness");
        var npc = ModEntityTypes.FIXER.get().create(h.getLevel());
        npc.setPos(caller.player.position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.markMet(caller.player);
        UUID token = UUID.randomUUID();
        try {
            startAndAnswer(caller, npc, token);
            drain(caller); drain(observer);
            npc.hurt(h.getLevel().damageSources().generic(), 1);
            var hungUp = state(drain(caller));
            h.assertTrue(hungUp.ended() && callTextKey(hungUp.dialogue()).endsWith("contact_hung_up")
                    && npc.isAlive() && !npc.isOnCall() && !NpcCallManager.hasCall(caller.player.getUUID()), "Damage did not immediately hang up and clear the phone");
            h.assertTrue(drain(observer).isEmpty(), "Another player received the hangup message");
            token = UUID.randomUUID();
            startAndAnswer(caller, npc, token);
            drain(caller);
            npc.hurt(h.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
            var death = state(drain(caller));
            h.assertTrue(death.ended() && callTextKey(death.dialogue()).endsWith("contact_died")
                    && !npc.isOnCall() && !NpcCallManager.hasCall(caller.player.getUUID()), "Lethal damage reported a normal hangup instead of a connection error");
            h.assertTrue(drain(observer).isEmpty(), "Another player received the death error");
        } finally {
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
            npc.discard();
            caller.player.server.getPlayerList().remove(caller.player);
            observer.player.server.getPlayerList().remove(observer.player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_call_context", timeoutTicks = 100)
    public static void repliesReflectNightWeatherAndRelationship(GameTestHelper h) {
        var caller = player(h, "call-context");
        var level = h.getLevel();
        var npc = ModEntityTypes.TECHIE.get().create(level);
        var runner = ModEntityTypes.NETRUNNER.get().create(level);
        npc.setPos(caller.player.position().add(0, 80, 12));
        runner.setPos(npc.position().add(2, 0, 0));
        caller.player.setPos(npc.position().add(0, 0, 12));
        long time = level.getDayTime();
        float rain = level.getRainLevel(1), thunder = level.getThunderLevel(1);
        try {
            level.setRainLevel(0);
            level.setThunderLevel(0);
            level.setDayTime(18000);
            h.assertTrue(callTextKey(NpcCallDialogue.greeting(npc, caller.player)).contains("greeting.night_sleepy"), "Night call did not wake a daytime contact");
            h.assertTrue(callTextKey(NpcCallDialogue.greeting(runner, caller.player)).contains("greeting.night_awake"), "Night-active contact claimed to have been asleep");
            level.setDayTime(6000);
            level.setRainLevel(1);
            h.assertTrue(level.isRainingAt(npc.blockPosition()), "Test contact was not exposed to rain");
            h.assertTrue(callTextKey(NpcCallDialogue.greeting(npc, caller.player)).contains("greeting.rain"), "Greeting ignored local rain");
            h.assertTrue(callTextKey(NpcCallDialogue.meeting(npc, caller.player)).contains("meeting.rain"), "Meeting reply ignored rain at the destination");
            level.setThunderLevel(1);
            h.assertTrue(callTextKey(NpcCallDialogue.meeting(npc, caller.player)).contains("meeting.storm"), "Meeting reply ignored a thunderstorm");
            level.setRainLevel(0);
            level.setThunderLevel(0);
            npc.raiseTrust(caller.player, 150);
            h.assertTrue(callTextKey(NpcCallDialogue.greeting(npc, caller.player)).contains("greeting.trusted"), "Trusted caller got no relationship-aware greeting");
            npc.applyDialoguePenalty(caller.player, 0, 1, false);
            h.assertTrue(callTextKey(NpcCallDialogue.greeting(npc, caller.player)).contains("greeting.annoyed"), "Annoyed contact greeted caller warmly");
            var variants = new java.util.HashSet<String>();
            for (int index = 0; index < 20; index++) variants.add(callTextKey(NpcCallDialogue.greeting(npc, caller.player)));
            h.assertTrue(variants.size() > 1, "Calls repeated a single fixed greeting");
        } finally {
            level.setDayTime(time);
            level.setRainLevel(rain);
            level.setThunderLevel(thunder);
            npc.discard(); runner.discard();
            caller.player.server.getPlayerList().remove(caller.player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_calls", timeoutTicks = 50)
    public static void unansweredCallsExpireDespiteKeepalivesAndAllowImmediateRedial(GameTestHelper h) {
        var caller = player(h, "redial-test");
        var npc = ModEntityTypes.FIXER.get().create(h.getLevel());
        npc.setNoAi(true); npc.setPos(caller.player.position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc); npc.markMet(caller.player);
        npc.applyDialoguePenalty(caller.player, 0, 8, false);
        long now = caller.player.server.overworld().getGameTime();
        UUID ignored = null;
        try {
            drain(caller);
            for (int attempt = 0; attempt < 32; attempt++) {
                npc.getRandom().setSeed(attempt);
                UUID id = UUID.randomUUID();
                NpcCallManager.handle(caller.player, new NpcCallRequestPayload(id, npc.getUUID(), START));
                state(drain(caller));
                NpcCallManager.answerPending(caller.player.server, now + 200);
                if (drain(caller).isEmpty()) { ignored = id; break; }
            }
            h.assertTrue(ignored != null, "Could not prepare an ignored call");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(ignored, npc.getUUID(), KEEPALIVE));
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(ignored, npc.getUUID(), START));
            state(drain(caller));
            NpcCallManager.expire(caller.player.server, now + 600);
            var ended = state(drain(caller));
            h.assertTrue(ended.ended() && callTextKey(ended.dialogue()).endsWith(".unanswered") && !NpcCallManager.hasCall(caller.player.getUUID()),
                    "Keepalives or duplicate starts extended ringing beyond 30 seconds");
            UUID next = UUID.randomUUID();
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(next, npc.getUUID(), START));
            var redial = state(drain(caller));
            h.assertTrue(!redial.ended() && redial.callId().equals(next), "Could not immediately call the NPC again");
        } finally {
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
            npc.discard(); caller.player.server.getPlayerList().remove(caller.player);
        }
        h.succeed();
    }

    private static String callTextKey(net.minecraft.network.chat.Component component) {
        return ((net.minecraft.network.chat.contents.TranslatableContents) component.getContents()).getKey();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_calls", timeoutTicks = 200)
    public static void callsAcceptOnlyReviewedCurrentOffersAndAnswerIssuerSpecificQuestions(GameTestHelper h) {
        var caller = player(h, "call-contract");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        var otherNpc = ModEntityTypes.MERC.get().create(h.getLevel());
        npc.setPos(caller.player.position().add(0, 0, 12));
        otherNpc.setPos(caller.player.position().add(12, 0, 0));
        h.getLevel().addFreshEntity(npc);
        h.getLevel().addFreshEntity(otherNpc);
        npc.markMet(caller.player);
        otherNpc.markMet(caller.player);
        UUID token = UUID.randomUUID();
        try {
            var offers = List.of(callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED), callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED),
                    callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED));
            seedCallBoard(caller.player, npc, offers, null);
            startAndAnswer(caller, npc, token);
            drain(caller);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), ACCEPT_CONTRACT));
            h.assertTrue(PlayerQuestManager.snapshotForIssuer(caller.player, npc).active() == null && drain(caller).isEmpty(), "Unreviewed offer was accepted");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), WORK));
            var list = state(drain(caller));
            h.assertTrue(list.choices().size() == 5 && list.choices().stream().anyMatch(choice -> choice.action() == MORE_CONTRACTS), "Call did not paginate offers within five answers");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                NpcCallStatePayload.STREAM_CODEC.encode(buffer, list);
                h.assertTrue(list.equals(NpcCallStatePayload.STREAM_CODEC.decode(buffer)), "Contract choice labels failed their network round trip");
            } finally { buffer.release(); }
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), OFFER_FIRST));
            h.assertTrue(state(drain(caller)).choices().stream().anyMatch(choice -> choice.action() == ACCEPT_CONTRACT), "Offer had no confirmation step");
            var refreshed = List.of(callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED), callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED),
                    callQuest(npc, QuestType.RECOVERY, QuestStatus.OFFERED));
            seedCallBoard(caller.player, npc, refreshed, null);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), ACCEPT_CONTRACT));
            h.assertTrue(PlayerQuestManager.snapshotForIssuer(caller.player, npc).active() == null, "Stale offer accepted a different contract at its former index");
            drain(caller);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), MORE_CONTRACTS));
            drain(caller);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), OFFER_FIRST));
            drain(caller);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, otherNpc.getUUID(), ACCEPT_CONTRACT));
            h.assertTrue(drain(caller).isEmpty() && PlayerQuestManager.snapshotForIssuer(caller.player, npc).active() == null, "Offer was accepted with a forged issuer");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), ACCEPT_CONTRACT));
            var accepted = drain(caller);
            h.assertTrue(PlayerQuestManager.snapshotForIssuer(caller.player, npc).active().id().equals(refreshed.get(2).id()), "Call accepted the wrong page's contract");
            h.assertTrue(accepted.stream().anyMatch(value -> value instanceof de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload), "Accepted job was not synced to the Archive");
            var otherJob = callQuest(otherNpc, QuestType.EXTRACTION, QuestStatus.READY);
            seedCallBoard(caller.player, otherNpc, List.of(), otherJob);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), QUEST_HELP));
            var help = state(drain(caller));
            h.assertTrue(help.dialogue().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    && text.getKey().contains("quest.help.recovery"), "Contract help came from another issuer");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), QUEST_LOCATION));
            h.assertTrue(state(drain(caller)).dialogue().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    && text.getKey().endsWith("quest.no_site"), "Recovery job invented a destination");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), END));
            PlayerQuestManager.clearAllQuestData(caller.player);
            var delivery = callQuest(npc, QuestType.DELIVERY, QuestStatus.OFFERED);
            net.minecraft.core.BlockPos site = null;
            for (int distance = 160; distance <= 2048; distance += 128) {
                var candidate = h.getLevel().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, npc.blockPosition().offset(0, 0, distance));
                if (de.artemis.cyberneticenhancements.common.quest.QuestSiteReservations.get(h.getLevel()).available(candidate, 32, delivery.id(), h.getLevel().getGameTime())) {
                    site = candidate;
                    break;
                }
            }
            h.assertTrue(site != null, "No unreserved delivery test site");
            delivery.setTargetLocation(h.getLevel().dimension().location().toString(), site, 4);
            seedCallBoard(caller.player, npc, List.of(delivery), null);
            token = UUID.randomUUID();
            startAndAnswer(caller, npc, token);
            drain(caller);
            for (int action : new int[]{WORK, OFFER_FIRST, ACCEPT_CONTRACT}) {
                NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), action));
                drain(caller);
            }
            h.assertTrue(PlayerQuestManager.snapshotForIssuer(caller.player, npc).active().type() == QuestType.DELIVERY
                    && caller.player.getInventory().countItem(net.minecraft.world.item.Items.COMPASS) == 0
                    && caller.player.getInventory().countItem(de.artemis.cyberneticenhancements.common.registry.ModItems.COURIER_PACKAGE.get()) == 1
                    && de.artemis.cyberneticenhancements.common.quest.QuestNavigation.capture(caller.player).targets().stream()
                            .anyMatch(target -> target.questId().equals(delivery.id())), "Phone acceptance did not issue the parcel and HUD navigation immediately");
        } finally {
            PlayerQuestManager.clearAllQuestData(caller.player);
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
            npc.discard(); otherNpc.discard();
            caller.player.server.getPlayerList().remove(caller.player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_calls", timeoutTicks = 200)
    public static void questHudCallsOpenTheRequestedTurnInAndHangUpAfterPayment(GameTestHelper h) {
        var caller = player(h, "call-hud-turn-in");
        var player = caller.player;
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(player.position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.markMet(player);
        try {
            var quest = callQuest(npc, QuestType.BREACH, QuestStatus.READY);
            var newer = callQuest(npc, QuestType.BREACH, QuestStatus.READY);
            seedCallBoard(player, npc, List.of(), quest);
            var staleRequest = new NpcCallRequestPayload(UUID.randomUUID(), npc.getUUID(), START, quest.id());
            NpcCallManager.handle(player, staleRequest);
            drain(caller);
            seedCallBoard(player, npc, List.of(), newer);
            NpcCallManager.answerPending(player.server, player.server.overworld().getGameTime() + 200);
            var rejected = state(drain(caller));
            h.assertTrue(rejected.ended() && rejected.choices().isEmpty() && !NpcCallManager.hasCall(player.getUUID()),
                    "A stale quest shortcut showed another contract");

            var request = new NpcCallRequestPayload(UUID.randomUUID(), npc.getUUID(), START, newer.id());
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                NpcCallRequestPayload.STREAM_CODEC.encode(buffer, request);
                h.assertTrue(request.equals(NpcCallRequestPayload.STREAM_CODEC.decode(buffer)), "Quest ID was lost in transit");
            } finally { buffer.release(); }
            int balance = PlayerEurodollarManager.balance(player);
            NpcCallManager.handle(player, request);
            drain(caller);
            NpcCallManager.answerPending(player.server, player.server.overworld().getGameTime() + 200);
            var connected = state(drain(caller));
            h.assertTrue(!connected.ended() && connected.choices().getFirst().action() == REPORT_CONTRACT
                    && PlayerEurodollarManager.balance(player) == balance, "Shortcut skipped confirmation or opened the general call menu");
            NpcCallManager.handle(player, new NpcCallRequestPayload(request.callId(), npc.getUUID(), REPORT_CONTRACT));
            h.assertTrue(state(drain(caller)).ended() && !NpcCallManager.hasCall(player.getUUID()) && !npc.isOnCall(),
                    "Successful turn-in left the call or NPC phone active");
            h.assertTrue(PlayerEurodollarManager.balance(player) == balance + newer.rewardMoney(), "Confirmed turn-in did not pay");
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
            npc.discard();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_calls", timeoutTicks = 200)
    public static void itemReturnsRequireAnInPersonHandoverWhileRemoteReportsPayOtherQuestsExactlyOnce(GameTestHelper h) {
        var caller = player(h, "call-payment");
        var npc = ModEntityTypes.TECHIE.get().create(h.getLevel());
        npc.setPos(caller.player.position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.markMet(caller.player);
        try {
            for (QuestType type : QuestType.values()) {
                var quest = callQuest(npc, type, QuestStatus.READY);
                caller.player.getInventory().clearContent();
                if (type == QuestType.RECOVERY) caller.player.getInventory().add(new net.minecraft.world.item.ItemStack(quest.recoveryItem(), quest.targetCount()));
                if (type == QuestType.CYBERPSYCHO) caller.player.getInventory().add(PlayerQuestManager.createCyberpsychoShard(quest));
                if (type == QuestType.EXTRACTION || type == QuestType.LOCATE) {
                    var shard = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD);
                    var tag = new CompoundTag();
                    tag.putString("QuestContractId", quest.id());
                    tag.putString("QuestItemType", type == QuestType.LOCATE ? "signal_beacon" : "exfil_shard");
                    shard.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    caller.player.getInventory().add(shard);
                }
                if (type == QuestType.LOCATE) { // An honest report: revived patient, beacon logged right beside them.
                    quest.addPatientStage(QuestContract.PATIENT_REVIVED | QuestContract.PATIENT_MARKED);
                    quest.setBeaconPos(caller.player.blockPosition());
                    quest.setPatientPos(caller.player.blockPosition());
                }
                seedCallBoard(caller.player, npc, List.of(), quest);
                UUID token = UUID.randomUUID();
                startAndAnswer(caller, npc, token);
                drain(caller);
                NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), ADVICE));
                var options = state(drain(caller)).choices();
                h.assertTrue(options.stream().anyMatch(choice -> choice.action() == REPORT_CONTRACT) != type.requiresInPersonTurnIn(), "Wrong remote-report option for " + type);
                int balance = PlayerEurodollarManager.balance(caller.player);
                int experience = caller.player.totalExperience;
                int itemCount = caller.player.getInventory().items.stream().mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
                NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), REPORT_CONTRACT));
                drain(caller);
                NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), REPORT_CONTRACT));
                drain(caller);
                if (type.requiresInPersonTurnIn()) {
                    h.assertTrue(PlayerEurodollarManager.balance(caller.player) == balance
                            && caller.player.getInventory().items.stream().mapToInt(net.minecraft.world.item.ItemStack::getCount).sum() == itemCount,
                            "Forged call turn-in paid or consumed physical items for " + type);
                    h.assertTrue(PlayerQuestManager.turnInRemotely(caller.player, quest.id()) == PlayerQuestManager.TurnInResult.IN_PERSON_REQUIRED,
                            "Server allowed a remote physical handover for " + type);
                    h.assertTrue(PlayerQuestManager.turnInIssuerActive(caller.player, npc) == PlayerQuestManager.TurnInResult.UNAVAILABLE,
                            "Direct turn-in allowed a distant physical handover");
                    var original = npc.position();
                    npc.setPos(caller.player.position().add(0, 0, 2));
                    h.assertTrue(PlayerQuestManager.turnInIssuerActive(caller.player, npc) == PlayerQuestManager.TurnInResult.OK,
                            "In-person handover failed for " + type);
                    h.assertTrue(PlayerQuestManager.turnInIssuerActive(caller.player, npc) != PlayerQuestManager.TurnInResult.OK, "Physical return paid twice");
                    npc.setPos(original);
                }
                h.assertTrue(PlayerEurodollarManager.balance(caller.player) == balance + quest.rewardMoney(), "Quest paid incorrectly or twice for " + type);
                h.assertTrue(caller.player.totalExperience == experience + quest.rewardExperience(), "Quest XP was missing or paid twice for " + type);
                h.assertTrue(PlayerQuestManager.snapshotForIssuer(caller.player, npc).active() == null, "Remote turn-in left the contract active");
                if (type == QuestType.RECOVERY) h.assertTrue(caller.player.getInventory().countItem(quest.recoveryItem()) == 0, "Remote turn-in did not consume requested materials");
                if (type == QuestType.EXTRACTION || type == QuestType.CYBERPSYCHO) h.assertTrue(caller.player.getInventory().countItem(net.minecraft.world.item.Items.AMETHYST_SHARD) == 0, "Remote turn-in did not consume its evidence shard");
                NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), END));
                PlayerQuestManager.clearAllQuestData(caller.player);
            }
        } finally {
            PlayerQuestManager.clearAllQuestData(caller.player);
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
            npc.discard();
            caller.player.server.getPlayerList().remove(caller.player);
        }
        h.succeed();
    }

    static QuestContract callQuest(AbstractCityNpcEntity npc, QuestType type, QuestStatus status) {
        return new QuestContract(UUID.randomUUID().toString(), npc.getUUID(), npc.getName().getString(), npc.npcTypeId(), type, 1, status,
                0, 2, status == QuestStatus.READY ? 2 : 0, status == QuestStatus.READY, "", null, 4,
                5, 1, QuestContract.REWARD_NONE, 0, npc.level().getDayTime() / 24000L, -1);
    }

    static void seedCallBoard(ServerPlayer player, AbstractCityNpcEntity npc, List<QuestContract> offers, QuestContract active) {
        var root = player.getPersistentData().getCompound("cyberneticenhancements.quest_state");
        var boards = root.getCompound("IssuerBoards");
        var board = new CompoundTag();
        var list = new ListTag();
        offers.forEach(offer -> list.add(offer.toTag()));
        board.put("Offers", list);
        board.putLong("RefreshDay", player.level().getDayTime() / 24000L);
        if (active != null) board.put("Active", active.toTag());
        boards.put(npc.getStringUUID(), board);
        root.put("IssuerBoards", boards);
        player.getPersistentData().put("cyberneticenhancements.quest_state", root);
    }

    @GameTest(template = "sandevistan_test", batch = "npc_calls", timeoutTicks = 200)
    public static void ringingBusyLinesRefusalsAndPhoneCleanup(GameTestHelper h) {
        var caller = player(h, "ring-owner");
        var other = player(h, "ring-other");
        var npc = ModEntityTypes.FIXER.get().create(h.getLevel());
        npc.setNoAi(true);
        npc.setPos(caller.player.position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.markMet(caller.player);
        npc.markMet(other.player);
        var held = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND);
        npc.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
        UUID token = UUID.randomUUID();
        long now = caller.player.server.overworld().getGameTime();
        try {
            drain(caller);
            drain(other);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), START));
            var ringing = state(drain(caller));
            h.assertTrue(!ringing.ended() && ringing.choices().isEmpty() && !npc.isOnCall(), "Ringing call already exposed dialogue or phone");
            NpcCallManager.answerPending(caller.player.server, now + 10);
            h.assertTrue(drain(caller).isEmpty(), "NPC answered before ringing");
            NpcCallManager.handle(other.player, new NpcCallRequestPayload(UUID.randomUUID(), npc.getUUID(), START));
            var busy = state(drain(other));
            h.assertTrue(busy.ended() && busy.dialogue().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    && text.getKey().endsWith(".line_busy"), "Occupied line did not return an explicit busy signal");
            NpcCallManager.answerPending(caller.player.server, now + 200);
            h.assertTrue(!state(drain(caller)).ended() && npc.isOnCall() && npc.getMainHandItem() == held,
                    "Answering did not show the phone or replaced real equipment");
            npc.mobInteract(other.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(other.player.containerMenu == other.player.inventoryMenu && npc.isOnCall(), "Another player interrupted the call in person");
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
            h.assertTrue(!npc.isOnCall() && npc.getMainHandItem() == held, "Logout left the phone active or changed equipment");
            npc.applyDialoguePenalty(caller.player, 0, 8, false);
            boolean ignored = false, rejected = false, checkedIgnoredLine = false;
            for (int attempt = 0; attempt < 32; attempt++) {
                npc.getRandom().setSeed(attempt);
                token = UUID.randomUUID();
                NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), START));
                h.assertTrue(!state(drain(caller)).ended(), "Hostile NPC did not ring before declining or ignoring");
                NpcCallManager.answerPending(caller.player.server, now + 200);
                var reply = drain(caller);
                if (reply.isEmpty()) {
                    ignored = true;
                    NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), KEEPALIVE));
                    NpcCallManager.answerPending(caller.player.server, now + 1000);
                    h.assertTrue(NpcCallManager.hasCall(caller.player.getUUID()) && drain(caller).isEmpty(), "Ignored call stopped ringing or answered");
                    if (!checkedIgnoredLine) {
                        UUID otherToken = UUID.randomUUID();
                        startAndAnswer(other, npc, otherToken);
                        drain(other);
                        NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), END));
                        h.assertTrue(npc.isOnCall() && NpcCallManager.hasCall(other.player.getUUID()), "Ignored caller's hangup interrupted another player's conversation");
                        NpcCallManager.handle(other.player, new NpcCallRequestPayload(otherToken, npc.getUUID(), END));
                        checkedIgnoredLine = true;
                    }
                } else {
                    rejected = true;
                    h.assertTrue(state(reply).ended() && !NpcCallManager.hasCall(caller.player.getUUID()), "Rejected call kept its session");
                }
                h.assertTrue(!npc.isOnCall(), "Refusing NPC displayed a phone");
                NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), END));
            }
            h.assertTrue(ignored && rejected, "Refusal behavior did not vary between ignoring and declining");
            token = UUID.randomUUID();
            startAndAnswer(other, npc, token);
            drain(other);
            npc.discard();
            NpcCallManager.expire(caller.player.server, now);
            h.assertTrue(!npc.isOnCall() && !NpcCallManager.hasCall(other.player.getUUID()), "Removed NPC retained phone or call");
        } finally {
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(other.player));
            npc.discard();
            caller.player.server.getPlayerList().remove(caller.player);
            other.player.server.getPlayerList().remove(other.player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_calls", timeoutTicks = 200)
    public static void inPersonConversationEndsCallAndRequiresNearbyVisibleContact(GameTestHelper h) {
        var caller = player(h, "call-in-person");
        var npc = ModEntityTypes.FIXER.get().create(h.getLevel());
        npc.setNoAi(true);
        npc.setPos(caller.player.position().add(0, 0, 12));
        h.getLevel().addFreshEntity(npc);
        npc.markMet(caller.player);
        UUID token = UUID.randomUUID();
        try {
            startAndAnswer(caller, npc, token);
            drain(caller);
            caller.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, npc.getEyePosition());
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), TALK_IN_PERSON));
            h.assertTrue(NpcCallManager.hasCall(caller.player.getUUID()) && caller.player.containerMenu == caller.player.inventoryMenu, "Distant call opened an in-person menu");
            npc.setPos(caller.player.position().add(0, 0, 2));
            caller.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, caller.player.getEyePosition().add(0, 0, -2));
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), TALK_IN_PERSON));
            h.assertTrue(NpcCallManager.hasCall(caller.player.getUUID()) && caller.player.containerMenu == caller.player.inventoryMenu, "Looking away opened the NPC menu");
            caller.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, npc.getEyePosition());
            var wall = BlockPos.containing(caller.player.getEyePosition().add(0, 0, 1));
            h.getLevel().setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), TALK_IN_PERSON));
            h.assertTrue(NpcCallManager.hasCall(caller.player.getUUID()) && caller.player.containerMenu == caller.player.inventoryMenu, "Call opened an NPC menu through a wall");
            h.getLevel().removeBlock(wall, false);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), TALK_IN_PERSON));
            h.assertTrue(!NpcCallManager.hasCall(caller.player.getUUID())
                    && caller.player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu menu
                    && menu.entityId() == npc.getId(), "Looking at the nearby contact did not replace the call with dialogue");
            h.assertTrue(state(drain(caller)).ended(), "Call overlay did not receive an end notification");
            h.assertTrue(npc.isInConversation() && npc.conversationSpokeAt() == npc.level().getGameTime(), "Conversation animation state was not synchronized when dialogue opened");
            caller.player.closeContainer();
            h.assertTrue(!npc.isInConversation(), "Closing dialogue left the NPC in its conversation pose");
            token = UUID.randomUUID();
            startAndAnswer(caller, npc, token);
            drain(caller);
            npc.mobInteract(caller.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(!NpcCallManager.hasCall(caller.player.getUUID())
                    && caller.player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu,
                    "Direct interaction left the call active");
        } finally {
            caller.player.closeContainer();
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
            npc.discard();
            caller.player.server.getPlayerList().remove(caller.player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "npc_calls", timeoutTicks = 200)
    public static void callsArePrivateAuthenticatedAndPerformOnlySelectedActions(GameTestHelper h) {
        var caller = player(h, "call-owner");
        var other = player(h, "call-other");
        var npc = ModEntityTypes.FIXER.get().create(h.getLevel());
        npc.setPos(caller.player.position().add(12, 0, 0));
        h.getLevel().addFreshEntity(npc);
        UUID token = UUID.randomUUID();
        try {
            drain(caller);
            drain(other);
            NpcCallManager.handle(other.player, new NpcCallRequestPayload(UUID.randomUUID(), npc.getUUID(), START));
            h.assertTrue(state(drain(other)).ended() && !NpcCallManager.hasCall(other.player.getUUID()), "Unmet NPC accepted a call");
            npc.markMet(caller.player);
            npc.markMet(other.player);
            startAndAnswer(caller, npc, token);
            var start = drain(caller);
            var connected = state(start);
            h.assertTrue(!connected.ended() && connected.callId().equals(token) && connected.choices().size() == 5, "Call did not connect with its choices");
            h.assertTrue(connected.name().equals(npc.getName().getString()) && connected.category().equals(npc.npcCategory().id()), "Call portrait used the wrong identity");
            h.assertTrue(drain(other).isEmpty(), "Another player received the caller's dialogue");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                NpcCallStatePayload.STREAM_CODEC.encode(buffer, connected);
                h.assertTrue(connected.equals(NpcCallStatePayload.STREAM_CODEC.decode(buffer)) && buffer.readableBytes() == 0, "Call state failed network round trip");
                var request = new NpcCallRequestPayload(token, npc.getUUID(), WORK);
                NpcCallRequestPayload.STREAM_CODEC.encode(buffer, request);
                h.assertTrue(request.equals(NpcCallRequestPayload.STREAM_CODEC.decode(buffer)), "Call request failed network round trip");
            } finally { buffer.release(); }
            NpcCallManager.handle(other.player, new NpcCallRequestPayload(token, npc.getUUID(), LOCATE));
            h.assertTrue(drain(other).isEmpty(), "Another player could use the caller's session");
            NpcCallManager.handle(other.player, new NpcCallRequestPayload(UUID.randomUUID(), npc.getUUID(), START));
            h.assertTrue(state(drain(other)).ended(), "Two players occupied the same contact call");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(UUID.randomUUID(), npc.getUUID(), LOCATE));
            h.assertTrue(drain(caller).isEmpty(), "Stale call ID was accepted");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), LOCATE));
            var located = drain(caller);
            h.assertTrue(located.stream().anyMatch(value -> value instanceof FaceHazardHighlightPayload highlight
                    && highlight.npcLocator() && highlight.entityIds().contains(npc.getUUID())), "Call location choice did not send the local locator");
            h.assertTrue(drain(other).isEmpty(), "Another player received the location marker");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), WORK));
            var work = state(drain(caller));
            h.assertTrue(work.choices().stream().allMatch(choice -> choice.action() == BACK || choice.action() == END), "Work dialogue exposed invalid actions");
            var before = npc.position();
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), KEEPALIVE));
            h.assertTrue(NpcCallManager.hasCall(caller.player.getUUID()) && drain(caller).isEmpty(), "Keepalive reset dialogue or ended the call");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), MEET));
            h.assertTrue(npc.position().equals(before) && drain(caller).isEmpty(), "Unavailable dialogue choice summoned the contact");
            var inventory = net.minecraft.world.inventory.ChestMenu.oneRow(7, caller.player.getInventory());
            caller.player.containerMenu = inventory;
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), BACK));
            h.assertTrue(caller.player.containerMenu == inventory && state(drain(caller)).choices().size() == 5, "Call could not continue during normal inventory use");
            caller.player.containerMenu = caller.player.inventoryMenu;
            drain(caller);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), MEET));
            h.assertTrue(state(drain(caller)).ended() && !NpcCallManager.hasCall(caller.player.getUUID())
                    && npc.distanceToSqr(caller.player) < 8, "Meeting did not bring the contact and finish the call");
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), LOCATE));
            h.assertTrue(drain(caller).isEmpty(), "Ended call could still request actions");
            token = UUID.randomUUID();
            startAndAnswer(caller, npc, token);
            drain(caller);
            NpcCallManager.expire(caller.player.server, caller.player.server.overworld().getGameTime() + 1601);
            h.assertTrue(state(drain(caller)).ended() && !NpcCallManager.hasCall(caller.player.getUUID()), "Idle call never expired");
            token = UUID.randomUUID();
            startAndAnswer(caller, npc, token);
            drain(caller);
            NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), END));
            h.assertTrue(!NpcCallManager.hasCall(caller.player.getUUID()), "Hangup left the contact occupied");
        } finally {
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(caller.player));
            NpcCallManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(other.player));
            npc.discard();
            caller.player.server.getPlayerList().remove(caller.player);
            other.player.server.getPlayerList().remove(other.player);
        }
        h.succeed();
    }

    private static void startAndAnswer(CallPlayer caller, de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc, UUID token) {
        NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), START));
        var ringing = state(drain(caller));
        if (ringing.ended() || !ringing.choices().isEmpty() || npc.isOnCall()) throw new AssertionError("Call did not ring before answering");
        int events = de.artemis.cyberneticenhancements.common.journal.PlayerJournal.get(caller.player).page(0, "calls", "", true).total();
        NpcCallManager.handle(caller.player, new NpcCallRequestPayload(token, npc.getUUID(), START));
        drain(caller);
        if (de.artemis.cyberneticenhancements.common.journal.PlayerJournal.get(caller.player).page(0, "calls", "", true).total() != events)
            throw new AssertionError("Replayed call packets created duplicate journal events");
        NpcCallManager.answerPending(caller.player.server, caller.player.server.overworld().getGameTime() + 200);
        if (!npc.isOnCall()) throw new AssertionError("Answered NPC did not display the phone");
    }

    static NpcCallStatePayload state(List<Object> packets) {
        return packets.stream().filter(NpcCallStatePayload.class::isInstance).map(NpcCallStatePayload.class::cast).findFirst().orElseThrow();
    }

    public static List<Object> drain(CallPlayer player) {
        // Packets sent on the server thread mid-tick stay unflushed until the tick ends; flush so this tick's packets are visible.
        player.channel.flushOutbound();
        player.channel.runPendingTasks();
        List<Object> payloads = new ArrayList<>();
        Object packet;
        while ((packet = player.channel.readOutbound()) != null) {
            if (packet instanceof ClientboundCustomPayloadPacket custom) payloads.add(custom.payload());
        }
        return payloads;
    }

    public static CallPlayer player(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return new CallPlayer(player, channel);
    }

    public record CallPlayer(ServerPlayer player, io.netty.channel.embedded.EmbeddedChannel channel) {}
}
