package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

public final class SchematicIconRenderer {
    private static ShaderInstance shadowShader;

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath("cyberneticenhancements", "gui_item_shadow"),
                            DefaultVertexFormat.NEW_ENTITY), shader -> shadowShader = shader);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load the GUI item shadow shader", exception);
        }
    }

    public static void drawWithShadow(GuiGraphics graphics, ItemStack stack, int x, int y, boolean known) {
        if (stack.isEmpty()) return;
        ItemStack shadow = stack.copy();
        shadow.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        drawSilhouette(graphics, shadow, x + UiRenderer.SHADOW_OFFSET_X * 0.5F, y + UiRenderer.SHADOW_OFFSET_Y * 0.5F,
                255, true);
        draw(graphics, stack, x, y, known);
    }

    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y, boolean known) {
        if (known) {
            graphics.renderItem(stack, x, y);
            return;
        }
        drawSilhouette(graphics, stack, x, y, 255, false);
    }

    private static void drawSilhouette(GuiGraphics graphics, ItemStack stack, float x, float y, int opacity, boolean shadow) {
        var mc = Minecraft.getInstance();
        graphics.flush();
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 8, y + 8, shadow ? 120 : 150);
        pose.scale(16, -16, 16);
        var buffers = mc.renderBuffers().bufferSource();
        MultiBufferSource black = type -> new BlackConsumer(buffers.getBuffer(shadow ? ShadowRenderType.of(type) : type), opacity);
        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, 15728880, OverlayTexture.NO_OVERLAY, pose, black, mc.level, 0);
        buffers.endBatch();
        pose.popPose();
    }

    private static final class ShadowRenderType extends RenderType {
        private static final Function<RenderType, RenderType> TYPES = Util.memoize(original -> new ShadowRenderType(original));

        private ShadowRenderType(RenderType original) {
            this(original, new float[4], new ShaderInstance[1]);
        }

        private ShadowRenderType(RenderType original, float[] previousColor, ShaderInstance[] previousShader) {
            super("cyberneticenhancements_item_shadow", original.format(), original.mode(), original.bufferSize(),
                    false, true, () -> {
                        System.arraycopy(RenderSystem.getShaderColor(), 0, previousColor, 0, 4);
                        previousShader[0] = RenderSystem.getShader();
                        original.setupRenderState();
                        RenderSystem.setShader(() -> shadowShader);
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                        RenderSystem.enableCull();
                        RenderSystem.depthMask(false);
                        RenderSystem.setShaderColor(0, 0, 0, previousColor[3] * (UiRenderer.SHADOW >>> 24) / 382.5F);
                    }, () -> {
                        original.clearRenderState();
                        RenderSystem.depthMask(true);
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableBlend();
                        RenderSystem.setShader(() -> previousShader[0]);
                        RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
                    });
        }

        private static RenderType of(RenderType original) {
            return TYPES.apply(original);
        }
    }

    private record BlackConsumer(VertexConsumer delegate, int opacity) implements VertexConsumer {
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor(0, 0, 0, a * opacity / 255);
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
