package de.artemis.cyberneticenhancements.common.item;

import de.artemis.cyberneticenhancements.common.block.WaterPipeBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** A red shutoff valve that is fitted into the middle of a water pipe, its handwheel standing out of the side that was clicked. */
public final class ShutoffValveItem extends Item {
    public ShutoffValveItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof WaterPipeBlock)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!WaterPipeBlock.fitValve(level, pos, context.getClickedFace())) return InteractionResult.FAIL;
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.shutoff_valve").withStyle(ChatFormatting.GRAY));
    }
}
