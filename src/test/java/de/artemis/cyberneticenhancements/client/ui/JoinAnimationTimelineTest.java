package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class JoinAnimationTimelineTest {
    private static JoinAnimationTimeline.Frame at(long millis) { return JoinAnimationTimeline.at(millis, 1); }

    @Test public void startsWithTheViewClosedAndEndsCompletelyClean() {
        var first = at(0);
        assertEquals(0, first.open(), 0); assertEquals(0, first.lineWidth(), 0); assertEquals(1, first.vignette(), 0);
        var last = at(JoinAnimationTimeline.DURATION);
        assertEquals(1, last.open(), 0); assertEquals(0, last.glitch(), 0); assertEquals(0, last.vignette(), 0);
        assertEquals(0, last.fade(), 0);
    }

    @Test public void theLineGrowsThenTheViewOpensWithoutEverClosingAgain() {
        assertEquals(1, at(JoinAnimationTimeline.LINE_END).lineWidth(), 1e-6);
        float previous = 0;
        for (long t = 0; t <= JoinAnimationTimeline.DURATION; t += 20) {
            float open = at(t).open();
            assertTrue("open must never decrease at " + t, open >= previous);
            previous = open;
        }
        assertEquals(0, at(JoinAnimationTimeline.LINE_END).open(), 1e-6);
        assertEquals(1, at(JoinAnimationTimeline.OPEN_END).open(), 1e-6);
    }

    @Test public void tearingIsBriefAndMild() {
        assertEquals(0, at(100).glitch(), 0);
        assertTrue(at(JoinAnimationTimeline.LINE_END + 20).glitch() <= 0.7F);
        assertTrue(at(JoinAnimationTimeline.LINE_END + 30).glitch() > at(JoinAnimationTimeline.LINE_END + 400).glitch());
        assertEquals(0, at(JoinAnimationTimeline.GLITCH_END).glitch(), 0);
    }

    @Test public void withScreenEffectsOffOnlyAGentleFadeRemains() {
        for (long t = 0; t <= JoinAnimationTimeline.DURATION; t += 50) {
            var frame = JoinAnimationTimeline.at(t, 0);
            assertEquals(0, frame.glitch(), 0);
            assertEquals(1, frame.open(), 0);
        }
        assertEquals(1, JoinAnimationTimeline.at(0, 0).fade(), 0);
        assertEquals(0, JoinAnimationTimeline.at(700, 0).fade(), 0);
    }

    @Test public void screenEffectsScalesTheTearingWithoutChangingTheTiming() {
        var half = JoinAnimationTimeline.at(400, 0.5);
        var full = JoinAnimationTimeline.at(400, 1);
        assertTrue(half.glitch() < full.glitch());
        assertEquals(full.open(), half.open(), 0);
    }
}
