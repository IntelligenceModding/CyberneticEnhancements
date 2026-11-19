package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArchiveContentLayoutTest {
    @Test
    public void cardsScrollbarAndOuterBorderKeepTheSameGapsAtEveryWidth() {
        for (int width = 320; width < 1600; width++) {
            int content = ArchiveContentLayout.contentWidth(width, true);
            int bar = ArchiveContentLayout.scrollbarX(0, width);
            int left = ArchiveContentLayout.contentX(0);
            assertEquals(10, left);
            assertEquals(width - 26, left + content);
            assertEquals(10, bar - (left + content));
            assertEquals(10, width - bar - UiScrollbar.WIDTH);
            int fullWidth = ArchiveContentLayout.contentWidth(width, false);
            assertEquals(10, width - left - fullWidth);
            assertEquals(ArchiveContentLayout.GAP + UiScrollbar.WIDTH, fullWidth - content);
            int columns = ArchiveContactLayout.columns(content);
            var last = ArchiveContactLayout.card(columns - 1, content);
            assertEquals(content, last.x() + last.width());
            if (columns > 1) {
                var first = ArchiveContactLayout.card(0, content);
                assertEquals(first.width(), last.width());
                assertEquals(ArchiveContactLayout.GAP + (content - ArchiveContactLayout.GAP) % columns,
                        last.x() - first.x() - first.width());
            }
        }
    }
}
