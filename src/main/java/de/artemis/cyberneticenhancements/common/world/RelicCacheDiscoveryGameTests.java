package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import de.artemis.cyberneticenhancements.common.block.RelicCacheTier;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RelicCacheDiscoveryGameTests {
    @GameTest(template = "sandevistan_test", batch = "cache_scanner", timeoutTicks = 30)
    public static void scannerRecognizesEveryTierAndPrioritizesCaches(GameTestHelper h) throws Exception {
        var manager = de.artemis.cyberneticenhancements.common.cyberware.FaceCyberwareManager.class;
        var matcher = manager.getDeclaredMethod("isRelicTarget", net.minecraft.world.level.block.state.BlockState.class);
        matcher.setAccessible(true);
        for (var cache : ModBlocks.allRelicCaches()) {
            h.assertTrue((boolean) matcher.invoke(null, cache.get().defaultBlockState()), "Scanner missed a cache tier");
        }
        var scan = manager.getDeclaredMethod("scanBlocks", net.minecraft.server.level.ServerLevel.class,
                net.minecraft.world.entity.player.Player.class, int.class, int.class, int.class,
                java.util.function.Predicate.class, net.minecraft.core.particles.ParticleOptions.class, boolean.class);
        scan.setAccessible(true);
        var player = player(h);
        try {
            h.setBlock(new BlockPos(1, 1, 2), Blocks.CHEST);
            h.setBlock(new BlockPos(2, 1, 3), Blocks.BARREL);
            h.setBlock(new BlockPos(5, 1, 2), ModBlocks.LEGENDARY_RELIC_CACHE.get());
            var results = (java.util.List<?>) scan.invoke(null, h.getLevel(), player, 4, 0, 1,
                    (java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState>) state ->
                            state.is(Blocks.CHEST) || state.is(Blocks.BARREL) || state.getBlock() instanceof RelicCacheBlock,
                    net.minecraft.core.particles.ParticleTypes.ENCHANT, false);
            h.assertTrue(results.equals(java.util.List.of(h.absolutePos(new BlockPos(5, 1, 2)))), "Ordinary containers crowded the cache out of the scanner results");
        } finally {
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "cache_pyramid", timeoutTicks = 30)
    public static void pyramidBasementProducesExactlyOneRareCache(GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(1, 1, 1));
        var piece = new PyramidFixture(origin);
        java.util.List<BlockPos> chests = new java.util.ArrayList<>();
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            chests.add(piece.chest(10 + direction.getStepX() * 2, -11, 10 + direction.getStepZ() * 2));
        }
        verifyStructurePlacement(h, piece, "desert_pyramid", chests, BuiltInLootTables.DESERT_PYRAMID, RelicCacheTier.RARE);
    }

    @GameTest(template = "sandevistan_test", batch = "cache_jungle_temple", timeoutTicks = 30)
    public static void jungleTempleBasementProducesExactlyOneUncommonCache(GameTestHelper h) {
        var piece = new JungleFixture(h.absolutePos(new BlockPos(1, 1, 1)));
        verifyStructurePlacement(h, piece, "jungle_pyramid", java.util.List.of(piece.chest(8, -3, 3), piece.chest(9, -3, 10)),
                BuiltInLootTables.JUNGLE_TEMPLE, RelicCacheTier.UNCOMMON);
    }

    private static void verifyStructurePlacement(GameTestHelper h,
            net.minecraft.world.level.levelgen.structure.StructurePiece piece, String structureName,
            java.util.List<BlockPos> chests, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot,
            RelicCacheTier expectedTier) {
        var level = h.getLevel();
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(structureName));
        var startPos = new net.minecraft.world.level.ChunkPos(piece.getBoundingBox().getCenter());
        var startChunk = level.getChunk(startPos.x, startPos.z);
        h.assertTrue(structure != null, "Unknown vanilla structure ID: " + structureName);
        var previousStarts = new java.util.HashMap<>(startChunk.getAllStarts());
        var start = new net.minecraft.world.level.levelgen.structure.StructureStart(structure, startPos, 0,
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
        var saved = RelicCacheStructurePlacementSavedData.get(level);
        String key = "minecraft:" + structureName + "|" + startPos.x + "|" + startPos.z;
        saved.forgetPlacement(key);
        java.util.Map<net.minecraft.world.level.chunk.LevelChunk, Boolean> references = new java.util.HashMap<>();
        try {
            startChunk.setStartForStructure(structure, start);
            for (BlockPos pos : chests) {
                h.assertTrue(!piece.getBoundingBox().isInside(pos), "Fixture did not reproduce the vanilla basement bounds");
                level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
                ((ChestBlockEntity) level.getBlockEntity(pos)).setLootTable(loot, 1);
                var chunk = level.getChunkAt(pos);
                references.putIfAbsent(chunk, chunk.getReferencesForStructure(structure).contains(startPos.toLong()));
                chunk.addReferenceForStructure(structure, startPos.toLong());
            }
            for (var chunk : references.keySet()) RelicCacheWorldgen.tryPlaceCaches(level, chunk);
            long caches = chests.stream().filter(pos -> level.getBlockState(pos).getBlock() instanceof RelicCacheBlock cache
                    && cache.tier() == expectedTier).count();
            h.assertTrue(caches == 1, "Vanilla basement loot did not yield exactly one cache");
            h.assertTrue(saved.isPlaced(key), "Structure placement was not persisted");
            for (var chunk : references.keySet()) RelicCacheWorldgen.tryPlaceCaches(level, chunk);
            h.assertTrue(chests.stream().filter(pos -> level.getBlockState(pos).getBlock() instanceof RelicCacheBlock).count() == 1,
                    "Reprocessing a structure created additional caches");
        } finally {
            for (BlockPos pos : chests) {
                var entity = level.getBlockEntity(pos);
                if (entity != null) entity.setRemoved();
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            references.forEach((chunk, existed) -> {
                if (!existed) chunk.getReferencesForStructure(structure).remove(startPos.toLong());
            });
            startChunk.setAllStarts(previousStarts);
            saved.forgetPlacement(key);
        }
        h.succeed();
    }

    private static final class PyramidFixture extends net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece {
        PyramidFixture(BlockPos origin) {
            super(net.minecraft.util.RandomSource.create(1), origin.getX(), origin.getZ());
            move(origin.getX() - getBoundingBox().minX(), origin.getY() + 11 - getBoundingBox().minY(), origin.getZ() - getBoundingBox().minZ());
        }

        BlockPos chest(int x, int y, int z) {
            return getWorldPos(x, y, z).immutable();
        }
    }

    private static final class JungleFixture extends net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece {
        JungleFixture(BlockPos origin) {
            super(net.minecraft.util.RandomSource.create(1), origin.getX(), origin.getZ());
            move(origin.getX() - getBoundingBox().minX(), origin.getY() + 3 - getBoundingBox().minY(), origin.getZ() - getBoundingBox().minZ());
        }

        BlockPos chest(int x, int y, int z) {
            return getWorldPos(x, y, z).immutable();
        }
    }
    @GameTest(template = "sandevistan_test", batch = "cache_compass_delivery", timeoutTicks = 30)
    public static void fullInventoryDropsTheTunedCompassWithoutReplacingItems(GameTestHelper h) {
        var player = player(h);
        BlockPos target = h.absolutePos(new BlockPos(5, 1, 2));
        try {
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            h.assertTrue(RelicCacheIntel.giveCompass(player, target), "Full inventory prevented delivery");
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                h.assertTrue(stack.is(Items.COBBLESTONE) && stack.getCount() == 64, "Compass delivery replaced an inventory item");
            }
            var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    player.getBoundingBox().inflate(3), item -> item.getItem().is(Items.COMPASS));
            h.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "Compass was lost or duplicated when the inventory was full");
            var tracker = drops.getFirst().getItem().get(DataComponents.LODESTONE_TRACKER);
            h.assertTrue(tracker != null && tracker.target().orElseThrow().pos().equals(target), "Dropped compass lost its target");
            drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
        } finally {
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "cache_compass_arrival", timeoutTicks = 30)
    public static void clickingCacheRemovesOnlyItsIssuedCompassesBeforeBreachCompletion(GameTestHelper h) {
        var player = player(h);
        BlockPos relative = new BlockPos(3, 1, 2);
        BlockPos target = h.absolutePos(relative);
        BlockPos other = h.absolutePos(new BlockPos(5, 1, 2));
        try {
            h.setBlock(relative, ModBlocks.RARE_RELIC_CACHE.get());
            RelicCacheIntel.giveCompass(player, target);
            ItemStack issued = player.getInventory().getItem(0).copy();
            issued.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("My renamed lead"));
            player.getInventory().setItem(40, issued.copy());
            RelicCacheIntel.giveCompass(player, other);
            ItemStack otherDimension = issued.copy();
            otherDimension.set(DataComponents.LODESTONE_TRACKER, new net.minecraft.world.item.component.LodestoneTracker(
                    java.util.Optional.of(net.minecraft.core.GlobalPos.of(net.minecraft.world.level.Level.NETHER, target)), false));
            player.getInventory().setItem(2, otherDimension);
            ItemStack personal = new ItemStack(Items.COMPASS);
            personal.set(DataComponents.LODESTONE_TRACKER, new net.minecraft.world.item.component.LodestoneTracker(
                    java.util.Optional.of(net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), target)), false));
            player.getInventory().setItem(3, personal);
            player.getInventory().setItem(4, new ItemStack(Items.COMPASS));
            ItemStack legacy = player.getInventory().getItem(0).copy();
            legacy.remove(DataComponents.CUSTOM_DATA);
            player.getInventory().setItem(5, legacy);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(target),
                    net.minecraft.core.Direction.UP, target, false);
            h.getLevel().getBlockState(target).useWithoutItem(h.getLevel(), player, hit);
            h.assertTrue(player.getInventory().getItem(0).isEmpty() && player.getInventory().getItem(40).isEmpty()
                    && player.getInventory().getItem(5).isEmpty(), "Clicking the cache left a matching issued compass in inventory or offhand");
            h.assertTrue(player.getInventory().getItem(1).is(Items.COMPASS), "Compass for a different cache was removed");
            h.assertTrue(player.getInventory().getItem(2).is(Items.COMPASS), "Compass for another dimension was removed");
            h.assertTrue(player.getInventory().getItem(3).is(Items.COMPASS) && player.getInventory().getItem(4).is(Items.COMPASS),
                    "Personal or ordinary compass was removed");
            h.assertTrue(!RelicCacheBlock.hasPlayerOpened(player, h.getLevel(), target), "Fixture completed the breach before checking compass removal");
        } finally {
            player.closeContainer();
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "cache-test");
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return player;
    }

    @GameTest(template = "sandevistan_test", batch = "cache_discovery", timeoutTicks = 30)
    public static void retrofitProtectsPlayerStorageAndOpenedLoot(GameTestHelper h) {
        BlockPos relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, Blocks.CHEST);
        BlockPos pos = h.absolutePos(relative);
        var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(!RelicCacheWorldgen.isEligibleContainer(chest.getBlockState(), chest), "Player chest was eligible");
        chest.setItem(0, new ItemStack(Items.DIAMOND));
        h.assertTrue(!RelicCacheWorldgen.isEligibleContainer(chest.getBlockState(), chest), "Filled player storage was eligible");
        chest.clearContent();
        chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, 1);
        h.assertTrue(RelicCacheWorldgen.isEligibleContainer(chest.getBlockState(), chest), "Untouched loot container was ineligible");
        h.assertTrue(!RelicCacheWorldgen.isEligibleContainer(chest.getBlockState().setValue(BlockStateProperties.CHEST_TYPE, ChestType.LEFT), chest), "Double chest was eligible");
        chest.unpackLootTable(null);
        h.assertTrue(!RelicCacheWorldgen.isEligibleContainer(chest.getBlockState(), chest), "Opened loot chest was eligible");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "cache_discovery", timeoutTicks = 30)
    public static void intelSkipsClaimedAndDestroyedCachesAndCompassDoesNotDuplicate(GameTestHelper h) {
        var player = player(h);
        var data = RelicCacheStructurePlacementSavedData.get(h.getLevel());
        BlockPos near = h.absolutePos(new BlockPos(3, 1, 2));
        BlockPos far = h.absolutePos(new BlockPos(5, 1, 2));
        BlockPos stale = player.blockPosition();
        try {
            h.setBlock(new BlockPos(3, 1, 2), ModBlocks.RARE_RELIC_CACHE.get());
            h.setBlock(new BlockPos(5, 1, 2), ModBlocks.EPIC_RELIC_CACHE.get());
            h.assertTrue(data.tier(near) == RelicCacheTier.RARE && data.tier(far) == RelicCacheTier.EPIC, "New caches were not registered automatically");
            data.register(near, RelicCacheTier.RARE);
            data.register(far, RelicCacheTier.EPIC);
            data.register(stale, RelicCacheTier.UNCOMMON);
            h.assertTrue(RelicCacheIntel.nearest(player).orElseThrow().equals(near), "Nearest verified cache was not selected");
            h.assertTrue(data.tier(stale) == null, "Destroyed cache was retained");
            RelicCacheBlock.markPlayerOpened(player, h.getLevel(), near);
            h.assertTrue(RelicCacheIntel.nearest(player).orElseThrow().equals(far), "Already claimed cache was offered");
            h.assertTrue(RelicCacheIntel.giveCompass(player, far), "Contact did not give a compass to an empty inventory");
            player.getInventory().setItem(1, new ItemStack(Items.COMPASS, 3));
            h.assertTrue(!RelicCacheIntel.giveCompass(player, far), "Repeated request gave a duplicate compass");
            h.assertTrue(!RelicCacheIntel.giveCompass(player, far), "Repeated request lost the compass target");
            int count = 0;
            int bound = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.is(Items.COMPASS)) continue;
                count += stack.getCount();
                var tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                if (tracker != null) {
                    bound += stack.getCount();
                    h.assertTrue(!tracker.tracked() && tracker.target().orElseThrow().pos().equals(far), "Compass requires a lodestone or points elsewhere");
                }
            }
            h.assertTrue(count == 4 && bound == 1, "Compass was duplicated, consumed or tuned repeatedly");
            CompoundTag saved = data.save(new CompoundTag(), h.getLevel().registryAccess());
            var restored = RelicCacheStructurePlacementSavedData.load(saved, h.getLevel().registryAccess());
            h.assertTrue(restored.tier(far) == RelicCacheTier.EPIC, "Cache index lost its position or tier on reload");
            h.setBlock(new BlockPos(5, 1, 2), Blocks.AIR);
            h.assertTrue(data.tier(far) == null, "Destroyed cache remained in the server index");
        } finally {
            data.remove(near);
            data.remove(far);
            data.remove(stale);
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }
}
