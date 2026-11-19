package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class FlatlinedTransitionTest {
    @Test
    public void corruptionStartsQuietlyAndSpreadsThroughDifferentRegions() {
        assertEquals(0, FlatlinedTransition.intensity(0), 0);
        assertTrue(FlatlinedTransition.intensity(0.2) < 0.05);
        int started = 0;
        int untouched = 0;
        for (int band = 0; band < FlatlinedTransition.BANDS; band++) {
            assertEquals(0, FlatlinedTransition.coverage(0, band), 0);
            double coverage = FlatlinedTransition.coverage(0.35, band);
            if (coverage > 0) started++;
            else untouched++;
        }
        assertTrue(started > 0);
        assertTrue(untouched > 0);
    }

    @Test
    public void everyRegionAdvancesWithoutRetreatingAndTakesOverWithinTwoSeconds() {
        for (int band = 0; band < FlatlinedTransition.BANDS; band++) {
            double previous = 0;
            for (int step = 0; step <= 40; step++) {
                double coverage = FlatlinedTransition.coverage(step / 20.0, band);
                assertTrue(coverage >= previous);
                assertTrue(coverage >= 0 && coverage <= 1);
                previous = coverage;
            }
            assertEquals(1, previous, 0);
        }
        assertEquals(1, FlatlinedTransition.intensity(1.8), 0);
        assertEquals(1, FlatlinedTransition.intensity(60), 0);
    }
}
