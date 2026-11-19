package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class TarotScanAnimationTest {
    @Test public void appearanceIsIndependentOfFrameRateAndReleaseEasesBack() {
        var fast = new TarotScanAnimation();
        var slow = new TarotScanAnimation();
        for (int time = 0; time <= 400; time += 10) fast.update(time, true, .6F, false);
        for (int time = 0; time <= 400; time += 40) slow.update(time, true, .6F, false);
        assertEquals(fast.opacity(), slow.opacity(), .0001);
        assertEquals(fast.progress(), slow.progress(), .0001);
        float before = fast.progress();
        fast.update(420, true, 0, false);
        assertTrue(fast.progress() > 0 && fast.progress() < before);
        fast.update(440, false, 0, false);
        assertTrue(fast.opacity() > 0 && fast.opacity() < slow.opacity());
    }

    @Test public void completionSurvivesTargetRemovalThenFadesWithoutRestarting() {
        var animation = new TarotScanAnimation();
        for (int time = 0; time <= 300; time += 20) animation.update(time, true, .95F, false);
        animation.update(320, false, 0, true);
        assertTrue(animation.completed());
        assertTrue(animation.completion() > 0 && animation.completion() < 1);
        for (int time = 340; time <= 740; time += 20) animation.update(time, false, 0, true);
        assertTrue(animation.opacity() > .99F);
        assertEquals(1, animation.progress(), .001);
        for (int time = 760; time <= 1400; time += 20) animation.update(time, false, 0, true);
        assertTrue(animation.opacity() < .01F);
        animation.reset();
        assertFalse(animation.completed());
        assertEquals(0, animation.progress(), 0);
        assertEquals(0, animation.opacity(), 0);
    }
}
