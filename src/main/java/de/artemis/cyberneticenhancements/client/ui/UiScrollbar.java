package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.GuiGraphics;

public final class UiScrollbar {
    public static final int WIDTH = 6;
    public static final int MIN_THUMB = 20;
    private boolean dragging;
    private double grabOffset;

    public record Track(int x, int y, int width, int height, boolean horizontal, double max,
                        int thumbStart, int thumbLength) {
        public int start() {
            return horizontal ? x : y;
        }

        public int length() {
            return horizontal ? width : height;
        }

        public int travel() {
            return Math.max(0, length() - thumbLength);
        }

        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }
    }

    public static Track layout(int x, int y, int width, int height, boolean horizontal,
                               double visible, double max, double value) {
        int length = Math.max(0, horizontal ? width : height);
        max = Math.max(0, max);
        int thumb = max == 0 ? length : Math.min(length, Math.max(MIN_THUMB,
                (int) Math.round(length * Math.max(0, visible) / Math.max(1, visible + max))));
        int start = horizontal ? x : y;
        int offset = max == 0 ? 0 : (int) Math.round(Math.clamp(value / max, 0, 1) * (length - thumb));
        return new Track(x, y, Math.max(0, width), Math.max(0, height), horizontal, max, start + offset, thumb);
    }

    public static void draw(GuiGraphics graphics, Track track, double mouseX, double mouseY, boolean dragging) {
        if (track.max <= 0 || track.width <= 0 || track.height <= 0) return;
        graphics.fill(track.x, track.y, track.x + track.width, track.y + track.height, UiRenderer.FIELD);
        UiRenderer.outline(graphics, track.x, track.y, track.width, track.height, UiRenderer.FIELD_EDGE);
        int x = track.horizontal ? track.thumbStart : track.x;
        int y = track.horizontal ? track.y : track.thumbStart;
        int width = track.horizontal ? track.thumbLength : track.width;
        int height = track.horizontal ? track.height : track.thumbLength;
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        int fill = dragging ? UiRenderer.PANEL_EDGE_ACTIVE : hovered ? UiRenderer.ROW_SELECTED : UiRenderer.ROW;
        int edge = dragging ? UiRenderer.ACCENT : hovered ? UiRenderer.PANEL_EDGE_ACTIVE : UiRenderer.FIELD_EDGE;
        graphics.fill(x, y, x + width, y + height, fill);
        UiRenderer.outline(graphics, x, y, width, height, edge);
    }

    public boolean begin(Track track, double mouseX, double mouseY) {
        if (track.max <= 0 || !track.contains(mouseX, mouseY)) return false;
        double pointer = track.horizontal ? mouseX : mouseY;
        grabOffset = pointer >= track.thumbStart && pointer < track.thumbStart + track.thumbLength
                ? pointer - track.thumbStart : track.thumbLength / 2.0;
        dragging = true;
        return true;
    }

    public double value(Track track, double mouseX, double mouseY) {
        if (track.travel() == 0 || track.max <= 0) return 0;
        double pointer = track.horizontal ? mouseX : mouseY;
        return Math.clamp((pointer - track.start() - grabOffset) / track.travel(), 0, 1) * track.max;
    }

    public boolean dragging() {
        return dragging;
    }

    public void release() {
        dragging = false;
    }
}
