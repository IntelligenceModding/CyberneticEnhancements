package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Prepares the site around the prefab: clears trees, eases the ground into the build and populates it. */
public final class ScorchedGroundPiece extends StructurePiece {
    public static final int RADIUS = 9;
    /** Trees closer than this to the build are cleared so no canopy or trunk clips into it. */
    private static final int CLEARING = 5;
    /** Leaves are cleared this far out, since a canopy reaches well past its trunk. */
    private static final int CANOPY = 8;
    /** How far the ground is reshaped around the build, blending from its floor into the natural surface. */
    private static final int BLEND = 8;
    private final BoundingBox core;
    /** Where the build's template was placed and how it was turned, which is what tells where its door is. */
    private final BlockPos origin;
    private final Rotation rotation;

    public ScorchedGroundPiece(BoundingBox core, BlockPos origin, Rotation rotation) {
        super(ModStructures.SCORCHED_GROUND_PIECE.get(), 0, core.inflatedBy(RADIUS));
        this.core = core;
        this.origin = origin;
        this.rotation = rotation;
    }

    public ScorchedGroundPiece(CompoundTag tag) {
        super(ModStructures.SCORCHED_GROUND_PIECE.get(), tag);
        this.core = BoundingBox.CODEC.parse(NbtOps.INSTANCE, tag.get("Core")).getOrThrow();
        this.rotation = tag.contains("Turn") ? Rotation.values()[Math.clamp(tag.getInt("Turn"), 0, Rotation.values().length - 1)] : Rotation.NONE;
        this.origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : new BlockPos(core.minX(), core.minY(), core.minZ());
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, core).ifSuccess(value -> tag.put("Core", value));
        tag.putLong("Origin", origin.asLong());
        tag.putInt("Turn", rotation.ordinal());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        writing.set(chunkPos);
        prepareSite(level, random, box);
        WorldEncounters.populate(level, origin, rotation, box);
        // Trees of a neighbouring chunk grow after this chunk is done and can reach over into it, so every chunk's pass also
        // clears the whole clearing around the build wherever the generation region allows writing, not just its own part.
        for (int x = core.minX() - CANOPY; x <= core.maxX() + CANOPY; x++) {
            for (int z = core.minZ() - CANOPY; z <= core.maxZ() + CANOPY; z++) {
                int dx = Math.max(Math.max(core.minX() - x, x - core.maxX()), 0);
                int dz = Math.max(Math.max(core.minZ() - z, z - core.maxZ()), 0);
                boolean own = x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ();
                boolean trunks = dx <= CLEARING && dz <= CLEARING && (own || dx > 0 || dz > 0); // Never the build's own logs in a chunk already built.
                if (own || canWrite(new BlockPos(x, core.minY(), z))) clearFoliage(level, x, z, trunks);
            }
        }
    }

    /**
     * Makes room before the build is placed: everything standing in its footprint (and one block around it) above its
     * lowest layer is cleared. Around it the ground is then eased between the build's floor and the natural surface with a
     * smooth, noisy slope, cutting hills down and filling hollows up, so the site sits in the land instead of in a square
     * pit or on a square plinth.
     */
    private void prepareSite(WorldGenLevel level, RandomSource random, BoundingBox box) {
        int top = Math.min(core.maxY() + 3, level.getMaxBuildHeight() - 1);
        int floor = core.minY() - 1;
        var pos = new BlockPos.MutableBlockPos();
        // What grows on the untouched land here, before anything is reshaped: new ground is dressed with the same plants at the same density.
        var palette = new java.util.ArrayList<BlockState>();
        int samples = 0;
        for (int x = Math.max(box.minX(), core.minX() - BLEND); x <= Math.min(box.maxX(), core.maxX() + BLEND); x++) {
            for (int z = Math.max(box.minZ(), core.minZ() - BLEND); z <= Math.min(box.maxZ(), core.maxZ() + BLEND); z++) {
                if (x >= core.minX() - 1 && x <= core.maxX() + 1 && z >= core.minZ() - 1 && z <= core.maxZ() + 1) continue;
                samples++;
                BlockState plant = level.getBlockState(pos.set(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), z));
                if (isGroundCover(plant) && !(plant.getBlock() instanceof net.minecraft.world.level.block.DoublePlantBlock
                        && plant.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF) != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER))
                    palette.add(plant);
            }
        }
        float density = samples == 0 ? 0 : (float) palette.size() / samples;
        for (int x = Math.max(box.minX(), core.minX() - BLEND); x <= Math.min(box.maxX(), core.maxX() + BLEND); x++) {
            for (int z = Math.max(box.minZ(), core.minZ() - BLEND); z <= Math.min(box.maxZ(), core.maxZ() + BLEND); z++) {
                int dx = Math.max(Math.max(core.minX() - x, x - core.maxX()), 0);
                int dz = Math.max(Math.max(core.minZ() - z, z - core.maxZ()), 0);
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (dx <= 1 && dz <= 1) {
                    for (int y = core.minY(); y <= top; y++) {
                        pos.set(x, y, z);
                        if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                double distance = Math.sqrt(dx * dx + dz * dz);
                double eased = distance == 0 ? 0 : Math.max(0, distance + wobble(x, z) * 1.5);
                if (eased >= BLEND) continue;
                int natural = ground - 1;
                double t = eased / BLEND;
                int target = (int) Math.round(floor + (natural - floor) * (t * t * (3 - 2 * t)));
                boolean ring = dx <= 1 && dz <= 1;
                if (target == natural && !ring) continue;
                // The land is made of what it is made of here: sand in a desert, terracotta in badlands, grass over dirt elsewhere.
                BlockState above = level.getBlockState(pos.set(x, natural + 1, z));
                // What grew here is put back; ground that had nothing, or was cleared around the build, gets the biome's plants at its density.
                BlockState plant = !ring && isGroundCover(above) ? above
                        : !palette.isEmpty() && random.nextFloat() < density ? palette.get(random.nextInt(palette.size())) : null;
                boolean cover = plant != null;
                BlockState surface = naturalSurface(level, x, natural, z, cover && plant.is(Blocks.SNOW));
                BlockState filler = naturalLayer(level, x, natural - 1, z, Blocks.DIRT.defaultBlockState());
                if (target == natural) {
                    // Only the ring around the build was cleared; its ground stays and just gets its plants back.
                } else if (target > natural) {
                    for (int y = Math.max(ground, level.getMinBuildHeight()); y <= target; y++) {
                        pos.set(x, y, z);
                        level.setBlock(pos, y == target ? surface : filler, 2);
                    }
                } else {
                    for (int y = natural; y > target; y--) {
                        pos.set(x, y, z);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                    level.setBlock(pos.set(x, target, z), surface, 2);
                }
                for (int y = natural + 1; y <= natural + 2; y++) { // Whatever grew on the old surface has nothing to stand on any more.
                    pos.set(x, y, z);
                    if (isGroundCover(level.getBlockState(pos))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                if (cover) restoreCover(level, pos.set(x, target + 1, z), plant);
            }
        }
    }

    /** Grass, flowers, petals, ferns, bushes, cacti and snow: the small things a biome scatters over its surface. */
    private static boolean isGroundCover(BlockState state) {
        return state.getBlock() instanceof BushBlock || state.getBlock() instanceof net.minecraft.world.level.block.SnowLayerBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.CactusBlock;
    }

    private static void restoreCover(WorldGenLevel level, BlockPos.MutableBlockPos pos, BlockState cover) {
        if (!level.getBlockState(pos).isAir() || !cover.canSurvive(level, pos)) return;
        if (cover.getBlock() instanceof net.minecraft.world.level.block.DoublePlantBlock) {
            if (cover.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF) != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                    || !level.getBlockState(pos.above()).isAir()) return;
            net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, cover, pos, 2);
        } else level.setBlock(pos, cover, 2);
    }

    /** The top of the land at this column, always something to walk on: where it would be bare dirt it is grass, so the site is never laid on dirt. */
    private static BlockState naturalSurface(WorldGenLevel level, int x, int y, int z, boolean snowy) {
        BlockState state = naturalLayer(level, x, y, z, Blocks.GRASS_BLOCK.defaultBlockState());
        if (state.is(Blocks.DIRT)) state = Blocks.GRASS_BLOCK.defaultBlockState();
        return state.hasProperty(net.minecraft.world.level.block.SnowyDirtBlock.SNOWY)
                ? state.setValue(net.minecraft.world.level.block.SnowyDirtBlock.SNOWY, snowy) : state;
    }

    /** The chunk whose generation is running on this thread. */
    private static final ThreadLocal<ChunkPos> writing = new ThreadLocal<>();
    /** How far from the chunk being generated a block may be written during the features step. */
    private static final int WRITE_RADIUS = 1;

    /** Whether the generation region allows writing here. Asking the region itself logs an error for every refusal, so this checks quietly. */
    private static boolean canWrite(BlockPos pos) {
        var centre = writing.get();
        return centre != null && Math.abs((pos.getX() >> 4) - centre.x) <= WRITE_RADIUS && Math.abs((pos.getZ() >> 4) - centre.z) <= WRITE_RADIUS;
    }

    /**
     * Smooth, slowly varying offset in -1..1 that makes the slope's edge irregular. It has to vary smoothly from block to
     * block: independent noise per column leaves neighbours at very different heights, which shows up as single-block pits.
     */
    private double wobble(int x, int z) {
        double a = origin.getX() * 0.37 + origin.getZ() * 0.61;
        return (Math.sin(x * 0.55 + z * 0.31 + a) + Math.sin(x * 0.23 - z * 0.47 + a * 1.7)) * 0.5;
    }

    /** The solid block the terrain really has at this height, or the fallback where there is none worth copying (water, plants, trees). */
    private static BlockState naturalLayer(WorldGenLevel level, int x, int y, int z, BlockState fallback) {
        if (y < level.getMinBuildHeight()) return fallback;
        var pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.isAir()
                || !state.isSolidRender(level, pos) && !state.is(Blocks.GRASS_BLOCK)) return fallback;
        return state;
    }

    private void clearFoliage(WorldGenLevel level, int x, int z, boolean trunks) {
        var pos = new BlockPos.MutableBlockPos();
        int top = Math.min(boundingBox.maxY() + 16, level.getMaxBuildHeight() - 1);
        // Start well below the build: on a slope a trunk's foot can sit lower than the build's lowest layer.
        for (int y = Math.max(level.getMinBuildHeight(), core.minY() - 10); y <= top; y++) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            boolean inside = core.isInside(pos); // The build's own logs stay; leaves and vines that crept into its bounds do not.
            if (state.is(BlockTags.LOGS)) {
                if (trunks && !inside) clearTree(level, pos.immutable());
            } else if (state.is(BlockTags.LEAVES)) {
                var trunk = trunkOf(level, pos.immutable());
                if (trunk != null) clearTree(level, trunk);
                else level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            } else if (state.is(Blocks.VINE)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    /** Follows a leaf back towards the trunk that holds it up, or null where it is not attached to one (a stray or planted leaf). */
    private BlockPos trunkOf(WorldGenLevel level, BlockPos leaf) {
        var at = leaf;
        for (int step = 0; step < 8; step++) {
            BlockPos next = null;
            int distance = level.getBlockState(at).hasProperty(net.minecraft.world.level.block.LeavesBlock.DISTANCE)
                    ? level.getBlockState(at).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE) : 7;
            for (int dx = -1; dx <= 1 && next == null; dx++) for (int dy = -1; dy <= 1 && next == null; dy++) for (int dz = -1; dz <= 1 && next == null; dz++) {
                var near = at.offset(dx, dy, dz);
                if (near.equals(at) || !canWrite(near)) continue;
                var state = level.getBlockState(near);
                if (state.is(BlockTags.LOGS)) return core.isInside(near) ? null : near;
                if (state.is(BlockTags.LEAVES) && state.hasProperty(net.minecraft.world.level.block.LeavesBlock.DISTANCE)
                        && state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE) < distance) next = near;
            }
            if (next == null) return null;
            at = next;
        }
        return null;
    }

    /** Removes a whole tree, trunk, branches and canopy, so nothing is left as a stump, a bare trunk or floating leaves. */
    private void clearTree(WorldGenLevel level, BlockPos start) {
        var queue = new java.util.ArrayDeque<BlockPos>();
        var seen = new java.util.HashSet<BlockPos>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < 4000) {
            var at = queue.poll();
            var state = level.getBlockState(at);
            boolean log = state.is(BlockTags.LOGS);
            int distance = state.hasProperty(net.minecraft.world.level.block.LeavesBlock.DISTANCE)
                    ? state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE) : 7;
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                var near = at.offset(dx, dy, dz);
                if (seen.contains(near) || Math.abs(near.getX() - start.getX()) > 14 || Math.abs(near.getZ() - start.getZ()) > 14
                        || !canWrite(near)) continue;
                var next = level.getBlockState(near);
                boolean follow = log && next.is(BlockTags.LOGS) && !core.isInside(near)
                        || next.is(BlockTags.LEAVES) && next.hasProperty(net.minecraft.world.level.block.LeavesBlock.DISTANCE)
                        && next.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE) == (log ? 1 : distance + 1);
                if (follow) { seen.add(near); queue.add(near); }
            }
        }
    }
}
