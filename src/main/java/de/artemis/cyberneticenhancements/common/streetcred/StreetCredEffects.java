package de.artemis.cyberneticenhancements.common.streetcred;

import de.artemis.cyberneticenhancements.common.skills.SkillEffect;
import de.artemis.cyberneticenhancements.common.skills.Skills;
import net.minecraft.world.entity.player.Player;

/**
 * What a name is worth. Street Cred only ever helps: the bigger the name, the better the pay, the faster the city warms to
 * you, the lower the prices and the earlier the dangerous work is offered. Each bonus grows evenly from nothing at level 1
 * to its maximum at the top level, so every level is felt a little.
 */
public final class StreetCredEffects {
    /** Percentages reached at the top level. */
    public static final int MAX_PAY_BONUS = 10, MAX_TRUST_BONUS = 25, MAX_PRICE_DISCOUNT = 10;
    /** Levels at which issuers start offering tier 2 and tier 3 contracts regardless of how much they trust you. */
    public static final int TIER_2_LEVEL = 10, TIER_3_LEVEL = 30;

    private StreetCredEffects() {}

    /** The share of a bonus that is earned at a level, in whole percent. */
    public static int percent(int level, int max) {
        return (Math.clamp(level, 1, StreetCred.MAX_LEVEL) - 1) * max / (StreetCred.MAX_LEVEL - 1);
    }

    public static int payBonus(Player player) { return percent(StreetCred.level(player), MAX_PAY_BONUS); }
    public static int trustBonus(Player player) { return percent(StreetCred.level(player), MAX_TRUST_BONUS); }
    public static int priceDiscount(Player player) { return percent(StreetCred.level(player), MAX_PRICE_DISCOUNT); }

    /** A contract's money once the name has had its say. */
    public static int questPay(Player player, int money) {
        int bonus = payBonus(player) + Skills.percent(player, SkillEffect.CONTRACT_PAY);
        return bonus == 0 ? money : Math.max(1, Math.round(money * (100 + bonus) / 100.0F));
    }

    /** Trust earned with a contact, on top of whatever the lifepath adds. People like working with a name they have heard of. */
    public static int trustGain(Player player, int amount) {
        int bonus = trustBonus(player) + Skills.percent(player, SkillEffect.TRUST_GAIN);
        return amount <= 0 || bonus == 0 ? amount : Math.max(amount + 1, Math.round(amount * (100 + bonus) / 100.0F));
    }

    /** Multiplier on a price: a name gets a better deal. */
    public static float priceMultiplier(Player player) {
        return Math.max(0.5F, (100 - priceDiscount(player) - Skills.percent(player, SkillEffect.SHOP_DISCOUNT)) / 100.0F);
    }

    /** The highest contract tier a name opens up on its own. */
    public static int contractTier(Player player) {
        int level = StreetCred.level(player);
        return level >= TIER_3_LEVEL ? 3 : level >= TIER_2_LEVEL ? 2 : 1;
    }
}
