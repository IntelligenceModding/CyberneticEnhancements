package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.artemis.cyberneticenhancements.common.entity.NpcCombatHealing;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class NpcMedicineLayer<T extends Mob, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {
    private final ItemInHandRenderer items;
    public NpcMedicineLayer(RenderLayerParent<T, M> parent, ItemInHandRenderer items) { super(parent); this.items = items; }

    public static void pose(PlayerModel<?> model, Mob entity) {
        var healing = NpcCombatHealing.of(entity);
        if (healing == null || !healing.usingItem()) return;
        model.leftArm.xRot = -2.1F + model.head.xRot * 0.3F;
        model.leftArm.yRot = 0.35F;
        model.leftArm.zRot = -0.1F;
        model.leftSleeve.copyFrom(model.leftArm);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float swing, float amount,
                       float partialTick, float age, float yaw, float pitch) {
        var healing = NpcCombatHealing.of(entity);
        if (entity.isInvisible() || healing == null || !healing.usingItem()) return;
        pose.pushPose();
        getParentModel().translateToHand(HumanoidArm.LEFT, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(-0.0625, 0.125, -0.625);
        items.renderItem(entity, new ItemStack(ModItems.consumable("maxdoc_mk1").get()),
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, true, pose, buffers, light);
        pose.popPose();
    }
}
