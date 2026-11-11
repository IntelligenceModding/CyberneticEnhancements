package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class UiViewportTest {
    private static final int[][] RESOLUTIONS = {{854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440},
            {3840, 2160}, {3440, 1440}, {1280, 1024}, {1024, 768}, {1080, 1920}, {5120, 1440}};

    @Test
    public void physicalSizeAndLayoutAreIndependentOfMinecraftGuiScale() {
        for (int[] size : RESOLUTIONS) {
            UiViewport reference = UiViewport.fit(size[0], size[1], 1, 960, 600);
            for (int guiScale = 1; guiScale <= 8; guiScale++) {
                UiViewport viewport = UiViewport.fit(size[0], size[1], guiScale, 960, 600);
                assertEquals(reference.width(), viewport.width());
                assertEquals(reference.height(), viewport.height());
                assertEquals(reference.renderScale(), viewport.renderScale() * guiScale, 0.000001);
            }
        }
    }

    @Test
    public void everyContainerFitsIncludingPortraitAndUltrawideScreens() {
        int[][] containers = {{800, 456}, {432, 302}, {640, 484}, {432, 300}};
        for (int[] size : RESOLUTIONS) {
            for (int[] container : containers) {
                UiViewport viewport = UiViewport.fit(size[0], size[1], 4,
                        960, 600);
                assertTrue(viewport.width() >= container[0] + 47);
                assertTrue(viewport.height() >= container[1] + 47);
                assertTrue((container[0] + 40) * viewport.renderScale() * 4 <= size[0] + 1);
                assertTrue((container[1] + 40) * viewport.renderScale() * 4 <= size[1] + 1);
            }
        }
    }

    @Test
    public void mouseAndDragCoordinatesRoundTripAtFractionalScales() {
        for (int[] size : RESOLUTIONS) {
            for (int scale = 1; scale <= 8; scale++) {
                UiViewport viewport = UiViewport.fit(size[0], size[1], scale, 960, 600);
                for (double coordinate : new double[]{0, 5.5, 64, 227.75, 600}) {
                    assertEquals(coordinate, viewport.toUi(coordinate * viewport.renderScale()), 0.000001);
                }
            }
        }
    }

    @Test
    public void scissorRoundingNeverCutsOffTheRequestedBounds() {
        for (int[] size : RESOLUTIONS) {
            UiViewport viewport = UiViewport.fit(size[0], size[1], 3, 960, 600);
            for (int coordinate = 0; coordinate < 960; coordinate += 7) {
                double exact = coordinate * viewport.renderScale();
                assertTrue(viewport.toScreenFloor(coordinate) <= exact);
                assertTrue(viewport.toScreenCeil(coordinate) >= exact);
                assertTrue(viewport.toScreenCeil(coordinate) - viewport.toScreenFloor(coordinate) <= 1);
            }
        }
    }
}
