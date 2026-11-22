package de.artemis.cyberneticenhancements.common.streetcred;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The top of the leaderboard, how many players are on it, and where the receiving player stands (0 if not on it). */
public record StreetCredBoardPayload(int total, int own, List<Row> rows) implements CustomPacketPayload {
    public record Row(UUID id, String name, int points, String texture) {}

    public static final Type<StreetCredBoardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "street_cred_board"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StreetCredBoardPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> {
                b.writeVarInt(p.total);
                b.writeVarInt(p.own);
                b.writeVarInt(p.rows.size());
                for (Row row : p.rows) { b.writeUUID(row.id); b.writeUtf(row.name, 64); b.writeVarInt(row.points); b.writeUtf(row.texture, 2048); }
            },
            b -> {
                int total = b.readVarInt(), own = b.readVarInt();
                int size = Math.min(b.readVarInt(), StreetCredBoard.TOP);
                List<Row> rows = new ArrayList<>(size);
                for (int i = 0; i < size; i++) rows.add(new Row(b.readUUID(), b.readUtf(64), b.readVarInt(), b.readUtf(2048)));
                return new StreetCredBoardPayload(total, own, List.copyOf(rows));
            });
    @Override public Type<StreetCredBoardPayload> type() { return TYPE; }
}
