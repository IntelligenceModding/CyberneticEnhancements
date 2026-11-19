package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.client.PsychosisPresentation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderer.class)
abstract class PsychosisSilhouetteMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void psychosis$hideNames(LivingEntity entity,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        if (de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState.isControlLocked()) ci.setReturnValue(false);
    }
    @WrapMethod(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void psychosis$render(LivingEntity entity, float yaw, float partial, PoseStack pose,
            MultiBufferSource buffers, int light, Operation<Void> original) {
        boolean obscure = PsychosisPresentation.obscures(entity);
        original.call(entity, yaw, partial, pose, obscure ? PsychosisPresentation.silhouette(buffers) : buffers, light);
        if (obscure) PsychosisPresentation.face(entity, pose, buffers);
    }
}
