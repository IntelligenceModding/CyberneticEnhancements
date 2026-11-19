package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.journal.PlayerJournal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public record JournalPayload(long request, int page, int total, List<PlayerJournal.Entry> entries) implements CustomPacketPayload {
    public static final Type<JournalPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "journal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> STREAM_CODEC = CustomPacketPayload.codec((p, b) -> {
        b.writeLong(p.request); b.writeVarInt(p.page); b.writeVarInt(p.total); b.writeVarInt(p.entries.size());
        for (var e : p.entries) {
            b.writeLong(e.id()); b.writeLong(e.timestamp()); b.writeLong(e.gameTime()); b.writeLong(e.dayTime()); b.writeUtf(e.category(), 32);
            ComponentSerialization.STREAM_CODEC.encode(b, e.message()); b.writeUtf(e.dimension(), 256); b.writeBlockPos(e.pos());
        }
    }, b -> {
        long request = b.readLong(); int page = b.readVarInt(), total = b.readVarInt(), size = b.readVarInt();
        if (size < 0 || size > PlayerJournal.PAGE_SIZE || page < 0 || total < 0) throw new IllegalArgumentException("Invalid journal page");
        List<PlayerJournal.Entry> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) entries.add(new PlayerJournal.Entry(b.readLong(), b.readLong(), b.readLong(), b.readLong(), b.readUtf(32),
                ComponentSerialization.STREAM_CODEC.decode(b), b.readUtf(256), b.readBlockPos()));
        return new JournalPayload(request, page, total, List.copyOf(entries));
    });
    public Type<JournalPayload> type() { return TYPE; }
}
