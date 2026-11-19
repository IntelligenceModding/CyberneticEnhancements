package de.artemis.cyberneticenhancements.common.world;

import com.mojang.authlib.GameProfile;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.BannerWaypointsPayload;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BannerNavigationGameTests {
    private BannerNavigationGameTests() {
    }

    @GameTest(template = "sandevistan_test", batch = "banner_navigation", timeoutTicks = 40)
    public static void bannerLandmarksArePrivatePersistentAndFollowLoadedBannerChanges(GameTestHelper h) {
        var owner = player(h, "banner-owner");
        var other = player(h, "banner-other");
        try {
            var pos = banner(h, "Home", DyeColor.BLUE);
            h.assertTrue(BannerNavigation.toggle(owner, pos).equals("marked"), "Renamed banner was not marked");
            var snapshot = BannerNavigation.capture(owner);
            h.assertTrue(snapshot.waypoints().size() == 1 && snapshot.waypoints().getFirst().name().equals("Home")
                    && snapshot.waypoints().getFirst().color() == (0xFF000000 | DyeColor.BLUE.getTextColor()), "Banner lost its name, color or position");
            h.assertTrue(BannerNavigation.capture(other).waypoints().isEmpty(), "Another player received this player's landmark");
            BannerNavigation.copyState(owner, other);
            h.assertTrue(snapshot.equals(BannerNavigation.capture(other)), "Respawn copy lost banner landmarks");
            h.assertTrue(BannerNavigation.toggle(other, pos).equals("removed") && BannerNavigation.capture(owner).waypoints().size() == 1,
                    "Copied banner state was shared between players");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                BannerWaypointsPayload.STREAM_CODEC.encode(buffer, snapshot);
                h.assertTrue(snapshot.equals(BannerWaypointsPayload.STREAM_CODEC.decode(buffer)), "Banner sync lost waypoint data");
                var deletion = new de.artemis.cyberneticenhancements.common.network.DeleteCompassMarkerPayload(snapshot.waypoints().getFirst().id());
                de.artemis.cyberneticenhancements.common.network.DeleteCompassMarkerPayload.STREAM_CODEC.encode(buffer, deletion);
                h.assertTrue(deletion.equals(de.artemis.cyberneticenhancements.common.network.DeleteCompassMarkerPayload.STREAM_CODEC.decode(buffer)), "Marker deletion lost its identifier in transit");
            } finally { buffer.release(); }
            banner(h, "Workshop", DyeColor.RED);
            tick(owner, 20);
            var updated = BannerNavigation.capture(owner).waypoints().getFirst();
            h.assertTrue(updated.name().equals("Workshop") && updated.color() == (0xFF000000 | DyeColor.RED.getTextColor()), "Loaded banner edits did not update its landmark");
            BannerNavigation.copyState(owner, other);
            h.assertTrue(BannerNavigation.deleteMarker(owner, updated.id()), "Could not delete the saved banner waypoint");
            tick(owner, 20);
            h.assertTrue(BannerNavigation.capture(owner).waypoints().isEmpty() && BannerNavigation.capture(other).waypoints().size() == 1,
                    "Deleting a banner marker was temporary or affected another player");
            h.assertTrue(BannerNavigation.toggle(owner, pos).equals("marked"), "Deleted banner could not be explicitly marked again");
            h.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
            tick(owner, 40);
            h.assertTrue(BannerNavigation.capture(owner).waypoints().isEmpty(), "Destroyed banner retained its landmark");
            banner(h, "Outpost", DyeColor.WHITE);
            BannerNavigation.toggle(owner, pos);
            var root = owner.getPersistentData().getCompound("cyberneticenhancements.banner_waypoints");
            var far = new BlockPos(25000000, 64, 25000000);
            root.getList("Points", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).putLong("Pos", far.asLong());
            h.assertTrue(!h.getLevel().hasChunkAt(far), "Faraway fixture was already loaded");
            tick(owner, 60);
            h.assertTrue(BannerNavigation.capture(owner).waypoints().size() == 1 && !h.getLevel().hasChunkAt(far), "Unloaded banner was forgotten or its chunk was force-loaded");
        } finally {
            owner.server.getPlayerList().remove(owner);
            other.server.getPlayerList().remove(other);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "banner_map_navigation", timeoutTicks = 40)
    public static void mapBannerTogglesAndEmptyHandInteractionKeepTheCompassInSync(GameTestHelper h) {
        var player = player(h, "banner-map");
        try {
            var pos = banner(h, "Base", DyeColor.GREEN);
            var map = MapItem.create(h.getLevel(), pos.getX(), pos.getZ(), (byte) 0, true, true);
            var data = MapItem.getSavedData(map, h.getLevel());
            h.assertTrue(data != null, "Map data was not created");
            var centered = data.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
            centered.putInt("xCenter", pos.getX());
            centered.putInt("zCenter", pos.getZ());
            data = net.minecraft.world.level.saveddata.maps.MapItemSavedData.load(centered, h.getLevel().registryAccess());
            h.getLevel().setMapData(map.get(DataComponents.MAP_ID), data);
            h.assertTrue(data != null && data.toggleBanner(h.getLevel(), pos), "Vanilla map could not mark the banner");
            player.getInventory().setItem(0, map);
            tick(player, 20);
            h.assertTrue(BannerNavigation.capture(player).waypoints().size() == 1, "Existing named map banner was not imported");
            var click = click(player, pos);
            BannerNavigation.onRightClickBlock(click);
            h.assertTrue(!click.isCanceled(), "Compass navigation blocked vanilla map interaction");
            data.toggleBanner(h.getLevel(), pos);
            tick(player, 21);
            h.assertTrue(BannerNavigation.capture(player).waypoints().isEmpty(), "Removing a map banner did not remove its compass landmark");
            BannerNavigation.onRightClickBlock(click(player, pos));
            data.toggleBanner(h.getLevel(), pos);
            tick(player, 22);
            h.assertTrue(BannerNavigation.capture(player).waypoints().size() == 1, "Marking a map banner did not add its compass landmark");
            player.getInventory().setItem(0, ItemStack.EMPTY);
            tick(player, 40);
            h.assertTrue(BannerNavigation.capture(player).waypoints().size() == 1, "Landmark required carrying the map");
            click = click(player, pos);
            BannerNavigation.onRightClickBlock(click);
            tick(player, 60);
            h.assertTrue(click.isCanceled() && BannerNavigation.capture(player).waypoints().isEmpty(), "Empty-hand interaction did not unmark the banner");
            BannerNavigation.onRightClickBlock(click(player, pos));
            h.assertTrue(BannerNavigation.capture(player).waypoints().size() == 1, "Empty-hand interaction did not mark the banner");
            h.setBlock(new BlockPos(2, 1, 2), Blocks.WHITE_BANNER);
            tick(player, 80);
            h.assertTrue(BannerNavigation.capture(player).waypoints().isEmpty() && BannerNavigation.toggle(player, pos).equals("invalid"), "Unnamed banner was retained or could be marked");
        } finally {
            player.server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    private static PlayerInteractEvent.RightClickBlock click(ServerPlayer player, BlockPos pos) {
        return new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void tick(ServerPlayer player, int tick) {
        player.tickCount = tick;
        BannerNavigation.onPlayerTick(new PlayerTickEvent.Post(player));
    }

    private static BlockPos banner(GameTestHelper h, String name, DyeColor color) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative.below(), Blocks.STONE);
        h.setBlock(relative, net.minecraft.world.level.block.BannerBlock.byColor(color));
        var pos = h.absolutePos(relative);
        var banner = (BannerBlockEntity) h.getLevel().getBlockEntity(pos);
        var item = new ItemStack(net.minecraft.world.level.block.BannerBlock.byColor(color));
        item.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name));
        banner.fromItem(item, color);
        banner.setChanged();
        return pos;
    }

    private static ServerPlayer player(GameTestHelper h, String name) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        return player;
    }
}
