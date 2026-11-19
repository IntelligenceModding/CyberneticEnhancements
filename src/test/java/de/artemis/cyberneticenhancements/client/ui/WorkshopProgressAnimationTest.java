package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorkshopProgressAnimationTest {
    @Test public void delayedServerUpdatesCannotMakeAnUnfinishedJobLookComplete() {
        var animation = new WorkshopProgressAnimation();
        animation.sample(0, "recipe", 30, 100, true, 0);
        for (int now = 20; now <= 1000; now += 20) {
            var frame = animation.sample(now, "recipe", 30, 100, true, 0);
            assertTrue(frame.progress() >= .3F && frame.progress() <= .31001F);
            assertFalse(frame.completed());
        }
        var resumed = animation.sample(1020, "recipe", 80, 100, true, 0);
        assertTrue(resumed.progress() > .31F && resumed.progress() < .8F);
    }

    @Test public void completionWaitsForRealOutputAndThenReturnsToIdle() {
        var animation = new WorkshopProgressAnimation();
        animation.sample(0, "recipe", 95, 100, true, 3);
        animation.sample(20, "recipe", 98, 100, true, 0); // Earlier output collected mid-job.
        assertFalse(animation.sample(40, "recipe", 0, 100, false, 0).completed());
        var completed = animation.sample(80, "recipe", 0, 100, false, 1);
        assertTrue(completed.completed());
        assertTrue(completed.completion() > 0 && completed.completion() < 1);
        for (int now = 100; now <= 600; now += 20) completed = animation.sample(now, "recipe", 0, 100, false, 1);
        assertEquals(1, completed.progress(), .001);
        assertTrue(completed.completed());
        for (int now = 620; now <= 1500; now += 20) completed = animation.sample(now, "recipe", 0, 100, false, 1);
        assertFalse(completed.completed());
        assertTrue(completed.progress() < .001F && completed.labelOpacity() < .001F);
    }

    @Test public void cancellationAndRecipeChangesDoNotShowACompletionFlash() {
        var animation = new WorkshopProgressAnimation();
        animation.sample(0, "first", 40, 100, true, 2);
        var canceled = animation.sample(20, "first", 0, 100, false, 2);
        assertFalse(canceled.completed());
        assertEquals(0, canceled.completion(), 0);
        assertTrue(canceled.progress() > 0 && canceled.progress() < .4F);
        var changed = animation.sample(40, "second", 0, 100, false, 2);
        assertEquals(0, changed.progress(), 0);
        assertFalse(changed.completed());
    }
}
