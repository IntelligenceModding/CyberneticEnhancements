package de.artemis.cyberneticenhancements.common.streetcred;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Asks the server for the leaderboard. */
public record StreetCredBoardRequestPayload() implements CustomPacketPayload {
    public static final Type<StreetCredBoardRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "street_cred_board_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StreetCredBoardRequestPayload> STREAM_CODEC = StreamCodec.unit(new StreetCredBoardRequestPayload());
    @Override public Type<StreetCredBoardRequestPayload> type() { return TYPE; }
}
