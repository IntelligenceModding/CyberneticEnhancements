package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CirculatoryCyberwareManager {
    private static final String ADRENALINE_BOOSTER_ID = "adrenaline_booster";
    private static final String BIOMONITOR_ID = "biomonitor";
    private static final String BLACK_MAMBA_ID = "black_mamba";
    private static final String BLOOD_PUMP_ID = "blood_pump";
    private static final String CLUTCH_PADDING_ID = "clutch_padding";
    private static final String ISOMETRIC_STABILIZER_ID = "isometric_stabilizer";
    private static final String FEEDBACK_CIRCUIT_ID = "feedback_circuit";
    private static final String ELECTROMAG_RECYCLER_ID = "electromag_recycler";
    private static final String HEAL_ON_KILL_ID = "heal_on_kill";
    private static final String MICROROTORS_ID = "microrotors";
    private static final String SECOND_HEART_ID = "second_heart";
    private static final String THREATEVAC_ID = "threatevac";
    private static final String HYDROLUNG_ID = "hydrolung";

    private static final Map<UUID, Long> ADRENALINE_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> ISOMETRIC_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> FEEDBACK_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> MICROROTORS_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> THREATEVAC_READY_UNTIL_TICK = new HashMap<>();

    private CirculatoryCyberwareManager() {
    }

    public static boolean hasSpecialBehavior(String id) {
        return switch (id) {
            case ADRENALINE_BOOSTER_ID,
                 BIOMONITOR_ID,
                 BLACK_MAMBA_ID,
                 BLOOD_PUMP_ID,
                 CLUTCH_PADDING_ID,
                 ISOMETRIC_STABILIZER_ID,
                 FEEDBACK_CIRCUIT_ID,
                 ELECTROMAG_RECYCLER_ID,
                 HEAL_ON_KILL_ID,
                 MICROROTORS_ID,
                 SECOND_HEART_ID,
                 THREATEVAC_ID,
                 HYDROLUNG_ID -> true;
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

    public static void mergePassiveEffects(Player player, PlayerCyberwareInventory inventory, java.util.EnumMap<CyberwareEffectType, Double> totals) {
        int hydrolungCount = inventory.countInstalledCyberware(HYDROLUNG_ID);
        if (hydrolungCount > 0 && player.isInWaterOrBubble()) {
            totals.merge(
                    CyberwareEffectType.BLOCK_BREAK_SPEED,
                    CyberwareUpgradeHelper.performance(inventory, HYDROLUNG_ID, "circulatory.hydrolung.break_speed_bonus") * hydrolungCount,
                    Double::sum
            );
        }
    }

    public static boolean requiresRealtimeRefresh(Player player) {
        return new PlayerCyberwareInventory(player).countInstalledCyberware(HYDROLUNG_ID) > 0;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            handleIncomingPlayerDamage(event, player);
        }

        if (event.getSource().getEntity() instanceof Player player && event.getEntity() != player) {
            handleOutgoingPlayerDamage(event, player);
        }
    }

    public static void onKill(Player player, LivingEntity target) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int healOnKillCount = inventory.countInstalledCyberware(HEAL_ON_KILL_ID);
        if (healOnKillCount > 0) {
            player.heal(Math.min((float) (player.getMaxHealth() * CyberwareBalance.doubleValue("circulatory.heal_on_kill.max_heal_percent")),
                    (float) (CyberwareUpgradeHelper.performance(inventory, "heal_on_kill", "circulatory.heal_on_kill.flat_heal_per_install") * healOnKillCount)));
            applyTimedEffects(player,
                    isEliteTarget(target)
                            ? CyberwareBalance.intValue("circulatory.heal_on_kill.elite_regen_ticks")
                            : CyberwareBalance.intValue("circulatory.heal_on_kill.regen_ticks"),
                    CyberwareEffect.healthRegen(CyberwareBalance.doubleValue("circulatory.heal_on_kill.regen_bonus") * healOnKillCount));
        }

        int adrenalineBoosterCount = inventory.countInstalledCyberware(ADRENALINE_BOOSTER_ID);
        if (adrenalineBoosterCount > 0 && isEliteTarget(target)) {
            applyTimedEffects(player, CyberwareBalance.intValue("circulatory.adrenaline_booster.kill_buff_ticks"),
                    CyberwareEffect.moveSpeed(CyberwareBalance.doubleValue("circulatory.adrenaline_booster.kill_move_speed_bonus") * adrenalineBoosterCount),
                    CyberwareEffect.healthRegen(CyberwareBalance.doubleValue("circulatory.adrenaline_booster.kill_regen_bonus") * adrenalineBoosterCount));
        }

        if (healOnKillCount > 0 || adrenalineBoosterCount > 0) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int clutchPaddingCount = inventory.countInstalledCyberware(CLUTCH_PADDING_ID);
        int isometricCount = inventory.countInstalledCyberware(ISOMETRIC_STABILIZER_ID);
        if (clutchPaddingCount <= 0 && isometricCount <= 0) {
            return;
        }

        float strength = event.getStrength();
        if (clutchPaddingCount > 0) {
            strength *= Math.max((float) CyberwareBalance.doubleValue("circulatory.clutch_padding.knockback_min_factor"),
                    1.0F - (float) CyberwareBalance.doubleValue("circulatory.clutch_padding.knockback_per_install") * clutchPaddingCount);
        }
        if (isometricCount > 0) {
            strength *= player.isCrouching()
                    ? (float) CyberwareBalance.doubleValue("circulatory.isometric_stabilizer.crouch_knockback_factor")
                    : (float) CyberwareBalance.doubleValue("circulatory.isometric_stabilizer.knockback_factor");
        }
        event.setStrength(strength);
    }

    public static void clearCooldowns(Player player) {
        UUID playerId = player.getUUID();
        ADRENALINE_READY_UNTIL_TICK.remove(playerId);
        ISOMETRIC_READY_UNTIL_TICK.remove(playerId);
        FEEDBACK_READY_UNTIL_TICK.remove(playerId);
        MICROROTORS_READY_UNTIL_TICK.remove(playerId);
        THREATEVAC_READY_UNTIL_TICK.remove(playerId);
    }

    private static void handleIncomingPlayerDamage(LivingIncomingDamageEvent event, Player player) {
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        float incomingDamage = event.getAmount();
        if (incomingDamage <= 0.0F) {
            return;
        }

        long gameTime = player.level().getGameTime();
        float healthAfterHit = player.getHealth() - incomingDamage;
        boolean refreshRequired = false;

        int clutchPaddingCount = inventory.countInstalledCyberware(CLUTCH_PADDING_ID);
        if (clutchPaddingCount > 0 && isImpactDamage(event.getSource())) {
            event.setAmount(event.getAmount() * Math.max(0.55F, 1.0F - 0.12F * clutchPaddingCount));
            refreshRequired = true;
        }

          int adrenalineBoosterCount = inventory.countInstalledCyberware(ADRENALINE_BOOSTER_ID);
          if (adrenalineBoosterCount > 0
                  && isReady(ADRENALINE_READY_UNTIL_TICK, player, gameTime)
                  && (incomingDamage >= (float) CyberwareBalance.doubleValue("circulatory.adrenaline_booster.proc_min_damage")
                    || healthAfterHit <= player.getMaxHealth() * (float) CyberwareBalance.doubleValue("circulatory.adrenaline_booster.proc_health_threshold"))) {
            applyTimedEffects(player, CyberwareBalance.intValue("circulatory.adrenaline_booster.proc_ticks"),
                    CyberwareEffect.healthRegen(CyberwareBalance.doubleValue("circulatory.adrenaline_booster.proc_regen_bonus") * adrenalineBoosterCount),
                    CyberwareEffect.moveSpeed(CyberwareBalance.doubleValue("circulatory.adrenaline_booster.proc_move_speed_bonus") * adrenalineBoosterCount));
            ADRENALINE_READY_UNTIL_TICK.put(player.getUUID(), gameTime + CyberwareBalance.intValue("circulatory.adrenaline_booster.cooldown_ticks"));
            refreshRequired = true;
          }

        int isometricCount = inventory.countInstalledCyberware(ISOMETRIC_STABILIZER_ID);
          if (isometricCount > 0
                  && isReady(ISOMETRIC_READY_UNTIL_TICK, player, gameTime)
                  && (incomingDamage >= (float) CyberwareBalance.doubleValue("circulatory.isometric_stabilizer.proc_min_damage")
                    || healthAfterHit <= player.getMaxHealth() * (float) CyberwareBalance.doubleValue("circulatory.isometric_stabilizer.proc_health_threshold"))) {
            applyTimedEffects(player, CyberwareBalance.intValue("circulatory.isometric_stabilizer.proc_ticks"),
                    CyberwareEffect.damageReduction(CyberwareBalance.doubleValue("circulatory.isometric_stabilizer.damage_reduction_bonus") * isometricCount),
                    CyberwareEffect.knockbackResistance(CyberwareBalance.doubleValue("circulatory.isometric_stabilizer.knockback_resistance_bonus") * isometricCount));
            ISOMETRIC_READY_UNTIL_TICK.put(player.getUUID(), gameTime + CyberwareBalance.intValue("circulatory.isometric_stabilizer.cooldown_ticks"));
            refreshRequired = true;
          }

        int threatevacCount = inventory.countInstalledCyberware(THREATEVAC_ID);
          if (threatevacCount > 0
                  && healthAfterHit > 0.0F
                  && isReady(THREATEVAC_READY_UNTIL_TICK, player, gameTime)
                  && (incomingDamage >= (float) CyberwareBalance.doubleValue("circulatory.threatevac.proc_min_damage")
                    || healthAfterHit <= player.getMaxHealth() * (float) CyberwareBalance.doubleValue("circulatory.threatevac.proc_health_threshold"))) {
            applyTimedEffects(player, CyberwareBalance.intValue("circulatory.threatevac.proc_ticks"),
                    CyberwareEffect.moveSpeed(CyberwareBalance.doubleValue("circulatory.threatevac.move_speed_bonus") * threatevacCount),
                    CyberwareEffect.stepHeight(CyberwareBalance.doubleValue("circulatory.threatevac.step_height_bonus") * threatevacCount),
                    CyberwareEffect.safeFall(CyberwareBalance.doubleValue("circulatory.threatevac.safe_fall_bonus") * threatevacCount),
                    CyberwareEffect.damageReduction(CyberwareBalance.doubleValue("circulatory.threatevac.damage_reduction_bonus") * threatevacCount));
            THREATEVAC_READY_UNTIL_TICK.put(player.getUUID(), gameTime + CyberwareBalance.intValue("circulatory.threatevac.cooldown_ticks"));
            refreshRequired = true;
          }

        int feedbackCount = inventory.countInstalledCyberware(FEEDBACK_CIRCUIT_ID);
        int recyclerCount = inventory.countInstalledCyberware(ELECTROMAG_RECYCLER_ID);
        int totalFeedbackCount = feedbackCount + recyclerCount;
          if (totalFeedbackCount > 0
                  && isReady(FEEDBACK_READY_UNTIL_TICK, player, gameTime)
                  && isRangedThreat(event.getSource().getDirectEntity(), player)) {
            long flatRefundTicks = Math.round(feedbackCount * CyberwareUpgradeHelper.performance(inventory, "feedback_circuit", "circulatory.feedback_circuit.flat_refund_ticks")
                    + recyclerCount * CyberwareUpgradeHelper.performance(inventory, "electromag_recycler", "circulatory.electromag_recycler.flat_refund_ticks"));
            double percentRefund = Math.min(0.20D, recyclerCount * CyberwareBalance.doubleValue("circulatory.electromag_recycler.percent_refund"));
            refundTrackedCooldowns(player, flatRefundTicks, percentRefund);
            applyTimedEffects(player, CyberwareBalance.intValue("circulatory.feedback_circuit.proc_ticks"),
                    CyberwareEffect.bonusAbsorption(CyberwareBalance.doubleValue("circulatory.feedback_circuit.absorption_bonus") * totalFeedbackCount));
            FEEDBACK_READY_UNTIL_TICK.put(player.getUUID(), gameTime + CyberwareBalance.intValue("circulatory.feedback_circuit.cooldown_ticks"));
            refreshRequired = true;
          }

        if (refreshRequired) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    private static void handleOutgoingPlayerDamage(LivingIncomingDamageEvent event, Player player) {
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        LivingEntity target = event.getEntity();
        Entity directEntity = event.getSource().getDirectEntity();
        boolean meleeHit = isMeleeHit(player, directEntity);
        boolean rangedHit = isRangedHit(player, directEntity);
        long gameTime = player.level().getGameTime();
        boolean refreshRequired = false;

          int blackMambaCount = inventory.countInstalledCyberware(BLACK_MAMBA_ID);
          if (blackMambaCount > 0 && (meleeHit || rangedHit)) {
              CombatStatusManager.applyStatus(target, CombatStatusType.CORROSION, 80L + blackMambaCount * 20L, meleeHit ? 2 : 1, true);
              if (hasStatus(target, CombatStatusType.CORROSION)) {
                  event.setAmount(event.getAmount() + (float) (CyberwareUpgradeHelper.performance(inventory, "black_mamba", "circulatory.black_mamba.bonus_damage") * blackMambaCount));
              }
          }

        int microrotorsCount = inventory.countInstalledCyberware(MICROROTORS_ID);
          if (microrotorsCount > 0
                  && (meleeHit || rangedHit)
                  && isReady(MICROROTORS_READY_UNTIL_TICK, player, gameTime)) {
            applyTimedEffects(player, CyberwareBalance.intValue("circulatory.microrotors.proc_ticks"),
                    CyberwareEffect.attackSpeed(CyberwareBalance.doubleValue("circulatory.microrotors.attack_speed_bonus") * microrotorsCount),
                    CyberwareEffect.moveSpeed(CyberwareBalance.doubleValue("circulatory.microrotors.move_speed_bonus") * microrotorsCount));
            MICROROTORS_READY_UNTIL_TICK.put(player.getUUID(), gameTime + CyberwareBalance.intValue("circulatory.microrotors.cooldown_ticks"));
            refreshRequired = true;
          }

          int recyclerCount = inventory.countInstalledCyberware(ELECTROMAG_RECYCLER_ID);
          if (recyclerCount > 0 && rangedHit) {
            refundTrackedCooldowns(player,
                    Math.round(CyberwareUpgradeHelper.performance(inventory, "electromag_recycler", "circulatory.electromag_recycler.ranged_flat_refund_ticks") * recyclerCount),
                    CyberwareBalance.doubleValue("circulatory.electromag_recycler.ranged_percent_refund") * recyclerCount);
            if (recyclerCount > 0) {
                CombatStatusManager.applyStatus(target, CombatStatusType.SHOCK, 50L + recyclerCount * 10L, 1, true);
            }
            refreshRequired = true;
        }

        if (refreshRequired) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    private static void applyTimedEffects(Player player, long durationTicks, CyberwareEffect... effects) {
        TemporaryCyberwareEffectManager.addEffects(player, durationTicks, effects);
    }

    private static void refundTrackedCooldowns(Player player, long flatTicks, double percentRefund) {
        CyberwareAbilities.reduceTrackedCooldowns(player, flatTicks, percentRefund);
        ArmCyberwareManager.reduceTrackedCooldowns(player, flatTicks, percentRefund);
        FaceCyberwareManager.reduceTrackedCooldowns(player, flatTicks, percentRefund);
        FrontalCortexManager.reduceTrackedCooldowns(player, flatTicks, percentRefund);
    }

    private static boolean hasStatus(LivingEntity target, CombatStatusType type) {
        for (CombatStatusManager.ActiveStatus status : CombatStatusManager.collectActiveStatuses(target, target.level().getGameTime())) {
            if (status.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean isImpactDamage(DamageSource source) {
        return source.is(DamageTypes.FALL)
                || source.is(DamageTypes.FALLING_BLOCK)
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
    }

    private static boolean isRangedThreat(Entity directEntity, Player player) {
        return directEntity != null && directEntity != player;
    }

    private static boolean isMeleeHit(Player player, Entity directEntity) {
        return directEntity == player;
    }

    private static boolean isRangedHit(Player player, Entity directEntity) {
        return directEntity != null && directEntity != player;
    }

    private static boolean isEliteTarget(LivingEntity target) {
        return target instanceof Enemy || target.getMaxHealth() >= 30.0F;
    }

    private static boolean isReady(Map<UUID, Long> cooldowns, Player player, long gameTime) {
        return cooldowns.getOrDefault(player.getUUID(), 0L) <= gameTime;
    }
}
