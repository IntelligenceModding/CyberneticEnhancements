package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public final class FrontalCortexManager {
    private static final String CAMILLO_RAM_MANAGER_ID = "camillo_ram_manager";
    private static final String RAM_REALLOCATOR_ID = "ram_reallocator";
    private static final String BIOCONDUCTOR_ID = "bioconductor";
    private static final String COX_2_ID = "cox_2_cybersomatic_optimizer";
    private static final String EX_DISK_ID = "ex_disk";
    private static final String KERENZIKOV_BOOST_SYSTEM_ID = "kerenzikov_boost_system";
    private static final String MEMORY_BOOST_ID = "memory_boost";
    private static final String NEWTON_MODULE_ID = "newton_module";
    private static final String AXOLOTL_ID = "axolotl";
    private static final String QUANTUM_TUNER_ID = "quantum_tuner";
    private static final String RAM_UPGRADE_ID = "ram_upgrade";
    private static final String SELF_ICE_ID = "self_ice";

    private static final Map<UUID, Long> EMERGENCY_REFUND_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> SELF_ICE_COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Integer> SELF_ICE_COOLDOWN_TOTAL_SECONDS = new HashMap<>();
    private static final Map<UUID, Long> QUANTUM_TUNER_COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Integer> QUANTUM_TUNER_COOLDOWN_TOTAL_SECONDS = new HashMap<>();

    private FrontalCortexManager() {
    }

    public static boolean hasSpecialBehavior(String id) {
        return switch (id) {
            case CAMILLO_RAM_MANAGER_ID,
                 RAM_REALLOCATOR_ID,
                 BIOCONDUCTOR_ID,
                 COX_2_ID,
                 EX_DISK_ID,
                 KERENZIKOV_BOOST_SYSTEM_ID,
                 MEMORY_BOOST_ID,
                 NEWTON_MODULE_ID,
                 AXOLOTL_ID,
                 QUANTUM_TUNER_ID,
                 RAM_UPGRADE_ID,
                 SELF_ICE_ID -> true;
            default -> false;
        };
    }

    public static void appendBehaviorTooltip(CyberwareDefinition definition, List<Component> tooltipComponents) {
        if (!hasSpecialBehavior(definition.id())) {
            return;
        }

        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.special." + definition.id())
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    public static void mergePassiveEffects(Player player, PlayerCyberwareInventory inventory, EnumMap<CyberwareEffectType, Double> totals) {
        int boostSystemCount = inventory.countInstalledCyberware(KERENZIKOV_BOOST_SYSTEM_ID);
        if (boostSystemCount <= 0) {
            return;
        }

        if (hasReflexPair(inventory)) {
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.08D * boostSystemCount);
            merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.05D * boostSystemCount);
        } else {
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.02D * boostSystemCount);
        }
    }

    public static long adjustTemporaryEffectDuration(Player player, long baseDurationTicks) {
        if (baseDurationTicks <= 0L || player.level().isClientSide()) {
            return baseDurationTicks;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        double multiplier = 1.0D + getStatusDurationBonus(inventory);
        return Math.max(20L, Math.round(baseDurationTicks * multiplier));
    }

    public static long adjustCyberwareCooldownTicks(Player player, PlayerCyberwareInventory inventory, long baseCooldownTicks, boolean allowEmergencyRefund) {
        long reducedTicks = Math.max(20L, Math.round(baseCooldownTicks
                * (1.0D - getCooldownReduction(inventory))
                * CyberstrainManager.getAbilityCooldownMultiplier(player, inventory)));

        if (!allowEmergencyRefund || baseCooldownTicks < 20L * 20L) {
            return reducedTicks;
        }

        double refundPercent = 0.0D;
        long internalCooldownTicks = 0L;
        if (inventory.countInstalledCyberware(RAM_REALLOCATOR_ID) > 0) {
            refundPercent = CyberwareBalance.doubleValue("frontal.ram_reallocator.emergency_refund_percent");
            internalCooldownTicks = CyberwareBalance.intValue("frontal.ram_reallocator.emergency_refund_cooldown_ticks");
        } else if (inventory.countInstalledCyberware(CAMILLO_RAM_MANAGER_ID) > 0) {
            refundPercent = CyberwareBalance.doubleValue("frontal.camillo_ram_manager.emergency_refund_percent");
            internalCooldownTicks = CyberwareBalance.intValue("frontal.camillo_ram_manager.emergency_refund_cooldown_ticks");
        }

        if (refundPercent <= 0.0D) {
            return reducedTicks;
        }

        long gameTime = player.level().getGameTime();
        if (EMERGENCY_REFUND_READY_UNTIL_TICK.getOrDefault(player.getUUID(), 0L) > gameTime) {
            return reducedTicks;
        }

        EMERGENCY_REFUND_READY_UNTIL_TICK.put(player.getUUID(), gameTime + internalCooldownTicks);
        return Math.max(20L, Math.round(reducedTicks * (1.0D - refundPercent)));
    }

    public static int getAdditionalCyberstrain(PlayerCyberwareInventory inventory) {
        return inventory.countInstalledCyberware(BIOCONDUCTOR_ID) * 4
                + inventory.countInstalledCyberware(COX_2_ID) * 8;
    }

    public static int getReflexExtensionTicks(PlayerCyberwareInventory inventory) {
        return hasReflexPair(inventory)
                ? (int) Math.round(inventory.countInstalledCyberware(KERENZIKOV_BOOST_SYSTEM_ID) * CyberwareBalance.intValue("frontal.kerenzikov_boost_system.reflex_extension_ticks")
                        * CyberwareUpgradeHelper.functionMultiplier(inventory, KERENZIKOV_BOOST_SYSTEM_ID))
                : 0;
    }

    public static void onKill(Player player, LivingEntity target) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int memoryBoostCount = inventory.countInstalledCyberware(MEMORY_BOOST_ID);
        int newtonCount = inventory.countInstalledCyberware(NEWTON_MODULE_ID);
        int axolotlCount = inventory.countInstalledCyberware(AXOLOTL_ID);
        if (memoryBoostCount <= 0 && newtonCount <= 0 && axolotlCount <= 0) {
            return;
        }

        long flatRefundTicks = Math.round(memoryBoostCount * CyberwareUpgradeHelper.performance(inventory, "memory_boost", "frontal.memory_boost.flat_refund_ticks"));
        double percentRefund = newtonCount * CyberwareUpgradeHelper.performance(inventory, "newton_module", "frontal.newton_module.percent_refund");
        if (axolotlCount > 0) {
            percentRefund += axolotlCount * (isEliteTarget(target)
                    ? CyberwareUpgradeHelper.performance(inventory, "axolotl", "frontal.axolotl.elite_percent_refund")
                    : CyberwareUpgradeHelper.performance(inventory, "axolotl", "frontal.axolotl.percent_refund"));
        }
        percentRefund = Math.min(percentRefund, CyberwareBalance.doubleValue("frontal.max_kill_refund_percent"));

        CyberwareAbilities.reduceTrackedCooldowns(player, flatRefundTicks, percentRefund);
        ArmCyberwareManager.reduceTrackedCooldowns(player, flatRefundTicks, percentRefund);
        reduceOwnCooldowns(player, flatRefundTicks, percentRefund);
    }

    public static void activateAuxiliary(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        boolean hasSelfIce = inventory.countInstalledCyberware(SELF_ICE_ID) > 0;
        boolean hasQuantumTuner = inventory.countInstalledCyberware(QUANTUM_TUNER_ID) > 0;
        boolean hasNegativeCyberStatus = hasNegativeCyberStatus(player);

        if (hasSelfIce && hasNegativeCyberStatus && isCooldownReady(SELF_ICE_COOLDOWN_UNTIL_TICK, player)) {
            activateSelfIce(player, inventory);
            return;
        }
        if (hasQuantumTuner && isCooldownReady(QUANTUM_TUNER_COOLDOWN_UNTIL_TICK, player) && findQuantumResetTarget(player) != QuantumResetTarget.NONE) {
            activateQuantumTuner(player, inventory);
            return;
        }
        if (hasSelfIce && isCooldownReady(SELF_ICE_COOLDOWN_UNTIL_TICK, player)) {
            activateSelfIce(player, inventory);
            return;
        }
        if (hasSelfIce && hasNegativeCyberStatus) {
            notifyCooldown(player, "item.cyberneticenhancements.self_ice", getSelfIceCooldownSecondsRemaining(player));
            return;
        }
        if (hasQuantumTuner && !isCooldownReady(QUANTUM_TUNER_COOLDOWN_UNTIL_TICK, player)) {
            notifyCooldown(player, "item.cyberneticenhancements.quantum_tuner", getQuantumTunerCooldownSecondsRemaining(player));
            return;
        }
        if (hasQuantumTuner) {
            notify(player, "message.cyberneticenhancements.aux.quantum_tuner.no_target");
            return;
        }
        if (hasSelfIce) {
            notifyCooldown(player, "item.cyberneticenhancements.self_ice", getSelfIceCooldownSecondsRemaining(player));
            return;
        }

        notify(player, "message.cyberneticenhancements.aux.no_active_ability");
    }

    public static int getSelfIceCooldownSecondsRemaining(Player player) {
        return getRemainingSeconds(SELF_ICE_COOLDOWN_UNTIL_TICK, player);
    }

    public static int getSelfIceCooldownTotalSeconds(Player player) {
        return SELF_ICE_COOLDOWN_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static int getQuantumTunerCooldownSecondsRemaining(Player player) {
        return getRemainingSeconds(QUANTUM_TUNER_COOLDOWN_UNTIL_TICK, player);
    }

    public static int getQuantumTunerCooldownTotalSeconds(Player player) {
        return QUANTUM_TUNER_COOLDOWN_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static void clearCooldowns(Player player) {
        UUID playerId = player.getUUID();
        EMERGENCY_REFUND_READY_UNTIL_TICK.remove(playerId);
        SELF_ICE_COOLDOWN_UNTIL_TICK.remove(playerId);
        SELF_ICE_COOLDOWN_TOTAL_SECONDS.remove(playerId);
        QUANTUM_TUNER_COOLDOWN_UNTIL_TICK.remove(playerId);
        QUANTUM_TUNER_COOLDOWN_TOTAL_SECONDS.remove(playerId);
    }

    public static void reduceTrackedCooldowns(Player player, long flatTicks, double percentRefund) {
        reduceOwnCooldowns(player, flatTicks, percentRefund);
    }

    private static void activateSelfIce(Player player, PlayerCyberwareInventory inventory) {
        CyberstrainManager.purgeNegativeStatuses(player, true);
        CombatStatusManager.clearStatuses(player);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        CyberstrainManager.reduceInstability(player, CyberwareUpgradeHelper.performance(inventory, "self_ice", "frontal.self_ice.instability_reduction"));
        TemporaryCyberwareEffectManager.addEffects(player, CyberwareBalance.intValue("frontal.self_ice.effect_ticks"),
                CyberwareEffect.damageReduction(CyberwareBalance.doubleValue("frontal.self_ice.damage_reduction_bonus")),
                CyberwareEffect.knockbackResistance(CyberwareBalance.doubleValue("frontal.self_ice.knockback_resistance_bonus")),
                CyberwareEffect.moveSpeed(CyberwareBalance.doubleValue("frontal.self_ice.move_speed_bonus")));

        long cooldownTicks = adjustCyberwareCooldownTicks(player, inventory, CyberwareBalance.intValue("frontal.self_ice.cooldown_seconds") * 20L, true);
        SELF_ICE_COOLDOWN_UNTIL_TICK.put(player.getUUID(), player.level().getGameTime() + cooldownTicks);
        SELF_ICE_COOLDOWN_TOTAL_SECONDS.put(player.getUUID(), (int) Math.max(1L, (cooldownTicks + 19L) / 20L));
        CyberwareEffects.refreshPlayerCyberware(player);
        notify(player, "message.cyberneticenhancements.aux.self_ice");
    }

    private static void activateQuantumTuner(Player player, PlayerCyberwareInventory inventory) {
        QuantumResetTarget target = findQuantumResetTarget(player);
        if (target == QuantumResetTarget.NONE) {
            notify(player, "message.cyberneticenhancements.aux.quantum_tuner.no_target");
            return;
        }

        clearQuantumResetTarget(player, target);
        CyberstrainManager.onCyberwareActivated(player, CyberwareBalance.doubleValue("frontal.quantum_tuner.activation_load"));
        long cooldownTicks = adjustCyberwareCooldownTicks(player, inventory, CyberwareBalance.intValue("frontal.quantum_tuner.cooldown_seconds") * 20L, true);
        QUANTUM_TUNER_COOLDOWN_UNTIL_TICK.put(player.getUUID(), player.level().getGameTime() + cooldownTicks);
        QUANTUM_TUNER_COOLDOWN_TOTAL_SECONDS.put(player.getUUID(), (int) Math.max(1L, (cooldownTicks + 19L) / 20L));
        CyberwareEffects.refreshPlayerCyberware(player);
        notify(player, "message.cyberneticenhancements.aux.quantum_tuner");
    }

    private static boolean hasReflexPair(PlayerCyberwareInventory inventory) {
        return inventory.hasInstalledCyberware("kerenzikov")
                || inventory.hasInstalledCyberware("synaptic_accelerator")
                || inventory.hasInstalledCyberware("reflex_tuner");
    }

    private static double getCooldownReduction(PlayerCyberwareInventory inventory) {
        double reduction = 0.0D;
        reduction += inventory.countInstalledCyberware(RAM_UPGRADE_ID) * CyberwareBalance.doubleValue("frontal.ram_upgrade.cooldown_reduction");
        reduction += inventory.countInstalledCyberware(EX_DISK_ID) * CyberwareBalance.doubleValue("frontal.ex_disk.cooldown_reduction");
        reduction += inventory.countInstalledCyberware(BIOCONDUCTOR_ID) * CyberwareUpgradeHelper.performance(inventory, "bioconductor", "frontal.bioconductor.cooldown_reduction");
        reduction += inventory.countInstalledCyberware(COX_2_ID) * CyberwareUpgradeHelper.performance(inventory, "cox_2_cybersomatic_optimizer", "frontal.cox_2_cybersomatic_optimizer.cooldown_reduction");
        reduction += inventory.countInstalledCyberware(QUANTUM_TUNER_ID) * CyberwareUpgradeHelper.performance(inventory, "quantum_tuner", "frontal.quantum_tuner.cooldown_reduction");
        return Math.min(reduction, CyberwareBalance.doubleValue("frontal.max_cooldown_reduction"));
    }

    private static double getStatusDurationBonus(PlayerCyberwareInventory inventory) {
        double bonus = 0.0D;
        bonus += inventory.countInstalledCyberware(RAM_UPGRADE_ID) * CyberwareBalance.doubleValue("frontal.ram_upgrade.status_duration_bonus");
        bonus += inventory.countInstalledCyberware(EX_DISK_ID) * CyberwareBalance.doubleValue("frontal.ex_disk.status_duration_bonus");
        bonus += inventory.countInstalledCyberware(BIOCONDUCTOR_ID) * CyberwareBalance.doubleValue("frontal.bioconductor.status_duration_bonus");
        bonus += inventory.countInstalledCyberware(COX_2_ID) * CyberwareBalance.doubleValue("frontal.cox_2_cybersomatic_optimizer.status_duration_bonus");
        return Math.min(bonus, CyberwareBalance.doubleValue("frontal.max_status_duration_bonus"));
    }

    private static boolean hasNegativeCyberStatus(Player player) {
        return CyberstrainManager.getSuppressionSecondsRemaining(player) > 0
                || CyberstrainManager.getRamJoltSecondsRemaining(player) > 0
                || CyberstrainManager.isPsychosisActive(player);
    }

    private static boolean isEliteTarget(LivingEntity target) {
        return target.getMaxHealth() >= 30.0F || target instanceof Monster;
    }

    private static void reduceOwnCooldowns(Player player, long flatTicks, double percentRefund) {
        reduceCooldown(SELF_ICE_COOLDOWN_UNTIL_TICK, player, flatTicks, percentRefund);
        reduceCooldown(QUANTUM_TUNER_COOLDOWN_UNTIL_TICK, player, flatTicks, percentRefund);
    }

    private static void reduceCooldown(Map<UUID, Long> cooldowns, Player player, long flatTicks, double percentRefund) {
        long gameTime = player.level().getGameTime();
        long until = cooldowns.getOrDefault(player.getUUID(), 0L);
        if (until <= gameTime) {
            return;
        }

        long remaining = until - gameTime;
        if (percentRefund > 0.0D) {
            remaining = Math.max(0L, Math.round(remaining * (1.0D - percentRefund)));
        }
        if (flatTicks > 0L) {
            remaining = Math.max(0L, remaining - flatTicks);
        }

        if (remaining <= 0L) {
            cooldowns.remove(player.getUUID());
            return;
        }
        cooldowns.put(player.getUUID(), gameTime + remaining);
    }

    private static boolean isCooldownReady(Map<UUID, Long> cooldowns, Player player) {
        return cooldowns.getOrDefault(player.getUUID(), 0L) <= player.level().getGameTime();
    }

    private static int getRemainingSeconds(Map<UUID, Long> cooldowns, Player player) {
        long remainingTicks = Math.max(0L, cooldowns.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
        return (int) ((remainingTicks + 19L) / 20L);
    }

    private static QuantumResetTarget findQuantumResetTarget(Player player) {
        int bestSeconds = 0;
        QuantumResetTarget bestTarget = QuantumResetTarget.NONE;

        bestTarget = pickBetterTarget(bestTarget, bestSeconds, QuantumResetTarget.ABILITY, CyberwareAbilities.getAbilityCooldownSecondsRemaining(player));
        bestSeconds = bestTarget == QuantumResetTarget.ABILITY ? CyberwareAbilities.getAbilityCooldownSecondsRemaining(player) : bestSeconds;

        bestTarget = pickBetterTarget(bestTarget, bestSeconds, QuantumResetTarget.BIOMONITOR, CyberwareAbilities.getBiomonitorCooldownSecondsRemaining(player));
        bestSeconds = bestTarget == QuantumResetTarget.BIOMONITOR ? CyberwareAbilities.getBiomonitorCooldownSecondsRemaining(player) : bestSeconds;

        bestTarget = pickBetterTarget(bestTarget, bestSeconds, QuantumResetTarget.BLOOD_PUMP, CyberwareAbilities.getBloodPumpCooldownSecondsRemaining(player));
        bestSeconds = bestTarget == QuantumResetTarget.BLOOD_PUMP ? CyberwareAbilities.getBloodPumpCooldownSecondsRemaining(player) : bestSeconds;

        bestTarget = pickBetterTarget(bestTarget, bestSeconds, QuantumResetTarget.SECOND_HEART, CyberwareAbilities.getSecondHeartCooldownSecondsRemaining(player));
        bestSeconds = bestTarget == QuantumResetTarget.SECOND_HEART ? CyberwareAbilities.getSecondHeartCooldownSecondsRemaining(player) : bestSeconds;

        bestTarget = pickBetterTarget(bestTarget, bestSeconds, QuantumResetTarget.REFLEX_TUNER, CyberwareAbilities.getReflexTunerCooldownSecondsRemaining(player));
        bestSeconds = bestTarget == QuantumResetTarget.REFLEX_TUNER ? CyberwareAbilities.getReflexTunerCooldownSecondsRemaining(player) : bestSeconds;

        bestTarget = pickBetterTarget(bestTarget, bestSeconds, QuantumResetTarget.ARMS, ArmCyberwareManager.getCooldownSecondsRemaining(player));
        bestSeconds = bestTarget == QuantumResetTarget.ARMS ? ArmCyberwareManager.getCooldownSecondsRemaining(player) : bestSeconds;

        bestTarget = pickBetterTarget(bestTarget, bestSeconds, QuantumResetTarget.SELF_ICE, getSelfIceCooldownSecondsRemaining(player));

        return bestTarget;
    }

    private static QuantumResetTarget pickBetterTarget(QuantumResetTarget currentTarget, int currentSeconds, QuantumResetTarget candidateTarget, int candidateSeconds) {
        if (candidateSeconds <= currentSeconds || candidateSeconds <= 0) {
            return currentTarget;
        }
        return candidateTarget;
    }

    private static void clearQuantumResetTarget(Player player, QuantumResetTarget target) {
        switch (target) {
            case ABILITY -> CyberwareAbilities.clearAbilityCooldown(player);
            case BIOMONITOR -> CyberwareAbilities.clearBiomonitorCooldown(player);
            case BLOOD_PUMP -> CyberwareAbilities.clearBloodPumpCooldown(player);
            case SECOND_HEART -> CyberwareAbilities.clearSecondHeartCooldown(player);
            case REFLEX_TUNER -> CyberwareAbilities.clearReflexTunerCooldown(player);
            case ARMS -> ArmCyberwareManager.clearCooldown(player);
            case SELF_ICE -> {
                SELF_ICE_COOLDOWN_UNTIL_TICK.remove(player.getUUID());
                SELF_ICE_COOLDOWN_TOTAL_SECONDS.remove(player.getUUID());
            }
            case NONE -> {
            }
        }
    }

    private static void merge(EnumMap<CyberwareEffectType, Double> totals, CyberwareEffectType type, double amount) {
        totals.merge(type, amount, type.isMobEffect() ? Math::max : Double::sum);
    }

    private static void notifyCooldown(Player player, String abilityNameKey, int seconds) {
        notify(player, "message.cyberneticenhancements.aux.cooldown", Component.translatable(abilityNameKey), seconds);
    }

    private static void notify(Player player, String translationKey, Object... args) {
        player.displayClientMessage(Component.translatable(translationKey, args).withStyle(ChatFormatting.AQUA), true);
    }

    private enum QuantumResetTarget {
        NONE,
        ABILITY,
        BIOMONITOR,
        BLOOD_PUMP,
        SECOND_HEART,
        REFLEX_TUNER,
        ARMS,
        SELF_ICE
    }
}
