package de.artemis.cyberneticenhancements.client.ui;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class PsychosisHudFadeTest {
    @Test public void crosshairColorRestorationDoesNotDoubleFadeLaterHudElements() {
        PsychosisHudFade.begin(.6F);
        float before=PsychosisHudFade.alpha(1);
        PsychosisHudFade.withoutFade(() -> assertEquals(1,PsychosisHudFade.alpha(1),0));
        assertEquals(before,PsychosisHudFade.alpha(1),0);
        try {
            PsychosisHudFade.withoutFade(() -> { throw new IllegalStateException("draw failed"); });
            fail("Expected draw failure");
        } catch(IllegalStateException expected) { }
        assertEquals(before,PsychosisHudFade.alpha(1),0);
    }
    @After public void reset() { PsychosisHudFade.end(); }
    @Test public void renderersResettingColorStillFadeAndWorldDrawingIsUnaffectedAfterward() {
        assertEquals(.8F, PsychosisHudFade.alpha(.8F), 0);
        PsychosisHudFade.begin(.75F);
        assertEquals(.25F, PsychosisHudFade.alpha(1), 0);
        assertEquals(.2F, PsychosisHudFade.alpha(.8F), .0001F);
        PsychosisHudFade.end();
        assertEquals(1, PsychosisHudFade.alpha(1), 0);
        assertEquals(.8F, PsychosisHudFade.alpha(.8F), 0);
    }
    @Test public void fadingBackInRestoresTheHudWithoutOvershooting() {
        float previous = 0;
        for (float amount : new float[]{1, .9F, .5F, .1F, 0}) {
            PsychosisHudFade.begin(amount);
            float current = PsychosisHudFade.alpha(1);
            assertTrue(current >= previous && current <= 1);
            previous = current;
        }
        assertEquals(1, previous, 0);
    }
}
