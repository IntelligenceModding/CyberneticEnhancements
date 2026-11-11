package de.artemis.cyberneticenhancements.client.ui;

public final class UiOpeningAnimation {
    private final long startedAtNanos;
    private final long bootMillis;
    private final long transitionMillis;
    private long elapsedNanos;
    private Frame frame;

    public UiOpeningAnimation(long startedAtNanos, long bootMillis, long transitionMillis) {
        this(startedAtNanos, bootMillis, transitionMillis, true);
    }

    public UiOpeningAnimation(long startedAtNanos, long bootMillis, long transitionMillis, boolean enabled) {
        if (bootMillis < 1 || transitionMillis < 1) {
            throw new IllegalArgumentException("Opening animation durations must be positive");
        }
        this.startedAtNanos = startedAtNanos;
        this.bootMillis = bootMillis;
        this.transitionMillis = transitionMillis;
        if (!enabled) frame = new Frame(1, 0, 1, 1, 0, 1, 0, true);
    }

    public Frame sample(long nowNanos) {
        if (frame != null && frame.complete()) {
            return frame;
        }
        elapsedNanos = Math.max(elapsedNanos, Math.max(0, nowNanos - startedAtNanos));
        double elapsed = elapsedNanos / 1_000_000.0;
        double handoff = elapsed - bootMillis;
        double entryDelay = Math.min(80.0, transitionMillis * 0.2);
        float exit = smootherStep(handoff / Math.min(260.0, transitionMillis));
        float entry = smootherStep((handoff - entryDelay) / (transitionMillis - entryDelay));
        frame = new Frame(unit(elapsed / bootMillis), 1.0F - exit, entry,
                0.975F + 0.025F * entry, 12.0F * (1.0F - entry),
                1.0F - 0.04F * exit, -10.0F * exit, handoff >= transitionMillis);
        return frame;
    }

    private static float unit(double value) {
        return (float) Math.clamp(value, 0.0, 1.0);
    }

    private static float smootherStep(double value) {
        double t = Math.clamp(value, 0.0, 1.0);
        return (float) (t * t * t * (t * (t * 6 - 15) + 10));
    }

    public record Frame(float bootProgress, float bootOpacity, float wikiOpacity, float wikiScale,
                        float wikiOffsetY, float bootScale, float bootOffsetY, boolean complete) {
    }
}
