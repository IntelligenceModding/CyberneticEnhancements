package de.artemis.cyberneticenhancements.client;

import com.mojang.logging.LogUtils;
import de.artemis.cyberneticenhancements.common.journal.PlayerJournal;
import de.artemis.cyberneticenhancements.common.network.JournalPayload;
import de.artemis.cyberneticenhancements.common.network.JournalRequestPayload;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Low-rate, paged history sync for offline browsing. It never initiates a connection. */
public final class JournalHistorySync {
    private static Object connection;
    private static JournalHistoryStore.Source source;
    private static UUID player;
    private static long token = Long.MIN_VALUE, waiting, next, cutoff;
    private static int page;
    private static final TreeMap<Long, JournalHistoryStore.Entry> history = new TreeMap<>();
    private static CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);

    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null) { clear(); return; }
        if (connection != mc.getConnection()) {
            clear(); connection = mc.getConnection(); player = mc.getUser().getProfileId();
            if (mc.getSingleplayerServer() != null) {
                var server = mc.getSingleplayerServer();
                var path = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                source = new JournalHistoryStore.Source(JournalHistoryStore.worldKey(path), server.getWorldData().getLevelName(), "world", path);
            } else {
                var server = mc.getCurrentServer();
                if (server == null) return;
                source = new JournalHistoryStore.Source(JournalHistoryStore.serverKey(server.ip), server.name, "server", null);
            }
            next = Util.getMillis() + 1_000;
        }
        if (source == null || mc.screen instanceof de.artemis.cyberneticenhancements.client.screen.CyberwareWikiScreen) return;
        long now = Util.getMillis();
        if (now < next) return;
        waiting = ++token;
        PacketDistributor.sendToServer(new JournalRequestPayload(waiting, page, "", "", true));
        next = now + 2_000; // Retry if the shared server request throttle dropped the request.
    }

    public static boolean receive(JournalPayload payload) {
        if (payload.request() >= -1) return false;
        if (source == null || payload.request() != waiting) return true;
        long previousMax = history.isEmpty() ? 0 : history.lastKey();
        if (page == 0) cutoff = previousMax;
        boolean reachedKnown = false;
        for (var entry : payload.entries()) {
            if (entry.id() <= cutoff) reachedKnown = true;
            history.put(entry.id(), JournalHistoryStore.Entry.from(entry));
        }
        boolean complete = reachedKnown || payload.entries().isEmpty() || (payload.page() + 1L) * PlayerJournal.PAGE_SIZE >= payload.total();
        var snapshot = new JournalHistoryStore.Snapshot(source.key(), source.name(), source.kind(), System.currentTimeMillis(), complete, List.copyOf(history.values()));
        var game = Minecraft.getInstance().gameDirectory.toPath();
        UUID owner = player;
        writes = writes.handle((ignored, error) -> null).thenRunAsync(() -> {
            try { JournalHistoryStore.save(game, owner, snapshot); }
            catch (java.io.IOException error) { LogUtils.getLogger().warn("Unable to save local journal history", error); }
        }, Util.ioPool());
        waiting = 0;
        page = complete ? 0 : payload.page() + 1;
        next = Util.getMillis() + (complete ? 15_000 : 300);
        return true;
    }

    public static void worldDeleted(java.nio.file.Path world) {
        var mc = Minecraft.getInstance();
        var game = mc.gameDirectory.toPath();
        var deleted = world.toAbsolutePath().normalize();
        mc.execute(() -> {
            if (source != null && source.key().equals(JournalHistoryStore.worldKey(deleted))) clear();
            // Append after outstanding writes so a last disconnect snapshot cannot recreate the cache.
            writes = writes.handle((ignored, error) -> null).thenRunAsync(() -> {
                try { JournalHistoryStore.deleteWorld(game, deleted); }
                catch (java.io.IOException error) { LogUtils.getLogger().warn("Unable to remove deleted world's journal cache", error); }
            }, Util.ioPool());
        });
    }

    public static void clear() { connection = null; source = null; player = null; waiting = 0; next = 0; page = 0; cutoff = 0; history.clear(); }
    private JournalHistorySync() {}
}
