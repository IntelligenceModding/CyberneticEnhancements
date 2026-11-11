package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CyberneticEnhancements.MOD_ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipe>> FABRICATION =
            RECIPE_TYPES.register("fabrication", RecipeType::simple);

    public static final DeferredHolder<RecipeType<?>, RecipeType<RecyclerRecipe>> RECYCLING =
            RECIPE_TYPES.register("recycling", RecipeType::simple);

    private ModRecipeTypes() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
