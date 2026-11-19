package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import de.artemis.cyberneticenhancements.common.menu.StationInventoryLayout;
import static org.junit.Assert.*;
import static de.artemis.cyberneticenhancements.common.fabrication.FabricatorLayout.*;

public class NativeStationInventoryTest {
    private static final StationInventoryLayout LAYOUT = new StationInventoryLayout(WIDTH, HEIGHT);

    @Test public void allStationsUseConfiguredGuiScaleAndOnlyShrinkWhenTheyCannotFit() {
        for (int[] size : new int[][]{{664, 316, 1}, {540, 322, 1}, {de.artemis.cyberneticenhancements.common.menu.RipperStationLayout.IMAGE_WIDTH, de.artemis.cyberneticenhancements.common.menu.RipperStationLayout.IMAGE_HEIGHT, 1}}) {
            assertEquals(1, StationGuiScale.fit(960, 600, size[0], size[1], size[2] == 1).renderScale(), 0);
            for (int[] gui : new int[][]{{1920, 1080}, {960, 540}, {640, 360}, {427, 240}}) {
                boolean inventory = size[2] == 1;
                var viewport = StationGuiScale.fit(gui[0], gui[1], size[0], size[1], inventory);
                double scale = viewport.renderScale();
                assertTrue(scale <= 1 && size[0] * scale <= gui[0] - 24 + .001);
                var layout = new StationInventoryLayout(size[0], size[1]);
                int imageHeight = inventory ? layout.y() + (int) Math.ceil(100 / scale) : size[1];
                int top = (viewport.height() - imageHeight) / 2;
                assertTrue(top >= 22);
                if (inventory) {
                    var panel = NativeStationInventory.at(layout, (viewport.width() - size[0]) / 2, top, scale);
                    assertTrue(panel.y() + 100 <= gui[1]);
                    assertEquals(18, panel.slotX(layout.inventoryX() + 18) - panel.slotX(layout.inventoryX()));
                    assertEquals(8, panel.slotX(layout.inventoryX()) - panel.x());
                    assertEquals(18, panel.slotY(layout.inventoryY()) - panel.y());
                    assertEquals(22, panel.slotY(layout.hotbarY()) - panel.slotY(layout.inventoryY() + 36));
                    double mouseX = (panel.slotX(layout.inventoryX()) + 8) / scale;
                    double mouseY = (panel.slotY(layout.inventoryY()) + 8) / scale;
                    assertTrue(panel.hovering(layout.inventoryX(), layout.inventoryY(), mouseX, mouseY, scale));
                } else assertTrue((top + imageHeight) * scale <= gui[1]);
            }
        }
    }
    @Test public void inventoryUsesNativePixelsAndHitboxesAcrossGuiScales() {
        for (int guiScale : new int[]{1, 2, 3, 4}) {
            int guiWidth = (int) Math.ceil(1920D / guiScale), guiHeight = (int) Math.ceil(1080D / guiScale);
            var viewport = StationGuiScale.fit(guiWidth, guiHeight, WIDTH, HEIGHT, true);
            double scale = viewport.renderScale();
            int totalHeight = INVENTORY_Y + (int) Math.ceil(INVENTORY_HEIGHT / scale);
            int left = (viewport.width() - WIDTH) / 2, top = (viewport.height() - totalHeight) / 2;
            var panel = NativeStationInventory.at(LAYOUT, left, top, scale);
            assertTrue(panel.x() >= 0 && panel.x() + 176 <= guiWidth);
            assertTrue(panel.y() > (top + HEIGHT) * scale);
            assertTrue(panel.y() + 100 <= guiHeight);
            assertEquals(8, panel.slotX(PLAYER_INVENTORY_X) - panel.x());
            assertEquals(18, panel.slotY(PLAYER_INVENTORY_Y) - panel.y());
            assertEquals(18, panel.slotX(PLAYER_INVENTORY_X + 18) - panel.slotX(PLAYER_INVENTORY_X));
            assertEquals(22, panel.slotY(HOTBAR_Y) - panel.slotY(PLAYER_INVENTORY_Y + 36));
            for (int column = 0; column < 9; column++) for (int row = 0; row < 4; row++) {
                int menuX = PLAYER_INVENTORY_X + column * 18;
                int menuY = row == 3 ? HOTBAR_Y : PLAYER_INVENTORY_Y + row * 18;
                double centerX = (panel.slotX(menuX) + 8) / scale, centerY = (panel.slotY(menuY) + 8) / scale;
                assertTrue(panel.hovering(menuX, menuY, centerX, centerY, scale));
                assertFalse(panel.hovering(menuX, menuY, (panel.slotX(menuX) + 18) / scale, centerY, scale));
                assertTrue(panel.contains(centerX, centerY, scale));
            }
        }
    }

    @Test public void shortWindowsLeaveRoomForTheUnscaledInventoryAndMachineTitle() {
        var viewport = StationGuiScale.fit(427, 240, WIDTH, HEIGHT, true);
        int totalHeight = INVENTORY_Y + (int) Math.ceil(INVENTORY_HEIGHT / viewport.renderScale());
        int top = (viewport.height() - totalHeight) / 2;
        var panel = NativeStationInventory.at(LAYOUT, (viewport.width() - WIDTH) / 2, top, viewport.renderScale());
        assertTrue(top >= 22);
        assertTrue(panel.y() + INVENTORY_HEIGHT <= 240);
        assertFalse(panel.contains(panel.x() / viewport.renderScale(), (panel.y() - 1) / viewport.renderScale(), viewport.renderScale()));
    }
}
