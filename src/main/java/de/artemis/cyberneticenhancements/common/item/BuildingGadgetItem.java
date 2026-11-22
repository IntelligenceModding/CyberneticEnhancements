package de.artemis.cyberneticenhancements.common.item;

import de.artemis.cyberneticenhancements.common.block.BuildingGadgetTarget;
import de.artemis.cyberneticenhancements.common.entity.HangingWireEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Changes building blocks that support it, for example which neighbours a copper cable joins. Right-click the block. */
public final class BuildingGadgetItem extends Item {
    public BuildingGadgetItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof BuildingGadgetTarget target) return target.useBuildingGadget(context);
        return takeDownWire(context);
    }

    /** Takes down the hanging wire whose end on the clicked block is nearest the click. */
    private static InteractionResult takeDownWire(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        HangingWireEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (var wire : level.getEntitiesOfClass(HangingWireEntity.class, new AABB(pos).inflate(HangingWireEntity.MAX_LENGTH))) {
            if (!wire.isAnchoredAt(pos)) continue;
            var end = wire.startBlock().equals(pos) ? wire.start() : wire.end();
            double distance = end.distanceToSqr(context.getClickLocation());
            if (distance < best) { best = distance; nearest = wire; }
        }
        if (nearest == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            nearest.takeDown(context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild);
            if (context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("message.cyberneticenhancements.hanging_wire.removed"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.building_gadget").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.building_gadget.cable").withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.building_gadget.wire").withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.building_gadget.pipe").withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.building_gadget.valve").withStyle(ChatFormatting.DARK_AQUA));
    }
}
