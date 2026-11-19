package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.common.menu.StationInventoryLayout;

public final class StationGuiScale {
    public static UiViewport fit(int guiWidth, int guiHeight, int machineWidth, int machineHeight, boolean inventory) {
        double availableHeight = guiHeight - (inventory ? StationInventoryLayout.HEIGHT : 0);
        double contentHeight = machineHeight + 48D + (inventory ? StationInventoryLayout.GAP : 0);
        double scale = Math.max(.01, Math.min(1, Math.min((guiWidth - 24D) / machineWidth, availableHeight / contentHeight)));
        return new UiViewport((int) Math.ceil(guiWidth / scale), (int) Math.ceil(guiHeight / scale), scale);
    }
    private StationGuiScale() {}
}
