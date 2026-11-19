package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.common.menu.StationInventoryLayout;

/** Inventory pixels use Minecraft's GUI scale even when a large station must shrink to fit. */
public record NativeStationInventory(StationInventoryLayout layout, int x, int y) {
    public static NativeStationInventory at(StationInventoryLayout layout, int left, int top, double scale) {
        return new NativeStationInventory(layout,
                (int) Math.round((left + layout.machineWidth() / 2D) * scale - StationInventoryLayout.WIDTH / 2D),
                (int) Math.round((top + layout.y()) * scale));
    }
    public int slotX(int menuX) { return x + menuX - layout.x(); }
    public int slotY(int menuY) { return y + menuY - layout.y(); }
    public boolean contains(double uiX, double uiY, double scale) {
        double px = uiX * scale, py = uiY * scale;
        return px >= x && px < x + StationInventoryLayout.WIDTH && py >= y && py < y + StationInventoryLayout.HEIGHT;
    }
    public boolean hovering(int menuX, int menuY, double uiX, double uiY, double scale) {
        double px = uiX * scale, py = uiY * scale;
        return px >= slotX(menuX) - 1 && px < slotX(menuX) + 17
                && py >= slotY(menuY) - 1 && py < slotY(menuY) + 17;
    }
}
