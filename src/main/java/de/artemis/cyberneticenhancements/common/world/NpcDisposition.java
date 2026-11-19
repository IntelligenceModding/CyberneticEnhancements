package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.network.NpcDispositionPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class NpcDisposition {
    private static final String TAG = "cyberneticenhancements.npc_disposition";
    public static final long ANGER_TICKS = 5 * 60 * 20L;
    private static final Map<ServerPlayer, NpcDispositionPayload> SENT = new WeakHashMap<>();
    private NpcDisposition() {}

    public static void anger(ServerPlayer player, UUID npc) {
        var tag = player.getPersistentData().getCompound(TAG);
        tag.putLong(npc.toString(), player.server.overworld().getGameTime() + ANGER_TICKS);
        player.getPersistentData().put(TAG, tag);
        sync(player);
    }

    public static boolean angry(ServerPlayer player, UUID npc) {
        return player.getPersistentData().getCompound(TAG).getLong(npc.toString()) > player.server.overworld().getGameTime();
    }

    public static String color(ServerPlayer player, UUID npc) {
        if (player.serverLevel().getEntity(npc) instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity contact)
            return contact.nameColorId(player);
        var identity = FixerIdentitySavedData.get(player.serverLevel()).identity(npc);
        if (identity != null && identity.nameColorId().equals("light_purple")) return "light_purple";
        return angry(player, npc) ? "yellow" : "aqua";
    }

    public static NpcDispositionPayload capture(ServerPlayer player) {
        var tag = player.getPersistentData().getCompound(TAG);
        var ids = new ArrayList<UUID>();
        long now = player.server.overworld().getGameTime();
        for (String key : new java.util.HashSet<>(tag.getAllKeys())) {
            if (tag.getLong(key) <= now) { tag.remove(key); continue; }
            try { ids.add(UUID.fromString(key)); } catch (IllegalArgumentException ignored) { tag.remove(key); }
        }
        ids.sort(UUID::compareTo);
        player.getPersistentData().put(TAG, tag);
        return new NpcDispositionPayload(ids);
    }

    public static void sync(ServerPlayer player) {
        var value = capture(player);
        if (!value.equals(SENT.put(player, value))) {
            PacketDistributor.sendToPlayer(player, value);
            PacketDistributor.sendToPlayer(player, de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.capture(player));
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) sync(player);
    }

    public static void reset(ServerPlayer player) { SENT.remove(player); }
    public static void copyState(Player original, Player clone) {
        if (original.getPersistentData().contains(TAG)) clone.getPersistentData().put(TAG, original.getPersistentData().getCompound(TAG).copy());
    }
}
