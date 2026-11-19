package de.artemis.cyberneticenhancements.common.cyberware;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class CyberpsychosisClientStateTest {
    @After public void reset() { CyberpsychosisClientState.setControlLocked(false); }

    @Test public void unlockingDuringAJumpClearsAllForcedMovement() {
        CyberpsychosisClientState.apply(true, 90, 15, 1, .5F, true, true);
        assertTrue(CyberpsychosisClientState.jump());
        assertTrue(CyberpsychosisClientState.sprint());
        // Even stale motion fields in an unlock update must never survive control restoration.
        CyberpsychosisClientState.apply(false, 90, 15, 1, .5F, true, true);
        assertFalse(CyberpsychosisClientState.isControlLocked());
        assertFalse(CyberpsychosisClientState.jump());
        assertFalse(CyberpsychosisClientState.sprint());
        assertEquals(0, CyberpsychosisClientState.forward(), 0);
        assertEquals(0, CyberpsychosisClientState.strafe(), 0);
    }
}
