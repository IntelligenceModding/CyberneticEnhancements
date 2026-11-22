package de.artemis.cyberneticenhancements.common.cyberware;

import static de.artemis.cyberneticenhancements.common.util.PersistentDataHelper.copyDouble;
import static de.artemis.cyberneticenhancements.common.util.PersistentDataHelper.copyFloat;
import static de.artemis.cyberneticenhancements.common.util.PersistentDataHelper.copyInt;
import static de.artemis.cyberneticenhancements.common.util.PersistentDataHelper.copyLong;
import static de.artemis.cyberneticenhancements.common.util.PersistentDataHelper.copyString;

import de.artemis.cyberneticenhancements.common.network.CyberpsychosisControlPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;

public final class CyberstrainManager {
    private static final String SUPPRESSION_VALUE_KEY = "cyberneticenhancements.cyberstrain_suppression";
    private static final String SUPPRESSION_UNTIL_KEY = "cyberneticenhancements.cyberstrain_suppression_until";
    private static final String SUPPRESSION_DURATION_KEY = "cyberneticenhancements.cyberstrain_suppression_duration";
    private static final String RAM_JOLT_UNTIL_KEY = "cyberneticenhancements.ram_jolt_until";
    private static final String RAM_JOLT_MULTIPLIER_KEY = "cyberneticenhancements.ram_jolt_multiplier";
    private static final String RAM_JOLT_DURATION_KEY = "cyberneticenhancements.ram_jolt_duration";
    private static final String PSYCHOSIS_UNTIL_KEY = "cyberneticenhancements.psychosis_until";
    private static final String PSYCHOSIS_COOLDOWN_UNTIL_KEY = "cyberneticenhancements.psychosis_cooldown_until";
    private static final String PSYCHOSIS_DURATION_KEY = "cyberneticenhancements.psychosis_duration";
    private static final String PSYCHOSIS_LAST_ATTACK_KEY = "cyberneticenhancements.psychosis_last_attack";
    private static final String PSYCHOSIS_WANDER_YAW_KEY = "cyberneticenhancements.psychosis_wander_yaw";
    private static final String PSYCHOSIS_TIER_KEY = "cyberneticenhancements.psychosis_tier";
    private static final String PSYCHOSIS_AFTERSHOCK_UNTIL_KEY = "cyberneticenhancements.psychosis_aftershock_until";
    private static final String PSYCHOSIS_AFTERSHOCK_DURATION_KEY = "cyberneticenhancements.psychosis_aftershock_duration";
    private static final String PSYCHOSIS_INSTABILITY_KEY = "cyberneticenhancements.psychosis_instability";

    private static final int PSYCHOSIS_THRESHOLD = CyberwareBalance.intValue("cyberstrain.threshold.psychosis");

    private static final double MAX_INSTABILITY = 100.0D;
    private static final double MINOR_TRIGGER_INSTABILITY = 55.0D;
    private static final double MAJOR_TRIGGER_INSTABILITY = 85.0D;
    private static final long BASE_EPISODE_COOLDOWN_TICKS = 45L * 20L;
    private static final long MINOR_STABILIZE_REDUCTION_TICKS = 120L;
    private static final long MAJOR_STABILIZE_REDUCTION_TICKS = 220L;
    private CyberstrainManager() {
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide() || !player.isAlive()) {
            return;
        }

        long gameTime = player.level().getGameTime();
        cleanupExpiredState(player, gameTime);
        if (player instanceof ServerPlayer server && PsychosisRecovery.tick(server)) return;

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int effectiveCyberstrain = getEffectiveCyberstrain(player, inventory, gameTime);

        if (isPsychosisActive(player, gameTime)) {
            runPsychosisEpisode(player, gameTime);
        } else {
            passivelyBuildInstability(player, effectiveCyberstrain);
            maybeStartPsychosisEpisode(player, effectiveCyberstrain, gameTime);
        }

