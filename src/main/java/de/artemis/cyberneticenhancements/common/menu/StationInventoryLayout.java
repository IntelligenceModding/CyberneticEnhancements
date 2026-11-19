package de.artemis.cyberneticenhancements.common.menu;

/** The supplied vanilla inventory texture is 176 x 100, with its first item row at (8, 18). */
public record StationInventoryLayout(int machineWidth, int machineHeight) {
    public static final int WIDTH = 176, HEIGHT = 100, GAP = 12;
    public int x() { return (machineWidth - WIDTH) / 2; }
    public int y() { return machineHeight + GAP; }
    public int inventoryX() { return x() + 8; }
    public int inventoryY() { return y() + 18; }
    public int hotbarY() { return y() + 76; }
}
