package de.artemis.cyberneticenhancements.gametest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.*;
import de.artemis.cyberneticenhancements.common.world.ChromiumGeology;
import de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery;
import de.artemis.cyberneticenhancements.common.block.ChromiumOreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChromiumGameTests {
    @GameTest(template = "sandevistan_test", batch = "chromium", timeoutTicks = 80)
    public static void seededDepositsReplaceRockAndHaveRealMiningDrops(GameTestHelper h) {
        var level = h.getLevel();
        var prospect = ChromiumGeology.nearest(level.getSeed(), h.absolutePos(BlockPos.ZERO), 32, 512);
        h.assertTrue(prospect != null, "No seeded prospect found");
        var saved = new java.util.HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        try {
            for (var mutable : BlockPos.betweenClosed(new BlockPos((prospect.getX() >> 4) * 16, Math.max(-60, prospect.getY() - 8), (prospect.getZ() >> 4) * 16),
                    new BlockPos((prospect.getX() >> 4) * 16 + 15, Math.min(16, prospect.getY() + 8), (prospect.getZ() >> 4) * 16 + 15))) {
                var pos = mutable.immutable(); saved.put(pos, level.getBlockState(pos));
                level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 2);
            }
            // Ore generation must preserve the cave opening.
            int caveZ = Math.min((prospect.getZ() >> 4) * 16 + 15, prospect.getZ() + 6);
            for (int x = -6; x <= 6; x++) for (int y = -5; y <= 5; y++) {
                var cave = new BlockPos(prospect.getX() + x, prospect.getY() + y, caveZ);
                if (saved.containsKey(cave)) level.setBlock(cave, Blocks.AIR.defaultBlockState(), 2);
            }
            var neighbor = new BlockPos(((prospect.getX() >> 4) + 1) * 16, prospect.getY(), prospect.getZ());
            var neighborBefore = level.getBlockState(neighbor);
            var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "chromium_deposit"));
            h.assertTrue(feature != null && feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(12), prospect), "Deposit failed to place");
            long ores = saved.keySet().stream().filter(p -> level.getBlockState(p).is(ModBlocks.DEEPSLATE_CHROMIUM_ORE.get())).count();
            h.assertTrue(ores >= 7, "Large vein core was missing ore: " + ores);
            h.assertTrue(saved.keySet().stream().anyMatch(p -> level.getBlockState(p).is(Blocks.TUFF)), "Large vein did not include vanilla tuff host rock");
            h.assertTrue(saved.keySet().stream().filter(p -> level.getBlockState(p).getBlock() instanceof ChromiumOreBlock)
                    .allMatch(p -> level.getBlockState(p).getValue(ChromiumOreBlock.LARGE_DEPOSIT)), "Generated deposit ore lost its membership flag");
            h.assertTrue(level.getBlockState(new BlockPos(prospect.getX(), prospect.getY(), caveZ)).isAir(), "Generation filled the cave");
            h.assertTrue(level.getBlockState(neighbor).equals(neighborBefore), "Feature wrote outside the current chunk");
            var state = ModBlocks.DEEPSLATE_CHROMIUM_ORE.get().defaultBlockState();
            h.assertTrue(!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state)
                    && new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state), "Chromium mining tier is incorrect");
            var drops = Block.getDrops(state, level, prospect, null, null, new ItemStack(Items.IRON_PICKAXE));
            h.assertTrue(drops.size() == 1 && drops.getFirst().is(ModItems.RAW_CHROMIUM.get()), "Mining did not yield raw chromium");
        } finally { saved.forEach((p, state) -> level.setBlock(p, state, 2)); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "chromium_discovery", timeoutTicks = 80)
    public static void discoveryRequiresDirectSightOfDepositOreAndPersistsPerPlayer(GameTestHelper h) {
        var level = h.getLevel();
        var center = ChromiumGeology.nearest(level.getSeed(), h.absolutePos(BlockPos.ZERO), 32, 512);
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "chrome-look"), false);
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        var observer = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "chrome-observer"), cookie.clientInformation());
        var saved = new java.util.HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        try {
            for (var mutable : BlockPos.betweenClosed(center.offset(-2, -1, -4), center.offset(2, 4, 2))) {
                var pos = mutable.immutable(); saved.put(pos, level.getBlockState(pos));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            var ore = ModBlocks.DEEPSLATE_CHROMIUM_ORE.get().defaultBlockState().setValue(ChromiumOreBlock.LARGE_DEPOSIT, true);
            level.setBlock(center, ore, 2);
            // Standing directly over the prospect, looking away, is not a discovery.
            player.setPos(center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5);
            player.setXRot(-80); player.setYRot(0);
            h.assertTrue(!ChromiumDiscovery.discoverLookedAtOre(player), "Standing over the deposit discovered it");
            player.setPos(center.getX() + 0.5, center.getY() + 0.5 - player.getEyeHeight(), center.getZ() - 3.5);
            player.setXRot(0); player.setYRot(0);
            var front = center.north();
            for (var obstruction : java.util.List.of(Blocks.STONE.defaultBlockState(),
                    ModBlocks.CHROMIUM_ORE.get().defaultBlockState())) {
                level.setBlock(front, obstruction, 2);
                h.assertTrue(!ChromiumDiscovery.discoverLookedAtOre(player), "Stone or ordinary ore triggered discovery");
            }
            level.setBlock(front, Blocks.AIR.defaultBlockState(), 2);
            h.assertTrue(ChromiumDiscovery.discoverLookedAtOre(player), "Looking at an exposed large-deposit ore block did not discover it");
            h.assertTrue(!ChromiumDiscovery.discoverLookedAtOre(player), "Repeated hover duplicated discovery");
            level.setBlock(front, ore, 2);
            h.assertTrue(!ChromiumDiscovery.discoverLookedAtOre(player), "Another ore block in the same deposit duplicated its marker");
            var targets = ChromiumDiscovery.navigationTargets(player);
            h.assertTrue(targets.size() == 1 && targets.getFirst().pos().equals(center), "Compass did not point to the deposit center");
            h.assertTrue(ChromiumDiscovery.navigationTargets(observer).isEmpty(), "Discovery leaked to another player");
            ChromiumDiscovery.copyState(player, observer);
            h.assertTrue(ChromiumDiscovery.navigationTargets(observer).equals(targets), "Respawning lost the saved discovery");
            h.assertTrue(de.artemis.cyberneticenhancements.common.quest.QuestNavigation.capture(player).targets().stream()
                    .anyMatch(target -> target.questId().equals(targets.getFirst().questId()) && target.pos().equals(center)), "Saved discovery was missing from compass synchronization");
            h.assertTrue(ChromiumDiscovery.deleteMarker(player, targets.getFirst().questId()), "Could not delete the saved deposit marker");
            h.assertTrue(ChromiumDiscovery.navigationTargets(player).isEmpty() && !ChromiumDiscovery.discoverLookedAtOre(player),
                    "Deleted deposit marker remained saved or was immediately rediscovered");
            h.assertTrue(ChromiumDiscovery.navigationTargets(observer).equals(targets), "Deleting a marker affected another player's discoveries");
            h.assertTrue(!ChromiumDiscovery.deleteMarker(player, targets.getFirst().questId()), "Deletion accepted a marker the player no longer owned");
        } finally {
            saved.forEach((pos, state) -> level.setBlock(pos, state, 2));
            player.server.getPlayerList().remove(player); channel.finishAndReleaseAll();
        }
        h.succeed();
    }
}
