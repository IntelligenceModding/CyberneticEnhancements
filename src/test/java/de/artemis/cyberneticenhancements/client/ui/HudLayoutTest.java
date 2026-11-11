package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class HudLayoutTest {
    @Test
    public void cardsAvoidHotbarOffhandAndStatusBarsAcrossSupportedGuiScales() {
        int[][] resolutions = {{854, 480}, {1280, 720}, {1920, 1080}, {3840, 2160},
                {3440, 1440}, {1024, 768}, {1080, 1920}, {5120, 1440}};
        for (int[] resolution : resolutions) {
            int maxScale = Math.min(resolution[0] / 320, resolution[1] / 240);
            for (int scale = 1; scale <= maxScale; scale++) {
                UiViewport viewport = UiViewport.fit(resolution[0], resolution[1], scale, 960, 600);
                for (int leftCount = 1; leftCount <= 2; leftCount++) {
                    for (int rightCount = 0; rightCount <= 2; rightCount++) {
                        for (int vanillaHeight : new int[]{80, 104, 128}) {
                            HudLayout layout = HudLayout.fit(viewport, leftCount, rightCount, vanillaHeight);
                            int minimumWidth = Math.max(leftCount, rightCount) * 112 + (Math.max(leftCount, rightCount) - 1) * 4;
                            for (HudLayout.Band band : new HudLayout.Band[]{layout.left(), layout.right()}) {
                                assertTrue(band.x() >= 0);
                                assertTrue(band.x() + band.width() <= viewport.width());
                                assertTrue(band.width() > 0);
                                if (band.columns() > 1) assertTrue(band.width() >= minimumWidth);
                                assertTrue(band.bottom() > 150);
                                assertEquals(viewport.height() - 6, band.bottom());
                                double nativeLeft = viewport.width() / 2 - viewport.toUi(125);
                                double nativeRight = viewport.width() / 2 + viewport.toUi(125);
                                double nativeTop = viewport.height() - viewport.toUi(vanillaHeight);
                                assertTrue("HUD intersects vanilla controls at " + resolution[0] + "x" + resolution[1] + " scale " + scale,
                                        band.x() + band.width() <= nativeLeft || band.x() >= nativeRight || band.bottom() < nativeTop);
                            }
                            assertTrue(layout.left().x() + layout.left().width() < layout.right().x());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void crowdedHudStacksBesideHotbarInsteadOfMovingAboveIt() {
        UiViewport viewport = UiViewport.fit(1280, 720, 3, 960, 600);
        HudLayout layout = HudLayout.fit(viewport, 2, 2, 80);
        assertEquals(1, layout.left().columns());
        assertEquals(1, layout.right().columns());
        assertEquals(viewport.height() - 6, layout.left().bottom());
        assertEquals(viewport.height() - 6, layout.right().bottom());
        assertTrue(layout.left().x() + layout.left().width() < viewport.width() / 2 - viewport.toUi(125));
    }

    @Test
    public void eachSideAdaptsIndependentlyAndHeartsNeverMoveTheAnchor() {
        UiViewport viewport = UiViewport.fit(1280, 720, 3, 960, 600);
        var normal = HudLayout.fit(viewport, 1, 2, 80);
        var extraHearts = HudLayout.fit(viewport, 1, 2, 160);
        assertEquals(normal, extraHearts);
        assertEquals(1, normal.left().columns());
        assertEquals(1, normal.right().columns());
    }
}