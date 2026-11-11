package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import java.util.List;

public final class CyberwareServiceHelper {
    public record MaterialRequirement(ItemStack stack, int count) {
        public MaterialRequirement {
            if (stack.isEmpty() || count < 1 || count > stack.getMaxStackSize())
                throw new IllegalArgumentException("Invalid service material");
            stack = stack.copyWithCount(1);
        }
    }

    private CyberwareServiceHelper() {}

    public static CyberwareServicePlan getRepairPlan(ItemStack input) {
        if (!(input.getItem() instanceof CyberwareItem implant)) return CyberwareServicePlan.empty();
        var definition = implant.getDefinition();
        if (!CyberwareConditionHelper.isDamaged(input, definition)) return CyberwareServicePlan.empty();
        var costs = CyberwareUpgradeRegistry.get(definition.id()).repairMaterials();
        if (costs.isEmpty()) return CyberwareServicePlan.empty();
        double missing = 1 - CyberwareConditionHelper.getIntegrityRatio(input, definition);
        return new CyberwareServicePlan(CyberwareConditionHelper.createServiceCopy(input, definition, true),
                costs.stream().map(m -> requirement(m, Math.max(1, (int) Math.ceil(m.count() * missing)))).toList());
    }

    public static CyberwareServicePlan getUpgradePlan(ItemStack input) {
        if (!(input.getItem() instanceof CyberwareItem implant) || !CyberwareUpgradeHelper.canUpgrade(input))
            return CyberwareServicePlan.empty();
        var costs = CyberwareUpgradeRegistry.get(implant.getDefinition().id()).at(CyberwareUpgradeHelper.getUpgradeLevel(input) + 1).materials();
        return new CyberwareServicePlan(CyberwareUpgradeHelper.createUpgradedCopy(input, implant.getDefinition()),
                costs.stream().map(m -> requirement(m, m.count())).toList());
    }

    private static MaterialRequirement requirement(CyberwareUpgradeDefinition.Material material, int count) {
        return new MaterialRequirement(new ItemStack(BuiltInRegistries.ITEM.get(material.item())), count);
    }

    public static List<MaterialRequirement> getRequiredMaterials(CyberwareServicePlan plan) { return plan.materials(); }

    public static boolean matchesMaterials(CyberwareServicePlan plan, ItemStack... provided) {
        if (!plan.isAvailable()) return false;
        int[] remaining = java.util.Arrays.stream(provided).mapToInt(ItemStack::getCount).toArray();
        for (var requirement : plan.materials()) {
            int missing = requirement.count();
            for (int slot = 0; slot < provided.length && missing > 0; slot++) {
                if (!ItemStack.isSameItemSameComponents(provided[slot], requirement.stack())) continue;
                int used = Math.min(remaining[slot], missing);
                remaining[slot] -= used;
                missing -= used;
            }
            if (missing > 0) return false;
        }
        return true;
    }

    public static ItemStack componentStack(CyberwareTier tier) { return new ItemStack(componentItem(tier)); }
    public static ItemLike componentItem(CyberwareTier tier) {
        return switch (tier) {
            case TIER_1 -> ModItems.COMMON_ITEM_COMPONENTS.get();
            case TIER_2 -> ModItems.UNCOMMON_ITEM_COMPONENTS.get();
            case TIER_3 -> ModItems.RARE_ITEM_COMPONENTS.get();
            case TIER_4 -> ModItems.EPIC_ITEM_COMPONENTS.get();
            case TIER_5 -> ModItems.LEGENDARY_ITEM_COMPONENTS.get();
        };
    }
}