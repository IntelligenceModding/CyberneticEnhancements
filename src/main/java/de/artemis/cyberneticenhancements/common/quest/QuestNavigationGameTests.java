package de.artemis.cyberneticenhancements.common.quest;

import com.mojang.authlib.GameProfile;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload;
import de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestNavigationGameTests {
    private QuestNavigationGameTests() {
    }

    @GameTest(template = "sandevistan_test", batch = "quest_navigation", timeoutTicks = 40)
    public static void navigationFollowsSavedQuestTargetsAndOnlySynchronizesToTheirOwner(GameTestHelper h) {
        var owner = player(h, "nav-owner");
        var observer = player(h, "nav-other");
        try {
            var quests = new ArrayList<QuestContract>();
            for (var type : QuestType.values()) quests.add(contract(h, type,
                    type == QuestType.RECOVERY ? null : h.absolutePos(new BlockPos(type.ordinal() + 1, 2, 2))));
            store(owner.player, quests);
            var payload = QuestNavigation.capture(owner.player);
            h.assertTrue(payload.targets().size() == QuestType.values().length - 1, "Not every site-based quest received HUD guidance");
            h.assertTrue(QuestNavigation.capture(observer.player).targets().isEmpty(), "Another player received quest coordinates");
            var investigation = quests.stream().filter(quest -> quest.type() == QuestType.INVESTIGATION).findFirst().orElseThrow();
            var next = investigation.targetPos().offset(24, -12, 16);
            investigation.setTargetLocation(h.getLevel().dimension().location().toString(), next, 2);
            store(owner.player, quests);
            h.assertTrue(QuestNavigation.capture(owner.player).targets().stream().anyMatch(target -> target.questId().equals(investigation.id()) && target.pos().equals(next)),
                    "Investigation navigation did not follow the next signal");
            var identities = FixerIdentitySavedData.get(h.getLevel());
            identities.getOrAssignIdentity(investigation.issuerId(), "netrunner", h.getLevel().random);
            var home = h.absolutePos(new BlockPos(3, 1, 3));
            identities.updateHome(investigation.issuerId(), h.getLevel().dimension().location().toString(), home);
            investigation.setStatus(QuestStatus.READY);
            store(owner.player, quests);
            h.assertTrue(QuestNavigation.capture(owner.player).targets().stream().anyMatch(target -> target.questId().equals(investigation.id()) && target.turnIn() && target.pos().equals(home)),
                    "Ready quest still pointed to its finished site");
            var ready = QuestNavigation.capture(owner.player);
            h.assertTrue(ready.turnIns().equals(List.of(new QuestNavigationPayload.TurnIn(investigation.id(), investigation.issuerId()))),
                    "Ready quest had no personal issuer hologram marker");
            h.assertTrue(QuestNavigation.capture(observer.player).turnIns().isEmpty(), "Turn-in hologram leaked to another player");
            var readyBuffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                QuestNavigationPayload.STREAM_CODEC.encode(readyBuffer, ready);
                h.assertTrue(ready.equals(QuestNavigationPayload.STREAM_CODEC.decode(readyBuffer)), "Turn-in hologram identities were lost in transit");
            } finally { readyBuffer.release(); }
            investigation.setStatus(QuestStatus.COMPLETED);
            quests.getFirst().setStatus(QuestStatus.ABANDONED);
            store(owner.player, quests);
            payload = QuestNavigation.capture(owner.player);
            h.assertTrue(payload.targets().size() == QuestType.values().length - 3, "Resolved quests retained navigation markers");
            h.assertTrue(payload.turnIns().isEmpty(), "Completed quest retained its NPC hologram");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                QuestNavigationPayload.STREAM_CODEC.encode(buffer, payload);
                h.assertTrue(payload.equals(QuestNavigationPayload.STREAM_CODEC.decode(buffer)), "Quest navigation lost data in transit");
            } finally { buffer.release(); }
            var recovery = quests.stream().filter(quest -> quest.type() == QuestType.RECOVERY).findFirst().orElseThrow();
            var liveIssuer = de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.NETRUNNER.get().create(h.getLevel());
            liveIssuer.setUUID(recovery.issuerId());
            liveIssuer.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(home.above()));
            liveIssuer.setNoAi(true);
            h.getLevel().addFreshEntity(liveIssuer);
            liveIssuer.ensureIdentity();
            liveIssuer.markMet(owner.player);
            try {
                recovery.setStatus(QuestStatus.READY);
                store(owner.player, quests);
                h.assertTrue(QuestNavigation.capture(owner.player).targets().stream().anyMatch(target -> target.questId().equals(recovery.id())
                        && target.turnIn() && target.pos().equals(liveIssuer.blockPosition())), "Loaded issuer without a saved home had no turn-in marker");
            } finally {
                recovery.setStatus(QuestStatus.ACTIVE);
                store(owner.player, quests);
                liveIssuer.discard();
            }
            drain(owner.channel); drain(observer.channel);
            QuestNavigation.reset(owner.player);
            QuestNavigation.sync(owner.player);
            h.assertTrue(drain(owner.channel) == 1 && drain(observer.channel) == 0, "Navigation was not synchronized privately");
            QuestNavigation.sync(owner.player);
            h.assertTrue(drain(owner.channel) == 0, "Unchanged navigation repeatedly sent packets");
            PlayerQuestManager.clearAllQuestData(owner.player);
            h.assertTrue(QuestNavigation.capture(owner.player).targets().isEmpty() && drain(owner.channel) == 1, "Clearing quests did not clear navigation");
        } finally {
            owner.player.server.getPlayerList().remove(owner.player);
            observer.player.server.getPlayerList().remove(observer.player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_navigation_migration", timeoutTicks = 40)
    public static void oldQuestCompassesAreRetiredWithoutRemovingNormalCompassesOrQuestItems(GameTestHelper h) {
        var owner = player(h, "nav-migrate");
        var player = owner.player;
        try {
            var quest = contract(h, QuestType.INVESTIGATION, h.absolutePos(new BlockPos(2, 2, 2)));
            store(player, List.of(quest));
            String[] legacy = {"hidden_stash_compass", "extraction_compass", "investigation_compass", "breach_compass", "hunt_compass"};
            for (int index = 0; index < legacy.length; index++) player.getInventory().setItem(index, tagged(Items.COMPASS, quest.id(), legacy[index]));
            player.getInventory().setItem(5, new ItemStack(Items.COMPASS));
            var cacheCompass = new ItemStack(Items.COMPASS);
            cacheCompass.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Relic Cache Compass"));
            player.getInventory().setItem(6, cacheCompass);
            player.getInventory().setItem(7, tagged(Items.AMETHYST_SHARD, quest.id(), "exfil_shard"));
            player.containerMenu.setCarried(tagged(Items.COMPASS, quest.id(), legacy[0]));
            PlayerQuestManager.sanitizeQuestItems(player);
            h.assertTrue(player.containerMenu.getCarried().isEmpty() && player.getInventory().countItem(Items.COMPASS) == 2
                    && player.getInventory().countItem(Items.AMETHYST_SHARD) == 1, "Migration removed unrelated items or retained quest compasses");
            for (int tick = 0; tick < 100; tick++) {
                player.tickCount++;
                PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            h.assertTrue(player.getInventory().countItem(Items.COMPASS) == 2 && QuestNavigation.capture(player).targets().size() == 1,
                    "Quest compass was recreated or its HUD navigation was lost");
            var dropped = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), player.getX(), player.getY(), player.getZ(), tagged(Items.COMPASS, quest.id(), legacy[2]));
            dropped.setNoPickUpDelay();
            h.getLevel().addFreshEntity(dropped);
            dropped.playerTouch(player);
            h.assertTrue(dropped.isRemoved() && player.getInventory().countItem(Items.COMPASS) == 2, "A dropped legacy quest compass could still be picked up");
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static ItemStack tagged(net.minecraft.world.item.Item item, String quest, String type) {
        var stack = new ItemStack(item);
        var tag = new CompoundTag();
        tag.putString("QuestContractId", quest);
        tag.putString("QuestItemType", type);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @GameTest(template = "sandevistan_test", batch = "quest_signal_survey", timeoutTicks = 40)
    public static void surveyRequiresSeparatedUninterruptedScansThenBreachingTheLocatedSource(GameTestHelper h) {
        var owner = player(h, "signal-survey");
        var player = owner.player;
        var source = h.absolutePos(new BlockPos(5, 2, 5));
        h.setBlock(new BlockPos(5, 1, 5), net.minecraft.world.level.block.Blocks.STONE);
        var origin = source.offset(-40, 0, -40);
        var quest = new QuestContract(UUID.randomUUID().toString(), UUID.randomUUID(), "Signal contact", "netrunner",
                QuestType.INVESTIGATION, 1, QuestStatus.ACTIVE, 0, 3, 0, false,
                h.getLevel().dimension().location().toString(), origin, 2, 100, 1, 0, 0, 0, 0);
        quest.setInvestigation(new InvestigationSurvey(origin, source));
        store(player, List.of(quest));
        try {
            PlayerQuestManager.onRelicCacheBreached(player, source);
            h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().status() == QuestStatus.ACTIVE,
                    "Investigation could complete before triangulation");
            h.assertTrue(QuestNavigation.capture(player).targets().getFirst().pos().equals(origin), "Navigation leaked the hidden source");
            player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin.offset(-50, 0, 0)));
            player.setShiftKeyDown(true);
            surveyTicks(player, 40);
            h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().investigation().count() == 0, "Scan finished too early");
            player.setShiftKeyDown(false);
            surveyTicks(player, 1);
            player.setShiftKeyDown(true);
            surveyTicks(player, 40);
            h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().investigation().count() == 0, "Interrupted scan retained its timer");
            surveyTicks(player, 22);
            h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().investigation().count() == 1, "Stationary scan did not record a bearing");
            surveyTicks(player, 100);
            h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().investigation().count() == 1, "One vantage point supplied multiple bearings");
            player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin.offset(50, 0, -30)));
            surveyTicks(player, 62);
            var restored = QuestContract.fromTag(PlayerQuestManager.activeContracts(player).getFirst().toTag());
            h.assertTrue(restored.investigation().count() == 2, "Survey readings did not survive reloading");
            store(player, List.of(restored));
            player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin.offset(0, 0, 70)));
            surveyTicks(player, 62);
            var located = PlayerQuestManager.activeContracts(player).getFirst();
            h.assertTrue(located.status() == QuestStatus.ACTIVE && !located.isProgressComplete() && located.targetPos().equals(source),
                    "Triangulation completed the quest instead of revealing its final objective");
            h.assertTrue(QuestNavigation.capture(player).targets().getFirst().pos().equals(source), "Located source had no HUD guidance");
            h.assertTrue(h.getLevel().getBlockState(source).is(de.artemis.cyberneticenhancements.common.registry.ModBlocks.UNCOMMON_RELIC_CACHE.get()),
                    "Located source had no secured cache");
            PlayerQuestManager.onRelicCacheBreached(player, source.above());
            h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().status() == QuestStatus.ACTIVE, "Unrelated cache completed the survey");
            PlayerQuestManager.onRelicCacheBreached(player, source);
            var ready = PlayerQuestManager.activeContracts(player).getFirst();
            h.assertTrue(ready.status() == QuestStatus.READY && ready.isProgressComplete(), "Breaching the source did not unlock turn-in");
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static void surveyTicks(ServerPlayer player, int count) {
        for (int tick = 0; tick < count; tick++) {
            player.tickCount++;
            PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        }
    }

    @GameTest(template = "sandevistan_test", batch = "quest_single_breach", timeoutTicks = 40)
    public static void failedBreachCostsTrustCannotRetryAndMovesToFailedHistory(GameTestHelper h) {
        var owner = player(h, "breach-failure");
        var observer = player(h, "breach-observer");
        var player = owner.player;
        var npc = de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.NETRUNNER.get().create(h.getLevel());
        npc.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 2, 1))));
        npc.setNoAi(true);
        h.getLevel().addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(player);
        npc.raiseTrust(player, 30);
        try {
            for (String outcome : List.of("timeout", "close", "destroyed")) {
                var pos = h.absolutePos(new BlockPos(4, 2, 4));
                h.setBlock(new BlockPos(4, 1, 4), net.minecraft.world.level.block.Blocks.STONE);
                var quest = breachContract(h, npc.getUUID(), pos);
                store(player, List.of(quest));
                surveyTicks(player, 1);
                var cache = (de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity) h.getLevel().getBlockEntity(pos);
                h.assertTrue(!cache.ensureHackSession(observer.player), "Another player could consume the contract's attempt");
                controlledPuzzle(h, cache);
                h.assertTrue(cache.ensureHackSession(player), "Could not start the contract cache");
                cache.onHackMenuClosed(player);
                h.assertTrue(PlayerQuestManager.activeContracts(player).size() == 1, "Planning the breach used up the attempt");
                cache.ensureHackSession(player);
                int trust = npc.trustPoints(player);
                cache.trySelectHackCell(player, 0);
                h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().breachAttemptStarted(), "First move did not persist the spent attempt");
                if (outcome.equals("timeout")) {
                    for (int i = 0; i < 10; i++)
                        de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity.tick(h.getLevel(), pos, cache.getBlockState(), cache);
                } else if (outcome.equals("close")) {
                    new de.artemis.cyberneticenhancements.common.menu.RelicCacheHackMenu(12, player.getInventory(), pos).removed(player);
                } else {
                    h.getLevel().removeBlock(pos, false);
                    surveyTicks(player, 1);
                }
                h.assertTrue(PlayerQuestManager.activeContracts(player).isEmpty(), "Failed breach remained active: " + outcome);
                h.assertTrue(npc.trustPoints(player) == trust - 3, "Breach failure did not apply one trust penalty: " + outcome);
                h.assertTrue(PlayerQuestManager.questLog(player).stream().anyMatch(entry -> entry.questId().equals(quest.id()) && entry.statusId().equals("failed")),
                        "Failed breach was missing from contract history");
                h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Failed cache was not removed");
                surveyTicks(player, 2);
                h.assertTrue(h.getLevel().getBlockState(pos).isAir() && npc.trustPoints(player) == trust - 3, "Failed cache respawned or charged trust twice");
            }
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            npc.discard();
            player.server.getPlayerList().remove(player);
            observer.player.server.getPlayerList().remove(observer.player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_breach_cleanup", timeoutTicks = 40)
    public static void successfulQuestCacheDeliversLootThenDisappearsWithoutRespawning(GameTestHelper h) {
        var owner = player(h, "breach-success");
        var player = owner.player;
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        h.setBlock(new BlockPos(4, 1, 4), net.minecraft.world.level.block.Blocks.STONE);
        try {
            store(player, List.of(breachContract(h, UUID.randomUUID(), pos)));
            surveyTicks(player, 1);
            var cache = (de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity) h.getLevel().getBlockEntity(pos);
            controlledPuzzle(h, cache);
            cache.ensureHackSession(player);
            cache.trySelectHackCell(player, 0);
            cache.trySelectHackCell(player, 4);
            h.assertTrue(player.totalExperience == de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.relicCache(
                    ((de.artemis.cyberneticenhancements.common.block.RelicCacheBlock) cache.getBlockState().getBlock()).tier()), "Solving the cache did not grant tiered XP");
            int experience = player.totalExperience;
            cache.trySelectHackCell(player, 4);
            h.assertTrue(player.totalExperience == experience, "Replayed cache input awarded XP twice");
            h.assertTrue(PlayerQuestManager.activeContracts(player).getFirst().status() == QuestStatus.READY && cache.isAnimating(), "Successful breach did not begin payout");
            var saved = cache.saveWithFullMetadata(h.getLevel().registryAccess());
            cache.loadWithComponents(saved, h.getLevel().registryAccess());
            for (int i = 0; i < de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity.ANIMATION_TICKS; i++) {
                surveyTicks(player, 1);
                de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity.tick(h.getLevel(), pos, cache.getBlockState(), cache);
            }
            h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Solved contract cache survived its reward animation");
            h.assertTrue(player.getInventory().items.stream().anyMatch(stack -> !stack.isEmpty()), "Removing the cache lost its loot");
            h.assertTrue(player.totalExperience == experience, "Cache animation or reload awarded XP again");
            surveyTicks(player, 2);
            h.assertTrue(h.getLevel().getBlockState(pos).isAir() && PlayerQuestManager.activeContracts(player).getFirst().status() == QuestStatus.READY,
                    "Solved cache respawned or the completed breach failed");
        } finally {
            PlayerQuestManager.clearAllQuestData(player);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static QuestContract breachContract(GameTestHelper h, UUID issuer, BlockPos pos) {
        return new QuestContract(UUID.randomUUID().toString(), issuer, "Contact", "netrunner", QuestType.BREACH,
                1, QuestStatus.ACTIVE, 0, 1, 0, false, h.getLevel().dimension().location().toString(), pos,
                1, 100, 1, 0, 0, 0, 0);
    }

    private static void controlledPuzzle(GameTestHelper h, de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity cache) {
        var tag = cache.saveWithFullMetadata(h.getLevel().registryAccess());
        tag.putInt("PuzzleVersion", 2);
        tag.putInt("PuzzleDifficulty", 2);
        tag.putInt("PuzzleGridSize", 4);
        tag.putInt("PuzzleTimeLimit", 10);
        tag.putInt("PuzzleTargetCount", 1);
        tag.putIntArray("PuzzleGridTokens", new int[]{0, 1, 2, 1, 3, 4, 5, 2, 0, 1, 2, 3, 4, 5, 1, 2});
        tag.putIntArray("PuzzleTargetLengths", new int[]{2});
        tag.putIntArray("PuzzleTargetTokens", new int[]{0, 3});
        cache.loadWithComponents(tag, h.getLevel().registryAccess());
    }

    private static QuestContract contract(GameTestHelper h, QuestType type, BlockPos pos) {
        return new QuestContract(UUID.randomUUID().toString(), UUID.randomUUID(), "Contact", "netrunner", type,
                1, QuestStatus.ACTIVE, 0, 1, 0, false, h.getLevel().dimension().location().toString(), pos,
                8, 100, 1, 0, 0, 0, 0);
    }

    private static void store(ServerPlayer player, List<QuestContract> quests) {
        var boards = new CompoundTag();
        for (var quest : quests) {
            var board = new CompoundTag();
            board.put("Active", quest.toTag());
            boards.put(quest.issuerId().toString(), board);
        }
        var root = new CompoundTag();
        root.put("IssuerBoards", boards);
        player.getPersistentData().put("cyberneticenhancements.quest_state", root);
    }

    private static int drain(EmbeddedChannel channel) {
        int count = 0;
        Object packet;
        while ((packet = channel.readOutbound()) != null)
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof QuestNavigationPayload) count++;
        return count;
    }

    private static PlayerConnection player(GameTestHelper h, String name) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new PlayerConnection(player, channel);
    }

    private record PlayerConnection(ServerPlayer player, EmbeddedChannel channel) {
    }
}
