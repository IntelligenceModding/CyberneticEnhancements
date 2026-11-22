package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.HolographicDeployment;
import de.artemis.cyberneticenhancements.common.entity.StreetRipperdocEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StreetRipperdocGameTests {
    private static ServerPlayer player(GameTestHelper h, BlockPos at) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "street-client");
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(Vec3.atBottomCenterOf(at));
        return player;
    }

    private static void flatten(GameTestHelper h, BlockPos origin) {
        for (int x = -2; x <= 14; x++) for (int z = -3; z <= 3; z++) {
            h.getLevel().setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) h.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
    }

    @GameTest(template = "sandevistan_test", batch = "street_ripperdoc", timeoutTicks = 200)
    public static void doctorWalksToTheClientAndKeepsNoContactRecord(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(2, 1, 2));
        flatten(h, origin);
        var player = player(h, origin);
        var doc = ModEntityTypes.STREET_RIPPERDOC.get().create(level);
        doc.moveTo(origin.getX() + 11.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
        level.addFreshEntity(doc);
        doc.serve(player);
        var start = doc.distanceToSqr(player);
        h.runAfterDelay(100, () -> {
            h.assertTrue(doc.isAlive() && !HolographicDeployment.departing(doc), "Doctor left although the client was still waiting");
            h.assertTrue(AbstractCityNpcEntity.archiveContacts(player).isEmpty(), "A street doctor appeared in the contact archive");
            h.assertTrue(!player.getPersistentData().contains("cyberneticenhancements.npc_relations"), "A street doctor wrote relation data");
            h.assertTrue(doc.hasResolvedIdentity(), "Doctor has no look or name, so the renderer would skip it");
            h.assertTrue(doc.distanceToSqr(player) < start, "Doctor did not move toward the client");
            player.server.getPlayerList().remove(player);
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "street_ripperdoc", timeoutTicks = 120)
    public static void doctorLeavesAsAHologramOnceTheWaitIsOver(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(2, 1, 2));
        flatten(h, origin);
        var player = player(h, origin);
        var doc = ModEntityTypes.STREET_RIPPERDOC.get().create(level);
        doc.moveTo(origin.getX() + 8.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
        level.addFreshEntity(doc);
        doc.serve(player);
        // The client logs off: with nobody to walk to, the doctor leaves like one who timed out.
        player.server.getPlayerList().remove(player);
        h.runAfterDelay(30, () -> h.assertTrue(HolographicDeployment.departing(doc), "Doctor did not start leaving"));
        h.runAfterDelay(30 + HolographicDeployment.DURATION + 25, () -> {
            h.assertTrue(doc.isRemoved(), "Doctor was not removed after the departure animation");
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "street_ripperdoc", timeoutTicks = 200)
    public static void triggeredEncountersSpawnNearThePlayer(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(2, 1, 2));
        var player = player(h, origin);
        player.setPos(Vec3.atBottomCenterOf(origin.above(2)));
        int[] count = new int[2];
        try {
            h.assertTrue(StreetEncounters.startRipperdoc(player), "The ripperdoc encounter found no room to spawn");
            h.assertTrue(StreetEncounters.startShakedown(player), "The shakedown found no room to spawn");
        } catch (RuntimeException e) {
            player.server.getPlayerList().remove(player);
            throw e;
        }
        var near = player.getBoundingBox().inflate(40);
        h.runAfterDelay(5, () -> {
            count[0] = (int) level.getEntitiesOfClass(StreetRipperdocEntity.class, near, doc -> player.getUUID().equals(doc.client())).size();
            var gang = level.getEntitiesOfClass(de.artemis.cyberneticenhancements.common.entity.GangMemberEntity.class, near, thug -> player.getUUID().equals(thug.victim()));
            count[1] = gang.size();
            h.assertTrue(count[0] == 1, "Expected one street ripperdoc, found " + count[0]);
            h.assertTrue(count[1] == ShakedownStance.of(player).size(), "Expected a full gang, found " + count[1]);
            gang.forEach(de.artemis.cyberneticenhancements.common.entity.GangMemberEntity::withdraw);
        });
        h.runAfterDelay(100, () -> {
            var gang = level.getEntitiesOfClass(de.artemis.cyberneticenhancements.common.entity.GangMemberEntity.class, near, thug -> player.getUUID().equals(thug.victim()));
            player.server.getPlayerList().remove(player);
            h.assertTrue(gang.stream().allMatch(thug -> HolographicDeployment.departing(thug) || thug.isRemoved()), "The gang did not leave as holograms");
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "street_ripperdoc", timeoutTicks = 60)
    public static void killingAStreetDoctorDoesNotCallMaxTec(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(2, 1, 2));
        flatten(h, origin);
        var player = player(h, origin);
        var doc = ModEntityTypes.STREET_RIPPERDOC.get().create(level);
        doc.moveTo(origin.getX() + 5.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
        level.addFreshEntity(doc);
        doc.serve(player);
        doc.hurt(level.damageSources().playerAttack(player), 1000);
        h.runAfterDelay(10, () -> {
            boolean pursued = MaxTacResponse.get(level).save(new net.minecraft.nbt.CompoundTag(), level.registryAccess())
                    .getList("Pursuits", net.minecraft.nbt.Tag.TAG_COMPOUND).stream()
                    .anyMatch(tag -> ((net.minecraft.nbt.CompoundTag) tag).hasUUID("Offender") && ((net.minecraft.nbt.CompoundTag) tag).getUUID("Offender").equals(player.getUUID()));
            player.server.getPlayerList().remove(player);
            h.assertTrue(!doc.isAlive(), "The doctor survived the hit");
            h.assertTrue(!pursued, "Killing a street doctor sent MaxTec after the player");
            h.succeed();
        });
    }
}
