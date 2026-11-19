package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TarotDecorationPayload(String cardId, TarotDecorations.Placement placement) implements CustomPacketPayload {
    public static final Type<TarotDecorationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "tarot_decoration"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TarotDecorationPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeUtf(p.cardId, 64); b.writeBlockPos(p.placement.anchor()); b.writeEnum(p.placement.facing()); },
            b -> new TarotDecorationPayload(b.readUtf(64), new TarotDecorations.Placement(b.readBlockPos(), b.readEnum(net.minecraft.core.Direction.class))));
    @Override public Type<TarotDecorationPayload> type() { return TYPE; }
}
