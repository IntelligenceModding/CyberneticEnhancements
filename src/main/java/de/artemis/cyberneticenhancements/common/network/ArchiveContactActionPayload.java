package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ArchiveContactActionPayload(int actionId, String npcId) implements CustomPacketPayload {
    public static final int ACTION_TOGGLE_PIN = 2;
    public static final int ACTION_TOGGLE_HIDE = 3;
    public static final int ACTION_TOGGLE_BLOCK = 4;
    public static final int ACTION_ACKNOWLEDGE_DECEASED = 5;

    public static final Type<ArchiveContactActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "archive_contact_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveContactActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ArchiveContactActionPayload::actionId,
            ByteBufCodecs.STRING_UTF8,
            ArchiveContactActionPayload::npcId,
            ArchiveContactActionPayload::new
    );

    @Override
    public Type<ArchiveContactActionPayload> type() {
        return TYPE;
    }
}
