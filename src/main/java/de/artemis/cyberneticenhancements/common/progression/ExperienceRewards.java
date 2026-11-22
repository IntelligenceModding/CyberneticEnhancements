package de.artemis.cyberneticenhancements.common.progression;

import de.artemis.cyberneticenhancements.common.block.RelicCacheTier;
import de.artemis.cyberneticenhancements.common.quest.QuestType;

public final class ExperienceRewards {
    public static final int CITY_NPC = 2;
    public static final int MAX_TAC = 25;
    public static final int SCHEMATIC = 10;

    private ExperienceRewards() {}

    public static int cyberpsycho(int tier) {
        return 50 + 25 * (Math.clamp(tier, 1, 3) - 1);
    }

    public static int relicCache(RelicCacheTier tier) {
        return switch (tier) {
            case UNCOMMON -> 20;
            case RARE -> 35;
            case EPIC -> 55;
            case LEGENDARY -> 80;
        };
    }

    public static int quest(QuestType type, int tier) {
        int rank = Math.clamp(tier, 1, 3) - 1;
        return switch (type) {
            case DELIVERY -> 15 + 10 * rank;
            case RECOVERY -> 20 + 15 * rank;
            case EXTRACTION -> 30 + 20 * rank;
            case BREACH, ELIMINATION -> 35 + 20 * rank;
            case INVESTIGATION -> 40 + 25 * rank;
            case CYBERPSYCHO -> 60 + 30 * rank;
            case LOCATE -> 30 + 15 * rank;
            case SALVAGE -> 25 + 15 * rank;
        };
    }
}
