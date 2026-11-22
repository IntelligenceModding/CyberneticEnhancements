package de.artemis.cyberneticenhancements.common.item;

import de.artemis.cyberneticenhancements.common.entity.HangingWireEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hangs a copper wire between two points. The first click on a block face sets where it starts, the second where it ends; any
 * number of wires can start or end on the same point. Sneak and use it in the air to forget the start.
 */
public final class HangingWireItem extends Item {
    private static final double SNAP = 8, EDGE = 1.0 / 8;

    public HangingWireItem(Properties properties) { super(properties); }

    /** The point on the clicked face the wire hangs from: the click, on a one-eighth grid and clear of the edges. */
    private static Vec3 anchorPoint(UseOnContext context) {
        Direction face = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        Vec3 local = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        double[] coordinates = {local.x, local.y, local.z};
        for (var axis : Direction.Axis.values()) {
            int index = axis.ordinal();
            if (axis == face.getAxis()) coordinates[index] = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
            else coordinates[index] = Math.clamp(Math.round(coordinates[index] * SNAP) / SNAP, EDGE, 1 - EDGE);
        }
        return new Vec3(pos.getX() + coordinates[0], pos.getY() + coordinates[1], pos.getZ() + coordinates[2]);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Vec3 point = anchorPoint(context);
        BlockPos block = context.getClickedPos();
        String dimension = level.dimension().location().toString();
        CompoundTag pending = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!pending.contains("StartX") || !pending.getString("Dimension").equals(dimension)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putString("Dimension", dimension);
                tag.putDouble("StartX", point.x);
                tag.putDouble("StartY", point.y);
                tag.putDouble("StartZ", point.z);
                tag.putLong("StartBlock", block.asLong());
            });
            level.playSound(null, block, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1F, 1.4F);
            say(player, "message.cyberneticenhancements.hanging_wire.start");
            return InteractionResult.CONSUME;
        }
        Vec3 start = new Vec3(pending.getDouble("StartX"), pending.getDouble("StartY"), pending.getDouble("StartZ"));
        double length = start.distanceTo(point);
        if (length < 0.25) {
            say(player, "message.cyberneticenhancements.hanging_wire.short");
            return InteractionResult.CONSUME;
        }
        if (length > HangingWireEntity.MAX_LENGTH) {
            say(player, "message.cyberneticenhancements.hanging_wire.far");
            return InteractionResult.CONSUME;
        }
        HangingWireEntity wire = ModEntityTypes.HANGING_WIRE.get().create(level);
        if (wire == null) return InteractionResult.FAIL;
        wire.hang(start, BlockPos.of(pending.getLong("StartBlock")), point, block);
        level.addFreshEntity(wire);
        level.playSound(null, block, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1F, 1.2F);
        stack.remove(DataComponents.CUSTOM_DATA);
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        say(player, "message.cyberneticenhancements.hanging_wire.hung");
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !stack.has(DataComponents.CUSTOM_DATA)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            stack.remove(DataComponents.CUSTOM_DATA);
            say(player, "message.cyberneticenhancements.hanging_wire.forgotten");
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void say(Player player, String key) {
        if (player != null) player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.hanging_wire").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.hanging_wire.more").withStyle(ChatFormatting.DARK_AQUA));
    }
}
