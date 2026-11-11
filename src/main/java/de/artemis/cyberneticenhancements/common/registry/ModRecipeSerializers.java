package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CyberneticEnhancements.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipe>> FABRICATION =
            RECIPE_SERIALIZERS.register("fabrication", de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RecyclerRecipe>> RECYCLING =
            RECIPE_SERIALIZERS.register("recycling", RecyclerRecipe.Serializer::new);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
