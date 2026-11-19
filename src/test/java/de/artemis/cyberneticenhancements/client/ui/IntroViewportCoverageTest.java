package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class IntroViewportCoverageTest {
    @Test
    public void introCoversBothEdgesAtOddResolutionsAndRoundedGuiScales() {
        int[][] sizes = {{2560, 1440}, {1920, 1080}, {854, 480}, {1365, 767}, {2719, 1531}, {3440, 1440}, {1080, 1920}};
        for (var size : sizes) for (double scale : new double[]{1, 2, 3, 4, 1.25, 1.5}) {
            int guiWidth = (int) Math.ceil(size[0] / scale);
            int guiHeight = (int) Math.ceil(size[1] / scale);
            var base = UiViewport.fit(size[0], size[1], scale, 960, 600);
            var covered = base.coveringGui(guiWidth, guiHeight);
            assertEquals(base.renderScale(), covered.renderScale(), 0);
            double actualScale = (float) covered.renderScale();
            assertTrue("Right edge remains exposed", covered.width() * actualScale >= guiWidth);
            assertTrue("Bottom edge remains exposed", covered.height() * actualScale >= guiHeight);
            assertTrue("Overscan should stay within one UI unit", covered.width() * actualScale <= guiWidth + actualScale + 1e-9);
            assertTrue(covered.height() * actualScale <= guiHeight + actualScale + 1e-9);
        }
    }
}
