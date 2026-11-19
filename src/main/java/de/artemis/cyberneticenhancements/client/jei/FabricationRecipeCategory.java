package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.fabrication.*;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.*;

public final class FabricationRecipeCategory implements IRecipeCategory<RecipeHolder<FabricationRecipe>> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public FabricationRecipeCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(ModBlocks.CYBERWARE_FABRICATOR.get());
        arrow = helper.getRecipeArrow();
    }

    public mezz.jei.api.recipe.RecipeType<RecipeHolder<FabricationRecipe>> getRecipeType() {
        return CyberneticEnhancementsJeiPlugin.FABRICATION;
    }

    public Component getTitle() {
        return Component.literal("Cyberware Fabrication");
    }

    public int getWidth() {
        return 152;
    }

    public int getHeight() {
        return 90;
    }

    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public boolean isHandled(RecipeHolder<FabricationRecipe> holder) {
        if (!holder.value().clientUnlocked()) return false;
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        var current = level.getRecipeManager().byKey(holder.id()).orElse(null);
        return current != null && current.value() instanceof FabricationRecipe recipe && recipe.clientUnlocked();
    }

    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FabricationRecipe> holder, IFocusGroup focuses) {
        if (!isHandled(holder)) return;
        // Keep empty grid positions so missing-item highlights use the actual 5x5 slot indexes.
        for (int slot = 0; slot < 25; slot++) {
            int index = slot;
            var input = builder.addInputSlot(1 + slot % 5 * 18, 1 + slot / 5 * 18)
                    .setStandardSlotBackground();
            input.setSlotName("fabrication_input_" + slot);
            var part = holder.value().parts().stream().filter(p -> p.slot() == index).findFirst().orElse(null);
            if (part != null) input.addItemStacks(Arrays.stream(part.ingredient().getItems()).map(s -> s.copyWithCount(part.count())).toList());
        }
        builder.addOutputSlot(131, 37).setOutputSlotBackground().addItemStack(holder.value().result());
    }

    public void draw(RecipeHolder<FabricationRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double x, double y) {
        // JEI's vanilla crafting layout, extended by two 18-pixel columns and rows.
        arrow.draw(g, 97, (getHeight() - arrow.getHeight()) / 2);
    }
}
