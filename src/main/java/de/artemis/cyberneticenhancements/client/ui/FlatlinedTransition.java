package de.artemis.cyberneticenhancements.client.ui;

public final class FlatlinedTransition {
    public static final int BANDS = 24;

    private FlatlinedTransition() {
    }

    public static double intensity(double seconds) {
        return ease(seconds / 1.8);
    }

    public static double coverage(double seconds, int band) {
        int order = Math.floorMod(band * 17 + 5, BANDS);
        double delay = 0.04 + order / (double) (BANDS - 1) * 0.9;
        double duration = 0.45 + Math.floorMod(band * 7, 5) * 0.1;
        return ease((seconds - delay) / duration);
    }

    private static double ease(double value) {
        double clamped = Math.max(0, Math.min(1, value));
        return clamped * clamped * (3 - 2 * clamped);
    }
}
