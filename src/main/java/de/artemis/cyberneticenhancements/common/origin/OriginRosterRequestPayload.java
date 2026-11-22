package de.artemis.cyberneticenhancements.common.origin;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Asks for one page of the players who hold a lifepath. */
public record OriginRosterRequestPayload(String origin, int page) implements CustomPacketPayload {
    public static final Type<OriginRosterRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "origin_roster_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OriginRosterRequestPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeUtf(p.origin, 32); b.writeVarInt(p.page); }, b -> new OriginRosterRequestPayload(b.readUtf(32), b.readVarInt()));
    @Override public Type<OriginRosterRequestPayload> type() { return TYPE; }
}
