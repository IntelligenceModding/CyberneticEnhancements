package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.cyberware.CyberpsychosisClientState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
abstract class PsychosisSprintFoodMixin {
    @Inject(method="hasEnoughFoodToStartSprinting",at=@At("HEAD"),cancellable=true)
    private void psychosis$ignoreHungerGate(CallbackInfoReturnable<Boolean> ci){
        if(CyberpsychosisClientState.isControlLocked())ci.setReturnValue(true);
    }
}
