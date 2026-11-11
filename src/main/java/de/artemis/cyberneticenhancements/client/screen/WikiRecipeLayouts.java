package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import java.util.Arrays;

 public final class WikiRecipeLayouts {
    private WikiRecipeLayouts() {}
    public static ItemStack[] grid(FabricationRecipe recipe) {
        ItemStack[] grid = empty(25);
        for (var part : recipe.parts()) grid[part.slot()] = example(part.ingredient(), part.count());
        return grid;
    }

    public static ItemStack[] grid(CraftingRecipe recipe) {
        ItemStack[] grid = empty(9);
        var ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            int dx = (3 - shaped.getWidth()) / 2, dy = (3 - shaped.getHeight()) / 2;
            for (int row = 0; row < shaped.getHeight(); row++)
                for (int col = 0; col < shaped.getWidth(); col++)
                    grid[(row + dy) * 3 + col + dx] = example(ingredients.get(row * shaped.getWidth() + col), 1);
        } else if (recipe instanceof ShapelessRecipe) {
            for (int slot = 0; slot < Math.min(9, ingredients.size()); slot++) grid[slot] = example(ingredients.get(slot), 1);
        } else throw new IllegalArgumentException("No static layout for special crafting recipes");
        return grid;
    }

    private static ItemStack example(Ingredient ingredient, int count) {
        var choices = ingredient.getItems();
        return choices.length == 0 ? ItemStack.EMPTY : choices[0].copyWithCount(count);
    }
    private static ItemStack[] empty(int size) {
        ItemStack[] grid = new ItemStack[size];
        Arrays.fill(grid, ItemStack.EMPTY);
        return grid;
    }
}