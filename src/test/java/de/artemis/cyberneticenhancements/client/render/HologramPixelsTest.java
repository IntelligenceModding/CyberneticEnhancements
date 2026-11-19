package de.artemis.cyberneticenhancements.client.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class HologramPixelsTest {
    @Test public void markerHasASeparateDotAndStemOnTheBlockTexelGrid() {
        assertEquals(1F / 16, HologramPixels.SIZE, 0);
        assertTrue(HologramPixels.markerRow(0));
        assertTrue(HologramPixels.markerRow(1));
        assertFalse(HologramPixels.markerRow(2));
        for (int row = 3; row < 8; row++) assertTrue(HologramPixels.markerRow(row));
        assertFalse(HologramPixels.markerRow(-1));
        assertFalse(HologramPixels.markerRow(8));
        assertEquals(.5F, HologramPixels.MARKER_ROWS * HologramPixels.SIZE, 0);
    }

    @Test public void scanlinesStayInWholeRowsAndWrapWithoutExceedingTheProjection() {
        for (int rows : new int[]{8, 32, 48}) {
            for (float time = 0; time < 160; time += .125F) {
                int row = HologramPixels.sweepRow(time, rows);
                assertTrue(row >= 0 && row < rows);
                assertEquals(row, HologramPixels.sweepRow(time + 80, rows));
            }
        }
        assertEquals(HologramPixels.stripe(3, 0), HologramPixels.stripe(3, 7.99F), 0);
        assertNotEquals(HologramPixels.stripe(3, 7.99F), HologramPixels.stripe(3, 8), 0);
        assertEquals(.9375F / 16, HologramPixels.PLAYER_PIXEL, 0);
        assertEquals(1, HologramPixels.sweepRow(2.5F, 32));
    }

    @Test public void pixelRingsAreSymmetricAndKeepTheirCenterOpen() {
        for (int outer : new int[]{2, 4, 7}) {
            int cells = 0;
            for (int x = -outer; x < outer; x++) for (int z = -outer; z < outer; z++) {
                boolean inside = HologramPixels.ringCell(x, z, outer, outer - 1);
                assertEquals(inside, HologramPixels.ringCell(-x - 1, z, outer, outer - 1));
                assertEquals(inside, HologramPixels.ringCell(z, x, outer, outer - 1));
                if (inside) cells++;
            }
            assertTrue(cells > 0);
            assertFalse(HologramPixels.ringCell(0, 0, outer, outer - 1));
        }
    }
}
