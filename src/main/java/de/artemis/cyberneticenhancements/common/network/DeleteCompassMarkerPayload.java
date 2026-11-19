package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteCompassMarkerPayload(String id) implements CustomPacketPayload {
    public static final Type<DeleteCompassMarkerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            CyberneticEnhancements.MOD_ID, "delete_compass_marker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteCompassMarkerPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeUtf(payload.id(), 256), buffer -> new DeleteCompassMarkerPayload(buffer.readUtf(256)));

    @Override public Type<DeleteCompassMarkerPayload> type() { return TYPE; }
}
