package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.fabrication.FabricatorLayout;

/** Shared Upgrade/Repair layout: one implant, a materials column and a vertical status gauge. */
public final class UpgradeStationLayout {
    public static final int IMAGE_WIDTH = RecyclerStationLayout.IMAGE_WIDTH, IMAGE_HEIGHT = FabricatorLayout.HEIGHT;
    public static final StationInventoryLayout PLAYER_INVENTORY = new StationInventoryLayout(IMAGE_WIDTH, IMAGE_HEIGHT);
    public static final int PANEL_X = RecyclerStationLayout.PANEL_X, PANEL_Y = RecyclerStationLayout.PANEL_Y;
    public static final int PANEL_WIDTH = RecyclerStationLayout.PANEL_WIDTH, PANEL_HEIGHT = RecyclerStationLayout.PANEL_HEIGHT;
    public static final int FRAME_SIZE = FabricatorLayout.OUTPUT_FRAME_SIZE, ITEM_SIZE = FabricatorLayout.OUTPUT_ITEM_SIZE;
    public static final int CONTROL_GAP = 10;
    public static final int INPUT_FRAME_X = (IMAGE_WIDTH - FRAME_SIZE) / 2, FRAME_Y = RecyclerStationLayout.INPUT_FRAME_Y;
    public static final int INPUT_X = INPUT_FRAME_X + FRAME_SIZE / 2 - 8, INPUT_Y = FRAME_Y + FRAME_SIZE / 2 - 8;
    public static final int MATERIAL_FRAME_SIZE = (FRAME_SIZE - CONTROL_GAP) / 2, MATERIAL_ITEM_SIZE = 40;
    public static final int MATERIAL_FRAME_X = INPUT_FRAME_X + FRAME_SIZE + CONTROL_GAP;
    public static final int[] MATERIAL_FRAME_Y = {FRAME_Y, FRAME_Y + MATERIAL_FRAME_SIZE + CONTROL_GAP};
    public static final int MATERIAL_SLOT_X = MATERIAL_FRAME_X + MATERIAL_FRAME_SIZE / 2 - 8;
    public static final int[] MATERIAL_X = {MATERIAL_SLOT_X, MATERIAL_SLOT_X};
    public static final int[] MATERIAL_Y = {MATERIAL_FRAME_Y[0] + MATERIAL_FRAME_SIZE / 2 - 8, MATERIAL_FRAME_Y[1] + MATERIAL_FRAME_SIZE / 2 - 8};
    public static final int MATERIAL_CENTER = MATERIAL_SLOT_X + 8;
    public static final int LEVEL_WIDTH = 18;
    public static final int LEVEL_X = INPUT_FRAME_X - CONTROL_GAP - LEVEL_WIDTH + 1;
    public static final int LABEL_Y = FabricatorLayout.ASSEMBLY_LABEL_Y, ACTION_Y = FabricatorLayout.ACTION_Y;
    public static final int PROGRESS_X = RecyclerStationLayout.PROGRESS_X, PROGRESS_Y = FabricatorLayout.PROGRESS_Y;
    public static final int PROGRESS_WIDTH = FabricatorLayout.PROGRESS_WIDTH;
    public static final int BENEFITS_X = PROGRESS_X;
    public static final int BENEFITS_WIDTH = LEVEL_X - 1 - CONTROL_GAP - BENEFITS_X;
    public static final int BENEFITS_Y = FRAME_Y;
    public static final int BENEFITS_HEIGHT = FRAME_SIZE;
    private UpgradeStationLayout() {}
}
