package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.PsychosisIntroClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(PlayerRenderer.class)
abstract class PsychosisIntroBodyYawMixin {
    @ModifyVariable(method="setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private float psychosis$bodyYaw(float yaw,AbstractClientPlayer player,PoseStack pose,float age,float originalYaw,float partial,float scale){
        var view=PsychosisIntroClient.view(player,partial);return view==null?yaw:view.baseYaw();
    }
}
