package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

public final class HolographicDeployment {
    public static final int DURATION = 20;
    private HolographicDeployment() {}

    public static boolean departing(Mob mob) {
        return mob instanceof HolographicNpc npc && mob.getEntityData().get(npc.hologramDeparture()) >= 0;
    }

    public static void dismiss(Mob mob) {
        if (!(mob instanceof HolographicNpc npc) || mob.level().isClientSide || departing(mob) || !mob.isAlive()) return;
        mob.getEntityData().set(npc.hologramDeparture(), mob.level().getGameTime());
        mob.getPersistentData().putLong("HologramDeparture", mob.level().getGameTime());
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setNoAi(true);
        mob.setInvulnerable(true);
    }

    /** Returns true while departing, so normal deployment logic can be skipped. */
    public static boolean tick(Mob mob) {
        if (!(mob instanceof HolographicNpc npc) || !(mob.level() instanceof ServerLevel level)) return false;
        long now = level.getGameTime();
        if (mob.getEntityData().get(npc.hologramArrival()) < 0) {
            boolean seen = mob.getPersistentData().getBoolean("HologramArrived");
            mob.getEntityData().set(npc.hologramArrival(), seen ? now - DURATION : now);
            mob.getPersistentData().putBoolean("HologramArrived", true);
            if (mob.getPersistentData().contains("HologramDeparture"))
                mob.getEntityData().set(npc.hologramDeparture(), mob.getPersistentData().getLong("HologramDeparture"));
        }
        boolean leaving = departing(mob);
        long started = mob.getEntityData().get(leaving ? npc.hologramDeparture() : npc.hologramArrival());
        long age = now - started;
        if (leaving && age >= DURATION) {
            mob.discard();
            return true;
        }
        if (mob.isAlive()) scan(mob, age, leaving);
        return leaving;
    }

    public static void scan(Entity entity, long age, boolean leaving) {
        if (age < 0 || age >= DURATION || entity.level().getGameTime() % 2 != 0) return;
        double y = entity.getY() + visibility(age, leaving) * entity.getBbHeight();
        var color = entity instanceof TraumaTeamEntity || entity instanceof StreetRipperdocEntity ? new Vector3f(0.22F, 1, 0.65F) : new Vector3f(0.2F, 0.75F, 1);
        var dust = new DustParticleOptions(color, 0.65F);
        double radius = entity.getBbWidth() * 0.75;
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6 + age * 0.18;
            particles(entity, dust, entity.getX() + Math.cos(angle) * radius, y,
                    entity.getZ() + Math.sin(angle) * radius, 1, 0.015, 0.025, 0.015, 0);
        }
        particles(entity, net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                entity.getX(), y, entity.getZ(), 2, 0.2, 0.15, 0.2, 0.015);
    }

    private static void particles(Entity entity, ParticleOptions type, double x, double y, double z, int count,
                                  double dx, double dy, double dz, double speed) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(type, x, y, z, count, dx, dy, dz, speed);
        } else {
            var random = entity.level().getRandom();
            for (int i = 0; i < count; i++) {
                entity.level().addParticle(type, x + random.nextGaussian() * dx, y + random.nextGaussian() * dy,
                        z + random.nextGaussian() * dz, random.nextGaussian() * speed,
                        random.nextGaussian() * speed, random.nextGaussian() * speed);
            }
        }
    }

    public static float visibility(Mob mob, float partialTick) {
        if (!(mob instanceof HolographicNpc npc)) return 1;
        boolean leaving = departing(mob);
        long started = mob.getEntityData().get(leaving ? npc.hologramDeparture() : npc.hologramArrival());
        if (started < 0) return 1;
        return visibility(mob.level().getGameTime() - started + partialTick, leaving);
    }

    public static float visibility(float age, boolean leaving) {
        float elapsed = Math.clamp(age / DURATION, 0, 1);
        return leaving ? 1 - elapsed : elapsed;
    }
}
