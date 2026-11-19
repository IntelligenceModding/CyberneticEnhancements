package de.artemis.cyberneticenhancements.client;

import com.google.gson.Gson;
import de.artemis.cyberneticenhancements.common.journal.PlayerJournal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** Local, account-scoped journal history. Never opens a world or connects to a server. */
public final class JournalHistoryStore {
    private static final Gson JSON = new Gson();
    public record Entry(long id, long timestamp, long dayTime, String category, String message, String dimension, int x, int y, int z) {
        public Entry {
            Objects.requireNonNull(category); Objects.requireNonNull(message); Objects.requireNonNull(dimension);
            if (timestamp < 0 || timestamp > 253402300799999L) throw new IllegalArgumentException("Invalid journal timestamp");
        }
        public static Entry from(PlayerJournal.Entry entry) {
            return new Entry(entry.id(), entry.timestamp(), entry.dayTime(), entry.category(), entry.message().getString(),
                    entry.dimension(), entry.pos().getX(), entry.pos().getY(), entry.pos().getZ());
        }
    }
    public record Source(String key, String name, String kind, Path world) {}
    public record Snapshot(String key, String name, String kind, long updated, boolean complete, List<Entry> entries) {
        public Snapshot {
            Objects.requireNonNull(key); Objects.requireNonNull(name); Objects.requireNonNull(kind);
            if (updated < 0 || updated > 253402300799999L) throw new IllegalArgumentException("Invalid journal snapshot timestamp");
            entries = List.copyOf(entries);
        }
        public static Snapshot empty(Source source) { return new Snapshot(source.key(), source.name(), source.kind(), 0, false, List.of()); }
    }

    public static String worldKey(Path world) { return "world:" + world.toAbsolutePath().normalize(); }
    public static String serverKey(String address) { return "server:" + address.strip().toLowerCase(Locale.ROOT); }
    private static Path directory(Path game, UUID player) { return game.resolve("cyberneticenhancements/journals").resolve(player.toString()); }
    public static Path file(Path game, UUID player, String key) {
        return directory(game, player).resolve(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)) + ".json");
    }
    public static Snapshot cached(Path game, UUID player, Source source) throws IOException {
        Path file = file(game, player, source.key());
        if (!Files.isRegularFile(file)) return Snapshot.empty(source);
        try (var reader = Files.newBufferedReader(file)) {
            var saved = JSON.fromJson(reader, Snapshot.class);
            if (saved == null || !source.key().equals(saved.key())) throw new IOException("Invalid journal snapshot");
            return saved;
        } catch (RuntimeException error) { throw new IOException("Unable to read journal snapshot", error); }
    }
    public static void save(Path game, UUID player, Snapshot snapshot) throws IOException {
        if (snapshot.kind().equals("world") && snapshot.key().startsWith("world:")
                && Files.notExists(Path.of(snapshot.key().substring("world:".length())))) return;
        Path destination = file(game, player, snapshot.key());
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "journal-", ".tmp");
        try {
            Files.writeString(temporary, JSON.toJson(snapshot), StandardCharsets.UTF_8);
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    /** Remove only this deleted world's cache, including other accounts sharing the instance. */
    public static int deleteWorld(Path game, Path world) throws IOException {
        if (!Files.notExists(world)) return 0;
        Path journals = game.resolve("cyberneticenhancements/journals");
        if (!Files.isDirectory(journals)) return 0;
        String filename = UUID.nameUUIDFromBytes(worldKey(world).getBytes(StandardCharsets.UTF_8)) + ".json";
        int removed = 0;
        IOException failure = null;
        try (var accounts = Files.list(journals)) {
            for (var account : accounts.filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)).toList()) {
                try { UUID.fromString(account.getFileName().toString()); }
                catch (IllegalArgumentException ignored) { continue; }
                try { if (Files.deleteIfExists(account.resolve(filename))) removed++; }
                catch (IOException error) {
                    if (failure == null) failure = error; else failure.addSuppressed(error);
                }
            }
        }
        if (failure != null) throw failure;
        return removed;
    }
    public static List<Source> sources(Path game, Path saves, UUID player, List<Source> servers) throws IOException {
        var sources = new LinkedHashMap<String, Source>();
        var lastPlayed = new HashMap<String, Long>();
        if (Files.isDirectory(saves)) try (var worlds = Files.list(saves)) {
            for (Path world : worlds.filter(Files::isDirectory)
                    .filter(path -> !TutorialWorld.isTutorialFolder(path.getFileName().toString())).toList()) {
                if (!Files.isRegularFile(world.resolve("level.dat"))) continue;
                String name = world.getFileName().toString();
                long played = 0;
                try {
                    var data = NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.create(64L * 1024 * 1024)).getCompound("Data");
                    name = data.getString("LevelName");
                    played = data.getLong("LastPlayed");
                }
                catch (IOException | RuntimeException ignored) { }
                var source = new Source(worldKey(world), name.isBlank() ? world.getFileName().toString() : name, "world", world);
                sources.put(source.key(), source);
                lastPlayed.put(source.key(), Math.max(0, played));
            }
        }
        for (var server : servers) sources.put(server.key(), server);
        Path directory = directory(game, player);
        if (Files.isDirectory(directory)) try (var files = Files.list(directory)) {
            for (var path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                try (var reader = Files.newBufferedReader(path)) {
                    var saved = JSON.fromJson(reader, Snapshot.class);
                    if (saved != null) {
                        if (saved.kind().equals("server"))
                            sources.putIfAbsent(saved.key(), new Source(saved.key(), saved.name(), saved.kind(), null));
                        // Server snapshots are refreshed while playing, never by browsing this screen.
                        // Worlds prefer Minecraft's LastPlayed, with cached history as a legacy fallback.
                        if (lastPlayed.getOrDefault(saved.key(), 0L) == 0)
                            lastPlayed.put(saved.key(), saved.updated());
                    }
                } catch (IOException | RuntimeException ignored) { }
            }
        }
        return sources.values().stream().sorted(Comparator
                .comparingLong((Source source) -> lastPlayed.getOrDefault(source.key(), 0L)).reversed()
                .thenComparing(Source::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Source::key)).toList();
    }
    public static Snapshot read(Path game, UUID player, Source source) throws IOException {
        if (source.world() == null) return cached(game, player, source);
        Path journal = source.world().resolve("data/cyberneticenhancements_journal_" + player + ".dat");
        if (!Files.isRegularFile(journal)) return cached(game, player, source);
        var root = NbtIo.readCompressed(journal, NbtAccounter.create(64L * 1024 * 1024));
        var data = PlayerJournal.load(root.getCompound("data"), RegistryAccess.EMPTY);
        var entries = new ArrayList<Entry>();
        for (int index = 0;; index++) {
            var page = data.page(index, "", "", false);
            page.entries().stream().map(Entry::from).forEach(entries::add);
            if (entries.size() >= page.total() || page.entries().isEmpty()) break;
        }
        return new Snapshot(source.key(), source.name(), source.kind(), Files.getLastModifiedTime(journal).toMillis(), true, entries);
    }
    public static List<Entry> filter(List<Entry> entries, String query, String category, boolean newest) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        var order = Comparator.comparingLong(Entry::id);
        return entries.stream().filter(e -> category.isEmpty() || e.category().equals(category))
                .filter(e -> needle.isEmpty() || (e.message() + " " + e.category() + " " + e.dimension() + " " + e.x() + ", " + e.y() + ", " + e.z()).toLowerCase(Locale.ROOT).contains(needle))
                .sorted(newest ? order.reversed() : order).toList();
    }
    private JournalHistoryStore() {}
}
