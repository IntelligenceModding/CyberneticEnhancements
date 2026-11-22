package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The terminal the player picked on the travel map. */
public record FastTravelRequestPayload(String dimension, BlockPos target) implements CustomPacketPayload {
    public static final Type<FastTravelRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "fast_travel_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FastTravelRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FastTravelRequestPayload::dimension,
            BlockPos.STREAM_CODEC, FastTravelRequestPayload::target,
            FastTravelRequestPayload::new);

    @Override public Type<FastTravelRequestPayload> type() { return TYPE; }
}
