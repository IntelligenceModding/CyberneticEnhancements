package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload;
import de.artemis.cyberneticenhancements.common.network.NpcCallStatePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload.*;

public final class NpcCallManager {
    private static final String PREFIX = "screen.cyberneticenhancements.call.";
    private static final long IDLE_TICKS = 200;
    private static final long RING_TICKS = 600;
    private static final Map<UUID, Session> CALLS = new HashMap<>();
    private static final Map<ServerPlayer, JournalStamp> JOURNAL = new java.util.WeakHashMap<>();
    private record JournalStamp(UUID call, Component line, boolean ended) {}

    private NpcCallManager() {}

    public static void handle(ServerPlayer player, NpcCallRequestPayload request) {
        long now = player.server.overworld().getGameTime();
        expire(player.server, now);
        if (request.action() == START) {
            if (PlayerCallManager.occupied(player.getUUID())) {
                unavailable(player, request);
                return;
            }
            if (!player.isAlive()) {
                unavailable(player, request);
                return;
            }
            Session previous = CALLS.get(player.getUUID());
            if (previous != null && previous.callId.equals(request.callId()) && previous.npc.getUUID().equals(request.npcId())) {
                Session repeated = new Session(previous.callId, previous.npc, previous.expires,
                        previous.conversation, previous.answerAt, previous.refused, previous.incoming);
                CALLS.put(player.getUUID(), repeated);
                send(player, repeated, previous.answerAt == 0 ? previous.conversation.line() : Component.translatable(PREFIX + "ringing"), false);
                return;
            }
            remove(player.getUUID());
            AbstractCityNpcEntity npc = AbstractCityNpcEntity.resolveCallContact(player, request.npcId());
            if (npc == null) {
                unavailable(player, request);
                return;
            }
            Session session = new Session(request.callId(), npc, now + IDLE_TICKS,
                    new NpcCallConversation(npc, request.questId()), 0, false);
            if (CALLS.values().stream().anyMatch(call -> call.npc.getUUID().equals(npc.getUUID()) && !call.refused)) {
                send(player, session, Component.translatable(PREFIX + "line_busy"), true);
                return;
            }
            if (!npc.isRefusing(player) && !npc.canTakeCall(player)) {
                send(player, session, Component.translatable(PREFIX + "busy"), true);
                return;
            }
            boolean refuse = npc.isRefusing(player) || (npc.isAnnoyed(player) && npc.getRandom().nextInt(4) != 0);
            long delay = 30 + npc.getRandom().nextInt(npc.trustLevel(player) >= 2 ? 45 : 110);
            if (NpcCallDialogue.isNight(npc.level()) && npc.npcType() != de.artemis.cyberneticenhancements.common.entity.NpcType.NETRUNNER
                    && npc.npcType() != de.artemis.cyberneticenhancements.common.entity.NpcType.MERC
                    && npc.npcType() != de.artemis.cyberneticenhancements.common.entity.NpcType.MAXTEC) delay += 20 + npc.getRandom().nextInt(40);
            session = new Session(request.callId(), npc, now + RING_TICKS, session.conversation,
                    refuse && npc.getRandom().nextBoolean() ? -1 : now + delay, refuse);
            CALLS.put(player.getUUID(), session);
            send(player, session, Component.translatable(PREFIX + "ringing"), false);
            return;
        }
        Session session = CALLS.get(player.getUUID());
        if (session == null || !session.callId.equals(request.callId()) || !session.npc.getUUID().equals(request.npcId())) return;
        if (request.action() == END) {
            de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "calls", "npc_call_ended", session.npc.getDisplayName());
            remove(player.getUUID());
            return;
        }
        if (request.action() == KEEPALIVE) {
            return;
        }
        if (session.incoming && request.action() == ANSWER_INCOMING) {
            if (!session.npc.canTakeCall(player)) {
                remove(player.getUUID());
                send(player, session, Component.translatable(PREFIX + "lost"), true);
                return;
            }
            session.conversation.openIncoming(player);
            Session connected = new Session(session.callId, session.npc, now + IDLE_TICKS, session.conversation, 0, false);
            CALLS.put(player.getUUID(), connected);
            refreshPhone(session.npc);
            send(player, connected, connected.conversation.line(), false);
            return;
        }
        if (request.action() == TALK_IN_PERSON) {
            var npc = session.npc;
            double range = player.entityInteractionRange();
            var eye = player.getEyePosition();
            if (player.containerMenu != player.inventoryMenu || player.level() != npc.level() || !npc.canTakeCall(player)
                    || !player.hasLineOfSight(npc) || npc.getBoundingBox().inflate(0.1).clip(eye, eye.add(player.getLookAngle().scale(range))).isEmpty()) return;
            npc.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            return;
        }
        if (session.answerAt != 0) return;
        if (session.conversation.choices().stream().noneMatch(choice -> choice.action() == request.action())) return;
        AbstractCityNpcEntity npc = session.npc;
        if (!npc.canTakeCall(player)) {
            remove(player.getUUID());
            send(player, session, Component.translatable(PREFIX + "lost"), true);
            return;
        }
        session.conversation.respond(player, request.action());
        Session next = new Session(session.callId, npc, now + IDLE_TICKS, session.conversation, 0, false);
        if (session.conversation.ended()) remove(player.getUUID());
        else CALLS.put(player.getUUID(), next);
        send(player, next, session.conversation.line(), session.conversation.ended());
    }

    private static void unavailable(ServerPlayer player, NpcCallRequestPayload request) {
        PacketDistributor.sendToPlayer(player, new NpcCallStatePayload(request.callId(), request.npcId(), "", "", "generic", "", "aqua",
                Component.translatable(PREFIX + (de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData.get(player.serverLevel()).isDeceased(request.npcId())
                        ? "contact_died" : "unavailable")), List.of(), true));
    }

    private static void send(ServerPlayer player, Session session, Component line, boolean ended) {
        var npc = session.npc;
        var stamp = new JournalStamp(session.callId, line.copy(), ended);
        if (!stamp.equals(JOURNAL.put(player, stamp))) de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "calls", "npc_call", npc.getDisplayName(), line);
        if (!ended && !session.incoming && session.answerAt == 0) npc.noteConversationReply();
        PacketDistributor.sendToPlayer(player, new NpcCallStatePayload(session.callId, npc.getUUID(), npc.npcTypeId(), npc.getName().getString(),
                npc.npcCategory().id(), npc.appearanceOverride(), npc.nameColorId(player), line, ended ? List.of()
                : session.incoming ? List.of(new NpcCallStatePayload.Choice(ANSWER_INCOMING, Component.translatable(PREFIX + "choice.answer")),
                        new NpcCallStatePayload.Choice(END, Component.translatable(PREFIX + "choice.decline")))
                : session.answerAt != 0 ? List.of() : session.conversation.choices(), ended, session.incoming && !ended));
    }

    static void expire(MinecraftServer server, long now) {
        var iterator = CALLS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Session session = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive() || session.npc.isRemoved() || !session.npc.isAlive() || session.expires <= now) {
                boolean idle = player != null && player.isAlive() && session.npc.isAlive() && !session.npc.isRemoved()
                        && session.answerAt == 0 && !session.incoming && session.expires <= now;
                iterator.remove();
                if (player != null) {
                    if (idle) session.conversation.idleClosed(player);
                    else session.conversation.closed(player);
                }
                refreshPhone(session.npc);
                if (player != null) send(player, session, Component.translatable(PREFIX + (!session.npc.isAlive() ? "contact_died"
                        : idle ? "idle_hangup" : session.incoming ? "incoming.missed" : session.answerAt != 0 && session.expires <= now ? "unanswered" : "lost")), true);
            }
        }
    }

    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 5 == 0) answerPending(event.getServer(), event.getServer().overworld().getGameTime());
        if (event.getServer().getTickCount() % 5 == 0) expire(event.getServer(), event.getServer().overworld().getGameTime());
        if (event.getServer().getTickCount() % 1200 == 0) {
            for (var player : event.getServer().getPlayerList().getPlayers()) NpcIncomingCalls.evaluate(player);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        remove(event.getEntity().getUUID());
    }

    public static void interruptForNpc(AbstractCityNpcEntity npc, boolean died) {
        if (npc.level().getServer() == null) return;
        for (var entry : List.copyOf(CALLS.entrySet())) {
            Session session = entry.getValue();
            if (session.npc != npc) continue;
            remove(entry.getKey());
            var caller = npc.level().getServer().getPlayerList().getPlayer(entry.getKey());
            if (caller != null) send(caller, session, Component.translatable(PREFIX + (died ? "contact_died" : "contact_hung_up")), true);
        }
    }

    public static void endForInteraction(ServerPlayer player, AbstractCityNpcEntity npc) {
        Session session = CALLS.get(player.getUUID());
        if (session == null || !session.npc.getUUID().equals(npc.getUUID())) return;
        remove(player.getUUID());
        send(player, session, Component.translatable(PREFIX + "ended"), true);
    }

    public static void onStop(ServerStoppedEvent event) {
        CALLS.values().forEach(session -> session.npc.setOnCall(false));
        CALLS.clear();
        JOURNAL.clear();
    }

    public static boolean hasCall(UUID player) { return CALLS.containsKey(player); }

    public static boolean startIncoming(ServerPlayer player, AbstractCityNpcEntity npc, NpcCallReason reason, boolean forced) {
        long now = player.server.overworld().getGameTime();
        expire(player.server, now);
        if (!player.isAlive() || player.isSpectator() || hasCall(player.getUUID()) || PlayerCallManager.occupied(player.getUUID()) || !npc.canTakeCall(player)
                || player.containerMenu != player.inventoryMenu
                || CALLS.values().stream().anyMatch(session -> session.npc == npc && !session.refused)) return false;
        if (!forced && (!npc.hasMet(player) || npc.isAnnoyed(player)
                || (player.level() == npc.level() && npc.distanceToSqr(player) < 64))) return false;
        if (reason == NpcCallReason.OVERDUE && (!npc.npcType().supportsContracts()
                || de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager.snapshotForIssuer(player, npc).active() == null)) return false;
        npc.ensureIdentity();
        if (forced) npc.markMet(player);
        var conversation = new NpcCallConversation(npc);
        if (!conversation.prepareIncoming(player, reason)) return false;
        var session = new Session(UUID.randomUUID(), npc, now + RING_TICKS, conversation, -2, false, true);
        CALLS.put(player.getUUID(), session);
        refreshPhone(npc);
        send(player, session, Component.translatable(PREFIX + "incoming.ringing"), false);
        return true;
    }

    public static boolean isOccupiedByOther(ServerPlayer player, AbstractCityNpcEntity npc) {
        return CALLS.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(player.getUUID())
                && entry.getValue().npc == npc && !entry.getValue().refused);
    }

    static void answerPending(MinecraftServer server, long now) {
        for (var entry : List.copyOf(CALLS.entrySet())) {
            Session session = entry.getValue();
            if (session.answerAt <= 0 || session.answerAt > now || session.expires <= now) continue;
            var player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive() || !session.npc.isAlive() || session.npc.isRemoved()) {
                remove(entry.getKey());
                continue;
            }
            if (session.refused || !session.npc.canTakeCall(player)) {
                remove(entry.getKey());
                send(player, session, Component.translatable(PREFIX + (session.refused ? "refused" : "busy")), true);
                continue;
            }
            Session connected = new Session(session.callId, session.npc, now + IDLE_TICKS, session.conversation, 0, false);
            connected.conversation.open(player);
            if (connected.conversation.ended()) remove(entry.getKey());
            else {
                CALLS.put(entry.getKey(), connected);
                refreshPhone(session.npc);
            }
            send(player, connected, connected.conversation.line(), connected.conversation.ended());
        }
    }

    private static void remove(UUID player) {
        Session removed = CALLS.remove(player);
        if (removed != null) {
            var owner = removed.npc.level().getServer().getPlayerList().getPlayer(player);
            if (owner != null) removed.conversation.closed(owner);
            refreshPhone(removed.npc);
        }
    }

    private static void refreshPhone(AbstractCityNpcEntity npc) {
        npc.setOnCall(CALLS.values().stream().anyMatch(session -> session.npc == npc && (session.answerAt == 0 || session.incoming)));
    }

    private record Session(UUID callId, AbstractCityNpcEntity npc, long expires, NpcCallConversation conversation, long answerAt, boolean refused, boolean incoming) {
        private Session(UUID callId, AbstractCityNpcEntity npc, long expires, NpcCallConversation conversation, long answerAt, boolean refused) {
            this(callId, npc, expires, conversation, answerAt, refused, false);
        }
    }
}
