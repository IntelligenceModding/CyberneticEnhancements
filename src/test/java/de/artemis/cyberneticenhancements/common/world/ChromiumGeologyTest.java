package de.artemis.cyberneticenhancements.common.world;

import net.minecraft.core.BlockPos;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChromiumGeologyTest {
    @Test public void prospectsAreStableRareAndDeep() {
        int deposits = 0;
        double height = 0;
        for (int x = -128; x < 128; x++) for (int z = -128; z < 128; z++) {
            var pos = ChromiumGeology.deposit(947231L, x, z);
            assertEquals(pos, ChromiumGeology.deposit(947231L, x, z));
            if (pos == null) continue;
            deposits++; height += pos.getY();
            assertEquals(x, pos.getX() >> 4);
            assertEquals(z, pos.getZ() >> 4);
            assertTrue(pos.getY() >= -56 && pos.getY() <= -9);
        }
        assertTrue(deposits > 850 && deposits < 1200);
        assertEquals(-32.5, height / deposits, 1.5);
    }

    @Test public void leadsMatchGenerationAndRespectSearchRange() {
        var origin = new BlockPos(-239, 80, 177);
        var prospect = ChromiumGeology.nearest(123L, origin, 160, 760);
        assertNotNull(prospect);
        assertEquals(prospect, ChromiumGeology.deposit(123L, prospect.getX() >> 4, prospect.getZ() >> 4));
        double distance = Math.hypot(prospect.getX() - origin.getX(), prospect.getZ() - origin.getZ());
        assertTrue(distance >= 160 && distance <= 760);
        assertEquals(prospect, ChromiumGeology.nearest(123L, origin.above(100), 160, 760));
        assertNotEquals(prospect, ChromiumGeology.nearest(456L, origin, 160, 760));
        assertNull(ChromiumGeology.nearest(123L, origin, 800, 760));
    }
}
