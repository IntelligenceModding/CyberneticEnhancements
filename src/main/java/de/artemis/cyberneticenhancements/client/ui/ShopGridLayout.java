package de.artemis.cyberneticenhancements.client.ui;

public record ShopGridLayout(int itemCount, int width, int height, int minimumTileWidth, int tileHeight, int gap, int scrollbarWidth) {
    public ShopGridLayout {
        itemCount = Math.max(0, itemCount);
        width = Math.max(1, width);
        height = Math.max(0, height);
        minimumTileWidth = Math.max(1, minimumTileWidth);
        tileHeight = Math.max(1, tileHeight);
        gap = Math.max(0, gap);
        scrollbarWidth = Math.max(0, scrollbarWidth);
    }

    public int columns() {
        return Math.max(1, Math.min(2, (width - scrollbarWidth) / (minimumTileWidth + gap)));
    }

    public int rows() {
        return (int) ((itemCount + (long) columns() - 1) / columns());
    }

    public int visibleRows() {
        return Math.min(rows(), Math.max(1, (height + gap) / (tileHeight + gap)));
    }

    public int maxScroll() {
        return Math.max(0, rows() - visibleRows());
    }

    public int clampScroll(int scroll) {
        return Math.max(0, Math.min(maxScroll(), scroll));
    }

    public int contentWidth() {
        return Math.max(1, width - (maxScroll() > 0 ? scrollbarWidth + gap : 0));
    }

    public int tileWidth() {
        return Math.max(1, (contentWidth() - (columns() - 1) * gap) / columns());
    }

    public int tileX(int index) {
        int rowItems = Math.min(columns(), itemCount - index / columns() * columns());
        int rowWidth = rowItems * tileWidth() + (rowItems - 1) * gap;
        return (contentWidth() - rowWidth) / 2 + index % columns() * (tileWidth() + gap);
    }

    public int tileY(int index, int scroll) {
        return (index / columns() - clampScroll(scroll)) * (tileHeight + gap);
    }

    public int firstSlot(int scroll) {
        return clampScroll(scroll) * columns();
    }

    public int lastSlot(int scroll) {
        return Math.min(itemCount, firstSlot(scroll) + visibleRows() * columns());
    }
}
