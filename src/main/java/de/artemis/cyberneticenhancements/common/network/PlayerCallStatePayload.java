package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record PlayerCallStatePayload(UUID callId, UUID requestId, UUID peerId, String name, String status, boolean incoming, boolean muted) implements CustomPacketPayload {
    public static final Type<PlayerCallStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player_call_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCallStatePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeUUID(p.callId); b.writeUUID(p.requestId); b.writeUUID(p.peerId); b.writeUtf(p.name, 64); b.writeUtf(p.status, 32); b.writeBoolean(p.incoming); b.writeBoolean(p.muted); },
            b -> new PlayerCallStatePayload(b.readUUID(), b.readUUID(), b.readUUID(), b.readUtf(64), b.readUtf(32), b.readBoolean(), b.readBoolean()));
    public Type<PlayerCallStatePayload> type() { return TYPE; }
}
