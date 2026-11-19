package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.common.blockentity.RipperStationBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RipperStationBlockEntityRenderer implements BlockEntityRenderer<RipperStationBlockEntity> {
    private static final Map<BlockPos, Runnable> PENDING = new LinkedHashMap<>();
    private static final float PROJECTION_Y = 1.08F;
    private static final float PLAYER_RENDER_SCALE = HologramPixels.PLAYER_SCALE;
    private final ModelPart wideRoot;
    private final ModelPart slimRoot;
    private final PlayerModel<AbstractClientPlayer> wideModel;
    private final PlayerModel<AbstractClientPlayer> slimModel;

    public RipperStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        wideRoot = context.bakeLayer(ModelLayers.PLAYER);
        slimRoot = context.bakeLayer(ModelLayers.PLAYER_SLIM);
        wideModel = new PlayerModel<>(wideRoot, false);
        slimModel = new PlayerModel<>(slimRoot, true);
    }

    @Override
    public void render(RipperStationBlockEntity station, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (ShaderPackCompatibility.isRenderingShadowPass()) return;

        PoseStack captured = new PoseStack();
        captured.last().pose().set(poseStack.last().pose());
        captured.last().normal().set(poseStack.last().normal());
        PENDING.put(station.getBlockPos(), () -> renderProjection(station, partialTick, captured,
                Minecraft.getInstance().renderBuffers().bufferSource()));
    }

    public static void flushProjections() {
        if (ShaderPackCompatibility.isRenderingShadowPass()) return;
        var pending = new ArrayList<>(PENDING.values());
        PENDING.clear();
        try {
            pending.forEach(Runnable::run);
        } finally {
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        }
    }

    public static void clearPending() {
        PENDING.clear();
    }

    public static boolean isShadowPass() {
        return ShaderPackCompatibility.isRenderingShadowPass();
    }

    private void renderProjection(RipperStationBlockEntity station, float partialTick, PoseStack poseStack,
                                  MultiBufferSource buffers) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || station.isRemoved() || station.getLevel() != Minecraft.getInstance().level) {
            return;
        }

        float time = (station.getLevel().getGameTime() % 24000L) + partialTick;
        float playerScale = player.getScale();
        PlayerSkin skin = player.getSkin();
        boolean slim = skin.model() == PlayerSkin.Model.SLIM;
        ModelPart root = slim ? slimRoot : wideRoot;
        PlayerModel<AbstractClientPlayer> model = slim ? slimModel : wideModel;
        root.getAllParts().forEach(ModelPart::resetPose);
        configureModel(model, player, time);

        poseStack.pushPose();
        poseStack.translate(0.5F, PROJECTION_Y, 0.5F);
        poseStack.scale(playerScale, playerScale, playerScale);

        VertexConsumer projection = buffers.getBuffer(HologramRenderTypes.PROJECTION);
        HologramPixelGeometry.ring(poseStack, projection, 0, 7, 6, HologramPixels.PLAYER_PIXEL, 0xA05DDEFF);
        HologramPixelGeometry.ring(poseStack, projection, 0, 5, 4, HologramPixels.PLAYER_PIXEL, 0x505DDEFF);
        float scanY = HologramPixels.sweepRow(time, 32) * HologramPixels.PLAYER_PIXEL;
        HologramPixelGeometry.ring(poseStack, projection, scanY, 6, 5, HologramPixels.PLAYER_PIXEL, 0x685DDEFF);

        var projectionPose = new org.joml.Matrix4f(poseStack.last().pose());
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.45F));
        poseStack.scale(-PLAYER_RENDER_SCALE, -PLAYER_RENDER_SCALE, PLAYER_RENDER_SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        int alpha = 175;
        int tint = (alpha << 24) | 0xB4EEFF;
        VertexConsumer skinVertices = new HologramVertexConsumer(
                buffers.getBuffer(HologramRenderTypes.player(skin.texture())), projectionPose, time);
        model.renderToBuffer(poseStack, skinVertices,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
        poseStack.popPose();
    }

    private static void configureModel(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player, float time) {
        model.setAllVisible(true);
        model.young = false;
        model.riding = false;
        model.crouching = false;
        model.attackTime = 0.0F;
        model.hat.visible = player.isModelPartShown(PlayerModelPart.HAT);
        model.jacket.visible = player.isModelPartShown(PlayerModelPart.JACKET);
        model.leftPants.visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        model.rightPants.visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        model.leftSleeve.visible = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        model.rightSleeve.visible = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        float armAngle = 0.06F + Mth.sin(time * 0.05F) * 0.015F;
        model.leftArm.zRot = -armAngle;
        model.rightArm.zRot = armAngle;
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
    }


    @Override
    public AABB getRenderBoundingBox(RipperStationBlockEntity station) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        float scale = player == null ? 1.0F : player.getScale();
        var pos = station.getBlockPos();
        return new AABB(pos.getX() + 0.5F - scale * 0.75F, pos.getY(), pos.getZ() + 0.5F - scale * 0.75F,
                pos.getX() + 0.5F + scale * 0.75F, pos.getY() + PROJECTION_Y + 2.0F * scale,
                pos.getZ() + 0.5F + scale * 0.75F);
    }

    @Override
    public int getViewDistance() {
        return 32;
    }
}
