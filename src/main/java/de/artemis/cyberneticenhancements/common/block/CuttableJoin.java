package de.artemis.cyberneticenhancements.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/** A pipe-like block whose joins to the blocks next to it can be cut and mended with the Building Gadget. */
public interface CuttableJoin extends BuildingGadgetTarget {
    /** Per side, whether the join to the block on that side is cut. */
    Map<Direction, BooleanProperty> CUT = cutProperties();

    private static Map<Direction, BooleanProperty> cutProperties() {
        Map<Direction, BooleanProperty> map = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) map.put(direction, BooleanProperty.create("cut_" + direction.getName()));
        return map;
    }

    /** Whether the block would join the neighbour on that side if the join were not cut. */
    boolean couldJoin(BlockState state, Direction direction, BlockState neighbour);

    /** The state with every join worked out again. */
    BlockState reconnect(BlockGetter level, BlockPos pos, BlockState state);

    /** Where the tube of this block starts across, in sixteenths. */
    double low();

    /** Where the tube of this block ends across, in sixteenths. */
    double high();

    /** Cuts the join the click was nearest to, or mends it if it is already cut. */
    default InteractionResult cutOrMend(UseOnContext context) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Vec3 click = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ()).scale(16);
        Direction best = null;
        double nearest = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            if (!couldJoin(state, direction, level.getBlockState(pos.relative(direction)))) continue;
            double distance = distance(click, direction) - (direction == context.getClickedFace() ? 1.0E-6 : 0);
            if (distance < nearest) { nearest = distance; best = direction; }
        }
        if (best == null) {
            if (!level.isClientSide && context.getPlayer() != null)
                context.getPlayer().displayClientMessage(Component.translatable("message.cyberneticenhancements.joint.nothing"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean cut = !state.getValue(CUT.get(best));
        level.setBlock(pos, reconnect(level, pos, state.setValue(CUT.get(best), cut)), 3);
        BlockPos otherPos = pos.relative(best);
        BlockState other = level.getBlockState(otherPos);
        if (other.getBlock() instanceof CuttableJoin joint)
            level.setBlock(otherPos, joint.reconnect(level, otherPos, other.setValue(CUT.get(best.getOpposite()), cut)), 3);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, cut ? 0.7F : 1.3F);
        if (context.getPlayer() != null)
            context.getPlayer().displayClientMessage(Component.translatable(cut ? "message.cyberneticenhancements.joint.cut" : "message.cyberneticenhancements.joint.joined"), true);
        return InteractionResult.CONSUME;
    }

    /** Squared distance, in sixteenths, from the point to the arm that reaches out on that side. */
    private double distance(Vec3 point, Direction direction) {
        double[] min = {low(), low(), low()}, max = {high(), high(), high()};
        int axis = direction.getAxis().ordinal();
        if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) max[axis] = 16;
        else min[axis] = 0;
        double x = Math.max(Math.max(min[0] - point.x, point.x - max[0]), 0);
        double y = Math.max(Math.max(min[1] - point.y, point.y - max[1]), 0);
        double z = Math.max(Math.max(min[2] - point.z, point.z - max[2]), 0);
        return x * x + y * y + z * z;
    }
}
