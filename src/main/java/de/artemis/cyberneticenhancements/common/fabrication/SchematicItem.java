package de.artemis.cyberneticenhancements.common.fabrication;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class SchematicItem extends Item {
    public final String design;

    public SchematicItem(Properties properties, String design) {
        super(properties);
        this.design = design;
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            boolean exists = level.getRecipeManager().getAllRecipesFor(de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes.FABRICATION.get())
                    .stream().anyMatch(r -> r.value().schematic().equals(design));
            if (!exists) return InteractionResultHolder.fail(stack);
            if (!SchematicKnowledge.learn(server, design))
                server.displayClientMessage(Component.literal("Schematic already learned."), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Use to permanently learn this design. Not consumed."));
        lines.add(Component.literal("View learned designs in the archive's Schematics tab."));
    }
}
