package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.EntityHitResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class FaceHazardHighlightClientState {
    private static final Map<BlockPos, Long> BLOCK_HIGHLIGHTS = new LinkedHashMap<>();
    private static final Map<UUID, Long> ENTITY_HIGHLIGHTS = new LinkedHashMap<>();
    private static final Map<UUID, Long> NPC_LOCATOR_HIGHLIGHTS = new LinkedHashMap<>();

    private FaceHazardHighlightClientState() {
    }

    public static void update(FaceHazardHighlightPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long expiry = minecraft.level.getGameTime() + Math.max(1, payload.ttlTicks());
        for (BlockPos pos : payload.positions()) {
            BLOCK_HIGHLIGHTS.put(pos.immutable(), expiry);
        }
        for (UUID entityId : payload.entityIds()) {
            (payload.npcLocator() ? NPC_LOCATOR_HIGHLIGHTS : ENTITY_HIGHLIGHTS).put(entityId, expiry);
        }
        dismissFoundNpc(minecraft);
    }

    public static Map<BlockPos, Long> activeBlockHighlights() {
        return BLOCK_HIGHLIGHTS;
    }

    public static Map<UUID, Long> activeEntityHighlights() {
        if (NPC_LOCATOR_HIGHLIGHTS.isEmpty()) return ENTITY_HIGHLIGHTS;
        Map<UUID, Long> highlights = new LinkedHashMap<>(ENTITY_HIGHLIGHTS);
        NPC_LOCATOR_HIGHLIGHTS.forEach((id, expiry) -> highlights.merge(id, expiry, Math::max));
        return highlights;
    }

    public static void prune() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        long now = minecraft.level.getGameTime();
        BLOCK_HIGHLIGHTS.entrySet().removeIf(entry -> entry.getValue() <= now);
        ENTITY_HIGHLIGHTS.entrySet().removeIf(entry -> entry.getValue() <= now);
        NPC_LOCATOR_HIGHLIGHTS.entrySet().removeIf(entry -> entry.getValue() <= now);
        dismissFoundNpc(minecraft);
    }

    private static void dismissFoundNpc(Minecraft minecraft) {
        if (NPC_LOCATOR_HIGHLIGHTS.isEmpty()) return;
        if (minecraft.screen == null) {
            if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof AbstractCityNpcEntity npc) {
                NPC_LOCATOR_HIGHLIGHTS.remove(npc.getUUID());
            } else if (minecraft.getCameraEntity() != null) {
                var camera = minecraft.getCameraEntity();
                var start = camera.getEyePosition(1.0F);
                var direction = camera.getViewVector(1.0F).scale(128.0D);
                var obstruction = camera.pick(128.0D, 1.0F, false);
                var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(camera, start, start.add(direction),
                        camera.getBoundingBox().expandTowards(direction).inflate(1.0D),
                        entity -> !entity.isSpectator() && entity.isPickable(), start.distanceToSqr(obstruction.getLocation()));
                if (hit != null && hit.getEntity() instanceof AbstractCityNpcEntity npc) {
                    NPC_LOCATOR_HIGHLIGHTS.remove(npc.getUUID());
                }
            }
        }
        if (minecraft.player != null && minecraft.level != null && minecraft.player.containerMenu instanceof FixerDialogMenu menu
                && minecraft.level.getEntity(menu.entityId()) instanceof AbstractCityNpcEntity npc) {
            NPC_LOCATOR_HIGHLIGHTS.remove(npc.getUUID());
        }
    }

    public static void clear() {
        BLOCK_HIGHLIGHTS.clear();
        ENTITY_HIGHLIGHTS.clear();
        NPC_LOCATOR_HIGHLIGHTS.clear();
    }
}
