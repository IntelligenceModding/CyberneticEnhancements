package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.GameplayHudVisibility.Choice;
import de.artemis.cyberneticenhancements.client.GameplayHudVisibility.Context;
import de.artemis.cyberneticenhancements.client.GameplayHudVisibility.Element;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameplayHudVisibilityTest {
    @Test public void explicitChoicesTakePrecedenceOverScreenDefaultsAndExistingSettings() {
        assertTrue(GameplayHudVisibility.resolve(Context.ARCHIVE, Element.COMPASS, Choice.SHOW, false));
        assertTrue(GameplayHudVisibility.resolve(Context.PAUSE, Element.CYBERWARE, Choice.SHOW, false));
        assertFalse(GameplayHudVisibility.resolve(Context.GAMEPLAY, Element.QUESTS, Choice.HIDE, true));
        assertFalse(GameplayHudVisibility.resolve(Context.CHAT, Element.CHAT, Choice.HIDE, true));
    }

    @Test public void defaultsPreserveThePreviousScreenRules() {
        assertTrue(GameplayHudVisibility.resolve(Context.GAMEPLAY, Element.QUESTS, Choice.DEFAULT, true));
        assertTrue(GameplayHudVisibility.resolve(Context.CHAT, Element.CYBERWARE, Choice.DEFAULT, true));
        assertFalse(GameplayHudVisibility.resolve(Context.CHAT, Element.CALLS, Choice.DEFAULT, true));
        assertTrue(GameplayHudVisibility.resolve(Context.INVENTORY, Element.COMPASS, Choice.DEFAULT, true));
        assertFalse(GameplayHudVisibility.resolve(Context.INVENTORY, Element.COMPASS, Choice.DEFAULT, false));
        assertFalse(GameplayHudVisibility.resolve(Context.PAUSE, Element.CYBERWARE, Choice.DEFAULT, true));
    }

    @Test public void serverSettingsLockOnlyTheHudRowsTheyControl() {
        try {
            EnforcedSettings.enforceForTests(ClientConfig.SPEC, List.of("hud.enabled=false", "showCyberpsychosisTimer=true"));
            assertTrue(GameplayHudVisibility.locked(Element.CYBERWARE));
            assertTrue(GameplayHudVisibility.locked(Element.COMPASS));
            assertTrue(GameplayHudVisibility.locked(Element.QUESTS));
            assertTrue(GameplayHudVisibility.locked(Element.PSYCHOSIS_TIMER));
            assertFalse(GameplayHudVisibility.locked(Element.CALLS));
            assertFalse(GameplayHudVisibility.locked(Element.EFFECTS));

            EnforcedSettings.enforceForTests(ClientConfig.SPEC, List.of("showCompass=true", "enablePlayerCalls=false"));
            assertTrue(GameplayHudVisibility.locked(Element.COMPASS));
            assertTrue(GameplayHudVisibility.locked(Element.CALLS));
            assertFalse(GameplayHudVisibility.locked(Element.QUESTS));
        } finally {
            EnforcedSettings.clearForTests();
        }
        assertFalse(GameplayHudVisibility.locked(Element.COMPASS));
    }
}
