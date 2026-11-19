package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.entity.NpcType;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class NpcDialogueTreesTest {
    @Test
    public void cacheQuestionsAreAvailableWithoutNavigatingOtherTopics() {
        for (NpcType type : new NpcType[]{NpcType.FIXER, NpcType.NETRUNNER}) {
            var tree = NpcDialogueTrees.tree(type);
            var choice = tree.node(tree.rootNodeId()).choices().stream().filter(candidate -> candidate.view() == NpcDialogueView.CACHE_INTEL).findFirst().orElseThrow();
            assertEquals(NpcDialogueView.CACHE_INTEL, choice.view());
            assertEquals(0, choice.trustDelta());
        }
    }
    @Test
    public void mainMenuOffersEverySupportedActionDirectlyAndNoUnsupportedActions() {
        for (NpcType type : NpcType.values()) {
            var tree = NpcDialogueTrees.tree(type);
            var choices = tree.node(tree.rootNodeId()).choices();
            var views = java.util.EnumSet.noneOf(NpcDialogueView.class);
            choices.forEach(choice -> views.add(choice.view()));
            assertEquals(type.supportsShop(), views.contains(NpcDialogueView.SHOP));
            assertEquals(!NpcService.forNpc(type).isEmpty(), views.contains(NpcDialogueView.SERVICES));
            assertTrue(views.contains(NpcDialogueView.DIALOGUE));
            // A world encounter's contact opens straight onto their one quest, so no choice leads there.
            if (type != NpcType.WORLD_MED_TECH) assertEquals(type.supportsContracts(), views.contains(NpcDialogueView.CONTRACTS));
            assertEquals(type.supportsIntel(), views.contains(NpcDialogueView.CACHE_INTEL));
            for (var choice : choices) {
                assertEquals(0, choice.trustDelta());
                assertEquals(0, choice.annoyanceDelta());
            }
        }
    }
    @Test
    public void everyTypeHasReachableExpandedBranchesAndValidChoices() throws Exception {
        String language = Files.readString(Path.of("src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"));
        Set<Integer> ids = new HashSet<>();
        for (NpcType type : NpcType.values()) {
            NpcDialogueTree tree = NpcDialogueTrees.tree(type);
            assertSame(tree, NpcDialogueTrees.tree(type));
            if (type == NpcType.WORLD_MED_TECH) { assertEquals(3, tree.nodes().size()); continue; } // A short quest-only talk.
            assertEquals(6, tree.nodes().size());
            Set<Integer> visited = new HashSet<>();
            ArrayDeque<Integer> pending = new ArrayDeque<>();
            pending.add(tree.rootNodeId());
            while (!pending.isEmpty()) {
                int id = pending.remove();
                if (!visited.add(id)) continue;
                NpcDialogueNode node = tree.node(id);
                assertTrue(language.contains(node.npcLineKey()));
                assertTrue(node.choices().size() <= 5);
                for (NpcDialogueChoice choice : node.choices()) {
                    assertTrue("Duplicate choice " + choice.id(), ids.add(choice.id()));
                    assertTrue(language.contains(choice.translationKey()));
                    assertTrue(language.contains(choice.playerLineKey()));
                    assertNotNull(tree.node(choice.nextNodeId()));
                    if (choice.trustDelta() > 0) assertFalse(choice.rewardMemoryKey().isBlank());
                    pending.add(choice.nextNodeId());
                }
            }
            assertEquals(tree.nodes().keySet(), visited);
        }
    }
}
