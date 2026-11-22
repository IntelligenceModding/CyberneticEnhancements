package de.artemis.cyberneticenhancements.common.origin;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The player's lifepath, or nothing yet, which opens the choice. */
public record OriginStatePayload(String id) implements CustomPacketPayload {
    public static final Type<OriginStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "origin_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OriginStatePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> b.writeUtf(p.id, 32), b -> new OriginStatePayload(b.readUtf(32)));
    @Override public Type<OriginStatePayload> type() { return TYPE; }
}
