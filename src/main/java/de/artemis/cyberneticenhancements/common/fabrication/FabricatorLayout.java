package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.menu.StationLayoutMetrics;

public final class FabricatorLayout {
    public static final int WIDTH = 664, HEIGHT = 316;
    public static final int INVENTORY_WIDTH = 176, INVENTORY_HEIGHT = 100;
    public static final int INVENTORY_X = (WIDTH - INVENTORY_WIDTH) / 2, INVENTORY_Y = HEIGHT + 12;
    public static final int PLAYER_INVENTORY_X = INVENTORY_X + 8, PLAYER_INVENTORY_Y = INVENTORY_Y + 18;
    public static final int PLAYER_SLOT_SPACING = 18, HOTBAR_Y = INVENTORY_Y + 76;
    public static final int TOTAL_HEIGHT = INVENTORY_Y + INVENTORY_HEIGHT;
    public static final int FRAME = StationLayoutMetrics.OUTER_FRAME, GUTTER = StationLayoutMetrics.PANEL_DIVIDER;
    public static final int PANEL_Y = FRAME, PANEL_HEIGHT = 292;
    public static final int BROWSER_X = FRAME, BROWSER_WIDTH = 218;
    public static final int ASSEMBLY_X = BROWSER_X + BROWSER_WIDTH + GUTTER, ASSEMBLY_WIDTH = 412;
    public static final int LIST_X = BROWSER_X + 8, LIST_Y = 48, LIST_WIDTH = 186, ROW_HEIGHT = 27;
    public static final int LIST_HEIGHT = PANEL_Y + PANEL_HEIGHT - 8 - LIST_Y;
    public static final int SCROLL_X = BROWSER_X + BROWSER_WIDTH - 14;
    public static final int HEADER_Y = 20, HEADER_HEIGHT = 20;
    public static final int GRID_STEP = 26;
    public static final int OUTPUT_FRAME_SIZE = 4 * GRID_STEP + 18, OUTPUT_ITEM_SIZE = 96;
    // Center both equally sized columns, with a clear gap between them.
    public static final int ASSEMBLY_COLUMN_GAP = 64;
    public static final int GRID_X = ASSEMBLY_X + (ASSEMBLY_WIDTH - 2 * OUTPUT_FRAME_SIZE - ASSEMBLY_COLUMN_GAP) / 2 + 1;
    public static final int GRID_Y = 124, ASSEMBLY_LABEL_Y = GRID_Y - 17;
    public static final int OUTPUT_X = GRID_X + 2 * GRID_STEP + OUTPUT_FRAME_SIZE + ASSEMBLY_COLUMN_GAP;
    public static final int OUTPUT_Y = GRID_Y + 2 * GRID_STEP;
    public static final int OUTPUT_FRAME_X = OUTPUT_X + 8 - OUTPUT_FRAME_SIZE / 2;
    public static final int OUTPUT_FRAME_Y = OUTPUT_Y + 8 - OUTPUT_FRAME_SIZE / 2;
    public static final int ACTION_Y = 48, PROGRESS_X = 252, PROGRESS_Y = 286, PROGRESS_WIDTH = 388;

    private FabricatorLayout() {
    }
}
