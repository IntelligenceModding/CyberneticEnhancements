package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.*;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
import static de.artemis.cyberneticenhancements.common.network.PlayerCallRequestPayload.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerCallGameTests {
    @GameTest(template = "sandevistan_test", batch = "player_voice_calls", timeoutTicks = 100)
    public static void optionalVoicePluginStartsWhenInstalledAndDoesNotLoadWhenAbsent(GameTestHelper h) throws ReflectiveOperationException {
        h.assertTrue(CallVoiceBridge.installed() == net.neoforged.fml.ModList.get().isLoaded("voicechat"), "Voice plugin did not finish initialization, or a missing dependency was loaded");
        if (CallVoiceBridge.installed()) {
            var apiClass = Class.forName("de.maxhenkel.voicechat.plugins.impl.VoicechatServerApiImpl");
            var api = apiClass.getMethod("instance").invoke(null);
            var categories = (Collection<?>) apiClass.getMethod("getVolumeCategories").invoke(api);
            Object category = null;
            for (var candidate : categories) {
                if (candidate.getClass().getMethod("getId").invoke(candidate).equals("neural_calls")) category = candidate;
            }
            h.assertTrue(category != null, "Call volume category was not registered");
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                category.getClass().getMethod("toBytes", net.minecraft.network.FriendlyByteBuf.class).invoke(category, buffer);
                var decoded = category.getClass().getMethod("fromBytes", net.minecraft.network.FriendlyByteBuf.class).invoke(null, buffer);
                h.assertTrue(category.getClass().getMethod("getName").invoke(category).equals(decoded.getClass().getMethod("getName").invoke(decoded)),
                        "Call volume category could not be synchronized to a joining client");
                h.assertTrue(buffer.readableBytes() == 0, "Category decoder left unread metadata");
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "player_voice_calls", timeoutTicks = 100)
    public static void callPacketsAndPlayerDirectoryPreserveIdentityAndStatus(GameTestHelper h) {
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            UUID id = UUID.randomUUID(), request = UUID.randomUUID(), peer = UUID.randomUUID();
            var state = new PlayerCallStatePayload(id, request, peer, "Caller", "connected", false, true);
            PlayerCallStatePayload.STREAM_CODEC.encode(buffer, state);
            h.assertTrue(state.equals(PlayerCallStatePayload.STREAM_CODEC.decode(buffer)), "Call state lost its server ID, correlation or mute state");
            var action = new PlayerCallRequestPayload(id, peer, ANSWER);
            PlayerCallRequestPayload.STREAM_CODEC.encode(buffer, action);
            h.assertTrue(action.equals(PlayerCallRequestPayload.STREAM_CODEC.decode(buffer)), "Call request did not preserve the peer and action");
            var preference = new PlayerCallPreferencesPayload(false);
            PlayerCallPreferencesPayload.STREAM_CODEC.encode(buffer, preference);
            h.assertTrue(preference.equals(PlayerCallPreferencesPayload.STREAM_CODEC.decode(buffer)), "Disabled-call preference did not round-trip");
            var directory = new ArchiveContactsPayload(List.of(new ArchiveContactsPayload.ContactEntry(peer.toString(), "player", "Player", "generic", 0,
                    true, false, false, false, false, "", false, 0, 0, 0, false, "", "aqua", "blocked")));
            ArchiveContactsPayload.STREAM_CODEC.encode(buffer, directory);
            h.assertTrue(directory.equals(ArchiveContactsPayload.STREAM_CODEC.decode(buffer)), "Player directory lost its call status");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "player_voice_calls", timeoutTicks = 100)
    public static void onlyTheCalleeCanAcceptAndBusyOrStaleRequestsCannotStealCalls(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            UUID request = UUID.randomUUID();
            var ringing = f.start(f.a, f.b, request);
            h.assertTrue(ringing.incoming() && ringing.status().equals("ringing") && !ringing.callId().equals(request)
                    && ringing.requestId().equals(request) && f.voice.routes.isEmpty(), "Ringing must allocate a server nonce and leave voice unrouted");
            var incoming = state(f.a);
            h.assertTrue(!incoming.incoming() && incoming.callId().equals(ringing.callId()), "Caller did not receive the same session");
            f.action(f.a, request, f.b, START);
            h.assertTrue(state(f.a).callId().equals(ringing.callId()) && NpcCallGameTests.drain(f.b).isEmpty(),
                    "A repeated start request replaced the call or rang the recipient again");
            f.action(f.a, ringing.callId(), f.b, ANSWER);
            f.action(f.c, ringing.callId(), f.a, ANSWER);
            f.action(f.b, UUID.randomUUID(), f.a, ANSWER);
            h.assertTrue(f.voice.routes.isEmpty(), "A forged or caller-side acceptance connected voice");
            f.action(f.c, UUID.randomUUID(), f.b, START);
            h.assertTrue(state(f.c).status().equals("busy") && f.voice.routes.isEmpty(), "An occupied incoming line was displaced");
            f.action(f.b, ringing.callId(), f.a, ANSWER);
            h.assertTrue(f.voice.routes.size() == 2 && f.voice.routes.get(f.a.player().getUUID()).peer().equals(f.b.player().getUUID()), "Accepted call did not install exactly two reciprocal routes");
            h.assertTrue(state(f.a).status().equals("connected") && state(f.b).status().equals("connected"), "Both participants were not notified");
            f.action(f.c, ringing.callId(), f.a, END);
            f.action(f.b, UUID.randomUUID(), f.a, END);
            h.assertTrue(f.voice.routes.size() == 2, "An unrelated player or stale nonce ended a call");
            f.action(f.a, ringing.callId(), f.b, MUTE);
            h.assertTrue(f.voice.routes.get(f.a.player().getUUID()).muted() && !f.voice.routes.get(f.b.player().getUUID()).muted(), "Mute affected the wrong participant");
            state(f.a); state(f.b);
            f.action(f.a, ringing.callId(), f.b, END);
            h.assertTrue(f.voice.routes.isEmpty() && !PlayerCallManager.occupied(f.a.player().getUUID()) && !PlayerCallManager.occupied(f.b.player().getUUID()), "Hangup retained audio or busy slots");
            h.assertTrue(state(f.a).status().equals("ended") && state(f.b).status().equals("ended"), "Hangup was not delivered to both participants");
            h.assertTrue(NpcCallGameTests.drain(f.c).isEmpty(), "A third player received private call updates");
            f.assertImmediateRedial(h);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "player_voice_calls", timeoutTicks = 100)
    public static void quickCancellationDeclinesTimeoutsDisconnectsAndDisabledVoiceReleaseBothLines(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            UUID request = UUID.randomUUID();
            f.start(f.a, f.b, request); state(f.a);
            f.action(f.a, request, f.b, END);
            h.assertTrue(!PlayerCallManager.occupied(f.b.player().getUUID()) && f.voice.routes.isEmpty(), "Cancel before ringing acknowledgement left a ghost call");
            state(f.a); state(f.b);
            f.assertImmediateRedial(h);
            f.reset();
            var ringing = f.start(f.a, f.b, UUID.randomUUID()); state(f.a);
            f.action(f.b, ringing.callId(), f.a, END);
            h.assertTrue(state(f.a).status().equals("declined") && state(f.b).status().equals("declined") && f.voice.routes.isEmpty(), "Decline failed to release the call");
            f.assertImmediateRedial(h);
            f.reset();
            f.start(f.a, f.b, UUID.randomUUID()); state(f.a);
            PlayerCallManager.expire(f.a.player().server, f.a.player().server.overworld().getGameTime() + 600);
            h.assertTrue(state(f.a).status().equals("unanswered") && state(f.b).status().equals("unanswered"), "Ringing did not time out");
            f.assertImmediateRedial(h);
            f.reset();
            ringing = f.start(f.a, f.b, UUID.randomUUID()); state(f.a);
            f.action(f.b, ringing.callId(), f.a, ANSWER); state(f.a); state(f.b);
            f.voice.available.remove(f.b.player().getUUID());
            PlayerCallManager.expire(f.a.player().server, f.a.player().server.overworld().getGameTime());
            h.assertTrue(f.voice.routes.isEmpty() && state(f.a).status().equals("voice_lost"), "Lost voice connection retained routing");
            state(f.b);
            f.reset();
            ringing = f.start(f.a, f.b, UUID.randomUUID()); state(f.a);
            f.action(f.b, ringing.callId(), f.a, ANSWER); state(f.a); state(f.b);
            PlayerCallManager.onLogout(new PlayerEvent.PlayerLoggedOutEvent(f.b.player()));
            h.assertTrue(f.voice.routes.isEmpty() && state(f.a).status().equals("disconnected"), "Disconnect did not notify partner and revoke audio");
            state(f.b);
            f.reset();
            ringing = f.start(f.a, f.b, UUID.randomUUID()); state(f.a);
            f.action(f.b, ringing.callId(), f.a, ANSWER); state(f.a); state(f.b);
            PlayerCallManager.preferences(f.b.player(), false);
            h.assertTrue(f.voice.routes.isEmpty() && state(f.a).status().equals("ended"), "Disabling player calls did not end an accepted call");
            state(f.b);
            f.reset();
            ringing = f.start(f.a, f.b, UUID.randomUUID()); state(f.a);
            f.action(f.b, ringing.callId(), f.a, ANSWER); state(f.a); state(f.b);
            f.b.player().setHealth(0);
            PlayerCallManager.expire(f.a.player().server, f.a.player().server.overworld().getGameTime());
            h.assertTrue(f.voice.routes.isEmpty() && state(f.a).status().equals("unavailable"), "Death retained voice routing");
            state(f.b);
            f.b.player().setHealth(f.b.player().getMaxHealth());
            f.reset();
            ringing = f.start(f.a, f.b, UUID.randomUUID()); state(f.a);
            f.action(f.b, ringing.callId(), f.a, ANSWER); state(f.a); state(f.b);
            PlayerCallManager.expire(f.a.player().server, f.a.player().server.overworld().getGameTime() + 1801);
            h.assertTrue(f.voice.routes.isEmpty() && state(f.a).status().equals("connection_lost"), "Missing heartbeat left the partner connected");
            state(f.b);
            f.reset();
            f.voice.available.clear();
            f.action(f.a, UUID.randomUUID(), f.b, START);
            h.assertTrue(state(f.a).status().equals("voice_unavailable") && !PlayerCallManager.occupied(f.a.player().getUUID()), "Missing voice support left an occupied line");
            f.voice.available.addAll(List.of(f.a.player().getUUID(), f.b.player().getUUID()));
            f.assertImmediateRedial(h);
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "player_voice_calls", timeoutTicks = 100)
    public static void blockingSurvivesRespawnAndNpcCallsShareTheSameOccupiedLine(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            PlayerCallManager.contactAction(f.b.player(), f.a.player().getUUID(), ArchiveContactActionPayload.ACTION_TOGGLE_BLOCK);
            h.assertTrue(PlayerCallManager.blocked(f.b.player(), f.a.player().getUUID()), "Block was not stored server-side");
            PlayerCallManager.onClone(new PlayerEvent.Clone(f.c.player(), f.b.player(), true));
            h.assertTrue(PlayerCallManager.blocked(f.c.player(), f.a.player().getUUID()), "Block list was lost on respawn");
            NpcCallGameTests.drain(f.b);
            f.action(f.a, UUID.randomUUID(), f.b, START);
            h.assertTrue(state(f.a).status().equals("unavailable") && NpcCallGameTests.drain(f.b).isEmpty(), "A blocked caller reached the recipient");
            f.reset();
            PlayerCallManager.contactAction(f.b.player(), f.a.player().getUUID(), ArchiveContactActionPayload.ACTION_TOGGLE_BLOCK);
            NpcCallGameTests.drain(f.b);
            var ringing = f.start(f.a, f.b, UUID.randomUUID()); state(f.a);
            var npc = ModEntityTypes.FIXER.get().create(h.getLevel());
            npc.setPos(f.a.player().position().add(0, 0, 12));
            h.getLevel().addFreshEntity(npc);
            npc.markMet(f.a.player());
            try {
                NpcCallManager.handle(f.a.player(), new NpcCallRequestPayload(UUID.randomUUID(), npc.getUUID(), NpcCallRequestPayload.START));
                h.assertTrue(!NpcCallManager.hasCall(f.a.player().getUUID()) && PlayerCallManager.occupied(f.a.player().getUUID()), "NPC call replaced a player call");
                NpcCallGameTests.drain(f.a);
                f.action(f.b, ringing.callId(), f.a, ANSWER); state(f.a); state(f.b);
                f.action(f.a, ringing.callId(), f.b, BLOCK); state(f.a); state(f.b);
                h.assertTrue(PlayerCallManager.blocked(f.a.player(), f.b.player().getUUID()) && f.voice.routes.isEmpty(), "Block-and-hang-up did not revoke audio");
                var contact = PlayerCallManager.contacts(f.a.player()).stream().filter(entry -> entry.npcId().equals(f.b.player().getUUID().toString())).findFirst().orElseThrow();
                h.assertTrue(contact.isPlayer() && contact.callStatus().equals("blocked") && !contact.hasLocation(), "Directory leaked position or lost blocked state");
            } finally { npc.discard(); }
            f.reset();
            PlayerCallManager.contactAction(f.a.player(), f.b.player().getUUID(), ArchiveContactActionPayload.ACTION_TOGGLE_BLOCK);
            NpcCallGameTests.drain(f.a);
            PlayerCallManager.preferences(f.b.player(), false);
            f.action(f.a, UUID.randomUUID(), f.b, START);
            h.assertTrue(state(f.a).status().equals("unavailable"), "Disabled calls still rang");
        }
        h.succeed();
    }

    private static PlayerCallStatePayload state(NpcCallGameTests.CallPlayer player) {
        return NpcCallGameTests.drain(player).stream().filter(PlayerCallStatePayload.class::isInstance).map(PlayerCallStatePayload.class::cast).reduce((first, last) -> last).orElseThrow();
    }

    private static final class Voice implements CallVoiceBridge.Transport {
        final Set<UUID> available = new HashSet<>();
        Map<UUID, CallVoiceBridge.Route> routes = Map.of();
        public boolean available(UUID player) { return available.contains(player); }
        public void routes(Map<UUID, CallVoiceBridge.Route> replacement) { routes = replacement; }
    }

    private static final class Fixture implements AutoCloseable {
        final NpcCallGameTests.CallPlayer a, b, c;
        final Voice voice = new Voice();
        final CallVoiceBridge.Transport previous;
        Fixture(GameTestHelper h) {
            a = NpcCallGameTests.player(h, "voice-caller"); b = NpcCallGameTests.player(h, "voice-callee"); c = NpcCallGameTests.player(h, "voice-observer");
            previous = CallVoiceBridge.install(voice);
            reset();
        }
        void reset() {
            PlayerCallManager.onStop(null);
            voice.available.addAll(List.of(a.player().getUUID(), b.player().getUUID(), c.player().getUUID()));
            for (var player : List.of(a, b, c)) { PlayerCallManager.preferences(player.player(), true); NpcCallGameTests.drain(player); }
        }
        void action(NpcCallGameTests.CallPlayer source, UUID id, NpcCallGameTests.CallPlayer peer, int action) {
            PlayerCallManager.handle(source.player(), new PlayerCallRequestPayload(id, peer.player().getUUID(), action));
        }
        PlayerCallStatePayload start(NpcCallGameTests.CallPlayer source, NpcCallGameTests.CallPlayer peer, UUID request) {
            action(source, request, peer, START);
            return state(peer);
        }
        void assertImmediateRedial(GameTestHelper h) {
            var ringing = start(a, b, UUID.randomUUID());
            h.assertTrue(ringing.status().equals("ringing") && state(a).status().equals("ringing"), "The caller could not immediately redial");
            action(a, ringing.callId(), b, END); state(a); state(b);
            var reverse = start(b, a, UUID.randomUUID());
            h.assertTrue(reverse.status().equals("ringing") && state(b).status().equals("ringing"), "The other participant could not immediately call back");
            action(b, reverse.callId(), a, END); state(a); state(b);
        }
        public void close() {
            PlayerCallManager.onStop(null);
            CallVoiceBridge.install(previous);
            for (var player : List.of(a, b, c)) player.player().server.getPlayerList().remove(player.player());
        }
    }
}
