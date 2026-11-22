package de.artemis.cyberneticenhancements.common.origin;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OriginChoosePayload(String id) implements CustomPacketPayload {
    public static final Type<OriginChoosePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "origin_choose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OriginChoosePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> b.writeUtf(p.id, 32), b -> new OriginChoosePayload(b.readUtf(32)));
    @Override public Type<OriginChoosePayload> type() { return TYPE; }
}
