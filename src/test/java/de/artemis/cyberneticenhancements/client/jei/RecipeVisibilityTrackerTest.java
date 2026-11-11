package de.artemis.cyberneticenhancements.client.jei;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class RecipeVisibilityTrackerTest {
    @Test
    public void learnedRecipesAppearImmediatelyWithoutPublishingLockedOnes() {
        var tracker = new RecipeVisibilityTracker<String, String>();
        var initial = tracker.update(Map.of("basic", "public recipe"));
        assertEquals(java.util.List.of("public recipe"), initial.add());
        assertTrue(initial.hide().isEmpty());
        assertTrue(tracker.update(Map.of("basic", "public recipe")).add().isEmpty());

        var learned = tracker.update(Map.of("basic", "public recipe", "apogee", "learned recipe"));
        assertEquals(java.util.List.of("learned recipe"), learned.add());
        assertTrue(learned.hide().isEmpty());
        assertTrue(learned.show().isEmpty());
    }

    @Test
    public void clearedKnowledgeHidesRecipesAndRelearningDoesNotDuplicateThem() {
        var tracker = new RecipeVisibilityTracker<String, String>();
        tracker.update(Map.of("apogee", "learned recipe"));
        assertEquals(java.util.List.of("learned recipe"), tracker.update(Map.of()).hide());
        assertTrue(tracker.update(Map.of()).hide().isEmpty());
        var relearned = tracker.update(Map.of("apogee", "learned recipe"));
        assertTrue(relearned.add().isEmpty());
        assertEquals(java.util.List.of("learned recipe"), relearned.show());
    }

    @Test
    public void datapackChangesReplaceOldRecipesAndWorldChangesClearState() {
        var tracker = new RecipeVisibilityTracker<String, String>();
        tracker.update(Map.of("design", "old ingredients"));
        var changed = tracker.update(Map.of("design", "new ingredients"));
        assertEquals(java.util.List.of("old ingredients"), changed.hide());
        assertEquals(java.util.List.of("new ingredients"), changed.add());
        assertEquals(java.util.List.of("new ingredients"), tracker.update(Map.of()).hide());
        tracker.clear();
        var nextWorld = tracker.update(Map.of("design", "new ingredients"));
        assertEquals(java.util.List.of("new ingredients"), nextWorld.add());
        assertTrue(nextWorld.show().isEmpty());
    }

    @Test
    public void aSchematicUnlockCanRevealMultipleRecipesTogether() {
        var tracker = new RecipeVisibilityTracker<String, String>();
        assertTrue(tracker.update(Map.of()).add().isEmpty());
        var unlocked = Map.of("first", "first recipe", "second", "second recipe");
        assertEquals(java.util.Set.copyOf(unlocked.values()), java.util.Set.copyOf(tracker.update(unlocked).add()));
        assertEquals(2, tracker.update(Map.of()).hide().size());
    }
}