package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.CityNavigationPayload;
import de.artemis.cyberneticenhancements.common.network.CityNavigationRequestPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CityNavigationClientState {
    public static final String MARKER_ID = "night_city";
    private static CityNavigationPayload snapshot = CityNavigationPayload.EMPTY;

    private CityNavigationClientState() {}

    public static void update(CityNavigationPayload payload) { snapshot = payload; }
    public static CityNavigationPayload get() { return snapshot; }
    public static void clear() { snapshot = CityNavigationPayload.EMPTY; }

    public static void setEnabled(boolean enabled) {
        if (!snapshot.present()) return;
        snapshot = new CityNavigationPayload(true, snapshot.dimension(), snapshot.pos(), snapshot.visited(), enabled);
        PacketDistributor.sendToServer(new CityNavigationRequestPayload(enabled));
    }
}
