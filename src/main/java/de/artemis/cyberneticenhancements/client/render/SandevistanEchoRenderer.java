package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.client.SandevistanClientOptions;
import de.artemis.cyberneticenhancements.common.network.SandevistanSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import java.util.*;

public final class SandevistanEchoRenderer {
    private static final int MAX_ECHOES = SandevistanSnapshotPayload.MAX_USERS * 40;
    private static final int LIFETIME = 40;

    private static final int[] PALETTE = {0x76DC38, 0x42D85C, 0x28C88C, 0x24B8B0, 0x329AC8};
    private static final List<Echo> ECHOES = new ArrayList<>();
    private static PlayerModel<AbstractClientPlayer> wideModel;
    private static PlayerModel<AbstractClientPlayer> slimModel;

    private SandevistanEchoRenderer() {
    }

    public static void registerModels(EntityRenderersEvent.AddLayers event) {
        wideModel = new PlayerModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        slimModel = new PlayerModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        clear();
    }

    public static void onPlayerRendered(RenderPlayerEvent.Post event) {}

    public static void tick(Collection<SandevistanSnapshotPayload.User> users) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !SandevistanClientOptions.afterimages) {
            clear();
            return;
        }
        long tick = minecraft.level.getGameTime();
        ECHOES.removeIf(echo -> tick - echo.born >= LIFETIME);
    }

    public static void receive(de.artemis.cyberneticenhancements.common.network.SandevistanEchoPayload sample) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || wideModel == null || !SandevistanClientOptions.afterimages
                || !minecraft.level.dimension().location().equals(sample.dimension())
                || minecraft.level.getGameTime() - sample.tick() >= LIFETIME
                || !(minecraft.level.getEntity(sample.entityId()) instanceof AbstractClientPlayer player)
                || !player.getUUID().equals(sample.owner())) return;
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        PlayerModel<AbstractClientPlayer> model = slim ? slimModel : wideModel;
        for (ModelPart part : parts(model)) {
            part.resetPose();
            part.xScale = part.yScale = part.zScale = 1;
            part.visible = true;
            part.skipDraw = false;
        }
        model.young = false;
        model.riding = false;
        model.crouching = sample.crouching();
        model.attackTime = sample.attack();
        boolean rightHanded = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        model.rightArmPose = (rightHanded ? sample.mainHandItem() : sample.offHandItem())
                ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = (rightHanded ? sample.offHandItem() : sample.mainHandItem())
                ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        model.swimAmount = 0;
        model.setupAnim(player, sample.stride(), sample.speed(), sample.tick() % 24000,
                Mth.wrapDegrees(sample.headYaw() - sample.yaw()), sample.pitch());
        model.hat.visible = player.isModelPartShown(PlayerModelPart.HAT);
        model.jacket.visible = player.isModelPartShown(PlayerModelPart.JACKET);
        model.leftSleeve.visible = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        model.rightSleeve.visible = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        model.leftPants.visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        model.rightPants.visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        ECHOES.add(new Echo(sample.owner(), sample.position(), sample.yaw(), sample.scale(), sample.crouching(),
                player.getSkin().texture(), slim, false, sample.tick(), capture(model)));
        if (ECHOES.size() > MAX_ECHOES) ECHOES.removeFirst();
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || ECHOES.isEmpty() || wideModel == null
                || ShaderPackCompatibility.isRenderingShadowPass() || !SandevistanClientOptions.afterimages) return;
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double tick = minecraft.level.getGameTime() + partialTick;
        List<Echo> ordered = ECHOES.stream().sorted(Comparator.comparingDouble((Echo echo) -> echo.position.distanceToSqr(camera)).reversed()).toList();
        var buffers = minecraft.renderBuffers().bufferSource();
        Set<RenderType> usedTypes = new HashSet<>();

        PoseStack poseStack = new PoseStack();
        for (Echo echo : ordered) {

            if (minecraft.options.getCameraType().isFirstPerson()
                    && echo.owner.equals(event.getCamera().getEntity().getUUID())
                    && cameraOverlapsEcho(echo, camera)) continue;
            float remaining = (float) (1 - (tick - echo.born) / LIFETIME);
            if (remaining <= 0 || echo.position.distanceToSqr(camera) > 64 * 64) continue;
            PlayerModel<AbstractClientPlayer> model = echo.slim ? slimModel : wideModel;
            model.young = echo.young;
            ModelPart[] parts = parts(model);
            for (int index = 0; index < parts.length; index++) echo.parts[index].apply(parts[index]);
            poseStack.pushPose();
            poseStack.translate(echo.position.x - camera.x, echo.position.y - camera.y - (echo.crouching ? echo.scale * 0.125 : 0), echo.position.z - camera.z);
            poseStack.scale(echo.scale, echo.scale, echo.scale);
            poseStack.mulPose(Axis.YP.rotationDegrees(180 - echo.yaw));
            poseStack.scale(-0.9375F, -0.9375F, 0.9375F);
            poseStack.translate(0, -1.501F, 0);
            RenderType type = SandevistanRenderTypes.echo();
            usedTypes.add(type);
            int alpha = Math.round(Math.min(1.0F, remaining / 0.65F) * 90);
            float phase = (1 - remaining) * (PALETTE.length - 1);
            int band = Math.min((int) phase, PALETTE.length - 2);
            int color = blend(PALETTE[band], PALETTE[band + 1], phase - band);
            int light = LightTexture.FULL_BRIGHT;
            model.renderToBuffer(poseStack, buffers.getBuffer(type), light, OverlayTexture.NO_OVERLAY,
                    (alpha << 24) | color);
            poseStack.popPose();
        }
        usedTypes.forEach(buffers::endBatch);
    }

    private static boolean cameraOverlapsEcho(Echo echo, Vec3 camera) {

        double radius = 0.85 * echo.scale;
        double dx = camera.x - echo.position.x;
        double dz = camera.z - echo.position.z;
        double height = (echo.crouching ? 1.65 : 1.95) * echo.scale;
        return dx * dx + dz * dz < radius * radius
                && camera.y > echo.position.y - 0.25 * echo.scale
                && camera.y < echo.position.y + height + 0.25 * echo.scale;
    }

    private static int blend(int a, int b, float fraction) {
        int r = Math.round(Mth.lerp(fraction, (a >> 16) & 255, (b >> 16) & 255));
        int g = Math.round(Mth.lerp(fraction, (a >> 8) & 255, (b >> 8) & 255));
        int blue = Math.round(Mth.lerp(fraction, a & 255, b & 255));
        return r << 16 | g << 8 | blue;
    }

    private static Pose[] capture(PlayerModel<AbstractClientPlayer> model) {
        return Arrays.stream(parts(model)).map(part -> new Pose(part.storePose(), part.xScale, part.yScale, part.zScale, part.visible)).toArray(Pose[]::new);
    }

    private static ModelPart[] parts(PlayerModel<?> model) {
        return new ModelPart[]{model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg,
                model.hat, model.jacket, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants};
    }

    public static void clear() {
        ECHOES.clear();
    }

    private record Pose(PartPose pose, float xScale, float yScale, float zScale, boolean visible) {
        private void apply(ModelPart part) {
            part.loadPose(pose);
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
        }
    }
    private record Echo(UUID owner, Vec3 position, float yaw, float scale, boolean crouching, ResourceLocation skin,
                        boolean slim, boolean young, long born, Pose[] parts) {
    }
}
