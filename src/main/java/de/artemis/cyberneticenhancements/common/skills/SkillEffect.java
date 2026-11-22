package de.artemis.cyberneticenhancements.common.skills;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * A number a skill can improve, and how much it is improved at the top rank. Percent values are whole percent; the others
 * are in the unit of the number they change. A skill gives {@code rank / 10} of this, so every rank is felt a little.
 */
public enum SkillEffect {
    SHOP_DISCOUNT(10), CONTRACT_PAY(10), TRUST_GAIN(25),
    COMBAT_PAY(5), COMMERCE_TRUST(25), TRUST_LOSS_CUT(50),
    CHROME_CAPACITY(10), STRAIN_SLOWER(20), WEAR_SLOWER(25), SERVICE_DISCOUNT(15), PSYCHOSIS_SLOWER(20), RIPPERDOC_DISCOUNT(10),
    CRAFT_SPEED(25), MATERIAL_REFUND(10), CACHE_QUALITY(30), SALVAGE_YIELD(20),
    MAX_HEALTH(4), FALL_REDUCTION(30), HEALING_BOOST(20), DAMAGE_REDUCTION(8);

    private final int atTopRank;

    SkillEffect(int atTopRank) {
        this.atTopRank = atTopRank;
    }

    /** The bonus at a rank, unrounded. */
    public double at(int rank) {
        return atTopRank * (double) Math.clamp(rank, 0, Skills.RANK_SCALE) / Skills.RANK_SCALE;
    }

    public Component describe(double amount) {
        return Component.translatable("skill.cyberneticenhancements.effect." + name().toLowerCase(Locale.ROOT), format(amount));
    }

    private Component format(double amount) {
        String value = switch (this) {
            case MAX_HEALTH -> String.format(Locale.ROOT, "%.1f", amount / 2.0D);
            case DAMAGE_REDUCTION -> String.format(Locale.ROOT, "%.1f", amount);
            default -> Integer.toString((int) Math.floor(amount + 1.0E-9D));
        };
        return Component.translatable(this == MAX_HEALTH
                ? "screen.cyberneticenhancements.skills.hearts" : "screen.cyberneticenhancements.skills.percent", value);
    }

    public double activeBonus(Player player) {
        return switch (this) {
            case MAX_HEALTH, DAMAGE_REDUCTION -> Skills.bonus(player, this);
            default -> Skills.percent(player, this);
        };
    }
}
