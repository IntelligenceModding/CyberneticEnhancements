package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudTextScrollTest {
    @Test public void textThatFitsNeverMoves() {
        assertEquals(0, HudText.scrollOffset(0, 5), 1e-9);
    }

    @Test public void overflowPausesGlidesAndPausesAgain() {
        assertEquals(0, HudText.scrollOffset(11, 1), 1e-9);          // Opening pause.
        double mid = HudText.scrollOffset(11, 2.7);
        assertTrue(mid > 0 && mid < 11);                              // Gliding.
        assertEquals(11, HudText.scrollOffset(11, 4.2), 1e-9);        // Held at the end.
        assertEquals(0, HudText.scrollOffset(11, 4 + 11 / 8.0 + 1), 1e-9); // A full cycle later it starts over.
    }
}
