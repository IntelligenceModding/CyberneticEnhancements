package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.ArchiveBankingPayload;

import java.util.List;

public final class ArchiveBankingClientState {
    private static ArchiveBankingPayload snapshot = new ArchiveBankingPayload(0, 0, List.of(), List.of());
    private static boolean loaded;

    private ArchiveBankingClientState() {
    }

    public static void update(ArchiveBankingPayload payload) {
        snapshot = payload;
        loaded = true;
    }

    public static ArchiveBankingPayload get() {
        return snapshot;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void clear() {
        snapshot = new ArchiveBankingPayload(0, 0, List.of(), List.of());
        loaded = false;
    }
}
