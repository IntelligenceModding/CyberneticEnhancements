package de.artemis.cyberneticenhancements.common.tarot;

import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.*;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

/** Loaded-chunk-only, budgeted wall search, including modded registered structures. */
public final class TarotWorldgen {
    private static final Map<ServerLevel, State> STATES = new IdentityHashMap<>();
    private static final Direction[] FACES = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
    private static final class State {
        final Set<Long> chunks = new LinkedHashSet<>();
        final LinkedHashMap<String, Job> jobs = new LinkedHashMap<>();
        final Map<Long, Set<Long>> waitingForStart = new HashMap<>();
    }
    private static final class Job {
        final String key; final ChunkPos chunk; final StructureStart start; final int minX, minY, minZ, nx, ny, nz, size, offset;
        int cursor;
        Job(String key, ChunkPos chunk, StructureStart start, ServerLevel level) {
            this.key = key; this.chunk = chunk; this.start = start;
            BoundingBox b = start.getBoundingBox();
            minX = Math.max(chunk.getMinBlockX(), b.minX() - 1); minZ = Math.max(chunk.getMinBlockZ(), b.minZ() - 1);
            minY = Math.max(level.getMinBuildHeight(), b.minY());
            nx = Math.max(0, Math.min(chunk.getMaxBlockX(), b.maxX() + 1) - minX + 1);
            nz = Math.max(0, Math.min(chunk.getMaxBlockZ(), b.maxZ() + 1) - minZ + 1);
            ny = Math.max(0, Math.min(level.getMaxBuildHeight() - 3, b.maxY()) - minY + 1);
            size = nx * ny * nz; offset = size == 0 ? 0 : Math.floorMod(key.hashCode() * 31L + chunk.toLong(), size);
        }
        BlockPos next() {
            int index = (cursor++ + offset) % size;
            return new BlockPos(minX + index % nx, minY + index / (nx * nz), minZ + index / nx % nz);
        }
    }
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk)
        {
            State state = STATES.computeIfAbsent(level, ignored -> new State());
            long pos = chunk.getPos().toLong();
            state.chunks.add(pos);
            Set<Long> waiting = state.waitingForStart.remove(pos);
            if (waiting != null) state.chunks.addAll(waiting);
        }
    }
    public static void onLevelUnload(LevelEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) STATES.remove(level); }
    public static String structureKey(ServerLevel level, StructureStart start) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure()) + "@" + start.getChunkPos().toLong();
    }
    public static boolean selected(long seed, String key) {
        return net.minecraft.util.RandomSource.create(seed ^ (long) key.hashCode() * 0x9E3779B97F4A7C15L).nextInt(3) == 0;
    }
    static void enqueue(ServerLevel level, LevelChunk chunk) {
        State state = STATES.computeIfAbsent(level, ignored -> new State());
        Set<StructureStart> starts = new HashSet<>(chunk.getAllStarts().values());
        chunk.getAllReferences().forEach((structure, references) -> references.forEach((long pos) -> {
            ChunkPos at = new ChunkPos(pos);
            LevelChunk loaded = level.getChunkSource().getChunkNow(at.x, at.z);
            if (loaded != null) { StructureStart start = loaded.getStartForStructure(structure); if (start != null) starts.add(start); }
            else state.waitingForStart.computeIfAbsent(pos, ignored -> new HashSet<>()).add(chunk.getPos().toLong());
        }));
        for (StructureStart start : starts) {
            if (!start.isValid()) continue;
            String key = structureKey(level, start);
            if (!selected(level.getSeed(), key) || TarotPlacements.get(level).contains(key)) continue;
            String jobKey = key + ":" + chunk.getPos().toLong();
            state.jobs.putIfAbsent(jobKey, new Job(key, chunk.getPos(), start, level));
        }
    }
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        State state = STATES.get(level); if (state == null) return;
        for (int i = 0; i < 2 && !state.chunks.isEmpty(); i++) {
            Iterator<Long> it = state.chunks.iterator(); ChunkPos pos = new ChunkPos(it.next()); it.remove();
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) enqueue(level, chunk);
        }
        int budget = 192;
        Iterator<Job> it = state.jobs.values().iterator();
        while (it.hasNext() && budget > 0) {
            Job job = it.next();
            if (TarotPlacements.get(level).contains(job.key) || level.getChunkSource().getChunkNow(job.chunk.x, job.chunk.z) == null) { it.remove(); continue; }
            boolean placed = false;
            while (job.cursor < job.size && budget-- > 0 && !placed) {
                BlockPos anchor = job.next();
                if (!level.getBlockState(anchor).getCollisionShape(level, anchor).isEmpty()) continue;
                for (Direction face : FACES) {
                    if (!insidePiece(job.start, anchor, face) || !TarotCardEntity.canAttach(level, anchor, face)) continue;
                    TarotCardEntity card = ModEntityTypes.TAROT_CARD.get().create(level);
                    if (card == null) break;
                    card.attach(anchor, face, TarotCatalog.forStructure(level.getSeed(), job.key).id());
                    if (!level.getEntitiesOfClass(TarotCardEntity.class, card.getBoundingBox().inflate(.1)).isEmpty()) continue;
                    if (level.addFreshEntity(card)) {
                        TarotPlacements.get(level).mark(job.key); TarotPlacements.get(level).register(card); placed = true;
                    }
                    break;
                }
            }
            if (placed || job.cursor >= job.size) it.remove();
        }
    }
    private static boolean insidePiece(StructureStart start, BlockPos anchor, Direction face) {
        Direction right = face.getCounterClockWise();
        for (int col = 0; col < 2; col++) for (int row = 0; row < 3; row++) {
            BlockPos support = anchor.relative(right, col).above(row).relative(face.getOpposite());
            if (start.getPieces().stream().noneMatch(piece -> piece.getBoundingBox().isInside(support))) return false;
        }
        return true;
    }
    private TarotWorldgen() {}
}
