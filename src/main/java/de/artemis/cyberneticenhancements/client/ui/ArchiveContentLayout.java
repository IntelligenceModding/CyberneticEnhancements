package de.artemis.cyberneticenhancements.client.ui;

public final class ArchiveContentLayout {
    public static final int GAP = 10;

    private ArchiveContentLayout() {}

    public static int contentX(int panelX) { return panelX + GAP; }
    public static int contentWidth(int panelWidth) { return contentWidth(panelWidth, false); }
    public static int contentWidth(int panelWidth, boolean scrolling) { return Math.max(1, panelWidth - GAP * 2 - (scrolling ? GAP + UiScrollbar.WIDTH : 0)); }
    public static int scrollbarX(int panelX, int panelWidth) { return panelX + panelWidth - GAP - UiScrollbar.WIDTH; }
}
