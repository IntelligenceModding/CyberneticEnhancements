package de.artemis.cyberneticenhancements.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.cyberneticenhancements.common.blockentity.CopperCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bare copper wire laid flat on the sides of a block space, placed the way glow lichen is: on any of the six sides, several
 * of them in one space, and more can be added by placing into a space that already has some. It is purely decoration.
 * <p>
 * Each side that holds a wire is a block state property. Which way the wires reach out is not stored: it follows from the
 * wires around, and is worked out again by whoever needs it, the outline here and the model on the client. A wire reaches
 * to the edge of its side wherever another one is there to meet it: on the next side of the same space (an inside corner),
 * on the same side of the next space along, or on the side of the space round the supporting block's edge (an outside corner).
 */
public final class CopperCableBlock extends MultifaceBlock implements SimpleWaterloggedBlock, EntityBlock, BuildingGadgetTarget {
    public static final MapCodec<CopperCableBlock> CODEC = simpleCodec(CopperCableBlock::new);
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** How far the wire stands off its support, and how wide it is, in sixteenths of a block. */
    private static final double THICKNESS = 2, WIDTH = 4;
    private static final double LOW = 8 - WIDTH / 2, HIGH = 8 + WIDTH / 2;
    private final MultifaceSpreader spreader = new MultifaceSpreader(this);
    private final Map<Long, VoxelShape> shapes = new ConcurrentHashMap<>();

    public CopperCableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<CopperCableBlock> codec() { return CODEC; }

    @Override
    public MultifaceSpreader getSpreader() { return spreader; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.getItemInHand().is(asItem()) || super.canBeReplaced(state, context);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CopperCableBlockEntity(pos, state); }

    // --- Connections ---

    /** The wire a wire meets, and whether it is met round the edge of the support rather than along a surface. */
    public record Partner(BlockPos pos, Direction side, Direction toward, boolean outside) {}

    /** Which reaches of the wires in this space are drawn: the ones that join another wire, and the outside corners that need filling. */
    public record Links(long reaches, long corners) {}

    private static boolean isWire(BlockState state) { return state.getBlock() instanceof CopperCableBlock; }

    private static boolean hasWire(BlockGetter level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        return isWire(state) && hasFace(state, side);
    }

    private static boolean isCut(BlockGetter level, BlockPos pos, Direction side, Direction toward) {
        return level.getBlockEntity(pos) instanceof CopperCableBlockEntity cable && cable.isCut(side, toward);
    }

    /**
     * The wire that the wire on side {@code side} of this space would meet in direction {@code toward}, which is square to it, if
     * there is one: on the next side of the same space (an inside corner), on the same side of the next space along, or on the
     * side of the space round the supporting block's edge (an outside corner). Whether the Building Gadget has cut the join is
     * not looked at.
     */
    public static Partner partner(BlockGetter level, BlockPos pos, BlockState state, Direction side, Direction toward) {
        if (hasFace(state, toward)) return new Partner(pos, toward, side, false);
        BlockPos beside = pos.relative(toward);
        if (hasWire(level, beside, side)) return new Partner(beside, side, toward.getOpposite(), false);
        BlockPos round = beside.relative(side);
        if (hasWire(level, round, toward.getOpposite())) return new Partner(round, toward.getOpposite(), side.getOpposite(), true);
        return null;
    }

    /** Whether the wire on side {@code side} of this space reaches out to the edge towards {@code toward}. */
    public static boolean reaches(BlockGetter level, BlockPos pos, BlockState state, Direction side, Direction toward) {
        Partner partner = partner(level, pos, state, side, toward);
        return partner != null && !isCut(level, pos, side, toward) && !isCut(level, partner.pos(), partner.side(), partner.toward());
    }

    /**
     * Every reach of the wires in this space as bits, one for each side and direction. Round an outside corner the two wires only
     * touch along an edge, so one of the pair, the same one from both ends, is drawn a little longer to fill the corner.
     */
    public static Links connections(BlockGetter level, BlockPos pos, BlockState state) {
        long reaches = 0, corners = 0;
        for (var side : Direction.values()) {
            if (!hasFace(state, side)) continue;
            for (var toward : Direction.values()) {
                if (toward.getAxis() == side.getAxis() || !reaches(level, pos, state, side, toward)) continue;
                reaches |= bit(side, toward);
                if (side.ordinal() < toward.ordinal() && partner(level, pos, state, side, toward).outside()) corners |= bit(side, toward);
            }
        }
        return new Links(reaches, corners);
    }

    public static long bit(Direction side, Direction toward) { return 1L << (side.ordinal() * 6 + toward.ordinal()); }

    // --- Building Gadget ---

