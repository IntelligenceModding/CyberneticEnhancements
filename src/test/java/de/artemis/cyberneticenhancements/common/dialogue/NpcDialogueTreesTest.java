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
            int cacheChoiceId = type == NpcType.FIXER ? 1020 : 1301;
            var choice = tree.choice(tree.rootNodeId(), cacheChoiceId);
            assertEquals(NpcDialogueView.DIALOGUE, choice.view());
            assertEquals(0, choice.trustDelta());
        }
    }
    @Test
    public void everyTypeHasReachableExpandedBranchesAndValidChoices() throws Exception {
        String language = Files.readString(Path.of("src/generated/resources/assets/cyberneticenhancements/lang/en_us.json"));
        Set<Integer> ids = new HashSet<>();
        for (NpcType type : NpcType.values()) {
            NpcDialogueTree tree = NpcDialogueTrees.tree(type);
            assertSame(tree, NpcDialogueTrees.tree(type));
            assertEquals(6, tree.nodes().size());
            Set<Integer> visited = new HashSet<>();
            ArrayDeque<Integer> pending = new ArrayDeque<>();
            pending.add(tree.rootNodeId());
            while (!pending.isEmpty()) {
                int id = pending.remove();
                if (!visited.add(id)) continue;
                NpcDialogueNode node = tree.node(id);
                assertTrue(language.contains(node.npcLineKey()));
                assertTrue(node.choices().size() <= 4);
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
