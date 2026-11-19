package de.artemis.cyberneticenhancements.client.ui;

public final class ArchiveContactLayout {
    public static final int GAP = ArchiveCardLayout.GAP, PADDING = ArchiveCardLayout.PADDING;
    public static final int BODY_TOP = PADDING + ArchiveCardLayout.LINE + GAP + 1 + GAP;
    public static final int PORTRAIT_SIZE = 58, FOOTER_Y = BODY_TOP + PORTRAIT_SIZE + GAP;
    public static final int BUTTON_Y = FOOTER_Y + 1 + GAP, CARD_HEIGHT = BUTTON_Y + ArchiveCardLayout.BUTTON + PADDING;
    private static final int MIN_CARD_WIDTH = 360;

    public record Bounds(int x, int y, int width, int height) {}

    private ArchiveContactLayout() {}

    public static int columns(int width) {
        return Math.max(1, Math.min(2, (width + GAP) / (MIN_CARD_WIDTH + GAP)));
    }

    public static Bounds card(int index, int width) {
        int columns = columns(width);
        int column = index % columns;
        int cardWidth = (width - GAP * (columns - 1)) / columns;
        int left = columns == 1 ? 0 : column * (width - cardWidth) / (columns - 1);
        return new Bounds(left, index / columns * (CARD_HEIGHT + GAP), cardWidth, CARD_HEIGHT);
    }

    public static int contentHeight(int count, int width) {
        int rows = (count + columns(width) - 1) / columns(width);
        return rows == 0 ? 0 : rows * CARD_HEIGHT + (rows - 1) * GAP;
    }

    public static Bounds action(int index, int cardWidth) {
        double stride = (cardWidth - PADDING * 2 + GAP) / 3.0;
        int left = PADDING + (int) Math.round(index * stride);
        int right = PADDING + (int) Math.round((index + 1) * stride) - GAP;
        return new Bounds(left, BUTTON_Y, right - left, ArchiveCardLayout.BUTTON);
    }
}
