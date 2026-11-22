package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.util.Mth;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.cyberneticenhancements.common.entity.HangingWireEntity;
import de.artemis.cyberneticenhancements.client.HangingSignClient;
import de.artemis.cyberneticenhancements.common.entity.WireSigns;
import static de.artemis.cyberneticenhancements.client.render.WireSway.*;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Draws a hanging wire as a thin square rod of vanilla cauldron iron following its sag, lit by the blocks it passes. */
public final class HangingWireRenderer extends EntityRenderer<HangingWireEntity> {
    private static final ResourceLocation COPPER = ResourceLocation.withDefaultNamespace("block/cauldron_inner");
    private static final double RADIUS = 0.035;

    private final ModelPart flag;

    public HangingWireRenderer(EntityRendererProvider.Context context) {
        super(context);
        flag = context.bakeLayer(ModelLayers.BANNER).getChild("flag");
        shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(HangingWireEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }

    @Override
    public boolean shouldRender(HangingWireEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBoxForCulling());
    }

    @Override
    public void render(HangingWireEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 start = entity.start(), end = entity.end();
        float time = WireSway.time(entity, partialTick);
        double wind = wind(entity, partialTick);
        double length = start.distanceTo(end);
        int segments = (int) Math.clamp(Math.ceil(length * 3), 6, 96);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(COPPER);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        Vec3 origin = entity.position();
        Vec3[][] rings = new Vec3[segments + 1][];
        Vec3[][] normals = new Vec3[segments + 1][];
        int[] lights = new int[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 at = swayed(start, end, t, time, wind);
            Vec3 tangent = swayed(start, end, Math.min(1, t + 0.01), time, wind).subtract(swayed(start, end, Math.max(0, t - 0.01), time, wind)).normalize();
            Vec3 a = Math.abs(tangent.y) > 0.99 ? new Vec3(1, 0, 0) : tangent.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 b = a.cross(tangent).normalize();
            Vec3 relative = at.subtract(origin);
            normals[i] = new Vec3[]{a.add(b).normalize(), a.scale(-1).add(b).normalize(), a.scale(-1).add(b.scale(-1)).normalize(), a.add(b.scale(-1)).normalize()};
            rings[i] = new Vec3[4];
            for (int corner = 0; corner < 4; corner++) rings[i][corner] = relative.add(normals[i][corner].scale(RADIUS * Math.sqrt(2)));
            lights[i] = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(at));
        }
        decorations(entity, start, end, origin, pose, buffers, time, wind);
        PoseStack.Pose last = pose.last();
        float du = sprite.getU1() - sprite.getU0(), dv = sprite.getV1() - sprite.getV0();
        float u0 = sprite.getU0() + du * 0.375F, u1 = sprite.getU0() + du * 0.625F;
        for (int i = 0; i < segments; i++) {
            float v0 = sprite.getV0() + dv * (i % 4) / 4F, v1 = sprite.getV0() + dv * (i % 4 + 1) / 4F;
            for (int corner = 0; corner < 4; corner++) {
                int next = (corner + 1) % 4;
                Vec3 normal = normals[i][corner].add(normals[i][next]).normalize();
                vertex(consumer, last, rings[i][corner], u0, v0, lights[i], normal);
                vertex(consumer, last, rings[i][next], u1, v0, lights[i], normal);
                vertex(consumer, last, rings[i + 1][next], u1, v1, lights[i + 1], normal);
                vertex(consumer, last, rings[i + 1][corner], u0, v1, lights[i + 1], normal);
            }
        }
    }

    /** The stand-in sign block entities of the signs on wires, kept for as long as the hung sign is. */
    private final Map<HangingWireEntity.Decoration, HangingSignClient.WireSign> signs = new WeakHashMap<>();

    private void decorations(HangingWireEntity entity, Vec3 start, Vec3 end, Vec3 origin, PoseStack pose, MultiBufferSource buffers, float time, double wind) {
        for (HangingWireEntity.Decoration decoration : entity.decorations()) {
            Vec3 at = swayed(start, end, decoration.t(), time, wind);
            boolean banner = decoration.stack().is(ItemTags.BANNERS);
            pose.pushPose();
            pose.translate(at.x - origin.x, at.y - origin.y, at.z - origin.z);
            // Turn so the item's width runs along the wire, then swing it across the wire about the point it hangs from.
            pose.mulPose(Axis.YP.rotation(yaw(start, end)));
            pose.mulPose(Axis.XP.rotationDegrees(swing(start, end, time, wind)));
            int light = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(at.add(0, banner ? -0.4 : -0.55, 0)));
            if (banner && decoration.stack().getItem() instanceof BannerItem item) {
                // Only the cloth, hanging from the wire: the pole and crossbar of the standing banner are left out.
                pose.translate(0, 0, -0.047);
                pose.scale(0.5F, -0.5F, -0.5F);
                flag.xRot = (-0.0125F + 0.01F * Mth.cos(time * 0.08F)) * (float) Math.PI;
                BannerRenderer.renderPatterns(pose, buffers, light, OverlayTexture.NO_OVERLAY, flag, ModelBakery.BANNER_BASE, true, item.getColor(),
                        decoration.stack().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY));
                pose.popPose();
                continue;
            }
            if (WireSigns.isSign(decoration.stack())) {
                // Drawn by the sign's own renderer, its chains meeting on the wire, and its text on both sides.
                var sign = signs.computeIfAbsent(decoration, d -> HangingSignClient.WireSign.of(d.stack(), entity.registryAccess(), BlockPos.containing(at)));
                pose.translate(-0.5, -1.0, -0.5);
                Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(sign, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            } else if (decoration.stack().getItem() instanceof BlockItem item && item.getBlock() instanceof LanternBlock lantern) {
                // The placed block's own model, hanging by its ring on the wire (the ring sits 13/16 up the block).
                pose.translate(-0.5, -0.84, -0.5);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(lantern.defaultBlockState().setValue(LanternBlock.HANGING, true),
                        pose, buffers, light, OverlayTexture.NO_OVERLAY);
            } else {
                pose.translate(0, -0.3, 0);
                pose.scale(2F, 2F, 2F);
                Minecraft.getInstance().getItemRenderer().renderStatic(decoration.stack(), ItemDisplayContext.FIXED,
                        light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
            }
            pose.popPose();
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, float u, float v, int light, Vec3 normal) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
