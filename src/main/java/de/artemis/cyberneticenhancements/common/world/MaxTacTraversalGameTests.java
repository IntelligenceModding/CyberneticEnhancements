package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MaxTacTraversalGameTests {
    @GameTest(template = "sandevistan_test", batch = "maxtac_traversal", timeoutTicks = 100)
    public static void officersPlanAndStepAcrossVillageBarriersButRejectSealedRoutes(GameTestHelper h) {
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(3, 40, 3));
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "maxtac-path");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(Vec3.atBottomCenterOf(center.offset(4, 0, 0)));
        var officer = ModEntityTypes.MAX_TAC.get().create(level);
        officer.setNoAi(true);
        Map<BlockPos, BlockState> original = new HashMap<>();
        java.util.function.BiConsumer<BlockPos, BlockState> put = (pos, state) -> {
            original.putIfAbsent(pos.immutable(), level.getBlockState(pos));
            level.setBlock(pos, state, 3);
        };
        try {
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                put.accept(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 5; y++) put.accept(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            officer.setPos(Vec3.atBottomCenterOf(center.offset(-3, 0, 0)));
            officer.setOnGround(true);
            level.addFreshEntity(officer);
            h.assertTrue(officer.maxUpStep() == 1.5F, "MaxTac step height must be 1.5 blocks");
            h.assertTrue(MaxTacResponse.reachablePath(officer, player) != null, "Open ground was rejected as a spawn route");
            var barriers = java.util.List.of(Blocks.OAK_FENCE.defaultBlockState(), Blocks.COBBLESTONE_WALL.defaultBlockState(),
                    Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));
            for (var barrier : barriers) {
                for (int z = -6; z <= 6; z++) put.accept(center.offset(0, 0, z), barrier);
                officer.getNavigation().stop();
                officer.setPos(Vec3.atBottomCenterOf(center.offset(-3, 0, 0)));
                officer.setOnGround(true);
                var path = MaxTacResponse.reachablePath(officer, player);
                h.assertTrue(path != null, "MaxTac could not plan a route over " + barrier.getBlock());
                boolean climbs = false;
                for (int index = 0; index < path.getNodeCount(); index++) climbs |= path.getNode(index).y > center.getY();
                h.assertTrue(climbs, "Fence route did not climb over the barrier");
                officer.setPos(Vec3.atBottomCenterOf(center.offset(-1, 0, 0)));
                officer.setOnGround(true);
                officer.move(MoverType.SELF, new Vec3(1.3, 0, 0));
                h.assertTrue(officer.getY() >= center.getY() + 1.49 && level.noCollision(officer),
                        "MaxTac could plan a path but could not physically step over " + barrier.getBlock());
            }
            for (int z = -6; z <= 6; z++) for (int y = 0; y < 4; y++)
                put.accept(center.offset(0, y, z), Blocks.STONE.defaultBlockState());
            officer.getNavigation().stop();
            officer.setPos(Vec3.atBottomCenterOf(center.offset(-3, 0, 0)));
            officer.setOnGround(true);
            h.assertTrue(MaxTacResponse.reachablePath(officer, player) == null, "A sealed location was accepted for deployment");
            officer.setPos(Vec3.atBottomCenterOf(center.offset(-1, 0, 0)));
            officer.setOnGround(true);
            officer.move(MoverType.SELF, new Vec3(1.3, 0, 0));
            h.assertTrue(officer.getX() < center.getX() && officer.getY() == center.getY(), "Enhanced stepping let MaxTac cross a tall solid wall");
        } finally {
            officer.discard();
            original.forEach((pos, state) -> level.setBlock(pos, state, 2));
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }
}
