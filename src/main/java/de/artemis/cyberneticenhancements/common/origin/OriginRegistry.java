package de.artemis.cyberneticenhancements.common.origin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Who holds which lifepath, for every player the server has seen, online or not. Kept sorted per lifepath so a page of
 * the roster costs the same however many players there are: one binary search to change an entry, one slice to read a page.
 */
public final class OriginRegistry extends SavedData {
    private static final Factory<OriginRegistry> FACTORY = new Factory<>(OriginRegistry::new, OriginRegistry::load, null);
    /** Names per roster page. */
    public static final int PAGE = 24;
    private static final Comparator<Entry> ORDER = Comparator.comparing((Entry entry) -> entry.name.toLowerCase(Locale.ROOT)).thenComparing(entry -> entry.id);

    private record Entry(UUID id, String name, Origin origin) {}

    private final Map<UUID, Entry> players = new HashMap<>();
    private final Map<Origin, List<Entry>> sorted = new EnumMap<>(Origin.class);

    public static OriginRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_origins");
    }

    /** Notes a player's current lifepath (null for none) and name. Does nothing, and saves nothing, when neither changed. */
    public void record(UUID id, String name, @Nullable Origin origin) {
        Entry old = players.get(id);
        if (old != null && old.origin == origin && old.name.equals(name)) return;
        if (old != null) { detach(old); players.remove(id); }
        if (origin != null) {
            Entry entry = new Entry(id, name, origin);
            players.put(id, entry);
            attach(entry);
        }
        setDirty();
    }

    public int count(Origin origin) { return sorted.getOrDefault(origin, List.of()).size(); }

    public static int pages(int total) { return Math.max(1, (total + PAGE - 1) / PAGE); }

    /** One page of names in alphabetical order; a page past the end is the last page. */
    public List<String> page(Origin origin, int page) {
        List<Entry> list = sorted.getOrDefault(origin, List.of());
        int from = Math.clamp(page, 0, pages(list.size()) - 1) * PAGE;
        List<String> names = new ArrayList<>(PAGE);
        for (Entry entry : list.subList(Math.min(from, list.size()), Math.min(from + PAGE, list.size()))) names.add(entry.name);
        return names;
    }

    public void respond(ServerPlayer player, OriginRosterRequestPayload request) {
        Origin origin = Origin.find(request.origin());
        if (origin == null) return;
        int total = count(origin);
        int page = Math.clamp(request.page(), 0, pages(total) - 1);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new OriginRosterPayload(origin.id(), page, total, page(origin, page)));
    }

    private void attach(Entry entry) {
        List<Entry> list = sorted.computeIfAbsent(entry.origin, ignored -> new ArrayList<>());
        int index = Collections.binarySearch(list, entry, ORDER);
        list.add(index >= 0 ? index : -index - 1, entry);
    }

    private void detach(Entry entry) {
        List<Entry> list = sorted.get(entry.origin);
        if (list == null) return;
        int index = Collections.binarySearch(list, entry, ORDER);
        if (index >= 0) list.remove(index);
    }

    static OriginRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        OriginRegistry registry = new OriginRegistry();
        for (Tag value : tag.getList("Players", Tag.TAG_COMPOUND)) {
            if (!(value instanceof CompoundTag data) || !data.hasUUID("Id")) continue;
            Origin origin = Origin.find(data.getString("Origin"));
            if (origin == null) continue;
            Entry entry = new Entry(data.getUUID("Id"), data.getString("Name"), origin);
            registry.players.put(entry.id, entry);
        }
        // Sorted once on load instead of by insertion, so a registry of thousands loads in one pass.
        for (Entry entry : registry.players.values()) registry.sorted.computeIfAbsent(entry.origin, ignored -> new ArrayList<>()).add(entry);
        registry.sorted.values().forEach(list -> list.sort(ORDER));
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : players.values()) {
            CompoundTag data = new CompoundTag();
            data.putUUID("Id", entry.id);
            data.putString("Name", entry.name);
            data.putString("Origin", entry.origin.id());
            list.add(data);
        }
        tag.put("Players", list);
        return tag;
    }
}
