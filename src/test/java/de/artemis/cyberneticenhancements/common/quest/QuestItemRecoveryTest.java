package de.artemis.cyberneticenhancements.common.quest;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

public class QuestItemRecoveryTest {
    @Test
    public void lossWaitsTenSecondsAndReturningAnItemRestartsTheGracePeriod() {
        var recovery = new QuestItemRecovery();
        assertFalse(recovery.missingLongEnough("quest", "compass", false, false, 0));
        assertFalse(recovery.missingLongEnough("quest", "compass", false, false, 199));
        assertTrue(recovery.missingLongEnough("quest", "compass", false, false, 200));
        assertFalse(recovery.missingLongEnough("quest", "compass", true, false, 201));
        assertFalse(recovery.missingLongEnough("quest", "compass", false, false, 250));
        assertFalse(recovery.missingLongEnough("quest", "compass", false, false, 449));
        assertTrue(recovery.missingLongEnough("quest", "compass", false, false, 450));
    }

    @Test
    public void creativeCursorHeartbeatsProtectItemsForAsLongAsTheyAreHeld() {
        var recovery = new QuestItemRecovery();
        for (int tick = 0; tick < 300; tick++) {
            if (tick % 10 == 0) recovery.cursor("quest", "package", tick);
            assertFalse(recovery.missingLongEnough("quest", "package", false, true, tick));
        }
        recovery.cursor("", "", 300);
        assertFalse(recovery.missingLongEnough("quest", "package", false, true, 300));
        assertFalse(recovery.missingLongEnough("quest", "package", false, true, 499));
        assertTrue(recovery.missingLongEnough("quest", "package", false, true, 500));
    }

    @Test
    public void expiredCreativeReportsCannotSuppressRecoveryOrAffectSurvival() {
        var recovery = new QuestItemRecovery();
        recovery.cursor("quest", "compass", 0);
        assertFalse(recovery.missingLongEnough("quest", "compass", false, true, 39));
        assertFalse(recovery.missingLongEnough("quest", "compass", false, true, 40));
        assertFalse(recovery.missingLongEnough("quest", "compass", false, true, 239));
        assertTrue(recovery.missingLongEnough("quest", "compass", false, true, 240));
        recovery.cursor("quest", "compass", 300);
        assertFalse(recovery.missingLongEnough("quest", "compass", false, false, 300));
        assertTrue(recovery.missingLongEnough("quest", "compass", false, false, 500));
    }

    @Test
    public void missingTimersAreIndependentAndDiscardedWithTheirContract() {
        var recovery = new QuestItemRecovery();
        recovery.missingLongEnough("first", "compass", false, false, 0);
        recovery.missingLongEnough("first", "package", false, false, 20);
        recovery.missingLongEnough("second", "compass", false, false, 0);
        recovery.retainContracts(Set.of("first"));
        assertTrue(recovery.missingLongEnough("first", "compass", false, false, 200));
        assertFalse(recovery.missingLongEnough("first", "package", false, false, 200));
        assertTrue(recovery.missingLongEnough("first", "package", false, false, 220));
        assertFalse(recovery.missingLongEnough("second", "compass", false, false, 220));
    }
}
