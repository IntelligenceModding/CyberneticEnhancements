package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestRecoveryGameTests {
    @GameTest(template = "sandevistan_test", batch = "quest_recovery_dead", timeoutTicks = 60)
    public static void confirmedIssuerDeathCancelsReadyHandoversWithoutPenaltyOrPayout(GameTestHelper h) {
        var owner = player(h, "recovery-owner");
        var observer = player(h, "recovery-observer");
        var p = owner.player;
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        var npc = ModEntityTypes.NETRUNNER.get().create(level);
        npc.setPos(p.position().add(0, 0, 2));
        npc.setNoAi(true);
        level.addFreshEntity(npc);
        npc.ensureIdentity();
        npc.markMet(p);
        npc.raiseTrust(p, 30);
        var q = quest(h, npc.getUUID(), QuestType.EXTRACTION, pos);
        try {
            PlayerQuestManager.ensureExtractionDrop(level, q);
            var crate = (IronQuestCrateBlockEntity) level.getBlockEntity(pos);
            var shard = crate.getItem(0).copy();
            p.containerMenu.setCarried(shard);
            crate.setItem(1, new ItemStack(Items.DIAMOND, 3));
            q.setDestinationReached(true);
            q.setStatus(QuestStatus.READY);
            seed(p, q);
            int trust = npc.trustPoints(p), experience = p.totalExperience;
            PlayerEurodollarManager.setBalance(p, 123);
            drain(owner); drain(observer);
            FixerIdentitySavedData.get(level).markDeceased(npc.getUUID());
            ticks(p, 1);
            var cancelled = history(p, q);
            h.assertTrue(PlayerQuestManager.activeContracts(p).isEmpty() && cancelled.status() == QuestStatus.CANCELLED, "Dead issuer stranded a ready physical handover");
            h.assertTrue(npc.trustPoints(p) == trust && p.totalExperience == experience && PlayerEurodollarManager.balance(p) == 123, "Cancellation charged trust or paid rewards");
            h.assertTrue(p.containerMenu.getCarried().isEmpty() && !QuestItemVersions.get(level).current(shard), "Cancelled quest items survived on the cursor or as old copies");
            h.assertTrue(level.getBlockState(pos).isAir() && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2), item -> item.getItem().is(Items.DIAMOND)).stream().mapToInt(item -> item.getItem().getCount()).sum() == 3, "Cleanup destroyed unrelated stored items");
            h.assertTrue(QuestNavigation.capture(p).targets().isEmpty(), "Cancelled quest kept its compass marker");
            h.assertTrue(QuestContract.fromTag(cancelled.toTag()).objectiveLine().getString().equals(cancelled.objectiveLine().getString()), "Cancellation reason did not survive saving");
            h.assertTrue(drain(owner).stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket).count() == 1, "Cancellation feedback was missing or duplicated");
            h.assertTrue(drain(observer).stream().noneMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket || packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket), "Cancellation feedback leaked to another player");
            ticks(p, 40);
            h.assertTrue(drain(owner).stream().noneMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket), "Cancelled contract notified again");
        } finally {
            PlayerQuestManager.clearAllQuestData(p);
            npc.discard();
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).forEach(net.minecraft.world.entity.Entity::discard);
            p.server.getPlayerList().remove(p);
            observer.player.server.getPlayerList().remove(observer.player);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_recovery_sites", timeoutTicks = 80)
    public static void safeMissingSitesRecoverAndBlockedSitesCancelWithoutOverwritingStorage(GameTestHelper h) {
        var owner = player(h, "site-recovery");
        var p = owner.player;
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        var q = quest(h, UUID.randomUUID(), QuestType.DELIVERY, pos);
        try {
            seed(p, q);
            ticks(p, 1);
            var original = (IronQuestCrateBlockEntity) level.getBlockEntity(pos);
            h.assertTrue(original != null && original.questId().equals(q.id()), "Initial quest container was not bound");
            level.removeBlock(pos, false);
            ticks(p, 1);
            h.assertTrue(level.getBlockEntity(pos) instanceof IronQuestCrateBlockEntity restored && restored.questId().equals(q.id()), "Safe destroyed container was not restored");
            PlayerQuestManager.clearAllQuestData(p);
            q = quest(h, UUID.randomUUID(), QuestType.BREACH, pos);
            seed(p, q);
            ticks(p, 1);
            h.assertTrue(level.getBlockEntity(pos) instanceof de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity, "Safe breach site was not created");
            level.removeBlock(pos, false);
            ticks(p, 1);
            h.assertTrue(level.getBlockEntity(pos) instanceof de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity restored && restored.isBoundToQuest(p.getUUID(), q.id()), "Unstarted cache was not safely restored");
            PlayerQuestManager.clearAllQuestData(p);
            h.setBlock(new BlockPos(4, 2, 4), Blocks.CHEST);
            var storage = (net.minecraft.world.Container) level.getBlockEntity(pos);
            storage.setItem(0, new ItemStack(Items.DIAMOND, 4));
            q = quest(h, UUID.randomUUID(), QuestType.BREACH, pos);
            seed(p, q);
            ticks(p, 320);
            p.setPos(Vec3.atCenterOf(pos.offset(200, 0, 0)));
            ticks(p, 40);
            p.setPos(Vec3.atCenterOf(pos.offset(-2, 0, 0)));
            ticks(p, 320);
            h.assertTrue(!PlayerQuestManager.activeContracts(p).isEmpty(), "Time spent away counted as a blocked-site deadline");
            ticks(p, 340);
            h.assertTrue(PlayerQuestManager.activeContracts(p).isEmpty() && history(p, q).status() == QuestStatus.CANCELLED, "Permanently blocked site did not cancel after its grace period");
            h.assertTrue(level.getBlockState(pos).is(Blocks.CHEST) && storage.getItem(0).getCount() == 4, "Recovery or cancellation overwrote unrelated storage");
            h.assertTrue(p.totalExperience == 0 && PlayerEurodollarManager.balance(p) == 0, "Blocked-site cancellation paid rewards");
        } finally {
            PlayerQuestManager.clearAllQuestData(p);
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container storage) storage.clearContent();
            level.removeBlock(pos, false);
            p.server.getPlayerList().remove(p);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "quest_recovery_unloaded", timeoutTicks = 80)
    public static void unloadedContactsAndSitesRemainValidButRemovedDimensionsCancelSafely(GameTestHelper h) {
        var owner = player(h, "unloaded-recovery");
        var p = owner.player;
        var level = h.getLevel();
        var far = h.absolutePos(new BlockPos(100000, 2, 100000));
        var q = quest(h, UUID.randomUUID(), QuestType.DELIVERY, far);
        try {
            h.assertTrue(!level.hasChunkAt(far), "Test site was already loaded");
            seed(p, q);
            ticks(p, 680);
            h.assertTrue(PlayerQuestManager.activeContracts(p).size() == 1 && !level.hasChunkAt(far), "Unloaded issuer/site was cancelled or force-loaded");
            q.setTargetLocation("Invalid Dimension!", far, 8);
            seed(p, q);
            ticks(p, 1);
            h.assertTrue(PlayerQuestManager.activeContracts(p).isEmpty() && history(p, q).status() == QuestStatus.CANCELLED, "Invalid dimension crashed or stranded a contract");
            var snapshot = QuestItemVersions.load(QuestItemVersions.get(level).save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            h.assertTrue(snapshot.retired(q.id()), "Artifact retirement did not persist");
        } finally {
            PlayerQuestManager.clearAllQuestData(p);
            p.server.getPlayerList().remove(p);
        }
        h.succeed();
    }

    private static QuestContract quest(GameTestHelper h, UUID issuer, QuestType type, BlockPos pos) {
        return new QuestContract(UUID.randomUUID().toString(), issuer, "Recovery contact", "netrunner", type, 1, QuestStatus.ACTIVE,
                0, 1, 0, false, h.getLevel().dimension().location().toString(), pos, 8, 100, 2, 0, 0, 0, 0);
    }

    @GameTest(template = "sandevistan_test", batch = "quest_recovery_ownership", timeoutTicks = 80)
    public static void foreignStorageAndCachesArePreservedAndRetiredContainersCleanUpOnReturn(GameTestHelper h) {
        var owner = player(h, "site-ownership");
        var p = owner.player;
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        try {
            h.setBlock(new BlockPos(4, 2, 4), Blocks.CHEST);
            var chest = (net.minecraft.world.Container) level.getBlockEntity(pos);
            chest.setItem(0, new ItemStack(Items.DIAMOND, 2));
            var q = quest(h, UUID.randomUUID(), QuestType.DELIVERY, pos);
            seed(p, q);
            ticks(p, 680);
            h.assertTrue(history(p, q).status() == QuestStatus.CANCELLED && level.getBlockState(pos).is(Blocks.CHEST)
                    && chest.getItem(0).getCount() == 2, "A new delivery commandeered unrelated storage as a legacy site");
            chest.clearContent();
            level.removeBlock(pos, false);
            level.setBlock(pos, de.artemis.cyberneticenhancements.common.registry.ModBlocks.UNCOMMON_RELIC_CACHE.get().defaultBlockState(), 3);
            var foreign = (de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity) level.getBlockEntity(pos);
            UUID foreignOwner = UUID.randomUUID();
            foreign.bindQuest(foreignOwner, "another-contract");
            q = quest(h, UUID.randomUUID(), QuestType.BREACH, pos);
            seed(p, q);
            ticks(p, 680);
            h.assertTrue(history(p, q).status() == QuestStatus.CANCELLED && level.getBlockEntity(pos) == foreign
                    && foreign.isBoundToQuest(foreignOwner, "another-contract"), "Recovery changed or deleted another player's cache");
            level.removeBlock(pos, false);
            q = quest(h, UUID.randomUUID(), QuestType.DELIVERY, pos);
            PlayerQuestManager.ensureCourierStash(level, q);
            var crate = (IronQuestCrateBlockEntity) level.getBlockEntity(pos);
            crate.setItem(0, new ItemStack(Items.DIAMOND, 2));
            var saved = crate.saveWithFullMetadata(level.registryAccess());
            crate.loadWithComponents(saved, level.registryAccess());
            h.assertTrue(crate.questId().equals(q.id()), "Container ownership did not persist");
            QuestItemVersions.get(level).retire(q.id());
            h.assertTrue(QuestRecovery.cleanupRetiredSite(level, pos) && level.getBlockState(pos).isAir(), "A returned retired site was not cleaned up");
        } finally {
            PlayerQuestManager.clearAllQuestData(p);
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container storage) storage.clearContent();
            level.removeBlock(pos, false);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).forEach(net.minecraft.world.entity.Entity::discard);
            p.server.getPlayerList().remove(p);
        }
        h.succeed();
    }

    private static void seed(ServerPlayer p, QuestContract q) {
        var root = new CompoundTag();
        var boards = new CompoundTag();
        var board = new CompoundTag();
        board.put("Active", q.toTag());
        board.put("Offers", new ListTag());
        board.putLong("RefreshDay", p.level().getDayTime() / 24000);
        boards.put(q.issuerId().toString(), board);
        root.put("IssuerBoards", boards);
        p.getPersistentData().put("cyberneticenhancements.quest_state", root);
    }

    private static QuestContract history(ServerPlayer p, QuestContract q) {
        return QuestContract.fromTag(p.getPersistentData().getCompound("cyberneticenhancements.quest_state").getCompound("IssuerBoards")
                .getCompound(q.issuerId().toString()).getList("History", Tag.TAG_COMPOUND).getCompound(0));
    }

    private static void ticks(ServerPlayer p, int count) {
        for (int i = 0; i < count; i++) { p.tickCount++; PlayerQuestManager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p)); }
    }

    private record TestPlayer(ServerPlayer player, io.netty.channel.embedded.EmbeddedChannel channel) {}
    private static TestPlayer player(GameTestHelper h, String name) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), name), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        p.server.getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        return new TestPlayer(p, channel);
    }

    private static java.util.List<Object> drain(TestPlayer p) {
        p.channel.runPendingTasks();
        var packets = new java.util.ArrayList<Object>();
        Object packet;
        while ((packet = p.channel.readOutbound()) != null) packets.add(packet);
        return packets;
    }
}
