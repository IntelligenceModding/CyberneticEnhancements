package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.common.tarot.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class TarotCardRenderer extends EntityRenderer<TarotCardEntity> {
    public TarotCardRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(TarotCardEntity entity) { return TarotCatalog.find(entity.cardId()).texture(); }
    @Override public void render(TarotCardEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (ShaderPackCompatibility.isRenderingShadowPass()) return;
        projection(TarotCatalog.find(entity.cardId()), entity.facing(), entity.tickCount + partial, pose, buffers, 0xFFFFFF, 1, entity.collectionProgress(partial));
    }
    public static RenderType preview(String id, net.minecraft.core.Direction facing, boolean valid, float time, PoseStack pose, MultiBufferSource buffers) {
        if (ShaderPackCompatibility.isRenderingShadowPass() || TarotCatalog.find(id) == null) return null;
        RenderType type = projection(TarotCatalog.find(id), facing, time, pose, buffers, valid ? 0x99FFBB : 0xFF7777, .65F, 0);
        pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), -1, 0, -.025, 1, 3, .025,
                valid ? .4F : 1, valid ? 1 : .3F, valid ? .65F : .3F, .8F);
        pose.popPose(); return type;
    }
    private static RenderType projection(TarotCatalog.Card card, net.minecraft.core.Direction facing, float age, PoseStack pose, MultiBufferSource buffers, int tint, float opacity, float dissolve) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        RenderType type = EmissiveProjectionRenderTypes.translucent(card.texture());
        VertexConsumer pixels = buffers.getBuffer(type);
        float motion = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float time = age * motion;
        float flicker = .88F + .06F * (float) Math.sin(time * .47F);
        int rows = Math.round(TarotCardEntity.HEIGHT * HologramPixels.PER_BLOCK);
        int sweepRow = HologramPixels.sweepRow(time, rows);
        for (int stripe = 0; stripe < rows; stripe++) {
            float v0 = stripe / (float) rows, v1 = (stripe + 1F) / rows;
            float bottom = TarotCardEntity.HEIGHT * (1 - v1), top = TarotCardEntity.HEIGHT * (1 - v0);
            float scan = .7F + .3F * HologramPixels.stripe(stripe, time);
            // Erase from the top down, following the particle sweep on the wall.
            float remaining = stripe < (int) Math.floor(dissolve * rows) ? 0 : 1;
            int alpha = (int) (215 * flicker * (1 - motion + motion * scan) * opacity * remaining * (1 - dissolve * .35F));
            if (alpha <= 0) continue;
            int red = stripe == sweepRow ? 220 : 180, green = stripe == sweepRow ? 255 : 225;
            // Quantize the effect bands, while mapping the complete artwork continuously.
            quad(pixels, pose.last(), -TarotCardEntity.WIDTH / 2, bottom, TarotCardEntity.WIDTH / 2, top, v0, v1,
                    red * (tint >> 16 & 255) / 255, green * (tint >> 8 & 255) / 255, tint & 255, alpha);
        }
        pose.popPose(); return type;
    }
    private static void quad(VertexConsumer v, PoseStack.Pose pose, float left, float bottom, float right, float top, float v0, float v1, int r, int g, int b, int a) {
        vertex(v, pose, left, bottom, 0, v1, r,g,b,a); vertex(v, pose, right, bottom, 1, v1,r,g,b,a);
        vertex(v, pose, right, top, 1, v0,r,g,b,a); vertex(v, pose, left, top, 0, v0,r,g,b,a);
    }
    private static void vertex(VertexConsumer v, PoseStack.Pose pose, float x, float y, float u, float vv, int r, int g, int b, int a) {
        v.addVertex(pose, x, y, 0).setColor(r,g,b,a).setUv(u,vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose,0,0,1);
    }
}
