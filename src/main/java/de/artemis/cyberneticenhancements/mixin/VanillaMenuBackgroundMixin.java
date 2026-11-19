package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import net.minecraft.client.Minecraft;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class VanillaMenuBackgroundMixin {
    @WrapMethod(method = "renderWithTooltip")
    private void cyberneticenhancements$scopeMenuText(GuiGraphics g, int mouseX, int mouseY,
            float partialTick, Operation<Void> original) {
        boolean previous = VanillaMenuPresentation.beginScreenText((Screen) (Object) this);
        TitleSlideshow.beginScreenFrame();
        try { original.call(g, mouseX, mouseY, partialTick); }
        finally { VanillaMenuPresentation.endScreenText(previous); }
    }

    @Inject(method = "renderMenuBackgroundTexture", at = @At("HEAD"), cancellable = true)
    private static void cyberneticenhancements$menuPanel(GuiGraphics g, ResourceLocation texture,
            int x, int y, float u, float v, int width, int height, CallbackInfo ci) {
        var screen = Minecraft.getInstance().screen;
        if (TitleSlideshow.renderedForCurrentScreen() && screen != null && x == 0 && y == 0
                && width >= screen.width && height >= screen.height) {
            // Child menus must not tint the entire shared image a second time.
            ci.cancel();
            return;
        }
        if (!VanillaMenuPresentation.enabled()) return;
        VanillaMenuPresentation.menuPanel(g, x, y, width, height);
        ci.cancel();
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$keepSharedBackdropCrisp(float partialTick, CallbackInfo ci) {
        if (TitleSlideshow.renderedForCurrentScreen()) ci.cancel();
    }
}
