package de.artemis.cyberneticenhancements.common.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.artemis.cyberneticenhancements.common.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.List;
import java.util.Optional;

/** A single jigsaw prefab, lifted one block above the terrain and ringed by scorched ground. */
public final class MedTecAvStructure extends Structure {
    public static final MapCodec<MedTecAvStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool)
    ).apply(instance, MedTecAvStructure::new));
    private static final int LIFT = 2, MAX_SLOPE = 3;
    private final Holder<StructureTemplatePool> startPool;

    public MedTecAvStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool) {
        super(settings);
        this.startPool = startPool;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        BlockPos origin = new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ());
        return JigsawPlacement.addPieces(context, startPool, Optional.empty(), 1, origin, false,
                        Optional.empty(), 80, PoolAliasLookup.create(List.of(), origin, context.seed()),
                        DimensionPadding.ZERO, LiquidSettings.APPLY_WATERLOGGING)
                .filter(stub -> isLevelEnough(context, stub.getPiecesBuilder().getBoundingBox()))
                .map(stub -> new GenerationStub(stub.position(), builder -> {
                    var inner = stub.getPiecesBuilder();
                    inner.offsetPiecesVertically(groundOffset(context, inner.getBoundingBox()));
                    BoundingBox core = inner.getBoundingBox();
                    var pieces = inner.build().pieces();
                    var build = pieces.stream().filter(PoolElementStructurePiece.class::isInstance).map(PoolElementStructurePiece.class::cast).findFirst();
                    builder.addPiece(new ScorchedGroundPiece(core, build.map(PoolElementStructurePiece::getPosition).orElse(new BlockPos(core.minX(), core.minY(), core.minZ())),
                            build.map(PoolElementStructurePiece::getRotation).orElse(Rotation.NONE))); // First, so it clears the site before the build goes in.
                    pieces.forEach(builder::addPiece);
                }));
    }

    private static int[] footprintHeights(GenerationContext context, BoundingBox box) {
        int[] heights = new int[25];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                int x = box.minX() + (box.getXSpan() - 1) * i / 4;
                int z = box.minZ() + (box.getZSpan() - 1) * j / 4;
                heights[i * 5 + j] = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                        context.heightAccessor(), context.randomState());
            }
        }
        java.util.Arrays.sort(heights);
        return heights;
    }

    /** Only builds where the ground is already close to flat and dry, so little has to be cleared or filled. */
    private static boolean isLevelEnough(GenerationContext context, BoundingBox box) {
        int[] heights = footprintHeights(context, box);
        return heights[24] - heights[0] <= MAX_SLOPE && heights[0] > context.chunkGenerator().getSeaLevel();
    }

    /** Sits on the middle height of the footprint; the site is levelled around it when it is placed. */
    private static int groundOffset(GenerationContext context, BoundingBox box) {
        return footprintHeights(context, box)[12] + LIFT - 1 - box.minY();
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.MED_TEC_AV.get();
    }
}
