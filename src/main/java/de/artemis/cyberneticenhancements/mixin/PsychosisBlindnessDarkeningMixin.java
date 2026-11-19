package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.PsychosisBlindnessVisual;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="net.minecraft.client.renderer.FogRenderer$MobEffectFogFunction")
interface PsychosisBlindnessDarkeningMixin {
    @Shadow Holder<MobEffect> getMobEffect();
    @Inject(method="getModifiedVoidDarkness",at=@At("HEAD"),cancellable=true)
    private void psychosis$continuousDarkening(LivingEntity entity,MobEffectInstance effect,float original,float partial,CallbackInfoReturnable<Float> ci){
        if(getMobEffect()==MobEffects.BLINDNESS&&PsychosisBlindnessVisual.active())
            ci.setReturnValue(original*(1-PsychosisBlindnessVisual.strength(1)));
    }
}
