package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.common.network.QuestProgressPayload.Objective;
import net.minecraft.network.chat.Component;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class QuestHudPresentationTest {
    private static Objective objective(int count) {
        return new Objective("recovery", Component.literal("Recovery Pull"), Component.literal(count + "/12 redstone collected"), count, 12, 1);
    }

    @Test public void noObjectiveMeansNoPanelOrPlaceholder() {
        var hud = new QuestHudPresentation();
        assertFalse(hud.sample(List.of(), 0).visible());
        assertTrue(hud.sample(List.of(), 1).objectives().isEmpty());
    }

    @Test public void appearsWithLiveProgressThenFullyRemovesTheCompletedObjective() {
        var hud = new QuestHudPresentation();
        hud.sample(List.of(objective(6)), 0);
        var entering = hud.sample(List.of(objective(6)), .09);
        assertTrue(entering.visible());
        assertEquals(.5F, entering.opacity(), .001F);
        var updated = hud.sample(List.of(objective(7)), .18);
        assertEquals(7, updated.objectives().getFirst().current());
        assertEquals(1, updated.opacity(), .001F);
        assertTrue(hud.sample(List.of(), .27).visible());
        var removed = hud.sample(List.of(), .5);
        assertFalse(removed.visible());
        assertTrue(removed.objectives().isEmpty());
    }

    @Test public void resetDoesNotCarryQuestTextIntoAnotherSession() {
        var hud = new QuestHudPresentation();
        hud.sample(List.of(objective(6)), 0);
        hud.sample(List.of(objective(6)), 1);
        hud.clear();
        assertTrue(hud.sample(List.of(), 2).objectives().isEmpty());
    }

    @Test public void hiddenQuestsAreRemovedFromPagesAndCanBeShownAgain() {
        var hud = new QuestHudPresentation();
        var quests = List.of(objective(6), new Objective("delivery", Component.literal("Delivery"),
                Component.literal("Deliver package"), 0, 1, 2), new Objective("extraction",
                Component.literal("Extraction"), Component.literal("Retrieve shard"), 0, 1, 2),
                new Objective("breach", Component.literal("Breach"), Component.literal("Open cache"), 0, 1, 2));
        hud.sample(quests, 0);
        hud.nextPage(quests.size());
        assertEquals(2, hud.sample(quests, .18).pages());

        var remaining = hud.sample(quests, .2, "recovery"::equals);
        assertEquals(quests.subList(1, 4), remaining.objectives());
        assertEquals(1, remaining.pages());
        assertEquals(0, remaining.page());

        var hidden = hud.sample(quests, .21, id -> true);
        assertTrue(hidden.objectives().isEmpty());
        assertFalse(hidden.visible());
        var restored = hud.sample(quests, .4);
        assertTrue(restored.visible());
        assertEquals(2, restored.pages());
        assertEquals(quests.subList(0, 3), restored.objectives());
    }

    @Test public void threeQuestsAppearPerPageAndEveryQuestCanBeReachedBeforeWrapping() {
        var hud = new QuestHudPresentation();
        var quests = java.util.stream.IntStream.range(0, 8).mapToObj(count -> new Objective("quest-" + count,
                Component.literal("Quest " + count), Component.literal("Collect redstone"), count, 12, 1)).toList();
        hud.sample(quests, 0);
        var first = hud.sample(quests, .18);
        assertEquals(quests.subList(0, 3), first.objectives());
        assertEquals(3, first.pages());
        hud.nextPage(quests.size());
        var second = hud.sample(quests, .27);
        assertEquals(quests.subList(3, 6), second.objectives());
        assertEquals(1, second.page());
        hud.nextPage(quests.size());
        assertEquals(quests.subList(6, 8), hud.sample(quests, .36).objectives());
        hud.nextPage(quests.size());
        assertEquals(quests.subList(0, 3), hud.sample(quests, .45).objectives());
    }

    @Test public void updatesPreserveThePageWhileCompletionClampsItAndLogoutResetsIt() {
        var hud = new QuestHudPresentation();
        var quests = java.util.stream.IntStream.range(0, 7).mapToObj(count -> new Objective("quest-" + count,
                Component.literal("Quest " + count), Component.literal("Collect redstone"), count, 12, 1)).toList();
        hud.sample(quests, 0);
        hud.nextPage(quests.size());
        hud.nextPage(quests.size());
        assertEquals(quests.subList(6, 7), hud.sample(quests, .18).objectives());
        assertEquals(2, hud.sample(quests, .2).page());
        var remaining = hud.sample(quests.subList(0, 4), .27);
        assertEquals(1, remaining.page());
        assertEquals(quests.subList(3, 4), remaining.objectives());
        assertEquals(quests.subList(3, 4), hud.sample(List.of(), .36).objectives());
        assertFalse(hud.sample(List.of(), .6).visible());
        hud.clear();
        assertEquals(0, hud.sample(quests, .7).page());
    }

    @Test public void progressAndPriorityUpdatesDoNotShuffleQuestsBetweenPages() {
        var hud = new QuestHudPresentation();
        var quests = java.util.stream.IntStream.range(0, 7).mapToObj(count -> new Objective("quest-" + count,
                Component.literal("Quest " + count), Component.literal("Collect redstone"), count, 12, 1)).toList();
        hud.sample(quests, 0);
        hud.nextPage(quests.size());
        var reordered = quests.reversed().stream().map(quest -> new Objective(quest.id(), quest.title(), quest.detail(),
                quest.current() + 1, quest.required(), 0)).toList();
        var frame = hud.sample(reordered, .18);
        assertEquals(List.of("quest-3", "quest-4", "quest-5"), frame.objectives().stream().map(Objective::id).toList());
        assertEquals(4, frame.objectives().getFirst().current());
    }
}
