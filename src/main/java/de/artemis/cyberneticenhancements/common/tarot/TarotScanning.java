package de.artemis.cyberneticenhancements.common.tarot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class TarotScanning {
    public static final int SCAN_TICKS = 60;
    private record Scan(UUID card, long started, long heartbeat) {}
    private static final Map<UUID, Scan> ACTIVE = new HashMap<>();
    public static TarotCardEntity target(Player player) {
        Vec3 from = player.getEyePosition(), to = from.add(player.getViewVector(1).scale(5));
        Vec3 end = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        TarotCardEntity nearest = null; double distance = from.distanceToSqr(end) + .002;
        for (TarotCardEntity card : player.level().getEntitiesOfClass(TarotCardEntity.class, new AABB(from, to).inflate(.1))) {
            if (card.isDecoration() || card.isCollecting()) continue;
            Optional<Vec3> hit = card.getBoundingBox().clip(from, to);
            if (hit.isPresent() && from.distanceToSqr(hit.get()) < distance) { nearest = card; distance = from.distanceToSqr(hit.get()); }
        }
        return nearest;
    }
    private static boolean eligible(ServerPlayer player, TarotCardEntity card) {
        return player.isAlive() && !player.isSpectator() && player.containerMenu == player.inventoryMenu
                && card != null && !card.isDecoration() && !card.isCollecting() && !card.isRemoved() && !TarotKnowledge.known(player).contains(card.cardId());
    }
    public static void receive(ServerPlayer player, TarotScanPayload payload) {
        TarotCardEntity card = target(player);
        if (!payload.held() || !eligible(player, card) || card.getId() != payload.entityId()) { ACTIVE.remove(player.getUUID()); return; }
        long now = player.level().getGameTime(); Scan scan = ACTIVE.get(player.getUUID());
        ACTIVE.put(player.getUUID(), new Scan(card.getUUID(), scan != null && scan.card.equals(card.getUUID()) && now - scan.heartbeat <= 12 ? scan.started : now, now));
    }
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Scan scan = ACTIVE.get(player.getUUID()); if (scan == null) return;
        TarotCardEntity card = target(player); long now = player.level().getGameTime();
        if (!eligible(player, card) || !scan.card.equals(card.getUUID()) || now - scan.heartbeat > 12) { ACTIVE.remove(player.getUUID()); return; }
        if (now - scan.started < SCAN_TICKS) return;
        ACTIVE.remove(player.getUUID());
        if (TarotKnowledge.learn(player, card.cardId())) {
            de.artemis.cyberneticenhancements.common.journal.PlayerJournal.recordAt(player, card.blockPosition(), "lore", "tarot", TarotCatalog.find(card.cardId()).name());
            player.displayClientMessage(Component.translatable("tarot.cyberneticenhancements.archived", TarotCatalog.find(card.cardId()).name()), true);
            player.playNotifySound(net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.PLAYERS, .25F, 1.3F);
            de.artemis.cyberneticenhancements.common.streetcred.StreetCred.gain(player, de.artemis.cyberneticenhancements.common.streetcred.StreetCredRewards.TAROT_CARD);
            card.collect();
        }
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { ACTIVE.remove(event.getEntity().getUUID()); }
}
