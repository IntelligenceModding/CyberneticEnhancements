package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TarotDecorationResultPayload(String cardId, boolean placed) implements CustomPacketPayload {
    public static final Type<TarotDecorationResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "tarot_decoration_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TarotDecorationResultPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeUtf(p.cardId, 64); b.writeBoolean(p.placed); }, b -> new TarotDecorationResultPayload(b.readUtf(64), b.readBoolean()));
    @Override public Type<TarotDecorationResultPayload> type() { return TYPE; }
}
