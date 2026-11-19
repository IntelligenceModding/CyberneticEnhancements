package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.client.render.DeathBodies;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class DeathBodyModelMixin {
    @Shadow protected EntityModel<?> model;

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void cyberneticenhancements$naturalSkin(LivingEntity entity, float whiteOverlay, CallbackInfoReturnable<Integer> ci) {
        if (DeathBodies.rendering(entity)) ci.setReturnValue(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$backwardFall(LivingEntity entity, PoseStack pose, float age, float yaw, float partial, float scale, CallbackInfo ci) {
        if (!DeathBodies.rendering(entity)) return;
        DeathBodies.rotations(entity, pose, partial);
        ci.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", shift = At.Shift.AFTER))
    private void cyberneticenhancements$limpPose(LivingEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        DeathBodies.poseModel(model, entity);
    }
}
