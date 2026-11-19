package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class TitleJournalLayoutTest {
    @Test public void toolbarScrollbarAndFooterShareAnOuterEdge() {
        for (int width : new int[]{960, 1066, 1440, 1920}) {
            var layout = new TitleJournalLayout(width, 600);
            assertEquals(layout.right(), layout.orderX() + 128);
            assertEquals(layout.right(), layout.scrollbarX() + UiScrollbar.WIDTH);
            assertEquals(layout.categoryX() - TitleJournalLayout.GAP,
                    TitleJournalLayout.MARGIN + layout.searchWidth(true));
            assertEquals(layout.orderX() - TitleJournalLayout.GAP, layout.categoryX() + 190);
            assertEquals(layout.scrollbarX() - TitleJournalLayout.GAP,
                    TitleJournalLayout.MARGIN + layout.contentWidth());
            assertEquals(layout.right(), TitleJournalLayout.MARGIN + layout.searchWidth(false));
            assertEquals(layout.right(), TitleJournalLayout.MARGIN + layout.contentWidth(false));
            assertEquals(512, layout.top() + layout.bodyHeight());
        }
    }

    @Test public void pageBoundariesCoverHistoryWithoutDuplicatesOrEmptyLastPages() {
        for (int count : new int[]{0, 1, 20, 21, 40, 41, 1003}) {
            int visited = 0;
            for (int page = 0; page < TitleJournalLayout.pageCount(count); page++) {
                int start = TitleJournalLayout.pageStart(count, page);
                assertEquals(visited, start);
                visited += Math.min(TitleJournalLayout.PAGE_SIZE, count - start);
            }
            assertEquals(count, visited);
            assertEquals(0, TitleJournalLayout.pageStart(count, -1));
            assertEquals(TitleJournalLayout.pageStart(count, TitleJournalLayout.pageCount(count) - 1),
                    TitleJournalLayout.pageStart(count, Integer.MAX_VALUE));
        }
    }
}
