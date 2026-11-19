package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.BannerWaypointsPayload;

import java.util.List;

public final class BannerWaypointsClientState {
    private static BannerWaypointsPayload snapshot = new BannerWaypointsPayload(List.of());

    private BannerWaypointsClientState() {
    }

    public static void update(BannerWaypointsPayload payload) {
        snapshot = payload;
    }

    public static BannerWaypointsPayload get() {
        return snapshot;
    }

    public static void clear() {
        snapshot = new BannerWaypointsPayload(List.of());
    }
}
