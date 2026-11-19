package de.artemis.cyberneticenhancements.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.cyberneticenhancements.common.blockentity.HoloBarrierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Anchors a holographic barrier across a doorway. The block itself is invisible, cannot be touched, hit or broken and
 * does not stop anyone; its renderer draws the projection. It stands in the doorway block on the left as seen from the
 * side it faces, and the projection spans that block and the one to its right.
 */
public final class HoloBarrierBlock extends BaseEntityBlock {
    public static final MapCodec<HoloBarrierBlock> CODEC = simpleCodec(HoloBarrierBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public HoloBarrierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    /** The projection's size in blocks: it spans this block and the next one along, and stands two blocks tall. */
    private static final double SPAN = 2, HEIGHT = 2, THICKNESS = .3;

    /** Whether this body is passing through the projection of any barrier right now, in the doorway itself. */
    public static boolean isCrossing(Level level, AABB body) {
        var search = body.inflate(1, 0, 1);
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(search.minX, search.minY - 1, search.minZ),
                BlockPos.containing(search.maxX, search.maxY, search.maxZ))) {
            var state = level.getBlockState(pos);
            if (!state.is(de.artemis.cyberneticenhancements.common.registry.ModBlocks.HOLO_BARRIER.get())) continue;
            var forward = state.getValue(FACING);
            var along = forward.getCounterClockWise();
            var centre = Vec3.atCenterOf(pos);
            // The panel as a thin box: from the block's outer edge, SPAN blocks along, around the plane through its middle.
            double start = -.5, end = start + SPAN;
            var corner = centre.add(along.getStepX() * start, -.5, along.getStepZ() * start)
                    .add(forward.getStepX() * -THICKNESS, 0, forward.getStepZ() * -THICKNESS);
            var far = centre.add(along.getStepX() * end, -.5 + HEIGHT, along.getStepZ() * end)
                    .add(forward.getStepX() * THICKNESS, 0, forward.getStepZ() * THICKNESS);
            if (body.intersects(new AABB(corner, far))) return true;
        }
        return false;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HoloBarrierBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) { return true; }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
}
