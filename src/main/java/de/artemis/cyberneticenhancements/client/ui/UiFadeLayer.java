package de.artemis.cyberneticenhancements.client.ui;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL30;

public final class UiFadeLayer implements AutoCloseable {
    private TextureTarget target;

    public void prepare() {
        var main = Minecraft.getInstance().getMainRenderTarget();
        if (target == null || target.width != main.width || target.height != main.height) {
            close();
            target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            main.bindWrite(true);
        }
    }

    public void capture() {
        RenderSystem.assertOnRenderThread();
        prepare();
        var main = Minecraft.getInstance().getMainRenderTarget();
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, target.width, target.height,
                    GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
            RenderSystem.colorMask(false, false, false, true);
            GL30.glClearBufferfv(GL30.GL_COLOR, 0, new float[]{0, 0, 0, 1});
        } finally {
            RenderSystem.colorMask(true, true, true, true);
            main.bindWrite(true);
        }
    }

    public void drawCaptured(GuiGraphics graphics, float opacity) {
        if (target == null) return;
        graphics.flush();
        composite(graphics, opacity, 1, 0, graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F);
    }

    public void render(GuiGraphics graphics, float opacity, float scale, float offsetY, Runnable content) {
        render(graphics, opacity, scale, offsetY, graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F, content);
    }

    public void render(GuiGraphics graphics, float opacity, float scale, float offsetY, float centerX, float centerY, Runnable content) {
        var main = Minecraft.getInstance().getMainRenderTarget();
        graphics.flush();
        prepare();
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        try {
            content.run();
        } finally {
            graphics.flush();
            main.bindWrite(true);
        }

        composite(graphics, opacity, scale, offsetY, centerX, centerY);
    }

    private void composite(GuiGraphics graphics, float opacity, float scale, float offsetY, float centerX, float centerY) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY + offsetY, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(-centerX, -centerY, 0);
        var matrix = graphics.pose().last().pose();
        int alpha = Math.round(Math.clamp(opacity, 0.0F, 1.0F) * 255);
        var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        vertices.addVertex(matrix, 0, 0, 0).setUv(0, 1).setColor(alpha, alpha, alpha, alpha);
        vertices.addVertex(matrix, 0, graphics.guiHeight(), 0).setUv(0, 0).setColor(alpha, alpha, alpha, alpha);
        vertices.addVertex(matrix, graphics.guiWidth(), graphics.guiHeight(), 0).setUv(1, 0).setColor(alpha, alpha, alpha, alpha);
        vertices.addVertex(matrix, graphics.guiWidth(), 0, 0).setUv(1, 1).setColor(alpha, alpha, alpha, alpha);
        BufferUploader.drawWithShader(vertices.buildOrThrow());
        graphics.pose().popPose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    @Override
    public void close() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }
}
