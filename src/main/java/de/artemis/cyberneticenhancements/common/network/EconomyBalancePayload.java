package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EconomyBalancePayload(String json) implements CustomPacketPayload {
    public static final Type<EconomyBalancePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "economy_balance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EconomyBalancePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeUtf(payload.json, 65536), buffer -> new EconomyBalancePayload(buffer.readUtf(65536)));
    @Override public Type<EconomyBalancePayload> type() { return TYPE; }
}
