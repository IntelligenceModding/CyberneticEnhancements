package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

import de.artemis.cyberneticenhancements.common.network.SandevistanSnapshotPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SandevistanTimeDilation {
    private static final Map<Level, FieldState> FIELDS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<Entity, Motion>> MOTION = ThreadLocal.withInitial(IdentityHashMap::new);

    private SandevistanTimeDilation() {
    }

    public static void replace(Level level, Map<UUID, Float> targets, long expires) {
        FieldState state = FIELDS.computeIfAbsent(level, ignored -> new FieldState());
        state.targets = Map.copyOf(targets);
        state.expires = expires;
        state.budgets.keySet().retainAll(targets.keySet());
        state.poses.keySet().retainAll(targets.keySet());
    }

    public static void applyClientSnapshot(Level level, SandevistanSnapshotPayload payload) {
        if (!level.dimension().location().equals(payload.dimension())) return;
        Map<UUID, Float> targets = new HashMap<>();
        for (var target : payload.targets()) targets.put(target.uuid(), target.timeScale());
        replace(level, targets, level.getGameTime() + 16);
    }

    public static float scale(Entity entity) {
        FieldState state = FIELDS.get(entity.level());
        return state == null || entity.level().getGameTime() > state.expires ? 1 : state.targets.getOrDefault(entity.getUUID(), 1.0F);
    }

    public static boolean supportsProjectile(Entity entity) {
        return (entity instanceof AbstractArrow || entity instanceof ThrowableProjectile) && !(entity instanceof ThrownEnderpearl);
    }

    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        float scale = scale(entity);
        if (scale >= 1) return;
        if (supportsProjectile(entity)) {
            Motion motion = new Motion(entity.getDeltaMovement(), scale);
            MOTION.get().put(entity, motion);
            entity.setDeltaMovement(motion.scaledBase);
        } else if (entity instanceof Mob mob && mob.isAlive()) {
            FieldState state = FIELDS.get(entity.level());
            if (state != null && !state.budgets.computeIfAbsent(entity.getUUID(), ignored -> new TemporalBudget(entity.level().isClientSide())).advance(scale)) {
                entity.tickCount--;
                VisualPose pose = state.poses.get(entity.getUUID());
                if (entity.level().isClientSide() && pose != null) {
                    entity.xOld = pose.x;
                    entity.yOld = pose.y;
                    entity.zOld = pose.z;
                    entity.yRotO = pose.yaw;
                    entity.xRotO = pose.pitch;
                }
                if (mob.invulnerableTime > 0) mob.invulnerableTime--;
                event.setCanceled(true);
            }
        }
    }

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        restoreMotion(entity);
        FieldState state = FIELDS.get(entity.level());
        if (entity.level().isClientSide() && entity instanceof Mob && state != null && scale(entity) < 1) {
            state.poses.put(entity.getUUID(), new VisualPose(entity.xOld, entity.yOld, entity.zOld, entity.yRotO, entity.xRotO));
        }
    }

    public static double gravityScale(Entity entity) {
        Motion motion = MOTION.get().get(entity);
        return motion == null || motion.impact ? 1 : motion.scale;
    }

    public static float renderPartial(Entity entity, float partialTick) {
        float scale = scale(entity);
        FieldState state = FIELDS.get(entity.level());
        TemporalBudget budget = state == null ? null : state.budgets.get(entity.getUUID());
        return entity instanceof Mob && scale < 1 && budget != null ? budget.renderPartial(partialTick, scale) : partialTick;
    }

    public static void beginHit(Projectile projectile, HitResult hit) {
        Motion motion = MOTION.get().get(projectile);
        if (motion != null && hit.getType() == HitResult.Type.BLOCK) {
            motion.blockImpact = true;
        } else {
            beginImpact(projectile);
        }
    }

    public static void beginImpact(Projectile projectile) {
        Motion motion = MOTION.get().get(projectile);
        if (motion != null) {
            motion.impact = true;
            projectile.setDeltaMovement(motion.physicalBase);
        }
    }

    public static void endImpact(Projectile projectile) {
        Motion motion = MOTION.get().get(projectile);
        if (motion != null && !motion.blockImpact) {
            motion.physicalBase = projectile.getDeltaMovement();
            motion.scaledBase = motion.physicalBase.scale(motion.scale);
            projectile.setDeltaMovement(motion.scaledBase);
            motion.impact = false;
        }
    }

    private static void restoreMotion(Entity entity) {
        Motion motion = MOTION.get().remove(entity);
        if (motion != null && !motion.blockImpact) entity.setDeltaMovement(restoreVelocity(motion.physicalBase, motion.scaledBase, entity.getDeltaMovement()));
    }

    public static Vec3 restoreVelocity(Vec3 physical, Vec3 scaledBefore, Vec3 scaledAfter) {
        return physical.add(scaledAfter.subtract(scaledBefore));
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        for (Entity entity : MOTION.get().keySet().toArray(Entity[]::new)) {
            if (entity.level() == event.getLevel()) restoreMotion(entity);
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        FIELDS.remove(event.getLevel());
        MOTION.get().keySet().removeIf(entity -> entity.level() == event.getLevel());
    }

    public static void clearClient() {
        FIELDS.keySet().removeIf(Level::isClientSide);
        MOTION.remove();
    }

    private static final class FieldState {
        private Map<UUID, Float> targets = Map.of();
        private final Map<UUID, TemporalBudget> budgets = new HashMap<>();
        private final Map<UUID, VisualPose> poses = new HashMap<>();
        private long expires;
    }

    private record VisualPose(double x, double y, double z, float yaw, float pitch) {
    }

    private static final class Motion {
        private Vec3 physicalBase;
        private Vec3 scaledBase;
        private final double scale;
        private boolean impact;
        private boolean blockImpact;

        private Motion(Vec3 physicalBase, double scale) {
            this.physicalBase = physicalBase;
            this.scaledBase = physicalBase.scale(scale);
            this.scale = scale;
        }
    }
}
