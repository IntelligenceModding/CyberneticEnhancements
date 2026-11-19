package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JournalRequestPayload(long request, int page, String category, String query, boolean newest) implements CustomPacketPayload {
    public static final Type<JournalRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "journal_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalRequestPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeLong(p.request); b.writeVarInt(p.page); b.writeUtf(p.category, 32); b.writeUtf(p.query, 128); b.writeBoolean(p.newest); },
            b -> new JournalRequestPayload(b.readLong(), b.readVarInt(), b.readUtf(32), b.readUtf(128), b.readBoolean()));
    public Type<JournalRequestPayload> type() { return TYPE; }
}
