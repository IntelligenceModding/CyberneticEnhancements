package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.*;

public final class SchematicIconRenderer {
    public static void drawWithShadow(GuiGraphics graphics, ItemStack stack, int x, int y, boolean known) {

        graphics.fill(x + 3, y + 15, x + 13, y + 16, 0x18000000);
        graphics.fill(x + 1, y + 16, x + 15, y + 18, 0x28000000);
        graphics.fill(x + 3, y + 18, x + 13, y + 19, 0x14000000);
        draw(graphics, stack, x, y, known);
    }

    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y, boolean known) {
        if (known) {
            graphics.renderItem(stack, x, y);
            return;
        }
        var mc = Minecraft.getInstance();
        graphics.flush();
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 8, y + 8, 150);
        pose.scale(16, -16, 16);
        var buffers = mc.renderBuffers().bufferSource();
        MultiBufferSource black = type -> new BlackConsumer(buffers.getBuffer(type));
        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, 15728880, 0, pose, black, mc.level, 0);
        buffers.endBatch();
        pose.popPose();
    }

    private record BlackConsumer(VertexConsumer delegate) implements VertexConsumer {
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor(0, 0, 0, a);
            return this;
        }

        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
