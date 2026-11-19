package de.artemis.cyberneticenhancements.client.ui;

public final class FlatlinedDeathMotion {
    private FlatlinedDeathMotion() {
    }

    public static Pose sample(double seconds) {
        double progress = Math.max(0, Math.min(1, seconds / 0.95));
        double tilt = progress * progress * (3 - 2 * progress);
        double fall = progress * progress;
        double impactTime = Math.max(0, seconds - 0.95);
        double impact = Math.sin(impactTime * 23) * Math.exp(-impactTime * 14);
        return new Pose(tilt, fall, Math.sin(progress * Math.PI) * -5 + impact * 0.8, impact * 1.5);
    }

    public record Pose(double tilt, double fall, double roll, double impact) {
    }
}
