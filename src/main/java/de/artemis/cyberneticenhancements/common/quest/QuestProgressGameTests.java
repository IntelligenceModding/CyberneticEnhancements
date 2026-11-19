package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.QuestProgressPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestProgressGameTests {
    @GameTest(template = "sandevistan_test", batch = "urgent_quest_deadline", timeoutTicks = 80)
    public static void urgentDeadlinesPersistExpireAndStopWhenAnObjectiveIsReady(GameTestHelper h) {
        var owner = player(h, "urgent-deadline");
        var player = owner.player;
        try {
            var expired = quest(player, QuestType.RECOVERY);
            var ready = quest(player, QuestType.BREACH);
            var normal = quest(player, QuestType.RECOVERY);
            expired.markUrgent();
            ready.markUrgent();
            expired.initializeUrgentDeadline(100);
            ready.initializeUrgentDeadline(100);
            long deadline = 100 + 20 * 60 * 20;
            h.assertTrue(!QuestContract.fromTag(expired.toTag()).urgentTimeIsUp(deadline - 1)
                    && QuestContract.fromTag(expired.toTag()).urgentTimeIsUp(deadline), "Deadline moved when the contract was saved");
            PlayerQuestManager.markQuestReady(player, ready);
            seed(player, List.of(expired, ready, normal));
            player.getStats().setValue(player, net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME), (int) deadline);
            PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            var logged = PlayerQuestManager.questLog(player);
            h.assertTrue(logged.stream().anyMatch(entry -> entry.questId().equals(expired.id()) && entry.statusId().equals("failed")),
                    "Expired urgent quest did not go to failed history");
            var remaining = PlayerQuestManager.activeContracts(player);
            h.assertTrue(remaining.stream().anyMatch(contract -> contract.id().equals(ready.id()) && contract.isReady())
                    && remaining.stream().anyMatch(contract -> contract.id().equals(normal.id())),
                    "Deadline removed a completed objective or an ordinary quest");
        } finally { cleanup(owner); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_item_compass_link", timeoutTicks = 80)
    public static void compassLinksUseQuestIdentityAndCollectionRequirementsAndSurviveTheNetwork(GameTestHelper h) {
        var first = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD);
        var data = new CompoundTag();
        data.putString("QuestContractId", "first");
        data.putString("QuestItemType", "exfil_shard");
        first.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        h.assertTrue(QuestItemLink.matches(first, "first", "") && !QuestItemLink.matches(first, "second", ""),
                "Identical-looking evidence was linked to the wrong quest");
        h.assertTrue(QuestItemLink.matches(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE), "collection", "minecraft:redstone")
                && !QuestItemLink.matches(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND), "collection", "minecraft:redstone"),
                "Collection marker was not linked to its requested material");
        h.assertTrue(!QuestItemLink.matches(net.minecraft.world.item.ItemStack.EMPTY, "first", ""), "Empty hand matched a quest marker");
        var netherTarget = new de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload.Target(
                "first", "Nether stash", "minecraft:the_nether", BlockPos.ZERO, false);
        var overworldTarget = new de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload.Target(
                "other", "Overworld turn-in", "minecraft:overworld", BlockPos.ZERO, true, "minecraft:amethyst_shard");
        h.assertTrue(QuestItemLink.uniqueTarget(first, List.of(overworldTarget, netherTarget)) == netherTarget,
                "Held shard was incorrectly linked to another quest requiring the same item type");
        h.assertTrue(QuestItemLink.uniqueTarget(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK),
                List.of(overworldTarget, netherTarget)) == null, "Unrelated held item surfaced a destination");
        var snapshot = new de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload(List.of(
                new de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload.Target("collection", "Contact", "minecraft:overworld",
                        BlockPos.ZERO, true, "minecraft:redstone")));
        var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload.STREAM_CODEC.encode(buffer, snapshot);
            h.assertTrue(snapshot.equals(de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload.STREAM_CODEC.decode(buffer)),
                    "Compass item link did not survive network serialization");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_progress", timeoutTicks = 80)
    public static void everyOngoingQuestAppearsAndOnlyCounterStagesShowProgressBars(GameTestHelper h) {
        var owner = player(h, "progress-owner");
        try {
            for (QuestType type : QuestType.values()) {
                var quest = quest(owner.player, type);
                seed(owner.player, List.of(quest));
                var snapshot = QuestProgress.capture(owner.player);
                h.assertTrue(snapshot.objectives().size() == 1,
                        "Unexpected HUD panel for " + type);
                h.assertTrue(snapshot.objectives().getFirst().counter() == (type == QuestType.RECOVERY || type == QuestType.ELIMINATION),
                        "Travel stage has an inappropriate progress bar for " + type);
            }
            var recovery = quest(owner.player, QuestType.RECOVERY);
            seed(owner.player, List.of(recovery));
            var snapshot = QuestProgress.capture(owner.player);
            h.assertTrue(snapshot.objectives().getFirst().current() == 6 && snapshot.objectives().getFirst().required() == 12,
                    "Collection count was not preserved");
            var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                QuestProgressPayload.STREAM_CODEC.encode(buffer, snapshot);
                h.assertTrue(snapshot.equals(QuestProgressPayload.STREAM_CODEC.decode(buffer)), "HUD payload lost localized text or counts");
            } finally { buffer.release(); }
            recovery.setStatus(QuestStatus.READY);
            seed(owner.player, List.of(recovery));
            h.assertTrue(QuestProgress.capture(owner.player).objectives().size() == 1
                    && !QuestProgress.capture(owner.player).objectives().getFirst().counter(), "Ready handover did not show its return instruction");
            h.assertTrue(QuestProgress.capture(owner.player).objectives().getFirst().callTarget() == null, "In-person handover offered a remote call shortcut");
            var remote = quest(owner.player, QuestType.BREACH);
            remote.setStatus(QuestStatus.READY);
            seed(owner.player, List.of(remote));
            var remoteSnapshot = QuestProgress.capture(owner.player);
            h.assertTrue(remoteSnapshot.objectives().getFirst().callTarget() != null
                    && remoteSnapshot.objectives().getFirst().callTarget().issuerId().equals(remote.issuerId()), "Ready remote handover did not identify its issuer");
            var readyBuffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                QuestProgressPayload.STREAM_CODEC.encode(readyBuffer, remoteSnapshot);
                h.assertTrue(remoteSnapshot.equals(QuestProgressPayload.STREAM_CODEC.decode(readyBuffer)), "Direct-call shortcut did not survive network serialization");
            } finally { readyBuffer.release(); }
            recovery.setStatus(QuestStatus.COMPLETED);
            seed(owner.player, List.of(recovery));
            h.assertTrue(QuestProgress.capture(owner.player).objectives().isEmpty(), "Completed quest left a HUD panel visible");
            var all = java.util.stream.IntStream.range(0, 8).mapToObj(index -> quest(owner.player, QuestType.RECOVERY)).toList();
            seed(owner.player, all);
            snapshot = QuestProgress.capture(owner.player);
            h.assertTrue(snapshot.objectives().size() == 8, "HUD still caps the number of quests");
            var multiBuffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                QuestProgressPayload.STREAM_CODEC.encode(multiBuffer, snapshot);
                h.assertTrue(snapshot.equals(QuestProgressPayload.STREAM_CODEC.decode(multiBuffer)), "Multiple quests did not survive network serialization");
            } finally { multiBuffer.release(); }
        } finally { cleanup(owner); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_progress", timeoutTicks = 80)
    public static void nearbyObjectivesTakePriorityWhileDistantAndTravelStagesRemainVisible(GameTestHelper h) {
        var owner = player(h, "progress-area");
        try {
            var player = owner.player;
            var elimination = quest(player, QuestType.ELIMINATION);
            var recovery = quest(player, QuestType.RECOVERY);
            seed(player, List.of(recovery, elimination));
            h.assertTrue(QuestProgress.capture(player).objectives().getFirst().id().equals(elimination.id()), "Nearby combat did not take priority");
            elimination.setTargetLocation(player.level().dimension().location().toString(), player.blockPosition().offset(300, 0, 0), 8);
            seed(player, List.of(elimination));
            h.assertTrue(QuestProgress.capture(player).objectives().size() == 1, "Distant quest disappeared");
            var investigation = quest(player, QuestType.INVESTIGATION);
            var center = player.blockPosition();
            var survey = new InvestigationSurvey(center, center);
            investigation.setInvestigation(survey);
            seed(player, List.of(investigation));
            h.assertTrue(QuestProgress.capture(player).objectives().size() == 1, "Nearby survey counter was missing");
            for (BlockPos reading : List.of(center.offset(100, 0, 0), center.offset(0, 0, 100), center.offset(-100, 0, -70)))
                h.assertTrue(survey.record(reading, 1), "Invalid survey fixture");
            investigation.setProgress(3);
            seed(player, List.of(investigation));
            h.assertTrue(QuestProgress.capture(player).objectives().size() == 1
                    && !QuestProgress.capture(player).objectives().getFirst().counter(), "Located source did not switch to its travel instruction");
            elimination.setTargetLocation("minecraft:the_nether", player.blockPosition(), 8);
            seed(player, List.of(elimination));
            h.assertTrue(QuestProgress.capture(player).objectives().size() == 1, "Quest in another dimension disappeared");
        } finally { cleanup(owner); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_progress", timeoutTicks = 80)
    public static void progressSyncIsPrivateAndSendsOnlyChangedCountersOrClearState(GameTestHelper h) {
        var owner = player(h, "progress-sync");
        var observer = player(h, "progress-observer");
        try {
            var quest = quest(owner.player, QuestType.RECOVERY);
            seed(owner.player, List.of(quest));
            drain(owner); drain(observer);
            QuestProgress.reset(owner.player);
            QuestProgress.sync(owner.player);
            h.assertTrue(drain(owner) == 1 && drain(observer) == 0, "Progress was not private to its owner");
            QuestProgress.sync(owner.player);
            h.assertTrue(drain(owner) == 0, "Unchanged HUD progress flooded packets");
            quest.setProgress(7);
            seed(owner.player, List.of(quest));
            QuestProgress.sync(owner.player);
            h.assertTrue(drain(owner) == 1, "Counter change was not synchronized");
            seed(owner.player, List.of());
            QuestProgress.sync(owner.player);
            h.assertTrue(drain(owner) == 1 && QuestProgress.capture(owner.player).objectives().isEmpty(), "Last quest did not clear the HUD");
            QuestProgress.sync(owner.player);
            h.assertTrue(drain(owner) == 0, "Empty HUD state was repeatedly sent");
        } finally { cleanup(owner); cleanup(observer); }
        h.succeed();
    }

    private static QuestContract quest(ServerPlayer player, QuestType type) {
        return new QuestContract(UUID.randomUUID().toString(), UUID.randomUUID(), "Contact", "techie", type, 1, QuestStatus.ACTIVE,
                0, 12, 6, false, player.level().dimension().location().toString(), player.blockPosition(), 8, 100, 1, 0, 0, 0, 0);
    }
    private static void seed(ServerPlayer player, List<QuestContract> quests) {
        var boards = new CompoundTag();
        for (var quest : quests) { var board = new CompoundTag(); board.put("Active", quest.toTag()); boards.put(quest.issuerId().toString(), board); }
        var root = new CompoundTag(); root.put("IssuerBoards", boards);
        player.getPersistentData().put("cyberneticenhancements.quest_state", root);
    }
    private static int drain(Connection owner) {
        int count = 0; Object packet;
        while ((packet = owner.channel.readOutbound()) != null)
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof QuestProgressPayload) count++;
        return count;
    }
    private static Connection player(GameTestHelper h, String name) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), name), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 2, 3))));
        return new Connection(player, channel);
    }
    private static void cleanup(Connection owner) { PlayerQuestManager.clearAllQuestData(owner.player); QuestProgress.reset(owner.player); owner.player.server.getPlayerList().remove(owner.player); }
    private record Connection(ServerPlayer player, io.netty.channel.embedded.EmbeddedChannel channel) {}
}
