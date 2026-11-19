package de.artemis.cyberneticenhancements.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.Locale;

public final class FlatlinedDeathCounter {
    private static String record = "------";

    private FlatlinedDeathCounter() {
    }

    public static void request() {
        record = "------";
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
    }

    public static void update(int deaths) {
        record = String.format(Locale.ROOT, "%06d", Math.max(0, deaths));
    }

    public static String record() {
        return record;
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        record = "------";
    }
}
