package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.client.PsychosisIntroClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
abstract class PsychosisIntroHeldItemMixin {
    @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",at=@At("HEAD"),cancellable=true)
    private void psychosis$emptyHands(PoseStack pose,MultiBufferSource buffers,int light,LivingEntity entity,float swing,float amount,float partial,float age,float yaw,float pitch,CallbackInfo ci){
        if(PsychosisIntroClient.view(entity,partial)!=null)ci.cancel();
    }
}
