package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The world encounters whose leaders have stopped dealing with this player: the positions barred for good, and pairs of
 * (position, ticks left) for the ones refusing only for a while after a failed quest.
 */
public record BarredSitesPayload(long[] sites, long[] lockouts) implements CustomPacketPayload {
    public static final Type<BarredSitesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "barred_sites"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BarredSitesPayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buffer) -> { buffer.writeLongArray(payload.sites()); buffer.writeLongArray(payload.lockouts()); },
                    buffer -> new BarredSitesPayload(buffer.readLongArray(), buffer.readLongArray()));

    @Override
    public Type<BarredSitesPayload> type() { return TYPE; }
}
