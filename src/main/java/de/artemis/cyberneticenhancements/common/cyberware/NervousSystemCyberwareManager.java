package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NervousSystemCyberwareManager {
    private static final String ADRENALINE_CONVERTER_ID = "adrenaline_converter";
    private static final String ADRENO_TRIGGER_ID = "adreno_trigger";
    private static final String ATOMIC_SENSORS_ID = "atomic_sensors";
    private static final String KERENZIKOV_ID = "kerenzikov";
    private static final String NEOFIBER_ID = "neofiber";
    private static final String REVULSOR_ID = "revulsor";
    private static final String STABBER_ID = "stabber";
    private static final String SYNAPTIC_ACCELERATOR_ID = "synaptic_accelerator";
    private static final String TYROSINE_INJECTOR_ID = "tyrosine_injector";
    private static final String VISUAL_CORTEX_SUPPORT_ID = "visual_cortex_support";
    private static final String DEEP_FIELD_VISUAL_INTERFACE_ID = "deep_field_visual_interface";
    private static final String KIROSHI_RETRIEVAL_SUITE_ID = "kiroshi_retrieval_suite";

    private static final Map<UUID, Long> ADRENO_TRIGGER_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> KERENZIKOV_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> REVULSOR_READY_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Long> SYNAPTIC_ACCELERATOR_READY_UNTIL_TICK = new HashMap<>();

    private NervousSystemCyberwareManager() {
    }

    public static boolean hasSpecialBehavior(String id) {
        return switch (id) {
            case ADRENALINE_CONVERTER_ID,
                 ADRENO_TRIGGER_ID,
                 ATOMIC_SENSORS_ID,
                 KERENZIKOV_ID,
                 NEOFIBER_ID,
                 REVULSOR_ID,
                 STABBER_ID,
                 SYNAPTIC_ACCELERATOR_ID,
                 TYROSINE_INJECTOR_ID,
                 VISUAL_CORTEX_SUPPORT_ID,
                 DEEP_FIELD_VISUAL_INTERFACE_ID,
                 KIROSHI_RETRIEVAL_SUITE_ID -> true;
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
        int retrievalCount = inventory.countInstalledCyberware(KIROSHI_RETRIEVAL_SUITE_ID);
        if (retrievalCount > 0 && player.tickCount % 4 == 0) {
            vacuumNearbyDrops(player, CyberwareUpgradeHelper.performance(inventory, "kiroshi_retrieval_suite", "nervous.kiroshi_retrieval_suite.pickup_radius") * retrievalCount);
        }

        int atomicSensorsCount = inventory.countInstalledCyberware(ATOMIC_SENSORS_ID);
        int visualCortexCount = inventory.countInstalledCyberware(VISUAL_CORTEX_SUPPORT_ID);
        int deepFieldCount = inventory.countInstalledCyberware(DEEP_FIELD_VISUAL_INTERFACE_ID);
        if (atomicSensorsCount <= 0 && visualCortexCount <= 0 && deepFieldCount <= 0) {
            return;
        }

        if (atomicSensorsCount > 0 && player.tickCount % 20 == 0) {
            pulseAtomicSensors(player, atomicSensorsCount);
        }
        if ((visualCortexCount > 0 || deepFieldCount > 0) && player.tickCount % 10 == 0) {
            pulseVisualAssist(player, visualCortexCount, deepFieldCount);
        }
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
        int tyrosineCount = inventory.countInstalledCyberware(TYROSINE_INJECTOR_ID);
        if (tyrosineCount <= 0) {
            return;
        }

        long duration = isEliteTarget(target) ? 160L : 120L;
        applyTimedEffects(player, duration,
                CyberwareEffect.damage(1.0D * tyrosineCount),
                CyberwareEffect.moveSpeed(0.06D * tyrosineCount),
                CyberwareEffect.attackSpeed(0.05D * tyrosineCount),
                CyberwareEffect.healthRegen(0.35D * tyrosineCount));
        CyberwareEffects.refreshPlayerCyberware(player);
    }

    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int neofiberCount = inventory.countInstalledCyberware(NEOFIBER_ID);
        if (neofiberCount <= 0) {
            return;
        }

        float strength = event.getStrength();
        float multiplier = player.onGround()
                ? Math.max(0.10F, 1.0F - 0.35F * neofiberCount)
                : Math.max(0.18F, 1.0F - 0.22F * neofiberCount);
        if (player.isCrouching()) {
            multiplier *= 0.75F;
        }
        event.setStrength(strength * multiplier);
    }

    public static void clearCooldowns(Player player) {
        UUID playerId = player.getUUID();
        ADRENO_TRIGGER_READY_UNTIL_TICK.remove(playerId);
        KERENZIKOV_READY_UNTIL_TICK.remove(playerId);
        REVULSOR_READY_UNTIL_TICK.remove(playerId);
        SYNAPTIC_ACCELERATOR_READY_UNTIL_TICK.remove(playerId);
    }

    private static void handleIncomingPlayerDamage(LivingIncomingDamageEvent event, Player player) {
        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        float incomingDamage = event.getAmount();
        if (incomingDamage <= 0.0F) {
            return;
        }

        long gameTime = player.level().getGameTime();
        float healthAfterHit = player.getHealth() - incomingDamage;
        float midHealthThreshold = player.getMaxHealth() * 0.55F;
        boolean refreshRequired = false;

        int neofiberCount = inventory.countInstalledCyberware(NEOFIBER_ID);
        if (neofiberCount > 0 && event.getSource().is(DamageTypes.FALL)) {
            event.setAmount(event.getAmount() * Math.max(0.20F, 1.0F - 0.25F * neofiberCount));
            refreshRequired = true;
        }

        int adrenalineCount = inventory.countInstalledCyberware(ADRENALINE_CONVERTER_ID);
        if (adrenalineCount > 0) {
            applyTimedEffects(player, 70L,
                    CyberwareEffect.moveSpeed(0.05D * adrenalineCount),
                    CyberwareEffect.attackSpeed(0.03D * adrenalineCount));
            refreshRequired = true;
        }

        int adrenoTriggerCount = inventory.countInstalledCyberware(ADRENO_TRIGGER_ID);
        if (adrenoTriggerCount > 0
                && isReady(ADRENO_TRIGGER_READY_UNTIL_TICK, player, gameTime)
                && (incomingDamage >= player.getMaxHealth() * 0.18F || healthAfterHit <= midHealthThreshold)) {
            applyTimedEffects(player, 120L,
                    CyberwareEffect.moveSpeed(0.10D * adrenoTriggerCount),
                    CyberwareEffect.attackSpeed(0.08D * adrenoTriggerCount),
                    CyberwareEffect.damageReduction(0.05D * adrenoTriggerCount));
            ADRENO_TRIGGER_READY_UNTIL_TICK.put(player.getUUID(), gameTime + 18L * 20L);
            refreshRequired = true;
        }

        int kerenzikovCount = inventory.countInstalledCyberware(KERENZIKOV_ID);
        if (kerenzikovCount > 0
                && isRangedHit(player, event.getSource().getDirectEntity())
                && isReady(KERENZIKOV_READY_UNTIL_TICK, player, gameTime)) {
            applyTimedEffects(player, 60L,
                    CyberwareEffect.moveSpeed(0.12D * kerenzikovCount),
                    CyberwareEffect.attackSpeed(0.06D * kerenzikovCount),
                    CyberwareEffect.damageReduction(0.04D * kerenzikovCount));
            KERENZIKOV_READY_UNTIL_TICK.put(player.getUUID(), gameTime + 12L * 20L);
            refreshRequired = true;
        }

        int synapticCount = inventory.countInstalledCyberware(SYNAPTIC_ACCELERATOR_ID);
        if (synapticCount > 0
                && healthAfterHit > 0.0F
                && isReady(SYNAPTIC_ACCELERATOR_READY_UNTIL_TICK, player, gameTime)
                && (incomingDamage >= 2.0F || healthAfterHit <= player.getMaxHealth() * 0.75F)) {
            applyTimedEffects(player, 90L,
                    CyberwareEffect.moveSpeed(0.10D * synapticCount),
                    CyberwareEffect.attackSpeed(0.08D * synapticCount),
                    CyberwareEffect.breakSpeed(0.06D * synapticCount));
            SYNAPTIC_ACCELERATOR_READY_UNTIL_TICK.put(player.getUUID(), gameTime + 16L * 20L);
            refreshRequired = true;
        }

        int revulsorCount = inventory.countInstalledCyberware(REVULSOR_ID);
        if (revulsorCount > 0
                && healthAfterHit > 0.0F
                && isReady(REVULSOR_READY_UNTIL_TICK, player, gameTime)
                && (incomingDamage >= 4.0F || healthAfterHit <= midHealthThreshold)) {
            triggerRevulsorPulse(player, revulsorCount);
            REVULSOR_READY_UNTIL_TICK.put(player.getUUID(), gameTime + 30L * 20L);
            refreshRequired = true;
        }

        if (refreshRequired) {
            CyberwareEffects.refreshPlayerCyberware(player);
        }
    }

    private static void handleOutgoingPlayerDamage(LivingIncomingDamageEvent event, Player player) {
        LivingEntity target = event.getEntity();
        Entity directEntity = event.getSource().getDirectEntity();
        if (!isMeleeHit(player, directEntity)) {
            return;
        }

        PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(player);
        int stabberCount = inventory.countInstalledCyberware(STABBER_ID);
        if (stabberCount <= 0) {
            return;
        }

        boolean behindTarget = isBehindTarget(player, target);
        boolean markedTarget = hasStatus(target, CombatStatusType.MARK);

        float bonusDamage = 0.60F * stabberCount;
        if (behindTarget) {
            bonusDamage += 1.10F * stabberCount;
        }
        if (markedTarget) {
            bonusDamage += 0.80F * stabberCount;
        }
        event.setAmount(event.getAmount() + bonusDamage);

        CombatStatusManager.applyStatus(target, CombatStatusType.BLEED, 80L + stabberCount * 20L, behindTarget ? 2 : 1, true);
        if (behindTarget) {
            CombatStatusManager.applyStatus(target, CombatStatusType.MARK, 60L + stabberCount * 20L, 1, true);
        }

        sendHighlight(player, List.of(target.getUUID()), 30);
    }

    private static void pulseAtomicSensors(Player player, int installedCount) {
        double radius = (8.0D + installedCount * 2.0D) * CyberwareUpgradeHelper.functionMultiplier(new PlayerCyberwareInventory(player), ATOMIC_SENSORS_ID);
        int maxTargets = 6 + installedCount * 2;
        List<UUID> highlightedEntities = new ArrayList<>();

        List<LivingEntity> livingThreats = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                target -> target != player && isThreat(player, target)
        );
        livingThreats.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (LivingEntity threat : livingThreats) {
            if (highlightedEntities.size() >= maxTargets) {
                break;
            }
            highlightedEntities.add(threat.getUUID());
        }

        List<Entity> hazards = player.level().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity instanceof PrimedTnt
                        || entity instanceof EndCrystal
                        || entity instanceof Projectile projectile && projectile.getOwner() != player
        );
        hazards.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (Entity hazard : hazards) {
            if (highlightedEntities.size() >= maxTargets + 3) {
                break;
            }
            highlightedEntities.add(hazard.getUUID());
        }

        sendHighlight(player, highlightedEntities, 24);
    }

    private static void pulseVisualAssist(Player player, int visualCortexCount, int deepFieldCount) {
        boolean deepField = deepFieldCount > 0;
        double range = deepField ? 20.0D + deepFieldCount * 2.0D : 12.0D + visualCortexCount * 2.0D;
        double minDot = deepField ? 0.82D : 0.92D;
        int maxTargets = deepField ? 2 : 1;

        Vec3 eyePosition = player.getEyePosition();
        Vec3 lookVector = player.getLookAngle().normalize();
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                target -> target != player
                        && isThreat(player, target)
                        && (deepField || player.hasLineOfSight(target))
                        && eyePosition.distanceToSqr(target.getEyePosition()) <= range * range
        );
        candidates.sort(Comparator
                .comparingDouble((LivingEntity target) -> -viewDot(eyePosition, lookVector, target))
                .thenComparingDouble(player::distanceToSqr));

        List<UUID> highlighted = new ArrayList<>();
        for (LivingEntity candidate : candidates) {
            if (highlighted.size() >= maxTargets) {
                break;
            }
            if (viewDot(eyePosition, lookVector, candidate) < minDot) {
                continue;
            }
            highlighted.add(candidate.getUUID());
        }

        sendHighlight(player, highlighted, 16);
    }

    private static void triggerRevulsorPulse(Player player, int installedCount) {
        double radius = 3.5D + installedCount;
        List<UUID> highlighted = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && isThreat(player, entity))) {
            Vec3 offset = target.position().subtract(player.position());
            if (offset.lengthSqr() < 0.001D) {
                continue;
            }

            target.knockback(0.55D + 0.20D * installedCount, offset.x, offset.z);
            CombatStatusManager.applyStatus(target, CombatStatusType.TRAUMA, 60L + installedCount * 20L, 1, true);
            highlighted.add(target.getUUID());
        }

        applyTimedEffects(player, 80L,
                CyberwareEffect.bonusAbsorption(1.5D * installedCount),
                CyberwareEffect.damageReduction(0.05D * installedCount));
        sendHighlight(player, highlighted, 24);
    }

    private static void applyTimedEffects(Player player, long baseDurationTicks, CyberwareEffect... effects) {
        TemporaryCyberwareEffectManager.addEffects(player, baseDurationTicks, effects);
    }

    private static boolean isThreat(Player player, LivingEntity target) {
        return target instanceof Enemy
                || target instanceof Mob mob && mob.getTarget() == player
                || target.getLastHurtByMob() == player
                || hasStatus(target, CombatStatusType.MARK);
    }

    private static boolean hasStatus(LivingEntity target, CombatStatusType type) {
        for (CombatStatusManager.ActiveStatus status : CombatStatusManager.collectActiveStatuses(target, target.level().getGameTime())) {
            if (status.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRangedHit(Player player, Entity directEntity) {
        return directEntity != null && directEntity != player;
    }

    private static boolean isMeleeHit(Player player, Entity directEntity) {
        return directEntity == player;
    }

    private static boolean isBehindTarget(Player player, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 toAttacker = player.position().subtract(target.position());
        if (toAttacker.lengthSqr() < 0.0001D) {
            return false;
        }
        return look.normalize().dot(toAttacker.normalize()) < -0.20D;
    }

    private static boolean isEliteTarget(LivingEntity target) {
        return target instanceof Enemy || target.getMaxHealth() >= 30.0F;
    }

    private static double viewDot(Vec3 eyePosition, Vec3 lookVector, LivingEntity target) {
        Vec3 toTarget = target.getEyePosition().subtract(eyePosition);
        if (toTarget.lengthSqr() < 0.0001D) {
            return 1.0D;
        }
        return lookVector.dot(toTarget.normalize());
    }

    private static boolean isReady(Map<UUID, Long> cooldowns, Player player, long gameTime) {
        return cooldowns.getOrDefault(player.getUUID(), 0L) <= gameTime;
    }

    private static void vacuumNearbyDrops(Player player, double extraRadius) {
        if (extraRadius <= 0.0D) {
            return;
        }

        for (ItemEntity itemEntity : player.level().getEntitiesOfClass(
                ItemEntity.class,
                player.getBoundingBox().inflate(extraRadius),
                candidate -> candidate.isAlive() && !candidate.hasPickUpDelay())) {
            itemEntity.playerTouch(player);
        }

        for (ExperienceOrb experienceOrb : player.level().getEntitiesOfClass(
                ExperienceOrb.class,
                player.getBoundingBox().inflate(extraRadius),
                ExperienceOrb::isAlive)) {
            experienceOrb.playerTouch(player);
        }
    }

    private static void sendHighlight(Player player, List<UUID> entityIds, int ttlTicks) {
        if (entityIds.isEmpty() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        PacketDistributor.sendToPlayer(serverPlayer, new FaceHazardHighlightPayload(List.of(), entityIds, ttlTicks));
    }
}
