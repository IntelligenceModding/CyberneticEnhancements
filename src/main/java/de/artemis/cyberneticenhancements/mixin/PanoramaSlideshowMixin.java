package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PanoramaRenderer.class)
abstract class PanoramaSlideshowMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$slideshow(GuiGraphics graphics, int width, int height,
                                                 float fade, float partialTick, CallbackInfo ci) {
        // Replace the shared Minecraft panorama wherever it is drawn, while leaving
        // independently constructed panorama renderers supplied by other mods alone.
        if ((Object) this == ScreenPanoramaAccess.cyberneticenhancements$panorama()
                && TitleSlideshow.render(graphics, fade)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cyberneticenhancements$panoramaEffects(GuiGraphics graphics, int width, int height,
                                                       float fade, float partialTick, CallbackInfo ci) {
        if ((Object) this == ScreenPanoramaAccess.cyberneticenhancements$panorama()
                && !TitleSlideshow.renderedForCurrentScreen())
            de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation.panoramaRendered(graphics, fade);
    }
}
