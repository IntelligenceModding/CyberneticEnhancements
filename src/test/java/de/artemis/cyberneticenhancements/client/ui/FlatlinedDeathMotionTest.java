package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class FlatlinedDeathMotionTest {
    @Test
    public void fallStartsFromTheCurrentViewAndLandsWithinASecond() {
        var initial = FlatlinedDeathMotion.sample(0);
        assertEquals(0, initial.tilt(), 0);
        assertEquals(0, initial.fall(), 0);
        assertEquals(0, initial.roll(), 0);
        double previous = 0;
        for (int step = 0; step <= 95; step++) {
            var pose = FlatlinedDeathMotion.sample(step / 100.0);
            assertTrue(pose.fall() >= previous);
            assertTrue(pose.tilt() >= 0 && pose.tilt() <= 1);
            assertTrue(Math.abs(pose.roll()) <= 5);
            previous = pose.fall();
        }
        assertEquals(1, previous, 0);
        assertEquals(1, FlatlinedDeathMotion.sample(0.95).tilt(), 0);
    }

    @Test
    public void impactSettlesAndTheDeadViewStaysStill() {
        assertTrue(Math.abs(FlatlinedDeathMotion.sample(1).impact()) > 0.1);
        var settled = FlatlinedDeathMotion.sample(3);
        assertEquals(1, settled.fall(), 0);
        assertEquals(1, settled.tilt(), 0);
        assertEquals(0, settled.impact(), 0.0001);
        assertEquals(0, settled.roll(), 0.0001);
        assertEquals(settled.fall(), FlatlinedDeathMotion.sample(60).fall(), 0);
    }
}
