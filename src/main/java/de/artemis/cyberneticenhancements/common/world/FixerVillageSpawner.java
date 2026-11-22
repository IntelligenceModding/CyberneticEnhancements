package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.fasttravel.FastTravelPoints;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

public final class FixerVillageSpawner {
    private static final ResourceLocation NIGHT_CITY_STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath("cyberneticenhancements", "night_city");
    private static final DeferredWorkQueue<ServerLevel, LevelChunk> PENDING_CHUNKS = new DeferredWorkQueue<>();

    private FixerVillageSpawner() {
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        PENDING_CHUNKS.add(serverLevel, chunk);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        List<LevelChunk> pending = PENDING_CHUNKS.takeBatch(serverLevel, 4);
        if (pending.isEmpty()) {
            return;
        }

        List<LevelChunk> deferred = new ArrayList<>();
        for (LevelChunk chunk : pending) {
            if (serverLevel.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) != chunk) continue;
            if (!chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) {
                deferred.add(chunk);
                continue;
            }
            cleanupVanillaVillagePopulation(serverLevel, chunk);
            replaceBells(serverLevel, chunk);
            trySpawnNpcs(serverLevel, chunk);
        }

        PENDING_CHUNKS.defer(serverLevel, deferred);
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PENDING_CHUNKS.clear(serverLevel);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Villager || entity instanceof Cat || entity instanceof IronGolem)) {
            return;
        }
        if (isInsideNightCity(serverLevel, entity.blockPosition())) {
            entity.discard();
            event.setCanceled(true);
        }
    }

    private static void trySpawnNpcs(ServerLevel level, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : level.structureManager().startsForStructure(chunkPos, structure -> true)) {
            ResourceLocation structureId = structureRegistry.getKey(start.getStructure());
            if (!NIGHT_CITY_STRUCTURE_ID.equals(structureId)) {
                continue;
            }
            NightCityResidents.get(level).register(start.getBoundingBox());
            break;
        }
    }

    /** Every city has a fast travel terminal where the village would hang its bell. */
    private static void replaceBells(ServerLevel level, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : level.structureManager().startsForStructure(chunkPos, structure -> true)) {
            if (!NIGHT_CITY_STRUCTURE_ID.equals(structureRegistry.getKey(start.getStructure()))) {
                continue;
            }
            var box = start.getBoundingBox();
            int minX = Math.max(box.minX(), chunkPos.getMinBlockX()), maxX = Math.min(box.maxX(), chunkPos.getMaxBlockX());
            int minZ = Math.max(box.minZ(), chunkPos.getMinBlockZ()), maxZ = Math.min(box.maxZ(), chunkPos.getMaxBlockZ());
            int minY = Math.max(box.minY(), level.getMinBuildHeight()), maxY = Math.min(box.maxY(), level.getMaxBuildHeight() - 1);
            var bells = new ArrayList<BlockPos>();
            for (int y = minY; y <= maxY; y++) {
                if (!chunk.getSection(chunk.getSectionIndex(y)).maybeHas(state -> state.is(Blocks.BELL))) {
                    y |= 15;
                    continue;
                }
                for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                    var pos = new BlockPos(x, y, z);
                    if (chunk.getBlockState(pos).is(Blocks.BELL)) bells.add(pos);
                }
            }
            for (BlockPos bell : bells) {
                // The bell hangs from a frame, so the terminal stands on the ground beneath it.
                BlockPos ground = bell;
                for (int i = 0; i < 8 && level.getBlockState(ground.below()).getCollisionShape(level, ground.below()).isEmpty(); i++) {
                    ground = ground.below();
                }
                level.setBlock(bell, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(ground, ModBlocks.FAST_TRAVEL.get().defaultBlockState(), 3);
                FastTravelPoints.get(level.getServer()).add(level.dimension().location().toString(), ground, null);
            }
            break;
        }
    }

    private static void cleanupVanillaVillagePopulation(ServerLevel level, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : level.structureManager().startsForStructure(chunkPos, structure -> true)) {
            ResourceLocation structureId = structureRegistry.getKey(start.getStructure());
            if (!NIGHT_CITY_STRUCTURE_ID.equals(structureId) || !start.getChunkPos().equals(chunkPos)) {
                continue;
            }
            AABB bounds = AABB.of(start.getBoundingBox());
            for (Villager villager : level.getEntitiesOfClass(Villager.class, bounds)) {
                villager.discard();
            }
            for (Cat cat : level.getEntitiesOfClass(Cat.class, bounds)) {
                cat.discard();
            }
            for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, bounds)) {
                golem.discard();
            }
            break;
        }
    }

    private static boolean isInsideNightCity(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : level.structureManager().startsForStructure(chunkPos, structure -> true)) {
            ResourceLocation structureId = structureRegistry.getKey(start.getStructure());
            if (!NIGHT_CITY_STRUCTURE_ID.equals(structureId)) {
                continue;
            }
            if (level.structureManager().structureHasPieceAt(pos, start)) {
                return true;
            }
        }
        return false;
    }

}
