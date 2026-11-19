package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransfer;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * One repair cycle of the Repair Station for one implant, from the server-synchronized repair costs: either a step of integrity
 * or, on a faulted implant, the cycle that clears the Neural Fault. Any copy of the implant finds it, whatever its integrity.
 */
public record RepairJeiRecipe(ResourceLocation item, RepairRecipeTransfer.Kind kind, ItemStack input, CyberwareServicePlan plan) {
    private CyberwareDefinition definition() { return ((CyberwareItem) input.getItem()).getDefinition(); }

    public Component step() {
        return kind == RepairRecipeTransfer.Kind.INTEGRITY
                ? Component.translatable("jei.cyberneticenhancements.repairing.step.integrity", CyberwareServiceHelper.repairAmount(definition()))
                : Component.translatable("jei.cyberneticenhancements.repairing.step.fault");
    }

    public List<Component> changes() {
        var definition = definition();
        int restored = CyberwareServiceHelper.repairAmount(definition);
        return kind == RepairRecipeTransfer.Kind.INTEGRITY
                ? List.of(Component.translatable("jei.cyberneticenhancements.repairing.integrity", restored, CyberwareConditionHelper.getMaxIntegrity(definition)),
                        Component.translatable("jei.cyberneticenhancements.repairing.integrity_missing", restored))
                : List.of(Component.translatable("jei.cyberneticenhancements.repairing.fault"),
                        Component.translatable("jei.cyberneticenhancements.repairing.fault_after"));
    }

    public static List<RepairJeiRecipe> all() {
        var recipes = new ArrayList<RepairJeiRecipe>();
        for (var entry : ModItems.cyberwareItems()) {
            var id = BuiltInRegistries.ITEM.getKey(entry.get());
            for (var kind : RepairRecipeTransfer.Kind.values()) {
                ItemStack input = RepairRecipeTransfer.displayInput(id, kind);
                if (input.isEmpty()) continue;
                recipes.add(new RepairJeiRecipe(id, kind, input, CyberwareServiceHelper.getRepairPlan(input)));
            }
        }
        return List.copyOf(recipes);
    }
}
