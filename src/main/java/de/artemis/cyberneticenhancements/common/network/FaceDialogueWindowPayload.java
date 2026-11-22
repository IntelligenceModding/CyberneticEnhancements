package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A request, from a key, to open one of the speaker's windows straight away: 1 shop, 2 services, 3 contracts, 4 customization. */
public record FaceDialogueWindowPayload(int view) implements CustomPacketPayload {
    public static final int SHOP = 1, SERVICES = 2, CONTRACTS = 3, CUSTOMIZE = 4;
    public static final Type<FaceDialogueWindowPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "face_dialogue_window"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FaceDialogueWindowPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeVarInt(payload.view), buffer -> new FaceDialogueWindowPayload(buffer.readVarInt()));

    @Override
    public Type<FaceDialogueWindowPayload> type() { return TYPE; }
}
