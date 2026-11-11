package de.artemis.cyberneticenhancements.client.ui;

public final class UiMotion {
    private long lastUpdate = System.nanoTime();
    private float amount;

    public float update(boolean highlighted) {
        long now = System.nanoTime();
        double elapsed = Math.min(0.1, Math.max(0, (now - lastUpdate) / 1_000_000_000.0));
        lastUpdate = now;
        amount += ((highlighted ? 1 : 0) - amount) * (float) (1 - Math.exp(-elapsed * 18));
        return amount;
    }
}
