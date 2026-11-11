package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public final class CyberwareRecycleHelper {
    private CyberwareRecycleHelper() {}

    public static CyberwareRecyclePlan resolve(Level level, ItemStack input) {
        if (input.isEmpty()) return CyberwareRecyclePlan.empty();
        return level.getRecipeManager().getRecipeFor(ModRecipeTypes.RECYCLING.get(), new SingleRecipeInput(input), level)
                .map(recipe -> new CyberwareRecyclePlan(recipe.value().recover(input)))
                .orElseGet(CyberwareRecyclePlan::empty);
    }
}