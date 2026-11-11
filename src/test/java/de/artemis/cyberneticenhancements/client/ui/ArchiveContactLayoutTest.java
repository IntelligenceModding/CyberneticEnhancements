package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArchiveContactLayoutTest {
    @Test
    public void wideViewportUsesTwoColumnsWithoutStretchingSparseCollections() {
        var first = ArchiveContactLayout.card(0, 886);
        var second = ArchiveContactLayout.card(1, 886);
        var third = ArchiveContactLayout.card(2, 886);
        assertEquals(2, ArchiveContactLayout.columns(886));
        assertEquals(first.width(), second.width());
        assertEquals(first.width() + ArchiveContactLayout.GAP, second.x());
        assertEquals(0, third.x());
        assertEquals(first.height() + ArchiveContactLayout.GAP, third.y());
        assertEquals(first.height(), ArchiveContactLayout.contentHeight(1, 886));
        assertEquals(first.height(), ArchiveContactLayout.contentHeight(2, 886));
        assertEquals(third.y() + third.height(), ArchiveContactLayout.contentHeight(3, 886));
        assertEquals(0, ArchiveContactLayout.contentHeight(0, 886));
    }

    @Test
    public void actionsFillTheFooterWithEqualOuterPaddingAndNoOverlap() {
        for (int areaWidth = 320; areaWidth <= 1000; areaWidth++) {
            var card = ArchiveContactLayout.card(0, areaWidth);
            var previous = ArchiveContactLayout.action(0, card.width());
            assertEquals(ArchiveContactLayout.PADDING, previous.x());
            int smallest = previous.width(), largest = previous.width();
            for (int i = 1; i < 4; i++) {
                var action = ArchiveContactLayout.action(i, card.width());
                assertEquals(ArchiveContactLayout.GAP, action.x() - previous.x() - previous.width());
                assertEquals(previous.y(), action.y());
                assertTrue(action.width() > 0);
                smallest = Math.min(smallest, action.width());
                largest = Math.max(largest, action.width());
                previous = action;
            }
            assertTrue(largest - smallest <= 1);
            assertEquals(ArchiveContactLayout.PADDING, card.width() - previous.x() - previous.width());
            assertEquals(ArchiveContactLayout.PADDING, card.height() - previous.y() - previous.height());
        }
    }

    @Test
    public void narrowViewportStacksCardsWithoutClipping() {
        assertEquals(1, ArchiveContactLayout.columns(600));
        var first = ArchiveContactLayout.card(0, 600);
        var second = ArchiveContactLayout.card(1, 600);
        assertEquals(600, first.width());
        assertEquals(0, second.x());
        assertEquals(first.height() + ArchiveContactLayout.GAP, second.y());
    }
}
