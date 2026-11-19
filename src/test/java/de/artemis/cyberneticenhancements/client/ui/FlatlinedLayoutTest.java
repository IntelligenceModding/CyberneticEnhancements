package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class FlatlinedLayoutTest {
    @Test
    public void controlsFitAtEveryStandardGuiScaleAndRemainBelowTheDeathDetails() {
        for (int width : new int[]{320, 426, 640, 854, 1280, 1920}) {
            for (int height : new int[]{180, 240, 360, 480, 720, 1080}) {
                var layout = FlatlinedLayout.of(width, height);
                assertTrue(layout.left() >= 12);
                assertTrue(layout.left() + layout.columnWidth() <= width - 12);
                assertTrue(layout.skullY() >= 12);
                assertTrue(layout.skullY() + 16 * layout.skullScale() <= layout.titleY() - 10);
                assertTrue(layout.causeY() >= layout.titleY() + 26);
                assertTrue(layout.scoreY() >= layout.causeY() + 12);
                assertTrue(layout.buttonTop() >= layout.scoreY() + 16);
                assertTrue(layout.buttonTop() + 68 <= height - 12);
                assertTrue(layout.center() + 52 <= layout.left() + layout.columnWidth());
                assertEquals(width / 2, layout.center());
                assertEquals(200, layout.columnWidth());
                if (height >= 240) {
                    assertTrue(layout.skullScale() >= 3);
                    assertEquals(layout.titleY() + 35, layout.causeY());
                    assertEquals(layout.titleY() + 50, layout.scoreY());
                    assertTrue(layout.titleY() + 70 < layout.buttonTop() - 7);
                    if (height >= 360) assertEquals(4, layout.skullScale());
                }
            }
        }
    }

    @Test
    public void deathMessagesRemainCenteredRegardlessOfLength() {
        var layout = FlatlinedLayout.of(640, 360);
        assertEquals(layout.center(), layout.causeCenter(60, 640));
        assertEquals(320, layout.causeCenter(250, 640));
        assertEquals(320, layout.causeCenter(900, 640));
    }
}
