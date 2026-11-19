package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.DisconnectAnimation;
import de.artemis.cyberneticenhancements.client.JoinAnimation;
import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import de.artemis.cyberneticenhancements.client.ui.NeuralLoadingPresentation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Changes drawing only; vanilla retains ownership of reload callbacks and failures. */
@Mixin(LoadingOverlay.class)
abstract class NeuralLoadingOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;
    @Shadow @Final private boolean fadeIn;
    @Unique private long cyberneticenhancements$openedAt;
    @Unique private boolean cyberneticenhancements$worldSlideshow;
    @Unique private boolean cyberneticenhancements$drawSlideshow;
    @Shadow private void drawProgressBar(GuiGraphics graphics, int minX, int minY, int maxX, int maxY, float alpha) { throw new AssertionError(); }

    @Unique private boolean cyberneticenhancements$enabled() {
        return cyberneticenhancements$drawSlideshow
                || ((Object) this).getClass() == LoadingOverlay.class && NeuralLoadingPresentation.enabled();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void cyberneticenhancements$worldLoading(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (((Object) this).getClass() != LoadingOverlay.class) return;
        if (DisconnectAnimation.isReturningToMenu() || JoinAnimation.isWorldLoadingScreen(minecraft.screen)) {
            cyberneticenhancements$worldSlideshow = true;
        }
        cyberneticenhancements$drawSlideshow = cyberneticenhancements$worldSlideshow && TitleSlideshow.transitionReady();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
    private int cyberneticenhancements$background(java.util.function.IntSupplier original) {
        return cyberneticenhancements$enabled() ? NeuralLoadingPresentation.BACKGROUND : original.getAsInt();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"))
    private void cyberneticenhancements$veil(GuiGraphics graphics, RenderType type, int x1, int y1, int x2, int y2, int color) {
        if (!cyberneticenhancements$enabled()) graphics.fill(type, x1, y1, x2, y2, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
    private void cyberneticenhancements$logo(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                                             float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        if (!cyberneticenhancements$enabled()) graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
    private void cyberneticenhancements$bar(LoadingOverlay overlay, GuiGraphics graphics, int x1, int y1, int x2, int y2, float alpha) {
        if (!cyberneticenhancements$enabled()) drawProgressBar(graphics, x1, y1, x2, y2, alpha);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cyberneticenhancements$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!cyberneticenhancements$enabled()) return;
        long now = Util.getMillis();
        if (cyberneticenhancements$drawSlideshow) {
            // A loading screen can change while the resource reload is being finalized.
            float opacity = DisconnectAnimation.isReturningToMenu() || minecraft.screen == null || JoinAnimation.isWorldLoadingScreen(minecraft.screen) ? 1
                    : fadeOutStart < 0 ? 1 : NeuralLoadingPresentation.opacity(now, fadeOutStart, fadeInStart, fadeIn);
            graphics.flush();
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 4000);
                TitleSlideshow.renderTransition(graphics, opacity);
            } finally {
                graphics.pose().popPose();
            }
            return;
        }
        if (cyberneticenhancements$openedAt == 0) cyberneticenhancements$openedAt = now;
        NeuralLoadingPresentation.render(graphics, fadeOutStart >= 0 ? 1 : currentProgress,
                NeuralLoadingPresentation.opacity(now, fadeOutStart, fadeInStart, fadeIn), (now - cyberneticenhancements$openedAt) / 1000.0);
    }
}
