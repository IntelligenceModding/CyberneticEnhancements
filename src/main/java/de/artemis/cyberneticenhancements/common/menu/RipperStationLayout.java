package de.artemis.cyberneticenhancements.common.menu;

public final class RipperStationLayout {
    public static final int IMAGE_WIDTH = 800;
    public static final int PANEL_X = StationLayoutMetrics.OUTER_FRAME, PANEL_Y = StationLayoutMetrics.OUTER_FRAME;
    public static final int PANEL_GAP = StationLayoutMetrics.PANEL_DIVIDER, PANEL_PADDING = 8;
    public static final int CENTER_WIDTH = 400;
    public static final int SIDE_WIDTH = (IMAGE_WIDTH - 2 * PANEL_X - 2 * PANEL_GAP - CENTER_WIDTH) / 2;
    public static final int CENTER_X = PANEL_X + SIDE_WIDTH + PANEL_GAP;
    public static final int RIGHT_X = CENTER_X + CENTER_WIDTH + PANEL_GAP;
    public static final int TOP_HEIGHT = 236, LOWER_Y = PANEL_Y + TOP_HEIGHT + PANEL_GAP;
    public static final int LOWER_HEIGHT = 70;
    public static final int STATUS_X = PANEL_X;
    public static final int STATUS_LABEL_Y = LOWER_Y + LOWER_HEIGHT + PANEL_GAP;
    public static final int STATUS_BAR_Y = STATUS_LABEL_Y + 16;
    public static final int STATUS_BAR_WIDTH = (IMAGE_WIDTH - 2 * STATUS_X - PANEL_GAP) / 2;
    public static final int IMAGE_HEIGHT = STATUS_BAR_Y + 10 + StationLayoutMetrics.BOTTOM_FRAME;

    public static final int[] CYBERWARE_SLOT_X = {
            370, 392, 414,
            392,
            526,
            258,
            515,
            504, 526,
            504, 526, 548,
            236, 258, 280,
            236, 258, 280,
            258,
            537,
            548
    };
    public static final int[] CYBERWARE_SLOT_Y = {
            214, 214, 214,
            46,
            46,
            108,
            108,
            214, 214,
            164, 164, 164,
            164, 164, 164,
            214, 214, 214,
            46,
            108,
            214
    };

    public static final int[] CHIP_CLUSTER_X = {236, 370, 504};
    public static final int[] CHIP_SLOT_OFFSET_X = {0, 22, 44};
    public static final int CHIP_SLOT_Y = 304;

    public static final int[] ARM_MODULE_X = {71, 93, 115};
    public static final int ARM_MODULE_Y = 304;

    public static final int[] LEG_MODULE_X = {669, 691, 713};
    public static final int LEG_MODULE_Y = 304;

    public static final StationInventoryLayout PLAYER_INVENTORY = new StationInventoryLayout(IMAGE_WIDTH, IMAGE_HEIGHT);
    public static final int PLAYER_INVENTORY_X = PLAYER_INVENTORY.inventoryX();
    public static final int PLAYER_INVENTORY_Y = PLAYER_INVENTORY.inventoryY();
    public static final int PLAYER_HOTBAR_Y = PLAYER_INVENTORY.hotbarY();
    public static final int PLAYER_SLOT_SPACING = AbstractBaseMenu.SLOT_SPACING;

    private RipperStationLayout() {
    }
}
