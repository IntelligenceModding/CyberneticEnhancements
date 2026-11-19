package de.artemis.cyberneticenhancements.common.compat;

import de.artemis.cyberneticenhancements.common.dialogue.CallVoiceBridge;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.api.packets.*;
import org.junit.After;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import static org.junit.Assert.*;

public class VoicechatCallPluginTest {
    @After public void clear() { CallVoiceBridge.clear(); }

    @Test public void acceptedCallForwardsEncodedVoiceOnlyToTheAuthenticatedPartner() {
        var f = new Fixture();
        f.connect();
        var event = f.microphone(f.a, false);
        assertTrue(event.cancelled);
        assertEquals(1, f.deliveries.size());
        var delivery = f.deliveries.getFirst();
        assertEquals(f.b, delivery.receiver);
        assertEquals(f.a, delivery.packet.getSender());
        assertEquals(f.routes.get(f.a).channel(), delivery.packet.getChannelId());
        assertArrayEquals(new byte[]{1, 2, 3}, delivery.packet.getOpusEncodedData());
        assertNotEquals(f.routes.get(f.a).channel(), f.routes.get(f.b).channel());
        assertNotEquals(f.routes.get(f.a).channel(), CallVoiceBridge.Route.channel(UUID.randomUUID(), f.a));
    }

    @Test public void pendingCallsAndHangupDoNotHijackNormalVoiceAndStaleCallPacketsAreRejected() {
        var f = new Fixture();
        assertFalse(f.microphone(f.a, false).cancelled);
        assertTrue(f.deliveries.isEmpty());
        f.connect();
        f.microphone(f.a, false);
        var packet = f.deliveries.getFirst().packet;
        f.plugin.routes(Map.of());
        assertTrue(f.sound(f.b, packet).cancelled);
        assertFalse(f.microphone(f.a, false).cancelled);
        assertEquals(1, f.deliveries.size());
    }

    @Test public void muteDisconnectAndEarlierModerationNeverFallBackToProximityBroadcast() {
        var f = new Fixture();
        f.connect();
        var route = f.routes.get(f.a);
        f.routes.put(f.a, new CallVoiceBridge.Route(route.call(), route.peer(), route.channel(), true));
        f.plugin.routes(f.routes);
        assertTrue(f.microphone(f.a, false).cancelled);
        assertTrue(f.deliveries.isEmpty());
        f.connect();
        f.connections.remove(f.b);
        assertTrue(f.microphone(f.a, false).cancelled);
        assertTrue(f.deliveries.isEmpty());
        f.connections.put(f.b, f.connection(f.b));
        assertTrue(f.microphone(f.a, true).cancelled);
        assertTrue(f.deliveries.isEmpty());
    }

    @Test public void thirdPlayersOtherGroupsAndForgedChannelsCannotEnterACall() {
        var f = new Fixture();
        f.connect();
        f.microphone(f.a, false);
        var legitimate = f.deliveries.getFirst().packet;
        assertFalse(f.sound(f.b, legitimate).cancelled);
        assertTrue(f.sound(f.c, legitimate).cancelled);
        assertTrue(f.sound(f.b, f.packet(f.a, UUID.randomUUID(), legitimate.getCategory())).cancelled);
        assertTrue(f.sound(f.b, f.packet(f.c, UUID.randomUUID(), null)).cancelled);
        assertTrue(f.sound(f.c, f.packet(f.a, f.a, null)).cancelled);
        assertFalse(f.sound(f.c, f.packet(f.c, f.c, null)).cancelled);
    }

    private record Delivery(UUID receiver, StaticSoundPacket packet) {}
    private static final class EventState { boolean cancelled; EventState(boolean cancelled) { this.cancelled = cancelled; } }

    private static final class Fixture {
        final UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), call = UUID.randomUUID();
        final Map<Class<?>, Consumer<Object>> handlers = new HashMap<>();
        final Map<UUID, VoicechatConnection> connections = new HashMap<>();
        final Map<UUID, CallVoiceBridge.Route> routes = new HashMap<>();
        final List<Delivery> deliveries = new ArrayList<>();
        final VoicechatCallPlugin plugin = new VoicechatCallPlugin();
        final VoicechatServerApi api;

