package de.artemis.cyberneticenhancements.common.world;

import com.mojang.authlib.GameProfile;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.CityNavigationPayload;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightCityNavigationGameTests {
    private NightCityNavigationGameTests() {}

    @GameTest(template = "sandevistan_test", batch = "city_navigation", timeoutTicks = 40)
    public static void cityMarkerIsPersonalAndDisabledOnlyOnFirstArrivalWhileRemainingSaved(GameTestHelper h) {
        var owner = player(h, "city-owner");
        var other = player(h, "city-other");
        var player = owner.player;
        try {
            BlockPos origin = BlockPos.of(player.getPersistentData().getCompound(NightCityNavigation.TAG).getLong("Origin"));
            player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(25, 2, 25))));
            NightCityNavigation.onLogin(player);
            h.assertTrue(origin.equals(BlockPos.of(player.getPersistentData().getCompound(NightCityNavigation.TAG).getLong("Origin"))),
                    "Moving or reconnecting changed the first-join search origin");
            drain(owner.channel); drain(other.channel);
            var corner = h.absolutePos(new BlockPos(2, 2, 2));
            var bounds = new BoundingBox(corner.getX(), corner.getY(), corner.getZ(), corner.getX() + 8, corner.getY() + 5, corner.getZ() + 8);
            String dimension = h.getLevel().dimension().location().toString();
            NightCityNavigation.mark(player, dimension, bounds);
            var initial = NightCityNavigation.capture(player);
            h.assertTrue(initial.present() && initial.enabled() && !initial.visited() && initial.pos().equals(bounds.getCenter()), "First city marker was not enabled");
            h.assertTrue(drain(owner.channel) == 1 && drain(other.channel) == 0 && !NightCityNavigation.capture(other.player).present(),
                    "First-join city location leaked to another player");
            NightCityNavigation.checkArrival(player);
            h.assertTrue(!NightCityNavigation.capture(player).visited(), "Getting near a city counted as entering it");
            player.setPos(Vec3.atCenterOf(bounds.getCenter().above(100)));
            NightCityNavigation.checkArrival(player);
            h.assertTrue(!NightCityNavigation.capture(player).visited(), "Flying high above a city counted as entering it");
            player.setPos(Vec3.atCenterOf(bounds.getCenter()));
            player.getPersistentData().getCompound(NightCityNavigation.TAG).putString("Dimension", "minecraft:the_nether");
            NightCityNavigation.checkArrival(player);
            h.assertTrue(!NightCityNavigation.capture(player).visited(), "A different dimension counted as arrival");
            player.getPersistentData().getCompound(NightCityNavigation.TAG).putString("Dimension", dimension);
            NightCityNavigation.checkArrival(player);
            var visited = NightCityNavigation.capture(player);
            h.assertTrue(visited.present() && visited.visited() && !visited.enabled() && visited.pos().equals(initial.pos()),
                    "Arrival deleted the marker instead of disabling it");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                CityNavigationPayload.STREAM_CODEC.encode(buffer, visited);
                h.assertTrue(visited.equals(CityNavigationPayload.STREAM_CODEC.decode(buffer)), "City visibility or visit state was lost in transit");
            } finally { buffer.release(); }
            NightCityNavigation.setEnabled(player, true);
            NightCityNavigation.checkArrival(player);
            h.assertTrue(NightCityNavigation.capture(player).enabled(), "Re-enabled city marker was immediately disabled again");
            NightCityNavigation.onLogin(player);
            NightCityNavigation.locate(player);
            NightCityNavigation.mark(player, dimension, bounds.moved(200, 0, 200));
            h.assertTrue(NightCityNavigation.capture(player).enabled() && NightCityNavigation.capture(player).pos().equals(initial.pos()),
                    "Rejoining replaced or reset an already discovered city");
            NightCityNavigation.copyState(player, other.player);
            h.assertTrue(NightCityNavigation.capture(player).equals(NightCityNavigation.capture(other.player)), "Death clone lost city discovery or preferences");
            NightCityNavigation.setEnabled(other.player, false);
            h.assertTrue(NightCityNavigation.capture(player).enabled(), "Another player's setting changed the owner's marker");
            NightCityNavigation.onLogin(other.player);
            h.assertTrue(!NightCityNavigation.capture(other.player).enabled() && NightCityNavigation.capture(other.player).present(),
                    "Disabled city marker was lost or enabled by login");
        } finally {
            NightCityNavigation.reset(player);
            NightCityNavigation.reset(other.player);
            player.server.getPlayerList().remove(player);
            other.player.server.getPlayerList().remove(other.player);
        }
        h.succeed();
    }

    private static int drain(EmbeddedChannel channel) {
        int count = 0;
        Object packet;
        while ((packet = channel.readOutbound()) != null)
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof CityNavigationPayload) count++;
        return count;
    }

    private static Connected player(GameTestHelper h, String name) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new Connected(player, channel);
    }

    private record Connected(ServerPlayer player, EmbeddedChannel channel) {}
}
