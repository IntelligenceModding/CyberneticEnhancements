package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.common.tutorial.TutorialScript;
import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

public class TutorialKeyRulesTest {
    private static boolean blocked(String mapping, String... revealed) {
        return TutorialKeyRules.blocked(mapping, true, Set.of(revealed)::contains);
    }

    @Test public void nothingIsBlockedOutsideTheTutorial() {
        assertFalse(TutorialKeyRules.blocked("key.advancements", false, element -> false));
        assertFalse(TutorialKeyRules.blocked("key.journeymap.fullscreen", false, element -> false));
    }

    @Test public void movementAndBasicInteractionAlwaysWork() {
        for (String key : new String[] {"key.forward", "key.left", "key.back", "key.right", "key.jump", "key.sneak", "key.sprint", "key.attack", "key.use"})
            assertFalse(key, blocked(key));
    }

    @Test public void screensAndUtilityActionsAreBlockedByDefault() {
        for (String key : new String[] {"key.advancements", "key.socialInteractions", "key.playerlist", "key.togglePerspective",
                "key.smoothCamera", "key.pickItem", "key.spectatorOutlines", "key.saveToolbarActivator", "key.loadToolbarActivator"})
            assertTrue(key, blocked(key, "hotbar", "inventory", "chat"));
    }

    @Test public void otherModsKeysAreBlockedWhateverIsRevealed() {
        assertTrue(blocked("key.journeymap.fullscreen_toggle", "hotbar", "inventory", "chat", "archive"));
        assertTrue(blocked("key.somemod.open_menu"));
        assertTrue(blocked("key.cyberneticenhancements.toggle_player_call_window", "calls", "quests"));
    }

    @Test public void actionsUnlockWithTheirInterfaceElement() {
        assertTrue(blocked("key.inventory"));
        assertFalse(blocked("key.inventory", "inventory"));
        assertTrue(blocked("key.chat", "hotbar"));
        assertFalse(blocked("key.chat", "chat"));
        assertFalse(blocked("key.command", "chat"));
        assertTrue(blocked("key.hotbar.3"));
        assertFalse(blocked("key.hotbar.3", "hotbar"));
        assertTrue(blocked("key.cyberneticenhancements.open_archive", "hotbar"));
        assertFalse(blocked("key.cyberneticenhancements.open_archive", "archive"));
        assertFalse(blocked("key.cyberneticenhancements.call_answer_1", "calls"));
        assertTrue(blocked("key.cyberneticenhancements.call_answer_1", "quests"));
    }

    @Test public void singleActionsUnlockWithoutTheirElementAndNeedNoTableEntry() {
        assertFalse(blocked("key.togglePerspective", "key:key.togglePerspective"));
        assertTrue(blocked("key.advancements", "key:key.togglePerspective"));
        assertFalse(blocked("key.somemod.open_menu", "key:key.somemod.open_menu"));
        assertFalse(blocked("key.chat", "key:key.chat"));
    }

    @Test public void onlyActionsTheTutorialIntroducesCountAsUnlockable() {
        assertTrue(TutorialKeyRules.unlockable("key.chat"));
        assertTrue(TutorialKeyRules.unlockable("key.cyberneticenhancements.open_archive"));
        assertFalse(TutorialKeyRules.unlockable("key.advancements"));
        assertFalse(TutorialKeyRules.unlockable("key.somemod.open_menu"));
    }

    @Test public void everyUnlockingElementIsOneTheScriptCanReveal() {
        assertTrue(TutorialScript.ELEMENTS.containsAll(TutorialKeyRules.elements()));
    }
}
