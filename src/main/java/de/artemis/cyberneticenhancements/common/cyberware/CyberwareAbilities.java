package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanManager;
import de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanProfile;
import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CyberwareAbilities {
    private static final String BIODYN_BERSERK_ID = "biodyn_berserk";
    private static final String MILITECH_BERSERK_ID = "militech_berserk";
    private static final String MOORE_TECH_BERSERK_ID = "moore_tech_berserk";
    private static final String ZETATECH_BERSERK_ID = "zetatech_berserk";
    private static final String ARASAKA_SHADOW_ID = "arasaka_shadow";
    private static final String BIOTECH_SIGMA_ID = "biotech_sigma";
    private static final String MILITECH_PARALINE_ID = "militech_paraline";
    private static final String NETWATCH_NETDRIVER_ID = "netwatch_netdriver";
    private static final String RAVEN_MICROCYBER_ID = "raven_microcyber";
    private static final String TETRATRONIC_RIPPLER_ID = "tetratronic_rippler";
    private static final String MILITECH_CANTO_ID = "militech_canto";
    private static final String DYNALAR_SANDEVISTAN_ID = "dynalar_sandevistan";
    private static final String ZETATECH_SANDEVISTAN_ID = "zetatech_sandevistan";
    private static final String MILITECH_FALCON_ID = "militech_falcon";
    private static final String QIANT_WARP_DANCER_ID = "qiant_warp_dancer";
    private static final String MILITECH_APOGEE_ID = "militech_apogee";
    private static final String CHROME_COMPRESSOR_ID = "chrome_compressor";
    private static final double ARASAKA_SHADOW_AGGRO_RADIUS = 10.0D;
    private static final double ARASAKA_SHADOW_SAFE_BUBBLE_SQR = 9.0D;

    private static final Map<String, OperatingSystemSpec> OPERATING_SYSTEMS = createOperatingSystems();
    private static final Map<UUID, Long> ACTIVE_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> ACTIVE_COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, String> ACTIVE_OPERATING_SYSTEM = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_DURATION_SECONDS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_COOLDOWN_SECONDS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_KILL_COUNTS = new HashMap<>();
    private static final Map<UUID, Long> BIOMONITOR_COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Integer> BIOMONITOR_COOLDOWN_TOTAL_SECONDS = new HashMap<>();
    private static final Map<UUID, Long> BLOOD_PUMP_COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Integer> BLOOD_PUMP_COOLDOWN_TOTAL_SECONDS = new HashMap<>();
    private static final Map<UUID, Long> SECOND_HEART_COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Integer> SECOND_HEART_COOLDOWN_TOTAL_SECONDS = new HashMap<>();
    private static final Map<UUID, Long> REFLEX_TUNER_COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Integer> REFLEX_TUNER_COOLDOWN_TOTAL_SECONDS = new HashMap<>();

    private CyberwareAbilities() {
    }

    public static void activate(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        CyberwareItem operatingSystem = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.OPERATING_SYSTEM);
        if (operatingSystem == null) {
            notify(player, "message.cyberneticenhancements.ability.no_operating_system");
            return;
        }

        OperatingSystemSpec spec = getOperatingSystemSpec(operatingSystem);
        if (spec == null || !spec.hasActiveAbility()) {
            notify(player, "message.cyberneticenhancements.ability.no_active_ability");
            return;
        }

        long gameTime = player.level().getGameTime();
        if (isCurrentlyActive(player, spec.id(), gameTime) && spec.family() == OperatingSystemFamily.SANDEVISTAN) {
            clearActiveState(player, false, true);
            notify(player, "message.cyberneticenhancements.ability.disengaged", Component.translatable(spec.itemTranslationKey()));
            return;
        }

        long cooldownUntil = Math.max(ACTIVE_COOLDOWN_UNTIL_TICK.getOrDefault(player.getUUID(), 0L),
                gameTime + SandevistanManager.recoveryRemaining(player));
        if (cooldownUntil > gameTime) {
            notify(player, "message.cyberneticenhancements.ability.cooldown", (cooldownUntil - gameTime + 19L) / 20L);
            return;
        }

        if (spec.family() == OperatingSystemFamily.SANDEVISTAN && !SandevistanManager.canActivate(player, spec.id())) return;

        clearActiveState(player, false, false);
        activateOperatingSystem(player, inventory, spec, gameTime);
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        cleanupExpiredActiveState(player, inventory, player.level().getGameTime(), true);
    }

    public static void applyTriggeredAbilities(Player player, PlayerCyberwareInventory inventory) {
        if (player.level().isClientSide()) {
            return;
        }

        long gameTime = player.level().getGameTime();
        float healthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());

        if (healthRatio <= 0.35F && inventory.hasInstalledCyberware("biomonitor") && isReady(BIOMONITOR_COOLDOWN_UNTIL_TICK, player, gameTime)) {
            player.heal((float) (4.0 * CyberwareUpgradeHelper.functionMultiplier(inventory, "biomonitor")));
            TemporaryCyberwareEffectManager.addEffects(player, 160,
                    CyberwareEffect.healthRegen(1.0D),
                    CyberwareEffect.bonusAbsorption(4.0D));
            setTriggeredCooldown(player, inventory, BIOMONITOR_COOLDOWN_UNTIL_TICK, BIOMONITOR_COOLDOWN_TOTAL_SECONDS,
                    CyberwareBalance.resolveTriggeredCooldownSeconds("biomonitor"));
            notify(player, "message.cyberneticenhancements.trigger.biomonitor");
        }

        if (healthRatio <= 0.25F && inventory.hasInstalledCyberware("blood_pump") && isReady(BLOOD_PUMP_COOLDOWN_UNTIL_TICK, player, gameTime)) {
            player.heal((float) (6.0 * CyberwareUpgradeHelper.functionMultiplier(inventory, "blood_pump")));
            TemporaryCyberwareEffectManager.addEffects(player, 200,
                    CyberwareEffect.bonusAbsorption(8.0D),
                    CyberwareEffect.damageReduction(0.10D));
            setTriggeredCooldown(player, inventory, BLOOD_PUMP_COOLDOWN_UNTIL_TICK, BLOOD_PUMP_COOLDOWN_TOTAL_SECONDS,
                    CyberwareBalance.resolveTriggeredCooldownSeconds("blood_pump"));
            notify(player, "message.cyberneticenhancements.trigger.blood_pump");
        }

        if (healthRatio <= 0.15F && inventory.hasInstalledCyberware("second_heart") && isReady(SECOND_HEART_COOLDOWN_UNTIL_TICK, player, gameTime)) {
            double multiplier = CyberwareUpgradeHelper.functionMultiplier(inventory, "second_heart");
            float healTarget = (float) Math.max(player.getMaxHealth() * 0.5 * multiplier, player.getHealth() + 10.0 * multiplier);
            player.setHealth(Math.min(player.getMaxHealth(), healTarget));
            TemporaryCyberwareEffectManager.addEffects(player, 240,
                    CyberwareEffect.healthRegen(1.5D),
                    CyberwareEffect.bonusAbsorption(12.0D),
                    CyberwareEffect.damageReduction(0.20D));
            setTriggeredCooldown(player, inventory, SECOND_HEART_COOLDOWN_UNTIL_TICK, SECOND_HEART_COOLDOWN_TOTAL_SECONDS,
                    CyberwareBalance.resolveTriggeredCooldownSeconds("second_heart"));
            notify(player, "message.cyberneticenhancements.trigger.second_heart");
        }

        if (healthRatio <= 0.30F && inventory.hasInstalledCyberware("reflex_tuner") && isReady(REFLEX_TUNER_COOLDOWN_UNTIL_TICK, player, gameTime)) {
            TemporaryCyberwareEffectManager.addEffects(player, 120L + FrontalCortexManager.getReflexExtensionTicks(inventory),
                    CyberwareEffect.moveSpeed(0.40D),
                    CyberwareEffect.breakSpeed(0.20D),
                    CyberwareEffect.attackSpeed(0.20D));
            setTriggeredCooldown(player, inventory, REFLEX_TUNER_COOLDOWN_UNTIL_TICK, REFLEX_TUNER_COOLDOWN_TOTAL_SECONDS,
                    CyberwareBalance.resolveTriggeredCooldownSeconds("reflex_tuner"));
            notify(player, "message.cyberneticenhancements.trigger.reflex_tuner");
        }
    }

    public static void mergeActiveEffects(Player player, PlayerCyberwareInventory inventory, EnumMap<CyberwareEffectType, Double> totals) {
        long gameTime = player.level().getGameTime();
        cleanupExpiredActiveState(player, inventory, gameTime, false);

        String activeId = ACTIVE_OPERATING_SYSTEM.get(player.getUUID());
        if (activeId == null) {
            return;
        }

        var sandevistan = SandevistanManager.session(player);
        if (sandevistan != null) {
            double condition = 0.55 + 0.45 * sandevistan.condition;
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, sandevistan.profile.movement() * condition);
            merge(totals, CyberwareEffectType.ATTACK_SPEED, sandevistan.profile.attackSpeed() * condition);
        }

        switch (activeId) {
            case DYNALAR_SANDEVISTAN_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 1.5D);
            }
            case ZETATECH_SANDEVISTAN_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 2.0D);
                if (!player.onGround()) {
                    merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.15D);
                    merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 1.5D);
                    merge(totals, CyberwareEffectType.JUMP_POWER, 0.25D);
                }
            }
            case MILITECH_FALCON_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 3.0D);
                merge(totals, CyberwareEffectType.JUMP_POWER, 0.20D);
            }
            case QIANT_WARP_DANCER_ID -> {
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.15D);
                merge(totals, CyberwareEffectType.JUMP_POWER, 0.20D);
                merge(totals, CyberwareEffectType.FIRE_RESISTANCE, 0.0D);
            }
            case MILITECH_APOGEE_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 4.0D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.10D);
                merge(totals, CyberwareEffectType.JUMP_POWER, 0.35D);
            }
            case BIODYN_BERSERK_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 3.0D);
                merge(totals, CyberwareEffectType.ARMOR, 4.0D);
                merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.15D);
                merge(totals, CyberwareEffectType.BONUS_ABSORPTION, 8.0D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.20D);
                merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.25D);
            }
            case MILITECH_BERSERK_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 4.0D);
                merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.25D);
                merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.15D);
                merge(totals, CyberwareEffectType.ARMOR, 4.0D);
                merge(totals, CyberwareEffectType.BONUS_ABSORPTION, 10.0D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.20D);
                if (player.getHealth() / Math.max(1.0F, player.getMaxHealth()) <= 0.50F) {
                    merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 2.0D);
                    merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.10D);
                }
            }
            case MOORE_TECH_BERSERK_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 4.0D);
                merge(totals, CyberwareEffectType.ARMOR, 5.0D);
                merge(totals, CyberwareEffectType.BONUS_ABSORPTION, 10.0D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.25D);
                merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.35D);
                if (player.getHealth() / Math.max(1.0F, player.getMaxHealth()) <= 0.40F) {
                    merge(totals, CyberwareEffectType.HEALTH_REGEN, 1.0D);
                }
            }
            case ZETATECH_BERSERK_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 3.5D);
                merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.25D);
                merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.20D);
                merge(totals, CyberwareEffectType.BONUS_ABSORPTION, 8.0D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.18D);
                merge(totals, CyberwareEffectType.FALL_DAMAGE_REDUCTION, -0.30D);
            }
            case ARASAKA_SHADOW_ID -> {
                merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.15D);
                merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.10D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.10D);
            }
            case BIOTECH_SIGMA_ID -> {
                merge(totals, CyberwareEffectType.HEALTH_REGEN, 0.8D);
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 2.0D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.08D);
            }
            case MILITECH_PARALINE_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.20D);
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 2.0D);
                merge(totals, CyberwareEffectType.ENTITY_REACH, 0.5D);
                merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.10D);
            }
            case NETWATCH_NETDRIVER_ID -> {
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.12D);
                merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.10D);
                merge(totals, CyberwareEffectType.ENTITY_REACH, 1.0D);
            }
            case RAVEN_MICROCYBER_ID -> {
                merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.10D);
                merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.15D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.10D);
            }
            case TETRATRONIC_RIPPLER_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.20D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.12D);
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 2.5D);
            }
            case MILITECH_CANTO_ID -> {
                merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 3.0D);
                merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.15D);
                merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.12D);
                merge(totals, CyberwareEffectType.NIGHT_VISION, 0.0D);
            }
            default -> {
            }
        }
    }

    public static void onKill(Player player, LivingEntity target) {
        if (player.level().isClientSide()) {
            return;
        }

        String activeId = ACTIVE_OPERATING_SYSTEM.get(player.getUUID());
        if (activeId == null) {
            return;
        }

        if (SandevistanProfile.supports(activeId)) {
            var session = SandevistanManager.session(player);
            if (session != null && target instanceof Enemy
                    && (MILITECH_FALCON_ID.equals(activeId) || MILITECH_APOGEE_ID.equals(activeId))
                    && session.rewardKill(target.getUUID(), MILITECH_FALCON_ID.equals(activeId) ? 20 : 30)) {
                  ACTIVE_UNTIL_TICK.put(player.getUUID(), player.level().getGameTime()
                          + Math.max(0, session.until() - player.getServer().overworld().getGameTime()));
                ACTIVE_DURATION_SECONDS.put(player.getUUID(), (int) ((session.until() - session.started + 19) / 20));
                if (MILITECH_FALCON_ID.equals(activeId)) player.heal(2);
                else TemporaryCyberwareEffectManager.addEffects(player, 80L,
                        CyberwareEffect.bonusAbsorption(4), CyberwareEffect.attackSpeed(0.10));
            }
            return;
        }

        switch (activeId) {
            case BIODYN_BERSERK_ID, MILITECH_BERSERK_ID, MOORE_TECH_BERSERK_ID, ZETATECH_BERSERK_ID ->
                    ACTIVE_KILL_COUNTS.merge(player.getUUID(), 1, Integer::sum);
            default -> {
            }
        }
    }

    public static boolean hasSpecialBehavior(String id) {
        OperatingSystemSpec spec = OPERATING_SYSTEMS.get(id);
        return spec != null && (!spec.hudKey().isEmpty() || id.equals(CHROME_COMPRESSOR_ID));
    }

    public static void appendBehaviorTooltip(CyberwareDefinition definition, List<Component> tooltipComponents) {
        if (!hasSpecialBehavior(definition.id())) {
            return;
        }

        tooltipComponents.add(Component.translatable("tooltip.cyberneticenhancements.special." + definition.id())
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    public static OperatingSystemFamily getOperatingSystemFamily(CyberwareItem operatingSystem) {
        OperatingSystemSpec spec = getOperatingSystemSpec(operatingSystem);
        return spec == null ? OperatingSystemFamily.NONE : spec.family();
    }

    public static String getAbilityTranslationKey(Player player, PlayerCyberwareInventory inventory) {
        String activeId = ACTIVE_OPERATING_SYSTEM.get(player.getUUID());
        OperatingSystemSpec spec = activeId == null ? null : OPERATING_SYSTEMS.get(activeId);
        if (spec != null && spec.hasActiveAbility()) {
            return spec.hudKey();
        }

        CyberwareItem operatingSystem = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.OPERATING_SYSTEM);
        spec = operatingSystem == null ? null : getOperatingSystemSpec(operatingSystem);
        if (spec == null || !spec.hasActiveAbility()) {
            return "";
        }
        return spec.hudKey();
    }

    public static int getBaseAbilityCooldownSeconds(CyberwareItem operatingSystem) {
        OperatingSystemSpec spec = getOperatingSystemSpec(operatingSystem);
        return spec == null ? 0 : spec.cooldownSeconds();
    }

    public static int getActiveSecondsRemaining(Player player) {
        return getRemainingSeconds(ACTIVE_UNTIL_TICK, player);
    }

    public static int getAbilityCooldownSecondsRemaining(Player player) {
        return Math.max(getRemainingSeconds(ACTIVE_COOLDOWN_UNTIL_TICK, player), (int) ((SandevistanManager.recoveryRemaining(player) + 19) / 20));
    }

    public static int getActiveTotalSeconds(Player player) {
        return ACTIVE_DURATION_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static int getAbilityCooldownTotalSeconds(Player player) {
        return Math.max(ACTIVE_COOLDOWN_SECONDS.getOrDefault(player.getUUID(), 0), SandevistanManager.recoveryTotalSeconds(player));
    }

    public static OperatingSystemFamily getActiveFamily(Player player) {
        String activeId = ACTIVE_OPERATING_SYSTEM.get(player.getUUID());
        OperatingSystemSpec spec = activeId == null ? null : OPERATING_SYSTEMS.get(activeId);
        return spec == null ? OperatingSystemFamily.NONE : spec.family();
    }

    public static int getBiomonitorCooldownSecondsRemaining(Player player) {
        return getRemainingSeconds(BIOMONITOR_COOLDOWN_UNTIL_TICK, player);
    }

    public static int getBloodPumpCooldownSecondsRemaining(Player player) {
        return getRemainingSeconds(BLOOD_PUMP_COOLDOWN_UNTIL_TICK, player);
    }

    public static int getBiomonitorCooldownTotalSeconds(Player player) {
        return BIOMONITOR_COOLDOWN_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static int getBloodPumpCooldownTotalSeconds(Player player) {
        return BLOOD_PUMP_COOLDOWN_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static int getSecondHeartCooldownSecondsRemaining(Player player) {
        return getRemainingSeconds(SECOND_HEART_COOLDOWN_UNTIL_TICK, player);
    }

    public static int getSecondHeartCooldownTotalSeconds(Player player) {
        return SECOND_HEART_COOLDOWN_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static int getReflexTunerCooldownSecondsRemaining(Player player) {
        return getRemainingSeconds(REFLEX_TUNER_COOLDOWN_UNTIL_TICK, player);
    }

    public static int getReflexTunerCooldownTotalSeconds(Player player) {
        return REFLEX_TUNER_COOLDOWN_TOTAL_SECONDS.getOrDefault(player.getUUID(), 0);
    }

    public static void clearCooldowns(Player player) {
        stopActiveOperatingSystem(player);
        SandevistanManager.resetRecovery(player);
        clearTransientState(player);
    }

    public static void onPlayerLogout(Player player) {
        stopActiveOperatingSystem(player);
        clearTransientState(player);
    }

    private static void clearTransientState(Player player) {
        UUID playerId = player.getUUID();
        ACTIVE_UNTIL_TICK.remove(playerId);
        ACTIVE_COOLDOWN_UNTIL_TICK.remove(playerId);
        ACTIVE_OPERATING_SYSTEM.remove(playerId);
        ACTIVE_DURATION_SECONDS.remove(playerId);
        ACTIVE_COOLDOWN_SECONDS.remove(playerId);
        ACTIVE_KILL_COUNTS.remove(playerId);
        BIOMONITOR_COOLDOWN_UNTIL_TICK.remove(playerId);
        BIOMONITOR_COOLDOWN_TOTAL_SECONDS.remove(playerId);
        BLOOD_PUMP_COOLDOWN_UNTIL_TICK.remove(playerId);
        BLOOD_PUMP_COOLDOWN_TOTAL_SECONDS.remove(playerId);
        SECOND_HEART_COOLDOWN_UNTIL_TICK.remove(playerId);
        SECOND_HEART_COOLDOWN_TOTAL_SECONDS.remove(playerId);
        REFLEX_TUNER_COOLDOWN_UNTIL_TICK.remove(playerId);
        REFLEX_TUNER_COOLDOWN_TOTAL_SECONDS.remove(playerId);
    }

    public static void reduceTrackedCooldowns(Player player, long flatTicks, double percentRefund) {
        reduceCooldown(ACTIVE_COOLDOWN_UNTIL_TICK, player, flatTicks, percentRefund);
        reduceCooldown(BIOMONITOR_COOLDOWN_UNTIL_TICK, player, flatTicks, percentRefund);
        reduceCooldown(BLOOD_PUMP_COOLDOWN_UNTIL_TICK, player, flatTicks, percentRefund);
        reduceCooldown(SECOND_HEART_COOLDOWN_UNTIL_TICK, player, flatTicks, percentRefund);
        reduceCooldown(REFLEX_TUNER_COOLDOWN_UNTIL_TICK, player, flatTicks, percentRefund);
    }

    public static void clearAbilityCooldown(Player player) {
        ACTIVE_COOLDOWN_UNTIL_TICK.remove(player.getUUID());
        ACTIVE_COOLDOWN_SECONDS.remove(player.getUUID());
    }

    public static void enforceAbilityCooldown(Player player, long until, long cooldownTicks) {
        ACTIVE_COOLDOWN_UNTIL_TICK.merge(player.getUUID(), until, Math::max);
        ACTIVE_COOLDOWN_SECONDS.put(player.getUUID(), (int) Math.max(1, (cooldownTicks + 19) / 20));
    }

    public static void stopActiveOperatingSystem(Player player) {
        if (!player.level().isClientSide()) clearActiveState(player, false, true);
    }

    public static void clearBiomonitorCooldown(Player player) {
        BIOMONITOR_COOLDOWN_UNTIL_TICK.remove(player.getUUID());
        BIOMONITOR_COOLDOWN_TOTAL_SECONDS.remove(player.getUUID());
    }

    public static void clearBloodPumpCooldown(Player player) {
        BLOOD_PUMP_COOLDOWN_UNTIL_TICK.remove(player.getUUID());
        BLOOD_PUMP_COOLDOWN_TOTAL_SECONDS.remove(player.getUUID());
    }

    public static void clearSecondHeartCooldown(Player player) {
        SECOND_HEART_COOLDOWN_UNTIL_TICK.remove(player.getUUID());
        SECOND_HEART_COOLDOWN_TOTAL_SECONDS.remove(player.getUUID());
    }

    public static void clearReflexTunerCooldown(Player player) {
        REFLEX_TUNER_COOLDOWN_UNTIL_TICK.remove(player.getUUID());
        REFLEX_TUNER_COOLDOWN_TOTAL_SECONDS.remove(player.getUUID());
    }

    private static void activateOperatingSystem(Player player, PlayerCyberwareInventory inventory, OperatingSystemSpec spec, long gameTime) {
        long adjustedCooldown = FrontalCortexManager.adjustCyberwareCooldownTicks(player, inventory, spec.cooldownSeconds() * 20L, true);
        if (spec.family() == OperatingSystemFamily.SANDEVISTAN) {
            var stack = inventory.getStackInSlot(CyberwareSlot.OPERATING_SYSTEM_1.ordinal());
            adjustedCooldown = de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanUpgrades.cooldown(adjustedCooldown, stack);
        }
        long durationTicks = spec.family() == OperatingSystemFamily.SANDEVISTAN
                ? SandevistanManager.adjustedDuration(player, spec.durationSeconds() * 20L) : spec.durationSeconds() * 20L;
        ACTIVE_OPERATING_SYSTEM.put(player.getUUID(), spec.id());
        ACTIVE_UNTIL_TICK.put(player.getUUID(), gameTime + durationTicks);
        ACTIVE_COOLDOWN_UNTIL_TICK.put(player.getUUID(), gameTime + adjustedCooldown);
        ACTIVE_DURATION_SECONDS.put(player.getUUID(), (int) ((durationTicks + 19) / 20));
        ACTIVE_COOLDOWN_SECONDS.put(player.getUUID(), (int) Math.max(1L, (adjustedCooldown + 19L) / 20L));
        ACTIVE_KILL_COUNTS.put(player.getUUID(), 0);
        applyActivationEffects(player, spec);
        if (spec.family() == OperatingSystemFamily.SANDEVISTAN) {
            SandevistanManager.start(player, spec.id(), durationTicks, adjustedCooldown);
        } else {
            CyberstrainManager.onCyberwareActivated(player, resolvePsychosisActivationLoad(spec));
        }
        CyberwareEffects.refreshPlayerCyberware(player);
        notify(player, "message.cyberneticenhancements.ability.engaged", Component.translatable(spec.itemTranslationKey()), (durationTicks + 19) / 20);
    }

    private static void applyActivationEffects(Player player, OperatingSystemSpec spec) {
        switch (spec.id()) {
            case MILITECH_BERSERK_ID -> TemporaryCyberwareEffectManager.addEffects(player, 40L,
                    CyberwareEffect.damageReduction(0.60D),
                    CyberwareEffect.bonusAbsorption(8.0D));
            case ARASAKA_SHADOW_ID -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, spec.durationSeconds() * 20 + 20, 0, true, false, false));
                softenShadowAggro(player);
            }
            case BIOTECH_SIGMA_ID -> applyPulse(player, 8.0D, 3,
                    effect(MobEffects.POISON, 80, 0),
                    effect(MobEffects.WEAKNESS, 80, 0));
            case MILITECH_PARALINE_ID -> applyPulse(player, 10.0D, 3,
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 80, 1),
                    effect(MobEffects.GLOWING, 80, 0));
            case NETWATCH_NETDRIVER_ID -> applyPulse(player, 14.0D, 4,
                    effect(MobEffects.WEAKNESS, 100, 0),
                    effect(MobEffects.GLOWING, 100, 0),
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            case RAVEN_MICROCYBER_ID -> applyPulse(player, 12.0D, 4,
                    effect(MobEffects.POISON, 80, 0),
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 80, 0),
                    effect(MobEffects.GLOWING, 80, 0));
            case TETRATRONIC_RIPPLER_ID -> applyPulse(player, 12.0D, 4,
                    effect(MobEffects.WEAKNESS, 100, 1),
                    effect(MobEffects.GLOWING, 100, 0));
            case MILITECH_CANTO_ID -> applyPulse(player, 14.0D, 5,
                    effect(MobEffects.WITHER, 100, 1),
                    effect(MobEffects.WEAKNESS, 100, 1),
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            default -> {
            }
        }
    }

    private static void cleanupExpiredActiveState(Player player, PlayerCyberwareInventory inventory, long gameTime, boolean refreshAfter) {
        String activeId = ACTIVE_OPERATING_SYSTEM.get(player.getUUID());
        if (activeId == null) {
            return;
        }

        CyberwareItem operatingSystem = inventory.getInstalledCyberwareBySlotType(CyberwareSlotType.OPERATING_SYSTEM);
        if (operatingSystem == null || !activeId.equals(operatingSystem.getDefinition().id())) {
            clearActiveState(player, false, refreshAfter);
            return;
        }

        long activeUntil = ACTIVE_UNTIL_TICK.getOrDefault(player.getUUID(), 0L);
        if (activeUntil <= gameTime) {
            clearActiveState(player, true, refreshAfter);
            return;
        }

        runActiveTick(player, activeId, gameTime);
    }

    private static void runActiveTick(Player player, String activeId, long gameTime) {
        if (gameTime % 20L != 0L) {
            return;
        }

        switch (activeId) {
            case ARASAKA_SHADOW_ID -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, false));
                softenShadowAggro(player);
                if (gameTime % 40L == 0L) {
                    applyPulse(player, 8.0D, 2,
                            effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 0),
                            effect(MobEffects.WEAKNESS, 60, 0));
                }
            }
            case BIOTECH_SIGMA_ID -> applyPulse(player, 8.0D, 3,
                    effect(MobEffects.POISON, 60, 0),
                    effect(MobEffects.WEAKNESS, 60, 0));
            case MILITECH_PARALINE_ID -> applyPulse(player, 10.0D, 3,
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 1),
                    effect(MobEffects.GLOWING, 60, 0));
            case NETWATCH_NETDRIVER_ID -> applyPulse(player, 14.0D, 4,
                    effect(MobEffects.WEAKNESS, 60, 0),
                    effect(MobEffects.GLOWING, 60, 0));
            case RAVEN_MICROCYBER_ID -> applyPulse(player, 12.0D, 4,
                    effect(MobEffects.POISON, 60, 0),
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 0),
                    effect(MobEffects.GLOWING, 60, 0));
            case TETRATRONIC_RIPPLER_ID -> applyPulse(player, 12.0D, 4,
                    effect(MobEffects.WEAKNESS, 60, 1),
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 0),
                    effect(MobEffects.GLOWING, 60, 0));
            case MILITECH_CANTO_ID -> applyPulse(player, 14.0D, 5,
                    effect(MobEffects.WITHER, 60, 1),
                    effect(MobEffects.WEAKNESS, 60, 1),
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            default -> {
            }
        }
    }

    private static void clearActiveState(Player player, boolean naturalExpiry, boolean refreshAfter) {
        UUID playerId = player.getUUID();
        String activeId = ACTIVE_OPERATING_SYSTEM.remove(playerId);
        int kills = ACTIVE_KILL_COUNTS.getOrDefault(playerId, 0);
        ACTIVE_UNTIL_TICK.remove(playerId);
        ACTIVE_DURATION_SECONDS.remove(playerId);
        ACTIVE_KILL_COUNTS.remove(playerId);
        if (SandevistanProfile.supports(activeId)) SandevistanManager.stop(player, false);

        if (ARASAKA_SHADOW_ID.equals(activeId)) {
            player.removeEffect(MobEffects.INVISIBILITY);
        }

        if (naturalExpiry && activeId != null) {
            handleActiveExpiry(player, activeId, kills);
        }

        if (refreshAfter) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    private static void handleActiveExpiry(Player player, String activeId, int kills) {
        if (kills <= 0) {
            return;
        }

        switch (activeId) {
            case BIODYN_BERSERK_ID -> player.heal(Math.min(8.0F, kills * 2.0F));
            case MILITECH_BERSERK_ID -> {
                player.heal(Math.min(6.0F, kills * 1.5F));
                TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.bonusAbsorption(4.0D), 80L);
            }
            case MOORE_TECH_BERSERK_ID -> {
                player.heal(Math.min(10.0F, kills * 2.5F));
                TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.healthRegen(1.0D), 100L);
            }
            case ZETATECH_BERSERK_ID -> {
                player.heal(Math.min(6.0F, kills * 1.5F));
                TemporaryCyberwareEffectManager.addEffect(player, CyberwareEffect.moveSpeed(0.10D), 80L);
            }
            default -> {
            }
        }
    }

    private static boolean isCurrentlyActive(Player player, String activeId, long gameTime) {
        return activeId.equals(ACTIVE_OPERATING_SYSTEM.get(player.getUUID()))
                && ACTIVE_UNTIL_TICK.getOrDefault(player.getUUID(), 0L) > gameTime;
    }

    private static void applyPulse(Player player, double radius, int maxTargets, PulseTemplate... effects) {
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && isPulseTarget(entity)));
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));
        int highlightTicks = findGlowDurationTicks(effects);
        List<UUID> highlightedEntities = highlightTicks > 0 ? new ArrayList<>() : null;

        int applied = 0;
        for (LivingEntity target : targets) {
            for (PulseTemplate effect : effects) {
                if (effect.effect().is(MobEffects.GLOWING)) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(effect.effect(), effect.durationTicks(), effect.amplifier(), true, true, true));
            }
            if (highlightedEntities != null) {
                highlightedEntities.add(target.getUUID());
            }
            applied++;
            if (applied >= maxTargets) {
                break;
            }
        }

        if (highlightedEntities != null && !highlightedEntities.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new FaceHazardHighlightPayload(List.of(), highlightedEntities, highlightTicks));
        }
    }

    private static int findGlowDurationTicks(PulseTemplate... effects) {
        int highlightTicks = 0;
        for (PulseTemplate effect : effects) {
            if (effect.effect().is(MobEffects.GLOWING)) {
                highlightTicks = Math.max(highlightTicks, effect.durationTicks());
            }
        }
        return highlightTicks;
    }

    private static boolean isPulseTarget(LivingEntity entity) {
        if (entity instanceof Player targetPlayer) {
            return !targetPlayer.isCreative() && !targetPlayer.isSpectator();
        }
        return entity instanceof Monster;
    }

    private static void softenShadowAggro(Player player) {
        AggroControlHelper.clearNearbyAggro(
                player,
                ARASAKA_SHADOW_AGGRO_RADIUS,
                mob -> (mob instanceof Enemy || AggroControlHelper.hasDirectAggro(mob, player))
                        && (AggroControlHelper.hasDirectAggro(mob, player)
                        || mob.hasLineOfSight(player) && mob.distanceToSqr(player) > ARASAKA_SHADOW_SAFE_BUBBLE_SQR)
        );
    }

    private static boolean isReady(Map<UUID, Long> cooldowns, Player player, long gameTime) {
        return cooldowns.getOrDefault(player.getUUID(), 0L) <= gameTime;
    }

    private static void setTriggeredCooldown(
            Player player,
            PlayerCyberwareInventory inventory,
            Map<UUID, Long> cooldownUntilMap,
            Map<UUID, Integer> cooldownTotalSecondsMap,
            int baseSeconds
    ) {
        long adjustedCooldown = FrontalCortexManager.adjustCyberwareCooldownTicks(player, inventory, baseSeconds * 20L, true);
        cooldownUntilMap.put(player.getUUID(), player.level().getGameTime() + adjustedCooldown);
        cooldownTotalSecondsMap.put(player.getUUID(), (int) Math.max(1L, (adjustedCooldown + 19L) / 20L));
    }

    private static int getRemainingSeconds(Map<UUID, Long> cooldowns, Player player) {
        long remainingTicks = Math.max(0L, cooldowns.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
        return (int) ((remainingTicks + 19L) / 20L);
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

    private static void merge(EnumMap<CyberwareEffectType, Double> totals, CyberwareEffectType type, double amount) {
        totals.merge(type, amount, type.isMobEffect() ? Math::max : Double::sum);
    }

    private static OperatingSystemSpec getOperatingSystemSpec(CyberwareItem operatingSystem) {
        return OPERATING_SYSTEMS.get(operatingSystem.getDefinition().id());
    }

    private static PulseTemplate effect(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        return new PulseTemplate(effect, durationTicks, amplifier);
    }

    private static void notify(Player player, String translationKey, Object... args) {
        player.displayClientMessage(Component.translatable(translationKey, args).withStyle(ChatFormatting.AQUA), true);
    }

    private static double resolvePsychosisActivationLoad(OperatingSystemSpec spec) {
        return switch (spec.family()) {
            case BERSERK -> 13.0D;
            case SANDEVISTAN -> 12.0D;
            case CYBERDECK -> 10.0D;
            case NONE -> 0.0D;
        };
    }

    private static Map<String, OperatingSystemSpec> createOperatingSystems() {
        Map<String, OperatingSystemSpec> systems = new LinkedHashMap<>();
        register(systems, BIODYN_BERSERK_ID, OperatingSystemFamily.BERSERK, "hud.cyberneticenhancements.ability.biodyn_berserk", 10, 34);
        register(systems, MILITECH_BERSERK_ID, OperatingSystemFamily.BERSERK, "hud.cyberneticenhancements.ability.militech_berserk", 10, 36);
        register(systems, MOORE_TECH_BERSERK_ID, OperatingSystemFamily.BERSERK, "hud.cyberneticenhancements.ability.moore_tech_berserk", 12, 35);
        register(systems, ZETATECH_BERSERK_ID, OperatingSystemFamily.BERSERK, "hud.cyberneticenhancements.ability.zetatech_berserk", 10, 33);
        register(systems, ARASAKA_SHADOW_ID, OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.arasaka_shadow", 8, 30);
        register(systems, BIOTECH_SIGMA_ID, OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.biotech_sigma", 9, 32);
        register(systems, MILITECH_PARALINE_ID, OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.militech_paraline", 10, 34);
        register(systems, NETWATCH_NETDRIVER_ID, OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.netwatch_netdriver", 10, 36);
        register(systems, RAVEN_MICROCYBER_ID, OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.raven_microcyber", 10, 38);
        register(systems, TETRATRONIC_RIPPLER_ID, OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.tetratronic_rippler", 10, 38);
        register(systems, MILITECH_CANTO_ID, OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.militech_canto", 12, 45);
        register(systems, DYNALAR_SANDEVISTAN_ID, OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.dynalar_sandevistan", 8, 28);
        register(systems, ZETATECH_SANDEVISTAN_ID, OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.zetatech_sandevistan", 8, 28);
        register(systems, MILITECH_FALCON_ID, OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.militech_falcon", 10, 30);
        register(systems, QIANT_WARP_DANCER_ID, OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.qiant_warp_dancer", 8, 32);
        register(systems, MILITECH_APOGEE_ID, OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.militech_apogee", 12, 34);
        register(systems, SandevistanProfile.EXPERIMENTAL, OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.militech_experimental_sandevistan", 8, 30);
        register(systems, CHROME_COMPRESSOR_ID, OperatingSystemFamily.NONE, "", 0, 0);
        return systems;
    }

    private static void register(Map<String, OperatingSystemSpec> systems, String id, OperatingSystemFamily family, String hudKey, int durationSeconds, int cooldownSeconds) {
        CyberwareBalance.OperatingSystemEntry balance = CyberwareBalance.resolveOperatingSystem(id);
        systems.put(id, new OperatingSystemSpec(id, balance.family(), balance.hudKey(), balance.durationSeconds(), balance.cooldownSeconds()));
    }

    private record OperatingSystemSpec(
            String id,
            OperatingSystemFamily family,
            String hudKey,
            int durationSeconds,
            int cooldownSeconds
    ) {
        boolean hasActiveAbility() {
            return durationSeconds > 0 && cooldownSeconds > 0;
        }

        String itemTranslationKey() {
            return "item.cyberneticenhancements." + id;
        }
    }

    private record PulseTemplate(Holder<MobEffect> effect, int durationTicks, int amplifier) {
    }
}
