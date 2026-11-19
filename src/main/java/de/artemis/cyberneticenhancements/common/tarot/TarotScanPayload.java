package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TarotScanPayload(int entityId, boolean held) implements CustomPacketPayload {
    public static final Type<TarotScanPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "tarot_scan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TarotScanPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.entityId); b.writeBoolean(p.held); }, b -> new TarotScanPayload(b.readVarInt(), b.readBoolean()));
    @Override public Type<TarotScanPayload> type() { return TYPE; }
}
