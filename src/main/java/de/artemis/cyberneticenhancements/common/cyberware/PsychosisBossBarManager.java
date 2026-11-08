package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.network.PsychosisOverlayPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PsychosisBossBarManager {
    private static String lastBroadcastSignature = "";

    private PsychosisBossBarManager() {
    }

    public static void update(ServerPlayer player) {
        sync(player.server, null);
    }

    public static void onPlayerLogin(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildPayload(player.server, null));
    }

    public static void onPlayerLogout(ServerPlayer player) {
        sync(player.server, player.getUUID());
    }

    private static void sync(MinecraftServer server, UUID excludedPlayerId) {
        PsychosisOverlayPayload payload = buildPayload(server, excludedPlayerId);
        String signature = signature(payload);
        if (signature.equals(lastBroadcastSignature)) {
            return;
        }

        lastBroadcastSignature = signature;
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (excludedPlayerId != null && onlinePlayer.getUUID().equals(excludedPlayerId)) {
                continue;
            }
            PacketDistributor.sendToPlayer(onlinePlayer, payload);
        }
    }

    private static PsychosisOverlayPayload buildPayload(MinecraftServer server, UUID excludedPlayerId) {
        List<PsychosisOverlayPayload.Entry> entries = new ArrayList<>();
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (excludedPlayerId != null && onlinePlayer.getUUID().equals(excludedPlayerId)) {
                continue;
            }
            if (!CyberstrainManager.isPsychosisActive(onlinePlayer)) {
                continue;
            }

            entries.add(new PsychosisOverlayPayload.Entry(
                    onlinePlayer.getScoreboardName(),
                    CyberstrainManager.getPsychosisTierName(onlinePlayer),
                    (int) CyberstrainManager.getPsychosisSecondsRemaining(onlinePlayer),
                    Math.max(1, (int) ((CyberstrainManager.getPsychosisTotalTicks(onlinePlayer) + 19L) / 20L))
            ));
        }

        entries.sort(Comparator
                .comparingInt((PsychosisOverlayPayload.Entry entry) -> "MAJOR".equals(entry.tier()) ? 0 : 1)
                .thenComparingInt(PsychosisOverlayPayload.Entry::remainingSeconds)
                .thenComparing(PsychosisOverlayPayload.Entry::playerName));
        return new PsychosisOverlayPayload(entries);
    }

    private static String signature(PsychosisOverlayPayload payload) {
        StringBuilder builder = new StringBuilder();
        for (PsychosisOverlayPayload.Entry entry : payload.entries()) {
            builder.append(entry.playerName())
                    .append('|')
                    .append(entry.tier())
                    .append('|')
                    .append(entry.remainingSeconds())
                    .append('|')
                    .append(entry.totalSeconds())
                    .append(';');
        }
        return builder.toString();
    }
}
