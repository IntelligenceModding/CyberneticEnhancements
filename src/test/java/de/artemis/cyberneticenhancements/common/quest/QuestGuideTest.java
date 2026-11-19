package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.entity.NpcType;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuestGuideTest {
    @Test
    public void coreRollReducesTheEffectiveSchematicChance() {
        var odds = QuestGuide.odds(10, 40);
        assertEquals(10, odds.core(), 0.0001);
        assertEquals(36, odds.discovery(), 0.0001);
        assertEquals(54, odds.none(), 0.0001);
        for (int core : new int[]{0, 10, 100}) {
            for (int discovery : new int[]{0, 40, 100}) {
                var result = QuestGuide.odds(core, discovery);
                assertEquals(100, result.core() + result.discovery() + result.none(), 0.0001);
                assertTrue(result.none() >= 0);
            }
        }
    }

    @Test
    public void contactsRespectContractSupportAndSurveyVariant() {
        assertEquals(java.util.List.of(NpcType.TECHIE), QuestGuide.issuers(QuestType.INVESTIGATION, true));
        assertEquals(java.util.List.of(NpcType.NETRUNNER), QuestGuide.issuers(QuestType.INVESTIGATION, false));
        assertEquals(java.util.List.of(NpcType.NETRUNNER), QuestGuide.issuers(QuestType.BREACH, false));
        for (QuestType type : QuestType.values()) {
            if (type == QuestType.LOCATE) continue; // Only handed out by world encounters, never by a city contact.
            assertFalse(QuestGuide.issuers(type, false).isEmpty());
            for (NpcType npc : QuestGuide.issuers(type, false)) assertTrue(npc.supportsContracts());
        }
    }

    @Test
    public void difficultyRemainsWithinFiveStarsAndIncreasesWithTier() {
        for (QuestType type : QuestType.values()) {
            int previous = 0;
            for (int tier = 1; tier <= 3; tier++) {
                int rating = QuestGuide.difficulty(type, tier);
                assertTrue(rating >= 1 && rating <= 5);
                assertTrue(rating >= previous);
                previous = rating;
            }
        }
        assertEquals(1, QuestGuide.difficulty(QuestType.DELIVERY, 1));
        assertEquals(5, QuestGuide.difficulty(QuestType.CYBERPSYCHO, 3));
    }

    @Test
    public void recoveryRangeIncludesMaterialValueAndIssuerMultiplier() {
        // Tier 1: 12 iron x 8 + 60 base; maximum is 6 circuits x 28 + 60 + 20 spread.
        assertEquals(Math.round(156 * 0.95F), QuestGuide.money(QuestType.RECOVERY, 1, NpcType.TECHIE, false));
        assertEquals(Math.round(248 * 0.95F), QuestGuide.money(QuestType.RECOVERY, 1, NpcType.TECHIE, true));
        assertEquals(Math.round(230 * 1.04F), QuestGuide.money(QuestType.BREACH, 1, NpcType.NETRUNNER, false));
    }
}
