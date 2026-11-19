package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.WikiArticleLayout.Block;
import de.artemis.cyberneticenhancements.client.ui.WikiSearch;
import net.minecraft.network.chat.Component;
import org.junit.Test;
import java.util.List;
import static de.artemis.cyberneticenhancements.client.screen.CyberwareWikiData.*;
import static org.junit.Assert.*;

public class WikiSearchIndexTest {
    private static Component text(String value) { return Component.literal(value); }
    private static WikiEntry entry(String id, String title, List<ArticleSection> sections) {
        return new WikiEntry(id, text(title), null, text("Quest"), text("Details"), List.of(), sections);
    }

    @Test public void indexesRewardTableLabelsValuesAndNotes() {
        var table = new ReferenceTableVisual(List.of(new ReferenceGroup(text("Guaranteed rewards"),
                List.of(new Block(text("Netrunner"), text("1040-1196€$"))), List.of(text("Restricted core chance: 10%")))));
        var doc = WikiSearchIndex.document(entry("psycho", "Cyberpsycho Sighting", List.of(
                new ArticleSection(text("Tier 1"), List.of(), table))), "Quests");
        for (String query : List.of("netrunner money", "restricted core", "1040", "tier1"))
            assertEquals(query, "psycho", WikiSearch.search(List.of(doc), query).getFirst().id());
    }

    @Test public void includesServiceDescriptionsAndUpgradeChanges() {
        var visual = new ServiceOperationsVisual(List.of(text("Repair")),
                List.of(new ServiceOperationVisual(text("Service"), text("Clears Neural Fault"), null, List.of(), null)),
                List.of(new UpgradeStageVisual(text("U1"), List.of(), List.of(text("Chrome capacity +7")))));
        var doc = WikiSearchIndex.document(entry("implant", "Implant", List.of(new ArticleSection(text("Workshop"), List.of(), visual))), "Cyberware");
        assertEquals("implant", WikiSearch.search(List.of(doc), "neural fault").getFirst().id());
        assertEquals("implant", WikiSearch.search(List.of(doc), "chrome headroom").getFirst().id());
    }

    @Test public void searchesTheWikiTreeIncludingParentCategoriesWithoutLiveTabDuplicates() {
        var section = new WikiSection("quest_guide", text("Guide"), List.of(entry("guide", "Delivery", List.of())));
        var topics = List.of(new WikiTopic("quests", text("Quests"), List.of(
                new WikiTopic("types", text("Contract types"), List.of(), section)), null));
        var docs = WikiSearchIndex.build(List.of(new ArchiveTab("wiki", text("Wiki"), text(""), text(""), topics),
                new ArchiveTab("quests", text("Live contracts"), text(""), text(""), topics)));
        assertEquals(1, docs.size());
        assertEquals("guide", WikiSearch.search(docs, "quest delivery").getFirst().id());
        assertTrue(docs.getFirst().category().contains("Quests / Contract types"));
    }
}
