package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record TarotKnowledgePayload(List<String> ids) implements CustomPacketPayload {
    public static final Type<TarotKnowledgePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "tarot_knowledge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TarotKnowledgePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.ids.size()); p.ids.forEach(id -> b.writeUtf(id, 64)); }, b -> {
                int count = b.readVarInt(); if (count < 0 || count > 256) throw new IllegalArgumentException("Invalid tarot collection size");
                List<String> ids = new ArrayList<>(); for (int i = 0; i < count; i++) ids.add(b.readUtf(64));
                return new TarotKnowledgePayload(List.copyOf(ids));
            });
    @Override public Type<TarotKnowledgePayload> type() { return TYPE; }
}
