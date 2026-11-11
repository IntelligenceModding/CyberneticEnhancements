package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class RelicCacheWorldgen {
    private static final Map<ResourceLocation, PlacementRule> RULES = Map.ofEntries(
            Map.entry(ResourceLocation.withDefaultNamespace("igloo"), new PlacementRule(ModBlocks.UNCOMMON_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("jungle_pyramid"), new PlacementRule(ModBlocks.UNCOMMON_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("pillager_outpost"), new PlacementRule(ModBlocks.UNCOMMON_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("desert_pyramid"), new PlacementRule(ModBlocks.RARE_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("woodland_mansion"), new PlacementRule(ModBlocks.RARE_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("stronghold"), new PlacementRule(ModBlocks.RARE_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("trial_chambers"), new PlacementRule(ModBlocks.EPIC_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("ancient_city"), new PlacementRule(ModBlocks.EPIC_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("bastion_remnant"), new PlacementRule(ModBlocks.EPIC_RELIC_CACHE.get())),
            Map.entry(ResourceLocation.withDefaultNamespace("end_city"), new PlacementRule(ModBlocks.LEGENDARY_RELIC_CACHE.get()))
    );
    private static final DeferredWorkQueue<ServerLevel, LevelChunk> PENDING_CHUNKS = new DeferredWorkQueue<>();

    private RelicCacheWorldgen() {
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

        List<LevelChunk> pending = PENDING_CHUNKS.takeBatch(serverLevel, 8);
        if (pending.isEmpty()) {
            return;
        }

        List<LevelChunk> deferred = new ArrayList<>();
        for (LevelChunk chunk : pending) {
            if (!chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) {
                deferred.add(chunk);
                continue;
            }
            if (serverLevel.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) != chunk) {
                continue;
            }
            tryPlaceCaches(serverLevel, chunk);
        }

        PENDING_CHUNKS.defer(serverLevel, deferred);
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PENDING_CHUNKS.clear(serverLevel);
        }
    }

    static void tryPlaceCaches(ServerLevel level, LevelChunk chunk) {
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        RelicCacheStructurePlacementSavedData savedData = RelicCacheStructurePlacementSavedData.get(level);
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (chunk.getBlockState(pos).getBlock() instanceof RelicCacheBlock cache) {
                savedData.register(pos, cache.tier());
            }
        }
        ChunkPos chunkPos = chunk.getPos();

        for (StructureStart start : level.structureManager().startsForStructure(chunkPos, structure -> true)) {
            ResourceLocation structureId = structureRegistry.getKey(start.getStructure());
            if (structureId == null) {
                continue;
            }

            PlacementRule rule = RULES.get(structureId);
            if (rule == null) {
                continue;
            }

            String placementKey = structurePlacementKey(structureId, start);
            if (savedData.isPlaced(placementKey)) {
                continue;
            }

            BlockPos placementPos = findPlacementCandidate(level, chunk, start);
            if (placementPos == null) {
                continue;
            }

            placeCache(level, placementPos, rule.block());
            savedData.register(placementPos, rule.block().tier());
            savedData.markPlaced(placementKey);
        }
    }

    private static BlockPos findPlacementCandidate(ServerLevel level, LevelChunk chunk, StructureStart start) {
        BoundingBox box = start.getBoundingBox();
        BlockPos center = box.getCenter();

        return chunk.getBlockEntitiesPos().stream()
                .filter(pos -> start.getPieces().stream().anyMatch(piece -> isInsideCacheSite(piece, pos)))
                .filter(pos -> isEligibleContainer(chunk.getBlockState(pos), chunk.getBlockEntity(pos)))
                .min(Comparator
                        .comparingInt((BlockPos pos) -> horizontalDistanceSquared(pos, center))
                        .thenComparingInt(BlockPos::getY))
                .orElse(null);
    }

    static boolean isInsideCacheSite(StructurePiece piece, BlockPos pos) {
        BoundingBox box = piece.getBoundingBox();
        int basementDepth = piece instanceof DesertPyramidPiece ? 11 : piece instanceof JungleTemplePiece ? 3 : 0;
        return pos.getX() >= box.minX() && pos.getX() <= box.maxX()
                && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ()
                && pos.getY() >= box.minY() - basementDepth && pos.getY() <= box.maxY();
    }

    private static void placeCache(ServerLevel level, BlockPos pos, RelicCacheBlock cacheBlock) {
        BlockState oldState = level.getBlockState(pos);
        Direction facing = oldState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? oldState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        BlockState cacheState = cacheBlock.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.setRemoved();
        }
        level.setBlock(pos, cacheState, Block.UPDATE_ALL);
    }

    private static boolean isSupportedContainer(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL;
    }

    static boolean isEligibleContainer(BlockState state, BlockEntity entity) {
        return isSupportedContainer(state)
                && (!state.hasProperty(BlockStateProperties.CHEST_TYPE)
                    || state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.SINGLE)
                && entity instanceof RandomizableContainerBlockEntity container
                && container.getLootTable() != null;
    }

    private static int horizontalDistanceSquared(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static String structurePlacementKey(ResourceLocation structureId, StructureStart start) {
        ChunkPos chunkPos = start.getChunkPos();
        return structureId + "|" + chunkPos.x + "|" + chunkPos.z;
    }

    private record PlacementRule(RelicCacheBlock block) {
    }
}
