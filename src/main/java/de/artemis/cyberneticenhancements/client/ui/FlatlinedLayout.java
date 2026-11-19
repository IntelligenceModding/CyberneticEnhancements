package de.artemis.cyberneticenhancements.client.ui;

public record FlatlinedLayout(int left, int columnWidth, int titleY, int skullScale, int buttonTop, int height) {
    public static FlatlinedLayout of(int width, int height) {
        int columnWidth = Math.min(200, Math.max(1, width - 24));
        int left = width / 2 - columnWidth / 2;
        int skullScale = height >= 260 ? 4 : height >= 220 ? 3 : 1;
        int titleY = height >= 220 ? 26 + 16 * skullScale : 38;
        int buttonTop = Math.min(height - 80, Math.max(height / 4 + 72, titleY + 78));
        titleY = Math.min(titleY, buttonTop - 62);
        return new FlatlinedLayout(left, columnWidth, titleY, skullScale, buttonTop, height);
    }

    public int center() {
        return left + columnWidth / 2;
    }

    public int causeY() {
        return titleY + (height < 200 ? 30 : 35);
    }

    public int scoreY() {
        return titleY + (height < 200 ? 44 : 50);
    }

    public int causeCenter(int textWidth, int screenWidth) {
        return screenWidth / 2;
    }

    public int skullY() {
        return titleY - 16 * skullScale - 10;
    }
}
