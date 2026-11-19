package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.PsychosisBlindnessVisual;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Iris 1.21.1 blindness uniform; no Iris classes are linked when it is absent. */
@Pseudo
@Mixin(targets="net.irisshaders.iris.uniforms.CommonUniforms",remap=false)
abstract class PsychosisIrisBlindnessMixin {
    @Inject(method="getBlindness",at=@At("RETURN"),cancellable=true,require=0)
    private static void psychosis$continuousShaderBlindness(CallbackInfoReturnable<Float> ci){
        if(PsychosisBlindnessVisual.active())ci.setReturnValue(PsychosisBlindnessVisual.strength(ci.getReturnValue()));
    }
}
