package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The answer picked from the options of the dialogue in front of you. */
public record FaceDialogueChoosePayload(int index) implements CustomPacketPayload {
    public static final Type<FaceDialogueChoosePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "face_dialogue_choose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FaceDialogueChoosePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeVarInt(payload.index), buffer -> new FaceDialogueChoosePayload(buffer.readVarInt()));

    @Override
    public Type<FaceDialogueChoosePayload> type() { return TYPE; }
}
