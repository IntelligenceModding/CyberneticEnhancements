package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;
import static de.artemis.cyberneticenhancements.client.ui.TitleStartFlow.Stage.*;

public class TitleStartFlowTest {
    @Test public void anOpenTutorialIsOfferedBeforeTheMenu() {
        var flow = new TitleStartFlow();
        assertEquals(TUTORIAL_OFFER, flow.stage(true, false));
        assertEquals(TUTORIAL_OFFER, flow.stage(true, false)); // Resizes rebuild the title without advancing.
        assertEquals(MENU, flow.advance());
        assertEquals(MENU, flow.stage(true, false)); // Returning to the title does not replay it.
    }

    @Test public void joiningAWorldBeforeEverSeeingTheTitleUsesUpTheOffer() {
        var flow = new TitleStartFlow();
        flow.worldEntered(); // A direct launch into a world: the title is only reached after leaving it.
        assertEquals(MENU, flow.stage(true, false));
        var other = new TitleStartFlow();
        other.stage(true, false); // Seen but not yet answered, then a world is joined some other way.
        other.worldEntered();
        assertEquals(MENU, other.stage(true, false));
    }

    @Test public void aDoneOrSkippedTutorialGoesStraightToTheMenu() {
        assertEquals(MENU, new TitleStartFlow().stage(false, false));
        var flow = new TitleStartFlow();
        assertEquals(MENU, flow.stage(true, true));
        assertEquals(MENU, flow.stage(true, false));
        var decided = new TitleStartFlow();
        decided.stage(false, false);
        assertEquals(MENU, decided.stage(true, false)); // Decided once, at the first title.
    }
}
