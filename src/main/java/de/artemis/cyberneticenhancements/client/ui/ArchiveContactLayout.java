package de.artemis.cyberneticenhancements.client.ui;

 public final class ArchiveContactLayout {
    public static final int GAP = 10, PADDING = 10, CARD_HEIGHT = 156;
    private static final int MIN_CARD_WIDTH = 360;

    public record Bounds(int x, int y, int width, int height) {}

    private ArchiveContactLayout() {}

    public static int columns(int width) {
        return Math.max(1, Math.min(2, (width + GAP) / (MIN_CARD_WIDTH + GAP)));
    }

    public static Bounds card(int index, int width) {
        int columns = columns(width);
        int cardWidth = (width - (columns - 1) * GAP) / columns;
        return new Bounds(index % columns * (cardWidth + GAP), index / columns * (CARD_HEIGHT + GAP), cardWidth, CARD_HEIGHT);
    }

    public static int contentHeight(int count, int width) {
        int rows = (count + columns(width) - 1) / columns(width);
        return rows == 0 ? 0 : rows * CARD_HEIGHT + (rows - 1) * GAP;
    }

    public static Bounds action(int index, int cardWidth) {
        double stride = (cardWidth - PADDING * 2 + GAP) / 4.0;
        int left = PADDING + (int) Math.round(index * stride);
        int right = PADDING + (int) Math.round((index + 1) * stride) - GAP;
        return new Bounds(left, CARD_HEIGHT - PADDING - 20, right - left, 20);
    }
}
