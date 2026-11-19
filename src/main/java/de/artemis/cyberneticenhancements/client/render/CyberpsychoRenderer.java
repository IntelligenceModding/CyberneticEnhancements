package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoLoadout;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CyberpsychoRenderer extends HumanoidMobRenderer<CyberpsychoEntity, PlayerModel<CyberpsychoEntity>> {
    private final PlayerModel<CyberpsychoEntity> wide, slim;

    public CyberpsychoRenderer(EntityRendererProvider.Context context) {
        super(context, new CombatModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        wide = model;
        slim = new CombatModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new SkinLayer(this));
        addLayer(new CyberwareLayer(this, context.getItemInHandRenderer()));
        addLayer(new NpcMedicineLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(CyberpsychoEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model = NpcSkinModels.isSlim(getTextureLocation(entity)) ? slim : wide;
        model.setAllVisible(true);
        super.render(entity, yaw, partialTick, pose, DeploymentHologram.buffers(entity, partialTick, pose, buffers), light);
    }

    @Override
    protected RenderType getRenderType(CyberpsychoEntity entity, boolean visible, boolean translucent, boolean glowing) {
        if (visible && DeploymentHologram.active(entity)) return RenderType.entityTranslucent(NpcSkinLayers.get(getTextureLocation(entity)).opaque());
        if (visible && !translucent) return RenderType.entityCutoutNoCull(NpcSkinLayers.get(getTextureLocation(entity)).opaque());
        return super.getRenderType(entity, visible, translucent, glowing);
    }

    @Override
    protected void renderNameTag(CyberpsychoEntity entity, net.minecraft.network.chat.Component name, PoseStack pose,
                                 MultiBufferSource buffers, int light, float partialTick) {
        super.renderNameTag(entity, name, pose, buffers, light, partialTick);
        NpcHealthBarRenderer.render(entity, name, pose, buffers, entityRenderDispatcher, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(CyberpsychoEntity entity) {
        if (entity instanceof de.artemis.cyberneticenhancements.common.entity.MaxTacEntity)
            return ResourceLocation.fromNamespaceAndPath(de.artemis.cyberneticenhancements.CyberneticEnhancements.MOD_ID, "textures/entity/npc/generic/max_tac.png");
        return FixerEntityRenderer.resolveTexture(entity.getUUID(), (entity.getUUID().hashCode() & 1) == 0
                ? NpcIdentityConfig.NpcCategory.MALE : NpcIdentityConfig.NpcCategory.FEMALE);
    }

    private static final class CombatModel extends PlayerModel<CyberpsychoEntity> {
        private final ModelPart root;
        private CombatModel(ModelPart root, boolean slim) { super(root, slim); this.root = root; }

        @Override
        public void setupAnim(CyberpsychoEntity entity, float swing, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            super.setupAnim(entity, swing, amount, age, yaw, pitch);
            if (entity.charging()) {
                rightArm.xRot = -1.5F + head.xRot;
                rightArm.yRot = head.yRot;
                leftArm.xRot = entity.loadout() == CyberpsychoLoadout.LAUNCHER ? -0.8F : -1.5F;
                leftSleeve.copyFrom(leftArm);
                rightSleeve.copyFrom(rightArm);
            }
            NpcMedicineLayer.pose(this, entity);
        }
    }

    private static final class SkinLayer extends RenderLayer<CyberpsychoEntity, PlayerModel<CyberpsychoEntity>> {
        private SkinLayer(CyberpsychoRenderer renderer) { super(renderer); }
        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CyberpsychoEntity entity, float swing, float amount,
                           float partialTick, float age, float yaw, float pitch) {
            if (entity.isInvisible()) return;
            var texture = NpcSkinLayers.get(getTextureLocation(entity)).translucent();
            if (texture != null) getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)),
                    light, LivingEntityRenderer.getOverlayCoords(entity, 0), -1);
        }
    }

    private static final class CyberwareLayer extends RenderLayer<CyberpsychoEntity, PlayerModel<CyberpsychoEntity>> {
        private final ItemInHandRenderer items;
        private CyberwareLayer(CyberpsychoRenderer renderer, ItemInHandRenderer items) { super(renderer); this.items = items; }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CyberpsychoEntity entity, float swing, float amount,
                           float partialTick, float age, float yaw, float pitch) {
            if (entity.isInvisible()) return;
            var loadout = entity.loadout();
            var stack = new ItemStack(loadout == CyberpsychoLoadout.LAUNCHER ? Items.CROSSBOW
                    : loadout == CyberpsychoLoadout.GORILLA ? Items.IRON_BLOCK : Items.IRON_SWORD);
            for (var arm : HumanoidArm.values()) {
                if (entity.combatHealing().usingItem() && arm == HumanoidArm.LEFT) continue;
                if (loadout == CyberpsychoLoadout.LAUNCHER && arm == HumanoidArm.LEFT) continue;
                pose.pushPose();
                getParentModel().translateToHand(arm, pose);
                if (loadout == CyberpsychoLoadout.GORILLA) {
                    pose.translate(0, 0.65, 0);
                    pose.scale(0.27F, 0.24F, 0.27F);
                    items.renderItem(entity, stack, ItemDisplayContext.FIXED, arm == HumanoidArm.LEFT, pose, buffers, light);
                } else {
                    pose.mulPose(Axis.XP.rotationDegrees(-90));
                    pose.mulPose(Axis.YP.rotationDegrees(180));
                    pose.translate(arm == HumanoidArm.RIGHT ? 0.0625 : -0.0625, 0.125, loadout == CyberpsychoLoadout.LAUNCHER ? -0.625 : -0.35);
                    if (loadout != CyberpsychoLoadout.LAUNCHER) pose.scale(0.8F, 1.05F, 0.8F);
                    items.renderItem(entity, stack, arm == HumanoidArm.RIGHT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                            : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, arm == HumanoidArm.LEFT, pose, buffers, light);
                }
                pose.popPose();
            }
        }
    }
}
