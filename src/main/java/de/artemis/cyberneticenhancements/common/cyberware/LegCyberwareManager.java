package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LegCyberwareManager {
    private static final String FORTIFIED_ANKLES_ID = "fortified_ankles";
    private static final String JENKINS_TENDONS_ID = "jenkins_tendons";
    private static final String LEEROY_LIGAMENT_SYSTEM_ID = "leeroy_ligament_system";
    private static final String LYNX_PAWS_ID = "lynx_paws";
    private static final String REINFORCED_TENDONS_ID = "reinforced_tendons";
    private static final String BRUSHSTEP_LEGS_ID = "brushstep_legs";

    private static final String DASH_PISTON_ID = "dash_piston";
    private static final String FALL_DAMPER_ID = "fall_damper";
    private static final String SPRINT_MOTOR_ID = "sprint_motor";
    private static final String CLIMBING_SERVO_ID = "climbing_servo";
    private static final String STEALTH_FOOT_ID = "stealth_foot";

    private static final Map<UUID, Integer> CHARGED_JUMP_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> FORTIFIED_CHARGE_WINDOW_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> SPRINT_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> AIR_TICKS = new HashMap<>();
    private static final Map<UUID, Float> MAX_AIR_FALL_DISTANCE = new HashMap<>();
    private static final Map<UUID, Boolean> LAST_ON_GROUND = new HashMap<>();
    private static final Map<UUID, Boolean> LAST_SPRINTING = new HashMap<>();
    private static final Map<UUID, Boolean> MANAGED_LYNX_SILENCE = new HashMap<>();
    private static final Map<UUID, Long> DASH_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> LEEROY_IMPACT_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Boolean> EXTRA_JUMP_USED = new HashMap<>();
    private static final double MIN_MOVEMENT_SQR = CyberwareBalance.doubleValue("legs.movement.min_sqr");
    private static final double MIN_CHARGE_MOVEMENT_SQR = CyberwareBalance.doubleValue("legs.movement.charge_min_sqr");
    private static final int FORTIFIED_MAX_CHARGE_TICKS = CyberwareBalance.intValue("legs.fortified_ankles.max_charge_ticks");
    private static final int FORTIFIED_RELEASE_WINDOW_TICKS = CyberwareBalance.intValue("legs.fortified_ankles.release_window_ticks");

    private LegCyberwareManager() {
    }

    public static boolean hasSpecialBehavior(String id) {
        return switch (id) {
            case FORTIFIED_ANKLES_ID,
                 JENKINS_TENDONS_ID,
                 LEEROY_LIGAMENT_SYSTEM_ID,
                 LYNX_PAWS_ID,
                 REINFORCED_TENDONS_ID,
                 BRUSHSTEP_LEGS_ID -> true;
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

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int fortifiedCount = inventory.countInstalledCyberware(FORTIFIED_ANKLES_ID);
        int jenkinsCount = inventory.countInstalledCyberware(JENKINS_TENDONS_ID);
        int leeroyCount = inventory.countInstalledCyberware(LEEROY_LIGAMENT_SYSTEM_ID);
        int lynxCount = inventory.countInstalledCyberware(LYNX_PAWS_ID);
        int reinforcedCount = inventory.countInstalledCyberware(REINFORCED_TENDONS_ID);
        int dashPistonCount = countInstalledLegModules(inventory, DASH_PISTON_ID);
        int fallDamperCount = countInstalledLegModules(inventory, FALL_DAMPER_ID);
        int sprintMotorCount = countInstalledLegModules(inventory, SPRINT_MOTOR_ID);
        int climbingServoCount = countInstalledLegModules(inventory, CLIMBING_SERVO_ID);
        int stealthFootCount = countInstalledLegModules(inventory, STEALTH_FOOT_ID);
        int brushstepCount = inventory.countInstalledCyberware(BRUSHSTEP_LEGS_ID);

        UUID playerId = player.getUUID();
        boolean onGround = player.onGround();
        boolean wasOnGround = LAST_ON_GROUND.getOrDefault(playerId, onGround);
        boolean sprinting = isEffectiveSprinting(player);
        boolean wasSprinting = LAST_SPRINTING.getOrDefault(playerId, false);

        updateSprintTicks(playerId, sprinting);
        updateChargeTicks(player, playerId, fortifiedCount);

        if (!onGround && wasOnGround && player.getDeltaMovement().y > 0.0D) {
            handleTakeoff(player, fortifiedCount, jenkinsCount, leeroyCount, sprinting || wasSprinting);
        } else if (onGround && !wasOnGround) {
            handleLanding(player, fortifiedCount, leeroyCount, lynxCount, reinforcedCount, fallDamperCount);
            EXTRA_JUMP_USED.put(playerId, false);
        }

        if (onGround || player.onClimbable() || player.isInWaterOrBubble() || player.getAbilities().flying) {
            EXTRA_JUMP_USED.put(playerId, false);
        }

        if ((dashPistonCount > 0 || leeroyCount > 0) && sprinting && !wasSprinting && onGround) {
            maybeTriggerSprintBurst(player, dashPistonCount, leeroyCount);
        }
        if (leeroyCount > 0) {
            tryLeeroyShoulderCheck(player, leeroyCount, sprinting || wasSprinting);
        }

        if (climbingServoCount > 0) {
            applyClimbingServo(player, climbingServoCount);
        }
        if (brushstepCount > 0) {
            applyBrushstepAssistance(player, brushstepCount);
        }

        manageLynxSilence(player, lynxCount, stealthFootCount);
        if (lynxCount > 0 || stealthFootCount > 0) {
            suppressStealthAggro(player, lynxCount, stealthFootCount);
        } else {
            clearManagedLynxSilence(player);
        }

        updateAirborneState(player, playerId, onGround);
        LAST_ON_GROUND.put(playerId, onGround);
        LAST_SPRINTING.put(playerId, sprinting);
    }

    public static void mergePassiveEffects(Player player, PlayerCyberwareInventory inventory, java.util.EnumMap<CyberwareEffectType, Double> totals) {
        int sprintTicks = SPRINT_TICKS.getOrDefault(player.getUUID(), 0);
        int jenkinsCount = inventory.countInstalledCyberware(JENKINS_TENDONS_ID);
        int leeroyCount = inventory.countInstalledCyberware(LEEROY_LIGAMENT_SYSTEM_ID);
        int lynxCount = inventory.countInstalledCyberware(LYNX_PAWS_ID);
        int sprintMotorCount = countInstalledLegModules(inventory, SPRINT_MOTOR_ID);
        int climbingServoCount = countInstalledLegModules(inventory, CLIMBING_SERVO_ID);
        int stealthFootCount = countInstalledLegModules(inventory, STEALTH_FOOT_ID);
        int brushstepCount = inventory.countInstalledCyberware(BRUSHSTEP_LEGS_ID);

        if (jenkinsCount > 0) {
            double jenkinsRamp = Mth.clamp(sprintTicks / 10.0D, 0.0D, 1.0D);
            if (jenkinsRamp > 0.0D) {
                merge(totals, CyberwareEffectType.MOVEMENT_SPEED, (0.04D + 0.10D * jenkinsRamp) * jenkinsCount);
                merge(totals, CyberwareEffectType.JUMP_POWER, (0.03D + 0.07D * jenkinsRamp) * jenkinsCount);
                merge(totals, CyberwareEffectType.STEP_HEIGHT, 0.12D * jenkinsCount * jenkinsRamp);
                merge(totals, CyberwareEffectType.SAFE_FALL_DISTANCE, 0.8D * jenkinsCount * jenkinsRamp);
            }
        }

        if (leeroyCount > 0) {
            double leeroyRamp = Mth.clamp((sprintTicks + 2) / 12.0D, 0.0D, 1.0D);
            if (leeroyRamp > 0.0D) {
                merge(totals, CyberwareEffectType.MOVEMENT_SPEED, (0.10D + 0.22D * leeroyRamp) * leeroyCount);
                merge(totals, CyberwareEffectType.JUMP_POWER, (0.05D + 0.12D * leeroyRamp) * leeroyCount);
                merge(totals, CyberwareEffectType.STEP_HEIGHT, 0.30D * leeroyCount * leeroyRamp);
                merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.10D * leeroyCount * leeroyRamp);
                merge(totals, CyberwareEffectType.SAFE_FALL_DISTANCE, 1.5D * leeroyCount * leeroyRamp);
            }
        }

        if (sprintMotorCount > 0 && sprintTicks >= 4) {
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.06D * sprintMotorCount);
            merge(totals, CyberwareEffectType.JUMP_POWER, 0.05D * sprintMotorCount);
        }

        boolean stealthStride = isStealthStride(player);
        if (lynxCount > 0 && stealthStride) {
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.08D * lynxCount);
            merge(totals, CyberwareEffectType.SAFE_FALL_DISTANCE, 1.5D * lynxCount);
        }

        if (stealthFootCount > 0 && stealthStride) {
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.06D * stealthFootCount);
            merge(totals, CyberwareEffectType.STEP_HEIGHT, 0.15D * stealthFootCount);
        }

        if (brushstepCount > 0 && isBrushstepTerrain(player)) {
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, CyberwareUpgradeHelper.performance(inventory, "brushstep_legs", "legs.brushstep_speed_bonus") * brushstepCount);
            merge(totals, CyberwareEffectType.STEP_HEIGHT, CyberwareBalance.doubleValue("legs.brushstep_step_height_bonus") * brushstepCount);
            merge(totals, CyberwareEffectType.SAFE_FALL_DISTANCE, 1.0D * brushstepCount);
        }

        if (climbingServoCount > 0 && shouldClimb(player)) {
            merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.08D * climbingServoCount);
            merge(totals, CyberwareEffectType.SAFE_FALL_DISTANCE, 1.0D * climbingServoCount);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        if (!event.getSource().is(DamageTypes.FALL)) {
            PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
            int brushstepCount = inventory.countInstalledCyberware(BRUSHSTEP_LEGS_ID);
            if (brushstepCount > 0 && event.getSource().is(DamageTypes.SWEET_BERRY_BUSH)) {
                event.setAmount(0.0F);
            }
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int fortifiedCount = inventory.countInstalledCyberware(FORTIFIED_ANKLES_ID);
        int leeroyCount = inventory.countInstalledCyberware(LEEROY_LIGAMENT_SYSTEM_ID);
        int lynxCount = inventory.countInstalledCyberware(LYNX_PAWS_ID);
        int reinforcedCount = inventory.countInstalledCyberware(REINFORCED_TENDONS_ID);
        int fallDamperCount = countInstalledLegModules(inventory, FALL_DAMPER_ID);

        if (fortifiedCount > 0) {
            event.setAmount(event.getAmount() * Math.max(0.35F, 1.0F - 0.18F * fortifiedCount));
        }
        if (leeroyCount > 0) {
            event.setAmount(event.getAmount() * Math.max(0.35F, 1.0F - 0.18F * leeroyCount));
        }
        if (lynxCount > 0) {
            event.setAmount(event.getAmount() * Math.max(0.35F, 1.0F - 0.20F * lynxCount));
        }
        if (reinforcedCount > 0) {
            event.setAmount(event.getAmount() * Math.max(0.25F, 1.0F - 0.25F * reinforcedCount));
        }
        if (fallDamperCount > 0) {
            event.setAmount(event.getAmount() * Math.max(0.10F, 1.0F - 0.45F * fallDamperCount));
        }
    }

    public static void activateMidairJump(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int reinforcedCount = inventory.countInstalledCyberware(REINFORCED_TENDONS_ID);
        if (reinforcedCount <= 0) {
            return;
        }

        UUID playerId = player.getUUID();
        if (player.onClimbable()
                || player.isInWaterOrBubble()
                || player.getAbilities().flying
                || EXTRA_JUMP_USED.getOrDefault(playerId, false)) {
            return;
        }

        boolean airborneEnough = !player.onGround()
                || AIR_TICKS.getOrDefault(playerId, 0) > 0
                || Math.abs(player.getDeltaMovement().y) > 0.01D
                || player.fallDistance > 0.0F;
        if (!airborneEnough) {
            return;
        }

        int dashPistonCount = countInstalledLegModules(inventory, DASH_PISTON_ID);
        Vec3 motion = player.getDeltaMovement();
        Vec3 forward = horizontalLook(player);
        double jumpPowerBonus = CyberwareEffects.collectEffectTotals(player)
                .getOrDefault(CyberwareEffectType.JUMP_POWER, 0.0D);
        double targetJumpVelocity = 0.56D + 0.48D * jumpPowerBonus + 0.04D * dashPistonCount;
        double forwardBoost = 0.06D + (player.isSprinting() ? 0.08D : 0.0D) + 0.04D * dashPistonCount;

        player.setDeltaMovement(
                motion.x + forward.x * forwardBoost,
                targetJumpVelocity,
                motion.z + forward.z * forwardBoost
        );
        player.fallDistance = 0.0F;
        player.hasImpulse = true;
        player.hurtMarked = true;
        PlayerMotionSyncHelper.sync(player);
        EXTRA_JUMP_USED.put(playerId, true);

        TemporaryCyberwareEffectManager.addEffects(player, 20L,
                CyberwareEffect.moveSpeed(0.04D * reinforcedCount),
                CyberwareEffect.safeFall(2.0D * reinforcedCount));
        CyberwareEffects.refreshPlayerCyberware(player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.35F, 1.45F);
    }

    public static boolean requiresRealtimeRefresh(Player player) {
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        if (inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.LEGS) != null) {
            return true;
        }
        return inventory.getInstalledModuleItems().stream().anyMatch(module -> module.getDefinition().category() == CyberwareModuleCategory.LEGS);
    }

    public static void clearCooldowns(Player player) {
        UUID playerId = player.getUUID();
        CHARGED_JUMP_TICKS.remove(playerId);
        FORTIFIED_CHARGE_WINDOW_TICKS.remove(playerId);
        SPRINT_TICKS.remove(playerId);
        AIR_TICKS.remove(playerId);
        MAX_AIR_FALL_DISTANCE.remove(playerId);
        LAST_ON_GROUND.remove(playerId);
        LAST_SPRINTING.remove(playerId);
        DASH_READY_UNTIL_TICK.remove(playerId);
        LEEROY_IMPACT_READY_UNTIL_TICK.remove(playerId);
        EXTRA_JUMP_USED.remove(playerId);
        clearManagedLynxSilence(player);
    }

    private static void updateSprintTicks(UUID playerId, boolean sprinting) {
        if (sprinting) {
            SPRINT_TICKS.put(playerId, SPRINT_TICKS.getOrDefault(playerId, 0) + 1);
        } else {
            SPRINT_TICKS.remove(playerId);
        }
    }

    private static void updateChargeTicks(Player player, UUID playerId, int fortifiedCount) {
        if (fortifiedCount <= 0) {
            CHARGED_JUMP_TICKS.remove(playerId);
            FORTIFIED_CHARGE_WINDOW_TICKS.remove(playerId);
            return;
        }

        boolean charging = player.onGround()
                && player.isCrouching()
                && !player.isSprinting()
                && player.getDeltaMovement().horizontalDistanceSqr() < 0.01D;
        if (charging) {
            CHARGED_JUMP_TICKS.put(playerId, Math.min(FORTIFIED_MAX_CHARGE_TICKS, CHARGED_JUMP_TICKS.getOrDefault(playerId, 0) + 1));
            FORTIFIED_CHARGE_WINDOW_TICKS.put(playerId, FORTIFIED_RELEASE_WINDOW_TICKS);
        } else if (player.onGround()) {
            int chargeTicks = CHARGED_JUMP_TICKS.getOrDefault(playerId, 0);
            int releaseWindow = FORTIFIED_CHARGE_WINDOW_TICKS.getOrDefault(playerId, 0);
            boolean shouldHoldCharge = chargeTicks > 0
                    && !player.isSprinting()
                    && player.getDeltaMovement().horizontalDistanceSqr() <= 0.08D;
            if (shouldHoldCharge && releaseWindow > 0) {
                FORTIFIED_CHARGE_WINDOW_TICKS.put(playerId, releaseWindow - 1);
            } else {
                CHARGED_JUMP_TICKS.remove(playerId);
                FORTIFIED_CHARGE_WINDOW_TICKS.remove(playerId);
            }
        }
    }

    private static void updateAirborneState(Player player, UUID playerId, boolean onGround) {
        if (onGround) {
            AIR_TICKS.remove(playerId);
            MAX_AIR_FALL_DISTANCE.remove(playerId);
            return;
        }

        AIR_TICKS.put(playerId, AIR_TICKS.getOrDefault(playerId, 0) + 1);
        MAX_AIR_FALL_DISTANCE.put(playerId, Math.max(MAX_AIR_FALL_DISTANCE.getOrDefault(playerId, 0.0F), player.fallDistance));
    }

    private static void handleTakeoff(Player player, int fortifiedCount, int jenkinsCount, int leeroyCount, boolean sprinting) {
        boolean refreshed = false;
        Vec3 motion = player.getDeltaMovement();
        Vec3 forward = horizontalLook(player);

        if (fortifiedCount > 0) {
            int chargeTicks = CHARGED_JUMP_TICKS.getOrDefault(player.getUUID(), 0);
            if (chargeTicks > 3) {
                double ratio = Math.min(1.0D, chargeTicks / (double) FORTIFIED_MAX_CHARGE_TICKS);
                double verticalBoost = CyberwareBalance.doubleValue("legs.fortified_ankles.vertical_base")
                        + CyberwareBalance.doubleValue("legs.fortified_ankles.vertical_scale") * ratio
                        + CyberwareBalance.doubleValue("legs.fortified_ankles.vertical_extra_per_install") * Math.max(0, fortifiedCount - 1);
                double forwardBoost = CyberwareBalance.doubleValue("legs.fortified_ankles.forward_base")
                        + CyberwareBalance.doubleValue("legs.fortified_ankles.forward_scale") * ratio;
                motion = new Vec3(motion.x + forward.x * forwardBoost, motion.y + verticalBoost, motion.z + forward.z * forwardBoost);
                player.setDeltaMovement(motion);
                player.hasImpulse = true;
                player.hurtMarked = true;
                PlayerMotionSyncHelper.sync(player);
                TemporaryCyberwareEffectManager.addEffects(player, 28L,
                        CyberwareEffect.safeFall(3.0D * fortifiedCount),
                        CyberwareEffect.knockbackResistance(0.08D * fortifiedCount));
                CHARGED_JUMP_TICKS.remove(player.getUUID());
                FORTIFIED_CHARGE_WINDOW_TICKS.remove(player.getUUID());
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.35F, 1.45F);
                refreshed = true;
            }
        }

        if (jenkinsCount > 0 && hasJenkinsStride(player, sprinting)) {
              double forwardBoost = CyberwareBalance.doubleValue("legs.jenkins_tendons.takeoff_forward") + 0.05D * jenkinsCount;
              double verticalBoost = CyberwareBalance.doubleValue("legs.jenkins_tendons.takeoff_vertical") + 0.02D * jenkinsCount;
            motion = player.getDeltaMovement();
            player.setDeltaMovement(
                    motion.x + forward.x * forwardBoost,
                    motion.y + verticalBoost,
                    motion.z + forward.z * forwardBoost
            );
            player.hasImpulse = true;
            player.hurtMarked = true;
            PlayerMotionSyncHelper.sync(player);
            TemporaryCyberwareEffectManager.addEffects(player, 18L,
                    CyberwareEffect.moveSpeed(0.05D * jenkinsCount),
                    CyberwareEffect.jumpPower(0.03D * jenkinsCount),
                    CyberwareEffect.safeFall(1.5D * jenkinsCount));
            refreshed = true;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 0.25F, 1.45F);
        }

        if (hasLeeroyCharge(player, leeroyCount) && sprinting) {
              double forwardBoost = CyberwareBalance.doubleValue("legs.leeroy_ligament_system.takeoff_forward") + 0.12D * leeroyCount;
              double verticalBoost = CyberwareBalance.doubleValue("legs.leeroy_ligament_system.takeoff_vertical") + 0.04D * leeroyCount;
            motion = player.getDeltaMovement();
            player.setDeltaMovement(
                    motion.x + forward.x * forwardBoost,
                    motion.y + verticalBoost,
                    motion.z + forward.z * forwardBoost
            );
            player.hasImpulse = true;
            player.hurtMarked = true;
            PlayerMotionSyncHelper.sync(player);
            TemporaryCyberwareEffectManager.addEffects(player, 26L,
                    CyberwareEffect.moveSpeed(0.12D * leeroyCount),
                    CyberwareEffect.knockbackResistance(0.10D * leeroyCount),
                    CyberwareEffect.safeFall(2.0D * leeroyCount));
            refreshed = true;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.35F, 1.30F);
        }

        if (refreshed) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    private static void handleLanding(Player player, int fortifiedCount, int leeroyCount, int lynxCount, int reinforcedCount, int fallDamperCount) {
        float peakFallDistance = MAX_AIR_FALL_DISTANCE.getOrDefault(player.getUUID(), 0.0F);
        if (peakFallDistance < 3.5F) {
            return;
        }

        boolean refreshed = false;
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int jenkinsCount = inventory.countInstalledCyberware(JENKINS_TENDONS_ID);
        if (fortifiedCount > 0 && peakFallDistance >= 4.0F) {
            TemporaryCyberwareEffectManager.addEffects(player, 50L,
                    CyberwareEffect.moveSpeed(0.10D * fortifiedCount),
                    CyberwareEffect.jumpPower(0.08D * fortifiedCount),
                    CyberwareEffect.safeFall(2.0D * fortifiedCount),
                    CyberwareEffect.stepHeight(0.15D * fortifiedCount));
            refreshed = true;
        }
        if (jenkinsCount > 0 && peakFallDistance >= 3.5F) {
            TemporaryCyberwareEffectManager.addEffects(player, 36L,
                    CyberwareEffect.moveSpeed(0.07D * jenkinsCount),
                    CyberwareEffect.stepHeight(0.12D * jenkinsCount),
                    CyberwareEffect.safeFall(1.0D * jenkinsCount));
            refreshed = true;
        }
        if (leeroyCount > 0 && peakFallDistance >= 4.0F) {
            TemporaryCyberwareEffectManager.addEffects(player, 60L,
                    CyberwareEffect.moveSpeed(0.14D * leeroyCount),
                    CyberwareEffect.stepHeight(0.25D * leeroyCount),
                    CyberwareEffect.jumpPower(0.08D * leeroyCount),
                    CyberwareEffect.knockbackResistance(0.10D * leeroyCount),
                    CyberwareEffect.safeFall(2.0D * leeroyCount));
            refreshed = true;
        }
        if (lynxCount > 0 && peakFallDistance >= 3.5F) {
            TemporaryCyberwareEffectManager.addEffects(player, 35L,
                    CyberwareEffect.moveSpeed(0.06D * lynxCount));
            refreshed = true;
        }
        if (reinforcedCount > 0 && peakFallDistance >= 5.0F) {
            TemporaryCyberwareEffectManager.addEffects(player, 45L,
                    CyberwareEffect.moveSpeed(0.08D * reinforcedCount),
                    CyberwareEffect.safeFall(2.0D * reinforcedCount));
            refreshed = true;
        }
        if (fallDamperCount > 0 && peakFallDistance >= 4.0F) {
            TemporaryCyberwareEffectManager.addEffects(player, 55L,
                    CyberwareEffect.moveSpeed(0.10D * fallDamperCount),
                    CyberwareEffect.knockbackResistance(0.08D * fallDamperCount));
            refreshed = true;
        }

        if (refreshed) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    private static void maybeTriggerSprintBurst(Player player, int dashPistonCount, int leeroyCount) {
        UUID playerId = player.getUUID();
        long gameTime = player.level().getGameTime();
        if (DASH_READY_UNTIL_TICK.getOrDefault(playerId, 0L) > gameTime || !hasHorizontalMovement(player, MIN_CHARGE_MOVEMENT_SQR)) {
            return;
        }

        Vec3 forward = horizontalLook(player);
        Vec3 motion = player.getDeltaMovement();
        double burst = CyberwareBalance.doubleValue("legs.dash.base_burst")
                + CyberwareBalance.doubleValue("legs.dash.leeroy_bonus") * leeroyCount
                + CyberwareBalance.doubleValue("legs.dash.module_bonus") * dashPistonCount;
        player.setDeltaMovement(motion.x + forward.x * burst, motion.y, motion.z + forward.z * burst);
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);

        double speedBonus = CyberwareBalance.doubleValue("legs.dash.leeroy_speed_bonus") * Math.max(0, leeroyCount)
                + CyberwareBalance.doubleValue("legs.dash.module_speed_bonus") * Math.max(0, dashPistonCount);
        if (speedBonus > 0.0001D) {
            TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.moveSpeed(speedBonus), 30L);
            CyberwareEffects.refreshPlayerCyberware(player);
        }

        long cooldownTicks = dashPistonCount > 0
                ? CyberwareBalance.intValue("legs.dash.cooldown_with_module_ticks")
                : CyberwareBalance.intValue("legs.dash.cooldown_without_module_ticks");
        DASH_READY_UNTIL_TICK.put(playerId, gameTime + cooldownTicks);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.55F, 1.15F);
    }

    private static void tryLeeroyShoulderCheck(Player player, int leeroyCount, boolean sprinting) {
        if (!hasLeeroyCharge(player, leeroyCount) || !sprinting) {
            return;
        }
        if (player.getDeltaMovement().horizontalDistanceSqr() < CyberwareBalance.doubleValue("legs.leeroy.impact_required_speed_sqr")) {
            return;
        }

        long gameTime = player.level().getGameTime();
        if (LEEROY_IMPACT_READY_UNTIL_TICK.getOrDefault(player.getUUID(), 0L) > gameTime) {
            return;
        }

        Vec3 forward = horizontalLook(player);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                  player.getBoundingBox()
                          .inflate(CyberwareBalance.doubleValue("legs.leeroy.impact_hitbox"), 0.75D, CyberwareBalance.doubleValue("legs.leeroy.impact_hitbox"))
                          .expandTowards(forward.scale(CyberwareBalance.doubleValue("legs.leeroy.impact_forward_expand"))),
                target -> target != player && target.isAlive() && isLeeroyImpactTarget(player, target)
        );

        int impacts = 0;
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(player.position());
            Vec3 flatOffset = new Vec3(toTarget.x, 0.0D, toTarget.z);
            if (flatOffset.lengthSqr() < 0.01D || flatOffset.normalize().dot(forward) < -0.10D) {
                continue;
            }

              target.knockback(
                      CyberwareBalance.doubleValue("legs.leeroy.impact_knockback")
                              + CyberwareBalance.doubleValue("legs.leeroy.impact_knockback_per_install") * leeroyCount,
                      flatOffset.x, flatOffset.z);
              CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 60L + leeroyCount * 10L, 1, true);
              target.hurt(player.damageSources().playerAttack(player),
                      (float) (CyberwareBalance.doubleValue("legs.leeroy.impact_damage") + leeroyCount));
            impacts++;
            if (impacts >= Math.min(2, leeroyCount)) {
                break;
            }
        }

        if (impacts <= 0) {
            return;
        }

          LEEROY_IMPACT_READY_UNTIL_TICK.put(player.getUUID(), gameTime + CyberwareBalance.intValue("legs.leeroy.impact_cooldown_ticks"));
        TemporaryCyberwareEffectManager.addEffects(player, 24L,
                CyberwareEffect.moveSpeed(0.06D * leeroyCount),
                CyberwareEffect.knockbackResistance(0.08D * leeroyCount));
        CyberwareEffects.refreshPlayerCyberware(player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.45F, 1.10F);
    }

    private static void applyClimbingServo(Player player, int climbingServoCount) {
        if (!shouldClimb(player)) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
          double climbSpeed = player.isCrouching()
                  ? CyberwareBalance.doubleValue("legs.climbing_servo.crouch_speed")
                  : CyberwareBalance.doubleValue("legs.climbing_servo.base_speed")
                          + CyberwareBalance.doubleValue("legs.climbing_servo.extra_speed_per_install") * Math.max(0, climbingServoCount - 1);
        player.setDeltaMovement(motion.x * 0.82D, Math.max(motion.y, climbSpeed), motion.z * 0.82D);
        player.fallDistance = 0.0F;
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);
    }

    private static void applyBrushstepAssistance(Player player, int brushstepCount) {
        BrushstepSurface surface = resolveBrushstepSurface(player);
        if (surface == BrushstepSurface.NONE) {
            return;
        }

        double multiplier = switch (surface) {
            case SWEET_BERRY_BUSH -> 4.50D;
            case SOUL_SAND -> 2.40D;
            case MUD -> 2.10D;
            case LEAVES -> 1.45D;
            case FARMLAND -> 1.20D;
            case NONE -> 1.0D;
        };
        multiplier += 0.03D * Math.max(0, brushstepCount - 1);
        double minHorizontalSpeed = switch (surface) {
            case SWEET_BERRY_BUSH -> 0.115D;
            case SOUL_SAND -> 0.095D;
            case MUD -> 0.088D;
            case LEAVES -> 0.060D;
            case FARMLAND -> 0.050D;
            case NONE -> 0.0D;
        };

        Vec3 motion = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        Vec3 direction;
        if (horizontal.lengthSqr() > 0.0001D) {
            direction = horizontal.normalize();
        } else {
            direction = movementIntent(player);
        }

        double horizontalSpeed = horizontal.length();
        double targetSpeed = horizontalSpeed > 0.0001D ? Math.max(minHorizontalSpeed, horizontalSpeed * multiplier) : minHorizontalSpeed;
        if (direction.lengthSqr() <= 0.0001D || targetSpeed <= 0.0D) {
            return;
        }

        Vec3 adjustedHorizontal = direction.scale(targetSpeed);
        player.setDeltaMovement(adjustedHorizontal.x, motion.y, adjustedHorizontal.z);
        player.fallDistance = 0.0F;
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);
    }

    private static boolean hasLeeroyCharge(Player player, int leeroyCount) {
        if (leeroyCount <= 0) {
            return false;
        }
        return isEffectiveSprinting(player)
                || SPRINT_TICKS.getOrDefault(player.getUUID(), 0) >= 4 && hasHorizontalMovement(player, MIN_CHARGE_MOVEMENT_SQR);
    }

    private static boolean hasJenkinsStride(Player player, boolean sprinting) {
        return sprinting
                && SPRINT_TICKS.getOrDefault(player.getUUID(), 0) >= 3
                && hasHorizontalMovement(player, MIN_CHARGE_MOVEMENT_SQR);
    }

    private static boolean shouldClimb(Player player) {
        return !player.onGround()
                && !player.onClimbable()
                && !player.isInWaterOrBubble()
                && !player.getAbilities().flying
                && player.horizontalCollision
                && hasHorizontalMovement(player, MIN_MOVEMENT_SQR);
    }

    private static boolean isLeeroyImpactTarget(Player player, LivingEntity target) {
        return target instanceof Enemy
                || target instanceof Mob mob && AggroControlHelper.hasDirectAggro(mob, player);
    }

    private static void suppressStealthAggro(Player player, int lynxCount, int stealthFootCount) {
        if (!isStealthStride(player)) {
            return;
        }

          double radius = CyberwareBalance.doubleValue("legs.lynx_paws.aggro_radius_base")
                  + CyberwareBalance.doubleValue("legs.lynx_paws.aggro_radius_per_install") * lynxCount
                  + CyberwareBalance.doubleValue("legs.stealth_foot.aggro_radius_per_install") * stealthFootCount;
          double closeThreatRange = player.isCrouching()
                  ? CyberwareBalance.doubleValue("legs.lynx_paws.close_threat_crouch")
                  : CyberwareBalance.doubleValue("legs.lynx_paws.close_threat_standing");
        AggroControlHelper.clearNearbyAggro(player, radius, candidate -> shouldLoseTrack(candidate, player, closeThreatRange));
    }

    private static void manageLynxSilence(Player player, int lynxCount, int stealthFootCount) {
        if (lynxCount <= 0) {
            clearManagedLynxSilence(player);
            return;
        }

        boolean shouldSilence = shouldApplyLynxSilence(player, stealthFootCount);
        UUID playerId = player.getUUID();
        if (shouldSilence) {
            if (!player.isSilent()) {
                player.setSilent(true);
                MANAGED_LYNX_SILENCE.put(playerId, true);
            }
        } else {
            clearManagedLynxSilence(player);
        }
    }

    private static void clearManagedLynxSilence(Player player) {
        UUID playerId = player.getUUID();
        if (MANAGED_LYNX_SILENCE.remove(playerId) != null && player.isSilent()) {
            player.setSilent(false);
        }
    }

    private static boolean shouldApplyLynxSilence(Player player, int stealthFootCount) {
        if (player.isSprinting() || player.isUsingItem()) {
            return false;
        }

          double maxSilentSpeed = stealthFootCount > 0
                  ? CyberwareBalance.doubleValue("legs.stealth_foot.max_silent_speed")
                  : CyberwareBalance.doubleValue("legs.lynx_paws.max_silent_speed");
          double horizontalSpeedSqr = player.getDeltaMovement().horizontalDistanceSqr();
          boolean softLanding = !player.onGround() && player.fallDistance <= (float) CyberwareBalance.doubleValue("legs.lynx_paws.soft_landing_fall_distance");
        return player.isCrouching()
                || horizontalSpeedSqr <= maxSilentSpeed * maxSilentSpeed
                || softLanding;
    }

    private static boolean shouldLoseTrack(Mob mob, Player player, double closeThreatRange) {
        double distance = player.distanceToSqr(mob);
        return AggroControlHelper.hasDirectAggro(mob, player)
                || mob instanceof Enemy && mob.hasLineOfSight(player) && distance >= closeThreatRange * closeThreatRange;
    }

    private static boolean isStealthStride(Player player) {
        return !player.isSprinting()
                && player.getDeltaMovement().horizontalDistanceSqr() <= 0.10D
                && (player.isCrouching() || !hasHorizontalMovement(player, 0.04D));
    }

    private static boolean isBrushstepTerrain(Player player) {
        return resolveBrushstepSurface(player) != BrushstepSurface.NONE;
    }

    private static BrushstepSurface resolveBrushstepSurface(Player player) {
        BlockState feetState = player.level().getBlockState(player.blockPosition());
        BlockState belowState = player.level().getBlockState(player.blockPosition().below());
        if (feetState.is(Blocks.SWEET_BERRY_BUSH)) {
            return BrushstepSurface.SWEET_BERRY_BUSH;
        }
        if (feetState.is(Blocks.MUD) || belowState.is(Blocks.MUD)) {
            return BrushstepSurface.MUD;
        }
        if (feetState.is(Blocks.SOUL_SAND) || belowState.is(Blocks.SOUL_SAND)) {
            return BrushstepSurface.SOUL_SAND;
        }
        if (feetState.is(BlockTags.LEAVES) || belowState.is(BlockTags.LEAVES)) {
            return BrushstepSurface.LEAVES;
        }
        if (feetState.is(Blocks.FARMLAND) || belowState.is(Blocks.FARMLAND)) {
            return BrushstepSurface.FARMLAND;
        }
        return BrushstepSurface.NONE;
    }

    private static boolean isEffectiveSprinting(Player player) {
        return player.isSprinting() && hasHorizontalMovement(player, MIN_MOVEMENT_SQR);
    }

    private static boolean hasHorizontalMovement(Player player, double minMovementSqr) {
        return player.getDeltaMovement().horizontalDistanceSqr() > minMovementSqr;
    }

    private static int countInstalledLegModules(PlayerCyberwareInventory inventory, String moduleId) {
        int count = 0;
        for (var moduleItem : inventory.getInstalledModuleItems()) {
            if (moduleItem.getDefinition().category() == CyberwareModuleCategory.LEGS
                    && moduleItem.getDefinition().id().equals(moduleId)) {
                count++;
            }
        }
        return count;
    }

    private static Vec3 horizontalLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        return flat.lengthSqr() > 0.0001D ? flat.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static Vec3 movementIntent(Player player) {
        Vec3 forward = horizontalLook(player);
        Vec3 strafe = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 intent = forward.scale(player.zza).add(strafe.scale(player.xxa));
        return intent.lengthSqr() > 0.0001D ? intent.normalize() : Vec3.ZERO;
    }

    private static void merge(java.util.EnumMap<CyberwareEffectType, Double> totals, CyberwareEffectType type, double amount) {
        totals.merge(type, amount, type.isMobEffect() ? Math::max : Double::sum);
    }

    private enum BrushstepSurface {
        NONE,
        SWEET_BERRY_BUSH,
        SOUL_SAND,
        MUD,
        LEAVES,
        FARMLAND
    }
}
