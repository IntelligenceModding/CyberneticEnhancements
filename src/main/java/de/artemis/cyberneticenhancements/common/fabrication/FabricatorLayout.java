package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.menu.StationLayoutMetrics;

public final class FabricatorLayout {
    public static final int WIDTH = 664, HEIGHT = 432;
    public static final int FRAME = StationLayoutMetrics.OUTER_FRAME, GUTTER = StationLayoutMetrics.PANEL_DIVIDER;
    public static final int PANEL_Y = FRAME, PANEL_HEIGHT = 292;
    public static final int BROWSER_X = FRAME, BROWSER_WIDTH = 218;
    public static final int ASSEMBLY_X = BROWSER_X + BROWSER_WIDTH + GUTTER, ASSEMBLY_WIDTH = 412;
    public static final int INVENTORY_X = FRAME, INVENTORY_Y = PANEL_Y + PANEL_HEIGHT + GUTTER;
    public static final int INVENTORY_WIDTH = WIDTH - FRAME * 2, INVENTORY_HEIGHT = StationLayoutMetrics.INVENTORY_SECTION_HEIGHT;
    public static final int LIST_X = BROWSER_X + 8, LIST_Y = 100, LIST_WIDTH = 186, ROW_HEIGHT = 26, VISIBLE_ROWS = 7;
    public static final int SCROLL_X = BROWSER_X + BROWSER_WIDTH - 14;
    public static final int GRID_X = 280, GRID_Y = 88, GRID_STEP = 26;
    public static final int OUTPUT_X = 548, OUTPUT_Y = 140;
    public static final int ACTION_Y = 224, PROGRESS_X = 252, PROGRESS_Y = 258, PROGRESS_WIDTH = 388, STATUS_Y = 276;
    public static final int PLAYER_X = (WIDTH - 162) / 2, PLAYER_Y = INVENTORY_Y + 20, HOTBAR_Y = PLAYER_Y + 58;

    private FabricatorLayout() {
    }
}
