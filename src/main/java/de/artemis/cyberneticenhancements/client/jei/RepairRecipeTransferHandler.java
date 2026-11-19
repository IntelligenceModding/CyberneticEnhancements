package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.menu.*;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;
import java.util.Optional;

public final class RepairRecipeTransferHandler implements IRecipeTransferHandler<TechStationMenu, RepairJeiRecipe> {
    private final IRecipeTransferHandlerHelper helper;
    public RepairRecipeTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }
    @Override public Class<TechStationMenu> getContainerClass() { return TechStationMenu.class; }
    @Override public Optional<MenuType<TechStationMenu>> getMenuType() { return Optional.of(ModMenuTypes.TECH_STATION.get()); }
    @Override public RecipeType<RepairJeiRecipe> getRecipeType() { return CyberneticEnhancementsJeiPlugin.REPAIRING; }
    @Override public IRecipeTransferError transferRecipe(TechStationMenu menu, RepairJeiRecipe recipe,
            IRecipeSlotsView slots, Player player, boolean maximum, boolean doTransfer) {
        var plan = RepairRecipeTransfer.prepare(menu, player, recipe.item(), recipe.kind(), maximum);
        if (plan.error() != UpgradeRecipeTransfer.Error.NONE) {
            var message = Component.translatable("screen.cyberneticenhancements.repair_station.transfer." + plan.error().name().toLowerCase(Locale.ROOT));
            if (plan.error() == UpgradeRecipeTransfer.Error.MISSING)
                return helper.createUserErrorForMissingSlots(message, slots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                        .filter(slot -> slot.getSlotName().map(name -> plan.missing().stream().anyMatch(i -> name.equals("repair_input_" + i))).orElse(false)).toList());
            return helper.createUserErrorWithTooltip(message);
        }
        if (doTransfer) PacketDistributor.sendToServer(new RepairRecipeTransferPayload(menu.containerId, recipe.item(), recipe.kind(), maximum));
        return null;
    }
}
