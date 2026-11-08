package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3f;

public final class CombatStatusManager {
    private static final String STATUSES_KEY = "cyberneticenhancements.combat_statuses";
    private static final String TYPE_KEY = "Type";
    private static final String STACKS_KEY = "Stacks";
    private static final String UNTIL_KEY = "Until";
    private static final String DURATION_KEY = "Duration";
    private static final String PLAYER_APPLIED_KEY = "PlayerApplied";
    private static final DustParticleOptions SHOCK_DUST = new DustParticleOptions(new Vector3f(0.46F, 0.98F, 0.95F), 0.9F);
    private static final DustParticleOptions SHOCK_CORE_DUST = new DustParticleOptions(new Vector3f(0.78F, 1.0F, 1.0F), 1.15F);
    private static final DustParticleOptions OVERHEAT_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.42F, 0.08F), 1.0F);
    private static final DustParticleOptions OVERHEAT_CORE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.76F, 0.18F), 0.85F);
    private static final DustParticleOptions CORROSION_DUST = new DustParticleOptions(new Vector3f(0.55F, 1.0F, 0.20F), 0.95F);
    private static final DustParticleOptions CORROSION_CORE_DUST = new DustParticleOptions(new Vector3f(0.88F, 1.0F, 0.32F), 0.75F);
    private static final DustParticleOptions TRAUMA_DUST = new DustParticleOptions(new Vector3f(0.88F, 0.16F, 0.14F), 1.0F);
    private static final DustParticleOptions TRAUMA_CORE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.84F), 0.6F);
    private static final DustParticleOptions BLEED_DUST = new DustParticleOptions(new Vector3f(0.78F, 0.05F, 0.08F), 0.95F);
    private static final DustParticleOptions MARK_DUST = new DustParticleOptions(new Vector3f(0.12F, 0.89F, 0.71F), 0.95F);
    private static final DustParticleOptions MARK_CORE_DUST = new DustParticleOptions(new Vector3f(0.46F, 1.0F, 0.88F), 0.7F);
    private static final double SHOCK_MOVE_SPEED_PER_STACK = CyberwareBalance.doubleValue("combat_status.shock.move_speed_per_stack");
    private static final double SHOCK_ATTACK_SPEED_PER_STACK = CyberwareBalance.doubleValue("combat_status.shock.attack_speed_per_stack");
    private static final double SHOCK_ABILITY_COOLDOWN_PER_STACK = CyberwareBalance.doubleValue("combat_status.shock.ability_cooldown_per_stack");
    private static final double OVERHEAT_DAMAGE_TAKEN_PER_STACK = CyberwareBalance.doubleValue("combat_status.overheat.damage_taken_per_stack");
    private static final double TRAUMA_DAMAGE_TAKEN_PER_STACK = CyberwareBalance.doubleValue("combat_status.trauma.damage_taken_per_stack");
    private static final double BLEED_DAMAGE_TAKEN_PER_STACK = CyberwareBalance.doubleValue("combat_status.bleed.damage_taken_per_stack");
    private static final double MARK_DAMAGE_TAKEN_PER_STACK = CyberwareBalance.doubleValue("combat_status.mark.damage_taken_per_stack");
    private static final double TRAUMA_KNOCKBACK_TAKEN_PER_STACK = CyberwareBalance.doubleValue("combat_status.trauma.knockback_taken_per_stack");

    private static final Map<ResourceKey<Level>, Set<java.util.UUID>> TRACKED_ENTITIES = new HashMap<>();

    private CombatStatusManager() {
    }

    public record ActiveStatus(CombatStatusType type, int stacks, int remainingSeconds, int totalSeconds, boolean playerApplied) {
    }

    public static void applyStatus(LivingEntity target, CombatStatusType type, long durationTicks, int stackDelta) {
        applyStatus(target, type, durationTicks, stackDelta, false);
    }

    public static void applyStatus(LivingEntity target, CombatStatusType type, long durationTicks, int stackDelta, boolean playerApplied) {
        if (target.level().isClientSide() || durationTicks <= 0L || stackDelta <= 0) {
            return;
        }

        long gameTime = target.level().getGameTime();
        ListTag updatedEntries = new ListTag();
        boolean merged = false;

        for (Tag tag : target.getPersistentData().getList(STATUSES_KEY, Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag entry)) {
                continue;
            }

            CombatStatusType existingType = parseType(entry);
            if (existingType == null || entry.getLong(UNTIL_KEY) <= gameTime) {
                continue;
            }

            if (existingType == type) {
                CompoundTag updated = entry.copy();
                updated.putInt(STACKS_KEY, Math.min(type.maxStacks(), Math.max(1, updated.getInt(STACKS_KEY)) + stackDelta));
                updated.putLong(UNTIL_KEY, Math.max(updated.getLong(UNTIL_KEY), gameTime + durationTicks));
                updated.putLong(DURATION_KEY, Math.max(updated.getLong(DURATION_KEY), durationTicks));
                updated.putBoolean(PLAYER_APPLIED_KEY, updated.getBoolean(PLAYER_APPLIED_KEY) || playerApplied);
                updatedEntries.add(updated);
                merged = true;
            } else {
                updatedEntries.add(entry.copy());
            }
        }

        if (!merged) {
            CompoundTag entry = new CompoundTag();
            entry.putString(TYPE_KEY, type.name());
            entry.putInt(STACKS_KEY, Math.min(type.maxStacks(), stackDelta));
            entry.putLong(UNTIL_KEY, gameTime + durationTicks);
            entry.putLong(DURATION_KEY, durationTicks);
            entry.putBoolean(PLAYER_APPLIED_KEY, playerApplied);
            updatedEntries.add(entry);
        }

        target.getPersistentData().put(STATUSES_KEY, updatedEntries);
        track(target);
    }

    public static void clearStatuses(LivingEntity entity) {
        entity.getPersistentData().remove(STATUSES_KEY);
        untrack(entity);
    }

    public static List<ActiveStatus> collectActiveStatuses(LivingEntity entity, long gameTime) {
        ListTag activeEntries = collectAndStoreActiveEntries(entity, gameTime);
        if (activeEntries.isEmpty()) {
            return List.of();
        }

        List<ActiveStatus> statuses = new ArrayList<>(activeEntries.size());
        for (Tag tag : activeEntries) {
            if (!(tag instanceof CompoundTag entry)) {
                continue;
            }

            CombatStatusType type = parseType(entry);
            if (type == null) {
                continue;
            }

            statuses.add(new ActiveStatus(
                    type,
                    Math.max(1, entry.getInt(STACKS_KEY)),
                    (int) ((entry.getLong(UNTIL_KEY) - gameTime + 19L) / 20L),
                    (int) Math.max(1L, (entry.getLong(DURATION_KEY) + 19L) / 20L),
                    entry.getBoolean(PLAYER_APPLIED_KEY)
            ));
        }
        return statuses;
    }

    public static void mergePlayerEffects(Player player, EnumMap<CyberwareEffectType, Double> totals) {
        long gameTime = player.level().getGameTime();
        int shockStacks = getStacks(player, CombatStatusType.SHOCK, gameTime);
        if (shockStacks > 0) {
            totals.merge(CyberwareEffectType.MOVEMENT_SPEED, SHOCK_MOVE_SPEED_PER_STACK * shockStacks, Double::sum);
            totals.merge(CyberwareEffectType.ATTACK_SPEED, SHOCK_ATTACK_SPEED_PER_STACK * shockStacks, Double::sum);
        }
    }

    public static double getAbilityCooldownMultiplier(Player player) {
        int shockStacks = getStacks(player, CombatStatusType.SHOCK, player.level().getGameTime());
        return shockStacks <= 0 ? 1.0D : 1.0D + shockStacks * SHOCK_ABILITY_COOLDOWN_PER_STACK;
    }

    public static double getHealingMultiplier(LivingEntity entity) {
        int corrosionStacks = getStacks(entity, CombatStatusType.CORROSION, entity.level().getGameTime());
        if (corrosionStacks <= 0) {
            return 1.0D;
        }
        return switch (corrosionStacks) {
            case 1 -> 0.80D;
            case 2 -> 0.60D;
            default -> 0.40D;
        };
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }

        long gameTime = target.level().getGameTime();
        float multiplier = 1.0F;
        multiplier += (float) (getStacks(target, CombatStatusType.OVERHEAT, gameTime) * OVERHEAT_DAMAGE_TAKEN_PER_STACK);
        multiplier += (float) (getStacks(target, CombatStatusType.TRAUMA, gameTime) * TRAUMA_DAMAGE_TAKEN_PER_STACK);

        int bleedStacks = getStacks(target, CombatStatusType.BLEED, gameTime);
        if (bleedStacks > 0 && hasAggressor(event.getSource())) {
            multiplier += (float) (bleedStacks * BLEED_DAMAGE_TAKEN_PER_STACK);
        }

        int markStacks = getStacks(target, CombatStatusType.MARK, gameTime);
        if (markStacks > 0 && isPlayerAppliedDamage(event.getSource())) {
            multiplier += (float) (markStacks * MARK_DAMAGE_TAKEN_PER_STACK);
        }

        if (multiplier > 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }

        int traumaStacks = getStacks(target, CombatStatusType.TRAUMA, target.level().getGameTime());
        if (traumaStacks > 0) {
            event.setStrength((float) (event.getStrength() * (1.0D + traumaStacks * TRAUMA_KNOCKBACK_TAKEN_PER_STACK)));
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Set<java.util.UUID> tracked = TRACKED_ENTITIES.get(level.dimension());
        if (tracked == null || tracked.isEmpty()) {
            return;
        }

        List<java.util.UUID> expired = new ArrayList<>();
        for (java.util.UUID uuid : List.copyOf(tracked)) {
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                expired.add(uuid);
                continue;
            }

            if (!tickStatuses(living, level.getGameTime())) {
                expired.add(uuid);
            }
        }

        tracked.removeAll(expired);
        if (tracked.isEmpty()) {
            TRACKED_ENTITIES.remove(level.dimension());
        }
    }

    public static Component describeStatus(String translationKey, double amount) {
        CombatStatusType type = fromTranslationKey(translationKey);
        if (type == null) {
            return Component.empty();
        }

        int stacks = Math.max(1, (int) Math.round(amount));
        return Component.translatable(type.descriptionKey(), stacks);
    }

    public static CombatStatusType fromTranslationKey(String translationKey) {
        for (CombatStatusType type : CombatStatusType.values()) {
            if (type.translationKey().equals(translationKey)) {
                return type;
            }
        }
        return null;
    }

    private static boolean tickStatuses(LivingEntity entity, long gameTime) {
        ListTag activeEntries = collectAndStoreActiveEntries(entity, gameTime);
        if (activeEntries.isEmpty()) {
            untrack(entity);
            return false;
        }

        for (Tag tag : activeEntries) {
            if (!(tag instanceof CompoundTag entry)) {
                continue;
            }

            CombatStatusType type = parseType(entry);
            if (type == null) {
                continue;
            }

            int stacks = Math.max(1, entry.getInt(STACKS_KEY));
            spawnAmbientParticles(entity, type, stacks, gameTime);
            switch (type) {
                case SHOCK -> tickShock(entity, stacks, gameTime);
                case OVERHEAT -> tickOverheat(entity, stacks, gameTime);
                case CORROSION -> tickCorrosion(entity, stacks, gameTime);
                case TRAUMA -> tickTrauma(entity, stacks, gameTime);
                case BLEED -> tickBleed(entity, stacks, gameTime);
                case MARK -> tickMark(entity, stacks, gameTime);
            }
        }
        return true;
    }

    private static void tickShock(LivingEntity entity, int stacks, long gameTime) {
        if (gameTime % 6L != 0L) {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();
        double horizontalScale = Math.max(0.25D, 0.72D - stacks * 0.12D);
        double wobble = 0.13D * stacks;
        double verticalJitter = entity.onGround() ? 0.0D : (entity.getRandom().nextDouble() - 0.5D) * 0.04D * stacks;
        entity.setDeltaMovement(
                motion.x * horizontalScale + (entity.getRandom().nextDouble() - 0.5D) * wobble,
                motion.y + verticalJitter,
                motion.z * horizontalScale + (entity.getRandom().nextDouble() - 0.5D) * wobble
        );
        entity.hurtMarked = true;
        PlayerMotionSyncHelper.sync(entity);
        broadcastOutlineHighlight(entity, 20);
    }

    private static void tickOverheat(LivingEntity entity, int stacks, long gameTime) {
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 20));
        if (gameTime % 20L != 0L) {
            return;
        }

        float damage = 0.75F * stacks;
        if (horizontalSpeed(entity) > 0.14D) {
            damage += 0.45F * stacks;
        }
        damageStatus(entity, damage);
    }

    private static void tickCorrosion(LivingEntity entity, int stacks, long gameTime) {
        if (gameTime % 20L != 0L) {
            return;
        }
        damageStatus(entity, 0.45F * stacks);
    }

    private static void tickTrauma(LivingEntity entity, int stacks, long gameTime) {
        if (gameTime % 12L != 0L) {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();
        if (motion.horizontalDistanceSqr() > 0.01D) {
            double damp = Math.max(0.55D, 0.90D - stacks * 0.10D);
            entity.setDeltaMovement(motion.x * damp, motion.y, motion.z * damp);
            entity.hurtMarked = true;
            PlayerMotionSyncHelper.sync(entity);
        }
    }

    private static void tickBleed(LivingEntity entity, int stacks, long gameTime) {
        if (gameTime % 20L != 0L) {
            return;
        }

        float damage = 0.50F * stacks;
        if (horizontalSpeed(entity) > 0.12D) {
            damage += 0.60F * stacks;
        }
        damageStatus(entity, damage);
    }

    private static void tickMark(LivingEntity entity, int stacks, long gameTime) {
        if (gameTime % 10L != 0L) {
            return;
        }
        broadcastOutlineHighlight(entity, 18);
    }

    private static void damageStatus(LivingEntity entity, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        entity.hurt(entity.damageSources().magic(), amount);
    }

    private static void broadcastOutlineHighlight(Entity entity, int ttlTicks) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) > 64.0D * 64.0D) {
                continue;
            }
            PacketDistributor.sendToPlayer(player, new FaceHazardHighlightPayload(List.of(), List.of(entity.getUUID()), ttlTicks));
        }
    }

    private static void spawnAmbientParticles(LivingEntity entity, CombatStatusType type, int stacks, long gameTime) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = entity.getBoundingBox().getCenter();
        double bodyRadius = Math.max(0.18D, entity.getBbWidth() * 0.42D);
        double shoulderY = center.y + entity.getBbHeight() * 0.18D;
        double headY = center.y + entity.getBbHeight() * 0.34D;
        switch (type) {
            case SHOCK -> {
                if (gameTime % 5L != 0L) {
                    return;
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, shoulderY, center.z, 3 + stacks * 3, bodyRadius + 0.08D, 0.34D, bodyRadius + 0.08D, 0.03D);
                level.sendParticles(SHOCK_DUST, center.x, shoulderY, center.z, 3 + stacks * 2, bodyRadius, 0.26D, bodyRadius, 0.01D);
                if (gameTime % 10L == 0L) {
                    level.sendParticles(SHOCK_CORE_DUST, center.x, headY, center.z, 2 + stacks, bodyRadius * 0.45D, 0.12D, bodyRadius * 0.45D, 0.0D);
                }
            }
            case OVERHEAT -> {
                if (gameTime % 4L != 0L) {
                    return;
                }
                level.sendParticles(ParticleTypes.SMALL_FLAME, center.x, shoulderY, center.z, 2 + stacks * 2, bodyRadius, 0.32D, bodyRadius, 0.01D);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.08D, center.z, 1 + stacks, bodyRadius * 0.65D, 0.10D, bodyRadius * 0.65D, 0.005D);
                level.sendParticles(OVERHEAT_DUST, center.x, shoulderY, center.z, 2 + stacks, bodyRadius * 0.8D, 0.24D, bodyRadius * 0.8D, 0.01D);
                if (gameTime % 8L == 0L) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, headY, center.z, 1 + stacks, bodyRadius * 0.55D, 0.12D, bodyRadius * 0.55D, 0.01D);
                    level.sendParticles(ParticleTypes.SMOKE, center.x, headY + 0.12D, center.z, 2 + stacks, bodyRadius * 0.75D, 0.18D, bodyRadius * 0.75D, 0.02D);
                }
                if (gameTime % 12L == 0L) {
                    spawnDustRing(level, OVERHEAT_CORE_DUST, center.x, center.y + 0.18D, center.z, bodyRadius + 0.08D, 5 + stacks);
                }
            }
            case CORROSION -> {
                if (gameTime % 6L != 0L) {
                    return;
                }
                level.sendParticles(ParticleTypes.SNEEZE, center.x, shoulderY, center.z, 2 + stacks * 2, bodyRadius, 0.24D, bodyRadius, 0.01D);
                level.sendParticles(CORROSION_DUST, center.x, shoulderY, center.z, 2 + stacks, bodyRadius * 0.8D, 0.20D, bodyRadius * 0.8D, 0.01D);
                if (gameTime % 12L == 0L) {
                    level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y - entity.getBbHeight() * 0.18D, center.z, 1 + stacks, bodyRadius * 0.65D, 0.06D, bodyRadius * 0.65D, 0.02D);
                    spawnVerticalDustLine(level, CORROSION_CORE_DUST, center.x, shoulderY, center.z, entity.getBbHeight() * 0.75D, 3 + stacks);
                }
            }
            case TRAUMA -> {
                if (gameTime % 7L != 0L) {
                    return;
                }
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, shoulderY, center.z, 2 + stacks, bodyRadius * 0.75D, 0.24D, bodyRadius * 0.75D, 0.01D);
                level.sendParticles(TRAUMA_DUST, center.x, shoulderY, center.z, 2 + stacks, bodyRadius * 0.65D, 0.18D, bodyRadius * 0.65D, 0.01D);
                if (gameTime % 14L == 0L) {
                    level.sendParticles(ParticleTypes.CRIT, center.x, shoulderY, center.z, 2 + stacks * 2, bodyRadius * 0.85D, 0.18D, bodyRadius * 0.85D, 0.03D);
                    spawnDustRing(level, TRAUMA_CORE_DUST, center.x, center.y + 0.10D, center.z, bodyRadius + 0.06D, 4 + stacks * 2);
                }
            }
            case BLEED -> {
                if (gameTime % 8L != 0L) {
                    return;
                }
                level.sendParticles(BLEED_DUST, center.x, shoulderY, center.z, 2 + stacks, bodyRadius * 0.70D, 0.22D, bodyRadius * 0.70D, 0.01D);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, shoulderY - 0.08D, center.z, 1 + stacks, bodyRadius * 0.45D, 0.12D, bodyRadius * 0.45D, 0.01D);
                if (horizontalSpeed(entity) > 0.12D) {
                    spawnDirectionalDustTrail(level, BLEED_DUST, center, entity.getDeltaMovement(), bodyRadius, 2 + stacks);
                }
            }
            case MARK -> {
                if (gameTime % 6L != 0L) {
                    return;
                }
                spawnDustRing(level, MARK_DUST, center.x, headY, center.z, bodyRadius + 0.18D, 6 + stacks * 2);
                spawnDustRing(level, MARK_CORE_DUST, center.x, center.y - entity.getBbHeight() * 0.10D, center.z, bodyRadius + 0.10D, 5 + stacks);
                level.sendParticles(ParticleTypes.ENCHANT, center.x, shoulderY, center.z, 1 + stacks, bodyRadius * 0.75D, 0.24D, bodyRadius * 0.75D, 0.05D);
            }
        }
    }

    private static void spawnDustRing(ServerLevel level, DustParticleOptions particle, double x, double y, double z, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            level.sendParticles(particle, px, y, pz, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    private static void spawnVerticalDustLine(ServerLevel level, DustParticleOptions particle, double x, double y, double z, double height, int points) {
        for (int i = 0; i < points; i++) {
            double progress = points <= 1 ? 0.0D : (double) i / (points - 1);
            level.sendParticles(particle, x, y - progress * height, z, 1, 0.04D, 0.02D, 0.04D, 0.0D);
        }
    }

    private static void spawnDirectionalDustTrail(ServerLevel level, DustParticleOptions particle, Vec3 center, Vec3 motion, double radius, int points) {
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 trailDir = horizontal.normalize().scale(-radius * 0.9D);
        for (int i = 0; i < points; i++) {
            double progress = points <= 1 ? 0.0D : (double) i / (points - 1);
            Vec3 point = center.add(trailDir.scale(progress));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.03D, 0.10D, 0.03D, 0.0D);
        }
    }

    private static boolean hasAggressor(DamageSource source) {
        return source.getEntity() instanceof LivingEntity || source.getDirectEntity() instanceof LivingEntity;
    }

    private static boolean isPlayerAppliedDamage(DamageSource source) {
        return source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player;
    }

    private static double horizontalSpeed(LivingEntity entity) {
        Vec3 motion = entity.getDeltaMovement();
        return Math.sqrt(motion.x * motion.x + motion.z * motion.z);
    }

    private static int getStacks(LivingEntity entity, CombatStatusType targetType, long gameTime) {
        int stacks = 0;
        for (Tag tag : collectAndStoreActiveEntries(entity, gameTime)) {
            if (!(tag instanceof CompoundTag entry)) {
                continue;
            }

            CombatStatusType type = parseType(entry);
            if (type == targetType) {
                stacks = Math.max(stacks, Math.max(1, entry.getInt(STACKS_KEY)));
            }
        }
        return stacks;
    }

    private static ListTag collectAndStoreActiveEntries(LivingEntity entity, long gameTime) {
        ListTag activeEntries = new ListTag();
        for (Tag tag : entity.getPersistentData().getList(STATUSES_KEY, Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag entry) || entry.getLong(UNTIL_KEY) <= gameTime) {
                continue;
            }

            if (parseType(entry) == null) {
                continue;
            }
            activeEntries.add(entry.copy());
        }

        if (activeEntries.isEmpty()) {
            entity.getPersistentData().remove(STATUSES_KEY);
        } else {
            entity.getPersistentData().put(STATUSES_KEY, activeEntries);
        }
        return activeEntries;
    }

    private static CombatStatusType parseType(CompoundTag entry) {
        try {
            return CombatStatusType.valueOf(entry.getString(TYPE_KEY));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void track(LivingEntity entity) {
        TRACKED_ENTITIES.computeIfAbsent(entity.level().dimension(), ignored -> new HashSet<>()).add(entity.getUUID());
    }

    private static void untrack(LivingEntity entity) {
        Set<java.util.UUID> tracked = TRACKED_ENTITIES.get(entity.level().dimension());
        if (tracked == null) {
            return;
        }
        tracked.remove(entity.getUUID());
        if (tracked.isEmpty()) {
            TRACKED_ENTITIES.remove(entity.level().dimension());
        }
    }
}
