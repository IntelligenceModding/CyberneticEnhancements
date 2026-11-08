package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleDefinition;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class CyberwareRecycleHelper {
    private CyberwareRecycleHelper() {
    }

    public static CyberwareRecyclePlan resolve(Level level, ItemStack inputStack) {
        if (inputStack.isEmpty()) {
            return CyberwareRecyclePlan.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.RECYCLING.get(), new SingleRecipeInput(inputStack), level)
                .map(recipeHolder -> buildPlan(inputStack, recipeHolder.value().assemble(new SingleRecipeInput(inputStack), level.registryAccess())))
                .orElseGet(CyberwareRecyclePlan::empty);
    }

    private static CyberwareRecyclePlan buildPlan(ItemStack inputStack, ItemStack primaryOutput) {
        if (primaryOutput.isEmpty()) {
            return CyberwareRecyclePlan.empty();
        }

        List<ItemStack> outputs = new ArrayList<>();
        appendOutput(outputs, primaryOutput);
        if (inputStack.getItem() instanceof CyberwareItem cyberwareItem) {
            appendOutput(outputs, new ItemStack(CyberwareServiceHelper.slotSupportItem(cyberwareItem.getDefinition().slotType())));
        }
        return new CyberwareRecyclePlan(outputs);
    }

    private static void appendOutput(List<ItemStack> outputs, ItemStack candidate) {
        if (candidate.isEmpty()) {
            return;
        }

        for (ItemStack existing : outputs) {
            if (ItemStack.isSameItemSameComponents(existing, candidate)) {
                existing.grow(candidate.getCount());
                return;
            }
        }
        outputs.add(candidate.copy());
    }

    public static ItemStack createCyberwareResult(CyberwareDefinition definition) {
        return new ItemStack(CyberwareServiceHelper.componentItem(definition.tier()), 1 + definition.tier().ordinal());
    }

    public static ItemStack createChipwareResult(ChipwareDefinition definition) {
        return new ItemStack(CyberwareServiceHelper.componentItem(definition.tier()), Math.max(1, definition.tier().ordinal()));
    }

    public static ItemStack createModuleResult(CyberwareModuleDefinition definition) {
        return new ItemStack(
                switch (definition.category()) {
                    case ARMS, LEGS -> ModItems.REPLACEMENT_JOINT.get();
                },
                1 + definition.tier().ordinal() / 2
        );
    }

    public static ItemStack createConsumableResult(CyberConsumableDefinition definition) {
        return switch (definition.category()) {
            case MEDICAL, SUPPRESSANT -> new ItemStack(ModItems.CONDUCTIVE_PASTE.get(), 1);
            case NEURAL -> new ItemStack(ModItems.BASIC_CIRCUIT_PLATE.get(), 1);
            case BOOSTER, STREET -> new ItemStack(ModItems.COPPER_WIRING.get(), 1);
        };
    }
}
