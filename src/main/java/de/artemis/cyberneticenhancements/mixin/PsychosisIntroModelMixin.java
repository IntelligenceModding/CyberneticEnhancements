package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.client.PsychosisIntroClient;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
abstract class PsychosisIntroModelMixin {
    @Shadow protected EntityModel<?> model;
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void psychosis$pose(LivingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){
        if(model instanceof HumanoidModel<?> humanoid)PsychosisIntroClient.pose(humanoid,entity,partial);
    }
}
