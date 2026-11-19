package de.artemis.cyberneticenhancements.common.dialogue;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public final class CallVoiceBridge {
    public interface Transport {
        boolean available(UUID player);
        void routes(Map<UUID, Route> routes);
    }
    public interface ClientStatus {
        boolean muted();
        boolean talking(UUID player);
    }

    public record Route(UUID call, UUID peer, UUID channel, boolean muted) {
        public static UUID channel(UUID call, UUID speaker) {
            return UUID.nameUUIDFromBytes((call + ":" + speaker).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final Transport ABSENT = new Transport() {
        public boolean available(UUID player) { return false; }
        public void routes(Map<UUID, Route> routes) {}
    };
    private static volatile Transport transport = ABSENT;
    private static volatile ClientStatus clientStatus;

    private CallVoiceBridge() {}
    public static boolean installed() { return transport != ABSENT; }
    public static void clientStatus(ClientStatus status) { clientStatus = status; }
    public static boolean microphoneMuted() { var status = clientStatus; return status != null && status.muted(); }
    public static boolean talking(UUID player) { var status = clientStatus; return status != null && status.talking(player); }
    public static boolean available(UUID player) { return transport.available(player); }
    public static void routes(Map<UUID, Route> routes) { transport.routes(Map.copyOf(routes)); }
    public static Transport install(Transport replacement) { var previous = transport; previous.routes(Map.of()); transport = replacement; return previous; }
    public static void clear() { transport.routes(Map.of()); transport = ABSENT; }
}
