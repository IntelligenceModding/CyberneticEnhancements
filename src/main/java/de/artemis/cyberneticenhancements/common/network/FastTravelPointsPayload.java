package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** The fast travel terminals a player has found. */
public record FastTravelPointsPayload(List<Point> points) implements CustomPacketPayload {
    public static final Type<FastTravelPointsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            CyberneticEnhancements.MOD_ID, "fast_travel_points"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FastTravelPointsPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeCollection(payload.points, (out, point) -> {
                out.writeUtf(point.dimension);
                out.writeBlockPos(point.pos);
                out.writeUtf(point.name);
            }), buffer -> new FastTravelPointsPayload(buffer.readList(in -> new Point(in.readUtf(), in.readBlockPos(), in.readUtf()))));

    public FastTravelPointsPayload {
        points = List.copyOf(points);
    }

    @Override
    public Type<FastTravelPointsPayload> type() { return TYPE; }

    public record Point(String dimension, BlockPos pos, String name) {
        public String id() { return dimension + "/" + pos.asLong(); }
    }
}
