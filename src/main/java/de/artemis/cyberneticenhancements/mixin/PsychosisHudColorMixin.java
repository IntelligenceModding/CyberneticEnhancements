package de.artemis.cyberneticenhancements.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.client.ui.PsychosisHudFade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderSystem.class)
abstract class PsychosisHudColorMixin {
    @ModifyVariable(method = "setShaderColor", at = @At("HEAD"), argsOnly = true, ordinal = 3, remap = false)
    private static float psychosis$hudAlpha(float alpha) { return PsychosisHudFade.alpha(alpha); }
}
