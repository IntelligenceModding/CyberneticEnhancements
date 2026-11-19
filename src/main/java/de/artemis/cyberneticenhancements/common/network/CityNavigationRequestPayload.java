package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CityNavigationRequestPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<CityNavigationRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "city_navigation_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CityNavigationRequestPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeBoolean(payload.enabled), buffer -> new CityNavigationRequestPayload(buffer.readBoolean()));

    @Override
    public Type<CityNavigationRequestPayload> type() { return TYPE; }
}
