package de.artemis.cyberneticenhancements.common.compat;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.dialogue.CallVoiceBridge;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import java.util.Map;
import java.util.UUID;

@ForgeVoicechatPlugin
public final class VoicechatCallPlugin implements VoicechatPlugin, CallVoiceBridge.Transport {
    private static final String CATEGORY = "neural_calls";
    private final Object lock = new Object();
    private Map<UUID, CallVoiceBridge.Route> routes = Map.of();
    private volatile VoicechatServerApi server;

    public String getPluginId() { return CyberneticEnhancements.MOD_ID; }

    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
            server = event.getVoicechat();
            server.registerVolumeCategory(server.volumeCategoryBuilder().setId(CATEGORY).setName("Neural calls")
                    .setDescription("Player-to-player calls from Cybernetic Enhancements").build());
            CallVoiceBridge.install(this);
        });
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
            CallVoiceBridge.clear();
            server = null;
        });
        registration.registerEvent(MicrophonePacketEvent.class, this::microphone, -1000);
        registration.registerEvent(StaticSoundPacketEvent.class, this::sound, -1000);
        registration.registerEvent(EntitySoundPacketEvent.class, this::sound, -1000);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::sound, -1000);
        registration.registerEvent(ClientVoicechatConnectionEvent.class, event -> {
            if (!event.isConnected()) { CallVoiceBridge.clientStatus(null); return; }
            var api = event.getVoicechat();
            CallVoiceBridge.clientStatus(new CallVoiceBridge.ClientStatus() {
                public boolean muted() { return api.isMuted(); }
                public boolean talking(UUID player) { return api.isTalking(player); }
            });
        });
    }

    public boolean available(UUID player) {
        var api = server;
        var connection = api == null ? null : api.getConnectionOf(player);
        return connection != null && connection.isInstalled() && connection.isConnected() && !connection.isDisabled();
    }

    public void routes(Map<UUID, CallVoiceBridge.Route> replacement) {
        synchronized (lock) { routes = Map.copyOf(replacement); }
    }

    private void microphone(MicrophonePacketEvent event) {
        if (event.isCancelled()) return;
        var sender = event.getSenderConnection();
        if (sender == null) return;
        UUID player = sender.getPlayer().getUuid();
        synchronized (lock) {
            var route = routes.get(player);
            if (route == null) return;
            event.cancel();
            var peerRoute = routes.get(route.peer());
            if (route.muted() || peerRoute == null || !peerRoute.call().equals(route.call()) || !peerRoute.peer().equals(player)
                    || !available(player) || !available(route.peer())) return;
            var api = server;
            if (api == null) return;
            var peer = api.getConnectionOf(route.peer());
            if (peer == null) return;
            api.sendStaticSoundPacketTo(peer, event.getPacket().staticSoundPacketBuilder()
                    .sender(player).channelId(route.channel()).category(CATEGORY).build());
        }
    }

    private void sound(SoundPacketEvent<?> event) {
        if (!(event.getPacket() instanceof SoundPacket packet) || event.getReceiverConnection() == null) return;
        UUID receiver = event.getReceiverConnection().getPlayer().getUuid();
        synchronized (lock) {
            var receiving = routes.get(receiver);
            var sending = routes.get(packet.getSender());
            boolean callPacket = sending != null && packet.getChannelId().equals(sending.channel()) && CATEGORY.equals(packet.getCategory());
            if (CATEGORY.equals(packet.getCategory()) && (!callPacket || receiving == null)) { event.cancel(); return; }
            if (sending != null && (!callPacket || sending.muted() || !sending.peer().equals(receiver))) { event.cancel(); return; }
            if (receiving != null && (!callPacket || !receiving.peer().equals(packet.getSender()) || !receiving.call().equals(sending.call()))) event.cancel();
        }
    }
}
