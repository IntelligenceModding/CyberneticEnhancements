package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UiVisibilityTransitionTest {
    @Test public void fadesOutAndBackInWithoutChangingInstantly() {
        var transition = new UiVisibilityTransition(true);
        assertEquals(1F, transition.sample(false, 0), .001F);
        assertEquals(.5F, transition.sample(false, .11), .001F);
        assertEquals(0F, transition.sample(false, .22), .001F);
        assertEquals(0F, transition.sample(true, .3), .001F);
        assertEquals(.5F, transition.sample(true, .41), .001F);
        assertEquals(1F, transition.sample(true, .52), .001F);
    }

    @Test public void reversingAnUnfinishedFadePreservesItsCurrentOpacity() {
        var transition = new UiVisibilityTransition(true);
        transition.sample(false, 0);
        float midway = transition.sample(false, .11);
        assertEquals(midway, transition.sample(true, .11), .001F);
        assertEquals(.75F, transition.sample(true, .22), .001F);
        assertEquals(1F, transition.sample(true, .33), .001F);
    }

    @Test public void initialHiddenStateDoesNotFlashAndIdleStateIsStable() {
        var hidden = new UiVisibilityTransition(false);
        assertEquals(0F, hidden.sample(false, 0), .001F);
        assertEquals(0F, hidden.sample(false, 100), .001F);
        var visible = new UiVisibilityTransition(true);
        assertEquals(1F, visible.sample(true, 100), .001F);
    }
}
