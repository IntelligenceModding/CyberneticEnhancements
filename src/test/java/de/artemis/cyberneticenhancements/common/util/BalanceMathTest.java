package de.artemis.cyberneticenhancements.common.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class BalanceMathTest {
    @Test
    public void largeCreditsNeverWrapOrEraseMoney() {
        assertEquals(BalanceMath.MAX_BALANCE, BalanceMath.adjust(500_000_000, Integer.MAX_VALUE));
        assertEquals(BalanceMath.MAX_BALANCE, BalanceMath.adjust(BalanceMath.MAX_BALANCE, 1));
    }

    @Test
    public void minimumIntegerDebitEmptiesAccount() {
        assertEquals(0, BalanceMath.adjust(500_000_000, Integer.MIN_VALUE));
    }

    @Test
    public void ordinaryAdjustmentsKeepTheirValues() {
        assertEquals(125, BalanceMath.adjust(100, 25));
        assertEquals(75, BalanceMath.adjust(100, -25));
        assertEquals(0, BalanceMath.adjust(100, -101));
        assertEquals(100, BalanceMath.adjust(100, 0));
    }

    @Test
    public void transfersMustFitInFull() {
        assertTrue(BalanceMath.canReceive(BalanceMath.MAX_BALANCE - 10, 10));
        assertFalse(BalanceMath.canReceive(BalanceMath.MAX_BALANCE - 10, 11));
        assertFalse(BalanceMath.canReceive(500_000_000, Integer.MAX_VALUE));
        assertFalse(BalanceMath.canReceive(0, 0));
        assertFalse(BalanceMath.canReceive(0, -1));
    }
}