        decayInstability(player, effectiveCyberstrain, gameTime);
        if (player instanceof ServerPlayer serverPlayer) {
            PsychosisBossBarManager.update(serverPlayer);
        }
    }

    public static boolean ignoresHunger(Player player) {
        return player.isAlive() && (isPsychosisActive(player) || PsychosisRecovery.pending(player));
    }

    public static void applyPenalties(Player player, PlayerCyberwareInventory inventory, EnumMap<CyberwareEffectType, Double> totals) {
        long gameTime = player.level().getGameTime();
        if (isAftershockActive(player, gameTime)) {
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, -0.05D);
            merge(totals, CyberwareEffectType.ATTACK_SPEED, -0.05D);
            merge(totals, CyberwareEffectType.BLOCK_BREAK_SPEED, -0.05D);
        }

        EpisodeTier tier = getEpisodeTier(player, gameTime);
        if (tier == EpisodeTier.MINOR) {
            merge(totals, CyberwareEffectType.MAX_HEALTH, 10.0D);
            merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.20D);
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.08D);
            merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 5.0D);
            merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.20D);
            merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.35D);
            merge(totals, CyberwareEffectType.STEP_HEIGHT, CyberwareBalance.doubleValue("cyberstrain.psychosis.minor.step_height_bonus"));
        } else if (tier == EpisodeTier.MAJOR) {
            merge(totals, CyberwareEffectType.MAX_HEALTH, 20.0D);
            merge(totals, CyberwareEffectType.ATTACK_SPEED, 0.40D);
            merge(totals, CyberwareEffectType.MOVEMENT_SPEED, 0.18D);
            merge(totals, CyberwareEffectType.ATTACK_DAMAGE, 9.0D);
            merge(totals, CyberwareEffectType.DAMAGE_REDUCTION, 0.35D);
            merge(totals, CyberwareEffectType.KNOCKBACK_RESISTANCE, 0.65D);
            merge(totals, CyberwareEffectType.STEP_HEIGHT, CyberwareBalance.doubleValue("cyberstrain.psychosis.major.step_height_bonus"));
            merge(totals, CyberwareEffectType.FIRE_RESISTANCE, 1.0D);
            merge(totals, CyberwareEffectType.WATER_BREATHING, 1.0D);
            merge(totals, CyberwareEffectType.DOLPHINS_GRACE, 1.0D);
        }
    }

    public static double getHealingMultiplier(Player player) {
        long gameTime = player.level().getGameTime();
        EpisodeTier tier = getEpisodeTier(player, gameTime);
        if (tier == EpisodeTier.MAJOR) {
            return 0.20D;
        }
        if (tier == EpisodeTier.MINOR) {
            return 0.35D;
        }
        if (isAftershockActive(player, gameTime)) {
            return 0.65D;
        }

        return 1.0D;
    }

    public static double getAbilityCooldownMultiplier(Player player, PlayerCyberwareInventory inventory) {
        long gameTime = player.level().getGameTime();
        EpisodeTier tier = getEpisodeTier(player, gameTime);

        double multiplier = 1.0D;
        if (tier == EpisodeTier.MAJOR) {
            multiplier *= 2.30D;
        } else if (tier == EpisodeTier.MINOR) {
            multiplier *= 1.85D;
        } else if (isAftershockActive(player, gameTime)) {
            multiplier *= 1.30D;
        }

        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(RAM_JOLT_UNTIL_KEY) && persistentData.getLong(RAM_JOLT_UNTIL_KEY) > gameTime) {
            multiplier *= persistentData.getDouble(RAM_JOLT_MULTIPLIER_KEY);
        }

        multiplier *= CombatStatusManager.getAbilityCooldownMultiplier(player);
        return Math.max(0.25D, multiplier);
    }

    public static int getEffectiveCyberstrain(Player player, PlayerCyberwareInventory inventory) {
        return getEffectiveCyberstrain(player, inventory, player.level().getGameTime());
    }

    public static int getEffectiveCyberstrain(Player player, PlayerCyberwareInventory inventory, long gameTime) {
        cleanupExpiredState(player, gameTime);
        return Math.max(0, inventory.getCyberstrain() + FrontalCortexManager.getAdditionalCyberstrain(inventory) - getSuppressionAmount(player, gameTime));
    }

    public static int getSuppressionAmount(Player player) {
        return getSuppressionAmount(player, player.level().getGameTime());
    }

    public static int getSuppressionSecondsRemaining(Player player) {
        long remainingTicks = Math.max(0L, player.getPersistentData().getLong(SUPPRESSION_UNTIL_KEY) - player.level().getGameTime());
        return (int) ((remainingTicks + 19L) / 20L);
    }

    public static void applySuppressionDose(Player player, int suppressionAmount, long durationTicks, boolean hallucinationSideEffect) {
        CompoundTag persistentData = player.getPersistentData();
        long until = player.level().getGameTime() + durationTicks;
        persistentData.putInt(SUPPRESSION_VALUE_KEY, Math.max(suppressionAmount, persistentData.getInt(SUPPRESSION_VALUE_KEY)));
        persistentData.putLong(SUPPRESSION_UNTIL_KEY, Math.max(until, persistentData.getLong(SUPPRESSION_UNTIL_KEY)));
        persistentData.putLong(SUPPRESSION_DURATION_KEY, Math.max(durationTicks, persistentData.getLong(SUPPRESSION_DURATION_KEY)));

        reduceInstability(player, hallucinationSideEffect ? 24.0D : 12.0D);
        boolean hadEpisode = persistentData.contains(PSYCHOSIS_UNTIL_KEY);
        if (player instanceof ServerPlayer serverPlayer && isPsychosisActive(player)) {
            stabilizePsychosis(serverPlayer, hallucinationSideEffect ? MAJOR_STABILIZE_REDUCTION_TICKS : MINOR_STABILIZE_REDUCTION_TICKS, hallucinationSideEffect);
        }
        boolean episodeEnded = hadEpisode && !persistentData.contains(PSYCHOSIS_UNTIL_KEY);

        if (hallucinationSideEffect) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, true, false, false));
            if (!episodeEnded) player.displayClientMessage(Component.translatable("message.cyberneticenhancements.consumable.immunoblockers"), true);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false, false));
            if (!episodeEnded) player.displayClientMessage(Component.translatable("message.cyberneticenhancements.consumable.chrome_suppressant"), true);
        }
    }

    public static void applyRamJolt(Player player, long durationTicks, double cooldownMultiplier) {
        CompoundTag persistentData = player.getPersistentData();
        long until = player.level().getGameTime() + durationTicks;
        persistentData.putLong(RAM_JOLT_UNTIL_KEY, Math.max(until, persistentData.getLong(RAM_JOLT_UNTIL_KEY)));
        persistentData.putDouble(RAM_JOLT_MULTIPLIER_KEY, Math.min(cooldownMultiplier, persistentData.contains(RAM_JOLT_MULTIPLIER_KEY) ? persistentData.getDouble(RAM_JOLT_MULTIPLIER_KEY) : 1.0D));
        persistentData.putLong(RAM_JOLT_DURATION_KEY, Math.max(durationTicks, persistentData.getLong(RAM_JOLT_DURATION_KEY)));
        reduceInstability(player, 4.0D);
        player.displayClientMessage(Component.translatable("message.cyberneticenhancements.consumable.ram_jolt"), true);
    }

    public static int getRamJoltSecondsRemaining(Player player) {
        long remainingTicks = Math.max(0L, player.getPersistentData().getLong(RAM_JOLT_UNTIL_KEY) - player.level().getGameTime());
        return (int) ((remainingTicks + 19L) / 20L);
    }

    public static double getRamJoltCooldownMultiplier(Player player) {
        return getRamJoltSecondsRemaining(player) > 0 ? player.getPersistentData().getDouble(RAM_JOLT_MULTIPLIER_KEY) : 1.0D;
    }

    public static int getSuppressionTotalSeconds(Player player) {
        long totalTicks = player.getPersistentData().getLong(SUPPRESSION_DURATION_KEY);
        return totalTicks <= 0L ? 0 : (int) Math.max(1L, (totalTicks + 19L) / 20L);
    }

    public static int getRamJoltTotalSeconds(Player player) {
        long totalTicks = player.getPersistentData().getLong(RAM_JOLT_DURATION_KEY);
        return totalTicks <= 0L ? 0 : (int) Math.max(1L, (totalTicks + 19L) / 20L);
    }

    public static void forcePsychosisEpisode(ServerPlayer player, int durationSeconds) {
        startPsychosisEpisode(player, Math.max(5, durationSeconds) * 20L, true, EpisodeTier.MAJOR);
    }

    public static void clearPsychosis(ServerPlayer player) {
        PsychosisRecovery.cancel(player);
        PsychosisIntro.cancel(player);
        clearEpisodeBlindness(player);
        clearPsychosisState(player.getPersistentData(), true);
        PsychosisPursuitController.clear(player);
        PsychosisBossBarManager.update(player);
        syncControlLock(player);
        CyberwareEffects.refreshPlayerCyberware(player);
    }

    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PsychosisRecovery.cancel(player);
            PsychosisIntro.cancel(player);
            clearEpisodeBlindness(player);
            clearPsychosisState(player.getPersistentData(), true);
            PsychosisPursuitController.clear(player);
            PsychosisBossBarManager.update(player);
            syncControlLock(player);
        }
    }

    public static void clearTemporaryStatuses(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.remove(SUPPRESSION_VALUE_KEY);
        persistentData.remove(SUPPRESSION_UNTIL_KEY);
        persistentData.remove(SUPPRESSION_DURATION_KEY);
        persistentData.remove(RAM_JOLT_UNTIL_KEY);
        persistentData.remove(RAM_JOLT_MULTIPLIER_KEY);
        persistentData.remove(RAM_JOLT_DURATION_KEY);
        persistentData.remove(PSYCHOSIS_AFTERSHOCK_UNTIL_KEY);
        persistentData.remove(PSYCHOSIS_AFTERSHOCK_DURATION_KEY);
        persistentData.remove(PSYCHOSIS_INSTABILITY_KEY);
        clearPsychosis(player);
    }

    public static void purgeNegativeStatuses(Player player, boolean clearPsychosis) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.remove(SUPPRESSION_VALUE_KEY);
        persistentData.remove(SUPPRESSION_UNTIL_KEY);
        persistentData.remove(SUPPRESSION_DURATION_KEY);
        persistentData.remove(RAM_JOLT_UNTIL_KEY);
        persistentData.remove(RAM_JOLT_MULTIPLIER_KEY);
        persistentData.remove(RAM_JOLT_DURATION_KEY);
        if (clearPsychosis && player instanceof ServerPlayer serverPlayer) {
            clearPsychosis(serverPlayer);
        }
    }

    public static boolean isPsychosisActive(Player player) {
        return isPsychosisActive(player, player.level().getGameTime());
    }

    public static boolean isPsychosisActive(Player player, long gameTime) {
        cleanupExpiredState(player, gameTime);
        return player.getPersistentData().getLong(PSYCHOSIS_UNTIL_KEY) > gameTime;
    }

    public static long getPsychosisSecondsRemaining(Player player) {
        long remainingTicks = Math.max(0L, player.getPersistentData().getLong(PSYCHOSIS_UNTIL_KEY) - player.level().getGameTime());
        return (remainingTicks + 19L) / 20L;
    }

    public static long getPsychosisRemainingTicks(Player player) {
        return Math.max(0L, player.getPersistentData().getLong(PSYCHOSIS_UNTIL_KEY) - player.level().getGameTime());
    }

    public static long getPsychosisTotalTicks(Player player) {
        return Math.max(0L, player.getPersistentData().getLong(PSYCHOSIS_DURATION_KEY));
    }

    public static String getPsychosisTierName(Player player) {
        return readStoredEpisodeTier(player.getPersistentData()).name();
    }

    public static String getPsychosisStateLabel(Player player, PlayerCyberwareInventory inventory) {
        long gameTime = player.level().getGameTime();
        EpisodeTier tier = getEpisodeTier(player, gameTime);
        if (tier != EpisodeTier.NONE) {
            return "PSYCHOTIC";
        }
        if (isAftershockActive(player, gameTime)) {
            return "AFTERSHOCK";
        }

        int effectiveCyberstrain = getEffectiveCyberstrain(player, inventory, gameTime);
        if (effectiveCyberstrain >= PSYCHOSIS_THRESHOLD) {
            return "EPISODE_RISK";
        }
        return "STABLE";
    }

    public static void syncControlLock(ServerPlayer player) {
        EpisodeTier tier = getEpisodeTier(player, player.level().getGameTime());
        PacketDistributor.sendToPlayer(player, (tier.locksControl() || PsychosisIntro.active(player) || PsychosisRecovery.active(player))
                ? new CyberpsychosisControlPayload(true, player.getYRot(), player.getXRot(), 0.0F, 0.0F, false, !PsychosisIntro.active(player))
                : new CyberpsychosisControlPayload(false, 0.0F, 0.0F, 0.0F, 0.0F, false, false));
    }

    private static void syncControlLock(ServerPlayer player, PsychosisPursuitController.PursuitIntent intent) {
        PacketDistributor.sendToPlayer(player, new CyberpsychosisControlPayload(
                true,
                intent.yaw(),
                intent.pitch(),
                intent.forward(),
                intent.strafe(),
                intent.jump(),
                intent.sprint()
        ));
    }

    public static void copyState(Player fromPlayer, Player toPlayer) {
        CompoundTag fromData = fromPlayer.getPersistentData();
        CompoundTag toData = toPlayer.getPersistentData();
        copyLong(fromData, toData, SUPPRESSION_UNTIL_KEY);
        copyLong(fromData, toData, SUPPRESSION_DURATION_KEY);
        copyInt(fromData, toData, SUPPRESSION_VALUE_KEY);
        copyLong(fromData, toData, RAM_JOLT_UNTIL_KEY);
        copyLong(fromData, toData, RAM_JOLT_DURATION_KEY);
        copyDouble(fromData, toData, RAM_JOLT_MULTIPLIER_KEY);
        copyLong(fromData, toData, PSYCHOSIS_UNTIL_KEY);
        copyLong(fromData, toData, PSYCHOSIS_COOLDOWN_UNTIL_KEY);
        copyLong(fromData, toData, PSYCHOSIS_DURATION_KEY);
        copyLong(fromData, toData, PSYCHOSIS_LAST_ATTACK_KEY);
        copyFloat(fromData, toData, PSYCHOSIS_WANDER_YAW_KEY);
        copyString(fromData, toData, PSYCHOSIS_TIER_KEY);
        copyLong(fromData,toData,PsychosisRecovery.START);copyFloat(fromData,toData,PsychosisRecovery.YAW);copyFloat(fromData,toData,PsychosisRecovery.PITCH);
        copyLong(fromData,toData,PsychosisIntro.READY);copyLong(fromData,toData,PsychosisIntro.START);copyFloat(fromData,toData,PsychosisIntro.YAW);copyFloat(fromData,toData,PsychosisIntro.PITCH);
        copyLong(fromData, toData, PSYCHOSIS_AFTERSHOCK_UNTIL_KEY);
        copyLong(fromData, toData, PSYCHOSIS_AFTERSHOCK_DURATION_KEY);
        copyDouble(fromData, toData, PSYCHOSIS_INSTABILITY_KEY);
    }

    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.level().isClientSide()) {
            return;
        }

        double multiplier = CombatStatusManager.getHealingMultiplier(livingEntity);
        if (livingEntity instanceof Player player) {
            multiplier *= getHealingMultiplier(player);
        }
        if (multiplier < 0.999D) {
            event.setAmount((float) (event.getAmount() * multiplier));
        }
    }

    public static void onIncomingDamage(Player player, float amount, LivingEntity attacker) {
        if (player.level().isClientSide() || amount <= 0.0F) {
            return;
        }

        double instability = amount * 1.8D;
        if (attacker instanceof Monster || attacker instanceof Player) {
            instability += 6.0D;
        } else if (attacker != null) {
            instability += 2.0D;
        }
        if (player.getHealth() / Math.max(1.0F, player.getMaxHealth()) <= 0.35F) {
            instability += 8.0D;
        }
        addInstability(player, instability);
    }

    public static void onKill(Player player, LivingEntity target) {
        if (player.level().isClientSide() || target == null) {
            return;
        }

        if (target instanceof Player) {
            addInstability(player, 16.0D);
        } else if (target instanceof Animal) {
            addInstability(player, 10.0D);
        } else if (target instanceof Monster) {
            addInstability(player, 6.0D);
        } else {
            addInstability(player, 4.0D);
        }
    }

    public static void onCyberwareActivated(Player player, double instability) {
        if (player.level().isClientSide() || instability <= 0.0D) {
            return;
        }
        addInstability(player, instability);
    }

    public static void addInstability(Player player, double amount) {
        addInstability(player, amount, false);
    }

    private static void addInstability(Player player, double amount, boolean fromStrain) {
        if (amount <= 0.0D) {
            return;
        }
        amount = de.artemis.cyberneticenhancements.common.skills.SkillEffects.instability(player, amount, fromStrain);
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putDouble(PSYCHOSIS_INSTABILITY_KEY, Mth.clamp(persistentData.getDouble(PSYCHOSIS_INSTABILITY_KEY) + amount, 0.0D, MAX_INSTABILITY));
    }

    public static double getInstability(Player player) {
        return player.getPersistentData().getDouble(PSYCHOSIS_INSTABILITY_KEY);
    }

    public static void reduceInstability(Player player, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        CompoundTag persistentData = player.getPersistentData();
        double reduced = Math.max(0.0D, persistentData.getDouble(PSYCHOSIS_INSTABILITY_KEY) - amount);
        if (reduced <= 0.0001D) {
            persistentData.remove(PSYCHOSIS_INSTABILITY_KEY);
        } else {
            persistentData.putDouble(PSYCHOSIS_INSTABILITY_KEY, reduced);
        }
    }

    private static void passivelyBuildInstability(Player player, int effectiveCyberstrain) {
        if (effectiveCyberstrain >= PSYCHOSIS_THRESHOLD) {
            addInstability(player, 0.05D + Math.max(0, effectiveCyberstrain - PSYCHOSIS_THRESHOLD) * 0.010D, true);
        }
    }

    private static void decayInstability(Player player, int effectiveCyberstrain, long gameTime) {
        double decayPerTick;
        if (isPsychosisActive(player, gameTime)) {
            decayPerTick = 0.02D;
        } else if (isAftershockActive(player, gameTime)) {
            decayPerTick = 0.08D;
        } else if (effectiveCyberstrain >= PSYCHOSIS_THRESHOLD) {
            decayPerTick = 0.01D;
        } else {
            decayPerTick = 0.20D;
        }
        reduceInstability(player, decayPerTick);
    }

    private static void maybeStartPsychosisEpisode(Player player, int effectiveCyberstrain, long gameTime) {
        if (!(player instanceof ServerPlayer serverPlayer) || effectiveCyberstrain < PSYCHOSIS_THRESHOLD) {
            return;
        }
        if (player.getPersistentData().getLong(PSYCHOSIS_COOLDOWN_UNTIL_KEY) > gameTime || gameTime % 20L != 0L) {
            return;
        }

        double instability = player.getPersistentData().getDouble(PSYCHOSIS_INSTABILITY_KEY);
        if (instability < MINOR_TRIGGER_INSTABILITY) {
            return;
        }

        float healthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        boolean forceMajor = instability >= MAJOR_TRIGGER_INSTABILITY || (healthRatio <= 0.30F && instability >= 65.0D);
        double pressure = Math.max(0.0D, effectiveCyberstrain - PSYCHOSIS_THRESHOLD);
        double healthPressure = Math.max(0.0D, 0.55D - healthRatio);
        double triggerChance = Mth.clamp(
                (instability - MINOR_TRIGGER_INSTABILITY) / 55.0D
                        + pressure * 0.015D
                        + healthPressure * 0.8D,
                0.08D,
                forceMajor ? 1.0D : 0.75D
        );
        if (!forceMajor && player.getRandom().nextDouble() >= triggerChance) {
            return;
        }

        EpisodeTier tier = forceMajor ? EpisodeTier.MAJOR : EpisodeTier.MINOR;
        int durationSeconds = tier == EpisodeTier.MAJOR
                ? Mth.clamp(8 + (int) Math.round(pressure / 3.0D) + (int) Math.round(instability / 18.0D), 8, 16)
                : Mth.clamp(5 + (int) Math.round(pressure / 4.0D) + (int) Math.round(instability / 30.0D), 5, 11);
        startPsychosisEpisode(serverPlayer, durationSeconds * 20L, false, tier);
    }

    private static void startPsychosisEpisode(ServerPlayer player, long durationTicks, boolean forced, EpisodeTier tier) {
        PsychosisRecovery.cancel(player);
        long gameTime = player.level().getGameTime();
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putLong(PSYCHOSIS_UNTIL_KEY, gameTime + durationTicks + PsychosisIntroMotion.DURATION);
        persistentData.putLong(PSYCHOSIS_COOLDOWN_UNTIL_KEY, gameTime + durationTicks + PsychosisIntroMotion.DURATION + BASE_EPISODE_COOLDOWN_TICKS);
        persistentData.putLong(PSYCHOSIS_DURATION_KEY, durationTicks);
        persistentData.putString(PSYCHOSIS_TIER_KEY, tier.name());
        persistentData.remove(PSYCHOSIS_AFTERSHOCK_UNTIL_KEY);
        persistentData.remove(PSYCHOSIS_AFTERSHOCK_DURATION_KEY);

        PsychosisIntro.begin(player);


        player.displayClientMessage(Component.translatable(
                forced
                        ? "message.cyberneticenhancements.psychosis.forced"
                        : tier == EpisodeTier.MAJOR
                        ? "message.cyberneticenhancements.psychosis.triggered_major"
                        : "message.cyberneticenhancements.psychosis.triggered_minor"), true);
        PsychosisBossBarManager.update(player);
        syncControlLock(player);
        player.removeEffect(MobEffects.WEAKNESS);
        CyberwareEffects.refreshPlayerCyberware(player);
    }

    private static void runPsychosisEpisode(Player player, long gameTime) {
        if (player instanceof ServerPlayer server && PsychosisIntro.tick(server)) return;
        if (player.isSpectator()) {
            if (player instanceof ServerPlayer serverPlayer) {
                PsychosisPursuitController.clear(player);
                syncControlLock(serverPlayer);
            }
            return;
        }

        EpisodeTier tier = getEpisodeTier(player, gameTime);
        if (tier == EpisodeTier.NONE) {
            return;
        }

        maintainEpisodeBlindness(player);

        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, tier == EpisodeTier.MAJOR ? 60 : 40, 0, true, false, false));
        player.removeEffect(MobEffects.WEAKNESS);

        if (player instanceof ServerPlayer server && PsychosisIntro.settling(player)) {
            PsychosisIntro.hold(server);
            return;
        }
        LivingEntity target = PsychosisPursuitController.findTarget(player);
        runMajorEpisode(player, target, gameTime);
    }

    private static void runMajorEpisode(Player player, LivingEntity target, long gameTime) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PsychosisPursuitController.PursuitIntent intent = target != null
                ? PsychosisPursuitController.buildHuntIntent(player, target, gameTime)
                : PsychosisPursuitController.buildWanderIntent(player, gameTime);
        PsychosisPursuitController.applyIntent(player, intent);
        syncControlLock(serverPlayer, intent);

        if (target != null || Math.abs(intent.forward()) > 0.01F || Math.abs(intent.strafe()) > 0.01F) {
            drivePsychosisMovementByIntent(player, intent, target != null ? 0.58D : 0.42D);
        }

        if (target != null) {
            tryAttackTarget(player, target, gameTime, 10L);
        }
    }

    private static void steerLookTowardTarget(Player player, LivingEntity target) {
        Vec3 delta = target.position().subtract(player.position());
        Vec3 horizontal = flatten(delta);
        if (horizontal.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 direction = horizontal.normalize();
        double yaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0D;
        float yawFloat = (float) yaw;
        player.setYRot(yawFloat);
        player.setYHeadRot(yawFloat);
        player.setYBodyRot(yawFloat);
        player.setXRot((float) Mth.clamp(-Math.toDegrees(Math.atan2(delta.y, horizontal.length())), -45.0D, 45.0D));
    }

    private static void tryAttackTarget(Player player, LivingEntity target, long gameTime, long attackDelayTicks) {
        if (!(player instanceof ServerPlayer serverPlayer) || !PsychosisPursuitController.isValidTarget(target) || !player.hasLineOfSight(target) || player.distanceToSqr(target) > 9.0D) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        long lastAttack = persistentData.getLong(PSYCHOSIS_LAST_ATTACK_KEY);
        if (gameTime - lastAttack < attackDelayTicks) {
            return;
        }

        serverPlayer.attack(target);
        serverPlayer.swing(InteractionHand.MAIN_HAND, true);
        persistentData.putLong(PSYCHOSIS_LAST_ATTACK_KEY, gameTime);
    }

    private static void drivePsychosisMovement(Player player, Vec3 direction, double speedMultiplier, boolean forceJump) {
        Vec3 horizontal = flatten(direction);
        if (horizontal.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 normalized = horizontal.normalize();
        double moveSpeed = Math.max(0.16D, player.getAttributeValue(Attributes.MOVEMENT_SPEED) * speedMultiplier);
        Vec3 targetHorizontal = normalized.scale(moveSpeed);
        Vec3 currentMotion = player.getDeltaMovement();
        double blendedX = Mth.lerp(0.35D, currentMotion.x, targetHorizontal.x);
        double blendedZ = Mth.lerp(0.35D, currentMotion.z, targetHorizontal.z);
        boolean shouldHop = player.onGround() && (forceJump || player.horizontalCollision);
        double verticalMotion = shouldHop ? Math.max(currentMotion.y, 0.42D) : currentMotion.y;

        player.setSprinting(true);
        player.setDeltaMovement(blendedX, verticalMotion, blendedZ);
        player.fallDistance = 0.0F;
        player.hasImpulse = true;
        player.hurtMarked = true;
    }

    private static void drivePsychosisMovementByIntent(Player player, PsychosisPursuitController.PursuitIntent intent, double speedMultiplier) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, intent.yaw());
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 desired = forward.scale(intent.forward()).add(right.scale(intent.strafe()));
        if (desired.lengthSqr() < 0.0001D) {
            return;
        }

        drivePsychosisMovement(player, desired, speedMultiplier, intent.jump());
    }

    private static Vec3 flatten(Vec3 vector) {
        return new Vec3(vector.x, 0.0D, vector.z);
    }

    private static int getSuppressionAmount(Player player, long gameTime) {
        cleanupExpiredState(player, gameTime);
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(SUPPRESSION_UNTIL_KEY) && persistentData.getLong(SUPPRESSION_UNTIL_KEY) > gameTime) {
            return persistentData.getInt(SUPPRESSION_VALUE_KEY);
        }
        return 0;
    }

    private static void stabilizePsychosis(ServerPlayer player, long reductionTicks, boolean hardStabilizer) {
        long gameTime = player.level().getGameTime();
        EpisodeTier tier = getEpisodeTier(player, gameTime);
        if (tier == EpisodeTier.NONE) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        long remaining = Math.max(0L, persistentData.getLong(PSYCHOSIS_UNTIL_KEY) - gameTime);
        if (tier == EpisodeTier.MINOR && hardStabilizer) {
            finishPsychosisEpisode(player, true);
            return;
        }

        long reducedRemaining = Math.max(0L, remaining - reductionTicks);
        if (reducedRemaining <= 0L) {
            finishPsychosisEpisode(player, true);
            return;
        }

        if (tier == EpisodeTier.MAJOR && hardStabilizer && reducedRemaining <= 5L * 20L) {
            persistentData.putLong(PSYCHOSIS_UNTIL_KEY, gameTime + 5L * 20L);
            persistentData.putString(PSYCHOSIS_TIER_KEY, EpisodeTier.MINOR.name());
        } else {
            persistentData.putLong(PSYCHOSIS_UNTIL_KEY, gameTime + reducedRemaining);
        }
        syncControlLock(player);
    }

    private static void cleanupExpiredState(Player player, long gameTime) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(SUPPRESSION_UNTIL_KEY) && persistentData.getLong(SUPPRESSION_UNTIL_KEY) <= gameTime) {
            persistentData.remove(SUPPRESSION_UNTIL_KEY);
            persistentData.remove(SUPPRESSION_VALUE_KEY);
            persistentData.remove(SUPPRESSION_DURATION_KEY);
        }
        if (persistentData.contains(RAM_JOLT_UNTIL_KEY) && persistentData.getLong(RAM_JOLT_UNTIL_KEY) <= gameTime) {
            persistentData.remove(RAM_JOLT_UNTIL_KEY);
            persistentData.remove(RAM_JOLT_MULTIPLIER_KEY);
            persistentData.remove(RAM_JOLT_DURATION_KEY);
        }
        if (persistentData.contains(PSYCHOSIS_AFTERSHOCK_UNTIL_KEY) && persistentData.getLong(PSYCHOSIS_AFTERSHOCK_UNTIL_KEY) <= gameTime) {
            persistentData.remove(PSYCHOSIS_AFTERSHOCK_UNTIL_KEY);
            persistentData.remove(PSYCHOSIS_AFTERSHOCK_DURATION_KEY);
        }
        if (persistentData.contains(PSYCHOSIS_UNTIL_KEY) && persistentData.getLong(PSYCHOSIS_UNTIL_KEY) <= gameTime) {
            if (player instanceof ServerPlayer serverPlayer) {
                finishPsychosisEpisode(serverPlayer, false);
            } else {
                clearPsychosisState(persistentData, false);
            }
        }
    }

    public static boolean endPsychosisEpisode(ServerPlayer player) {
        boolean hadEpisode = player.getPersistentData().contains(PSYCHOSIS_UNTIL_KEY);
        if (isPsychosisActive(player)) finishPsychosisEpisode(player, true); else clearPsychosis(player);
        return hadEpisode;
    }

    private static void finishPsychosisEpisode(ServerPlayer player, boolean stabilized) {
        PsychosisIntro.cancel(player);
        long gameTime = player.level().getGameTime();
        CompoundTag persistentData = player.getPersistentData();
        EpisodeTier tier = readStoredEpisodeTier(persistentData);
        if (tier == EpisodeTier.NONE) {
            return;
        }

        if (player.isAlive()) CyberwareConditionManager.inducePsychosisFaults(player, player.getRandom());
        long aftershockTicks = tier == EpisodeTier.MAJOR ? 35L * 20L : 20L * 20L;
        persistentData.putLong(PSYCHOSIS_AFTERSHOCK_UNTIL_KEY, gameTime + aftershockTicks);
        persistentData.putLong(PSYCHOSIS_AFTERSHOCK_DURATION_KEY, aftershockTicks);
        reduceInstability(player, tier == EpisodeTier.MAJOR ? 42.0D : 28.0D);
        clearPsychosisState(persistentData, false);
        PsychosisPursuitController.clear(player);
        PsychosisRecovery.begin(player);
        PsychosisBossBarManager.update(player);
        player.displayClientMessage(Component.translatable(
                stabilized
                        ? "message.cyberneticenhancements.psychosis.stabilized"
                        : "message.cyberneticenhancements.psychosis.ended"), true);
        syncControlLock(player);
        CyberwareEffects.refreshPlayerCyberware(player);
    }

    public static void maintainEpisodeBlindness(Player player) {
        var effect=player.getEffect(MobEffects.BLINDNESS);
        if(effect==null||effect.getDuration()<40)player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,60,0,true,false,false));
    }

    private static void clearEpisodeBlindness(Player player) {
        if (player.getPersistentData().contains(PSYCHOSIS_UNTIL_KEY)) player.removeEffect(MobEffects.BLINDNESS);
    }

    private static void clearPsychosisState(CompoundTag persistentData, boolean clearAftershock) {
        persistentData.remove(PSYCHOSIS_UNTIL_KEY);
        persistentData.remove(PSYCHOSIS_DURATION_KEY);
        persistentData.remove(PSYCHOSIS_LAST_ATTACK_KEY);
        persistentData.remove(PSYCHOSIS_WANDER_YAW_KEY);
        persistentData.remove(PSYCHOSIS_TIER_KEY);
        persistentData.remove(PsychosisIntro.START); persistentData.remove(PsychosisIntro.YAW); persistentData.remove(PsychosisIntro.PITCH);
        persistentData.remove(PsychosisIntro.READY);
        if (clearAftershock) {
            persistentData.remove(PSYCHOSIS_AFTERSHOCK_UNTIL_KEY);
            persistentData.remove(PSYCHOSIS_AFTERSHOCK_DURATION_KEY);
        }
    }

    private static boolean isAftershockActive(Player player, long gameTime) {
        return player.getPersistentData().getLong(PSYCHOSIS_AFTERSHOCK_UNTIL_KEY) > gameTime;
    }

    private static EpisodeTier getEpisodeTier(Player player, long gameTime) {
        if (!isPsychosisActive(player, gameTime) || PsychosisIntro.active(player)) {
            return EpisodeTier.NONE;
        }
        return readStoredEpisodeTier(player.getPersistentData());
    }

    private static EpisodeTier readStoredEpisodeTier(CompoundTag persistentData) {
        String storedTier = persistentData.getString(PSYCHOSIS_TIER_KEY);
        try {
            return EpisodeTier.valueOf(storedTier);
        } catch (IllegalArgumentException ignored) {
            return EpisodeTier.MAJOR;
        }
    }

    private static void merge(EnumMap<CyberwareEffectType, Double> totals, CyberwareEffectType type, double amount) {
        totals.merge(type, amount, type.isMobEffect() ? Math::max : Double::sum);
    }

    private enum EpisodeTier {
        NONE(false),
        MINOR(true),
        MAJOR(true);

        private final boolean locksControl;

        EpisodeTier(boolean locksControl) {
            this.locksControl = locksControl;
        }

        boolean locksControl() {
            return locksControl;
        }
    }
}
