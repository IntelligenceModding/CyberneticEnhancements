package de.artemis.cyberneticenhancements.common.streetcred;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The player's Street Cred points, from which both sides work out the level. */
public record StreetCredPayload(int points) implements CustomPacketPayload {
    public static final Type<StreetCredPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "street_cred"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StreetCredPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> b.writeVarInt(p.points), b -> new StreetCredPayload(b.readVarInt()));
    @Override public Type<StreetCredPayload> type() { return TYPE; }
}
