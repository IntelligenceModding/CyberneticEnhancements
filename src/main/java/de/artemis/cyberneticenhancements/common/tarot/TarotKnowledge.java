package de.artemis.cyberneticenhancements.common.tarot;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class TarotKnowledge {
    private static final String KEY = "cyberneticenhancements.tarot_cards";
    private static Set<String> client = Set.of();
    public static Set<String> known(Player player) {
        Set<String> ids = new HashSet<>();
        for (Tag tag : player.getPersistentData().getList(KEY, Tag.TAG_STRING))
            if (TarotCatalog.find(tag.getAsString()) != null) ids.add(tag.getAsString());
        return Set.copyOf(ids);
    }
    public static boolean clientKnows(String id) { return client.contains(id); }
    public static void receive(List<String> ids) { client = ids.stream().filter(id -> TarotCatalog.find(id) != null).collect(java.util.stream.Collectors.toUnmodifiableSet()); }
    public static void clearClient() { client = Set.of(); }
    public static boolean learn(ServerPlayer player, String id) {
        return change(player, List.of(id), true) > 0;
    }
    /** Apply a batch once so commands do not send one update per card. */
    public static int change(ServerPlayer player, Collection<String> selected, boolean give) {
        Set<String> ids = new HashSet<>(known(player));
        int changed = 0;
        for (String id : selected) {
            if (TarotCatalog.find(id) == null) continue;
            if (give ? ids.add(id) : ids.remove(id)) changed++;
        }
        if (changed == 0) return 0;
        ListTag tags = new ListTag(); ids.stream().sorted().forEach(value -> tags.add(StringTag.valueOf(value)));
        float previousMax = player.getMaxHealth();
        player.getPersistentData().put(KEY, tags); sync(player);
        float addedHealth = player.isAlive() ? Math.max(0, player.getMaxHealth() - previousMax) : 0;
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + addedHealth));
        return changed;
    }
    public static void sync(ServerPlayer player) {
        TarotRewards.apply(player);
        PacketDistributor.sendToPlayer(player, new TarotKnowledgePayload(known(player).stream().sorted().toList()));
    }
    public static void onSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) sync(event.getPlayer()); else event.getPlayerList().getPlayers().forEach(TarotKnowledge::sync);
    }
    public static void onClone(PlayerEvent.Clone event) {
        Tag saved = event.getOriginal().getPersistentData().get(KEY);
        if (saved != null) event.getEntity().getPersistentData().put(KEY, saved.copy());
        TarotRewards.apply(event.getEntity());
    }
}
