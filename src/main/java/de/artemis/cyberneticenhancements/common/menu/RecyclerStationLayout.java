package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity;
import de.artemis.cyberneticenhancements.common.fabrication.FabricatorLayout;

/** The Fabricator's finished assembly panel, with input and output roles reversed. */
public final class RecyclerStationLayout {
    public static final int PANEL_X = FabricatorLayout.FRAME, PANEL_Y = FabricatorLayout.PANEL_Y;
    public static final int PANEL_WIDTH = FabricatorLayout.ASSEMBLY_WIDTH, PANEL_HEIGHT = FabricatorLayout.PANEL_HEIGHT;
    public static final int IMAGE_WIDTH = PANEL_WIDTH + 2 * PANEL_X, IMAGE_HEIGHT = FabricatorLayout.HEIGHT;
    public static final StationInventoryLayout PLAYER_INVENTORY = new StationInventoryLayout(IMAGE_WIDTH, IMAGE_HEIGHT);
    public static final int INPUT_FRAME_SIZE = FabricatorLayout.OUTPUT_FRAME_SIZE, INPUT_ITEM_SIZE = FabricatorLayout.OUTPUT_ITEM_SIZE;
    public static final int INPUT_FRAME_X = PANEL_X + (PANEL_WIDTH - 2 * INPUT_FRAME_SIZE - FabricatorLayout.ASSEMBLY_COLUMN_GAP) / 2;
    public static final int INPUT_FRAME_Y = FabricatorLayout.GRID_Y - 1;
    public static final int INPUT_X = INPUT_FRAME_X + INPUT_FRAME_SIZE / 2 - 8, INPUT_Y = INPUT_FRAME_Y + INPUT_FRAME_SIZE / 2 - 8;
    public static final int GRID_X = INPUT_FRAME_X + INPUT_FRAME_SIZE + FabricatorLayout.ASSEMBLY_COLUMN_GAP + 1;
    public static final int GRID_Y = FabricatorLayout.GRID_Y, GRID_STEP = FabricatorLayout.GRID_STEP, GRID_COLUMNS = 5;
    public static final int LABEL_Y = FabricatorLayout.ASSEMBLY_LABEL_Y;
    public static final int RESULT_SLOT_COUNT = RecyclerStationBlockEntity.OUTPUT_COUNT;
    public static final int MACHINE_SLOT_COUNT = RecyclerStationBlockEntity.SLOT_COUNT;
    public static final int ACTION_Y = FabricatorLayout.ACTION_Y;
    public static final int PROGRESS_X = FabricatorLayout.PROGRESS_X - FabricatorLayout.ASSEMBLY_X + PANEL_X;
    public static final int PROGRESS_Y = FabricatorLayout.PROGRESS_Y, PROGRESS_WIDTH = FabricatorLayout.PROGRESS_WIDTH;
    public static final int ACTION_WIDTH = (PROGRESS_WIDTH - 12) / 2;
    public static int resultX(int index) { return GRID_X + index % GRID_COLUMNS * GRID_STEP; }
    public static int resultY(int index) { return GRID_Y + index / GRID_COLUMNS * GRID_STEP; }
    private RecyclerStationLayout() {}
}
