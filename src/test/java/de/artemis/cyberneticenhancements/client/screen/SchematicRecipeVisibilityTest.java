package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.common.fabrication.SchematicKnowledge;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SchematicRecipeVisibilityTest {
    @Test
    public void cachedRecipeVisibilityFollowsItsRequiredSchematic() {

        var locked = new CyberwareWikiData.CraftingGridVisual(List.of(), null, "militech_apogee");
        var unrelated = new CyberwareWikiData.CraftingGridVisual(List.of(), null, "dynalar_sandevistan");
        var basic = new CyberwareWikiData.CraftingGridVisual(List.of(), null);
        SchematicKnowledge.clearClient();
        try {
            assertFalse(locked.unlocked());
            assertFalse(unrelated.unlocked());
            assertTrue(basic.unlocked());
            SchematicKnowledge.receive(List.of("militech_apogee"));
            assertTrue(locked.unlocked());
            assertFalse(unrelated.unlocked());
            SchematicKnowledge.clearClient();
            assertFalse(locked.unlocked());
            assertTrue(basic.unlocked());
        } finally {
            SchematicKnowledge.clearClient();
        }
    }
}
