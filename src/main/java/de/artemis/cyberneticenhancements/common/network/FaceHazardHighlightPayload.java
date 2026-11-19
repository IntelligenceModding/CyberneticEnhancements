package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FaceHazardHighlightPayload(List<BlockPos> positions, List<UUID> entityIds, int ttlTicks, boolean npcLocator) implements CustomPacketPayload {
    public static final Type<FaceHazardHighlightPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "face_hazard_highlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FaceHazardHighlightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC),
            FaceHazardHighlightPayload::positions,
            ByteBufCodecs.collection(ArrayList::new, UUIDUtil.STREAM_CODEC),
            FaceHazardHighlightPayload::entityIds,
            ByteBufCodecs.INT,
            FaceHazardHighlightPayload::ttlTicks,
            ByteBufCodecs.BOOL,
            FaceHazardHighlightPayload::npcLocator,
            FaceHazardHighlightPayload::new
    );

    public FaceHazardHighlightPayload(List<BlockPos> positions, List<UUID> entityIds, int ttlTicks) {
        this(positions, entityIds, ttlTicks, false);
    }

    @Override
    public Type<FaceHazardHighlightPayload> type() {
        return TYPE;
    }
}
