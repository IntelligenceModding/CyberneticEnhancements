package de.artemis.cyberneticenhancements.common.util;

import de.artemis.cyberneticenhancements.client.ui.DeathBodyMotion;
import org.joml.Vector3f;
import org.junit.Test;
import static org.junit.Assert.*;

public class DeathFallDirectionTest {
    @Test public void hitsFromEverySideTipTheBodyAwayFromTheSource() {
        double[][] attackers = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}};
        var fallen = DeathBodyMotion.sample(1, false);
        for (var attacker : attackers) {
            float yaw = DeathFallDirection.yaw(-attacker[0], -attacker[1], 37);
            var up = fallen.rotation(yaw).transform(new Vector3f(0, 1, 0));
            double length = Math.hypot(attacker[0], attacker[1]);
            assertEquals(-attacker[0] / length, up.x, 0.00001);
            assertEquals(-attacker[1] / length, up.z, 0.00001);
            assertEquals(0, up.y, 0.00001);
        }
    }

    @Test public void missingOrVerticalDirectionsKeepTheBackwardFallback() {
        assertEquals(37, DeathFallDirection.yaw(0, 0, 37), 0);
        assertEquals(37, DeathFallDirection.yaw(Double.NaN, 0, 37), 0);
    }

    @Test public void shoulderClearanceIsTemporaryAndSettledTorsoRestsNearTheFloor() {
        var standing = DeathBodyMotion.sample(0, false);
        assertEquals(standing, standing.forDirection(0, 90));
        var impact = DeathBodyMotion.sample(DeathBodyMotion.IMPACT_SECONDS, false);
        assertEquals(impact.lift() + 0.14, impact.forDirection(0, 90).lift(), 0.00001);
        var fallen = DeathBodyMotion.sample(1, false);
        assertEquals(fallen.lift(), fallen.forDirection(0, 0).lift(), 0.00001);
        assertEquals(fallen.lift(), fallen.forDirection(0, 90).lift(), 0.00001);
        assertEquals(0.14, fallen.forDirection(0, 90).lift(), 0.00001);
    }

    @Test public void everyHitDirectionSettlesFaceUpOrDownWithoutChangingTheFallDirection() {
        for (int bodyYaw = -180; bodyYaw <= 180; bodyYaw += 15) {
            for (int fallYaw = -180; fallYaw <= 180; fallYaw += 15) {
                var settled = DeathBodyMotion.sample(1, false).forDirection(bodyYaw, fallYaw);
                var rotation = settled.rotation(fallYaw);
                var bodyAxis = rotation.transform(new Vector3f(0, 1, 0));
                assertEquals(Math.sin(Math.toRadians(fallYaw)), bodyAxis.x, 0.00001);
                assertEquals(-Math.cos(Math.toRadians(fallYaw)), bodyAxis.z, 0.00001);
                var face = rotation.transform(new Vector3f((float) -Math.sin(Math.toRadians(bodyYaw)), 0,
                        (float) Math.cos(Math.toRadians(bodyYaw))));
                assertEquals(1, Math.abs(face.y), 0.00001);
                assertEquals(0.14, settled.lift(), 0.00001);
                assertEquals(1, settled.spread(bodyYaw, fallYaw), 0.00001);
            }
        }
    }

    @Test public void sidewaysImpactRollsSmoothlyIntoAFlatRest() {
        double previousRoll = 0;
        for (int tick = 46; tick <= 82; tick++) {
            var pose = DeathBodyMotion.sample(tick / 100.0, false).forDirection(0, 90);
            assertTrue(Math.abs(pose.roll()) >= previousRoll - 0.00001);
            previousRoll = Math.abs(pose.roll());
        }
        assertEquals(Math.PI / 2, previousRoll, 0.00001);
        assertEquals(DeathBodyMotion.sample(1, false).forDirection(0, 90), DeathBodyMotion.sample(0, true).forDirection(0, 90));
    }

    @Test public void cameraFollowsBackwardForwardAndSidewaysFalls() {
        var fallen = DeathBodyMotion.sample(1, false);
        var backward = fallen.view(0, 0, 0);
        assertEquals(-90, backward.pitch(), 0.03);
        assertEquals(0, backward.roll(), 0.03);
        var forward = fallen.view(180, 0, 0);
        assertEquals(90, forward.pitch(), 0.03);
        var side = fallen.view(90, 0, 0);
        assertEquals(0, side.pitch(), 0.03);
        assertEquals(90, Math.abs(side.roll()), 0.03);
        var standing = DeathBodyMotion.sample(0, false).view(63, 32, 17);
        assertEquals(32, standing.yaw(), 0.001);
        assertEquals(17, standing.pitch(), 0.001);
        assertEquals(0, standing.roll(), 0.001);
    }
}
