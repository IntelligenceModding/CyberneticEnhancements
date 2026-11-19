package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.common.entity.HolographicDeployment;
import de.artemis.cyberneticenhancements.common.network.PlayerDeploymentPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

public final class PlayerDeploymentRender {
    private static final int TINT = 0x55CEFF;
    private static final Map<Integer, Long> ARRIVALS = new HashMap<>();
    private static final Map<RemotePlayer, Long> DEPARTURES = new HashMap<>();
    private static ClientLevel level;

    private PlayerDeploymentRender() {}

    private static boolean updateLevel() {
        var current = Minecraft.getInstance().level;
        if (level != current) {
            ARRIVALS.clear();
            DEPARTURES.clear();
            level = current;
        }
        return level != null;
    }

    public static void receive(PlayerDeploymentPayload payload) {
        if (!updateLevel()) return;
        if (!payload.departing()) {
            ARRIVALS.put(payload.entityId(), payload.started());
        } else if (level.getEntity(payload.entityId()) instanceof RemotePlayer player && visible(player)) {
            // Cache the player info before the server removes the player-list entry.
            player.getSkin();
            ARRIVALS.remove(player.getId());
            DEPARTURES.put(player, level.getGameTime());
        }
    }

    public static void tick(ClientTickEvent.Post event) {
        if (!updateLevel()) return;
        long now = level.getGameTime();
        ARRIVALS.values().removeIf(started -> now - started >= HolographicDeployment.DURATION);
        DEPARTURES.entrySet().removeIf(entry -> now - entry.getValue() >= HolographicDeployment.DURATION
                || entry.getKey().isRemoved() && level.getPlayerByUUID(entry.getKey().getUUID()) != null);
        for (var arrival : ARRIVALS.entrySet()) {
            if (level.getEntity(arrival.getKey()) instanceof AbstractClientPlayer player && visible(player))
                HolographicDeployment.scan(player, now - arrival.getValue(), false);
        }
        for (var departure : DEPARTURES.entrySet()) {
            if (departure.getKey().isRemoved()) HolographicDeployment.scan(departure.getKey(), now - departure.getValue(), true);
        }
    }

    private static boolean visible(AbstractClientPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isInvisible();
    }

    public static MultiBufferSource arrivalBuffers(AbstractClientPlayer player, float partial, PoseStack pose, MultiBufferSource buffers) {
        if (level == null || player.level() != level || !visible(player) || DeathBodies.rendering(player)) return buffers;
        var started = ARRIVALS.get(player.getId());
        if (started == null) return buffers;
        float visibility = HolographicDeployment.visibility(level.getGameTime() - started + partial, false);
        return visibility >= 1 ? buffers
                : DeploymentHologram.translucentBuffers(visibility, TINT, player.tickCount + partial, pose, buffers);
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !updateLevel()
                || DEPARTURES.isEmpty() || ShaderPackCompatibility.isRenderingShadowPass()) return;
        var minecraft = Minecraft.getInstance();
        var camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = minecraft.renderBuffers().bufferSource();
        var pose = new PoseStack();
        for (var departure : DEPARTURES.entrySet()) {
            var player = departure.getKey();
            if (!player.isRemoved() || player.distanceToSqr(camera) > 80 * 80 || !level.hasChunkAt(player.blockPosition())
                    || level.getPlayerByUUID(player.getUUID()) != null) continue;
            float age = level.getGameTime() - departure.getValue() + partial;
            float visibility = HolographicDeployment.visibility(age, true);
            if (visibility <= 0) continue;
            if (!(minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) continue;
            var position = player.position().add(renderer.getRenderOffset(player, 1)).subtract(camera);
            pose.pushPose();
            try {
                pose.translate(position.x, position.y, position.z);
                var target = DeploymentHologram.translucentBuffers(visibility, TINT, player.tickCount + age, pose, buffers);
                renderer.render(player, player.getYRot(), 1, pose, target,
                        LevelRenderer.getLightColor(level, BlockPos.containing(player.position())));
                buffers.endBatch();
            } finally {
                pose.popPose();
            }
        }
    }

    public static void name(RenderNameTagEvent event) {
        if (event.getEntity() instanceof RemotePlayer player && player.isRemoved() && DEPARTURES.containsKey(player))
            event.setCanRender(TriState.FALSE);
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ARRIVALS.clear();
        DEPARTURES.clear();
        level = null;
    }
}
