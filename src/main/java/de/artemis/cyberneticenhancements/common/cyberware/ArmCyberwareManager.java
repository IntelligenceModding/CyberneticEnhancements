package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareModuleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ArmCyberwareManager {
    private static final String GORILLA_ARMS_ID = "gorilla_arms";
    private static final String ELECTRIFYING_GORILLA_ARMS_ID = "electrifying_gorilla_arms";
    private static final String THERMAL_GORILLA_ARMS_ID = "thermal_gorilla_arms";
    private static final String CHEMICAL_GORILLA_ARMS_ID = "chemical_gorilla_arms";
    private static final String MANTIS_BLADES_ID = "mantis_blades";
    private static final String ELECTRIFYING_MANTIS_BLADES_ID = "electrifying_mantis_blades";
    private static final String THERMAL_MANTIS_BLADES_ID = "thermal_mantis_blades";
    private static final String TOXIC_MANTIS_BLADES_ID = "toxic_mantis_blades";
    private static final String MAXTAC_MANTIS_BLADES_ID = "maxtac_mantis_blades";
    private static final String MONOWIRE_ID = "monowire";
    private static final String ELECTRIFYING_MONOWIRE_ID = "electrifying_monowire";
    private static final String THERMAL_MONOWIRE_ID = "thermal_monowire";
    private static final String TOXIC_MONOWIRE_ID = "toxic_monowire";
    private static final String PROJECTILE_LAUNCH_SYSTEM_ID = "projectile_launch_system";
    private static final String ELECTRIFYING_PROJECTILE_LAUNCH_SYSTEM_ID = "electrifying_projectile_launch_system";
    private static final String THERMAL_PROJECTILE_LAUNCH_SYSTEM_ID = "thermal_projectile_launch_system";
    private static final String TOXIC_PROJECTILE_LAUNCH_SYSTEM_ID = "toxic_projectile_launch_system";
    private static final String EXCAVATOR_ARMS_ID = "excavator_arms";
    private static final String SHOCK_PALM_MODULE_ID = "shock_palm";

    private static final Map<UUID, Long> ARM_COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Integer> ARM_COOLDOWN_TOTAL_SECONDS = new HashMap<>();
    private static final Map<UUID, Long> ACTIVE_ARM_UNTIL_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_ARM_TOTAL_SECONDS = new HashMap<>();
    private static final Map<UUID, MantisLungeState> MANTIS_LUNGE_STATES = new HashMap<>();
    private static final Map<UUID, GorillaRushState> GORILLA_RUSH_STATES = new HashMap<>();
    private static final Map<UUID, MonowireSweepState> MONOWIRE_SWEEP_STATES = new HashMap<>();
    private static final Map<UUID, ProjectileLaunchState> PROJECTILE_LAUNCH_STATES = new HashMap<>();
    private static final Set<UUID> EXCAVATOR_CHAIN_BREAKERS = new HashSet<>();

    private ArmCyberwareManager() {
    }

    public static void activate(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        CyberwareItem armware = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.ARMS);
        if (armware == null) {
            notify(player, "message.cyberneticenhancements.arms.no_cyberware");
            return;
        }

        String id = armware.getDefinition().id();
        if (!hasSpecialBehavior(id)) {
            notify(player, "message.cyberneticenhancements.arms.no_active_ability");
            return;
        }

        int cooldownSeconds = getCooldownSecondsRemaining(player);
        if (cooldownSeconds > 0) {
            notify(player, "message.cyberneticenhancements.arms.cooldown", Component.translatable(itemTranslationKey(id)), cooldownSeconds);
            return;
        }

        switch (id) {
            case GORILLA_ARMS_ID -> activateGorillaArms(player, inventory, id, 18, 6.0F, 4.0D);
            case ELECTRIFYING_GORILLA_ARMS_ID -> activateGorillaArms(player, inventory, id, 20, 7.0F, 4.0D);
            case THERMAL_GORILLA_ARMS_ID -> activateGorillaArms(player, inventory, id, 20, 7.0F, 4.0D);
            case CHEMICAL_GORILLA_ARMS_ID -> activateGorillaArms(player, inventory, id, 20, 6.5F, 4.0D);
            case MANTIS_BLADES_ID -> activateMantisBlades(player, inventory, id, 14, 7.0F, 12.0D);
            case ELECTRIFYING_MANTIS_BLADES_ID -> activateMantisBlades(player, inventory, id, 15, 7.5F, 12.0D);
            case THERMAL_MANTIS_BLADES_ID -> activateMantisBlades(player, inventory, id, 15, 7.5F, 12.0D);
            case TOXIC_MANTIS_BLADES_ID -> activateMantisBlades(player, inventory, id, 15, 7.5F, 12.0D);
            case MAXTAC_MANTIS_BLADES_ID -> activateMantisBlades(player, inventory, id, 12, 10.0F, 14.0D);
            case MONOWIRE_ID -> activateMonowire(player, inventory, id, 10, 5.0F, 8.0D, 4);
            case ELECTRIFYING_MONOWIRE_ID -> activateMonowire(player, inventory, id, 11, 5.5F, 8.0D, 4);
            case THERMAL_MONOWIRE_ID -> activateMonowire(player, inventory, id, 11, 5.5F, 8.0D, 4);
            case TOXIC_MONOWIRE_ID -> activateMonowire(player, inventory, id, 11, 5.5F, 8.0D, 4);
            case PROJECTILE_LAUNCH_SYSTEM_ID -> activateProjectileSystem(player, inventory, id, 18, 8.0F, 3.5D, 22.0D);
            case ELECTRIFYING_PROJECTILE_LAUNCH_SYSTEM_ID -> activateProjectileSystem(player, inventory, id, 19, 8.5F, 3.5D, 22.0D);
            case THERMAL_PROJECTILE_LAUNCH_SYSTEM_ID -> activateProjectileSystem(player, inventory, id, 19, 8.5F, 3.5D, 22.0D);
            case TOXIC_PROJECTILE_LAUNCH_SYSTEM_ID -> activateProjectileSystem(player, inventory, id, 19, 8.5F, 3.5D, 22.0D);
            case EXCAVATOR_ARMS_ID -> activateExcavatorArms(player, inventory, id);
            default -> notify(player, "message.cyberneticenhancements.arms.no_active_ability");
        }
    }

    public static boolean hasSpecialBehavior(String id) {
        return switch (id) {
            case GORILLA_ARMS_ID,
                 ELECTRIFYING_GORILLA_ARMS_ID,
                 THERMAL_GORILLA_ARMS_ID,
                 CHEMICAL_GORILLA_ARMS_ID,
                 MANTIS_BLADES_ID,
                 ELECTRIFYING_MANTIS_BLADES_ID,
                 THERMAL_MANTIS_BLADES_ID,
                 TOXIC_MANTIS_BLADES_ID,
                 MAXTAC_MANTIS_BLADES_ID,
                 MONOWIRE_ID,
                 ELECTRIFYING_MONOWIRE_ID,
                 THERMAL_MONOWIRE_ID,
                 TOXIC_MONOWIRE_ID,
                 PROJECTILE_LAUNCH_SYSTEM_ID,
                 ELECTRIFYING_PROJECTILE_LAUNCH_SYSTEM_ID,
                 THERMAL_PROJECTILE_LAUNCH_SYSTEM_ID,
                 TOXIC_PROJECTILE_LAUNCH_SYSTEM_ID,
                 EXCAVATOR_ARMS_ID -> true;
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

    public static int getCooldownSecondsRemaining(Player player) {
        long remainingTicks = Math.max(0L, ARM_COOLDOWN_UNTIL_TICK.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
        return (int) ((remainingTicks + 19L) / 20L);
    }

    public static int getCooldownTotalSeconds(Player player) {
        return ARM_COOLDOWN_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static int getActiveSecondsRemaining(Player player) {
        long remainingTicks = Math.max(0L, ACTIVE_ARM_UNTIL_TICKS.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
        return (int) ((remainingTicks + 19L) / 20L);
    }

    public static int getActiveTotalSeconds(Player player) {
        return ACTIVE_ARM_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static void onPlayerTick(Player player) {
        tickExcavatorMode(player);
        tickProjectileLaunch(player);
        tickMonowireSweep(player);
        tickGorillaRush(player);
        tickMantisLunge(player);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player player) || event.getEntity() == player) {
            return;
        }

        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity != player) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int shockPalmCount = countInstalledArmModules(inventory, SHOCK_PALM_MODULE_ID);
        if (shockPalmCount <= 0) {
            return;
        }

        event.setAmount(event.getAmount() + 0.75F * shockPalmCount);
        CombatStatusManager.applyStatus(event.getEntity(), CombatStatusType.SHOCK, 50L + shockPalmCount * 20L, 1 + Math.max(0, shockPalmCount - 1), true);
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (EXCAVATOR_CHAIN_BREAKERS.contains(player.getUUID()) || !isExcavatorModeActive(player)) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        CyberwareItem armware = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.ARMS);
        if (armware == null || !EXCAVATOR_ARMS_ID.equals(armware.getDefinition().id())) {
            clearExcavatorMode(player);
            return;
        }

        BlockState centerState = event.getState();
        BlockPos centerPos = event.getPos();
        if (!canExcavate(player, centerState) || isOreBlock(centerState)) {
            return;
        }

        EXCAVATOR_CHAIN_BREAKERS.add(player.getUUID());
        try {
            int destroyed = 0;
            for (BlockPos targetPos : excavatorPattern(centerPos, resolveExcavatorFace(player))) {
                if (targetPos.equals(centerPos)) {
                    continue;
                }

                BlockState targetState = player.level().getBlockState(targetPos);
                if (!canExcavateInPattern(player, targetPos, centerPos, targetState)) {
                    continue;
                }
                if (player.gameMode.destroyBlock(targetPos)) {
                    destroyed++;
                }
            }

            if (destroyed > 0) {
                spawnExcavatorFeedback(player, centerPos);
            }
        } finally {
            EXCAVATOR_CHAIN_BREAKERS.remove(player.getUUID());
        }
    }

    public static String getInstalledArmsTranslationKey(PlayerCyberwareInventory inventory) {
        CyberwareItem armware = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.ARMS);
        return armware == null ? "" : itemTranslationKey(armware.getDefinition().id());
    }

    public static void reduceTrackedCooldowns(Player player, long flatTicks, double percentRefund) {
        long gameTime = player.level().getGameTime();
        long until = ARM_COOLDOWN_UNTIL_TICK.getOrDefault(player.getUUID(), 0L);
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
            clearCooldown(player);
            return;
        }

        ARM_COOLDOWN_UNTIL_TICK.put(player.getUUID(), gameTime + remaining);
    }

    public static void clearCooldowns(Player player) {
        clearCooldown(player);
        clearExcavatorMode(player);
        clearMantisLunge(player);
        clearGorillaRush(player);
        clearMonowireSweep(player);
        clearProjectileLaunch(player);
    }

    public static void clearCooldown(Player player) {
        ARM_COOLDOWN_UNTIL_TICK.remove(player.getUUID());
        ARM_COOLDOWN_TOTAL_SECONDS.remove(player.getUUID());
    }

    private static void activateGorillaArms(Player player, PlayerCyberwareInventory inventory, String id, int cooldownSeconds, float damage, double radius) {
        LivingEntity target = findViewTarget(player, 5.0D, 0.20D);
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.22D, 1.0D, 0.22D));
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);
        GORILLA_RUSH_STATES.put(player.getUUID(), new GorillaRushState(id, damage, radius, player.level().getGameTime() + 5L, target == null ? null : target.getUUID()));
        TemporaryCyberwareEffectManager.addEffects(player, 80L,
                CyberwareEffect.knockbackResistance(0.20D),
                CyberwareEffect.damageReduction(0.05D));
        spawnGorillaRushStartFeedback(player, id);
        startCooldown(player, inventory, cooldownSeconds);
        notify(player, "message.cyberneticenhancements.arms.activated", Component.translatable(itemTranslationKey(id)));
        tickGorillaRush(player);
    }

    private static void activateMantisBlades(Player player, PlayerCyberwareInventory inventory, String id, int cooldownSeconds, float damage, double range) {
        LivingEntity target = findViewTarget(player, range, 0.45D);
        Vec3 forward = resolveMantisDirection(player, target);
        double speed = target == null ? 1.15D : 1.35D;
        player.setDeltaMovement(forward.scale(speed).add(0.0D, 0.32D, 0.0D));
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);
        MANTIS_LUNGE_STATES.put(player.getUUID(), new MantisLungeState(id, damage, range, player.level().getGameTime() + 8L, target == null ? null : target.getUUID()));
        spawnMantisLungeStartFeedback(player, id);

        TemporaryCyberwareEffectManager.addEffects(player, 80L,
                CyberwareEffect.moveSpeed(0.12D),
                CyberwareEffect.attackSpeed(0.12D));
        startCooldown(player, inventory, cooldownSeconds);
        notify(player, "message.cyberneticenhancements.arms.activated", Component.translatable(itemTranslationKey(id)));
        tickMantisLunge(player);
    }

    private static void activateMonowire(Player player, PlayerCyberwareInventory inventory, String id, int cooldownSeconds, float damage, double range, int maxTargets) {
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.45D, 1.0D, 0.45D));
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);
        MONOWIRE_SWEEP_STATES.put(player.getUUID(), new MonowireSweepState(id, damage, range, maxTargets, player.level().getGameTime() + 5L, flattenLook(player), new HashSet<>()));
        spawnMonowireSweepStartFeedback(player, id);
        TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.entityReach(0.75D), 80L);
        startCooldown(player, inventory, cooldownSeconds);
        notify(player, "message.cyberneticenhancements.arms.activated", Component.translatable(itemTranslationKey(id)));
        tickMonowireSweep(player);
    }

    private static void activateProjectileSystem(Player player, PlayerCyberwareInventory inventory, String id, int cooldownSeconds, float damage, double radius, double range) {
        Vec3 launchOrigin = player.getEyePosition().add(flattenLook(player).scale(0.55D)).add(0.0D, -0.18D, 0.0D);
        Vec3 impactPoint = resolveImpactPoint(player, range);
        long travelTicks = Math.max(4L, Math.min(9L, Math.round(launchOrigin.distanceTo(impactPoint) / 3.2D)));
        PROJECTILE_LAUNCH_STATES.put(player.getUUID(), new ProjectileLaunchState(
                id,
                damage,
                radius,
                player.level().getGameTime() + travelTicks,
                launchOrigin,
                impactPoint,
                new HashSet<>()
        ));
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.65D, 1.0D, 0.65D));
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);
        spawnProjectileLaunchStartFeedback(player, launchOrigin, id);
        startCooldown(player, inventory, cooldownSeconds);
        notify(player, "message.cyberneticenhancements.arms.activated", Component.translatable(itemTranslationKey(id)));
        tickProjectileLaunch(player);
    }

    private static void activateExcavatorArms(Player player, PlayerCyberwareInventory inventory, String id) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        if (isExcavatorModeActive(player)) {
            notify(player, "message.cyberneticenhancements.arms.active", Component.translatable(itemTranslationKey(id)));
            return;
        }

        int activeSeconds = CyberwareBalance.intValue("arms.excavator.active_seconds");
        long activeTicks = activeSeconds * 20L;
        ACTIVE_ARM_UNTIL_TICKS.put(player.getUUID(), player.level().getGameTime() + activeTicks);
        ACTIVE_ARM_TOTAL_SECONDS.put(player.getUUID(), activeSeconds);
        TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.breakSpeed(CyberwareUpgradeHelper.performance(inventory, "excavator_arms", "arms.excavator.break_speed_bonus")), activeTicks);
        startCooldown(player, inventory, CyberwareBalance.intValue("arms.excavator.cooldown_seconds"));
        spawnExcavatorActivationFeedback(player);
        notify(player, "message.cyberneticenhancements.arms.excavator_mode", Component.translatable(itemTranslationKey(id)), activeSeconds);
    }

    private static void startCooldown(Player player, PlayerCyberwareInventory inventory, int cooldownSeconds) {
        long adjustedCooldown = FrontalCortexManager.adjustCyberwareCooldownTicks(player, inventory, cooldownSeconds * 20L, true);
        ARM_COOLDOWN_UNTIL_TICK.put(player.getUUID(), player.level().getGameTime() + adjustedCooldown);
        ARM_COOLDOWN_TOTAL_SECONDS.put(player.getUUID(), (int) Math.max(1L, (adjustedCooldown + 19L) / 20L));
        CyberstrainManager.onCyberwareActivated(player, Math.max(7.0D, cooldownSeconds * 0.45D));
        CyberwareEffects.refreshPlayerCyberware(player);
    }

    private static List<BlockPos> excavatorPattern(BlockPos centerPos, Direction face) {
        List<BlockPos> positions = new ArrayList<>(9);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos targetPos = switch (face.getAxis()) {
                    case Y -> centerPos.offset(a, 0, b);
                    case X -> centerPos.offset(0, a, b);
                    case Z -> centerPos.offset(a, b, 0);
                };
                positions.add(targetPos);
            }
        }
        return positions;
    }

    private static boolean canExcavateInPattern(Player player, BlockPos targetPos, BlockPos centerPos, BlockState state) {
        if (!canExcavate(player, state)) {
            return false;
        }
        if (!targetPos.equals(centerPos) && isOreBlock(state)) {
            return false;
        }
        return true;
    }

    private static boolean canExcavate(Player player, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(player.level(), player.blockPosition()) < 0.0F || state.hasBlockEntity()) {
            return false;
        }
        if (player.getMainHandItem().isEmpty() || !player.getMainHandItem().isCorrectToolForDrops(state)) {
            return false;
        }
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    private static boolean isOreBlock(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.endsWith("_ore") || path.equals("ancient_debris");
    }

    private static Direction resolveExcavatorFace(Player player) {
        Vec3 look = player.getLookAngle();
        if (Math.abs(look.y) >= 0.65D) {
            return look.y > 0.0D ? Direction.UP : Direction.DOWN;
        }
        return Math.abs(look.x) >= Math.abs(look.z)
                ? (look.x > 0.0D ? Direction.EAST : Direction.WEST)
                : (look.z > 0.0D ? Direction.SOUTH : Direction.NORTH);
    }

    private static void hitTarget(Player player, LivingEntity target, float damage) {
        boolean hurt = target.hurt(player.damageSources().playerAttack(player), damage);
        if (!hurt) {
            target.hurt(player.damageSources().generic(), Math.max(1.0F, damage * 0.5F));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        }
    }

    private static void applyVariantStatus(String id, LivingEntity target, int fireSeconds) {
        boolean playerApplied = true;
        switch (id) {
            case GORILLA_ARMS_ID -> CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 120L, 2, playerApplied);
            case ELECTRIFYING_GORILLA_ARMS_ID,
                 ELECTRIFYING_MANTIS_BLADES_ID,
                 ELECTRIFYING_MONOWIRE_ID,
                 ELECTRIFYING_PROJECTILE_LAUNCH_SYSTEM_ID -> {
                if (id.endsWith("gorilla_arms") || id.endsWith("projectile_launch_system")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 100L, 1, playerApplied);
                }
                if (id.contains("mantis")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.BLEED, 120L, 1, playerApplied);
                }
                if (id.contains("monowire")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.MARK, 120L, 1, playerApplied);
                }
                CombatStatusManager.applyStatus(target, CombatStatusType.SHOCK, 80L, id.contains("projectile") ? 2 : 1, playerApplied);
            }
            case THERMAL_GORILLA_ARMS_ID,
                 THERMAL_MANTIS_BLADES_ID,
                 THERMAL_MONOWIRE_ID,
                 THERMAL_PROJECTILE_LAUNCH_SYSTEM_ID -> {
                if (id.endsWith("gorilla_arms") || id.endsWith("projectile_launch_system")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 100L, 1, playerApplied);
                }
                if (id.contains("mantis")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.BLEED, 120L, 1, playerApplied);
                }
                if (id.contains("monowire")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.MARK, 120L, 1, playerApplied);
                }
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), fireSeconds * 20));
                CombatStatusManager.applyStatus(target, CombatStatusType.OVERHEAT, 120L, 2, playerApplied);
            }
            case CHEMICAL_GORILLA_ARMS_ID,
                 TOXIC_MANTIS_BLADES_ID,
                 TOXIC_MONOWIRE_ID,
                 TOXIC_PROJECTILE_LAUNCH_SYSTEM_ID -> {
                if (id.endsWith("gorilla_arms") || id.endsWith("projectile_launch_system")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 100L, 1, playerApplied);
                }
                if (id.contains("mantis")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.BLEED, 120L, 1, playerApplied);
                }
                if (id.contains("monowire")) {
                    CombatStatusManager.applyStatus(target, CombatStatusType.MARK, 120L, 1, playerApplied);
                }
                CombatStatusManager.applyStatus(target, CombatStatusType.CORROSION, 140L, 2, playerApplied);
            }
            case MANTIS_BLADES_ID -> CombatStatusManager.applyStatus(target, CombatStatusType.BLEED, 120L, 2, playerApplied);
            case MAXTAC_MANTIS_BLADES_ID -> {
                CombatStatusManager.applyStatus(target, CombatStatusType.BLEED, 140L, 2, playerApplied);
                CombatStatusManager.applyStatus(target, CombatStatusType.MARK, 100L, 1, playerApplied);
                CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 80L, 1, playerApplied);
            }
            case MONOWIRE_ID -> CombatStatusManager.applyStatus(target, CombatStatusType.MARK, 140L, 2, playerApplied);
            case PROJECTILE_LAUNCH_SYSTEM_ID -> {
                CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 100L, 1, playerApplied);
                CombatStatusManager.applyStatus(target, CombatStatusType.MARK, 80L, 1, playerApplied);
            }
            default -> {
            }
        }
    }

    private static List<LivingEntity> findTargetsAround(Player player, Vec3 center, double radius) {
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(center, radius * 2.0D, 4.0D, radius * 2.0D),
                entity -> entity != player && entity.isAlive() && isValidTarget(entity)));
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)));
        return targets;
    }

    private static List<LivingEntity> findTargetsInFront(Player player, double range, double dotThreshold, int maxTargets) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                entity -> entity != player && entity.isAlive() && isValidTarget(entity)));
        targets.removeIf(entity -> {
            Vec3 delta = entity.position().subtract(origin);
            if (delta.lengthSqr() > range * range) {
                return true;
            }
            return delta.normalize().dot(look) < dotThreshold;
        });
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        if (targets.size() > maxTargets) {
            return new ArrayList<>(targets.subList(0, maxTargets));
        }
        return targets;
    }

    private static LivingEntity findViewTarget(Player player, double range, double dotThreshold) {
        List<LivingEntity> targets = findTargetsInFront(player, range, dotThreshold, 1);
        return targets.isEmpty() ? null : targets.get(0);
    }

    private static Vec3 resolveImpactPoint(Player player, double range) {
        HitResult hitResult = player.pick(range, 0.0F, false);
        return hitResult == null ? player.getEyePosition().add(player.getLookAngle().scale(range)) : hitResult.getLocation();
    }

    private static boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof Player targetPlayer) {
            return !targetPlayer.isCreative() && !targetPlayer.isSpectator();
        }
        return true;
    }

    private static int countInstalledArmModules(PlayerCyberwareInventory inventory, String moduleId) {
        int count = 0;
        for (CyberwareModuleItem moduleItem : inventory.getInstalledModuleItems()) {
            if (moduleItem.getDefinition().category() == CyberwareModuleCategory.ARMS
                    && moduleItem.getDefinition().id().equals(moduleId)) {
                count++;
            }
        }
        return count;
    }

    private static void spawnGorillaImpactFeedback(Player player, Vec3 center, double radius, boolean hit, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.2D, center.z, hit ? 24 : 12, radius * 0.35D, 0.15D, radius * 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.35D, center.z, hit ? 16 : 8, radius * 0.25D, 0.1D, radius * 0.25D, 0.08D);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y + 0.25D, center.z, hit ? 10 : 4, radius * 0.18D, 0.1D, radius * 0.18D, 0.04D);

        switch (id) {
            case ELECTRIFYING_GORILLA_ARMS_ID -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.45D, center.z, hit ? 30 : 14, radius * 0.40D, 0.22D, radius * 0.40D, 0.05D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.15D, center.z, hit ? 18 : 8, radius * 0.25D, 0.05D, radius * 0.25D, 0.12D);
            }
            case THERMAL_GORILLA_ARMS_ID -> {
                level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.30D, center.z, hit ? 22 : 10, radius * 0.30D, 0.18D, radius * 0.30D, 0.03D);
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.45D, center.z, hit ? 18 : 8, radius * 0.35D, 0.22D, radius * 0.35D, 0.01D);
            }
            case CHEMICAL_GORILLA_ARMS_ID -> {
                level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.35D, center.z, hit ? 20 : 10, radius * 0.30D, 0.18D, radius * 0.30D, 0.02D);
                level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y + 0.15D, center.z, hit ? 10 : 4, radius * 0.20D, 0.05D, radius * 0.20D, 0.03D);
            }
            default -> {
            }
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, hit ? 0.85F : 0.95F);
    }

    private static void spawnGorillaTargetFeedback(Player player, LivingEntity target, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y + 0.15D, center.z, 4, 0.18D, 0.20D, 0.18D, 0.01D);

        switch (id) {
            case ELECTRIFYING_GORILLA_ARMS_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.30D, center.z, 8, 0.25D, 0.25D, 0.25D, 0.03D);
            case THERMAL_GORILLA_ARMS_ID -> {
                level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.30D, center.z, 6, 0.18D, 0.20D, 0.18D, 0.01D);
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.45D, center.z, 4, 0.20D, 0.15D, 0.20D, 0.01D);
            }
            case CHEMICAL_GORILLA_ARMS_ID -> {
                level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.25D, center.z, 6, 0.18D, 0.18D, 0.18D, 0.01D);
                level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y + 0.10D, center.z, 3, 0.15D, 0.08D, 0.15D, 0.01D);
            }
            default -> level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.25D, center.z, 5, 0.16D, 0.16D, 0.16D, 0.02D);
        }
    }

    private static String itemTranslationKey(String id) {
        return "item.cyberneticenhancements." + id;
    }

    private static void notify(Player player, String translationKey, Object... args) {
        player.displayClientMessage(Component.translatable(translationKey, args).withStyle(ChatFormatting.AQUA), true);
    }

    private static void tickMantisLunge(Player player) {
        MantisLungeState state = MANTIS_LUNGE_STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!player.isAlive()) {
            clearMantisLunge(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime > state.endTick()) {
            clearMantisLunge(player);
            return;
        }

        LivingEntity preferredTarget = state.resolvePreferredTarget(player);
        Vec3 direction = resolveMantisDirection(player, preferredTarget);
        double speed = preferredTarget == null ? 1.12D : 1.32D;
        double verticalBoost = player.onGround() ? 0.26D : Math.max(player.getDeltaMovement().y, 0.08D);
        player.setDeltaMovement(direction.x * speed, verticalBoost, direction.z * speed);
        player.hasImpulse = true;
        player.hurtMarked = true;
        PlayerMotionSyncHelper.sync(player);
        spawnMantisLungeTrail(player, state.cyberwareId(), direction);

        LivingEntity impactTarget = findMantisImpactTarget(player, preferredTarget, direction);
        if (impactTarget == null) {
            return;
        }

        hitTarget(player, impactTarget, state.damage());
        applyVariantStatus(state.cyberwareId(), impactTarget, 4);
        impactTarget.knockback(state.cyberwareId().equals(MAXTAC_MANTIS_BLADES_ID) ? 1.15D : 0.95D, direction.x, direction.z);
        impactTarget.setDeltaMovement(impactTarget.getDeltaMovement().add(0.0D, 0.18D, 0.0D));
        impactTarget.hurtMarked = true;
        PlayerMotionSyncHelper.sync(impactTarget);
        spawnMantisImpactFeedback(player, impactTarget, state.cyberwareId(), direction);
        clearMantisLunge(player);
    }

    private static LivingEntity findMantisImpactTarget(Player player, LivingEntity preferredTarget, Vec3 direction) {
        Vec3 strikeCenter = player.getBoundingBox().getCenter().add(direction.scale(1.2D));
        List<LivingEntity> nearby = findTargetsAround(player, strikeCenter, 1.8D);
        if (preferredTarget != null && nearby.contains(preferredTarget)) {
            return preferredTarget;
        }
        return nearby.isEmpty() ? null : nearby.get(0);
    }

    private static Vec3 resolveMantisDirection(Player player, LivingEntity target) {
        if (target != null && target.isAlive()) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 start = player.getBoundingBox().getCenter();
            Vec3 delta = targetCenter.subtract(start);
            if (delta.lengthSqr() > 0.0001D) {
                return delta.normalize();
            }
        }
        return player.getLookAngle().normalize();
    }

    private static void clearMantisLunge(Player player) {
        MANTIS_LUNGE_STATES.remove(player.getUUID());
    }

    private static void tickGorillaRush(Player player) {
        GorillaRushState state = GORILLA_RUSH_STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!player.isAlive()) {
            clearGorillaRush(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime > state.endTick()) {
            detonateGorillaSlam(player, state, null, false);
            return;
        }

        Vec3 look = resolveGorillaDirection(player, null);
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(current.x * 0.18D, current.y, current.z * 0.18D);
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);
        spawnGorillaRushTrail(player, state.cyberwareId(), look);
    }

    private static void detonateGorillaSlam(Player player, GorillaRushState state, LivingEntity primaryTarget, boolean directImpact) {
        LivingEntity preferredTarget = state.resolvePreferredTarget(player);
        Vec3 direction = resolveGorillaDirection(player, preferredTarget);
        Vec3 center = player.position().add(direction.x * 0.75D, 0.0D, direction.z * 0.75D);
        double radius = state.radius() + 0.35D;
        boolean hit = false;
        List<LivingEntity> targets = findTargetsAround(player, center, radius);
        if (targets.isEmpty()) {
            targets = findTargetsAround(player, player.position(), radius * 0.9D);
        }
        LivingEntity frontTarget = resolvePrimaryGorillaTarget(player, preferredTarget, center, radius, targets);

        for (LivingEntity target : targets) {
            hit = true;
            float damage = state.damage();
            if (target == frontTarget) {
                damage += 2.0F;
            } else if (!targets.isEmpty()) {
                damage += 0.5F;
            }

            hitTarget(player, target, damage);
            Vec3 push = target.position().subtract(center);
            if (push.lengthSqr() < 0.0001D) {
                push = direction;
            }
            double strength = target == frontTarget ? 1.65D : 1.20D;
            target.knockback(strength, push.x, push.z);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, target == frontTarget ? 0.46D : 0.30D, 0.0D));
            target.hurtMarked = true;
            PlayerMotionSyncHelper.sync(target);
            applyVariantStatus(state.cyberwareId(), target, 5);
            if (target == frontTarget) {
                CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 140L, 1, true);
            }
            spawnGorillaTargetFeedback(player, target, state.cyberwareId());
        }

        player.setDeltaMovement(player.getDeltaMovement().multiply(0.12D, 0.10D, 0.12D).add(0.0D, 0.12D, 0.0D));
        player.hasImpulse = true;
        PlayerMotionSyncHelper.sync(player);
        spawnGorillaImpactFeedback(player, center, radius, hit, state.cyberwareId());
        if (hit) {
            player.swing(player.getUsedItemHand() == null ? net.minecraft.world.InteractionHand.MAIN_HAND : player.getUsedItemHand(), true);
        }
        clearGorillaRush(player);
    }

    private static LivingEntity resolvePrimaryGorillaTarget(Player player, LivingEntity preferredTarget, Vec3 center, double radius, List<LivingEntity> targets) {
        if (preferredTarget != null && targets.contains(preferredTarget) && preferredTarget.distanceToSqr(center) <= radius * radius) {
            return preferredTarget;
        }
        Vec3 look = resolveGorillaDirection(player, null);
        Vec3 playerCenter = player.position();
        for (LivingEntity target : targets) {
            Vec3 delta = target.getBoundingBox().getCenter().subtract(playerCenter);
            if (delta.lengthSqr() > 0.0001D && delta.normalize().dot(look) >= 0.30D) {
                return target;
            }
        }
        return targets.isEmpty() ? null : targets.get(0);
    }

    private static Vec3 resolveGorillaDirection(Player player, LivingEntity target) {
        if (target != null && target.isAlive()) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 start = player.getBoundingBox().getCenter();
            Vec3 delta = targetCenter.subtract(start);
            if (delta.lengthSqr() > 0.0001D) {
                return new Vec3(delta.x, 0.0D, delta.z).normalize();
            }
        }
        Vec3 look = player.getLookAngle();
        Vec3 flattened = new Vec3(look.x, 0.0D, look.z);
        return flattened.lengthSqr() > 0.0001D ? flattened.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static void clearGorillaRush(Player player) {
        GORILLA_RUSH_STATES.remove(player.getUUID());
    }

    private static void tickMonowireSweep(Player player) {
        MonowireSweepState state = MONOWIRE_SWEEP_STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!player.isAlive()) {
            clearMonowireSweep(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime > state.endTick()) {
            clearMonowireSweep(player);
            return;
        }

        double progress = 1.0D - (double) (state.endTick() - gameTime) / 5.0D;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        Vec3 baseDirection = state.baseDirection().lengthSqr() > 0.0001D ? state.baseDirection() : flattenLook(player);
        double angleDegrees = -58.0D + 116.0D * progress;
        Vec3 sweepDirection = rotateHorizontal(baseDirection, angleDegrees);
        Vec3 sweepCenter = player.getBoundingBox().getCenter().add(sweepDirection.scale(Math.min(4.8D, state.range() * 0.72D)));
        spawnMonowireSweepTrail(player, state.cyberwareId(), sweepCenter, sweepDirection, progress);

        List<LivingEntity> targets = findTargetsInMonowireArc(player, sweepCenter, sweepDirection, state.range(), state.maxTargets(), state.hitTargets());
        for (LivingEntity target : targets) {
            state.hitTargets().add(target.getUUID());
            hitTarget(player, target, state.damage());
            applyVariantStatus(state.cyberwareId(), target, 4);
            Vec3 lateral = new Vec3(-sweepDirection.z, 0.0D, sweepDirection.x);
            target.setDeltaMovement(target.getDeltaMovement()
                    .add(lateral.scale(0.14D))
                    .add(sweepDirection.scale(0.10D))
                    .add(0.0D, 0.08D, 0.0D));
            target.hurtMarked = true;
            PlayerMotionSyncHelper.sync(target);
            spawnMonowireHitFeedback(player, target, state.cyberwareId(), sweepDirection);
        }
    }

    private static List<LivingEntity> findTargetsInMonowireArc(Player player, Vec3 sweepCenter, Vec3 sweepDirection, double range, int maxTargets, Set<UUID> alreadyHit) {
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(sweepCenter, 3.0D, 2.8D, 3.0D),
                entity -> entity != player
                        && entity.isAlive()
                        && isValidTarget(entity)
                        && !alreadyHit.contains(entity.getUUID())));
        Vec3 origin = player.getBoundingBox().getCenter();
        targets.removeIf(entity -> {
            Vec3 delta = entity.getBoundingBox().getCenter().subtract(origin);
            if (delta.lengthSqr() > range * range) {
                return true;
            }
            return delta.normalize().dot(sweepDirection) < 0.15D;
        });
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(sweepCenter)));
        if (targets.size() > maxTargets) {
            return new ArrayList<>(targets.subList(0, maxTargets));
        }
        return targets;
    }

    private static void clearMonowireSweep(Player player) {
        MONOWIRE_SWEEP_STATES.remove(player.getUUID());
    }

    private static void tickProjectileLaunch(Player player) {
        ProjectileLaunchState state = PROJECTILE_LAUNCH_STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!player.isAlive()) {
            clearProjectileLaunch(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        long totalTicks = Math.max(1L, state.totalTicks());
        long ticksRemaining = Math.max(0L, state.endTick() - gameTime);
        double progress = 1.0D - (double) ticksRemaining / (double) totalTicks;
        progress = Math.max(0.0D, Math.min(1.0D, progress));

        Vec3 previousPos = lerp(state.launchOrigin(), state.impactPoint(), Math.max(0.0D, progress - 1.0D / totalTicks));
        Vec3 currentPos = lerp(state.launchOrigin(), state.impactPoint(), progress);
        Vec3 direction = state.impactPoint().subtract(state.launchOrigin()).normalize();
        spawnProjectileLaunchTrail(player, previousPos, currentPos, state.cyberwareId());

        LivingEntity impactTarget = findProjectileLaunchImpactTarget(player, currentPos, direction, state.hitTargets());
        if (impactTarget != null) {
            detonateProjectileLaunch(player, state, impactTarget.position().add(0.0D, impactTarget.getBbHeight() * 0.35D, 0.0D), impactTarget);
            return;
        }

        if (gameTime >= state.endTick()) {
            detonateProjectileLaunch(player, state, state.impactPoint(), null);
        }
    }

    private static LivingEntity findProjectileLaunchImpactTarget(Player player, Vec3 currentPos, Vec3 direction, Set<UUID> alreadyHit) {
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(currentPos, 2.0D, 2.0D, 2.0D),
                entity -> entity != player
                        && entity.isAlive()
                        && isValidTarget(entity)
                        && !alreadyHit.contains(entity.getUUID())));
        targets.removeIf(entity -> {
            Vec3 delta = entity.getBoundingBox().getCenter().subtract(currentPos);
            if (delta.lengthSqr() > 2.25D) {
                return true;
            }
            return delta.normalize().dot(direction) < -0.25D;
        });
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(currentPos)));
        return targets.isEmpty() ? null : targets.get(0);
    }

    private static void detonateProjectileLaunch(Player player, ProjectileLaunchState state, Vec3 impactPoint, LivingEntity directTarget) {
        boolean hit = false;
        List<LivingEntity> targets = findTargetsAround(player, impactPoint, state.radius());
        for (LivingEntity target : targets) {
            hit = true;
            state.hitTargets().add(target.getUUID());
            float damage = state.damage();
            if (target == directTarget) {
                damage += 2.0F;
            }
            hitTarget(player, target, damage);
            Vec3 push = target.position().subtract(impactPoint);
            if (push.lengthSqr() < 0.0001D) {
                push = flattenLook(player);
            }
            target.knockback(target == directTarget ? 1.45D : 1.20D, push.x, push.z);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, target == directTarget ? 0.30D : 0.20D, 0.0D));
            target.hurtMarked = true;
            PlayerMotionSyncHelper.sync(target);
            applyVariantStatus(state.cyberwareId(), target, 5);
            spawnProjectileLaunchTargetFeedback(player, target, state.cyberwareId());
        }

        spawnProjectileLaunchImpactFeedback(player, impactPoint, state.radius(), hit, state.cyberwareId());
        clearProjectileLaunch(player);
    }

    private static void clearProjectileLaunch(Player player) {
        PROJECTILE_LAUNCH_STATES.remove(player.getUUID());
    }

    private static void tickExcavatorMode(Player player) {
        long until = ACTIVE_ARM_UNTIL_TICKS.getOrDefault(player.getUUID(), 0L);
        if (until <= 0L) {
            return;
        }
        if (!player.isAlive()) {
            clearExcavatorMode(player);
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        CyberwareItem armware = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.ARMS);
        if (armware == null || !EXCAVATOR_ARMS_ID.equals(armware.getDefinition().id()) || until <= player.level().getGameTime()) {
            clearExcavatorMode(player);
            return;
        }

        if (player.tickCount % 10 == 0) {
            spawnExcavatorModeTrail(player);
        }
    }

    private static boolean isExcavatorModeActive(Player player) {
        return ACTIVE_ARM_UNTIL_TICKS.getOrDefault(player.getUUID(), 0L) > player.level().getGameTime();
    }

    private static void clearExcavatorMode(Player player) {
        ACTIVE_ARM_UNTIL_TICKS.remove(player.getUUID());
        ACTIVE_ARM_TOTAL_SECONDS.remove(player.getUUID());
    }

    private static void spawnExcavatorModeTrail(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.getBoundingBox().getCenter().add(flattenLook(player).scale(0.45D));
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.16D, center.z, 4, 0.10D, 0.08D, 0.10D, 0.03D);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.08D, center.z, 2, 0.08D, 0.06D, 0.08D, 0.01D);
    }

    private static void spawnMantisLungeStartFeedback(Player player, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.getBoundingBox().getCenter().add(player.getLookAngle().scale(0.8D));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.2D, center.z, 2, 0.12D, 0.08D, 0.12D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.25D, center.z, 10, 0.20D, 0.20D, 0.20D, 0.04D);

        switch (id) {
            case ELECTRIFYING_MANTIS_BLADES_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.2D, center.z, 10, 0.22D, 0.14D, 0.22D, 0.04D);
            case THERMAL_MANTIS_BLADES_ID -> {
                level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.2D, center.z, 8, 0.18D, 0.14D, 0.18D, 0.02D);
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.28D, center.z, 4, 0.18D, 0.12D, 0.18D, 0.01D);
            }
            case TOXIC_MANTIS_BLADES_ID -> {
                level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.2D, center.z, 8, 0.18D, 0.14D, 0.18D, 0.02D);
                level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y + 0.12D, center.z, 4, 0.14D, 0.08D, 0.14D, 0.01D);
            }
            case MAXTAC_MANTIS_BLADES_ID -> {
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y + 0.22D, center.z, 6, 0.14D, 0.10D, 0.14D, 0.02D);
                level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.28D, center.z, 10, 0.20D, 0.14D, 0.20D, 0.06D);
            }
            default -> {
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 1.1F);
    }

    private static void spawnGorillaRushStartFeedback(Player player, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.getBoundingBox().getCenter().add(resolveGorillaDirection(player, null).scale(0.7D));
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.1D, center.z, 10, 0.20D, 0.08D, 0.20D, 0.02D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.2D, center.z, 8, 0.18D, 0.10D, 0.18D, 0.04D);
        switch (id) {
            case ELECTRIFYING_GORILLA_ARMS_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.20D, center.z, 10, 0.20D, 0.10D, 0.20D, 0.03D);
            case THERMAL_GORILLA_ARMS_ID -> {
                level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.18D, center.z, 8, 0.16D, 0.10D, 0.16D, 0.02D);
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.28D, center.z, 4, 0.18D, 0.10D, 0.18D, 0.01D);
            }
            case CHEMICAL_GORILLA_ARMS_ID -> {
                level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.18D, center.z, 8, 0.16D, 0.10D, 0.16D, 0.02D);
                level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y + 0.10D, center.z, 4, 0.14D, 0.06D, 0.14D, 0.01D);
            }
            default -> {
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.95F, 0.85F);
    }

    private static void spawnGorillaRushTrail(Player player, String id, Vec3 direction) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.getBoundingBox().getCenter().add(direction.scale(0.65D));
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.1D, center.z, 4, 0.08D, 0.05D, 0.08D, 0.01D);
        switch (id) {
            case ELECTRIFYING_GORILLA_ARMS_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.18D, center.z, 3, 0.10D, 0.08D, 0.10D, 0.02D);
            case THERMAL_GORILLA_ARMS_ID -> level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.16D, center.z, 3, 0.08D, 0.08D, 0.08D, 0.01D);
            case CHEMICAL_GORILLA_ARMS_ID -> level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.16D, center.z, 3, 0.08D, 0.08D, 0.08D, 0.01D);
            default -> level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.18D, center.z, 2, 0.08D, 0.08D, 0.08D, 0.02D);
        }
    }

    private static void spawnMantisLungeTrail(Player player, String id, Vec3 direction) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.getBoundingBox().getCenter().add(direction.scale(0.7D));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.15D, center.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.2D, center.z, 3, 0.08D, 0.08D, 0.08D, 0.02D);

        switch (id) {
            case ELECTRIFYING_MANTIS_BLADES_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.2D, center.z, 3, 0.10D, 0.10D, 0.10D, 0.02D);
            case THERMAL_MANTIS_BLADES_ID -> level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.2D, center.z, 3, 0.08D, 0.08D, 0.08D, 0.01D);
            case TOXIC_MANTIS_BLADES_ID -> level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.18D, center.z, 3, 0.08D, 0.08D, 0.08D, 0.01D);
            case MAXTAC_MANTIS_BLADES_ID -> level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.2D, center.z, 4, 0.12D, 0.10D, 0.12D, 0.04D);
            default -> {
            }
        }
    }

    private static void spawnMonowireSweepStartFeedback(Player player, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.getBoundingBox().getCenter().add(flattenLook(player).scale(0.8D));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.18D, center.z, 2, 0.14D, 0.08D, 0.14D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.22D, center.z, 8, 0.22D, 0.10D, 0.22D, 0.04D);
        switch (id) {
            case ELECTRIFYING_MONOWIRE_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.22D, center.z, 10, 0.24D, 0.12D, 0.24D, 0.03D);
            case THERMAL_MONOWIRE_ID -> {
                level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.20D, center.z, 8, 0.18D, 0.10D, 0.18D, 0.02D);
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.28D, center.z, 4, 0.18D, 0.10D, 0.18D, 0.01D);
            }
            case TOXIC_MONOWIRE_ID -> {
                level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.20D, center.z, 8, 0.18D, 0.10D, 0.18D, 0.02D);
                level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y + 0.10D, center.z, 4, 0.14D, 0.06D, 0.14D, 0.01D);
            }
            default -> {
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 1.25F);
    }

    private static void spawnMonowireSweepTrail(Player player, String id, Vec3 center, Vec3 direction, double progress) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 lateral = new Vec3(-direction.z, 0.0D, direction.x).scale(0.7D);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.16D, center.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x + lateral.x, center.y + 0.18D, center.z + lateral.z, 3, 0.10D, 0.08D, 0.10D, 0.02D);
        level.sendParticles(ParticleTypes.CRIT, center.x - lateral.x, center.y + 0.18D, center.z - lateral.z, 3, 0.10D, 0.08D, 0.10D, 0.02D);

        switch (id) {
            case ELECTRIFYING_MONOWIRE_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.18D, center.z, 4, 0.16D, 0.10D, 0.16D, 0.03D);
            case THERMAL_MONOWIRE_ID -> level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.16D, center.z, 4, 0.12D, 0.08D, 0.12D, 0.02D);
            case TOXIC_MONOWIRE_ID -> level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.16D, center.z, 4, 0.12D, 0.08D, 0.12D, 0.02D);
            default -> {
            }
        }
    }

    private static void spawnMonowireHitFeedback(Player player, LivingEntity target, String id, Vec3 direction) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.16D, center.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.22D, center.z, 8, 0.18D, 0.18D, 0.18D, 0.04D);
        switch (id) {
            case ELECTRIFYING_MONOWIRE_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.22D, center.z, 8, 0.20D, 0.20D, 0.20D, 0.03D);
            case THERMAL_MONOWIRE_ID -> {
                level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.18D, center.z, 6, 0.14D, 0.14D, 0.14D, 0.02D);
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.28D, center.z, 3, 0.14D, 0.12D, 0.14D, 0.01D);
            }
            case TOXIC_MONOWIRE_ID -> {
                level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.18D, center.z, 6, 0.14D, 0.14D, 0.14D, 0.02D);
                level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y + 0.10D, center.z, 3, 0.12D, 0.08D, 0.12D, 0.01D);
            }
            default -> level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.22D, center.z, 6, 0.18D, 0.14D, 0.18D, 0.04D);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.72F, 1.35F);
    }

    private static void spawnProjectileLaunchStartFeedback(Player player, Vec3 launchOrigin, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(ParticleTypes.SMOKE, launchOrigin.x, launchOrigin.y, launchOrigin.z, 7, 0.12D, 0.12D, 0.12D, 0.01D);
        level.sendParticles(ParticleTypes.CRIT, launchOrigin.x, launchOrigin.y, launchOrigin.z, 6, 0.10D, 0.10D, 0.10D, 0.04D);
        switch (id) {
            case ELECTRIFYING_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, launchOrigin.x, launchOrigin.y, launchOrigin.z, 10, 0.14D, 0.14D, 0.14D, 0.04D);
            case THERMAL_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.SMALL_FLAME, launchOrigin.x, launchOrigin.y, launchOrigin.z, 8, 0.10D, 0.10D, 0.10D, 0.02D);
            case TOXIC_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.SNEEZE, launchOrigin.x, launchOrigin.y, launchOrigin.z, 8, 0.10D, 0.10D, 0.10D, 0.02D);
            default -> {
            }
        }
        level.playSound(null, launchOrigin.x, launchOrigin.y, launchOrigin.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.55F, 0.9F);
    }

    private static void spawnProjectileLaunchTrail(Player player, Vec3 from, Vec3 to, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 delta = to.subtract(from);
        int steps = Math.max(1, (int) Math.ceil(delta.length() / 0.45D));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 1.0D : (double) i / (double) steps;
            Vec3 point = from.add(delta.scale(t));
            level.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            switch (id) {
                case ELECTRIFYING_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.04D, 0.04D, 0.04D, 0.01D);
                case THERMAL_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.SMALL_FLAME, point.x, point.y, point.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
                case TOXIC_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.SNEEZE, point.x, point.y, point.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
                default -> level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.03D, 0.03D, 0.03D, 0.01D);
            }
        }
    }

    private static void spawnProjectileLaunchImpactFeedback(Player player, Vec3 impactPoint, double radius, boolean hit, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(ParticleTypes.EXPLOSION, impactPoint.x, impactPoint.y + 0.15D, impactPoint.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.POOF, impactPoint.x, impactPoint.y + 0.20D, impactPoint.z, hit ? 18 : 10, radius * 0.22D, 0.16D, radius * 0.22D, 0.02D);
        level.sendParticles(ParticleTypes.SMOKE, impactPoint.x, impactPoint.y + 0.32D, impactPoint.z, hit ? 16 : 8, radius * 0.28D, 0.18D, radius * 0.28D, 0.01D);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, impactPoint.x, impactPoint.y + 0.20D, impactPoint.z, hit ? 8 : 3, radius * 0.16D, 0.10D, radius * 0.16D, 0.02D);
        switch (id) {
            case ELECTRIFYING_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impactPoint.x, impactPoint.y + 0.24D, impactPoint.z, hit ? 20 : 10, radius * 0.30D, 0.18D, radius * 0.30D, 0.04D);
            case THERMAL_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.SMALL_FLAME, impactPoint.x, impactPoint.y + 0.20D, impactPoint.z, hit ? 16 : 8, radius * 0.22D, 0.14D, radius * 0.22D, 0.02D);
            case TOXIC_PROJECTILE_LAUNCH_SYSTEM_ID -> {
                level.sendParticles(ParticleTypes.SNEEZE, impactPoint.x, impactPoint.y + 0.22D, impactPoint.z, hit ? 16 : 8, radius * 0.22D, 0.14D, radius * 0.22D, 0.02D);
                level.sendParticles(ParticleTypes.ITEM_SLIME, impactPoint.x, impactPoint.y + 0.12D, impactPoint.z, hit ? 8 : 4, radius * 0.14D, 0.08D, radius * 0.14D, 0.01D);
            }
            default -> {
            }
        }
        level.playSound(null, impactPoint.x, impactPoint.y, impactPoint.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.85F, 0.92F);
    }

    private static void spawnProjectileLaunchTargetFeedback(Player player, LivingEntity target, String id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y + 0.15D, center.z, 4, 0.16D, 0.16D, 0.16D, 0.01D);
        switch (id) {
            case ELECTRIFYING_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.22D, center.z, 6, 0.18D, 0.18D, 0.18D, 0.03D);
            case THERMAL_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.18D, center.z, 5, 0.14D, 0.14D, 0.14D, 0.02D);
            case TOXIC_PROJECTILE_LAUNCH_SYSTEM_ID -> level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.18D, center.z, 5, 0.14D, 0.14D, 0.14D, 0.02D);
            default -> level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.20D, center.z, 4, 0.14D, 0.14D, 0.14D, 0.02D);
        }
    }

    private static void spawnExcavatorFeedback(Player player, BlockPos centerPos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(centerPos);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 10, 0.75D, 0.35D, 0.75D, 0.03D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.1D, center.z, 10, 0.85D, 0.35D, 0.85D, 0.06D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.45F, 0.85F);
    }

    private static void spawnExcavatorActivationFeedback(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.getBoundingBox().getCenter().add(flattenLook(player).scale(0.55D));
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.2D, center.z, 10, 0.20D, 0.16D, 0.20D, 0.04D);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.1D, center.z, 8, 0.18D, 0.12D, 0.18D, 0.02D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.45F, 1.55F);
    }

    private static void spawnMantisImpactFeedback(Player player, LivingEntity target, String id, Vec3 direction) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.2D, center.z, 2, 0.10D, 0.10D, 0.10D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.25D, center.z, 12, 0.18D, 0.20D, 0.18D, 0.06D);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y + 0.15D, center.z, 6, 0.16D, 0.16D, 0.16D, 0.02D);

        switch (id) {
            case ELECTRIFYING_MANTIS_BLADES_ID -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.25D, center.z, 10, 0.22D, 0.22D, 0.22D, 0.03D);
            case THERMAL_MANTIS_BLADES_ID -> {
                level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, center.y + 0.2D, center.z, 8, 0.16D, 0.18D, 0.16D, 0.02D);
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.32D, center.z, 4, 0.16D, 0.14D, 0.16D, 0.01D);
            }
            case TOXIC_MANTIS_BLADES_ID -> {
                level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.22D, center.z, 8, 0.16D, 0.16D, 0.16D, 0.02D);
                level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y + 0.12D, center.z, 4, 0.12D, 0.08D, 0.12D, 0.01D);
            }
            case MAXTAC_MANTIS_BLADES_ID -> {
                level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.28D, center.z, 12, 0.20D, 0.18D, 0.20D, 0.05D);
                level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.30D, center.z, 10, 0.22D, 0.20D, 0.22D, 0.08D);
            }
            default -> {
            }
        }

        level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9F, id.equals(MAXTAC_MANTIS_BLADES_ID) ? 0.7F : 0.9F);
    }

    private static Vec3 flattenLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        return flat.lengthSqr() > 0.0001D ? flat.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static Vec3 rotateHorizontal(Vec3 direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(direction.x * cos - direction.z * sin, 0.0D, direction.x * sin + direction.z * cos).normalize();
    }

    private static Vec3 lerp(Vec3 start, Vec3 end, double progress) {
        return start.add(end.subtract(start).scale(progress));
    }

    private record MantisLungeState(String cyberwareId, float damage, double range, long endTick, UUID preferredTargetId) {
        private LivingEntity resolvePreferredTarget(Player player) {
            if (preferredTargetId == null) {
                return null;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return null;
            }
            if (!(level.getEntity(preferredTargetId) instanceof LivingEntity livingEntity)) {
                return null;
            }
            return livingEntity.isAlive() && isValidTarget(livingEntity) ? livingEntity : null;
        }
    }

    private record GorillaRushState(String cyberwareId, float damage, double radius, long endTick, UUID preferredTargetId) {
        private LivingEntity resolvePreferredTarget(Player player) {
            if (preferredTargetId == null) {
                return null;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return null;
            }
            if (!(level.getEntity(preferredTargetId) instanceof LivingEntity livingEntity)) {
                return null;
            }
            return livingEntity.isAlive() && isValidTarget(livingEntity) ? livingEntity : null;
        }
    }

    private record MonowireSweepState(String cyberwareId, float damage, double range, int maxTargets, long endTick, Vec3 baseDirection, Set<UUID> hitTargets) {
    }

    private record ProjectileLaunchState(String cyberwareId, float damage, double radius, long endTick, Vec3 launchOrigin, Vec3 impactPoint, Set<UUID> hitTargets) {
        private long totalTicks() {
            return Math.max(1L, Math.round(launchOrigin.distanceTo(impactPoint) / 3.2D));
        }
    }
}
