package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ArchiveTabOrderTest {
    @Test
    public void savedOrderKeepsEveryTabExactlyOnceAndAppendsNewTabs() {
        assertEquals(List.of("quests", "wiki", "schematics", "new_tab"),
                ArchiveTabOrder.normalize(List.of("wiki", "schematics", "quests", "new_tab"),
                        List.of("quests", "removed", "quests", "wiki")));
    }

    @Test
    public void draggingCanMoveToBothEndsWithoutLosingTabs() {
        var order = List.of("wiki", "schematics", "quests");
        assertEquals(List.of("quests", "wiki", "schematics"), ArchiveTabOrder.move(order, "quests", 0));
        assertEquals(List.of("schematics", "quests", "wiki"), ArchiveTabOrder.move(order, "wiki", 2));
        assertEquals(order, ArchiveTabOrder.move(order, "unknown", 1));
        assertEquals(order, List.of("wiki", "schematics", "quests"));
    }

    @Test
    public void draggingStartsInItsOwnSlotUntilCrossingTheNextTabMidpoint() {
        var order = List.of("wiki", "schematics", "quests");
        var centers = List.of(35, 115, 195);
        assertEquals(0, ArchiveTabOrder.dropIndex(order, centers, "wiki", 46));
        assertEquals(1, ArchiveTabOrder.dropIndex(order, centers, "wiki", 116));
        assertEquals(2, ArchiveTabOrder.dropIndex(order, centers, "wiki", 196));
        assertEquals(0, ArchiveTabOrder.dropIndex(order, centers, "quests", 20));
    }

    @Test
    public void normalizationPreservesTheOrderAfterSavingAndReloading() {
        var available = List.of("wiki", "schematics", "banking", "quests");
        var saved = ArchiveTabOrder.move(available, "quests", 1);
        assertEquals(saved, ArchiveTabOrder.normalize(available, saved));
        assertEquals(List.of("quests", "wiki", "schematics"),
                ArchiveTabOrder.move(List.of("wiki", "schematics", "quests"), "quests", -10));
    }
}