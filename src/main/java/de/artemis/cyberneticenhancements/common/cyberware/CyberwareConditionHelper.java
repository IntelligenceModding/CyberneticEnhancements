package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CyberwareConditionHelper {
    private static final String INTEGRITY_KEY = "CyberwareIntegrity";

    private CyberwareConditionHelper() {
    }

    public static int getMaxIntegrity(CyberwareDefinition definition) {
        return switch (definition.tier()) {
            case TIER_1 -> 100;
            case TIER_2 -> 120;
            case TIER_3 -> 140;
            case TIER_4 -> 160;
            case TIER_5 -> 180;
        };
    }

    public static int getIntegrity(ItemStack stack, CyberwareDefinition definition) {
        int maxIntegrity = getMaxIntegrity(definition);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(INTEGRITY_KEY)) {
            return maxIntegrity;
        }
        return Math.max(0, Math.min(maxIntegrity, tag.getInt(INTEGRITY_KEY)));
    }

    public static double getIntegrityRatio(ItemStack stack, CyberwareDefinition definition) {
        int maxIntegrity = getMaxIntegrity(definition);
        if (maxIntegrity <= 0) {
            return 1.0D;
        }
        return (double) getIntegrity(stack, definition) / (double) maxIntegrity;
    }

    public static boolean isDamaged(ItemStack stack, CyberwareDefinition definition) {
        return getIntegrity(stack, definition) < getMaxIntegrity(definition);
    }

    public static boolean damage(ItemStack stack, CyberwareDefinition definition, int amount) {
        if (stack.isEmpty() || amount <= 0) {
            return false;
        }

        int current = getIntegrity(stack, definition);
        int updated = Math.max(0, current - amount);
        if (updated == current) {
            return false;
        }

        setIntegrity(stack, definition, updated);
        return true;
    }

    public static boolean repair(ItemStack stack, CyberwareDefinition definition, int amount) {
        if (stack.isEmpty() || amount <= 0) {
            return false;
        }

        int maxIntegrity = getMaxIntegrity(definition);
        int current = getIntegrity(stack, definition);
        int updated = Math.min(maxIntegrity, current + amount);
        if (updated == current) {
            return false;
        }

        setIntegrity(stack, definition, updated);
        return true;
    }

    public static void fullyRepair(ItemStack stack, CyberwareDefinition definition) {
        setIntegrity(stack, definition, getMaxIntegrity(definition));
    }

    public static ItemStack createServiceCopy(ItemStack sourceStack, CyberwareDefinition targetDefinition, boolean fullyRepair) {
        var target = de.artemis.cyberneticenhancements.common.registry.ModItems.cyberware(targetDefinition.id()).get();
        ItemStack output = sourceStack.is(target) ? sourceStack.copyWithCount(1) : new ItemStack(target);
        if (sourceStack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = sourceStack.get(DataComponents.CUSTOM_DATA).copyTag();
            output.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        if (fullyRepair) {
            fullyRepair(output, targetDefinition);
        } else {
            int sourceIntegrity = targetDefinition.id().equals(getDefinitionId(sourceStack))
                    ? getIntegrity(sourceStack, targetDefinition)
                    : getMaxIntegrity(targetDefinition);
            setIntegrity(output, targetDefinition, sourceIntegrity);
        }
        return output;
    }

    private static void setIntegrity(ItemStack stack, CyberwareDefinition definition, int integrity) {
        int clampedIntegrity = Math.max(0, Math.min(getMaxIntegrity(definition), integrity));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (clampedIntegrity >= getMaxIntegrity(definition)) {
            tag.remove(INTEGRITY_KEY);
        } else {
            tag.putInt(INTEGRITY_KEY, clampedIntegrity);
        }

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private static String getDefinitionId(ItemStack stack) {
        if (stack.getItem() instanceof de.artemis.cyberneticenhancements.common.item.CyberwareItem cyberwareItem) {
            return cyberwareItem.getDefinition().id();
        }
        return "";
    }
}
