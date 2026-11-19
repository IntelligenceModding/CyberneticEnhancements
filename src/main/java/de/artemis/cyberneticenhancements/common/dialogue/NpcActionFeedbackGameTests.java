package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.quest.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NpcActionFeedbackGameTests {
    @GameTest(template = "sandevistan_test", batch = "quest_action_feedback", timeoutTicks = 30)
    public static void gameplayChatKeepsHoverTextAndGroupsPrivateNotificationSounds(GameTestHelper h) {
        var owner = feedbackPlayer(h, "notice-owner");
        var observer = feedbackPlayer(h, "notice-observer");
        try {
            drain(owner.channel()); drain(observer.channel());
            var hover = new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                    net.minecraft.network.chat.Component.literal("Destination"));
            var coordinate = net.minecraft.network.chat.Component.literal("12, 64, -30").withStyle(style -> style.withHoverEvent(hover));
            var message = net.minecraft.network.chat.Component.translatable("message.cyberneticenhancements.test", coordinate);
            for (int i = 0; i < 2; i++) de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(owner.player(), message);
            var packets = drain(owner.channel());
            h.assertTrue(packets.stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket).count() == 1,
                    "A burst of two chat messages played duplicate sounds");
            int messages = 0;
            for (var packet : packets) if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat) {
                messages++;
                var wrapper = (net.minecraft.network.chat.contents.TranslatableContents) chat.content().getContents();
                h.assertTrue(wrapper.getKey().equals("message.cyberneticenhancements.gameplay"), "Chat bypassed shared formatting");
                var body = (net.minecraft.network.chat.Component) wrapper.getArgs()[0];
                var argument = (net.minecraft.network.chat.Component) ((net.minecraft.network.chat.contents.TranslatableContents) body.getContents()).getArgs()[0];
                h.assertTrue(hover.equals(argument.getStyle().getHoverEvent()), "Shared chat formatting removed hover text");
            }
            h.assertTrue(messages == 2 && drain(observer.channel()).stream().noneMatch(NpcActionFeedbackGameTests::isActionFeedback),
                    "Chat was dropped or notifications leaked to another player");
            h.succeed();
        } finally {
            owner.player().server.getPlayerList().remove(owner.player());
            observer.player().server.getPlayerList().remove(observer.player());
        }
    }

    @GameTest(template = "sandevistan_test", batch = "quest_action_feedback", timeoutTicks = 80)
    public static void acceptanceTurnInsAndAbandonmentGiveOnePrivateFeedbackPerAction(GameTestHelper h) {
        var owner = feedbackPlayer(h, "action-owner");
        var observer = feedbackPlayer(h, "action-observer");
        var player = owner.player();
        var npc = de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 2, 3))));
        npc.setPos(player.position().add(0, 0, 2));
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(player);
        try {
            var offer = feedbackQuest(h, npc, QuestStatus.ACTIVE);
            seedFeedbackBoard(player, npc, offer, false);
            drain(owner.channel()); drain(observer.channel());
            h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, npc, 0) == PlayerQuestManager.Result.OK, "Acceptance failed");
            assertActionFeedback(h, drain(owner.channel()), false);
            h.assertTrue(PlayerQuestManager.acceptIssuerOffer(player, npc, 0) == PlayerQuestManager.Result.HAS_ACTIVE, "Replayed acceptance succeeded");
            h.assertTrue(drain(owner.channel()).stream().noneMatch(NpcActionFeedbackGameTests::isActionFeedback), "Replayed acceptance repeated feedback");
            for (int route = 0; route < 3; route++) {
                var quest = feedbackQuest(h, npc, QuestStatus.READY);
                seedFeedbackBoard(player, npc, quest, true);
                var token = UUID.randomUUID();
                if (route == 2) {
                    var start = new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(token, npc.getUUID(), -1);
                    de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(player, start);
                    de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.answerPending(player.server, player.server.overworld().getGameTime() + 200);
                    de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(player,
                            new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(token, npc.getUUID(), 4));
                }
                drain(owner.channel());
                if (route == 0) {
                    var menu = new de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu(1, player.getInventory(), npc);
                    player.containerMenu = menu;
                    menu.handleChoice(player, de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_TURN_IN_CONTRACT);
                } else if (route == 1) {
                    h.assertTrue(PlayerQuestManager.turnInRemotely(player, quest.id()) == PlayerQuestManager.TurnInResult.OK, "Archive-style turn-in failed");
                } else {
                    de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.handle(player,
                            new de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload(token, npc.getUUID(), 11));
                }
                assertActionFeedback(h, drain(owner.channel()), true);
                h.assertTrue(PlayerQuestManager.turnInRemotely(player, quest.id()) != PlayerQuestManager.TurnInResult.OK, "Turn-in replay succeeded");
                h.assertTrue(drain(owner.channel()).stream().noneMatch(NpcActionFeedbackGameTests::isActionFeedback), "Turn-in replay repeated completion feedback");
                player.closeContainer();
                de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.endForInteraction(player, npc);
                drain(owner.channel());
            }
            for (boolean byId : new boolean[]{false, true}) {
                var quest = feedbackQuest(h, npc, QuestStatus.ACTIVE);
                seedFeedbackBoard(player, npc, quest, true);
                var result = byId ? PlayerQuestManager.abandonQuestById(player, quest.id()) : PlayerQuestManager.abandonIssuerActive(player, npc);
                h.assertTrue(result == PlayerQuestManager.Result.OK, "Abandonment failed");
                assertActionFeedback(h, drain(owner.channel()), false);
                var history = player.getPersistentData().getCompound("cyberneticenhancements.quest_state").getCompound("IssuerBoards")
                        .getCompound(npc.getStringUUID()).getList("History", net.minecraft.nbt.Tag.TAG_COMPOUND);
                h.assertTrue(history.size() == 1 && QuestContract.fromTag(history.getCompound(0)).status() == QuestStatus.ABANDONED, "Abandonment lost contract history");
            }
            h.assertTrue(drain(observer.channel()).stream().noneMatch(NpcActionFeedbackGameTests::isActionFeedback), "Another player received quest sounds, chat or particles");
        } finally {
            player.closeContainer();
            de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.endForInteraction(player, npc);
            PlayerQuestManager.clearAllQuestData(player);
            npc.discard();
            player.server.getPlayerList().remove(player);
            observer.player().server.getPlayerList().remove(observer.player());
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_action_feedback", timeoutTicks = 80)
    public static void questReadinessAndFailureUseChatWithoutRepeatingAboveTheHotbar(GameTestHelper h) {
        var owner = feedbackPlayer(h, "quest-feedback");
        var player = owner.player();
        var npc = de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        npc.setPos(player.position()); npc.setNoAi(true); h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity(); npc.markMet(player);
        try {
            var pos = h.absolutePos(new BlockPos(2, 1, 2));
            for (boolean failed : new boolean[]{false, true}) {
                var quest = new QuestContract(UUID.randomUUID().toString(), npc.getUUID(), npc.getName().getString(), "netrunner",
                        QuestType.BREACH, 1, QuestStatus.ACTIVE, 0, 1, 0, false,
                        h.getLevel().dimension().location().toString(), pos, 8, 100, 1, 0, 0, 0, 0);
                seedFeedbackBoard(player, npc, quest, true);
                drain(owner.channel());
                if (failed) PlayerQuestManager.onQuestCacheFailed(player, pos);
                else h.assertTrue(PlayerQuestManager.onRelicCacheBreached(player, pos), "Breach did not report quest feedback");
                assertActionFeedback(h, drain(owner.channel()), false);
            }
        } finally {
            PlayerQuestManager.clearAllQuestData(player); npc.discard(); player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_action_feedback", timeoutTicks = 80)
    public static void psychosisCommandsConfirmEachActionInOnlyOneTextChannel(GameTestHelper h) {
        var owner = feedbackPlayer(h, "command-feedback");
        var player = owner.player();
        try {
            var commands = player.server.getCommands().getDispatcher();
            var source = player.createCommandSourceStack().withPermission(2);
            drain(owner.channel());
            commands.execute("cyberneticenhancements cyberpsychosis trigger 5", source);
            assertTextChannels(h, drain(owner.channel()), 0, 1);
            commands.execute("cyberneticenhancements cyberpsychosis clear", source);
            assertTextChannels(h, drain(owner.channel()), 0, 1);
            commands.execute("cyberneticenhancements cyberpsychosis clear", source);
            assertTextChannels(h, drain(owner.channel()), 1, 0);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new AssertionError(exception);
        } finally {
            de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager.clearPsychosis(player);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_action_feedback", timeoutTicks = 80)
    public static void neuralDosesAndStabilizationDoNotRepeatTheirOwnActivationNotice(GameTestHelper h) {
        var owner = feedbackPlayer(h, "dose-feedback");
        var player = owner.player();
        try {
            for (String id : new String[]{"ram_jolt", "immunoblockers", "chrome_suppressant"}) {
                var dose = de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCatalog.get(id);
                drain(owner.channel());
                dose.profile().apply(player);
                de.artemis.cyberneticenhancements.common.consumable.CyberConsumableManager.onConsumed(player, dose);
                assertTextChannels(h, drain(owner.channel()), 0, 1);
            }
            de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager.forcePsychosisEpisode(player, 5);
            drain(owner.channel());
            var dose = de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCatalog.get("immunoblockers");
            dose.profile().apply(player);
            de.artemis.cyberneticenhancements.common.consumable.CyberConsumableManager.onConsumed(player, dose);
            assertTextChannels(h, drain(owner.channel()), 0, 1);
        } finally {
            de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager.clearPsychosis(player);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static void assertTextChannels(GameTestHelper h, java.util.List<Object> packets, int chat, int overlay) {
        long chatCount = packets.stream().filter(p -> p instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket text && !text.overlay()).count();
        long overlayCount = packets.stream().filter(p -> p instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket text && text.overlay()).count();
        h.assertTrue(chatCount == chat && overlayCount == overlay, "Duplicate or missing feedback: chat=" + chatCount + ", hotbar=" + overlayCount);
    }

    private static QuestContract feedbackQuest(GameTestHelper h, de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc, QuestStatus status) {
        return new QuestContract(UUID.randomUUID().toString(), npc.getUUID(), npc.getName().getString(), "netrunner", QuestType.ELIMINATION,
                1, status, 0, 1, status == QuestStatus.READY ? 1 : 0, false, h.getLevel().dimension().location().toString(), null,
                8, 100, 1, 0, 0, 0, 0);
    }

    private static void seedFeedbackBoard(net.minecraft.server.level.ServerPlayer player, de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc, QuestContract quest, boolean active) {
        var root = new net.minecraft.nbt.CompoundTag();
        var boards = new net.minecraft.nbt.CompoundTag();
        var board = new net.minecraft.nbt.CompoundTag();
        var offers = new net.minecraft.nbt.ListTag();
        if (active) board.put("Active", quest.toTag());
        else offers.add(quest.toTag());
        board.put("Offers", offers);
        board.putLong("RefreshDay", player.level().getDayTime() / 24000);
        boards.put(npc.getStringUUID(), board);
        root.put("IssuerBoards", boards);
        player.getPersistentData().put("cyberneticenhancements.quest_state", root);
    }

    private static boolean isActionFeedback(Object packet) {
        return packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket
                || packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket
                || packet instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
    }

    private static void assertActionFeedback(GameTestHelper h, java.util.List<Object> packets, boolean completion) {
        long sounds = packets.stream().filter(p -> p instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket).count();
        long chat = packets.stream().filter(p -> p instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket message && !message.overlay()
                && message.content().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents wrapper
                && wrapper.getKey().equals("message.cyberneticenhancements.gameplay")
                && wrapper.getArgs()[0] instanceof net.minecraft.network.chat.Component body
                && body.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && java.util.Set.of("screen.cyberneticenhancements.call.quest.accepted_chat", "screen.cyberneticenhancements.call.quest.paid_chat",
                        "screen.cyberneticenhancements.call.quest.paid_direct_chat",
                        "message.cyberneticenhancements.quest.breach_ready", "message.cyberneticenhancements.quest.breach_failed",
                        "message.cyberneticenhancements.quest.abandoned").contains(text.getKey())).count();
        long actionBar = packets.stream().filter(p -> p instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket message && message.overlay()).count();
        long particles = packets.stream().filter(p -> p instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket).count();
        h.assertTrue(sounds == 1 && chat == 1 && actionBar == 0 && particles == (completion ? 1 : 0),
                "Action feedback was missing or duplicated: sound=" + sounds + ", chat=" + chat + ", actionbar=" + actionBar + ", particles=" + particles);
    }

    private static FeedbackPlayer feedbackPlayer(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return new FeedbackPlayer(player, channel);
    }

    private static java.util.List<Object> drain(io.netty.channel.embedded.EmbeddedChannel channel) {
        channel.runPendingTasks();
        java.util.List<Object> packets = new java.util.ArrayList<>();
        Object packet;
        while ((packet = channel.readOutbound()) != null) packets.add(packet);
        return packets;
    }

    private record FeedbackPlayer(net.minecraft.server.level.ServerPlayer player, io.netty.channel.embedded.EmbeddedChannel channel) {
    }
}
