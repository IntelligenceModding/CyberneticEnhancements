package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.UUID;

public record NpcDispositionPayload(List<UUID> angry) implements CustomPacketPayload {
    public static final Type<NpcDispositionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "npc_disposition"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcDispositionPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeCollection(payload.angry, (out, id) -> out.writeUUID(id)),
            buffer -> new NpcDispositionPayload(buffer.readList(in -> in.readUUID())));
    public NpcDispositionPayload { angry = List.copyOf(angry); }
    @Override public Type<NpcDispositionPayload> type() { return TYPE; }
}
