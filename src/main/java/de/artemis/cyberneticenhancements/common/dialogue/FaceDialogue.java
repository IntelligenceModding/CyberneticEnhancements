package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.network.FaceDialogueStatePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Talking to someone without a screen: looking at them is enough to hear them, and the answers sit on the HUD beside the
 * crosshair, so the game never stops. A node is one thing they say and the ways to answer it. An answer that is not yet
 * available is still shown, with the reason, so a player can see what a higher Street Cred would have opened.
 */
public final class FaceDialogue {
    private static final double LOOK_RANGE = 5, KEEP_RANGE = 7, STICKY_RANGE = 24, STICKY_LOOK = 20;
    private static final int AWAY_TICKS = 4, REPLY_TICKS = 100, COOLDOWN_TICKS = 100, AWAY_COOLDOWN = 2;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Conversations set aside while one of their windows is open; closing the window picks them up again where they were. */
    private static final Map<UUID, Session> PAUSED = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    /** {@code action} returns what is said next, or {@code null} to end the conversation. */
    public record Option(Component label, @Nullable Component locked, Function<ServerPlayer, Node> action) {
        public static Option of(Component label, Function<ServerPlayer, Node> action) { return new Option(label, null, action); }
        public static Option locked(Component label, Component reason) { return new Option(label, reason, player -> null); }
    }

    /**
     * {@code back} is what a right click does: it returns what is said next, or {@code null} to end the conversation, and is
     * itself {@code null} when there is no way back from here.
     */
    public record Node(Component speaker, Component line, List<Option> options, @Nullable Function<ServerPlayer, Node> back) {
        public Node(Component speaker, Component line, List<Option> options) { this(speaker, line, options, null); }

        /** Something said and nothing to answer: it fades by itself. */
        public static Node say(Component speaker, Component line) { return new Node(speaker, line, List.of()); }
    }

    private static final class Session {
        final Entity npc;
        final boolean sticky;
        final Predicate<ServerPlayer> valid;
        Node node;
        long expiresAt = Long.MAX_VALUE, lastLooked;
        boolean shown;

        Session(Entity npc, Node node, boolean sticky, Predicate<ServerPlayer> valid) {
            this.npc = npc; this.node = node; this.sticky = sticky; this.valid = valid;
        }
    }

    /** The index a client sends for a right click, in place of one of the answers. */
    public static final int BACK = -1;

    private FaceDialogue() {}

    public static boolean active(UUID player) { return SESSIONS.containsKey(player); }

    /**
     * Starts or replaces what a player is hearing. A {@code sticky} conversation does not depend on looking at the speaker (a
     * timed demand must stay answerable while you watch the others) and lasts for as long as {@code valid} says so.
     */
    public static void open(ServerPlayer player, Entity npc, Node node, boolean sticky, Predicate<ServerPlayer> valid) {
        var session = new Session(npc, node, sticky, valid);
        session.lastLooked = player.level().getGameTime();
        SESSIONS.put(player.getUUID(), session);
        show(player, session);
    }

    public static void open(ServerPlayer player, Entity npc, Node node) { open(player, npc, node, false, p -> true); }

    public static void close(ServerPlayer player) {
        if (SESSIONS.remove(player.getUUID()) != null) PacketDistributor.sendToPlayer(player, FaceDialogueStatePayload.closed());
    }

    private static void show(ServerPlayer player, Session session) {
        var node = session.node;
        // Nothing to answer: it is said once and then goes.
        session.shown = true;
        session.expiresAt = node.options().isEmpty() ? player.level().getGameTime() + REPLY_TICKS : Long.MAX_VALUE;
        var options = node.options().stream().limit(FaceDialogueStatePayload.MAX_OPTIONS)
                .map(option -> new FaceDialogueStatePayload.Option(option.label(), option.locked())).toList();
        PacketDistributor.sendToPlayer(player, new FaceDialogueStatePayload(session.npc.getId(), node.speaker(), node.line(), options, true,
                session.npc instanceof FaceTalker talker ? talker.header(player) : null));
    }

    public static void choose(ServerPlayer player, int index) {
        if (index == BACK) { back(player); return; }
        var session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        // Right click has always handed over what is held, so with a contract item in hand it still does instead of answering.
        if (session.npc instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc
                && de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager.canHandIn(player, npc, player.getMainHandItem())) {
            advance(player, session, npc::handIn);
            return;
        }
        if (index < 0 || index >= session.node.options().size() || index >= FaceDialogueStatePayload.MAX_OPTIONS) return;
        var option = session.node.options().get(index);
        if (option.locked() != null) return; // A locked answer cannot be forced by a modified client.
        advance(player, session, option.action());
    }

