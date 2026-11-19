package de.artemis.cyberneticenhancements.common.util;

/** Shared death classification and timing for visible corpses. */
public final class DeathBodyLifetime {
    public static final int HOLD_TICKS = 200, DISSOLVE_TICKS = 20;
    public static boolean isCorpse(net.minecraft.world.entity.LivingEntity entity, boolean confirmedDeath) {
        // isAlive() also becomes false on healthy discard/unload, which must never create a corpse.
        return confirmedDeath || entity.isDeadOrDying();
    }
    private DeathBodyLifetime() { }
}
