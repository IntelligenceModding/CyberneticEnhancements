package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.common.blockentity.FastTravelBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** The hologram a fast travel terminal projects where the enchanting table keeps its book. */
public final class FastTravelBlockEntityRenderer implements BlockEntityRenderer<FastTravelBlockEntity> {
    /** Half widths, in pixels, of the rows of the diamond from bottom to top. */
    private static final int[] DIAMOND = {1, 2, 3, 4, 4, 3, 2, 1};
    private static final float TABLE_TOP = 0.75F;
    private static final int ORBITS = 3;

    public FastTravelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FastTravelBlockEntity terminal, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (terminal.getLevel() == null || ShaderPackCompatibility.isRenderingShadowPass()) return;
        float time = (terminal.getLevel().getGameTime() % 24000) + partialTick;
        var vertices = buffers.getBuffer(HologramRenderTypes.BARRIER);
        float flicker = 0.94F + 0.04F * (float) Math.sin(time * 0.23561945) + 0.02F * (float) Math.sin(time * 0.9424778);

        pose.pushPose();
        pose.translate(0.5, TABLE_TOP + HologramPixels.SIZE * 0.25F, 0.5);
        HologramPixelGeometry.ring(pose, vertices, 0, 5, 4, HologramPixels.SIZE, 0x985DDEFF);
        HologramPixelGeometry.ring(pose, vertices, 0, 3, 2, HologramPixels.SIZE, 0x505DDEFF);

        pose.translate(0, 0.2 + Math.sin(time * 0.07) * 0.03, 0);
        pose.mulPose(Axis.YP.rotationDegrees(time * 1.6F));
        int sweep = HologramPixels.sweepRow(time, DIAMOND.length);
        for (int row = 0; row < DIAMOND.length; row++) {
            float stripe = HologramPixels.stripe(row, time);
            int alpha = Math.clamp(Math.round((75 + stripe * 85 + (row == sweep ? 45 : 0)) * flicker), 0, 210);
            prism(pose, vertices, row * HologramPixels.SIZE, DIAMOND[row] * HologramPixels.SIZE, alpha << 24 | (row == sweep ? 0xB4EEFF : 0x5DDEFF),
                    row == 0, row == DIAMOND.length - 1);
        }
        pose.popPose();

        // Pixel sparks circle the diamond at different heights and speeds.
        for (int i = 0; i < ORBITS; i++) {
            pose.pushPose();
            double angle = time * (0.09 + i * 0.03) + i * Math.PI * 2 / ORBITS;
            double radius = 0.34 + 0.03 * i;
            pose.translate(0.5 + Math.cos(angle) * radius, TABLE_TOP + 0.3 + i * 0.16 + Math.sin(time * 0.1 + i * 2) * 0.04, 0.5 + Math.sin(angle) * radius);
            prism(pose, vertices, 0, HologramPixels.SIZE * 0.5F, 0xC8B4EEFF, true, true);
            pose.popPose();
        }
    }

    /** A square column one pixel tall, centred on the origin, with the caps only where nothing is stacked. */
    private static void prism(PoseStack pose, VertexConsumer vertices, float bottom, float half, int color, boolean bottomCap, boolean topCap) {
        float top = bottom + HologramPixels.SIZE;
        for (int side = 0; side < 4; side++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(side * 90));
            var matrix = pose.last().pose();
            vertices.addVertex(matrix, -half, bottom, half).setColor(color);
            vertices.addVertex(matrix, half, bottom, half).setColor(color);
            vertices.addVertex(matrix, half, top, half).setColor(color);
            vertices.addVertex(matrix, -half, top, half).setColor(color);
            pose.popPose();
        }
        var matrix = pose.last().pose();
        if (bottomCap) cap(matrix, vertices, bottom, half, color);
        if (topCap) cap(matrix, vertices, top, half, color);
    }

    private static void cap(org.joml.Matrix4f matrix, VertexConsumer vertices, float y, float half, int color) {
        vertices.addVertex(matrix, -half, y, -half).setColor(color);
        vertices.addVertex(matrix, half, y, -half).setColor(color);
        vertices.addVertex(matrix, half, y, half).setColor(color);
        vertices.addVertex(matrix, -half, y, half).setColor(color);
    }

    @Override
    public int getViewDistance() { return 48; }

    @Override
    public AABB getRenderBoundingBox(FastTravelBlockEntity terminal) { return new AABB(terminal.getBlockPos()).inflate(0.5).expandTowards(0, 1.5, 0); }
}
