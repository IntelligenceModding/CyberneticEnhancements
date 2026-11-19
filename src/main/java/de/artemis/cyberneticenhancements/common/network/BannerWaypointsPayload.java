package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record BannerWaypointsPayload(List<Waypoint> waypoints) implements CustomPacketPayload {
    public static final Type<BannerWaypointsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            CyberneticEnhancements.MOD_ID, "banner_waypoints"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BannerWaypointsPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeCollection(payload.waypoints, (out, waypoint) -> {
                out.writeUtf(waypoint.dimension);
                out.writeBlockPos(waypoint.pos);
                out.writeUtf(waypoint.name);
                out.writeInt(waypoint.color);
            }), buffer -> new BannerWaypointsPayload(buffer.readList(in -> new Waypoint(
                    in.readUtf(), in.readBlockPos(), in.readUtf(), in.readInt()))));

    public BannerWaypointsPayload {
        waypoints = List.copyOf(waypoints);
    }

    @Override
    public Type<BannerWaypointsPayload> type() {
        return TYPE;
    }

    public record Waypoint(String dimension, BlockPos pos, String name, int color) {
        public String id() {
            return dimension + "/" + pos.asLong();
        }
    }
}
