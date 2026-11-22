package de.artemis.cyberneticenhancements.common.world;

import org.junit.Test;
import static org.junit.Assert.*;

public class StreetEncountersTest {
    private static final ShakedownStance STREET = ShakedownStance.of(20);

    @Test public void theTollIsATenthWithinAFloorAndACeiling() {
        assertEquals(25, STREET.demand(50));
        assertEquals(120, STREET.demand(1200));
        assertEquals(400, STREET.demand(1_000_000));
    }

    @Test public void moreChromeMakesABluffLikelier() {
        assertEquals(0, STREET.bluffChance(0), 1e-9);
        assertTrue(STREET.bluffChance(4) > STREET.bluffChance(2));
        assertEquals(0.9, STREET.bluffChance(40), 1e-9);
    }

    @Test public void aBiggerNameMeansASmallerToll() {
        assertTrue(ShakedownStance.of(1).demand(1000) > ShakedownStance.of(80).demand(1000));
    }
}
