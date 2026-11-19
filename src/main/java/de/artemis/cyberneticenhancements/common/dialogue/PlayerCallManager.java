package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

import static de.artemis.cyberneticenhancements.common.network.PlayerCallRequestPayload.*;

public final class PlayerCallManager {
    private static final String DATA = "CyberneticPlayerCalls";
    private static final Map<UUID, Session> CALLS = new HashMap<>();
    private static final Map<UUID, Long> SETTINGS_AFTER = new HashMap<>();
    private static final Map<UUID, Boolean> ENABLED = new HashMap<>();

    private PlayerCallManager() {}
    public static boolean occupied(UUID player) { return CALLS.containsKey(player); }

    public static void handle(ServerPlayer player, PlayerCallRequestPayload request) {
        if (request.action() == START) { start(player, request); return; }
        Session call = CALLS.get(player.getUUID());
        if (call == null || !call.peer(player.getUUID()).equals(request.peerId())) return;
        boolean initialCancel = request.action() == END && call.caller.equals(player.getUUID()) && call.requestId.equals(request.callId());
        if (!call.id.equals(request.callId()) && !initialCancel) return;
        if (!call.connected && now(player.server) - call.started >= 600) {
            finish(player.server, call, "unanswered");
            return;
        }
        if (request.action() == END || request.action() == BLOCK) {
            boolean stored = request.action() != BLOCK || setFlag(player, request.peerId(), "blocked", true);
            finish(player.server, call, !stored ? "block_limit" : !call.connected && player.getUUID().equals(call.callee) ? "declined" : "ended");
            return;
        }
        if (request.action() == KEEPALIVE) {
            call.heartbeat.put(player.getUUID(), now(player.server));
            return;
        }
        if (request.action() == ANSWER && !call.connected && player.getUUID().equals(call.callee)) {
            var caller = player.server.getPlayerList().getPlayer(call.caller);
            if (caller == null || !canUse(caller) || !canUse(player) || blocked(caller, player.getUUID()) || blocked(player, caller.getUUID())) {
                finish(player.server, call, "unavailable");
                return;
            }
            if (!CallVoiceBridge.available(call.caller) || !CallVoiceBridge.available(call.callee)) {
                finish(player.server, call, "voice_unavailable");
                return;
            }
            call.connected = true;
            long now = now(player.server);
            call.heartbeat.put(call.caller, now);
            call.heartbeat.put(call.callee, now);
            publishRoutes();
            sendBoth(player.server, call, "connected");
        } else if (request.action() == MUTE && call.connected) {
            if (now(player.server) < call.muteAfter.getOrDefault(player.getUUID(), 0L)) return;
            call.muteAfter.put(player.getUUID(), now(player.server) + 5);
            if (!call.muted.add(player.getUUID())) call.muted.remove(player.getUUID());
            publishRoutes();
            sendBoth(player.server, call, "connected");
        }
    }

    private static void start(ServerPlayer player, PlayerCallRequestPayload request) {
        long now = now(player.server);
        expire(player.server, now);
        var existing = CALLS.get(player.getUUID());
        if (existing != null && existing.caller.equals(player.getUUID()) && existing.requestId.equals(request.callId())
                && existing.callee.equals(request.peerId())) {
            send(player.server, existing, player.getUUID(), existing.connected ? "connected" : "ringing");
            return;
        }
        ServerPlayer peer = player.server.getPlayerList().getPlayer(request.peerId());
        String error = peer == null || peer == player || !canUse(player) || !canUse(peer) || blocked(player, request.peerId()) || blocked(peer, player.getUUID()) ? "unavailable"
                : occupied(player.getUUID()) || occupied(peer.getUUID()) || NpcCallManager.hasCall(player.getUUID()) || NpcCallManager.hasCall(peer.getUUID()) ? "busy"
                : !CallVoiceBridge.available(player.getUUID()) || !CallVoiceBridge.available(peer.getUUID()) ? "voice_unavailable" : null;
        if (error != null) {
            PacketDistributor.sendToPlayer(player, new PlayerCallStatePayload(request.callId(), request.callId(), request.peerId(), peer == null ? "" : peer.getGameProfile().getName(), error, false, false));
            return;
        }
        Session call = new Session(UUID.randomUUID(), request.callId(), player.getUUID(), peer.getUUID(), now);
        CALLS.put(call.caller, call);
        CALLS.put(call.callee, call);
        sendBoth(player.server, call, "ringing");
    }

