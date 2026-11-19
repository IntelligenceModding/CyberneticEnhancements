package de.artemis.cyberneticenhancements.client;

import java.nio.file.*;
import java.util.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class JournalHistoryStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final UUID owner = UUID.fromString("00cafe00-0000-0000-0000-000000000001");
    @Test public void deletingAWorldRemovesEveryAccountsCacheWithoutTouchingOtherWorldsOrServers() throws Exception {
        var game = temporary.getRoot().toPath();
        var world = Files.createDirectories(game.resolve("saves/Deleted world"));
        var other = Files.createDirectories(game.resolve("saves/Other world"));
        var second = UUID.randomUUID();
        var deleted = new JournalHistoryStore.Snapshot(JournalHistoryStore.worldKey(world), "Deleted", "world", 100, true, List.of());
        var retained = new JournalHistoryStore.Snapshot(JournalHistoryStore.worldKey(other), "Other", "world", 100, true, List.of());
        var server = new JournalHistoryStore.Snapshot(JournalHistoryStore.serverKey("friends.example"), "Friends", "server", 100, true, List.of());
        for (var account : List.of(owner, second)) {
            JournalHistoryStore.save(game, account, deleted);
            JournalHistoryStore.save(game, account, retained);
            JournalHistoryStore.save(game, account, server);
        }
        assertEquals(0, JournalHistoryStore.deleteWorld(game, world));
        assertTrue(Files.exists(JournalHistoryStore.file(game, owner, deleted.key())));
        // A corrupt cache must still be removable without reading its contents.
        Files.writeString(JournalHistoryStore.file(game, second, deleted.key()), "broken JSON");
        Files.delete(world);
        assertEquals(2, JournalHistoryStore.deleteWorld(game, world.resolve(".").normalize()));
        for (var account : List.of(owner, second)) {
            assertFalse(Files.exists(JournalHistoryStore.file(game, account, deleted.key())));
            assertTrue(Files.exists(JournalHistoryStore.file(game, account, retained.key())));
            assertTrue(Files.exists(JournalHistoryStore.file(game, account, server.key())));
        }
        JournalHistoryStore.save(game, owner, deleted);
        assertFalse("Late background save recreated a deleted world's journal", Files.exists(JournalHistoryStore.file(game, owner, deleted.key())));
        assertEquals(0, JournalHistoryStore.deleteWorld(game, world));
    }
    private JournalHistoryStore.Entry entry(long id, String message, String category) {
        return new JournalHistoryStore.Entry(id, 1_700_000_000_000L, id * 100, category, message, "minecraft:overworld", 1, 2, 3);
    }
    @Test public void serverHistoryRoundTripsWithAccountAndServerIsolation() throws Exception {
        var game = temporary.getRoot().toPath();
        String key = JournalHistoryStore.serverKey(" EXAMPLE.org:25565 ");
        var source = new JournalHistoryStore.Source(key, "Friends", "server", null);
        var entries = java.util.stream.LongStream.rangeClosed(1, 83).mapToObj(i -> entry(i, "Quest \"" + i + "\"\nDone", "quests")).toList();
        var snapshot = new JournalHistoryStore.Snapshot(key, source.name(), "server", 1_700_000_000_000L, true, entries);
        JournalHistoryStore.save(game, owner, snapshot);
        assertEquals(snapshot, JournalHistoryStore.read(game, owner, source));
        assertTrue(JournalHistoryStore.read(game, UUID.randomUUID(), source).entries().isEmpty());
        var other = new JournalHistoryStore.Source(JournalHistoryStore.serverKey("other.org"), "Other", "server", null);
        assertTrue(JournalHistoryStore.read(game, owner, other).entries().isEmpty());
        assertEquals(key, JournalHistoryStore.serverKey("example.ORG:25565"));
    }
    @Test public void filtersAndOrderingOperateOnTheWholeSavedHistory() {
        var entries = List.of(entry(1, "Bought parts", "commerce"), entry(3, "Completed Night City job", "quests"), entry(2, "Accepted job", "quests"));
        assertEquals(List.of(3L, 2L), JournalHistoryStore.filter(entries, "", "quests", true).stream().map(JournalHistoryStore.Entry::id).toList());
        assertEquals(3, JournalHistoryStore.filter(entries, "NIGHT CITY", "", false).getFirst().id());
        assertEquals(List.of(1L, 2L, 3L), JournalHistoryStore.filter(entries, "", "", false).stream().map(JournalHistoryStore.Entry::id).toList());
    }
    @Test public void malformedSnapshotsReportAnErrorInsteadOfReplacingExistingData() throws Exception {
        var game = temporary.getRoot().toPath();
        var source = new JournalHistoryStore.Source("server:friends", "Friends", "server", null);
        Path file = JournalHistoryStore.file(game, owner, source.key());
        Files.createDirectories(file.getParent()); Files.writeString(file, "broken JSON");
        assertThrows(java.io.IOException.class, () -> JournalHistoryStore.read(game, owner, source));
        assertEquals("broken JSON", Files.readString(file));
    }
    @Test public void backgroundSyncRepliesNeverReplaceTheArchivesInteractiveJournalPage() {
        JournalClientState.clear();
        var interactive = new de.artemis.cyberneticenhancements.common.network.JournalPayload(4, 2, 80, List.of());
        JournalClientState.receive(interactive);
        JournalClientState.receive(new de.artemis.cyberneticenhancements.common.network.JournalPayload(Long.MIN_VALUE + 1, 0, 0, List.of()));
        assertEquals(interactive, JournalClientState.get());
        JournalClientState.clear();
    }

    @Test public void sourcesSortByLastPlayedAcrossWorldsAndServersWithoutTouchingTheirDates() throws Exception {
        var game = temporary.getRoot().toPath();
        var saves = game.resolve("saves");
        for (var world : Map.of("Newest world", 500L, "Older world", 100L).entrySet()) {
            var folder = Files.createDirectories(saves.resolve(world.getKey()));
            var root = new net.minecraft.nbt.CompoundTag();
            var data = new net.minecraft.nbt.CompoundTag();
            data.putString("LevelName", world.getKey()); data.putLong("LastPlayed", world.getValue());
            root.put("Data", data);
            net.minecraft.nbt.NbtIo.writeCompressed(root, folder.resolve("level.dat"));
        }
        var recent = new JournalHistoryStore.Source("server:recent", "Recent server", "server", null);
        var archived = new JournalHistoryStore.Source("server:archived", "Archived server", "server", null);
        var unplayed = new JournalHistoryStore.Source("server:unplayed", "Unplayed server", "server", null);
        JournalHistoryStore.save(game, owner, new JournalHistoryStore.Snapshot(recent.key(), recent.name(), "server", 300, true, List.of()));
        JournalHistoryStore.save(game, owner, new JournalHistoryStore.Snapshot(archived.key(), archived.name(), "server", 200, true, List.of()));
        var expected = List.of("Newest world", "Recent server", "Archived server", "Older world", "Unplayed server");
        for (int i = 0; i < 2; i++)
            assertEquals(expected, JournalHistoryStore.sources(game, saves, owner, List.of(unplayed, recent)).stream()
                    .map(JournalHistoryStore.Source::name).toList());
        assertEquals(300, JournalHistoryStore.cached(game, owner, recent).updated());
    }
}
