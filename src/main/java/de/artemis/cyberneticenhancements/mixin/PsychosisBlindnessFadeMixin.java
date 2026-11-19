package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.PsychosisBlindnessVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="net.minecraft.client.renderer.FogRenderer$BlindnessFogFunction")
abstract class PsychosisBlindnessFadeMixin {
    @ModifyArg(method="setupFog",at=@At(value="INVOKE",target="Lnet/minecraft/util/Mth;lerp(FFF)F"),index=0)
    private float psychosis$continuousBlindness(float vanilla){return PsychosisBlindnessVisual.strength(vanilla);}
}
