package de.artemis.cyberneticenhancements.common.world;

import com.mojang.serialization.MapCodec;
import de.artemis.cyberneticenhancements.common.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A freight container that ended up on a beach or the sea floor: intact, snapped in half or holed, flooded or sealed. */
public final class ShippingContainerStructure extends Structure {
    public static final MapCodec<ShippingContainerStructure> CODEC = simpleCodec(ShippingContainerStructure::new);
    /** The most the ground may rise or dip over the footprint; the container is propped up on the low side. */
    private static final int MAX_SLOPE = 6;
    /** A beach is only a few blocks above the water; anything higher is hillside, not shore. */
    private static final int MAX_SHORE = 4;
    /** A cargo is hidden in this share of the containers that carry barrels. */
    private static final double CARGO_CHANCE = 0.35;

    public ShippingContainerStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        var random = context.random();
        int centreX = chunk.getMiddleBlockX(), centreZ = chunk.getMiddleBlockZ();
        Rotation rotation = Rotation.getRandom(random);
        boolean turned = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        int halfX = turned ? 13 : 3, halfZ = turned ? 3 : 13;
        int[] heights = new int[25];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                heights[i * 5 + j] = context.chunkGenerator().getFirstFreeHeight(centreX - halfX + 2 * halfX * i / 4,
                        centreZ - halfZ + 2 * halfZ * j / 4, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            }
        }
        java.util.Arrays.sort(heights);
        int sea = context.chunkGenerator().getSeaLevel();
        if (heights[24] - heights[0] > MAX_SLOPE || heights[24] > sea + MAX_SHORE) return Optional.empty();
        int ground = heights[12];
        boolean underwater = ground < sea - 2;
        var damage = switch (random.nextInt(20)) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8 -> ShippingContainerPiece.Damage.INTACT;
            case 9, 10, 11, 12, 13 -> ShippingContainerPiece.Damage.SPLIT;
            default -> ShippingContainerPiece.Damage.HOLE;
        };
        int roll = random.nextInt(100) < 20 ? (random.nextBoolean() ? 1 : -1) : 0; // Some have toppled over onto their side.
        var cargo = ShippingContainerPiece.Cargo.values()[random.nextInt(ShippingContainerPiece.Cargo.values().length)];
        boolean flooded = underwater && (damage != ShippingContainerPiece.Damage.INTACT || random.nextInt(100) < 45);
        var growth = ShippingContainerPiece.Growth.values()[random.nextInt(ShippingContainerPiece.Growth.values().length)];
        if (growth == ShippingContainerPiece.Growth.SEAGRASS && !flooded) growth = ShippingContainerPiece.Growth.MOSS; // Seagrass needs the water.
        // Sits on the sea floor; on a beach it settles a block into the sand.
        int floor = ground < sea ? ground : ground - 1;
        BlockPos position = new BlockPos(centreX, floor, centreZ).subtract(ShippingContainerPiece.PIVOT).atY(floor);
        var piece = new ShippingContainerPiece(context.structureTemplateManager(), 1 + random.nextInt(ShippingContainerPiece.TEMPLATES),
                rotation, position, random.nextLong(), damage, cargo, growth, flooded, random.nextDouble() < CARGO_CHANCE, roll);
        long groundSeed = random.nextLong();
        return Optional.of(new GenerationStub(new BlockPos(centreX, floor, centreZ), builder -> {
            builder.addPiece(new ShippingContainerGroundPiece(piece.getBoundingBox(), groundSeed));
            builder.addPiece(piece);
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SHIPPING_CONTAINER.get();
    }
}
