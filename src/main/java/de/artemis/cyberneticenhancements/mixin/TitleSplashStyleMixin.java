package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SplashRenderer.class)
abstract class TitleSplashStyleMixin {
    @ModifyConstant(method = "render", constant = @Constant(intValue = 16776960))
    private int cyberneticenhancements$splashTint(int original) {
        return TitleScreenPresentation.enabled(Minecraft.getInstance().screen) ? 0xFF8C9B : original;
    }
}
