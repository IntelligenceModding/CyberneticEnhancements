package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CallMedtecPayload() implements CustomPacketPayload {
    public static final Type<CallMedtecPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "call_medtec"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CallMedtecPayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buffer) -> {}, buffer -> new CallMedtecPayload());
    @Override
    public Type<CallMedtecPayload> type() { return TYPE; }
}
