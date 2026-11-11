package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.*;

public class SandevistanSessionTest {
    private static SandevistanSession session(double condition, int uses) {
        return new SandevistanSession(SandevistanProfile.DEFAULTS.getLast(), 100, 120, 900, condition, uses);
    }

    @Test
    public void KillRewardsAreUniqueAndBoundedByCountAndTotalDuration() {
        SandevistanSession session = session(1, 1);
        UUID first = UUID.randomUUID();
        assertTrue(session.rewardKill(first, 40));
        assertFalse(session.rewardKill(first, 40));
        for (int i = 0; i < 5; i++) assertTrue(session.rewardKill(UUID.randomUUID(), Long.MAX_VALUE));
        assertFalse(session.rewardKill(UUID.randomUUID(), 40));
        assertEquals(280, session.until());
        assertEquals(session.hardStop, session.until());
    }

    @Test
    public void SafeFirstUseCannotRandomlyOverload() {
        assertFalse(session(1, 1).shouldOverload(100));
        assertFalse(session(1, 2).shouldOverload(100));
        assertTrue(session(1, 3).shouldOverload(95));
        assertFalse(session(1, 3).shouldOverload(75));
        assertTrue(session(1, 4).shouldOverload(75));
        assertTrue(session(0.1, 3).shouldOverload(0));
    }

    @Test
    public void DamagedHardwareAndRepeatedUseCostMoreAndDilateLess() {
        SandevistanSession healthy = session(1, 1);
        SandevistanSession worn = session(0.2, 4);
        assertEquals(0.08, healthy.effectiveTimeScale(), 0.000001);
        assertTrue(worn.effectiveTimeScale() > healthy.effectiveTimeScale());
        assertTrue(worn.activationLoad() > healthy.activationLoad() * 2);
    }

    @Test
    public void ExperimentalSlowdownRemainsUsefulAtHalfCondition() {
        assertEquals(0.149, session(0.5, 1).effectiveTimeScale(), 0.000001);
        assertEquals(0.218, session(0, 1).effectiveTimeScale(), 0.000001);
        var apogee = new SandevistanSession(SandevistanProfile.DEFAULTS.get(4), 0, 240, 680, 0.5, 1);
        assertEquals(0.34125, apogee.effectiveTimeScale(), 0.000001);
    }

    @Test
    public void UsageHistoryRecoversGraduallyRatherThanResettingAtOnce() {
        assertEquals(5, SandevistanSession.recentUseCount(4, 100, 100, 2400));
        assertEquals(4, SandevistanSession.recentUseCount(4, 100, 2500, 2400));
        assertEquals(3, SandevistanSession.recentUseCount(4, 100, 4900, 2400));
        assertEquals(1, SandevistanSession.recentUseCount(4, 100, 20000, 2400));
        assertEquals(8, SandevistanSession.recentUseCount(8, 100, 50, 2400));
    }

    @Test
    public void DurationAndConditionStayWithinSafetyBounds() {
        SandevistanSession shortSession = new SandevistanSession(SandevistanProfile.DEFAULTS.getFirst(), 0, -5, -1, -10, -1);
        assertEquals(20, shortSession.until());
        assertEquals(20, shortSession.cooldownTicks);
        assertEquals(0, shortSession.condition, 0);
        SandevistanSession longSession = new SandevistanSession(SandevistanProfile.DEFAULTS.getFirst(), 0, Long.MAX_VALUE, 900, 10, 100);
        assertEquals(1200, longSession.until());
        assertEquals(1800, longSession.hardStop);
        assertEquals(1, longSession.condition, 0);
    }
}
