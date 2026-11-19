package de.artemis.cyberneticenhancements.common.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;

final class DimensionalQuestSites {
    private static final int CENTRAL_ATTEMPTS = 8, MAX_ATTEMPTS = 20;
    private static final Map<ServerLevel, Map<String, Search>> SEARCHES = new WeakHashMap<>();

    record Result(BlockPos pos, boolean preparing) {}

    private static final class Search {
        int attempt;
        long touched;
    }

    private DimensionalQuestSites() {}

    static Result find(ServerLevel level, ServerLevel issuerLevel, BlockPos issuerPos, String contractId, long seed, int clearance) {
        long now = level.getGameTime();
        var searches = SEARCHES.computeIfAbsent(level, ignored -> new HashMap<>());
        searches.values().removeIf(search -> now - search.touched > 6000);
        var search = searches.computeIfAbsent(contractId, ignored -> new Search());
        search.touched = now;
        var reservations = QuestSiteReservations.get(level);
        boolean end = level.dimension() == Level.END;
        double scale = DimensionType.getTeleportationScale(issuerLevel.dimensionType(), level.dimensionType());
        for (; search.attempt < MAX_ATTEMPTS; search.attempt++) {
            var random = RandomSource.create(seed + search.attempt * 341873128712L);
            double angle = random.nextDouble() * Math.PI * 2;
            boolean centralIsland = end && search.attempt < CENTRAL_ATTEMPTS;
            int distance = !end ? 128 + random.nextInt(280)
                    : centralIsland ? 58 + random.nextInt(23) : 1150 + random.nextInt(950);
            int x = Mth.floor((end ? 0 : issuerPos.getX() * scale) + Math.cos(angle) * distance);
            int z = Mth.floor((end ? 0 : issuerPos.getZ() * scale) + Math.sin(angle) * distance);
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 64, z))) continue;
            if (!QuestTerrainPreparation.ready(level, x, z)) return new Result(null, true);
            var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) return new Result(null, true);
            int chunkX = x & ~15, chunkZ = z & ~15;
            for (int dx = 2; dx < 14; dx += 3) {
                for (int dz = 2; dz < 14; dz += 3) {
                    int px = chunkX + dx, pz = chunkZ + dz;
                    if (centralIsland && px * px + pz * pz < 48 * 48) continue;
                    var column = new BlockPos(px, 64, pz);
                    if (!reservations.available(column, clearance, contractId, now)) continue;
                    int high = end ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz) + 1
                            : Math.min(level.dimensionType().logicalHeight() - 8, level.getMaxBuildHeight() - 3);
                    int low = end ? high : Math.max(33, level.getMinBuildHeight() + 2);
                    for (int y = high; y >= low; y--) {
                        var pos = column.atY(y);
                        if (usable(level, pos)) {
                            searches.remove(contractId);
                            return new Result(pos, false);
                        }
                    }
                }
            }
        }
        return new Result(null, false);
    }

    static boolean usable(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos.below()) || !level.isInWorldBounds(pos.above(2))
                || !level.hasChunksAt(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) return false;
        if (level.dimension() == Level.NETHER && pos.getY() >= level.dimensionType().logicalHeight() - 4) return false;
        for (var foot : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            if (!level.getWorldBorder().isWithinBounds(foot)) return false;
            var below = foot.below();
            var ground = level.getBlockState(below);
            if (ground.is(Blocks.BEDROCK) || ground.is(Blocks.MAGMA_BLOCK) || !ground.getFluidState().isEmpty()
                    || !ground.isFaceSturdy(level, below, Direction.UP)) return false;
            if (!level.getBlockState(foot).isAir() || !level.getBlockState(foot.above()).isAir()
                    || !level.getBlockState(foot.above(2)).isAir()) return false;
        }
        return true;
    }

    static BlockPos nearbySpawn(ServerLevel level, BlockPos column) {
        for (int offset = 0; offset <= 8; offset++) {
            var above = column.above(offset);
            if (usable(level, above)) return above;
            if (offset > 0) {
                var below = column.below(offset);
                if (usable(level, below)) return below;
            }
        }
        return null;
    }

    static BlockPos dossierSite(ServerLevel level, BlockPos death, BlockPos objective) {
        for (var offset : BlockPos.spiralAround(death, 4, Direction.EAST, Direction.SOUTH)) {
            var pos = nearbySpawn(level, offset);
            if (pos != null) return pos;
        }
        return objective == null ? death : objective;
    }

    static BlockPos surveyApproach(ServerLevel level, BlockPos source) {
        for (int distance : new int[]{32, 48, 64}) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                var approach = nearbySpawn(level, source.relative(direction, distance));
                if (approach != null) return approach;
            }
        }
        return source;
    }
}