    public static void preferences(ServerPlayer player, boolean enabled) {
        long now = now(player.server);
        if (enabled && now < SETTINGS_AFTER.getOrDefault(player.getUUID(), 0L)) return;
        SETTINGS_AFTER.put(player.getUUID(), now + 10);
        ENABLED.put(player.getUUID(), enabled);
        if (!enabled && occupied(player.getUUID())) finish(player.server, CALLS.get(player.getUUID()), "ended");
    }

    private static boolean canUse(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && ENABLED.getOrDefault(player.getUUID(), false);
    }

    public static boolean blocked(ServerPlayer player, UUID peer) { return flags(player, peer).getBoolean("blocked"); }
    private static CompoundTag flags(ServerPlayer player, UUID peer) { return player.getPersistentData().getCompound(DATA).getCompound(peer.toString()); }

    private static boolean setFlag(ServerPlayer player, UUID peer, String flag, boolean value) {
        CompoundTag root = player.getPersistentData().getCompound(DATA);
        if (!root.contains(peer.toString()) && root.size() >= 256) return false;
        CompoundTag entry = root.getCompound(peer.toString());
        entry.putBoolean(flag, value);
        var other = player.server.getPlayerList().getPlayer(peer);
        if (other != null) entry.putString("name", other.getGameProfile().getName());
        if (entry.getBoolean("pinned") || entry.getBoolean("hidden") || entry.getBoolean("blocked")) root.put(peer.toString(), entry);
        else root.remove(peer.toString());
        player.getPersistentData().put(DATA, root);
        return true;
    }

    public static boolean contactAction(ServerPlayer player, UUID peer, int action) {
        if (peer.equals(player.getUUID()) || player.server.getPlayerList().getPlayer(peer) == null && !player.getPersistentData().getCompound(DATA).contains(peer.toString())) return false;
        String flag = switch (action) {
            case ArchiveContactActionPayload.ACTION_TOGGLE_PIN -> "pinned";
            case ArchiveContactActionPayload.ACTION_TOGGLE_HIDE -> "hidden";
            case ArchiveContactActionPayload.ACTION_TOGGLE_BLOCK -> "blocked";
            default -> null;
        };
        if (flag == null) return false;
        if (!setFlag(player, peer, flag, !flags(player, peer).getBoolean(flag))) return false;
        if (blocked(player, peer) && occupied(player.getUUID()) && CALLS.get(player.getUUID()).peer(player.getUUID()).equals(peer))
            finish(player.server, CALLS.get(player.getUUID()), "ended");
        PacketDistributor.sendToPlayer(player, ArchiveContactsPayload.capture(player));
        return true;
    }

