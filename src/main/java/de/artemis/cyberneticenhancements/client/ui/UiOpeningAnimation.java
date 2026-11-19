package de.artemis.cyberneticenhancements.client.ui;

public final class UiOpeningAnimation {
    private static final double[] LOADING_TIMES = {0, 0.12, 0.22, 0.37, 0.53, 0.66, 0.77, 0.90, 0.94, 1};
    private static final float[] LOADING_PROGRESS = {0, 0.16F, 0.16F, 0.48F, 0.53F, 0.80F, 0.80F, 0.96F, 0.96F, 1};
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
        if (!enabled) frame = new Frame(1, 1, 0, 1, 1, 0, 1, 0, true);
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
        frame = new Frame(loadingProgress(elapsed / bootMillis), (float) Math.clamp(elapsed / bootMillis, 0, 1), 1.0F - exit, entry,
                0.975F + 0.025F * entry, 12.0F * (1.0F - entry),
                1.0F - 0.04F * exit, -10.0F * exit, handoff >= transitionMillis);
        return frame;
    }

    private static float loadingProgress(double value) {
        double progress = Math.clamp(value, 0.0, 1.0);
        for (int index = 1; index < LOADING_TIMES.length; index++) {
            if (progress <= LOADING_TIMES[index]) {
                float blend = smootherStep((progress - LOADING_TIMES[index - 1]) / (LOADING_TIMES[index] - LOADING_TIMES[index - 1]));
                return LOADING_PROGRESS[index - 1] + (LOADING_PROGRESS[index] - LOADING_PROGRESS[index - 1]) * blend;
            }
        }
        return 1;
    }

    private static float smootherStep(double value) {
        double t = Math.clamp(value, 0.0, 1.0);
        return (float) (t * t * t * (t * (t * 6 - 15) + 10));
    }

    public record Frame(float bootProgress, float bootLinearProgress, float bootOpacity, float wikiOpacity, float wikiScale,
                        float wikiOffsetY, float bootScale, float bootOffsetY, boolean complete) {
    }
}
