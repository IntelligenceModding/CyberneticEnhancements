package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.client.FastTravelClientState;
import de.artemis.cyberneticenhancements.common.entity.HolographicDeployment;
import de.artemis.cyberneticenhancements.common.fasttravel.FastTravel;
import de.artemis.cyberneticenhancements.common.network.FastTravelEffectPayload;
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
    /** Players dissolving at a fast travel terminal, still standing there until the trip ends. */
    private static final Map<Integer, Long> TRAVEL_OUT = new HashMap<>();
    private static final int TRAVEL_TIMEOUT = 60;
    private static ClientLevel level;

    private PlayerDeploymentRender() {}

    private static boolean updateLevel() {
        var current = Minecraft.getInstance().level;
        if (level != current) {
            ARRIVALS.clear();
            DEPARTURES.clear();
            TRAVEL_OUT.clear();
            level = current;
        }
        return level != null;
    }

    public static void receiveTravel(FastTravelEffectPayload payload) {
        if (!updateLevel()) return;
        long now = level.getGameTime();
        if (payload.departing()) {
            ARRIVALS.remove(payload.entityId());
            TRAVEL_OUT.put(payload.entityId(), now);
        } else {
            TRAVEL_OUT.remove(payload.entityId());
            ARRIVALS.put(payload.entityId(), now);
        }
        var player = Minecraft.getInstance().player;
        if (player != null && player.getId() == payload.entityId()) FastTravelClientState.trip(payload.departing(), now);
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
        TRAVEL_OUT.values().removeIf(started -> now - started >= TRAVEL_TIMEOUT);
        for (var arrival : ARRIVALS.entrySet()) {
            if (level.getEntity(arrival.getKey()) instanceof AbstractClientPlayer player && visible(player))
                HolographicDeployment.scan(player, now - arrival.getValue(), false);
        }
        for (var leaving : TRAVEL_OUT.entrySet()) {
            if (level.getEntity(leaving.getKey()) instanceof AbstractClientPlayer player && visible(player))
                HolographicDeployment.scan(player, Math.min(now - leaving.getValue(), HolographicDeployment.DURATION), true);
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
        var leaving = TRAVEL_OUT.get(player.getId());
        if (leaving != null) {
            // The hologram builds up over the whole wait, so the player is gone by the time the teleport fires.
            float age = Math.min(level.getGameTime() - leaving + partial, FastTravel.DELAY) * HolographicDeployment.DURATION / FastTravel.DELAY;
            float fade = HolographicDeployment.visibility(age, true);
            return DeploymentHologram.translucentBuffers(fade, TINT, player.tickCount + partial, pose, buffers);
        }
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
        TRAVEL_OUT.clear();
        level = null;
    }
}
