package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiOpeningAnimationTest {
    @Test
    public void disabledOpeningIsImmediatelyVisibleAndInteractiveAndNeverRestarts() {
        UiOpeningAnimation animation = new UiOpeningAnimation(nanos(1000), 2400, 650, false);
        var immediate = animation.sample(nanos(1000));
        assertTrue(immediate.complete());
        assertEquals(1, immediate.bootLinearProgress(), 0);
        assertEquals(0, immediate.bootOpacity(), 0);
        assertEquals(1, immediate.wikiOpacity(), 0);
        assertEquals(1, immediate.wikiScale(), 0);
        assertEquals(0, immediate.wikiOffsetY(), 0);
        for (long timestamp : new long[]{0, 1200, 2400, 3050, 5000})
            assertSame(immediate, animation.sample(nanos(timestamp)));
    }

    @Test
    public void loadingPanelLeavesBeforeTheWikiFinishesEntering() {
        UiOpeningAnimation animation = new UiOpeningAnimation(0, 2400, 650);
        var loading = animation.sample(nanos(1200));
        assertTrue(loading.bootProgress() > 0 && loading.bootProgress() < 1);
        assertEquals(1, loading.bootOpacity(), 0);
        assertEquals(0, loading.wikiOpacity(), 0);
        var entering = animation.sample(nanos(2700));
        assertEquals(0, entering.bootOpacity(), 0);
        assertTrue(entering.wikiOpacity() > 0 && entering.wikiOpacity() < 1);
        assertFalse(entering.complete());
        var complete = animation.sample(nanos(3050));
        assertTrue(complete.complete());
        assertEquals(0, complete.bootOpacity(), 0);
        assertEquals(1, complete.wikiOpacity(), 0);
        assertEquals(1, complete.wikiScale(), 0);
        assertEquals(0, complete.wikiOffsetY(), 0);
    }

    @Test
    public void completedOpeningCannotReturnToTheLoadingScreen() {
        UiOpeningAnimation animation = new UiOpeningAnimation(0, 2400, 650);
        var complete = animation.sample(nanos(3100));
        for (long timestamp : new long[]{0, 1200, 2400, 3049, 5000}) {
            assertSame(complete, animation.sample(nanos(timestamp)));
        }
    }

    @Test
    public void frameTimingDoesNotChangeTheAnimation() {
        UiOpeningAnimation reference = new UiOpeningAnimation(0, 2400, 650);
        var expected = reference.sample(nanos(2900));
        for (int frameMillis : new int[]{7, 16, 33, 100}) {
            UiOpeningAnimation animation = new UiOpeningAnimation(0, 2400, 650);
            for (int elapsed = 0; elapsed < 2900; elapsed += frameMillis) {
                animation.sample(nanos(elapsed));
            }
            assertEquals(expected, animation.sample(nanos(2900)));
        }
    }

    @Test
    public void opacityAndMotionSettleContinuouslyAtTheHandoff() {
        UiOpeningAnimation animation = new UiOpeningAnimation(0, 2400, 650);
        float previousOpacity = 0;
        float previousOffset = 12;
        for (int elapsed = 2400; elapsed <= 3050; elapsed++) {
            var frame = animation.sample(nanos(elapsed));
            assertTrue(frame.wikiOpacity() >= previousOpacity);
            assertTrue(frame.wikiOffsetY() <= previousOffset);
            assertTrue(frame.wikiOpacity() - previousOpacity < 0.004F);
            previousOpacity = frame.wikiOpacity();
            previousOffset = frame.wikiOffsetY();
        }
        assertEquals(1, previousOpacity, 0);
        assertEquals(0, previousOffset, 0);
    }

    @Test
    public void loadingPausesAndCatchesUpWithoutMovingBackwardsOrChangingTheDuration() {
        var animation = new UiOpeningAnimation(0, 2400, 650);
        var before = animation.sample(nanos(300));
        var after = animation.sample(nanos(500));
        float beforePause = before.bootProgress();
        float afterPause = after.bootProgress();
        assertEquals(300 / 2400.0F, before.bootLinearProgress(), 0.000001F);
        assertEquals(500 / 2400.0F, after.bootLinearProgress(), 0.000001F);
        assertTrue(beforePause > 0);
        assertEquals(beforePause, afterPause, 0);
        float afterBurst = animation.sample(nanos(900)).bootProgress();
        assertTrue(afterBurst - afterPause > 400 / 2400.0F);
        float previous = afterBurst;
        for (int elapsed = 905; elapsed < 2400; elapsed += 5) {
            var frame = animation.sample(nanos(elapsed));
            assertTrue(frame.bootProgress() >= previous && frame.bootProgress() < 1);
            assertFalse(frame.complete());
            previous = frame.bootProgress();
        }
        assertEquals(1, animation.sample(nanos(2400)).bootProgress(), 0);
        assertTrue(animation.sample(nanos(3050)).complete());
    }

    private static long nanos(long millis) {
        return millis * 1_000_000L;
    }
}
