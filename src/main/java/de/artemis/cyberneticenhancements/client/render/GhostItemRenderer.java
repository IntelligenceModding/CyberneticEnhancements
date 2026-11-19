package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.cyberneticenhancements.mixin.CorpseRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Recipe hints use translucent item geometry rather than an opaque item with a dark overlay. */
public final class GhostItemRenderer {
    private static final float OPACITY = .25F;

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        var minecraft = Minecraft.getInstance();
        var renderer = minecraft.getItemRenderer();
        var preview = stack.copy();
        preview.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        var model = renderer.getModel(preview, minecraft.level, null, 0);
        graphics.flush();
        graphics.pose().pushPose();
        boolean flatLighting = !model.usesBlockLight();
        try {
            graphics.pose().translate(x + 8, y + 8, 150);
            graphics.pose().scale(16, -16, 16);
            if (flatLighting) Lighting.setupForFlatItems();
            MultiBufferSource translucent = type -> {
                if (type instanceof CorpseRenderTypes.CompositeAccess composite) {
                    var state = (CorpseRenderTypes.StateAccess) (Object) composite.cyberneticenhancements$state();
                    var texture = ((CorpseRenderTypes.TextureAccess) state.cyberneticenhancements$texture()).cyberneticenhancements$location();
                    if (texture.isPresent()) type = RenderType.entityTranslucent(texture.get());
                }
                return new GhostPixels(graphics.bufferSource().getBuffer(type));
            };
            renderer.render(preview, ItemDisplayContext.GUI, false, graphics.pose(), translucent,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            graphics.flush();
        } finally {
            if (flatLighting) Lighting.setupFor3DItems();
            graphics.pose().popPose();
        }
    }

    private record GhostPixels(VertexConsumer target) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { target.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { target.setColor(r, g, b, Math.round(a * OPACITY)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { target.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { target.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { target.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { target.setNormal(x, y, z); return this; }
    }

    private GhostItemRenderer() {}
}
