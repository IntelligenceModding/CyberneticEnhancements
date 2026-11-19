package de.artemis.cyberneticenhancements.client.ui;

/** Presentation state: health gains fill over 2.5 seconds, while damage lowers the bar immediately. */
public final class HealingHealthBar {
    public static final double FILL_TICKS = 50;
    private float from, target;
    private double started, ends;
    private boolean initialized, green;
    private float colorFrom;
    private double colorStarted;

    public record Frame(float health, float pendingHealth, float green, boolean filling) {}

    public Frame sample(float health, boolean usingMedicine, boolean highlighted, double now, float healFraction) {
        health = Math.clamp(health, 0, 1);
        if (!initialized) {
            initialized = true;
            from = target = health;
            started = ends = now;
        }
        float displayed = displayed(now);
        if (health > target) {
            from = displayed;
            target = health;
            started = now;
            ends = now + FILL_TICKS;
        } else if (health < target) {
            // Account for hits during a healing animation without retaining the old healing endpoint.
            displayed = Math.max(0, displayed - (target - health));
            from = displayed;
            target = health;
            started = now;
            if (ends <= now || displayed >= health) { from = health; ends = now; }
        }
        displayed = displayed(now);
        boolean filling = displayed < target && now < ends;
        float pending = filling ? target : usingMedicine ? Math.min(1, target + healFraction) : displayed;
        float colorProgress = smooth((now - colorStarted) / 7);
        float color = colorFrom + ((green ? 1 : 0) - colorFrom) * colorProgress;
        boolean nextGreen = highlighted || usingMedicine || filling;
        if (green != nextGreen) {
            colorFrom = color;
            green = nextGreen;
            colorStarted = now;
        }
        return new Frame(displayed, pending, color, filling);
    }

    private float displayed(double now) {
        return ends <= started ? target : from + (target - from) * smooth((now - started) / (ends - started));
    }

    private static float smooth(double progress) {
        float t = (float) Math.clamp(progress, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
