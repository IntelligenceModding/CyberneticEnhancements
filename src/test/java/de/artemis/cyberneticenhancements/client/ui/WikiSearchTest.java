package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class WikiSearchTest {
    private static WikiSearch.Document doc(String id, String title, String category, String... body) {
        return new WikiSearch.Document(id, title, category, "", List.of(body));
    }

    @Test public void titleMatchesRankAheadOfIncidentalMentionsAndOrderIsStable() {
        var docs = List.of(doc("armor", "Armor", "Cyberware", "Repair Station restores integrity."),
                doc("repair", "Repair Station", "Stations", "Clear Neural Faults before repairing integrity."));
        assertEquals(List.of("repair", "armor"), WikiSearch.search(docs, "repair station").stream().map(WikiSearch.Hit::id).toList());
        assertEquals("repair", WikiSearch.search(docs.reversed(), "station repair").getFirst().id());
    }

    @Test public void queryWordsMatchAcrossFieldsAndInAnyOrder() {
        var docs = List.of(doc("breach", "Breach", "Quests", "Netrunner", "Missing schematic: 30%", "XP: 35"));
        assertEquals("breach", WikiSearch.search(docs, "schematic netrunner quest").getFirst().id());
        assertEquals("breach", WikiSearch.search(docs, "quest netrunner schematic").getFirst().id());
        assertTrue(WikiSearch.search(docs, "netrunner bananas").isEmpty());
    }

    @Test public void spacingPunctuationAccentsAndTypedTierNumbersAreNormalized() {
        var docs = List.of(doc("clinic", "Cliníc", "Stations / Repair", "Tier 3", "Neural Fault: offline"));
        assertEquals("clinic", WikiSearch.search(docs, "  CLINIC,   neural-fault ").getFirst().id());
        assertEquals("clinic", WikiSearch.search(docs, "tier3").getFirst().id());
        assertTrue(WikiSearch.search(docs, "tier1").isEmpty());
        assertTrue(WikiSearch.search(docs, "   ... ").isEmpty());
    }

    @Test public void familiarTermsPrefixesAndSmallTyposFindTheRightMechanic() {
        var docs = List.of(doc("recycler", "Recycler Station", "Stations", "Recovered materials"),
                doc("repair", "Repair Station", "Stations", "Neural Fault"),
                doc("psycho", "Cyberpsycho Sighting", "Quests", "Combat reward"),
                doc("psychosis", "Cyberpsychosis", "Systems", "Chrome capacity"));
        assertEquals("recycler", WikiSearch.search(docs, "recycling").getFirst().id());
        assertEquals("repair", WikiSearch.search(docs, "how do I repair neural fualt").getFirst().id());
        assertEquals("repair", WikiSearch.search(docs, "repa stat").getFirst().id());
        assertEquals("psychosis", WikiSearch.search(docs, "chrome headroom").getFirst().id());
        assertEquals(List.of("psychosis"), WikiSearch.search(docs, "cyberpsychosis").stream().map(WikiSearch.Hit::id).toList());
    }

    @Test public void rewardsAreSearchableUsingCurrencyAndExperienceNames() {
        var docs = List.of(doc("delivery", "Delivery", "Quests", "Money: 150-180€$", "XP: 15"));
        for (String query : List.of("quest money", "contract eurodollars", "delivery €$", "delivery experience"))
            assertEquals(query, "delivery", WikiSearch.search(docs, query).getFirst().id());
    }

    @Test public void resultContextExplainsTheMatchingPassage() {
        var docs = List.of(doc("repair", "Repair Station", "Stations", "Uses materials.", "Neural Fault: first cycle clears the fault."));
        assertTrue(WikiSearch.search(docs, "neural fault").getFirst().context().startsWith("Neural Fault:"));
    }
}
