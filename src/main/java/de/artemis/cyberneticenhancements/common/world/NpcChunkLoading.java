package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.MaxTacEntity;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Entity-owned, persistent ticking tickets follow contacts and city service NPCs. */
public final class NpcChunkLoading {
    private static final TicketController TICKETS = new TicketController(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "resident_npcs"));
    private static final String HELD = "ce_npc_chunk_ticket", CHUNK = "ce_npc_ticket_chunk", DIMENSION = "ce_npc_ticket_dimension";
    private static final Map<ServerLevel, Set<Mob>> TRACKED = new IdentityHashMap<>();

    public static void register(RegisterTicketControllersEvent event) { event.register(TICKETS); }
    public static void track(Mob npc) {
        if (npc.level() instanceof ServerLevel level && (npc instanceof AbstractCityNpcEntity || npc instanceof MaxTacEntity guard && guard.isCityGuard()))
            TRACKED.computeIfAbsent(level, ignored -> new HashSet<>()).add(npc);
    }
    public static void joined(EntityJoinLevelEvent event) { if (event.getEntity() instanceof Mob npc) track(npc); }

    private static void update(ServerLevel level, Mob npc) {
        var data = npc.getPersistentData();
        var pos = npc.chunkPosition();
        String dimension = level.dimension().location().toString();
        TICKETS.forceChunk(level, npc.getUUID(), pos.x, pos.z, true, true);
        if (data.getBoolean(HELD) && (data.getLong(CHUNK) != pos.toLong() || !dimension.equals(data.getString(DIMENSION)))) {
            var old = new ChunkPos(data.getLong(CHUNK));
            for (var previousLevel : level.getServer().getAllLevels())
                if (previousLevel.dimension().location().toString().equals(data.getString(DIMENSION)))
                    TICKETS.forceChunk(previousLevel, npc.getUUID(), old.x, old.z, false, true);
        }
        data.putBoolean(HELD, true); data.putLong(CHUNK, pos.toLong()); data.putString(DIMENSION, dimension);
    }

    private static void release(Mob npc) {
        if (!(npc.level() instanceof ServerLevel level) || !npc.getPersistentData().getBoolean(HELD)) return;
        var data = npc.getPersistentData(); var pos = new ChunkPos(data.getLong(CHUNK));
        for (var previousLevel : level.getServer().getAllLevels())
            if (previousLevel.dimension().location().toString().equals(data.getString(DIMENSION)))
                TICKETS.forceChunk(previousLevel, npc.getUUID(), pos.x, pos.z, false, true);
        data.remove(HELD); data.remove(CHUNK); data.remove(DIMENSION);
    }
    public static void died(LivingDeathEvent event) { if (event.getEntity() instanceof Mob npc) release(npc); }
    public static void left(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof Mob npc) || !(event.getLevel() instanceof ServerLevel level) || !npc.isRemoved()) return;
        var tracked = TRACKED.get(level); if (tracked != null) tracked.remove(npc);
        if (npc.getRemovalReason() != Entity.RemovalReason.UNLOADED_TO_CHUNK) release(npc);
    }
    public static void unloaded(LevelEvent.Unload event) { TRACKED.remove(event.getLevel()); }
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var tracked = TRACKED.get(level); if (tracked == null) return;
        for (var npc : List.copyOf(tracked)) {
            if (npc.isRemoved()) { tracked.remove(npc); continue; }
            if (npc.isAlive() && (!npc.getPersistentData().getBoolean(HELD)
                    || npc.getPersistentData().getLong(CHUNK) != npc.chunkPosition().toLong()
                    || level.getGameTime() % 20 == 0)) update(level, npc);
        }
    }
    private NpcChunkLoading() { }
}
