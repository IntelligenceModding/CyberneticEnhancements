package de.artemis.cyberneticenhancements.common.skills;

import de.artemis.cyberneticenhancements.common.config.SkillsConfig;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.feedback.GameplayMessages;
import de.artemis.cyberneticenhancements.common.origin.Origins;
import de.artemis.cyberneticenhancements.common.streetcred.StreetCred;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Purchased ranks are stored and synced by the server. Lifepath ranks come from the saved origin,
 * and the spendable budget comes from Street Cred minus the purchased ranks.
 */
public final class Skills {
    public static final String KEY = "cyberneticenhancements.skills";
    /** Ranks a skill's top bonus is spread over, whatever the configured cap is. */
    public static final int RANK_SCALE = 10;
    private static final int[] client = new int[Skill.values().length];

    private Skills() {}

    /** Points a level adds up to. Level 1 already gives one. */
    public static int pointsFor(int level) {
        return Math.clamp(level, 1, StreetCred.MAX_LEVEL);
    }

    /** The ranks as stored, never more than the highest rank the code allows. */
    public static int storedRank(Player player, Skill skill) {
        int rank = player.level().isClientSide ? client[skill.ordinal()] : player.getPersistentData().getCompound(KEY).getInt(skill.key());
        return Math.clamp(rank, 0, RANK_SCALE);
    }

    public static int originRank(Player player, Skill skill) {
        var origin = Origins.of(player);
        return origin == null ? 0 : Math.min(origin.startingRank(skill), SkillsConfig.maxRank());
    }

    /** The combined rank, held to the configured cap. */
    public static int rank(Player player, Skill skill) {
        return Math.min(originRank(player, skill) + storedRank(player, skill), SkillsConfig.maxRank());
    }

    public static int spent(Player player) {
        int total = 0;
        for (Skill skill : Skill.values()) total += storedRank(player, skill);
        return total;
    }

    public static int available(Player player) {
        return Math.max(0, pointsFor(StreetCred.level(player)) - spent(player));
    }

    public static boolean canSpend(Player player, Skill skill) {
        return SkillsConfig.enabled() && skill != null && available(player) > 0 && rank(player, skill) < SkillsConfig.maxRank();
    }

    public static int respecCost(Player player) {
        return spent(player) * SkillsConfig.respecBaseCost();
    }

    public static boolean canRespec(Player player) {
        return SkillsConfig.enabled() && SkillsConfig.respecEnabled() && spent(player) > 0;
    }

    /** The total a skill-given number gets from every skill that improves it, in the unit of the effect. */
    public static double bonus(Player player, SkillEffect effect) {
        if (!SkillsConfig.enabled()) return 0.0D;
        double total = 0.0D;
        for (Skill skill : Skill.values()) if (skill.improves(effect)) total += effect.at(rank(player, skill));
        return total;
    }

    /** {@link #bonus} in whole units, rounded down. */
    public static int percent(Player player, SkillEffect effect) {
        return (int) Math.floor(bonus(player, effect) + 1.0E-9D);
    }

    /** A multiplier for something a skill makes smaller: 1 with no bonus, never below the floor. */
    public static float lessBy(Player player, SkillEffect effect, float floor) {
        return Math.max(floor, 1.0F - percent(player, effect) / 100.0F);
    }

    /** A multiplier for something a skill makes bigger. */
    public static float moreBy(Player player, SkillEffect effect) {
        return 1.0F + percent(player, effect) / 100.0F;
    }

    /** True if the player has the rank a dialogue option asks for. With skills switched off every check passes. */
    public static boolean meets(Player player, Skill skill, int rank) {
        return !SkillsConfig.enabled() || rank(player, skill) >= rank;
    }

    /** Buys one rank. Returns false, and changes nothing, if the player has no point to spend or the skill is at its cap. */
    public static boolean spend(ServerPlayer player, Skill skill) {
        if (!canSpend(player, skill)) return false;
        CompoundTag ranks = player.getPersistentData().getCompound(KEY).copy();
        ranks.putInt(skill.key(), storedRank(player, skill) + 1);
        player.getPersistentData().put(KEY, ranks);
        sync(player);
        return true;
    }

    /** Takes every rank back for a price that grows with how many are spent. Returns false if respec is off or the money is missing. */
    public static boolean respec(ServerPlayer player, int quotedCost) {
        if (!canRespec(player)) return false;
        int cost = respecCost(player);
        if (cost != quotedCost) {
            GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.skills.respec_price_changed"));
            return false;
        }
        if (cost > 0 && !PlayerEurodollarManager.tryWithdraw(player, cost, "expense", "Skill respec", null, null)) {
            GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.skills.respec_funds", cost));
            return false;
        }
        player.getPersistentData().remove(KEY);
        sync(player);
        GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.skills.respec_done"));
        return true;
    }

    public static int[] ranksOf(Player player) {
        int[] ranks = new int[Skill.values().length];
        for (Skill skill : Skill.values()) ranks[skill.ordinal()] = storedRank(player, skill);
        return ranks;
    }

    public static void receive(int[] ranks) {
        for (int i = 0; i < client.length; i++) client[i] = i < ranks.length ? Math.clamp(ranks[i], 0, RANK_SCALE) : 0;
    }

    public static void clearClient() { java.util.Arrays.fill(client, 0); }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SkillsPayload(ranksOf(player)));
    }

    public static void onSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) sync(event.getPlayer()); else event.getPlayerList().getPlayers().forEach(Skills::sync);
    }

    public static void onClone(PlayerEvent.Clone event) {
        var saved = event.getOriginal().getPersistentData().get(KEY);
        if (saved != null) event.getEntity().getPersistentData().put(KEY, saved.copy());
    }
}
