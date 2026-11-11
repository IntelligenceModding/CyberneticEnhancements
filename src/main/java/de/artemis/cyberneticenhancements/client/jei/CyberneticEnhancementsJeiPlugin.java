package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes;
import de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipe;
import de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import de.artemis.cyberneticenhancements.client.screen.RipperStationScreen;
import de.artemis.cyberneticenhancements.client.screen.TechStationScreen;
import de.artemis.cyberneticenhancements.client.screen.RecyclerStationScreen;
import de.artemis.cyberneticenhancements.client.screen.FixerDialogScreen;
import de.artemis.cyberneticenhancements.client.screen.RelicCacheHackScreen;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import mezz.jei.api.runtime.IJeiRuntime;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@JeiPlugin
public final class CyberneticEnhancementsJeiPlugin implements IModPlugin {
    private final RecipeVisibilityTracker<ResourceLocation, RecipeHolder<FabricationRecipe>> visibility = new RecipeVisibilityTracker<>();
    private final Consumer<ClientTickEvent.Post> tickListener = event -> refreshVisibility();
    private final Consumer<RecipesUpdatedEvent> recipesListener = event -> recipesChanged = true;
    private IJeiRuntime runtime;
    private net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private int knowledgeRevision = -1;
    private boolean recipesChanged = true;
    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "jei_plugin");
    public static final mezz.jei.api.recipe.RecipeType<RecipeHolder<RecyclerRecipe>> RECYCLING =
            mezz.jei.api.recipe.RecipeType.createRecipeHolderType(
                    ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "recycling")
            );

    public static final mezz.jei.api.recipe.RecipeType<RecipeHolder<de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipe>> FABRICATION =
            mezz.jei.api.recipe.RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "fabrication"));

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(de.artemis.cyberneticenhancements.client.screen.FabricatorScreen.class, ResponsiveJeiGuiProperties::of);
        registration.addGuiScreenHandler(RipperStationScreen.class, ResponsiveJeiGuiProperties::of);
        registration.addGuiScreenHandler(TechStationScreen.class, ResponsiveJeiGuiProperties::of);
        registration.addGuiScreenHandler(RecyclerStationScreen.class, ResponsiveJeiGuiProperties::of);
        registration.addGuiScreenHandler(FixerDialogScreen.class, ResponsiveJeiGuiProperties::of);
        registration.addGuiScreenHandler(RelicCacheHackScreen.class, ResponsiveJeiGuiProperties::of);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FabricationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RecyclerRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<RecipeHolder<RecyclerRecipe>> recipes = minecraft.level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.RECYCLING.get());
        registration.addRecipes(RECYCLING, recipes);
        visibility.clear();
        registration.addRecipes(FABRICATION, visibility.update(knownRecipes()).add());
    }

    private Map<ResourceLocation, RecipeHolder<FabricationRecipe>> knownRecipes() {
        var level = Minecraft.getInstance().level;
        if (level == null) return Map.of();
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.FABRICATION.get()).stream()
                .filter(holder -> holder.value().clientUnlocked())
                .collect(Collectors.toMap(RecipeHolder::id, holder -> holder));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        recipesChanged = true;
        NeoForge.EVENT_BUS.addListener(tickListener);
        NeoForge.EVENT_BUS.addListener(recipesListener);
        refreshVisibility();
    }

    @Override
    public void onRuntimeUnavailable() {
        NeoForge.EVENT_BUS.unregister(tickListener);
        NeoForge.EVENT_BUS.unregister(recipesListener);
        runtime = null;
        lastLevel = null;
        knowledgeRevision = -1;
        recipesChanged = true;
        visibility.clear();
    }

    private void refreshVisibility() {
        if (runtime == null) return;
        var level = Minecraft.getInstance().level;
        int revision = SchematicKnowledge.revision();
        if (!recipesChanged && lastLevel == level && knowledgeRevision == revision) return;
        var changes = visibility.update(knownRecipes());
        var manager = runtime.getRecipeManager();
        if (!changes.hide().isEmpty()) manager.hideRecipes(FABRICATION, changes.hide());
        if (!changes.add().isEmpty()) manager.addRecipes(FABRICATION, changes.add());
        if (!changes.show().isEmpty()) manager.unhideRecipes(FABRICATION, changes.show());
        lastLevel = level;
        knowledgeRevision = revision;
        recipesChanged = false;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.RECYCLER_STATION.get(), RECYCLING);
        registration.addRecipeCatalyst(ModBlocks.CYBERWARE_FABRICATOR.get(), FABRICATION);
    }
}
