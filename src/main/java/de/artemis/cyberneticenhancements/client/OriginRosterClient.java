package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.origin.OriginRosterPayload;
import de.artemis.cyberneticenhancements.common.origin.OriginRosterRequestPayload;
import net.minecraft.Util;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/** Roster pages the server has sent, asked for only while the wiki shows them and refreshed now and then. */
public final class OriginRosterClient {
    private static final long REFRESH = 15_000, RETRY = 1_500;
    private record Key(String origin, int page) {}
    private record Held(OriginRosterPayload roster, long at) {}
    private static final Map<Key, Held> PAGES = new HashMap<>();
    private static final Map<Key, Long> ASKED = new HashMap<>();
    /** The newest total per lifepath, so the page count is right before every page has been fetched. */
    private static final Map<String, Integer> TOTALS = new HashMap<>();

    private OriginRosterClient() {}

    public static void receive(OriginRosterPayload payload) {
        PAGES.put(new Key(payload.origin(), payload.page()), new Held(payload, Util.getMillis()));
        TOTALS.put(payload.origin(), payload.total());
    }

    /** The page if it has arrived; asks the server for it (again) when it is missing or old. Never blocks. */
    public static OriginRosterPayload page(String origin, int page) {
        Key key = new Key(origin, page);
        Held held = PAGES.get(key);
        long now = Util.getMillis();
        if ((held == null || now - held.at > REFRESH) && now - ASKED.getOrDefault(key, 0L) > RETRY
                && net.minecraft.client.Minecraft.getInstance().getConnection() != null) {
            ASKED.put(key, now);
            PacketDistributor.sendToServer(new OriginRosterRequestPayload(origin, page));
        }
        return held == null ? null : held.roster;
    }

    public static int total(String origin) { return TOTALS.getOrDefault(origin, -1); }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PAGES.clear();
        ASKED.clear();
        TOTALS.clear();
    }
}