        Fixture() {
            api = proxy(VoicechatServerApi.class, (method, args) -> switch (method) {
                case "getConnectionOf" -> connections.get(args[0]);
                case "volumeCategoryBuilder" -> fluent(VolumeCategory.Builder.class, proxy(VolumeCategory.class, (m, ar) -> null));
                case "sendStaticSoundPacketTo" -> {
                    var receiver = ((VoicechatConnection) args[0]).getPlayer().getUuid();
                    var packet = (StaticSoundPacket) args[1];
                    if (!sound(receiver, packet).cancelled) deliveries.add(new Delivery(receiver, packet));
                    yield null;
                }
                default -> null;
            });
            connections.put(a, connection(a)); connections.put(b, connection(b)); connections.put(c, connection(c));
            plugin.registerEvents(proxy(EventRegistration.class, (method, args) -> {
                @SuppressWarnings("unchecked") var handler = (Consumer<Object>) args[1];
                handlers.put((Class<?>) args[0], handler);
                return null;
            }));
            handlers.get(VoicechatServerStartedEvent.class).accept(proxy(VoicechatServerStartedEvent.class, (m, ar) -> api));
        }

        VoicechatConnection connection(UUID id) {
            var player = proxy(de.maxhenkel.voicechat.api.ServerPlayer.class, (m, ar) -> m.equals("getUuid") ? id : null);
            return proxy(VoicechatConnection.class, (m, ar) -> switch (m) {
                case "getPlayer" -> player;
                case "isConnected", "isInstalled" -> true;
                case "isDisabled" -> false;
                default -> null;
            });
        }

        void connect() {
            routes.put(a, new CallVoiceBridge.Route(call, b, CallVoiceBridge.Route.channel(call, a), false));
            routes.put(b, new CallVoiceBridge.Route(call, a, CallVoiceBridge.Route.channel(call, b), false));
            plugin.routes(routes);
        }

        EventState microphone(UUID sender, boolean cancelled) {
            var state = new EventState(cancelled);
            var microphone = proxy(MicrophonePacket.class, (method, args) -> {
                if (!method.equals("staticSoundPacketBuilder")) return null;
                Map<String, Object> values = new HashMap<>();
                return builder(values);
            });
            handlers.get(MicrophonePacketEvent.class).accept(proxy(MicrophonePacketEvent.class, (m, ar) -> switch (m) {
                case "cancel" -> { state.cancelled = true; yield true; }
                case "isCancelled" -> state.cancelled;
                case "getSenderConnection" -> connections.get(sender);
                case "getPacket" -> microphone;
                case "getVoicechat" -> api;
                default -> null;
            }));
            return state;
        }

        StaticSoundPacket.Builder<?> builder(Map<String, Object> values) {
            return proxy(StaticSoundPacket.Builder.class, (method, args) -> {
                if (method.equals("build")) return packet((UUID) values.get("sender"), (UUID) values.get("channelId"), (String) values.get("category"));
                values.put(method, args[0]);
                return builder(values);
            });
        }

        StaticSoundPacket packet(UUID sender, UUID channel, String category) {
            return proxy(StaticSoundPacket.class, (m, ar) -> switch (m) {
                case "getSender" -> sender;
                case "getChannelId" -> channel;
                case "getCategory" -> category;
                case "getOpusEncodedData" -> new byte[]{1, 2, 3};
                default -> null;
            });
        }

        EventState sound(UUID receiver, StaticSoundPacket packet) {
            var state = new EventState(false);
            handlers.get(StaticSoundPacketEvent.class).accept(proxy(StaticSoundPacketEvent.class, (m, ar) -> switch (m) {
                case "cancel" -> { state.cancelled = true; yield true; }
                case "isCancelled" -> state.cancelled;
                case "getReceiverConnection" -> connections.get(receiver);
                case "getPacket" -> packet;
                default -> null;
            }));
            return state;
        }
    }

    private static <T> T fluent(Class<T> type, Object result) {
        return proxy(type, (method, args) -> method.equals("build") ? result : fluent(type, result));
    }

    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (method.getName().equals("hashCode")) return System.identityHashCode(instance);
            if (method.getName().equals("equals")) return instance == args[0];
            if (method.getName().equals("toString")) return type.getSimpleName();
            return handler.apply(method.getName(), args);
        }));
    }
}
