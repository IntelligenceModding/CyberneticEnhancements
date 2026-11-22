package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A player dissolving into, or materialising out of, a fast travel hologram. */
public record FastTravelEffectPayload(int entityId, long started, boolean departing) implements CustomPacketPayload {
    public static final Type<FastTravelEffectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "fast_travel_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FastTravelEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FastTravelEffectPayload::entityId,
            ByteBufCodecs.VAR_LONG, FastTravelEffectPayload::started,
            ByteBufCodecs.BOOL, FastTravelEffectPayload::departing,
            FastTravelEffectPayload::new);

    @Override public Type<FastTravelEffectPayload> type() { return TYPE; }
}
