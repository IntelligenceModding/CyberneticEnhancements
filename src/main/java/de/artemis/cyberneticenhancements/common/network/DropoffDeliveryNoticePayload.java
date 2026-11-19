package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DropoffDeliveryNoticePayload() implements CustomPacketPayload {
    public static final Type<DropoffDeliveryNoticePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "dropoff_delivery_notice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DropoffDeliveryNoticePayload> STREAM_CODEC = CustomPacketPayload.codec((payload, buffer) -> {}, buffer -> new DropoffDeliveryNoticePayload());

    @Override
    public Type<DropoffDeliveryNoticePayload> type() { return TYPE; }
}
