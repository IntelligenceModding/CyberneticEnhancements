package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CyberwareUpgradeHelper {
    private static final String UPGRADE_LEVEL_KEY = "CyberwareUpgradeLevel";

    private CyberwareUpgradeHelper() {
    }

    public static boolean supportsFunction(String id) {
        return FUNCTION_UPGRADES.contains(id);
    }

    private static final java.util.Set<String> FUNCTION_UPGRADES = java.util.Set.of(
            "bioconductor", "cox_2_cybersomatic_optimizer", "kerenzikov_boost_system", "memory_boost",
            "newton_module", "axolotl", "quantum_tuner", "self_ice", "excavator_arms", "basic_kiroshi_optics",
            "clairvoyant", "doomsayer", "sentry", "stalker", "the_oracle", "cockatrice",
            "behavioral_imprint_synced_faceplate", "forager_lens", "vein_reader_optics", "relic_scanner",
            "feen_x", "ram_recoup", "cargo_spine", "microgenerator", "precision_miner", "kiroshi_retrieval_suite",
            "black_mamba", "feedback_circuit", "electromag_recycler", "heal_on_kill", "optical_camo",
            "shock_n_awe", "brushstep_legs", "blood_pump", "biomonitor", "second_heart", "atomic_sensors",
            "hydrolung");

    public static int getUpgradeLevel(ItemStack stack) {
        if (!(stack.getItem() instanceof CyberwareItem cyberwareItem)) {
            return 0;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int storedLevel = tag.contains(UPGRADE_LEVEL_KEY, CompoundTag.TAG_INT) ? tag.getInt(UPGRADE_LEVEL_KEY) : 0;
        return Math.max(0, Math.min(storedLevel, getMaxUpgrades(cyberwareItem.getDefinition())));
    }

    public static int getMaxUpgrades(ItemStack stack) {
        return stack.getItem() instanceof CyberwareItem cyberwareItem ? getMaxUpgrades(cyberwareItem.getDefinition()) : 0;
    }

    public static int getMaxUpgrades(CyberwareDefinition definition) {
        return CyberwareUpgradeRegistry.get(definition.id()).levels().size();
    }

    public static boolean canUpgrade(ItemStack stack) {
        return stack.getItem() instanceof CyberwareItem cyberwareItem
                && getUpgradeLevel(stack) < getMaxUpgrades(cyberwareItem.getDefinition());
    }

    public static ItemStack createUpgradedCopy(ItemStack sourceStack, CyberwareDefinition definition) {
        ItemStack upgraded = CyberwareConditionHelper.createServiceCopy(sourceStack, definition, false);
        setUpgradeLevel(upgraded, getUpgradeLevel(sourceStack) + 1, definition);
        return upgraded;
    }

    public static int getCapacityBonus(ItemStack stack, CyberwareDefinition definition) {
        return definition.capacityBonus() + getUpgradeState(definition, getUpgradeLevel(stack)).capacityBonus();
    }

    public static int getChipSlotCount(ItemStack stack, CyberwareDefinition definition) {
        return definition.chipSlotCount() + getUpgradeState(definition, getUpgradeLevel(stack)).chipSlotBonus();
    }

    public static int getModuleSlotCount(ItemStack stack, CyberwareDefinition definition) {
        return definition.moduleSlotCount() + getUpgradeState(definition, getUpgradeLevel(stack)).moduleSlotBonus();
    }

    public static List<CyberwareEffect> getEffects(ItemStack stack, CyberwareDefinition definition) {
        EnumMap<CyberwareEffectType, Double> totals = new EnumMap<>(CyberwareEffectType.class);
        for (CyberwareEffect effect : definition.effects()) {
            totals.merge(effect.type(), effect.amount(), Double::sum);
        }
        for (CyberwareEffect effect : getUpgradeState(definition, getUpgradeLevel(stack)).effects()) {
            totals.merge(effect.type(), effect.amount(), Double::sum);
        }

        List<CyberwareEffect> combined = new ArrayList<>();
        for (CyberwareEffectType type : CyberwareEffectType.values()) {
            if (!totals.containsKey(type)) {
                continue;
            }
            combined.add(new CyberwareEffect(type, totals.get(type)));
        }
        return combined;
    }

    public static Component getUpgradeStatusComponent(ItemStack stack, CyberwareDefinition definition) {
        return Component.translatable(
                "tooltip.cyberneticenhancements.upgrades",
                getUpgradeLevel(stack),
                getMaxUpgrades(definition)
        );
    }

    public static Component getUpgradePreviewStatusComponent(ItemStack stack, CyberwareDefinition definition) {
        int currentLevel = getUpgradeLevel(stack);
        return Component.translatable(
                "screen.cyberneticenhancements.tech_station.upgrade_progress_preview",
                currentLevel,
                getMaxUpgrades(definition),
                Math.min(getMaxUpgrades(definition), currentLevel + 1),
                getMaxUpgrades(definition)
        );
    }

    public static List<Component> getUpgradeBonusTooltipLines(ItemStack stack, CyberwareDefinition definition) {
        int level = getUpgradeLevel(stack);
        List<Component> lines = describeUpgradeState(getUpgradeState(definition, level));
        appendSandevistanTuning(lines, definition, 0, level);
        appendFunctionTuning(lines, definition, 0, level);
        return lines;
    }

    public static List<Component> getUpgradePreviewLines(ItemStack stack, CyberwareDefinition definition) {
        int currentLevel = getUpgradeLevel(stack);
        int maxUpgrades = getMaxUpgrades(definition);
        if (currentLevel >= maxUpgrades) {
            return List.of();
        }

        UpgradeState current = getUpgradeState(definition, currentLevel);
        UpgradeState next = getUpgradeState(definition, currentLevel + 1);
        List<Component> lines = describeUpgradeState(diff(next, current));
        appendSandevistanTuning(lines, definition, currentLevel, currentLevel + 1);
        appendFunctionTuning(lines, definition, currentLevel, currentLevel + 1);
        return lines;
    }

    public static double functionMultiplier(PlayerCyberwareInventory inventory, String id) {
        double multiplier = 1;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof CyberwareItem item && item.getDefinition().id().equals(id)
                    && CyberwareConditionHelper.getIntegrity(stack, item.getDefinition()) > 0)
                multiplier = Math.max(multiplier, CyberwareUpgradeRegistry.get(id).at(getUpgradeLevel(stack)).functionMultiplier());
        }
        return multiplier;
    }

    public static double performance(PlayerCyberwareInventory inventory, String id, String balanceKey) {
        return CyberwareBalance.doubleValue(balanceKey) * functionMultiplier(inventory, id);
    }

    private static void appendFunctionTuning(List<Component> lines, CyberwareDefinition definition, int from, int to) {
        var data = CyberwareUpgradeRegistry.get(definition.id());
        double before = data.at(from).functionMultiplier(), after = data.at(to).functionMultiplier();
        if (Math.abs(after - before) < 0.0001) return;
        lines.add(Component.translatable("tooltip.cyberneticenhancements.function." + definition.id())
                .append(Component.literal(String.format(java.util.Locale.ROOT, ": %.0f%% -> %.0f%%", before * 100, after * 100))));
    }

    private static void appendSandevistanTuning(List<Component> lines, CyberwareDefinition definition, int from, int to) {
        var data = CyberwareUpgradeRegistry.get(definition.id());
        var current = data.at(from);
        var next = data.at(to);
        double duration = next.durationMultiplier() - current.durationMultiplier();
        double cooldown = next.cooldownMultiplier() - current.cooldownMultiplier();
        double instability = next.instabilityMultiplier() - current.instabilityMultiplier();
        if (Math.abs(duration) + Math.abs(cooldown) + Math.abs(instability) > 0.0001) {
            lines.add(Component.translatable("tooltip.cyberneticenhancements.sandevistan_tuning",
                    percent(duration), percent(cooldown), percent(instability)));
        }
    }

    private static String percent(double value) {
        return String.format(java.util.Locale.ROOT, "%+.1f", value * 100);
    }

    private static void setUpgradeLevel(ItemStack stack, int level, CyberwareDefinition definition) {
        int clampedLevel = Math.max(0, Math.min(level, getMaxUpgrades(definition)));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (clampedLevel <= 0) {
            tag.remove(UPGRADE_LEVEL_KEY);
        } else {
            tag.putInt(UPGRADE_LEVEL_KEY, clampedLevel);
        }

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private static UpgradeState getUpgradeState(CyberwareDefinition definition, int level) {
        var data = CyberwareUpgradeRegistry.get(definition.id()).at(level);
        List<CyberwareEffect> effects = new ArrayList<>();
        for (var type : CyberwareEffectType.values()) {
            Double amount = data.effects().get(type.id());
            if (amount != null && Math.abs(amount) > 0.0001) effects.add(new CyberwareEffect(type, amount));
        }
        return new UpgradeState(effects, data.capacityBonus(), data.chipSlotBonus(), data.moduleSlotBonus());
    }

    private static UpgradeState diff(UpgradeState target, UpgradeState current) {
        EnumMap<CyberwareEffectType, Double> deltas = new EnumMap<>(CyberwareEffectType.class);
        for (CyberwareEffect effect : target.effects()) {
            deltas.merge(effect.type(), effect.amount(), Double::sum);
        }
        for (CyberwareEffect effect : current.effects()) {
            deltas.merge(effect.type(), -effect.amount(), Double::sum);
        }

        List<CyberwareEffect> effects = new ArrayList<>();
        for (CyberwareEffectType type : CyberwareEffectType.values()) {
            double amount = deltas.getOrDefault(type, 0.0D);
            if (Math.abs(amount) > 0.0001D) {
                effects.add(new CyberwareEffect(type, amount));
            }
        }

        return new UpgradeState(
                effects,
                target.capacityBonus() - current.capacityBonus(),
                target.chipSlotBonus() - current.chipSlotBonus(),
                target.moduleSlotBonus() - current.moduleSlotBonus()
        );
    }

    private static List<Component> describeUpgradeState(UpgradeState state) {
        List<Component> lines = new ArrayList<>();
        for (CyberwareEffect effect : state.effects()) {
            lines.add(Component.literal("-> ").append(effect.describe()));
        }
        if (state.capacityBonus() > 0) {
            lines.add(Component.literal("-> ").append(Component.translatable("tooltip.cyberneticenhancements.capacity_bonus", state.capacityBonus())));
        }
        if (state.chipSlotBonus() > 0) {
            lines.add(Component.literal("-> ").append(Component.translatable("tooltip.cyberneticenhancements.upgrade_chip_slots", state.chipSlotBonus())));
        }
        if (state.moduleSlotBonus() > 0) {
            lines.add(Component.literal("-> ").append(Component.translatable("tooltip.cyberneticenhancements.upgrade_module_slots", state.moduleSlotBonus())));
        }
        return lines;
    }

    private record UpgradeState(
            List<CyberwareEffect> effects,
            int capacityBonus,
            int chipSlotBonus,
            int moduleSlotBonus
    ) {
    }
}
