package de.artemis.cyberneticenhancements.client.ui;

/** A short eased fade that can reverse without jumping to either endpoint. */
public final class UiVisibilityTransition {
    public static final double DURATION_SECONDS = .22;
    private boolean visible;
    private float from;
    private double changedAt = Double.NEGATIVE_INFINITY;

    public UiVisibilityTransition(boolean visible) {
        this.visible = visible;
        this.from = visible ? 1 : 0;
    }

    public float sample(boolean wanted, double seconds) {
        float current = current(seconds);
        if (wanted != visible) {
            from = current;
            visible = wanted;
            changedAt = seconds;
        }
        return current;
    }

    private float current(double seconds) {
        float t = (float) Math.clamp((seconds - changedAt) / DURATION_SECONDS, 0, 1);
        float eased = t * t * (3 - 2 * t);
        return from + ((visible ? 1 : 0) - from) * eased;
    }
}
