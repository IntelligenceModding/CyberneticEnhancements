package de.artemis.cyberneticenhancements.common.quest;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Never join a terrain-generation future while servicing a menu on the server thread. */
final class QuestTerrainPreparation {
    private static final TicketType<ChunkPos> PREPARING = TicketType.create("cybernetic_contract_preparation", Comparator.comparingLong(ChunkPos::toLong));
    private static final TicketType<ChunkPos> READY = TicketType.create("cybernetic_contract_ready", Comparator.comparingLong(ChunkPos::toLong), 200);
    private record Key(ServerLevel level, int x, int z) { }
    private static final Map<Key, CompletableFuture<?>> REQUESTS = new HashMap<>();

    static boolean ready(ServerLevel level, int blockX, int blockZ) {
        int x = blockX >> 4, z = blockZ >> 4;
        if (level.getChunkSource().getChunkNow(x, z) != null) return true;
        var key = new Key(level, x, z);
        if (!REQUESTS.containsKey(key) && REQUESTS.size() < 2) {
            var pos = new ChunkPos(x, z);
            var source = level.getChunkSource();
            source.addRegionTicket(PREPARING, pos, 0, pos);
            // This native API waits when invoked ON the server thread, despite returning a future.
            // Its off-thread path schedules the actual chunk work safely on the server executor.
            var future = CompletableFuture.supplyAsync(() -> source.getChunkFuture(x, z, ChunkStatus.FULL, true)).thenCompose(value -> value);
            REQUESTS.put(key, future);
            future.whenComplete((result, error) -> level.getServer().execute(() -> {
                if (!REQUESTS.remove(key, future)) return;
                if (error == null) source.addRegionTicket(READY, pos, 0, pos);
                source.removeRegionTicket(PREPARING, pos, 0, pos);
            }));
        }
        return false;
    }

    static void unload(ServerLevel level) { REQUESTS.keySet().removeIf(key -> key.level == level); }
    private QuestTerrainPreparation() { }
}
