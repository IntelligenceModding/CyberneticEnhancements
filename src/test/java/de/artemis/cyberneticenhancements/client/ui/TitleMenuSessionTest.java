package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class TitleMenuSessionTest {
    @Test public void waitsForTheReadyTitleBeforeStartingAndSurvivesAllChildMenus() {
        var session = new TitleMenuSession();
        assertFalse(session.update(false, false)); // Startup loading/intro.
        assertTrue(session.update(false, true));
        for (int tick = 0; tick < 1000; tick++)
            assertTrue(session.update(false, false)); // Options, world creation, connection, reload.
        assertTrue(session.update(false, true)); // Return to title without restarting.
    }

    @Test public void enteringAnyWorldEndsTheSessionUntilAnotherTitleVisit() {
        var session = new TitleMenuSession();
        session.update(false, true);
        assertFalse(session.update(true, false));
        assertFalse(session.update(true, true)); // In-world screens cannot restart title audio.
        assertFalse(session.update(false, false));
        assertTrue(session.update(false, true));
    }
}
