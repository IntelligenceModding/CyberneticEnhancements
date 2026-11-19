package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.cyberneticenhancements.common.blockentity.DropoffPointBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class DropoffPointBlockEntityRenderer implements BlockEntityRenderer<DropoffPointBlockEntity> {
    private static final java.util.Map<Object, Runnable> PENDING = new java.util.LinkedHashMap<>();
    private static final MarkerVisibility VISIBILITY = new MarkerVisibility();
    private static Object markerLevel;
    public DropoffPointBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DropoffPointBlockEntity point, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (ShaderPackCompatibility.isRenderingShadowPass()) return;
        if (!visibleToOwner(point)) { VISIBILITY.forget(point.getBlockPos()); return; }
        prepareLevel(point.getLevel());
        float visibility = VISIBILITY.sample(point.getBlockPos(), point.hasUnreadDelivery(), point.getLevel().getGameTime() + (double) partialTick);
        if (visibility <= 0) return;
        var captured = new PoseStack();
        captured.last().pose().set(pose.last().pose());
        captured.last().normal().set(pose.last().normal());
        PENDING.put(point.getBlockPos(), () -> renderProjection(point, partialTick, captured, visibility));
    }

    public static void queueQuestTurnIns(PoseStack worldPose, net.minecraft.world.phys.Vec3 camera, float partialTick) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || ShaderPackCompatibility.isRenderingShadowPass()) return;
        prepareLevel(minecraft.level);
        double time = minecraft.level.getGameTime() + (double) partialTick;
        VISIBILITY.prune(time);
        var ready = de.artemis.cyberneticenhancements.client.QuestNavigationClientState.visibleTurnInNpcs();
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity npc)
                    || !npc.isAlive() || npc.isInvisible()
                    || npc.position().distanceToSqr(camera) > 48 * 48) continue;
            float visibility = VISIBILITY.sample(npc.getUUID(), ready.contains(npc.getUUID()), time);
            if (visibility <= 0) continue;
            var captured = new PoseStack();
            captured.last().pose().set(worldPose.last().pose());
            captured.last().normal().set(worldPose.last().normal());
            var position = npc.getPosition(partialTick);
            captured.translate(position.x - camera.x, position.y - camera.y + npc.getBbHeight() + 0.55, position.z - camera.z);
            PENDING.put(npc.getUUID(), () -> {
                if (npc.isAlive() && !npc.isInvisible())
                    renderExclamation((npc.level().getGameTime() % 24000) + partialTick, captured, false, visibility);
            });
        }
    }

    public static void clearPending() {
        PENDING.clear();
        VISIBILITY.clear();
        markerLevel = null;
        DropoffMarkerDepth.close();
    }

    private static void prepareLevel(Object level) {
        if (markerLevel == level) return;
        PENDING.clear();
        VISIBILITY.clear();
        markerLevel = level;
    }

    public static void captureOpaqueDepth() {
        if (ShaderPackCompatibility.isRenderingShadowPass()) return;
        if (PENDING.isEmpty()) DropoffMarkerDepth.invalidate();
        else DropoffMarkerDepth.capture();
    }

    public static void flushProjections() {
        if (ShaderPackCompatibility.isRenderingShadowPass()) return;
        var pending = new java.util.ArrayList<>(PENDING.values());
        PENDING.clear();
        if (pending.isEmpty()) {
            DropoffMarkerDepth.invalidate();
            return;
        }
        DropoffMarkerDepth.render(() -> {
            try { pending.forEach(Runnable::run); }
            finally { Minecraft.getInstance().renderBuffers().bufferSource().endBatch(HologramRenderTypes.DELIVERY_MARKER); }
        });
    }

    private static void renderProjection(DropoffPointBlockEntity point, float partialTick, PoseStack pose, float visibility) {
        if (!visibleToOwner(point) || ShaderPackCompatibility.isRenderingShadowPass()) return;
        pose.pushPose();
        pose.translate(0.5, 1 + HologramPixels.SIZE, 0.5);
        renderExclamation((point.getLevel().getGameTime() % 24000) + partialTick, pose, true, visibility);
        pose.popPose();
    }

    private static void renderExclamation(float time, PoseStack pose, boolean withBase, float visibility) {
        pose.pushPose();
        var vertices = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(HologramRenderTypes.DELIVERY_MARKER);
        float opacity = MarkerVisibility.opacity(visibility);
        if (withBase) {
            HologramPixelGeometry.ring(pose, vertices, 0, 4, 3, HologramPixels.SIZE, fade(0x985DDEFF, opacity));
            HologramPixelGeometry.ring(pose, vertices, 0, 2, 1, HologramPixels.SIZE, fade(0x505DDEFF, opacity));
        }
        pose.translate(0, 0.25 + Math.sin(time * 0.055) * 0.025, 0);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * 0.45F));
        int sweepRow = HologramPixels.sweepRow(time, HologramPixels.MARKER_ROWS);
        float sweep = sweepRow * HologramPixels.SIZE;
        float flicker = 0.94F + 0.04F * (float) Math.sin(time * 0.23561945) + 0.02F * (float) Math.sin(time * 0.9424778);
        for (int row = 0; row < HologramPixels.MARKER_ROWS; row++) {
            if (!HologramPixels.markerRow(row)) continue;
            float rowOpacity = MarkerVisibility.rowOpacity(visibility, row);
            if (rowOpacity <= 0) continue;
            float bottom = row * HologramPixels.SIZE;
            float width = HologramPixels.SIZE;
            float stripe = HologramPixels.stripe(row, time);
            float scan = row == sweepRow ? 1 : 0;
            int alpha = Math.clamp(Math.round((75 + stripe * 85 + scan * 45) * flicker * opacity * rowOpacity), 0, 210);
            int color = alpha << 24 | (scan > 0.45F ? 0xB4EEFF : 0x5DDEFF);
            for (int side = 0; side < 4; side++) {
                pose.pushPose();
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * 90));
                quad(pose, vertices, -width, bottom, width, bottom + HologramPixels.SIZE, width, color);
                pose.popPose();
            }
            var matrix = pose.last().pose();
            if (!HologramPixels.markerRow(row - 1)) cap(matrix, vertices, bottom, width, color);
            if (!HologramPixels.markerRow(row + 1) || MarkerVisibility.rowOpacity(visibility, row + 1) <= 0)
                cap(matrix, vertices, bottom + HologramPixels.SIZE, width, color);
        }
        if (withBase) HologramPixelGeometry.ring(pose, vertices, sweep, 2, 1, HologramPixels.SIZE, fade(0x585DDEFF, opacity));
        pose.popPose();
    }

    private static boolean visibleToOwner(DropoffPointBlockEntity point) {
        var minecraft = Minecraft.getInstance();
        return !point.isRemoved() && point.getLevel() != null && point.getLevel() == minecraft.level
                && minecraft.player != null && minecraft.player.getUUID().equals(point.owner());
    }

    private static int fade(int color, float opacity) {
        return Math.round((color >>> 24) * opacity) << 24 | color & 0xFFFFFF;
    }

    private static void cap(org.joml.Matrix4f matrix, VertexConsumer vertices, float y, float width, int color) {
        vertices.addVertex(matrix, -width, y, -width).setColor(color);
        vertices.addVertex(matrix, width, y, -width).setColor(color);
        vertices.addVertex(matrix, width, y, width).setColor(color);
        vertices.addVertex(matrix, -width, y, width).setColor(color);
    }

    private static void quad(PoseStack pose, VertexConsumer vertices, float left, float bottom, float right, float top, float z, int color) {
        var matrix = pose.last().pose();
        vertices.addVertex(matrix, left, bottom, z).setColor(color);
        vertices.addVertex(matrix, right, bottom, z).setColor(color);
        vertices.addVertex(matrix, right, top, z).setColor(color);
        vertices.addVertex(matrix, left, top, z).setColor(color);
    }

    @Override
    public int getViewDistance() { return 48; }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(DropoffPointBlockEntity point) { return new net.minecraft.world.phys.AABB(point.getBlockPos()).expandTowards(0, 1.3, 0); }
}