    /** One step back: what the speaker says to go back, or the end of the conversation where there is nothing further back. */
    public static void back(ServerPlayer player) {
        var session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        // Right click has always handed over what is held, so with a contract item in hand it still does, wherever the talk is.
        if (session.npc instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc
                && de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager.canHandIn(player, npc, player.getMainHandItem())) {
            advance(player, session, npc::handIn);
            return;
        }
        if (session.node.back() == null) return;
        advance(player, session, session.node.back());
    }

    private static void advance(ServerPlayer player, Session session, Function<ServerPlayer, Node> action) {
        if (!session.valid.test(player) || !session.npc.isAlive()) return;
        var next = action.apply(player);
        if (SESSIONS.get(player.getUUID()) != session) return; // The answer itself ended or replaced the conversation.
        if (next == null) {
            close(player);
            COOLDOWN.put(player.getUUID(), player.level().getGameTime() + COOLDOWN_TICKS);
            return;
        }
        session.node = next;
        show(player, session);
    }

    /** Opens one of the speaker's windows at once, for a key pressed while their dialogue is up. */
    public static void openWindow(ServerPlayer player, int view) {
        var session = SESSIONS.get(player.getUUID());
        if (session == null || !(session.npc instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc)
                || !session.valid.test(player) || !npc.willTalk(player) || !npc.opensWindow(player, view)) return;
        // The client keeps what it shows, so coming back needs no more than the server taking the conversation up again.
        SESSIONS.remove(player.getUUID());
        PAUSED.put(player.getUUID(), session);
        npc.openTradeMenu(player, view);
        if (player.containerMenu == player.inventoryMenu && PAUSED.remove(player.getUUID(), session)) SESSIONS.put(player.getUUID(), session); // No window opened.
    }

    /** A window of theirs was closed: back to what they were saying, as it was. */
    public static void resume(ServerPlayer player) {
        var session = PAUSED.remove(player.getUUID());
        if (session == null) return;
        if (!player.isAlive() || !session.npc.isAlive() || !session.valid.test(player)
                || session.npc instanceof FaceTalker talker && !talker.willTalk(player)) {
            PacketDistributor.sendToPlayer(player, FaceDialogueStatePayload.closed());
            COOLDOWN.put(player.getUUID(), player.level().getGameTime() + COOLDOWN_TICKS);
            return;
        }
        session.lastLooked = player.level().getGameTime();
        SESSIONS.put(player.getUUID(), session);
        show(player, session);
    }

    public static void onTick(ServerTickEvent.Post event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            long now = player.level().getGameTime();
            var session = SESSIONS.get(player.getUUID());
            if (session != null) {
                if (!hold(player, session, now)) {
                    close(player);
                    COOLDOWN.put(player.getUUID(), now + AWAY_COOLDOWN);
                }
            }
        }
    }

    private static boolean hold(ServerPlayer player, Session session, long now) {
        if (!player.isAlive() || !session.npc.isAlive() || session.npc.level() != player.level() || now >= session.expiresAt
                || !session.valid.test(player)) return false;
        double distance = player.distanceTo(session.npc);
        if (session.npc instanceof FaceTalker talker && (!session.sticky || session.shown)) talker.listening(player);
        // The crosshair is the whole interface: it has to stay on them, give or take a moment, or the conversation goes.
        if (watching(player, session.npc, session.sticky ? STICKY_LOOK : LOOK_RANGE)) session.lastLooked = now;
        boolean away = now - session.lastLooked > AWAY_TICKS;
        if (session.sticky) {
            // A timed demand cannot be dropped by looking elsewhere: it hides, keeps its clock, and returns when you look back.
            if (distance > STICKY_RANGE) return false;
            if (away && session.shown) { session.shown = false; PacketDistributor.sendToPlayer(player, FaceDialogueStatePayload.closed()); }
            else if (!away && !session.shown) show(player, session);
            return true;
        }
        return !away && distance <= KEEP_RANGE && (!(session.npc instanceof FaceTalker talker) || talker.willTalk(player));
    }

    private static boolean watching(ServerPlayer player, Entity npc, double range) {
        Vec3 eye = player.getEyePosition();
        return npc.getBoundingBox().inflate(0.2).clip(eye, eye.add(player.getLookAngle().scale(range))).isPresent();
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        var id = event.getEntity().getUUID();
        SESSIONS.remove(id); PAUSED.remove(id); COOLDOWN.remove(id);
    }

    public static void onStop(ServerStoppedEvent event) {
        SESSIONS.clear(); PAUSED.clear(); COOLDOWN.clear();
    }
}
