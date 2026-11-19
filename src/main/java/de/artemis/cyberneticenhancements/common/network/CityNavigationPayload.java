package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CityNavigationPayload(boolean present, String dimension, BlockPos pos, boolean visited, boolean enabled) implements CustomPacketPayload {
    public static final CityNavigationPayload EMPTY = new CityNavigationPayload(false, "", BlockPos.ZERO, false, false);
    public static final Type<CityNavigationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "city_navigation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CityNavigationPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {
                buffer.writeBoolean(payload.present);
                buffer.writeUtf(payload.dimension);
                buffer.writeBlockPos(payload.pos);
                buffer.writeBoolean(payload.visited);
                buffer.writeBoolean(payload.enabled);
            }, buffer -> new CityNavigationPayload(buffer.readBoolean(), buffer.readUtf(), buffer.readBlockPos(), buffer.readBoolean(), buffer.readBoolean()));

    @Override
    public Type<CityNavigationPayload> type() { return TYPE; }
}
