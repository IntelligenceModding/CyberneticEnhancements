package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class BankingLayoutTest {
    @Test
    public void cardsStayWithinThePanelAndKeepEqualGapsAcrossWidths() {
        for (int width : new int[]{240, 320, 480, 619, 620, 699, 700, 940, 1280, 1800}) {
            var layout = BankingLayout.dashboard(width);
            for (var card : layout.stats()) {
                assertTrue(card.x() >= 0 && card.x() + card.width() <= width);
                assertTrue(card.width() >= 100);
                assertEquals(74, card.height());
            }
            for (int index = 0; index < layout.stats().size() - 1; index++) {
                var left = layout.stats().get(index);
                var right = layout.stats().get(index + 1);
                if (left.y() == right.y()) {
                    assertEquals(10, right.x() - left.x() - left.width());
                    assertTrue(Math.abs(left.width() - right.width()) <= 1);
                }
            }
            assertEquals(10, layout.graph().y() - layout.stats().getLast().y() - 74);
        }
    }

    @Test
    public void transferAndChartNeverOverlapAndStackOnNarrowPanels() {
        for (int width = 240; width <= 1600; width += 11) {
            var layout = BankingLayout.dashboard(width);
            var chart = layout.graph();
            var form = layout.transfer();
            assertEquals(228, chart.height());
            assertEquals(228, form.height());
            if (width < 700) {
                assertEquals(width, chart.width());
                assertEquals(width, form.width());
                assertEquals(10, form.y() - chart.y() - chart.height());
            } else {
                assertEquals(chart.y(), form.y());
                assertEquals(10, form.x() - chart.x() - chart.width());
                assertEquals(width, form.x() + form.width());
                assertTrue(form.width() >= 270 && form.width() <= 340);
            }
            assertEquals(form.y() + form.height() + 10, layout.balance().y());
            assertEquals(0, layout.balance().x());
            assertEquals(width, layout.balance().width());
            assertEquals(228, layout.balance().height());
            assertEquals(layout.balance().y() + layout.balance().height(), layout.bottom());
        }
    }

}
