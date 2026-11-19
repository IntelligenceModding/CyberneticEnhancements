package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.PlayerCallPreferencesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.UUID;

public final class PlayerCallClient {
    private static Boolean enabled;
    private static long nextSync;

    private PlayerCallClient() {}
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        boolean setting = ClientConfig.ENABLE_PLAYER_CALLS.get();
        if ((enabled == null || enabled != setting) && System.nanoTime() >= nextSync) {
            PacketDistributor.sendToServer(new PlayerCallPreferencesPayload(setting));
            enabled = setting;
            nextSync = System.nanoTime() + 1_000_000_000L;
        }
    }
    public static ResourceLocation skin(UUID id) {
        var connection = Minecraft.getInstance().getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(id);
        return info == null ? DefaultPlayerSkin.get(id).texture() : info.getSkin().texture();
    }
    public static void clear() { enabled = null; nextSync = 0; de.artemis.cyberneticenhancements.common.dialogue.CallVoiceBridge.clientStatus(null); }
}
