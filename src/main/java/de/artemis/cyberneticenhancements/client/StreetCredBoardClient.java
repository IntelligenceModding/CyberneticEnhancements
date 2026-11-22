package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.streetcred.StreetCredBoardPayload;
import de.artemis.cyberneticenhancements.common.streetcred.StreetCredBoardRequestPayload;
import net.minecraft.Util;
import net.neoforged.neoforge.network.PacketDistributor;

/** The leaderboard the server last sent, asked for only while the Reputation tab shows it and refreshed now and then. */
public final class StreetCredBoardClient {
    private static final long REFRESH = 10_000, RETRY = 1_500;
    private static StreetCredBoardPayload board;
    private static long receivedAt, askedAt;

    private StreetCredBoardClient() {}

    public static void receive(StreetCredBoardPayload payload) { board = payload; receivedAt = Util.getMillis(); }

    /** The board if it has arrived; asks the server for it (again) when it is missing or old. Never blocks. */
    public static StreetCredBoardPayload board() {
        long now = Util.getMillis();
        if ((board == null || now - receivedAt > REFRESH) && now - askedAt > RETRY && net.minecraft.client.Minecraft.getInstance().getConnection() != null) {
            askedAt = now;
            PacketDistributor.sendToServer(new StreetCredBoardRequestPayload());
        }
        return board;
    }

    public static void clear() { board = null; receivedAt = 0; askedAt = 0; }
}