    public static List<ArchiveContactsPayload.ContactEntry> contacts(ServerPlayer player) {
        Map<UUID, String> players = new LinkedHashMap<>();
        for (var other : player.server.getPlayerList().getPlayers()) if (other != player) players.put(other.getUUID(), other.getGameProfile().getName());
        var saved = player.getPersistentData().getCompound(DATA);
        for (String id : saved.getAllKeys()) {
            try { if (!saved.getCompound(id).getString("name").isBlank()) players.putIfAbsent(UUID.fromString(id), saved.getCompound(id).getString("name")); }
            catch (IllegalArgumentException ignored) {}
        }
        return players.entrySet().stream().sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER)).map(entry -> {
            UUID peer = entry.getKey();
            var other = player.server.getPlayerList().getPlayer(peer);
            String status = blocked(player, peer) ? "blocked" : other == null ? "offline" : !canUse(other) ? "unavailable"
                    : occupied(peer) || NpcCallManager.hasCall(peer) ? "busy" : !CallVoiceBridge.available(peer) ? "voice_unavailable" : "available";
            var flags = flags(player, peer);
            return new ArchiveContactsPayload.ContactEntry(peer.toString(), "player", entry.getValue(), "generic", 0,
                    flags.getBoolean("pinned"), flags.getBoolean("hidden"), false, false, false, "", false, 0, 0, 0, false, "", "aqua", status);
        }).toList();
    }

    private static void sendBoth(MinecraftServer server, Session call, String status) {
        send(server, call, call.caller, status);
        send(server, call, call.callee, status);
    }

    private static void send(MinecraftServer server, Session call, UUID recipient, String status) {
        ServerPlayer player = server.getPlayerList().getPlayer(recipient), peer = server.getPlayerList().getPlayer(call.peer(recipient));
        if (player != null) {
            String journalState = status + ":" + call.muted.contains(recipient);
            if (!journalState.equals(call.journalStates.put(recipient, journalState)))
                de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "calls", "player_call", peer == null ? call.peer(recipient).toString() : peer.getGameProfile().getName(),
                        net.minecraft.network.chat.Component.translatable("screen.cyberneticenhancements.call.player." + (status.equals("connected") && call.muted.contains(recipient) ? "microphone_muted" : status)));
            PacketDistributor.sendToPlayer(player, new PlayerCallStatePayload(call.id, call.requestId, call.peer(recipient),
                    peer == null ? "" : peer.getGameProfile().getName(), status, !call.connected && recipient.equals(call.callee) && status.equals("ringing"), call.muted.contains(recipient)));
        }
    }

    private static void finish(MinecraftServer server, Session call, String status) {
        CALLS.remove(call.caller, call);
        CALLS.remove(call.callee, call);
        publishRoutes();
        sendBoth(server, call, status);
    }

    private static void publishRoutes() {
        Map<UUID, CallVoiceBridge.Route> routes = new HashMap<>();
        CALLS.forEach((player, call) -> {
            if (call.connected) routes.put(player, new CallVoiceBridge.Route(call.id, call.peer(player), CallVoiceBridge.Route.channel(call.id, player), call.muted.contains(player)));
        });
        CallVoiceBridge.routes(routes);
    }

    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        expire(event.getServer(), now(event.getServer()));
    }

    static void expire(MinecraftServer server, long now) {
        for (Session call : Set.copyOf(CALLS.values())) {
            var caller = server.getPlayerList().getPlayer(call.caller);
            var callee = server.getPlayerList().getPlayer(call.callee);
            String end = caller == null || callee == null || !canUse(caller) || !canUse(callee) ? "unavailable"
                    : !CallVoiceBridge.available(call.caller) || !CallVoiceBridge.available(call.callee) ? "voice_lost"
                    : !call.connected && now - call.started >= 600 ? "unanswered"
                    : call.connected && call.heartbeat.values().stream().anyMatch(time -> now - time > 1800) ? "connection_lost" : null;
            if (end != null) finish(server, call, end);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (occupied(player.getUUID())) finish(player.server, CALLS.get(player.getUUID()), "disconnected");
        UUID id = player.getUUID();
        ENABLED.remove(id); SETTINGS_AFTER.remove(id);
    }

    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(DATA))
            event.getEntity().getPersistentData().put(DATA, event.getOriginal().getPersistentData().getCompound(DATA).copy());
    }

    public static void onStop(ServerStoppedEvent event) {
        CALLS.clear(); ENABLED.clear(); SETTINGS_AFTER.clear();
        CallVoiceBridge.routes(Map.of());
    }

    private static long now(MinecraftServer server) { return server.overworld().getGameTime(); }
    private static final class Session {
        final UUID id, requestId, caller, callee;
        final long started;
        final Map<UUID, Long> heartbeat = new HashMap<>(), muteAfter = new HashMap<>();
        final Map<UUID, String> journalStates = new HashMap<>();
        final Set<UUID> muted = new HashSet<>();
        boolean connected;
        Session(UUID id, UUID requestId, UUID caller, UUID callee, long started) { this.id = id; this.requestId = requestId; this.caller = caller; this.callee = callee; this.started = started; }
        UUID peer(UUID player) { return player.equals(caller) ? callee : caller; }
    }
}
