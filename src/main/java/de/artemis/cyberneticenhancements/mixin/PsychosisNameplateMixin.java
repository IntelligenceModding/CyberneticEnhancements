package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
abstract class PsychosisNameplateMixin {
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void psychosis$hideIdentity(Entity entity, Component name, PoseStack pose, MultiBufferSource buffers,
                                        int light, float partialTick, CallbackInfo ci) {
        if (CyberpsychosisClientState.isControlLocked()) ci.cancel();
    }
}
