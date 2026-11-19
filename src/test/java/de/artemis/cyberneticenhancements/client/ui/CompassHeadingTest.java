package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompassHeadingTest {
    @Test
    public void cardinalDirectionsMatchMinecraftWorldAxes() {
        assertEquals(0, CompassHeading.offsetDegrees(180, 0), 0.00001);
        assertEquals(0, CompassHeading.offsetDegrees(-90, 90), 0.00001);
        assertEquals(0, CompassHeading.offsetDegrees(0, 180), 0.00001);
        assertEquals(0, CompassHeading.offsetDegrees(90, 270), 0.00001);
        assertEquals(-90, CompassHeading.offsetDegrees(0, 90), 0.00001);
        assertEquals(90, CompassHeading.offsetDegrees(0, 270), 0.00001);
    }

    @Test
    public void turningThroughNorthAndRepeatedRotationsDoNotJump() {
        assertEquals(1, CompassHeading.offsetDegrees(179, 0), 0.00001);
        assertEquals(-1, CompassHeading.offsetDegrees(-179, 0), 0.00001);
        for (int turns = -20; turns <= 20; turns++) {
            assertEquals(-45, CompassHeading.offsetDegrees(-135 + turns * 360, 0), 0.00001);
            assertEquals(45, CompassHeading.offsetDegrees(-135 + turns * 360, 90), 0.00001);
        }
    }
}
