package de.artemis.cyberneticenhancements.client.ui;

/** Scoped to the HUD draw; renderers resetting their shader color still respect the fade. */
public final class PsychosisHudFade {
    private static boolean active;
    private static float visibility = 1;
    public static void begin(float amount) {
        visibility = Math.clamp(1 - amount, 0, 1);
        active = true;
    }
    public static float alpha(float alpha) { return active ? alpha * visibility : alpha; }
    public static void withoutFade(Runnable action) {
        boolean previous=active;
        active=false;
        try { action.run(); } finally { active=previous; }
    }
    public static void end() { active = false; visibility = 1; }
    private PsychosisHudFade() {}
}
