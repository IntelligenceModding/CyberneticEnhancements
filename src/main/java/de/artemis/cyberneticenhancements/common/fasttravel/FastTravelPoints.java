package de.artemis.cyberneticenhancements.common.fasttravel;

import de.artemis.cyberneticenhancements.common.network.FastTravelPointsPayload;
import de.artemis.cyberneticenhancements.common.network.FastTravelPointsPayload.Point;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Every fast travel terminal in the world, and which of them each player has found. */
public final class FastTravelPoints extends SavedData {
    private static final Factory<FastTravelPoints> FACTORY = new Factory<>(FastTravelPoints::new, FastTravelPoints::load, null);
    private final Map<String, Point> points = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> found = new HashMap<>();
    private int named;

    public static FastTravelPoints get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_fast_travel");
    }

    public Point find(String dimension, BlockPos pos) { return points.get(new Point(dimension, pos, "").id()); }

    public List<Point> all() { return List.copyOf(points.values()); }

    /** Sets a terminal up, under the given name or the next free number. */
    public Point add(String dimension, BlockPos pos, String name) {
        var point = new Point(dimension, pos.immutable(), name == null || name.isBlank() ? "Node " + ++named : name);
        points.put(point.id(), point);
        setDirty();
        return point;
    }

    public void remove(String dimension, BlockPos pos) {
        var point = find(dimension, pos);
        if (point == null) return;
        points.remove(point.id());
        found.values().forEach(set -> set.remove(point.id()));
        setDirty();
    }

    public boolean discovered(UUID player, Point point) { return found.getOrDefault(player, Set.of()).contains(point.id()); }

    /** Whether this is the first time the player has found the terminal. */
    public boolean discover(UUID player, Point point) {
        if (!found.computeIfAbsent(player, id -> new HashSet<>()).add(point.id())) return false;
        setDirty();
        return true;
    }

    public FastTravelPointsPayload capture(UUID player) {
        return new FastTravelPointsPayload(points.values().stream().filter(point -> discovered(player, point)).toList());
    }

    public void sync(ServerPlayer player) { PacketDistributor.sendToPlayer(player, capture(player.getUUID())); }

    public void syncAll(MinecraftServer server) { server.getPlayerList().getPlayers().forEach(this::sync); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        for (var point : points.values()) {
            var entry = new CompoundTag();
            entry.putString("Dimension", point.dimension());
            entry.putLong("Pos", point.pos().asLong());
            entry.putString("Name", point.name());
            list.add(entry);
        }
        tag.put("Points", list);
        var players = new CompoundTag();
        found.forEach((id, set) -> {
            var ids = new ListTag();
            set.forEach(value -> ids.add(StringTag.valueOf(value)));
            players.put(id.toString(), ids);
        });
        tag.put("Found", players);
        tag.putInt("Named", named);
        return tag;
    }

    private static FastTravelPoints load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new FastTravelPoints();
        for (Tag raw : tag.getList("Points", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) raw;
            var point = new Point(entry.getString("Dimension"), BlockPos.of(entry.getLong("Pos")), entry.getString("Name"));
            data.points.put(point.id(), point);
        }
        var players = tag.getCompound("Found");
        for (String key : players.getAllKeys()) {
            try {
                var set = new HashSet<String>();
                players.getList(key, Tag.TAG_STRING).forEach(value -> set.add(value.getAsString()));
                data.found.put(UUID.fromString(key), set);
            } catch (IllegalArgumentException ignored) {
                // A key that is not a player is dropped.
            }
        }
        data.named = tag.getInt("Named");
        return data;
    }
}
