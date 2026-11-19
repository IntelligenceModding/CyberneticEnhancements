package de.artemis.cyberneticenhancements.common.world;

import org.junit.Test;
import static org.junit.Assert.*;

public class StreetEncountersTest {
    @Test public void theTollIsATenthWithinAFloorAndACeiling() {
        assertEquals(25, StreetEncounters.demand(50));
        assertEquals(120, StreetEncounters.demand(1200));
        assertEquals(400, StreetEncounters.demand(1_000_000));
    }

    @Test public void moreChromeMakesABluffLikelier() {
        assertEquals(0, StreetEncounters.bluffChance(0), 1e-9);
        assertTrue(StreetEncounters.bluffChance(4) > StreetEncounters.bluffChance(2));
        assertEquals(0.75, StreetEncounters.bluffChance(40), 1e-9);
    }
}
