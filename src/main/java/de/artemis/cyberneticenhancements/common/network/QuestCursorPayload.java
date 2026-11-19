package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestCursorPayload(String contract, String itemType, long revision) implements CustomPacketPayload {
    public static final Type<QuestCursorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "quest_cursor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestCursorPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> { buffer.writeUtf(payload.contract, 64); buffer.writeUtf(payload.itemType, 64); buffer.writeVarLong(payload.revision); },
            buffer -> new QuestCursorPayload(buffer.readUtf(64), buffer.readUtf(64), buffer.readVarLong()));

    @Override
    public Type<QuestCursorPayload> type() { return TYPE; }
}
