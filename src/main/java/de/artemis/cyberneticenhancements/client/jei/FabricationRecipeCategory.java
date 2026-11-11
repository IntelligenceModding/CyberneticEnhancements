package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.fabrication.*;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.*;

public final class FabricationRecipeCategory implements IRecipeCategory<RecipeHolder<FabricationRecipe>> {
    private final IDrawable icon;

    public FabricationRecipeCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(ModBlocks.CYBERWARE_FABRICATOR.get());
    }

    public mezz.jei.api.recipe.RecipeType<RecipeHolder<FabricationRecipe>> getRecipeType() {
        return CyberneticEnhancementsJeiPlugin.FABRICATION;
    }

    public Component getTitle() {
        return Component.literal("Cyberware Fabrication");
    }

    public int getWidth() {
        return 180;
    }

    public int getHeight() {
        return 126;
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
        for (var part : holder.value().parts()) {
            List<ItemStack> items = Arrays.stream(part.ingredient().getItems()).map(s -> s.copyWithCount(part.count())).toList();
            builder.addInputSlot(2 + part.slot() % 5 * 18, 2 + part.slot() / 5 * 18).addItemStacks(items);
        }
        builder.addOutputSlot(142, 38).addItemStack(holder.value().result());
    }

    public void draw(RecipeHolder<FabricationRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double x, double y) {
        UiRenderer.panel(g, 0, 0, 180, 126, UiRenderer.PANEL);
        if (!isHandled(holder)) {
            g.drawString(Minecraft.getInstance().font, Component.translatable("screen.cyberneticenhancements.workshop.schematic_locked"), 8, 10, UiRenderer.TEXT_SECONDARY, false);
            return;
        }
        for (int i = 0; i < 25; i++)
            UiRenderer.panel(g, 1 + i % 5 * 18, 1 + i / 5 * 18, 18, 18, UiRenderer.FIELD, UiRenderer.FIELD_EDGE);
        UiRenderer.arrow(g, 110, 42, UiRenderer.ACCENT);
        var font = Minecraft.getInstance().font;
        g.drawString(font, "Tier " + holder.value().tier() + " / " + holder.value().ticks() / 20 + "s", 2, 98, UiRenderer.TEXT, false);
        g.drawString(font, holder.value().schematic().isEmpty() ? "Known design" : "Schematic learned", 2, 112, UiRenderer.TEXT, false);
    }
}
