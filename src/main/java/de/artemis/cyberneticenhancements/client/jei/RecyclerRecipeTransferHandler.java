package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.menu.*;
import de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe;
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

public final class RecyclerRecipeTransferHandler implements IRecipeTransferHandler<RecyclerStationMenu, RecipeHolder<RecyclerRecipe>> {
    private final IRecipeTransferHandlerHelper helper;
    public RecyclerRecipeTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }
    @Override public Class<RecyclerStationMenu> getContainerClass() { return RecyclerStationMenu.class; }
    @Override public Optional<MenuType<RecyclerStationMenu>> getMenuType() { return Optional.of(ModMenuTypes.RECYCLER_STATION.get()); }
    @Override public RecipeType<RecipeHolder<RecyclerRecipe>> getRecipeType() { return CyberneticEnhancementsJeiPlugin.RECYCLING; }
    @Override public IRecipeTransferError transferRecipe(RecyclerStationMenu menu, RecipeHolder<RecyclerRecipe> recipe,
            IRecipeSlotsView slots, Player player, boolean maximum, boolean doTransfer) {
        var plan = RecyclerRecipeTransfer.prepare(menu, player, recipe.id(), maximum);
        if (plan.error() != RecyclerRecipeTransfer.Error.NONE) {
            var message = Component.translatable("screen.cyberneticenhancements.recycler_station.transfer." + plan.error().name().toLowerCase(Locale.ROOT));
            if (plan.error() == RecyclerRecipeTransfer.Error.MISSING)
                return helper.createUserErrorForMissingSlots(message, slots.getSlotViews(RecipeIngredientRole.INPUT));
            return helper.createUserErrorWithTooltip(message);
        }
        if (doTransfer) PacketDistributor.sendToServer(new RecyclerRecipeTransferPayload(menu.containerId, recipe.id(), maximum));
        return null;
    }
}
