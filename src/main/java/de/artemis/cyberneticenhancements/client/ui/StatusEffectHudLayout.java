package de.artemis.cyberneticenhancements.client.ui;

public final class StatusEffectHudLayout {
    private static final int COLUMN_WIDTH = 25;
    private static final int ROW_HEIGHT = 26;
    private static final int ICON_SIZE = 24;

    private StatusEffectHudLayout() {}

    public static int rows(int height, boolean demo) {
        return Math.max(1, 1 + (height - top(demo) - ICON_SIZE) / ROW_HEIGHT);
    }

    public static int x(int width, int index, int rows) {
        return width - COLUMN_WIDTH * (index / rows + 1);
    }

    public static int y(int index, int rows, boolean demo) {
        return top(demo) + index % rows * ROW_HEIGHT;
    }

    public static int reservedWidth(int count, int rows) {
        return Math.ceilDiv(count, rows) * COLUMN_WIDTH;
    }

    private static int top(boolean demo) {
        return demo ? 16 : 1;
    }
}
