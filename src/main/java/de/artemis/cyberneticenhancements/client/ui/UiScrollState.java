package de.artemis.cyberneticenhancements.client.ui;

public class UiScrollState {
    public static final double WHEEL_STEP = 24;
    public double current, target;

    public void tick() {
        current += (target - current) * 0.35;
        if (Math.abs(current - target) < 0.5) current = target;
    }

    public void add(double amount, double max) {
        target = Math.clamp(target + amount, 0, Math.max(0, max));
    }

    public void clamp(double max) {
        max = Math.max(0, max);
        current = Math.clamp(current, 0, max);
        target = Math.clamp(target, 0, max);
    }

    public void set(double value, double max) {
        current = target = Math.clamp(value, 0, Math.max(0, max));
    }

    public void reset() {
        current = target = 0;
    }
}
