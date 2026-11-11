package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiScrollbarTest {
    @Test
    public void VerticalGrabPreservesThumbPositionAndDraggingReachesBothEnds() {
        var bar = new UiScrollbar();
        var track = UiScrollbar.layout(20, 10, 6, 200, false, 200, 800, 400);
        assertTrue(bar.begin(track, 22, track.thumbStart() + 7));
        assertEquals(400, bar.value(track, 22, track.thumbStart() + 7), 0.001);
        assertEquals(0, bar.value(track, 22, -100), 0);
        assertEquals(800, bar.value(track, 22, 10000), 0);
        assertTrue(bar.dragging());
        bar.release();
        assertFalse(bar.dragging());
    }

    @Test
    public void HorizontalBarsUseTheSameGrabAndEndpointRules() {
        var bar = new UiScrollbar();
        var track = UiScrollbar.layout(10, 20, 200, 6, true, 200, 800, 400);
        assertTrue(bar.begin(track, track.thumbStart() + 7, 22));
        assertEquals(400, bar.value(track, track.thumbStart() + 7, 22), 0.001);
        assertEquals(0, bar.value(track, -100, 22), 0);
        assertEquals(800, bar.value(track, 10000, 22), 0);
    }

    @Test
    public void TrackClickCentersThumbAndRejectsClicksOutsideTheTrack() {
        var bar = new UiScrollbar();
        var track = UiScrollbar.layout(20, 10, 6, 200, false, 200, 800, 0);
        assertFalse(bar.begin(track, 19, 100));
        assertFalse(bar.begin(track, 22, 210));
        assertTrue(bar.begin(track, 22, 110));
        assertEquals(400, bar.value(track, 22, 110), 0.001);
    }

    @Test
    public void NonOverflowingAndTinyTracksNeverProduceOversizedThumbs() {
        var bar = new UiScrollbar();
        var full = UiScrollbar.layout(0, 0, 6, 100, false, 100, 0, 900);
        assertEquals(100, full.thumbLength());
        assertEquals(0, full.travel());
        assertFalse(bar.begin(full, 2, 10));
        var tiny = UiScrollbar.layout(0, 0, 6, 8, false, 8, 10000, 5000);
        assertEquals(8, tiny.thumbLength());
        assertEquals(0, tiny.travel());
        assertEquals(0, bar.value(tiny, 2, 100), 0);
    }

    @Test
    public void WheelScrollIsSmoothButDraggingAndShrinkingContentClampImmediately() {
        var state = new UiScrollState();
        state.add(UiScrollState.WHEEL_STEP, 100);
        state.tick();
        assertTrue(state.current > 0 && state.current < 24);
        for (int i = 0; i < 30; i++) state.tick();
        assertEquals(24, state.current, 0);
        state.set(90, 100);
        assertEquals(90, state.current, 0);
        assertEquals(90, state.target, 0);
        state.clamp(10);
        assertEquals(10, state.current, 0);
        assertEquals(10, state.target, 0);
        state.add(-100, 10);
        for (int i = 0; i < 30; i++) state.tick();
        assertEquals(0, state.current, 0);
    }
}
