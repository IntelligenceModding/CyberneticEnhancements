package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeathBodyMotionTest {
    @Test public void headAndBodyFollowTheSameBackwardArc() {
        var start = DeathBodyMotion.sample(0, false);
        assertEquals(1.62, start.eyeHeight(1.62), 0.00001);
        assertEquals(0, start.eyeBackward(1.62), 0);
        double previous = 0;
        for (int i = 0; i <= 46; i++) {
            var pose = DeathBodyMotion.sample(i / 100.0, false);
            assertTrue(pose.angle() >= previous);
            double vertical = pose.eyeHeight(1.62) - pose.lift();
            assertEquals(1.62 * 1.62, vertical * vertical + Math.pow(pose.eyeBackward(1.62), 2), 0.00001);
            previous = pose.angle();
        }
        var fallen = DeathBodyMotion.sample(1, false);
        assertEquals(Math.PI / 2, fallen.angle(), 0);
        assertEquals(0.14, fallen.eyeHeight(1.62), 0.00001);
        assertEquals(1.62, fallen.eyeBackward(1.62), 0.00001);
        assertEquals(fallen, DeathBodyMotion.sample(600, false));
    }

    @Test public void fallAcceleratesIntoAQuickImpactThenReboundsAndSettles() {
        double earlySpeed = DeathBodyMotion.sample(0.2, false).angle() - DeathBodyMotion.sample(0.1, false).angle();
        double lateSpeed = DeathBodyMotion.sample(0.4, false).angle() - DeathBodyMotion.sample(0.3, false).angle();
        assertTrue(lateSpeed > earlySpeed * 2);
        var impact = DeathBodyMotion.sample(DeathBodyMotion.IMPACT_SECONDS, false);
        assertEquals(Math.PI / 2, impact.angle(), 0);
        var bounce = DeathBodyMotion.sample(0.52, false);
        assertTrue(bounce.angle() < impact.angle());
        assertTrue(bounce.lift() > impact.lift());
        assertEquals(DeathBodyMotion.sample(DeathBodyMotion.FALL_SECONDS, false), DeathBodyMotion.sample(100, false));
    }

    @Test public void restingLimbsVaryPerDeathWithoutChangingBetweenFrames() {
        var rest = DeathBodyMotion.restPose(42);
        assertEquals(rest, DeathBodyMotion.restPose(42));
        assertNotEquals(rest, DeathBodyMotion.restPose(43));
        assertNotEquals(rest.rightArm().pitch(), rest.leftArm().pitch(), 0);
        assertNotEquals(rest.rightLeg().pitch(), rest.leftLeg().pitch(), 0);
        assertEquals(rest, rest.sample(1, false));
        assertEquals(rest, rest.sample(100, false));
        assertEquals(rest, rest.sample(0, true));
        assertEquals(0, rest.sample(0, false).head().yaw(), 0);
        assertEquals(0, rest.sample(0, false).rightArm().roll(), 0);
        for (long seed = 0; seed < 100; seed++) {
            var pose = DeathBodyMotion.restPose(seed);
            assertTrue(pose.rightArm().roll() > 0 && pose.leftArm().roll() < 0);
            assertTrue(Math.abs(pose.head().yaw()) <= 0.4);
            assertTrue(Math.abs(pose.rightLeg().pitch()) <= 0.17);
        }
    }

    @Test public void revivalReturnsSmoothlyToStandingWithoutImpactBounce() {
        double previousAngle = Math.PI / 2;
        for (int tick = 0; tick <= DeathBodyMotion.RECOVERY_TICKS; tick++) {
            var pose = DeathBodyMotion.sample(DeathBodyMotion.recoverySeconds(tick), false);
            assertTrue(pose.angle() <= previousAngle + 0.00001);
            previousAngle = pose.angle();
        }
        assertEquals(0, previousAngle, 0);
    }

    @Test public void corpsesHoldForTenSecondsThenDissolveOverOneSecond() {
        assertEquals(1, DeathBodyMotion.visibility(0), 0);
        assertEquals(1, DeathBodyMotion.visibility(199), 0);
        assertEquals(1, DeathBodyMotion.visibility(200), 0);
        assertEquals(0.5, DeathBodyMotion.visibility(210), 0);
        assertEquals(0, DeathBodyMotion.visibility(220), 0);
        assertEquals(0, DeathBodyMotion.visibility(10000), 0);
    }

    @Test public void reducedMotionUsesTheFinalPoseImmediately() {
        assertEquals(DeathBodyMotion.sample(1, false), DeathBodyMotion.sample(0, true));
        assertEquals(DeathBodyMotion.sample(0, true), DeathBodyMotion.sample(600, true));
    }
}
