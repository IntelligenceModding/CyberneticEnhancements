package de.artemis.cyberneticenhancements.common.skills;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

/**
 * Where skills meet the systems they improve. Each method is one added term for the system that calls it, so that system
 * never has to know how a rank turns into a number. Every result is bounded so that no bonus can make something free.
 */
public final class SkillEffects {
    /** The least a price, cost or loss can be scaled down to. */
    private static final float PRICE_FLOOR = 0.5F;

    private SkillEffects() {}

    /** Max health and fall damage, which the cyberware effects already apply as attribute modifiers. */
    public static void mergeSurvival(Player player, EnumMap<CyberwareEffectType, Double> totals) {
        double health = Skills.bonus(player, SkillEffect.MAX_HEALTH);
        if (health > 0.0D) totals.merge(CyberwareEffectType.MAX_HEALTH, health, Double::sum);
        double fall = Skills.bonus(player, SkillEffect.FALL_REDUCTION);
        if (fall > 0.0D) totals.merge(CyberwareEffectType.FALL_DAMAGE_REDUCTION, -fall / 100.0D, Double::sum);
    }

    /** Evasion: people who shoot at you hit a little less hard. Falls, fire and the like are not theirs to soften. */
    public static float damageTakenMultiplier(Player player, Entity attacker) {
        if (!(attacker instanceof AbstractCityNpcEntity) && !(attacker instanceof CyberpsychoEntity)) return 1.0F;
        return 1.0F - (float) Skills.bonus(player, SkillEffect.DAMAGE_REDUCTION) / 100.0F;
    }

    /** Street Medicine: what a healing consumable heals. */
    public static float healing(Player player, float amount) {
        return amount * Skills.moreBy(player, SkillEffect.HEALING_BOOST);
    }

    /** Maintenance and Ripperdoc Rapport: a vendor's price on top of the Street Cred and lifepath terms. */
    public static float vendorMultiplier(Player player, NpcType type, boolean service) {
        boolean ripperdoc = type == NpcType.RIPPERDOC || type == NpcType.STREET_RIPPERDOC;
        boolean cyberwareService = service && (ripperdoc || type == NpcType.FIXER);
        int discount = (cyberwareService ? Skills.percent(player, SkillEffect.SERVICE_DISCOUNT) : 0)
                + (ripperdoc ? Skills.percent(player, SkillEffect.RIPPERDOC_DISCOUNT) : 0);
        return Math.max(PRICE_FLOOR, 1.0F - discount / 100.0F);
    }

    /** Deception: what is left of a trust loss. */
    public static int trustLoss(Player player, int loss) {
        return loss <= 0 ? loss : Math.max(1, Math.round(loss * Skills.lessBy(player, SkillEffect.TRUST_LOSS_CUT, PRICE_FLOOR)));
    }

    /** Streetwise: how much trust a day of trading can earn, given the usual cap. */
    public static int commerceCap(Player player, int cap) {
        return Math.round(cap * Skills.moreBy(player, SkillEffect.COMMERCE_TRUST));
    }

    /** Intimidation: dangerous work pays a little more. */
    public static int combatPay(Player player, boolean dangerous, int money) {
        return dangerous ? Math.round(money * Skills.moreBy(player, SkillEffect.COMBAT_PAY)) : money;
    }

    /** Neural Resistance, and for strain-driven build-up Chrome Tolerance as well: how much of an instability gain is left. */
    public static double instability(Player player, double amount, boolean fromStrain) {
        int slower = Skills.percent(player, SkillEffect.PSYCHOSIS_SLOWER) + (fromStrain ? Skills.percent(player, SkillEffect.STRAIN_SLOWER) : 0);
        return amount * Math.max(0.5D, 1.0D - slower / 100.0D);
    }

    /** Maintenance: whether this wear tick is skipped, so that on average wear comes this much slower. */
    public static boolean skipsWear(Player player) {
        int chance = Skills.percent(player, SkillEffect.WEAR_SLOWER);
        return chance > 0 && player.getRandom().nextInt(100) < chance;
    }

    /** Chrome Tolerance: capacity on top of the implants and the tarot headroom. */
    public static int capacity(Player player, int capacity) {
        return capacity + Math.round(capacity * Skills.percent(player, SkillEffect.CHROME_CAPACITY) / 100.0F);
    }

    /** The online player who started a machine, or null when they are away. A machine only works at skill speed for its owner. */
    private static ServerPlayer owner(Level level, UUID owner) {
        return owner == null || level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(owner);
    }

    /** Fabrication: 1 extra step of progress on this share of ticks. */
    public static int extraProgress(Level level, UUID owner) {
        var player = owner(level, owner);
        if (player == null) return 0;
        int bonus = Skills.percent(player, SkillEffect.CRAFT_SPEED);
        return (level.getGameTime() % 100L) * bonus % 100L + bonus >= 100 ? 1 : 0;
    }

    /** Fabrication: whether a finished item gives some of its parts back. */
    public static boolean refunds(Level level, UUID owner) {
        var player = owner(level, owner);
        int chance = player == null ? 0 : Skills.percent(player, SkillEffect.MATERIAL_REFUND);
        return chance > 0 && level.random.nextInt(100) < chance;
    }

    /** Engineering: recycled parts come out up to the bonus bigger, rounded at random so every part gets its share. */
    public static List<ItemStack> salvage(Level level, UUID owner, List<ItemStack> outputs) {
        var player = owner(level, owner);
        int bonus = player == null ? 0 : Skills.percent(player, SkillEffect.SALVAGE_YIELD);
        if (bonus <= 0) return outputs;
        var boosted = new ArrayList<ItemStack>(outputs.size());
        for (ItemStack stack : outputs) {
            double exact = stack.getCount() * bonus / 100.0D;
            int extra = (int) exact + (level.random.nextDouble() < exact - (int) exact ? 1 : 0);
            ItemStack copy = stack.copy();
            copy.grow(Math.min(extra, Math.max(0, copy.getMaxStackSize() - copy.getCount())));
            boosted.add(copy);
        }
        return boosted;
    }

    /** How good a roll of loot is, by the rarity of what is in it. */
    private static int lootValue(List<ItemStack> loot) {
        int value = 0;
        for (ItemStack stack : loot) value += (stack.getRarity() == Rarity.COMMON ? 0 : stack.getRarity().ordinal() * 3) + 1;
        return value;
    }

    /** Scavenging: a chance to look once more at the cache's loot and keep the better of the two. The number of attempts never changes. */
    public static List<ItemStack> betterLoot(Player player, List<ItemStack> first, java.util.function.Supplier<List<ItemStack>> reroll, RandomSource random) {
        int chance = Skills.percent(player, SkillEffect.CACHE_QUALITY);
        if (chance == 0 || random.nextInt(100) >= chance) return first;
        var second = reroll.get();
        return !second.isEmpty() && lootValue(second) > lootValue(first) ? second : first;
    }
}
