package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class PsychosisMouseMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void psychosis$lockLook(CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (CyberpsychosisClientState.isControlLocked() && mc.player != null && mc.screen == null) {
            accumulatedDX = accumulatedDY = 0;
            ci.cancel();
        }
    }
}
