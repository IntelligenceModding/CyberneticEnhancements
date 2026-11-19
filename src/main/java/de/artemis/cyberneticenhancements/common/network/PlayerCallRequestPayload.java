package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record PlayerCallRequestPayload(UUID callId, UUID peerId, int action) implements CustomPacketPayload {
    public static final int START = -1, END = 0, MUTE = 1, BLOCK = 2, KEEPALIVE = 6, ANSWER = 15;
    public static final Type<PlayerCallRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player_call_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCallRequestPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeUUID(p.callId); b.writeUUID(p.peerId); b.writeVarInt(p.action); },
            b -> new PlayerCallRequestPayload(b.readUUID(), b.readUUID(), b.readVarInt()));
    public Type<PlayerCallRequestPayload> type() { return TYPE; }
}
