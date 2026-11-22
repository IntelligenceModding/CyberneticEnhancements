package de.artemis.cyberneticenhancements.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A decorative slatted vent: a thin grille on the side of its block that was clicked, facing out, that ducts join at its back. */
public class VentBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        SHAPES[Direction.NORTH.ordinal()] = Block.box(0, 0, 14, 16, 16, 16);
        SHAPES[Direction.SOUTH.ordinal()] = Block.box(0, 0, 0, 16, 16, 2);
        SHAPES[Direction.WEST.ordinal()] = Block.box(14, 0, 0, 16, 16, 16);
        SHAPES[Direction.EAST.ordinal()] = Block.box(0, 0, 0, 2, 16, 16);
        SHAPES[Direction.UP.ordinal()] = Block.box(0, 0, 0, 16, 2, 16);
        SHAPES[Direction.DOWN.ordinal()] = Block.box(0, 14, 0, 16, 16, 16);
    }

    public VentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
