package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

public final class TemporalBudget {
    private double accumulated;

    public TemporalBudget() {
    }

    public TemporalBudget(boolean immediate) { accumulated = immediate ? 1 : 0; }

    public float renderPartial(float partialTick, double scale) {
        return (float) Math.clamp(accumulated + partialTick * scale, 0, 1);
    }

    public boolean advance(double scale) {
        if (!Double.isFinite(scale) || scale >= 1) {
            accumulated = 0;
            return true;
        }
        accumulated += Math.clamp(scale, 0.05, 1);
        if (accumulated + 1.0E-9 >= 1) {
            accumulated = Math.max(0, accumulated - 1);
            return true;
        }
        return false;
    }
}
