package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.fabrication.*;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;
import java.util.Optional;

public final class FabricatorRecipeTransferHandler implements IRecipeTransferHandler<FabricatorMenu, RecipeHolder<FabricationRecipe>> {
    private final IRecipeTransferHandlerHelper helper;
    public FabricatorRecipeTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }
    @Override public Class<FabricatorMenu> getContainerClass() { return FabricatorMenu.class; }
    @Override public Optional<MenuType<FabricatorMenu>> getMenuType() { return Optional.of(ModMenuTypes.FABRICATOR.get()); }
    @Override public RecipeType<RecipeHolder<FabricationRecipe>> getRecipeType() { return CyberneticEnhancementsJeiPlugin.FABRICATION; }

    @Override public IRecipeTransferError transferRecipe(FabricatorMenu menu, RecipeHolder<FabricationRecipe> recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        var plan = FabricatorRecipeTransfer.prepare(menu, player, recipe.id(), maxTransfer);
        if (plan.error() != FabricatorRecipeTransfer.Error.NONE) {
            Component message = Component.translatable("screen.cyberneticenhancements.fabricator.transfer." + plan.error().name().toLowerCase(Locale.ROOT));
            if (plan.error() == FabricatorRecipeTransfer.Error.MISSING) {
                var inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
                var missingNames = plan.missing().stream().map(slot -> "fabrication_input_" + slot).toList();
                var missing = inputs.stream().filter(slot -> slot.getSlotName().map(missingNames::contains).orElse(false)).toList();
                if (!missing.isEmpty()) return helper.createUserErrorForMissingSlots(message, missing);
            }
            return helper.createUserErrorWithTooltip(message);
        }
        if (doTransfer) PacketDistributor.sendToServer(new FabricatorRecipeTransferPayload(menu.containerId, recipe.id(), maxTransfer));
        return null;
    }
}
