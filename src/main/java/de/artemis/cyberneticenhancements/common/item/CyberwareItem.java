package de.artemis.cyberneticenhancements.common.item;

import de.artemis.cyberneticenhancements.common.cyberware.ChipwareSocketHandler;
import de.artemis.cyberneticenhancements.common.cyberware.ArmCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffect;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareAbilities;
import de.artemis.cyberneticenhancements.common.cyberware.CirculatoryCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.FrontalCortexManager;
import de.artemis.cyberneticenhancements.common.cyberware.FaceCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.HandsCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.IntegumentaryCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleHandler;
import de.artemis.cyberneticenhancements.common.cyberware.LegCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.NervousSystemCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.SkeletonCyberwareManager;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlotType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper;
import de.artemis.cyberneticenhancements.common.item.ChipwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareModuleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CyberwareItem extends Item {
    private final CyberwareDefinition definition;

    public CyberwareItem(Properties properties, CyberwareDefinition definition) {
        super(properties.stacksTo(1));
        this.definition = definition;
    }

    public CyberwareDefinition getDefinition() {
        return definition;
    }

    public CyberwareSlotType getSlotType() {
        return definition.slotType();
    }

    public CyberwareTier getTier() {
        return definition.tier();
    }

    public int getChromeCost() {
        return definition.chromeCost();
    }

    public int getCapacityBonus() {
        return definition.capacityBonus();
    }

    public int getCapacityBonus(ItemStack stack) {
        return CyberwareConditionHelper.hasNeuralFault(stack) ? 0 : CyberwareUpgradeHelper.getCapacityBonus(stack, definition);
    }

    public List<CyberwareEffect> getEffects() {
        return definition.effects();
    }

    public List<CyberwareEffect> getEffects(ItemStack stack) {
        return CyberwareUpgradeHelper.getEffects(stack, definition);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(definition.tier().getColor());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable(definition.descriptionKey()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.cyberware_slot", Component.translatable(definition.slotType().translationKey()))
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.cyberware_tier", Component.translatable(definition.tier().translationKey()))
                .withStyle(definition.tier().getColor()));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.acquisition", Component.translatable(definition.acquisitionMethod().translationKey()))
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.chrome_cost", definition.chromeCost())
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable(
                        "tooltip.cyberneticenhancements.integrity",
                        CyberwareConditionHelper.getIntegrity(stack, definition),
                        CyberwareConditionHelper.getMaxIntegrity(definition))
                .withStyle(ChatFormatting.YELLOW));
        if (CyberwareConditionHelper.hasNeuralFault(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.neural_fault").withStyle(ChatFormatting.RED));
            tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.neural_fault.repair").withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(CyberwareUpgradeHelper.getUpgradeStatusComponent(stack, definition)
                .copy()
                .withStyle(ChatFormatting.DARK_AQUA));
        for (Component bonusLine : CyberwareUpgradeHelper.getUpgradeBonusTooltipLines(stack, definition)) {
            tooltipComponents.add(bonusLine.copy().withStyle(ChatFormatting.GREEN));
        }
        if (definition.supportsChipware()) {
            tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.chip_slots", CyberwareUpgradeHelper.getChipSlotCount(stack, definition))
                    .withStyle(ChatFormatting.DARK_AQUA));
            appendStoredChipwareTooltip(stack, context, tooltipComponents);
        }
        if (definition.supportsModules()) {
            tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.module_slots", CyberwareUpgradeHelper.getModuleSlotCount(stack, definition))
                    .withStyle(ChatFormatting.DARK_AQUA));
            appendStoredModulesTooltip(stack, context, tooltipComponents);
        }
        double chromeHeadroom = CyberwareUpgradeHelper.getCapacityBonus(stack, definition);
        for (CyberwareEffect effect : definition.effects()) {
            if (effect.type() == CyberwareEffectType.CHROME_CAPACITY) {
                chromeHeadroom += effect.amount();
                continue;
            }
            tooltipComponents.add(effect.describe().copy().withStyle(ChatFormatting.GREEN));
        }
        if (chromeHeadroom > 0.0001D) {
            tooltipComponents.add(new CyberwareEffect(CyberwareEffectType.CHROME_CAPACITY, chromeHeadroom).describe().copy().withStyle(ChatFormatting.GREEN));
        }
        FrontalCortexManager.appendBehaviorTooltip(definition, tooltipComponents);
        CyberwareAbilities.appendBehaviorTooltip(definition, tooltipComponents);
        ArmCyberwareManager.appendBehaviorTooltip(definition, tooltipComponents);
        FaceCyberwareManager.appendBehaviorTooltip(definition, tooltipComponents);
        CirculatoryCyberwareManager.appendBehaviorTooltip(definition, tooltipComponents);
        HandsCyberwareManager.appendBehaviorTooltip(definition, tooltipComponents);
        IntegumentaryCyberwareManager.appendBehaviorTooltip(definition, tooltipComponents);
        SkeletonCyberwareManager.appendBehaviorTooltip(definition, tooltipComponents);
        NervousSystemCyberwareManager.appendBehaviorTooltip(definition, tooltipComponents);
        LegCyberwareManager.appendBehaviorTooltip(definition, tooltipComponents);
        if (definition.isPlaceholder()) {
            tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.placeholder_effect").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void appendStoredChipwareTooltip(ItemStack stack, TooltipContext context, List<Component> tooltipComponents) {
        NonNullList<ItemStack> storedChipware = ChipwareSocketHandler.getStoredChipware(stack, context.registries());
        int installed = 0;
        for (ItemStack chipStack : storedChipware) {
            if (!chipStack.isEmpty()) {
                installed++;
            }
        }
        if (installed <= 0) {
            return;
        }

        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.installed_chipware", installed)
                .withStyle(ChatFormatting.BLUE));
        appendStoredItems(tooltipComponents, storedChipware);
    }

    private static void appendStoredModulesTooltip(ItemStack stack, TooltipContext context, List<Component> tooltipComponents) {
        NonNullList<ItemStack> storedModules = CyberwareModuleHandler.getStoredModules(stack, context.registries());
        int installed = 0;
        for (ItemStack moduleStack : storedModules) {
            if (!moduleStack.isEmpty()) {
                installed++;
            }
        }
        if (installed <= 0) {
            return;
        }

        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.installed_modules", installed)
                .withStyle(ChatFormatting.BLUE));
        appendStoredItems(tooltipComponents, storedModules);
    }

    private static void appendStoredItems(List<Component> tooltipComponents, NonNullList<ItemStack> storedItems) {
        for (ItemStack storedItem : storedItems) {
            if (!storedItem.isEmpty()) {
                tooltipComponents.add(Component.literal(" - ").append(storedItem.getHoverName()).withStyle(ChatFormatting.GRAY));
                appendStoredItemDetails(tooltipComponents, storedItem);
            }
        }
    }

    private static void appendStoredItemDetails(List<Component> tooltipComponents, ItemStack storedItem) {
        if (storedItem.getItem() instanceof CyberwareModuleItem moduleItem) {
            tooltipComponents.add(indentedDetail(Component.translatable(
                    "tooltip.cyberneticenhancements.chrome_cost",
                    moduleItem.getDefinition().chromeCost()
            )).withStyle(ChatFormatting.DARK_AQUA));
            for (CyberwareEffect effect : moduleItem.getDefinition().effects()) {
                tooltipComponents.add(indentedDetail(effect.describe()).withStyle(ChatFormatting.GREEN));
            }
            return;
        }

        if (storedItem.getItem() instanceof ChipwareItem chipwareItem) {
            tooltipComponents.add(indentedDetail(Component.translatable(
                    "tooltip.cyberneticenhancements.chipware",
                    chipwareItem.getDefinition().chromeCost()
            )).withStyle(ChatFormatting.DARK_AQUA));
            for (CyberwareEffect effect : chipwareItem.getDefinition().effects()) {
                tooltipComponents.add(indentedDetail(effect.describe()).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    private static MutableComponent indentedDetail(Component detail) {
        return Component.literal("   ").append(detail.copy());
    }
}
