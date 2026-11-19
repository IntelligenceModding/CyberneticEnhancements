package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Pure planning for the Repair Station's JEI transfer; the server resolves all costs from its own definitions. */
public final class RepairRecipeTransfer {
    private RepairRecipeTransfer() {}

    /** A repair cycle either restores integrity or, on a faulted implant, only clears the Neural Fault first. */
    public enum Kind { INTEGRITY, FAULT }

    /** A neural fault always comes with a loss of at least this share of the maximum integrity. */
    private static final int FAULT_DAMAGE_PERCENT = 35;

    /** The implant a repair recipe is drawn with: one repair step short of full, or faulted. Empty when the item cannot be repaired. */
    public static ItemStack displayInput(ResourceLocation id, Kind kind) {
        if (!(BuiltInRegistries.ITEM.get(id) instanceof CyberwareItem implant)) return ItemStack.EMPTY;
        var definition = implant.getDefinition();
        var stack = new ItemStack(implant);
        switch (kind) {
            case INTEGRITY -> CyberwareConditionHelper.damage(stack, definition, CyberwareServiceHelper.repairAmount(definition));
            case FAULT -> CyberwareConditionHelper.applyNeuralFault(stack, definition, CyberwareConditionHelper.getMaxIntegrity(definition) * FAULT_DAMAGE_PERCENT / 100);
        }
        return CyberwareServiceHelper.getRepairPlan(stack).isAvailable() ? stack : ItemStack.EMPTY;
    }

    /** Any copy of the implant that needs this kind of repair, whatever its exact integrity or name. */
    private static boolean matches(ItemStack stack, ItemStack sample, Kind kind) {
        return !stack.isEmpty() && stack.is(sample.getItem()) && CyberwareServiceHelper.getRepairPlan(stack).isAvailable()
                && CyberwareConditionHelper.hasNeuralFault(stack) == (kind == Kind.FAULT);
    }

    public static UpgradeRecipeTransfer.Plan prepare(TechStationMenu menu, Player player, ResourceLocation id, Kind kind, boolean maximum) {
        return UpgradeRecipeTransfer.assemble(menu, player, false, () -> displayInput(id, kind), (stack, sample) -> matches(stack, sample, kind),
                CyberwareServiceHelper::getRepairPlan, maximum);
    }
}
