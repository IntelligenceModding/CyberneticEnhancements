package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PsychosisExhaustionMixin {
    @Inject(method="causeFoodExhaustion",at=@At("HEAD"),cancellable=true)
    private void psychosis$noDeferredExhaustion(float amount,CallbackInfo ci){
        if(CyberstrainManager.ignoresHunger((Player)(Object)this))ci.cancel();
    }
}
