package de.artemis.cyberneticenhancements.common.util;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import org.junit.Test;
import static org.junit.Assert.*;

public class CyberwareTierTest {
    @Test
    public void upgradesAdvanceOneTierAndStopAtMaximum() {
        assertEquals(CyberwareTier.TIER_2, CyberwareTier.TIER_1.next());
        assertEquals(CyberwareTier.TIER_3, CyberwareTier.TIER_2.next());
        assertEquals(CyberwareTier.TIER_4, CyberwareTier.TIER_3.next());
        assertEquals(CyberwareTier.TIER_5, CyberwareTier.TIER_4.next());
        assertEquals(CyberwareTier.TIER_5, CyberwareTier.TIER_5.next());
    }
}
