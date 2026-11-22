package de.artemis.cyberneticenhancements.common.streetcred;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Street Cred of every player the server has seen, online or not, so the leaderboard can rank them all. */
public final class StreetCredBoard extends SavedData {
    private static final Factory<StreetCredBoard> FACTORY = new Factory<>(StreetCredBoard::new, StreetCredBoard::load, null);
    /** Names shown on the leaderboard. */
    public static final int TOP = 10;
    private static final Comparator<Entry> ORDER = Comparator.comparingInt((Entry entry) -> entry.points).reversed()
            .thenComparing(entry -> entry.name.toLowerCase(Locale.ROOT)).thenComparing(entry -> entry.id);

    public record Entry(UUID id, String name, int points, String texture) {}

    private final Map<UUID, Entry> players = new HashMap<>();

    public static StreetCredBoard get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_street_cred");
    }

    /** Notes a player's points and name. Does nothing, and saves nothing, when neither changed. */
    public void record(UUID id, String name, int points, String texture) {
        Entry old = players.get(id);
        if (old != null && old.points == points && old.name.equals(name) && old.texture.equals(texture)) return;
        if (points <= 0) { if (players.remove(id) != null) setDirty(); return; }
        players.put(id, new Entry(id, name, points, texture));
        setDirty();
    }

    private List<Entry> ranked() {
        List<Entry> list = new ArrayList<>(players.values());
        list.sort(ORDER);
        return list;
    }

    public void respond(ServerPlayer player) {
        List<Entry> ranked = ranked();
        int own = 0;
        for (int i = 0; i < ranked.size(); i++) if (ranked.get(i).id.equals(player.getUUID())) { own = i + 1; break; }
        List<StreetCredBoardPayload.Row> rows = new ArrayList<>();
        for (Entry entry : ranked.subList(0, Math.min(TOP, ranked.size()))) rows.add(new StreetCredBoardPayload.Row(entry.id, entry.name, entry.points, entry.texture));
        PacketDistributor.sendToPlayer(player, new StreetCredBoardPayload(ranked.size(), own, rows));
    }

    static StreetCredBoard load(CompoundTag tag, HolderLookup.Provider registries) {
        StreetCredBoard board = new StreetCredBoard();
        for (Tag value : tag.getList("Players", Tag.TAG_COMPOUND)) {
            if (!(value instanceof CompoundTag data) || !data.hasUUID("Id")) continue;
            int points = data.getInt("Points");
            if (points > 0) board.players.put(data.getUUID("Id"), new Entry(data.getUUID("Id"), data.getString("Name"), points, data.getString("Texture")));
        }
        return board;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : players.values()) {
            CompoundTag data = new CompoundTag();
            data.putUUID("Id", entry.id);
            data.putString("Name", entry.name);
            data.putInt("Points", entry.points);
            data.putString("Texture", entry.texture);
            list.add(data);
        }
        tag.put("Players", list);
        return tag;
    }
}
