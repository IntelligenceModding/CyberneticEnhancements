package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.Heightmap;
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
    private static final List<SpawnDefinition> NPC_SPAWNS = List.of(
            new SpawnDefinition(ModEntityTypes.FIXER.get(), 0.0D, -4.0D),
            new SpawnDefinition(ModEntityTypes.RIPPERDOC.get(), 6.0D, -2.0D),
            new SpawnDefinition(ModEntityTypes.TECHIE.get(), -6.0D, -2.0D),
            new SpawnDefinition(ModEntityTypes.NETRUNNER.get(), 4.0D, 5.0D),
            new SpawnDefinition(ModEntityTypes.MERC.get(), -4.0D, 5.0D)
    );

    private FixerVillageSpawner() {
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel serverLevel) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        PENDING_CHUNKS.add(serverLevel, chunk);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        List<LevelChunk> pending = PENDING_CHUNKS.takeBatch(serverLevel);
        if (pending.isEmpty()) {
            return;
        }

        List<LevelChunk> deferred = new ArrayList<>();
        for (LevelChunk chunk : pending) {
            if (!chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) {
                deferred.add(chunk);
                continue;
            }
            cleanupVanillaVillagePopulation(serverLevel, chunk);
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
            if (!start.getChunkPos().equals(chunkPos)) {
                continue;
            }
            BlockPos meetingPoint = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start.getBoundingBox().getCenter());
            for (SpawnDefinition definition : NPC_SPAWNS) {
                if (hasNpcOfType(level, start, definition)) {
                    continue;
                }
                BlockPos target = meetingPoint.offset(
                        (int) Math.round(definition.offsetX()),
                        0,
                        (int) Math.round(definition.offsetZ())
                );
                BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target).above();
                AbstractCityNpcEntity npc = definition.entityType().create(level);
                if (npc == null) {
                    continue;
                }
                npc.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                npc.setHome(meetingPoint);
                npc.setPersistenceRequired();
                npc.ensureIdentity();
                level.addFreshEntity(npc);
            }
            break;
        }
    }

    private static boolean hasNpcOfType(ServerLevel level, StructureStart start, SpawnDefinition definition) {
        return !level.getEntitiesOfClass(
                AbstractCityNpcEntity.class,
                AABB.of(start.getBoundingBox()).inflate(16.0D),
                npc -> npc.getType() == definition.entityType()
        ).isEmpty();
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

    private record SpawnDefinition(net.minecraft.world.entity.EntityType<? extends AbstractCityNpcEntity> entityType, double offsetX, double offsetZ) {
    }
}
