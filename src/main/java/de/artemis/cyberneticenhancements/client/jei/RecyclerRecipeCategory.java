package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public final class RecyclerRecipeCategory implements IRecipeCategory<RecipeHolder<RecyclerRecipe>> {
    private static final int WIDTH = 116;
    private static final int HEIGHT = 26;
    private final IDrawable arrow;
    private final IDrawable icon;

    public RecyclerRecipeCategory(IGuiHelper guiHelper) {
        this.arrow = guiHelper.getRecipeArrow();
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.RECYCLER_STATION.get());
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<RecyclerRecipe>> getRecipeType() {
        return CyberneticEnhancementsJeiPlugin.RECYCLING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("screen.cyberneticenhancements.recycler_station.recycler_bay");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RecyclerRecipe> recipeHolder, IFocusGroup focuses) {
        RecyclerRecipe recipe = recipeHolder.value();
        List<List<ItemStack>> outputSlots = buildDisplayOutputs(recipe);
        builder.addInputSlot(5, 5).setSlotName("recycling_input").setOutputSlotBackground()
                .addIngredients(recipe.getInputIngredient());
        for (int slot = 0; slot < outputSlots.size(); slot++) {
            var output = builder.addOutputSlot(63 + slot * 18, 5)
                    .setSlotName("recycling_output_" + slot).setStandardSlotBackground();
            output.addItemStacks(outputSlots.get(slot));
        }
    }

    @Override
    public void draw(RecipeHolder<RecyclerRecipe> recipeHolder, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 31, (HEIGHT - arrow.getHeight()) / 2);
    }

    private List<List<ItemStack>> buildDisplayOutputs(RecyclerRecipe recipe) {
        // The displayed ingredient is undamaged; keep its yield stable and consistent with that input.
        return recipe.outputs().stream().map(stack -> List.of(stack)).toList();
    }
}
