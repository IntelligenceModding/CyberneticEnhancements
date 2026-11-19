package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.client.render.PlayerDeploymentRender;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerRenderer.class)
abstract class PlayerDeploymentRenderMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), argsOnly = true)
    private MultiBufferSource cyberneticenhancements$arrivalBuffers(MultiBufferSource buffers, AbstractClientPlayer player,
                                                                    float yaw, float partialTick, PoseStack pose) {
        return PlayerDeploymentRender.arrivalBuffers(player, partialTick, pose, buffers);
    }
}
