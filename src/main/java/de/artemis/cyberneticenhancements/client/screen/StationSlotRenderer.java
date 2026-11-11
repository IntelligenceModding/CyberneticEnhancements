package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.menu.AbstractBaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;

final class StationSlotRenderer {
    static final int SLOT_SIZE = 16;
    static final int SLOT_FRAME_SIZE = AbstractBaseMenu.SLOT_SPACING;
    static final int UPGRADE_SLOT_FRAME_SIZE = SLOT_FRAME_SIZE + 2;

    private static final int SLOT_FRAME_INSET = (SLOT_FRAME_SIZE - SLOT_SIZE) / 2;
    private static final int UPGRADE_FRAME_PADDING = (UPGRADE_SLOT_FRAME_SIZE - SLOT_FRAME_SIZE) / 2;

    private StationSlotRenderer() {
    }

    static void drawStandardSlot(GuiGraphics guiGraphics, int leftPos, int topPos, int x, int y, int fill) {
        int drawX = leftPos + x - SLOT_FRAME_INSET;
        int drawY = topPos + y - SLOT_FRAME_INSET;
        UiRenderer.panel(guiGraphics, drawX, drawY, SLOT_FRAME_SIZE, SLOT_FRAME_SIZE, fill, UiRenderer.FIELD_EDGE);
    }

    static void drawUpgradeableSlot(GuiGraphics guiGraphics, int leftPos, int topPos, int x, int y, int fill, int outline) {
        drawStandardSlot(guiGraphics, leftPos, topPos, x, y, fill);
        int frameX = leftPos + x - SLOT_FRAME_INSET - UPGRADE_FRAME_PADDING;
        int frameY = topPos + y - SLOT_FRAME_INSET - UPGRADE_FRAME_PADDING;
        drawPixelFrame(guiGraphics, frameX, frameY, UPGRADE_SLOT_FRAME_SIZE, UPGRADE_SLOT_FRAME_SIZE, outline);
    }

    static int getTierFrameColor(CyberwareTier tier) {
        return switch (tier) {
            case TIER_1 -> 0xFF4A5662;
            case TIER_2 -> 0xFF3FAE6A;
            case TIER_3 -> 0xFF35A8E0;
            case TIER_4 -> 0xFFAA63E8;
            case TIER_5 -> 0xFFE2A93A;
        };
    }

    private static void drawPixelFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, color);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
