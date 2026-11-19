package de.artemis.cyberneticenhancements.client.render;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;

public final class FixerEntityRenderer extends HumanoidMobRenderer<AbstractCityNpcEntity, PlayerModel<AbstractCityNpcEntity>> {
    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private static final NpcTextureCatalog<ResourceLocation> TEXTURES = new NpcTextureCatalog<>();

    private final PlayerModel<AbstractCityNpcEntity> wideModel;
    private final PlayerModel<AbstractCityNpcEntity> slimModel;

    public FixerEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new CallPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new CallPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.model.setAllVisible(true);
        addLayer(new TranslucentSkinLayer(this));
        addLayer(new PhoneLayer(this, context.getItemInHandRenderer()));
        addLayer(new NpcMedicineLayer<>(this, context.getItemInHandRenderer()));
    }

    private static final class CallPlayerModel extends PlayerModel<AbstractCityNpcEntity> {
        private final net.minecraft.client.model.geom.ModelPart root;
        private CallPlayerModel(net.minecraft.client.model.geom.ModelPart root, boolean slim) {
            super(root, slim);
            this.root = root;
        }

        @Override
        public void setupAnim(AbstractCityNpcEntity entity, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(net.minecraft.client.model.geom.ModelPart::resetPose);
            if (entity instanceof de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity encounter && encounter.isSitting()) riding = true;
            super.setupAnim(entity, limbSwing, limbSwingAmount, age, yaw, pitch);
            var motion = NpcConversationPoses.pose(entity, age - entity.tickCount);
            if (motion != null) {
                head.yRot += motion.headYaw();
                head.xRot += motion.headPitch();
                head.zRot += motion.headRoll();
                body.xRot += motion.bodyPitch();
                body.zRot += motion.bodyRoll();
                leftArm.xRot += motion.leftPitch();
                leftArm.zRot += motion.leftRoll();
                rightArm.xRot += motion.rightPitch();
                rightArm.zRot += motion.rightRoll();
                hat.copyFrom(head);
                jacket.copyFrom(body);
                leftSleeve.copyFrom(leftArm);
                rightSleeve.copyFrom(rightArm);
            }
            NpcMedicineLayer.pose(this, entity);
            if (entity.isOnCall()) {
                rightArm.xRot = -2.1F;
                rightArm.yRot = -0.15F;
                rightArm.zRot = 0.2F;
                rightSleeve.copyFrom(rightArm);
            }
        }
    }

    private static final class PhoneLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<AbstractCityNpcEntity, PlayerModel<AbstractCityNpcEntity>> {
        private final net.minecraft.client.renderer.ItemInHandRenderer items;

        private PhoneLayer(FixerEntityRenderer renderer, net.minecraft.client.renderer.ItemInHandRenderer items) {
            super(renderer);
            this.items = items;
        }

        @Override
        public void render(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                           AbstractCityNpcEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
            if (!entity.isOnCall() || entity.isInvisible()) return;
            pose.pushPose();
            getParentModel().translateToHand(net.minecraft.world.entity.HumanoidArm.RIGHT, pose);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            pose.translate(0.0625F, 0.125F, -0.625F);
            items.renderItem(entity, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_INGOT),
                    net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, buffers, light);
            pose.popPose();
        }
    }

    @Override
    public void render(AbstractCityNpcEntity entity, float entityYaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        if (!entity.hasResolvedIdentity()) {
            return;
        }
        model = NpcSkinModels.isSlim(getTextureLocation(entity)) ? slimModel : wideModel;
        model.setAllVisible(true);
        super.render(entity, entityYaw, partialTick, poseStack, DeploymentHologram.buffers(entity, partialTick, poseStack, buffer), packedLight);
        if (entity instanceof de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity encounter && encounter.offersQuest()
                && !"red".equals(encounter.nameColorId())) renderQuestMark(encounter, partialTick, poseStack, buffer);
    }

    /** The floating "!" over someone who has a quest to give; it drifts gently and turns to face the camera. */
    private void renderQuestMark(AbstractCityNpcEntity entity, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buffer) {
        var mark = net.minecraft.network.chat.Component.literal("!").withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.GOLD);
        pose.pushPose();
        pose.translate(0, entity.getBbHeight() + 0.95 + Math.sin((entity.tickCount + partialTick) * 0.12) * 0.05, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(-0.07F, -0.07F, 0.07F);
        getFont().drawInBatch(mark, -getFont().width(mark) / 2F, 0, 0xFFFFFFFF, false, pose.last().pose(), buffer,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        pose.popPose();
    }

    @Override
    protected net.minecraft.client.renderer.RenderType getRenderType(AbstractCityNpcEntity entity, boolean visible, boolean translucent, boolean glowing) {
        if (visible && DeploymentHologram.active(entity))
            return net.minecraft.client.renderer.RenderType.entityTranslucent(NpcSkinLayers.get(getTextureLocation(entity)).opaque());
        if (visible && !translucent) {
            return net.minecraft.client.renderer.RenderType.entityCutoutNoCull(NpcSkinLayers.get(getTextureLocation(entity)).opaque());
        }
        return super.getRenderType(entity, visible, translucent, glowing);
    }

    /** Someone sitting has their hips on the seat rather than their feet, so the whole body sits lower. */
    @Override
    protected void scale(AbstractCityNpcEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        if (entity instanceof de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity encounter && encounter.isSitting())
            pose.translate(0, 0.5F, 0); // The model is drawn upside down at this point, so this moves it down.
    }

    @Override
    protected void renderNameTag(AbstractCityNpcEntity entity, net.minecraft.network.chat.Component name, com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buffers, int light, float partialTick) {
        boolean defending = "red".equals(entity.nameColorId()) || entity.combatHealing().highlighted()
                || NpcHealingColors.amount(entity, partialTick) > 0;
        pose.pushPose();
        try {
            if (defending) pose.translate(0, 0.175, 0);
            super.renderNameTag(entity, name, pose, buffers, light, partialTick);
            if (defending) NpcHealthBarRenderer.render(entity, name, pose, buffers, entityRenderDispatcher, partialTick);
        } finally {
            pose.popPose();
        }
    }

    private static final class TranslucentSkinLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<AbstractCityNpcEntity, PlayerModel<AbstractCityNpcEntity>> {
        private TranslucentSkinLayer(FixerEntityRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                int light, AbstractCityNpcEntity entity, float limbSwing, float limbSwingAmount, float partialTick,
                float ageInTicks, float headYaw, float headPitch) {
            if (entity.isInvisible()) return;
            ResourceLocation texture = NpcSkinLayers.get(getTextureLocation(entity)).translucent();
            if (texture == null) return;
            getParentModel().renderToBuffer(pose, buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(texture)),
                    light, net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(entity, 0), -1);
        }
    }
    @Override
    public ResourceLocation getTextureLocation(AbstractCityNpcEntity entity) {
        if (entity instanceof de.artemis.cyberneticenhancements.common.entity.MaxTecGuardEntity)
            return ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "textures/entity/npc/generic/max_tac.png");
        if (entity.npcType() == de.artemis.cyberneticenhancements.common.entity.NpcType.MED_TECH
                || entity.npcType() == de.artemis.cyberneticenhancements.common.entity.NpcType.WORLD_MED_TECH
                && !(entity instanceof de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity encounter
                && encounter.role() == de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity.Role.PATIENT)) return ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "textures/entity/npc/generic/trauma_team.png"); if (!entity.appearanceOverride().isBlank()) {
            ResourceLocation override = ResourceLocation.tryParse(entity.appearanceOverride());
            if (override != null && !de.artemis.cyberneticenhancements.common.world.NpcAppearanceRules.reserved(override.toString())
                    && Minecraft.getInstance().getResourceManager().getResource(override).isPresent()) {
                return override;
            }
        }
        return resolveTexture(entity.getUUID(), entity.npcCategory());
    }

    public static ResourceLocation resolveTexture(java.util.UUID entityId, NpcCategory category) {
        List<ResourceLocation> textures = texturesFor(category);
        if (textures.isEmpty()) {
            return FALLBACK_TEXTURE;
        }
        int index = Math.floorMod(entityId.hashCode(), textures.size());
        return textures.get(index);
    }

    private static List<ResourceLocation> texturesFor(NpcCategory category) {
        List<ResourceLocation> categoryTextures = listTextures(NpcIdentityConfig.textureFolder(category));
        if (!categoryTextures.isEmpty()) {
            return categoryTextures;
        }

        List<ResourceLocation> genericTextures = listTextures(NpcIdentityConfig.textureFolder(NpcCategory.GENERIC));
        if (!genericTextures.isEmpty()) {
            return genericTextures;
        }

        return listTextures("textures/entity/npc");
    }

    public static List<ResourceLocation> availableTextures() {
        return listTextures("textures/entity/npc");
    }

    public static List<ResourceLocation> availableTextures(NpcCategory category) {
        return texturesFor(category);
    }

    static void clearTextureCache() {
        TEXTURES.clear();
    }

    private static List<ResourceLocation> listTextures(String prefix) {
        return TEXTURES.get(prefix, () -> discoverTextures(prefix));
    }

    private static List<ResourceLocation> discoverTextures(String prefix) {
        return Minecraft.getInstance().getResourceManager()
                .listResources(prefix, location ->
                        location.getNamespace().equals(CyberneticEnhancements.MOD_ID)
                                && !de.artemis.cyberneticenhancements.common.world.NpcAppearanceRules.reserved(location.toString())
                                && location.getPath().endsWith(".png"))
                .keySet()
                .stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }
}
