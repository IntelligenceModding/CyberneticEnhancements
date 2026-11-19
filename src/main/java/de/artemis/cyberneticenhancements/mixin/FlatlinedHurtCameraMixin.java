package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.cyberneticenhancements.client.FlatlinedDeathCamera;
import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class FlatlinedHurtCameraMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberneticenhancements$hideDeathHands(CallbackInfo ci) {
        if (FlatlinedPresentation.hidesGameplayUi()) ci.cancel();
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberneticenhancements$deathMotion(PoseStack pose, float partialTick, CallbackInfo ci) {
        if (FlatlinedDeathCamera.active(Minecraft.getInstance().getCameraEntity())) ci.cancel();
    }
}
