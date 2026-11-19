package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.entity.HolographicDeployment;
import de.artemis.cyberneticenhancements.common.network.PlayerDeploymentPayload;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayerDeployment {
    private static final Map<ServerPlayer, PlayerDeploymentPayload> ARRIVALS = new WeakHashMap<>();

    private PlayerDeployment() {}

    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !visible(player)) return;
        var payload = new PlayerDeploymentPayload(player.getId(), player.level().getGameTime(), false);
        ARRIVALS.put(player, payload);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
    }

    public static void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer) || !(event.getTarget() instanceof ServerPlayer player)) return;
        var arrival = ARRIVALS.get(player);
        if (arrival != null && visible(player) && player.level().getGameTime() - arrival.started() < HolographicDeployment.DURATION)
            PacketDistributor.sendToPlayer(viewer, arrival);
    }

    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ARRIVALS.remove(player);
        if (visible(player)) PacketDistributor.sendToPlayersTrackingEntity(player,
                new PlayerDeploymentPayload(player.getId(), player.level().getGameTime(), true));
    }

    private static boolean visible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isInvisible() && !DeathBodySync.fallen(player);
    }
}
