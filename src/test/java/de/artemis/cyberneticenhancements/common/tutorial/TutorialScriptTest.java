package de.artemis.cyberneticenhancements.common.tutorial;

import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class TutorialScriptTest {
    @Test public void chaptersFlattenIntoOrderedStepsWithTheirTriggers() {
        var script = TutorialScript.parse(JsonParser.parseString("""
                { "chapters": [
                  { "title": "a", "steps": [
                    { "id": "wait", "objective": "o1", "trigger": { "type": "ticks", "ticks": 20 } },
                    { "id": "walk", "objective": "o2", "trigger": { "type": "area", "x": 3, "y": 0, "z": -4, "relative": true, "radius": 2 } } ] },
                  { "title": "b", "steps": [
                    { "id": "open", "objective": "o3", "args": ["key:k"], "trigger": { "type": "signal", "signal": "archive_opened" } } ] } ] }
                """));
        assertEquals(3, script.steps().size());
        assertEquals("b", script.steps().get(2).chapter());
        assertEquals(java.util.List.of("key:k"), script.steps().get(2).args());
        assertEquals(TutorialScript.TriggerType.AREA, script.steps().get(1).trigger().type());
        assertTrue(script.steps().get(1).trigger().relative());
        assertEquals(-4, script.steps().get(1).trigger().pos().getZ());
        assertEquals("archive_opened", script.steps().get(2).trigger().signal());
    }

    @Test public void revealedElementsAccumulateAndStayIntroduced() {
        var script = TutorialScript.parse(JsonParser.parseString("""
                { "chapters": [ { "title": "a", "steps": [
                  { "id": "1", "objective": "o", "trigger": { "type": "ticks", "ticks": 1 } },
                  { "id": "2", "objective": "o", "reveal": ["archive"], "trigger": { "type": "ticks", "ticks": 1 } },
                  { "id": "3", "objective": "o", "reveal": ["hotbar", "archive"], "trigger": { "type": "ticks", "ticks": 1 } } ] } ] }
                """));
        assertEquals(java.util.List.of(), script.revealed(0));
        assertEquals(java.util.List.of("archive"), script.revealed(1));
        assertEquals(java.util.List.of("archive", "hotbar"), script.revealed(2));
        assertThrows(RuntimeException.class, () -> TutorialScript.parse(JsonParser.parseString(
                "{ \"chapters\": [ { \"title\": \"a\", \"steps\": [ { \"id\": \"x\", \"objective\": \"o\", \"reveal\": [\"minimap\"], \"trigger\": { \"type\": \"ticks\", \"ticks\": 1 } } ] } ] }")));
    }

    @Test public void invalidScriptsAreRejected() {
        assertThrows(RuntimeException.class, () -> TutorialScript.parse(JsonParser.parseString("{ \"chapters\": [] }")));
        assertThrows(RuntimeException.class, () -> TutorialScript.parse(JsonParser.parseString(
                "{ \"chapters\": [ { \"title\": \"a\", \"steps\": [ { \"id\": \"x\", \"objective\": \"o\", \"trigger\": { \"type\": \"teleport\" } } ] } ] }")));
        assertThrows(RuntimeException.class, () -> TutorialScript.parse(JsonParser.parseString(
                "{ \"chapters\": [ { \"title\": \"a\", \"steps\": [ { \"id\": \"x\", \"objective\": \"o\" } ] } ] }")));
    }
}
