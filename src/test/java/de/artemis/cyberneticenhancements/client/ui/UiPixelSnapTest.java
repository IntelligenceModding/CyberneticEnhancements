package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiPixelSnapTest {
    @Test
    public void equalEntriesAndControlsHaveEqualRasterSizesAtFractionalScales() {
        for (double scale : new double[]{0.75, 1.25, 1.5, 1.6, 1.8, 2.4, 3.6}) {
            for (int width = 730; width < 1100; width++) {
                var first = ArchiveContactLayout.card(0, width);
                var second = ArchiveContactLayout.card(1, width);
                double left = snappedOrigin((16 + first.x()) * scale, scale);
                double right = snappedOrigin((16 + second.x()) * scale, scale);
                assertEquals(Math.round(left + first.width() * scale) - Math.round(left),
                        Math.round(right + second.width() * scale) - Math.round(right));
                assertEquals(Math.round(left + 94 * scale) - Math.round(left),
                        Math.round(right + 94 * scale) - Math.round(right));
            }
        }
    }

    @Test
    public void alignmentPreservesSizeAndNeverMovesAnEntryMoreThanHalfAPixel() {
        for (double origin : new double[]{-23.8, 0, 3.25, 11.5, 534.77}) {
            double offset = UiPixelSnap.offset(origin, 1.8);
            assertEquals(Math.rint(origin), origin + offset * 1.8, 0.000001);
            assertTrue(Math.abs(offset * 1.8) <= 0.500001);
        }
    }

    private static double snappedOrigin(double origin, double scale) {
        return origin + UiPixelSnap.offset(origin, scale) * scale;
    }
}
