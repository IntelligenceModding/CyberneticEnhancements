package de.artemis.cyberneticenhancements.client.ui;

/** Shared edges and spacing for the journal toolbar, body and footer. */
public record TitleJournalLayout(int width, int height) {
    public static final int MARGIN = 44, GAP = 10, PAGE_SIZE = 20;
    public int right() { return width - MARGIN; }
    public int top() { return 104; }
    public int bodyHeight() { return height - 192; }
    public int scrollbarX() { return right() - UiScrollbar.WIDTH; }
    public int contentWidth() { return contentWidth(true); }
    public int contentWidth(boolean scrolling) { return right() - MARGIN - (scrolling ? GAP + UiScrollbar.WIDTH : 0); }
    public int orderX() { return right() - 128; }
    public int categoryX() { return orderX() - GAP - 190; }
    public int searchWidth(boolean reading) { return (reading ? categoryX() - GAP : right()) - MARGIN; }
    public static int pageCount(int entries) { return Math.max(1, (entries + PAGE_SIZE - 1) / PAGE_SIZE); }
    public static int pageStart(int entries, int page) { return Math.clamp(page, 0, pageCount(entries) - 1) * PAGE_SIZE; }
}
