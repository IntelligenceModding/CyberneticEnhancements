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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The ground a container lies on: it is raised up to the hull's floor beneath it, and eased from there into the natural surface with a
 * smooth, noisy slope, so the container sits in the land instead of on a square plinth. The ground keeps what it is made of here, so a
 * gravel sea floor stays gravel and a beach stays sand.
 */
public final class ShippingContainerGroundPiece extends StructurePiece {
    /** How far the ground is reshaped around the container. */
    private static final int BLEND = 6;
    private final BoundingBox core;
    private final long seed;

    public ShippingContainerGroundPiece(BoundingBox core, long seed) {
        super(ModStructures.SHIPPING_CONTAINER_GROUND_PIECE.get(), 0, core.inflatedBy(BLEND + 1));
        this.core = core;
        this.seed = seed;
    }

    public ShippingContainerGroundPiece(CompoundTag tag) {
        super(ModStructures.SHIPPING_CONTAINER_GROUND_PIECE.get(), tag);
        this.core = BoundingBox.CODEC.parse(NbtOps.INSTANCE, tag.get("Core")).getOrThrow();
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, core).ifSuccess(value -> tag.put("Core", value));
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int sea = generator.getSeaLevel();
        int floor = core.minY() - 1;
        var pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), core.minX() - BLEND); x <= Math.min(box.maxX(), core.maxX() + BLEND); x++) {
            for (int z = Math.max(box.minZ(), core.minZ() - BLEND); z <= Math.min(box.maxZ(), core.maxZ() + BLEND); z++) {
                int dx = Math.max(Math.max(core.minX() - x, x - core.maxX()), 0);
                int dz = Math.max(Math.max(core.minZ() - z, z - core.maxZ()), 0);
                double distance = Math.sqrt(dx * dx + dz * dz);
                double eased = distance == 0 ? 0 : Math.max(0, distance + wobble(x, z) * 1.5);
                if (eased >= BLEND) continue;
                int natural = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                double t = eased / BLEND;
                int target = (int) Math.round(floor + (natural - floor) * (t * t * (3 - 2 * t)));
                if (target == natural) continue;
                BlockState surface = naturalLayer(level, x, natural, z, Blocks.SAND.defaultBlockState());
                // Sand and gravel are the same all the way down; anything else sits on its usual filling.
                BlockState filler = surface.is(BlockTags.SAND) || surface.is(Blocks.GRAVEL) ? surface
                        : naturalLayer(level, x, natural - 1, z, Blocks.DIRT.defaultBlockState());
                if (target > natural) {
                    for (int y = Math.max(natural + 1, level.getMinBuildHeight()); y <= target; y++)
                        level.setBlock(pos.set(x, y, z), y == target ? surface : filler, 2);
                } else {
                    for (int y = natural; y > target; y--) level.setBlock(pos.set(x, y, z), open(y, sea), 2);
                    level.setBlock(pos.set(x, target, z), surface, 2);
                }
                for (int y = natural + 1; y <= natural + 2; y++) { // Whatever grew on the old surface has nothing to stand on any more.
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);
                    if (target < natural && (state.getBlock() instanceof BushBlock || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)))
                        level.setBlock(pos, open(y, sea), 2);
                }
            }
        }
    }

    private static BlockState open(int y, int sea) {
        return y < sea ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    /** Smooth offset in -1..1 that makes the slope's edge irregular; it must vary smoothly or neighbouring columns end up at very different heights. */
    private double wobble(int x, int z) {
        double a = (seed & 0xFFFF) * 0.0003 + (seed >>> 16 & 0xFFFF) * 0.0007;
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
}
