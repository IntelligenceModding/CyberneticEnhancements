package de.artemis.cyberneticenhancements.common.util;

/** Horizontal fall direction, using Minecraft's yaw convention for a backward fall. */
public final class DeathFallDirection {
    public static float yaw(double awayX, double awayZ, float fallback) {
        if (!Double.isFinite(awayX) || !Double.isFinite(awayZ) || awayX * awayX + awayZ * awayZ < 0.000001) return fallback;
        return (float) Math.toDegrees(Math.atan2(awayX, -awayZ));
    }

    private DeathFallDirection() { }
}
