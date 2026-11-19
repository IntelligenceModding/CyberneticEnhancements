package de.artemis.cyberneticenhancements.client.ui;

import java.util.random.RandomGenerator;

public final class SlideshowFrame {
    private static final long INTERVAL = 12_500;
    private static final long CROSSFADE = 1_500;
    private final SlideshowOrder order;
    private int current, next;
    private long lastFrame = -1, elapsed;

    public SlideshowFrame(int count, RandomGenerator random) {
        order = new SlideshowOrder(count, random);
        current = order.next();
        next = order.next();
    }

    public void advance(long now) {
        if (lastFrame >= 0) {
            long delta = now - lastFrame;
            // Pause after a gap without menu ticks/renders instead of skipping unseen images.
            if (delta >= 0 && delta <= 1_000) elapsed += delta;
        }
        lastFrame = now;
        if (elapsed >= INTERVAL) {
            elapsed -= INTERVAL;
            current = next;
            next = order.next();
        }
    }

    public int current() { return current; }
    public int next() { return next; }

    public float blend() {
        float value = Math.clamp((elapsed - (INTERVAL - CROSSFADE)) / (float) CROSSFADE, 0, 1);
        return value * value * (3 - 2 * value);
    }

    public record Crop(int x, int y, int width, int height) {}

    public static Crop cover(int imageWidth, int imageHeight, int screenWidth, int screenHeight) {
        if (imageWidth <= 0 || imageHeight <= 0 || screenWidth <= 0 || screenHeight <= 0)
            throw new IllegalArgumentException("Dimensions must be positive");
        double scale = Math.max(screenWidth / (double) imageWidth, screenHeight / (double) imageHeight);
        int width = Math.clamp((int) Math.round(screenWidth / scale), 1, imageWidth);
        int height = Math.clamp((int) Math.round(screenHeight / scale), 1, imageHeight);
        return new Crop((imageWidth - width) / 2, (imageHeight - height) / 2, width, height);
    }
}
