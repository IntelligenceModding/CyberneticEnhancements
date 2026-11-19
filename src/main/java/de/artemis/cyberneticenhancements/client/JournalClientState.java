package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.JournalPayload;
import java.util.List;

public final class JournalClientState {
    private static JournalPayload page = new JournalPayload(-1, 0, 0, List.of());
    private static long sequence;
    public static long nextRequest() { return ++sequence; }
    public static JournalPayload get() { return page; }
    public static void receive(JournalPayload payload) {
        if (JournalHistorySync.receive(payload)) return;
        if (payload.request() >= page.request()) page = payload;
    }
    public static void clear() { page = new JournalPayload(-1, 0, 0, List.of()); sequence = 0; JournalHistorySync.clear(); }
}
