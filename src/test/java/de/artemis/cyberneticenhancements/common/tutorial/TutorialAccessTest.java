package de.artemis.cyberneticenhancements.common.tutorial;

import com.google.gson.JsonParser;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class TutorialAccessTest {
    private static TutorialScript script(String steps) {
        return TutorialScript.parse(JsonParser.parseString("{ \"chapters\": [ { \"title\": \"a\", \"steps\": [" + steps + "] } ] }"));
    }

    private static String step(String id, String access) {
        return "{ \"id\": \"" + id + "\", \"objective\": \"o\", " + access + " \"trigger\": { \"type\": \"ticks\", \"ticks\": 1 } }";
    }

    @Test public void allowancesLastOnlyForTheStepThatGrantedThem() {
        var script = script(step("1", "") + "," + step("2", "\"allow\": [\"chat\"],") + "," + step("3", ""));
        assertFalse(script.accessAt(0).allows("chat"));
        assertTrue(script.accessAt(1).allows("chat"));
        assertFalse(script.accessAt(2).allows("chat"));
    }

    @Test public void revealStaysAndHideTakesItAwayAgain() {
        var script = script(step("1", "\"reveal\": [\"archive\", \"chat\"],") + "," + step("2", "\"hide\": [\"chat\"],") + "," + step("3", ""));
        assertEquals(List.of("archive", "chat"), script.revealed(0));
        assertEquals(List.of("archive"), script.revealed(1));
        assertEquals(List.of("archive"), script.revealed(2));
    }

    @Test public void hidingBeatsAnAllowanceMadeEarlierInTheSameStep() {
        var access = new TutorialAccess().allow(List.of("chat")).hide(List.of("chat"));
        assertFalse(access.allows("chat"));
        assertEquals(List.of(), access.snapshot());
    }

    @Test public void aStepCanHideAndThenAllowTheSameThingForItself() {
        var script = script(step("1", "\"reveal\": [\"chat\"],") + "," + step("2", "\"hide\": [\"chat\"], \"allow\": [\"chat\"],") + "," + step("3", ""));
        assertTrue(script.accessAt(1).allows("chat"));
        assertFalse(script.accessAt(2).allows("chat"));
    }

    @Test public void singleKeyActionsCanBeUnlockedWithoutAnElement() {
        var script = script(step("1", "\"allow\": [\"key:key.togglePerspective\"],"));
        assertTrue(script.accessAt(0).allows("key:key.togglePerspective"));
        assertThrows(RuntimeException.class, () -> script(step("1", "\"allow\": [\"key:\"],")));
        assertThrows(RuntimeException.class, () -> script(step("1", "\"hide\": [\"minimap\"],")));
    }

    @Test public void changesMadeWhileRunningSurviveUntilTheNextStepEnds() {
        var access = new TutorialAccess().reveal(List.of("archive")).allow(List.of("chat"));
        assertTrue(access.allows("chat") && access.allows("archive"));
        access.endStep();
        assertFalse(access.allows("chat"));
        assertTrue(access.allows("archive"));
    }
}
