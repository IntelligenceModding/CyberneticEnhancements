package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

final class DropoffMarkerDepth {
    private static int texture = -1;
    private static int width, height;
    private static boolean ready;
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f MODEL_VIEW = new Matrix4f();

    private DropoffMarkerDepth() {
    }

    static void capture() {
        ready = false;
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        if (viewport[2] <= 0 || viewport[3] <= 0) return;
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, drawFramebuffer);
            if (texture == -1) texture = TextureUtil.generateTextureId();
            GlStateManager._bindTexture(texture);
            if (width != viewport[2] || height != viewport[3]) {
                GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
                GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
                GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F,
                        viewport[0], viewport[1], viewport[2], viewport[3], 0);
                width = viewport[2];
                height = viewport[3];
            } else {
                GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, viewport[0], viewport[1], width, height);
            }
            PROJECTION.set(RenderSystem.getProjectionMatrix());
            MODEL_VIEW.set(RenderSystem.getModelViewMatrix());
            ready = true;
        } finally {
            GlStateManager._activeTexture(activeTexture);
            GlStateManager._bindTexture(binding);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        }
    }

    static void render(Runnable draw) {
        if (!ready) return;
        ready = false;
        var main = Minecraft.getInstance().getMainRenderTarget();
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            main.bindWrite(true);
            RenderSystem.setProjectionMatrix(PROJECTION, sorting);
            modelView.set(MODEL_VIEW);
            RenderSystem.applyModelViewMatrix();
            HologramRenderTypes.prepareDeliveryMarker(texture, main.viewWidth, main.viewHeight);
            draw.run();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }

    static void invalidate() {
        ready = false;
    }

    static void close() {
        ready = false;
        if (texture != -1) TextureUtil.releaseTextureId(texture);
        texture = -1;
        width = height = 0;
    }
}
