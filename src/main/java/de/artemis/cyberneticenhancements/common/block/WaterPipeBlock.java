package de.artemis.cyberneticenhancements.common.block;

import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A decorative iron water pipe that joins the pipes next to it by itself, in the colour of a cauldron. A red shutoff valve can be
 * fitted in the middle of any pipe, its handwheel standing out of one side that has no pipe on it.
 */
public class WaterPipeBlock extends Block implements SimpleWaterloggedBlock, CuttableJoin {
    /** Which side the fitted valve's handwheel points to, if there is a valve. */
    public enum Valve implements StringRepresentable {
        NONE(null), DOWN(Direction.DOWN), UP(Direction.UP), NORTH(Direction.NORTH), SOUTH(Direction.SOUTH), WEST(Direction.WEST), EAST(Direction.EAST);

        private final Direction direction;

        Valve(Direction direction) { this.direction = direction; }

        public Direction direction() { return direction; }

        public static Valve of(Direction direction) { return values()[direction.ordinal() + 1]; }

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Valve> VALVE = EnumProperty.create("valve", Valve.class);
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private final VoxelShape[][] shapes = new VoxelShape[64][Valve.values().length];

    public WaterPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(WATERLOGGED, false).setValue(VALVE, Valve.NONE);
        for (Direction direction : Direction.values()) state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false).setValue(CUT.get(direction), false);
        registerDefaultState(state);
        for (int bits = 0; bits < 64; bits++) {
            VoxelShape pipe = CORE;
            for (Direction direction : Direction.values())
                if ((bits & 1 << direction.ordinal()) != 0) pipe = Shapes.or(pipe, arm(direction));
            for (Valve valve : Valve.values()) shapes[bits][valve.ordinal()] = valve == Valve.NONE ? pipe : Shapes.or(pipe, valveShape(valve.direction()));
        }
    }

    /** The tube from the middle out to the edge of the block in the direction. */
    private static VoxelShape arm(Direction direction) {
        double[] from = {5, 5, 5}, to = {11, 11, 11};
        int axis = direction.getAxis().ordinal();
        if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) to[axis] = 16;
        else from[axis] = 0;
        return Block.box(from[0], from[1], from[2], to[0], to[1], to[2]);
    }

    /** The valve: a housing round the middle of the pipe, and a stem and handwheel standing out of the side it points to. */
    private static VoxelShape valveShape(Direction facing) {
        VoxelShape shape = Block.box(4, 4, 4, 12, 12, 12);
        // The stem and wheel are drawn standing up; this stands them on the side the valve points to.
        double[][] parts = {{7, 12, 7, 9, 14, 9}, {4, 14, 4, 12, 15, 12}, {7, 15, 7, 9, 16, 9}};
        for (double[] part : parts) {
            double[] low = new double[3], high = new double[3];
            for (int corner = 0; corner < 2; corner++) {
                double x = corner == 0 ? part[0] : part[3], y = corner == 0 ? part[1] : part[4], z = corner == 0 ? part[2] : part[5];
                double[] mapped = switch (facing) {
                    case UP -> new double[]{x, y, z};
                    case DOWN -> new double[]{x, 16 - y, z};
                    case NORTH -> new double[]{x, z, 16 - y};
                    case SOUTH -> new double[]{x, z, y};
                    case EAST -> new double[]{y, z, x};
                    case WEST -> new double[]{16 - y, z, x};
                };
                for (int axis = 0; axis < 3; axis++) {
                    low[axis] = corner == 0 ? mapped[axis] : Math.min(low[axis], mapped[axis]);
                    high[axis] = corner == 0 ? mapped[axis] : Math.max(high[axis], mapped[axis]);
                }
            }
            shape = Shapes.or(shape, Block.box(low[0], low[1], low[2], high[0], high[1], high[2]));
        }
        return shape;
    }

    /** Whether a pipe on the block beside it, which meets this one on {@code face}, joins: any pipe whose valve does not stand on that side. */
    private static boolean connects(BlockState neighbour, Direction face) {
        return neighbour.getBlock() instanceof WaterPipeBlock && neighbour.getValue(VALVE).direction() != face;
    }

    private static boolean joins(BlockState state, Direction direction, BlockState neighbour) {
        return !state.getValue(CUT.get(direction)) && state.getValue(VALVE).direction() != direction && connects(neighbour, direction.getOpposite())
                && !neighbour.getValue(CUT.get(direction.getOpposite()));
    }

    @Override
    public boolean couldJoin(BlockState state, Direction direction, BlockState neighbour) {
        return state.getValue(VALVE).direction() != direction && connects(neighbour, direction.getOpposite());
    }

    @Override
    public BlockState reconnect(BlockGetter level, BlockPos pos, BlockState state) { return connected(level, pos, state); }

    @Override
    public double low() { return 5; }

    @Override
    public double high() { return 11; }

    private static BlockState connected(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values())
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), joins(state, direction, level.getBlockState(pos.relative(direction))));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return connected(context.getLevel(), context.getClickedPos(), defaultBlockState().setValue(WATERLOGGED, water));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), joins(state, direction, neighbour));
    }

    // --- Valve ---

    /** The side a valve would stand on, preferring the given one: a side with nothing joined to it. Null if every side is taken. */
    private static Direction freeSide(BlockState state, Direction preferred, Direction after) {
        Direction[] order = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        if (preferred != null && !state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(preferred)) && preferred != after) return preferred;
        int start = after == null ? -1 : java.util.Arrays.asList(order).indexOf(after);
        for (int i = 1; i <= order.length; i++) {
            Direction direction = order[(start + i) % order.length];
            if (direction != after && !state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) return direction;
        }
        return null;
    }

    /** Fits a valve in the pipe, on the clicked side if that is free. Returns whether it fitted. */
    public static boolean fitValve(Level level, BlockPos pos, Direction clicked) {
        BlockState state = level.getBlockState(pos);
        Direction side = freeSide(state, clicked, null);
        if (side == null || state.getValue(VALVE) != Valve.NONE) return false;
        level.setBlock(pos, connected(level, pos, state.setValue(VALVE, Valve.of(side))), 3);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1F, 1.3F);
        return true;
    }

    @Override
    public InteractionResult useBuildingGadget(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Direction current = state.getValue(VALVE).direction();
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        // The handwheel turns when it is clicked, and sneaking takes the valve off; anywhere else the click cuts or mends a join.
        if (current == null || !sneaking && context.getClickedFace() != current) return cutOrMend(context);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (sneaking) {
            // Take the valve off again.
            level.setBlock(pos, connected(level, pos, state.setValue(VALVE, Valve.NONE)), 3);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.SHUTOFF_VALVE.get())));
            level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1F, 1.2F);
            return InteractionResult.CONSUME;
        }
        // Turn the wheel to the clicked side, or on to the next free one.
        Direction side = freeSide(state, context.getClickedFace(), current);
        if (side == null) return InteractionResult.CONSUME;
        level.setBlock(pos, connected(level, pos, state.setValue(VALVE, Valve.of(side))), 3);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1F, 1.3F);
        return InteractionResult.CONSUME;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int bits = 0;
        for (Direction direction : Direction.values()) if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) bits |= 1 << direction.ordinal();
        return shapes[bits][state.getValue(VALVE).ordinal()];
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
        builder.add(WATERLOGGED, VALVE);
        for (Direction direction : Direction.values()) builder.add(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), CUT.get(direction));
    }
}
