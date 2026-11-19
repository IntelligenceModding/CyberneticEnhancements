package de.artemis.cyberneticenhancements.common.cyberware;

import net.minecraft.util.Mth;

public final class CyberpsychosisClientState {
    private static volatile boolean controlLocked;
    private static volatile float targetYaw;
    private static volatile float targetPitch;
    private static volatile float appliedYaw;
    private static volatile float appliedPitch;
    private static volatile float forward;
    private static volatile float strafe;
    private static volatile boolean jump;
    private static volatile boolean sprint;
    private static long lastSmoothingNanos;

    private CyberpsychosisClientState() {
    }

    public static boolean isControlLocked() {
        return controlLocked;
    }

    public static float yaw() {
        return appliedYaw;
    }

    public static float pitch() {
        return appliedPitch;
    }

    public static float targetYaw() {
        return targetYaw;
    }

    public static float targetPitch() {
        return targetPitch;
    }

    public static void alignView(float yaw, float pitch) {
        appliedYaw = yaw;
        appliedPitch = Mth.clamp(pitch, -90, 90);
    }

    public static float forward() {
        return forward;
    }

    public static float strafe() {
        return strafe;
    }

    public static boolean jump() {
        return jump;
    }

    public static boolean sprint() {
        return sprint;
    }

    public static void apply(boolean locked, float newYaw, float newPitch, float newForward, float newStrafe, boolean newJump, boolean newSprint) {
        boolean wasLocked = controlLocked;
        controlLocked = locked;
        targetYaw = newYaw;
        targetPitch = newPitch;
        if (!wasLocked || !locked) {
            lastSmoothingNanos = 0;
            appliedYaw = newYaw;
            appliedPitch = newPitch;
        }
        forward = locked ? newForward : 0;
        strafe = locked ? newStrafe : 0;
        jump = locked && newJump;
        sprint = locked && newSprint;
    }

    public static void tickSmoothing() {
        if (!controlLocked) {
            return;
        }

        long now = System.nanoTime();
        float seconds = lastSmoothingNanos == 0 ? 1F / 60 : Math.clamp((now - lastSmoothingNanos) / 1_000_000_000F, 0, .1F);
        lastSmoothingNanos = now;
        float blend = 1 - (float) Math.exp(-seconds / .085F);
        float yawDelta = Mth.wrapDegrees(targetYaw - appliedYaw);
        float pitchDelta = targetPitch - appliedPitch;
        appliedYaw += Mth.clamp(yawDelta * blend, -360 * seconds, 360 * seconds);
        appliedPitch += Mth.clamp(pitchDelta * blend, -240 * seconds, 240 * seconds);
        appliedPitch = Mth.clamp(appliedPitch, -90.0F, 90.0F);
    }

    public static void setControlLocked(boolean locked) {
        apply(locked, 0.0F, 0.0F, 0.0F, 0.0F, false, false);
    }
}
