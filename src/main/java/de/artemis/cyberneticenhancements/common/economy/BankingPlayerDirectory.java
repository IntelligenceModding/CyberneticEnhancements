package de.artemis.cyberneticenhancements.common.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BankingPlayerDirectory extends SavedData {
    private static final Factory<BankingPlayerDirectory> FACTORY = new Factory<>(BankingPlayerDirectory::new, BankingPlayerDirectory::load, null);
    private final Map<UUID, Entry> players = new LinkedHashMap<>();
    private boolean imported;

    public record Entry(UUID id, String name, long lastSeen) {}

    public static BankingPlayerDirectory get(MinecraftServer server) {
        var directory = server.overworld().getDataStorage().computeIfAbsent(FACTORY, "cyberneticenhancements_bank_players");
        if (!directory.imported) directory.importPlayers(server);
        return directory;
    }

    public void update(ServerPlayer player) {
        record(player.getUUID(), player.getGameProfile().getName(), System.currentTimeMillis());
    }

    void record(UUID id, String name, long lastSeen) {
        players.put(id, new Entry(id, name, lastSeen));
        setDirty();
    }

    public static long daysAgo(long lastSeen, long now) { return Math.max(0, (now - lastSeen) / 86400000L); }

    public List<Entry> entries() { return players.values().stream().sorted(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER)).toList(); }

    public Entry find(String nameOrId) {
        return players.values().stream().filter(entry -> entry.name.equalsIgnoreCase(nameOrId.strip()) || entry.id.toString().equalsIgnoreCase(nameOrId.strip())).findFirst().orElse(null);
    }

    private void importPlayers(MinecraftServer server) {
        var path = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (Files.isDirectory(path)) {
            try (var files = Files.list(path)) {
                files.filter(file -> file.getFileName().toString().endsWith(".dat")).forEach(file -> {
                    try {
                        String filename = file.getFileName().toString();
                        UUID id = UUID.fromString(filename.substring(0, filename.length() - 4));
                        String name = server.getProfileCache() == null ? id.toString()
                                : server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName).orElse(id.toString());
                        players.putIfAbsent(id, new Entry(id, name, Files.getLastModifiedTime(file).toMillis()));
                    } catch (IllegalArgumentException | java.io.IOException ignored) {}
                });
            } catch (java.io.IOException exception) {
                org.slf4j.LoggerFactory.getLogger(BankingPlayerDirectory.class).warn("Could not import saved banking recipients", exception);
                return;
            }
        }
        imported = true;
        for (var player : server.getPlayerList().getPlayers()) update(player);
        setDirty();
    }

    static BankingPlayerDirectory load(CompoundTag tag, HolderLookup.Provider registries) {
        var directory = new BankingPlayerDirectory();
        directory.imported = tag.getBoolean("Imported");
        for (var value : tag.getList("Players", Tag.TAG_COMPOUND)) {
            if (!(value instanceof CompoundTag entry) || !entry.hasUUID("Id")) continue;
            UUID id = entry.getUUID("Id");
            String name = entry.getString("Name");
            directory.players.put(id, new Entry(id, name.isBlank() ? id.toString() : name, entry.getLong("LastSeen")));
        }
        return directory;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var entries = new ListTag();
        for (var entry : players.values()) {
            var value = new CompoundTag();
            value.putUUID("Id", entry.id); value.putString("Name", entry.name); value.putLong("LastSeen", entry.lastSeen);
            entries.add(value);
        }
        tag.put("Players", entries); tag.putBoolean("Imported", imported);
        return tag;
    }
}
