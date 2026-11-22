package de.artemis.cyberneticenhancements.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A decorative square sheet-metal HVAC duct that joins the ducts next to it, and the vents set on its ends, by itself. */
public class DuctBlock extends Block implements SimpleWaterloggedBlock, CuttableJoin {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private final VoxelShape[] shapes = new VoxelShape[64];

    public DuctBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(WATERLOGGED, false);
        for (Direction direction : Direction.values()) state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false).setValue(CUT.get(direction), false);
        registerDefaultState(state);
        for (int bits = 0; bits < 64; bits++) {
            VoxelShape shape = CORE;
            for (Direction direction : Direction.values()) if ((bits & 1 << direction.ordinal()) != 0) shape = Shapes.or(shape, arm(direction));
            shapes[bits] = shape;
        }
    }

    private static VoxelShape arm(Direction direction) {
        double[] from = {4, 4, 4}, to = {12, 12, 12};
        int axis = direction.getAxis().ordinal();
        if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) to[axis] = 16;
        else from[axis] = 0;
        return Block.box(from[0], from[1], from[2], to[0], to[1], to[2]);
    }

    /** A duct joins another duct, and a vent whose back is turned to it (the vent faces away from the duct). */
    private static boolean joins(BlockState state, Direction direction, BlockState neighbour) {
        return !state.getValue(CUT.get(direction)) && (neighbour.getBlock() instanceof DuctBlock && !neighbour.getValue(CUT.get(direction.getOpposite()))
                || neighbour.getBlock() instanceof VentBlock && neighbour.getValue(VentBlock.FACING) == direction);
    }

    @Override
    public boolean couldJoin(BlockState state, Direction direction, BlockState neighbour) {
        return neighbour.getBlock() instanceof DuctBlock || neighbour.getBlock() instanceof VentBlock && neighbour.getValue(VentBlock.FACING) == direction;
    }

    @Override
    public BlockState reconnect(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values())
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), joins(state, direction, level.getBlockState(pos.relative(direction))));
        return state;
    }

    @Override
    public double low() { return 4; }

    @Override
    public double high() { return 12; }

    @Override
    public InteractionResult useBuildingGadget(UseOnContext context) { return cutOrMend(context); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return reconnect(context.getLevel(), context.getClickedPos(),
                defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), joins(state, direction, neighbour));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int bits = 0;
        for (Direction direction : Direction.values()) if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) bits |= 1 << direction.ordinal();
        return shapes[bits];
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        for (Direction direction : Direction.values()) builder.add(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), CUT.get(direction));
    }
}
