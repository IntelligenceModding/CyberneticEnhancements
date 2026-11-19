package de.artemis.cyberneticenhancements.client.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class MarkerVisibilityTest {
    @Test public void markerAppearsAndContinuesFadingAfterTheReadyStateIsRemoved() {
        var animation = new MarkerVisibility();
        assertEquals(0, animation.sample("npc", true, 100), 0);
        assertEquals(.5F, animation.sample("npc", true, 104), .0001F);
        assertEquals(1, animation.sample("npc", true, 108), 0);
        assertEquals(.5F, animation.sample("npc", false, 112), .0001F);
        assertEquals(0, animation.sample("npc", false, 116), 0);
        assertEquals(0, animation.sample("npc", true, 117), 0);
    }

    @Test public void reversingDirectionAndRepeatedRenderPassesNeverRestartTheAnimation() {
        var animation = new MarkerVisibility();
        animation.sample("npc", true, 0);
        assertEquals(.5F, animation.sample("npc", true, 4), .0001F);
        assertEquals(.5F, animation.sample("npc", false, 4), .0001F);
        assertEquals(.25F, animation.sample("npc", false, 6), .0001F);
        assertEquals(.5F, animation.sample("npc", true, 8), .0001F);
        assertEquals(.5F, animation.sample("npc", true, 8), .0001F);
    }

    @Test public void markersAreIndependentAndStaleOrUnloadedWorldStateIsDiscarded() {
        var animation = new MarkerVisibility();
        animation.sample("npc", true, 0);
        animation.sample("barrel", true, 2);
        assertEquals(1, animation.sample("npc", true, 8), 0);
        assertEquals(.75F, animation.sample("barrel", true, 8), .0001F);
        animation.prune(60);
        assertEquals(0, animation.sample("npc", true, 60), 0);
        animation.sample("npc", true, 68);
        animation.clear();
        assertEquals(0, animation.sample("npc", false, 69), 0);
        assertEquals(0, animation.sample("npc", true, 70), 0);
    }

    @Test public void rowRevealPreservesWholePixelGeometryAndOnlyFadesTheBoundaryRow() {
        assertEquals(0, MarkerVisibility.rowOpacity(0, 0), 0);
        assertEquals(1, MarkerVisibility.rowOpacity(.5F, 3), 0);
        assertEquals(0, MarkerVisibility.rowOpacity(.5F, 4), 0);
        assertEquals(.5F, MarkerVisibility.rowOpacity(4.5F / 8, 4), .0001F);
        for (int row = 0; row < HologramPixels.MARKER_ROWS; row++)
            assertEquals(1, MarkerVisibility.rowOpacity(1, row), 0);
    }

    @Test public void timingDependsOnGameTimeRatherThanRenderFrameRate() {
        for (double step : new double[]{.125, .25, .5, 1}) {
            var animation = new MarkerVisibility();
            animation.sample("npc", true, 0);
            for (double time = step; time <= 8; time += step) animation.sample("npc", true, time);
            assertEquals(1, animation.sample("npc", true, 8), .0001F);
        }
    }
}
