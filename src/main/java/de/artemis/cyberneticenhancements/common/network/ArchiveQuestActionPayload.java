package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ArchiveQuestActionPayload(int actionId, String questId) implements CustomPacketPayload {
    public static final int ACTION_DISCARD = 0;
    public static final int ACTION_TURN_IN = 1;
    public static final int ACTION_CLAIM_DROPOFF = 2;

    public static final Type<ArchiveQuestActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "archive_quest_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveQuestActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ArchiveQuestActionPayload::actionId,
            ByteBufCodecs.STRING_UTF8,
            ArchiveQuestActionPayload::questId,
            ArchiveQuestActionPayload::new
    );

    @Override
    public Type<ArchiveQuestActionPayload> type() {
        return TYPE;
    }
}