    /** Cuts the join the click was nearest to, or mends it if it is already cut. */
    @Override
    public InteractionResult useBuildingGadget(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        Vec3 click = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ()).scale(16);
        Direction bestSide = null, bestToward = null;
        double best = Double.MAX_VALUE;
        for (var side : Direction.values()) {
            if (!hasFace(state, side)) continue;
            for (var toward : Direction.values()) {
                if (toward.getAxis() == side.getAxis() || partner(level, pos, state, side, toward) == null) continue;
                double distance = distance(click, armBox(side, toward));
                if (distance < best) { best = distance; bestSide = side; bestToward = toward; }
            }
        }
        if (bestSide == null) {
            if (!level.isClientSide && context.getPlayer() != null)
                context.getPlayer().displayClientMessage(Component.translatable("message.cyberneticenhancements.copper_cable.nothing"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Partner partner = partner(level, pos, state, bestSide, bestToward);
        boolean cut = reaches(level, pos, state, bestSide, bestToward); // Joined now, so it is cut; cut now, so it is mended.
        if (level.getBlockEntity(pos) instanceof CopperCableBlockEntity cable) cable.setCut(bestSide, bestToward, cut);
        if (level.getBlockEntity(partner.pos()) instanceof CopperCableBlockEntity other) other.setCut(partner.side(), partner.toward(), cut);
        level.playSound(null, pos, SoundType.COPPER.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, cut ? 0.7F : 1.3F);
        if (context.getPlayer() != null)
            context.getPlayer().displayClientMessage(Component.translatable(cut ? "message.cyberneticenhancements.copper_cable.cut" : "message.cyberneticenhancements.copper_cable.joined"), true);
        return InteractionResult.CONSUME;
    }

    private static double distance(Vec3 point, double[] box) {
        double x = Math.max(Math.max(box[0] - point.x, point.x - box[3]), 0);
        double y = Math.max(Math.max(box[1] - point.y, point.y - box[4]), 0);
        double z = Math.max(Math.max(box[2] - point.z, point.z - box[5]), 0);
        return x * x + y * y + z * z;
    }

    // --- Shape ---

    /** The middle of a wire on this side, as a box in sixteenths: min x, y, z then max x, y, z. */
    public static double[] coreBox(Direction side) {
        return box(side, anyPerpendicular(side), 0, THICKNESS, LOW, HIGH);
    }

    /** The arm that runs from the middle of the wire on {@code side} out to the block edge in direction {@code toward}. */
    public static double[] armBox(Direction side, Direction toward) {
        return box(side, toward, 0, THICKNESS, HIGH, 16);
    }

    /** The same arm, run on past the block edge by a wire's thickness to fill the corner where it turns round the support. */
    public static double[] cornerArmBox(Direction side, Direction toward) {
        return box(side, toward, 0, THICKNESS, HIGH, 16 + THICKNESS);
    }

    /** The name of one of the model pieces the wires are put together from: a core for a side, or an arm for a side and direction. */
    public static String modelName(String piece, Direction side, Direction toward) {
        return "copper_cable_" + piece + "_" + side.getSerializedName() + (toward == null ? "" : "_" + toward.getSerializedName());
    }

    private static Direction anyPerpendicular(Direction direction) {
        return direction.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
    }

    /** A box in the wire's own frame: {@code u} runs out from the side, {@code w} along {@code toward}, and the width is centred. */
    private static double[] box(Direction side, Direction toward, double u0, double u1, double w0, double w1) {
        double[] box = new double[6];
        for (var axis : Direction.Axis.values()) {
            double low, high;
            if (axis == side.getAxis()) {
                boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                low = positive ? 16 - u1 : u0;
                high = positive ? 16 - u0 : u1;
            } else if (axis == toward.getAxis()) {
                boolean positive = toward.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                low = positive ? w0 : 16 - w1;
                high = positive ? w1 : 16 - w0;
            } else {
                low = LOW;
                high = HIGH;
            }
            box[axis.ordinal()] = low;
            box[axis.ordinal() + 3] = high;
        }
        return box;
    }

    private static VoxelShape shapeOf(double[] box) {
        return Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        long connections = connections(level, pos, state).reaches();
        long key = connections;
        for (var side : Direction.values()) if (hasFace(state, side)) key |= 1L << (36 + side.ordinal());
        return shapes.computeIfAbsent(key, ignored -> buildShape(state, connections));
    }

    private static VoxelShape buildShape(BlockState state, long connections) {
        VoxelShape shape = Shapes.empty();
        for (var side : Direction.values()) {
            if (!hasFace(state, side)) continue;
            shape = Shapes.or(shape, shapeOf(coreBox(side)));
            for (var toward : Direction.values())
                if ((connections & bit(side, toward)) != 0) shape = Shapes.or(shape, shapeOf(armBox(side, toward)));
        }
        return shape.isEmpty() ? Shapes.block() : shape;
    }
}
