package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Tells a player to open the sign hung {@code t} of the way along a hanging wire for writing, on its front or back. */
public record OpenHangingSignPayload(int wire, double t, boolean front) implements CustomPacketPayload {
    public static final Type<OpenHangingSignPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "open_hanging_sign"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenHangingSignPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> { buffer.writeVarInt(payload.wire()); buffer.writeDouble(payload.t()); buffer.writeBoolean(payload.front()); },
            buffer -> new OpenHangingSignPayload(buffer.readVarInt(), buffer.readDouble(), buffer.readBoolean()));

    @Override public Type<OpenHangingSignPayload> type() { return TYPE; }
}
