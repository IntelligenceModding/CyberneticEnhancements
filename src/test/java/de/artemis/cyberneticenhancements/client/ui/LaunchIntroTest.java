package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;
import static de.artemis.cyberneticenhancements.client.ui.LaunchIntroTimeline.Phase.*;

public class LaunchIntroTest {
    private long now;

    private void run(LaunchIntroTimeline timeline, long millis, boolean focused) {
        for (long passed = 0; passed < millis; passed += 50) {
            now += Math.min(50, millis - passed);
            timeline.advance(now, focused);
        }
    }

    @Test
    public void sequenceShowsFourLogoPagesThenDisclaimerAndAutomaticallyEntersTheMenu() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true);
        assertEquals(MOJANG, timeline.phase());
        run(timeline, 3_260, true);
        assertEquals(INTELLIGENCE, timeline.phase());
        run(timeline, 2_920, true);
        assertEquals(NEOFORGE, timeline.phase());
        run(timeline, 3_000, true);
        assertEquals(MINECRAFT, timeline.phase());
        assertEquals(0, timeline.phaseMillis());
        assertTrue(timeline.logoGlitch() > 0);
        run(timeline, 1_000, true);
        assertEquals(1, timeline.logoOpacity(), 0.001);
        run(timeline, 2_040, true);
        assertEquals(DISCLAIMER, timeline.phase());
        run(timeline, 2_880, true);
        assertEquals(THANK_YOU, timeline.phase());
        assertEquals(0, timeline.phaseMillis());
        run(timeline, LaunchIntroTimeline.PRESENTATION_MILLIS - LaunchIntroTimeline.THANK_YOU_START_MILLIS, true);
        assertEquals(ENTERING, timeline.phase());
        assertEquals(0, timeline.exitProgress(), 0.001);
        assertFalse("A skip must not restart the automatic transition", timeline.skipPresentation());
        run(timeline, LaunchIntroTimeline.ENTER_MILLIS, true);
        assertEquals(FINISHED, timeline.phase());
    }

    @Test
    public void thankYouAndBetaNoticesFollowTheCombinedDisclaimerWithoutInterruptingAudio() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true, 0);
        timeline.advance(15_099, true, 15_099);
        assertEquals(DISCLAIMER, timeline.phase());
        timeline.advance(15_100, true, 15_100);
        assertEquals(THANK_YOU, timeline.phase());
        assertEquals(0, timeline.phaseMillis());
        timeline.advance(17_999, true, 17_999);
        assertEquals(THANK_YOU, timeline.phase());
        timeline.advance(18_000, true, 18_000);
        assertEquals(BETA_NOTICE, timeline.phase());
        assertEquals(0, timeline.phaseMillis());
        timeline.advance(20_630, true, 20_630);
        assertEquals(BETA_NOTICE, timeline.phase());
        timeline.advance(20_640, true, -1);
        assertEquals(ENTERING, timeline.phase());
    }

    @Test
    public void unfocusedWindowAndLongFrameStallsDoNotSkipThePresentation() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true);
        run(timeline, 10_000, false);
        assertEquals(0, timeline.phaseMillis());
        timeline.advance(now + 100_000, true);
        assertEquals(MOJANG, timeline.phase());
        assertTrue(timeline.phaseMillis() <= 250);
    }

    @Test
    public void logosGlitchOnlyAtTheirEntranceAndExitAndHaveABlackSeparator() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true);
        assertTrue(timeline.logoGlitch() > 0);
        run(timeline, 1_000, true);
        assertEquals(0, timeline.logoGlitch(), 0.001);
        assertEquals(1, timeline.logoOpacity(), 0.001);
        run(timeline, 2_150, true);
        assertTrue(timeline.logoGlitch() > 0);
        run(timeline, 100, true);
        assertEquals(0, timeline.logoOpacity(), 0.001);
        assertTrue(timeline.logoGlitch() > 0);
    }

    @Test
    public void audioPlayheadControlsEveryTransitionEvenAfterRenderStalls() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true, 0);
        long[] cues = {3_260, 6_180, 9_180, 12_220};
        var before = new LaunchIntroTimeline.Phase[]{MOJANG, INTELLIGENCE, NEOFORGE, MINECRAFT};
        var after = new LaunchIntroTimeline.Phase[]{INTELLIGENCE, NEOFORGE, MINECRAFT, DISCLAIMER};
        for (int index = 0; index < cues.length; index++) {
            timeline.advance(cues[index] * 10, true, cues[index] - 1);
            assertEquals(before[index], timeline.phase());
            timeline.advance(cues[index] * 10 + 1, true, cues[index]);
            assertEquals(after[index], timeline.phase());
            assertEquals(0, timeline.phaseMillis());
        }
        timeline.advance(130_000, true, 14_800);
        assertEquals(DISCLAIMER, timeline.phase());
        assertEquals(2_580, timeline.phaseMillis());
        timeline.advance(130_050, true, -1);
        assertEquals("After the full clip ends, disclaimer time continues", 2_630, timeline.phaseMillis());
        timeline.skipPresentation();
        timeline.advance(130_100, true, 10_000);
        assertEquals("Late sound callbacks must not undo a skip", ENTERING, timeline.phase());
    }

    @Test
    public void audioDecoderWarmupAndPausedPlaybackDoNotAdvanceTheLogos() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true, 0);
        timeline.advance(10_000, true, 0);
        assertEquals(0, timeline.phaseMillis());
        timeline.advance(10_016, true, 3_000);
        timeline.advance(20_000, false, 3_100);
        assertEquals(3_000, timeline.phaseMillis());
        timeline.advance(20_016, true, 3_260);
        assertEquals(INTELLIGENCE, timeline.phase());
    }

    @Test
    public void skippingCrossfadesDirectlyIntoTheMenuWithoutWaitingForMoreInput() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true);
        assertTrue(timeline.skipPresentation());
        assertEquals(ENTERING, timeline.phase());
        assertFalse(timeline.skipPresentation());
        run(timeline, LaunchIntroTimeline.ENTER_MILLIS, true);
        assertEquals(FINISHED, timeline.phase());
    }

    @Test
    public void menuCrossfadeAdvancesBetweenGameTicksAndPausesWhenUnfocused() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true);
        timeline.advance(200, true);
        timeline.advance(400, true);
        assertTrue(timeline.skipPresentation());
        timeline.advance(408, true);
        float firstFrame = timeline.exitProgress();
        timeline.advance(416, true);
        assertTrue("A fade must advance even when no 50 ms game tick has passed", timeline.exitProgress() > firstFrame);
        float beforeFocusLoss = timeline.exitProgress();
        timeline.advance(616, false);
        assertEquals(beforeFocusLoss, timeline.exitProgress(), 0.00001);
        timeline.advance(624, true);
        assertTrue(timeline.exitProgress() > beforeFocusLoss);
        assertTrue(timeline.exitProgress() < 0.01F);
    }

    @Test
    public void menuHandoffTearsOutThenRevealsTheTitleAcrossABriefBlackSeparator() {
        var timeline = new LaunchIntroTimeline();
        timeline.advance(0, true);
        timeline.skipPresentation();
        assertFalse(timeline.menuVisible());
        assertEquals(1, timeline.menuOpacity(), 0.001);
        timeline.advance(190, true);
        assertTrue(timeline.menuGlitch() > 0.9F);
        timeline.advance(200, true);
        assertEquals(0, timeline.menuOpacity(), 0.001);
        timeline.advance(220, true);
        assertTrue(timeline.menuVisible());
        assertEquals(0, timeline.menuOpacity(), 0.001);
        assertEquals(1, timeline.menuGlitch(), 0.001);
        timeline.advance(330, true);
        assertEquals(1, timeline.menuOpacity(), 0.001);
        timeline.advance(LaunchIntroTimeline.ENTER_MILLIS, true);
        assertEquals(FINISHED, timeline.phase());
        assertEquals(0, timeline.menuGlitch(), 0.001);
    }

    @Test
    public void launchGatePreservesOnboardingAndOnlyClaimsTheFirstNativeTitle() {
        var gate = new LaunchIntroGate();
        assertFalse(gate.claim(true, false, false));
        assertFalse(gate.claim(false, false, true));
        assertTrue(gate.claim(true, false, true));
        assertFalse(gate.claim(true, false, true));
        var quickPlay = new LaunchIntroGate();
        quickPlay.gameplayStarted();
        assertFalse("Returning from a direct-to-world launch must not play an intro", quickPlay.claim(true, false, true));
        var inWorld = new LaunchIntroGate();
        assertFalse(inWorld.claim(true, true, true));
        assertFalse(inWorld.claim(true, false, true));
    }
}
