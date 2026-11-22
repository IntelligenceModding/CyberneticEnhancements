package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Opens the travel map at the terminal the player used. */
public record FastTravelOpenPayload(BlockPos origin, int cost) implements CustomPacketPayload {
    public static final Type<FastTravelOpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "fast_travel_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FastTravelOpenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FastTravelOpenPayload::origin,
            ByteBufCodecs.VAR_INT, FastTravelOpenPayload::cost,
            FastTravelOpenPayload::new);

    @Override public Type<FastTravelOpenPayload> type() { return TYPE; }
}
