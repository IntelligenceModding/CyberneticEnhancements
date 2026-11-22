package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.economy.EconomyBalance;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import java.util.Arrays;
import java.util.List;

/** Read-only descriptions of the same rules used to generate contracts. */
public final class QuestGuide {
    private QuestGuide() {}

    public static List<NpcType> issuers(QuestType type, boolean survey) {
        return Arrays.stream(NpcType.values()).filter(NpcType::supportsContracts)
                .filter(npc -> PlayerQuestManager.primaryQuestType(npc) == type
                        || PlayerQuestManager.weightedQuestPool(npc, 3).contains(type))
                .filter(npc -> type != QuestType.INVESTIGATION || (npc == NpcType.TECHIE) == survey)
                .toList();
    }

    public static int money(QuestType type, int tier, NpcType npc, boolean maximum) {
        int amount = EconomyBalance.tier("quest." + type.id() + ".base", tier);
        if (maximum) amount += EconomyBalance.tier("quest." + type.id() + ".spread", tier);
        if (type == QuestType.RECOVERY) {
            int compensation = maximum ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            for (int index = 0; index < Math.min(5, 2 + tier); index++) {
                int value = QuestContract.targetCountFor(type, tier, index) * EconomyBalance.value("recovery.unit." + index);
                compensation = maximum ? Math.max(compensation, value) : Math.min(compensation, value);
            }
            amount += compensation;
        }
        return Math.max(1, Math.round(amount * PlayerQuestManager.moneyProfileMultiplier(npc)));
    }

    public static Odds odds(double corePercent, double discoveryPercent) {
        double discovery = (100 - corePercent) * discoveryPercent / 100;
        return new Odds(corePercent, discovery, 100 - corePercent - discovery);
    }

    public static int difficulty(QuestType type, int tier) {
        int base = switch (type) {
            case DELIVERY, RECOVERY, LOCATE, SALVAGE -> 1;
            case EXTRACTION, INVESTIGATION -> 2;
            case BREACH, ELIMINATION -> 3;
            case CYBERPSYCHO -> 4;
        };
        return Math.clamp(base + Math.clamp(tier, 1, 3) - 1, 1, 5);
    }

    public record Odds(double core, double discovery, double none) {}
}
