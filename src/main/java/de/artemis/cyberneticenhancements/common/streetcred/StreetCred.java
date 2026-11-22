package de.artemis.cyberneticenhancements.common.streetcred;

import de.artemis.cyberneticenhancements.common.feedback.GameplayMessages;
import de.artemis.cyberneticenhancements.common.origin.Origin;
import de.artemis.cyberneticenhancements.common.origin.Origins;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * How well the city knows a player. It only ever grows: points are earned by what the player does, and the level they add
 * up to is what other systems compare against their own thresholds.
 */
public final class StreetCred {
    public static final String KEY = "cyberneticenhancements.street_cred";
    public static final int MAX_LEVEL = 100;
    /** Percentage. */
    public static final int STREET_KID_GAIN = 50;
    private static int client;

    private StreetCred() {}

    /** Points needed to go from this level to the next. */
    public static int cost(int level) {
        return 60 + 12 * level;
    }

    /** Total points at which a level is reached. Level 1 is where everyone starts. */
    public static int threshold(int level) {
        int clamped = Math.clamp(level, 1, MAX_LEVEL);
        return 60 * (clamped - 1) + 6 * (clamped - 1) * clamped;
    }

    public static int levelFor(int points) {
        int level = 1;
        while (level < MAX_LEVEL && points >= threshold(level + 1)) level++;
        return level;
    }

    /** The name a level goes by. It changes every ten levels, and the top level is the only one called a legend. */
    public static Component rank(int level) {
        return Component.translatable("streetcred.cyberneticenhancements.rank." + Math.clamp(level, 1, MAX_LEVEL) / 10);
    }

    public static int points(Player player) {
        return player.level().isClientSide ? client : player.getPersistentData().getInt(KEY);
    }

    public static int level(Player player) {
        return levelFor(points(player));
    }

    /** Points still missing for the next level, or 0 at the top. */
    public static int toNext(Player player) {
        int level = level(player);
        return level >= MAX_LEVEL ? 0 : threshold(level + 1) - points(player);
    }

    /** True if the player's name is big enough for something that asks for this level. */
    public static boolean meets(Player player, int level) {
        return level(player) >= level;
    }

    public static void receive(int points) { client = Math.max(0, points); }
    public static void clearClient() { client = 0; }

    /** Points earned for something done. The street remembers its own, so a street kid builds a name faster. */
    public static int scaled(Player player, int amount) {
        if (amount <= 0 || Origins.of(player) != Origin.STREET_KID) return amount;
        return Math.max(amount + 1, Math.round(amount * (100 + STREET_KID_GAIN) / 100.0F));
    }

    /** Awards points and tells the player about any level reached. Returns the points actually added. */
    public static int gain(ServerPlayer player, int amount) {
        int added = scaled(player, amount);
        if (added <= 0) return 0;
        int before = level(player);
        store(player, points(player) + added);
        int after = level(player);
        if (after > before) GameplayMessages.chat(player, Component.translatable("streetcred.cyberneticenhancements.level_up", after));
        return added;
    }

    /** Sets the total directly. Only commands and tests use this; play only ever adds. */
    public static void set(ServerPlayer player, int points) {
        store(player, points);
    }

    private static void store(ServerPlayer player, int points) {
        int capped = Math.clamp(points, 0, threshold(MAX_LEVEL));
        if (capped == 0) player.getPersistentData().remove(KEY); else player.getPersistentData().putInt(KEY, capped);
        StreetCredBoard.get(player.server).record(player.getUUID(), player.getGameProfile().getName(), capped,
                player.getGameProfile().getProperties().get("textures").stream().findFirst().map(property -> property.value()).orElse(""));
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new StreetCredPayload(points(player)));
    }

    public static void onSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) sync(event.getPlayer()); else event.getPlayerList().getPlayers().forEach(StreetCred::sync);
    }

    public static void onClone(PlayerEvent.Clone event) {
        var saved = event.getOriginal().getPersistentData().get(KEY);
        if (saved != null) event.getEntity().getPersistentData().put(KEY, saved.copy());
    }
}
