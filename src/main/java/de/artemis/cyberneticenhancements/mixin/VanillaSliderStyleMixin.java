package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
abstract class VanillaSliderStyleMixin extends AbstractWidget {
    @Shadow protected double value;

    protected VanillaSliderStyleMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$slider(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
        if (!VanillaMenuPresentation.enabled(this)) return;
        int tint = FlatlinedPresentation.buttonFrame(g, this, alpha, false);
        if (alpha > 0.02F) {
            int handleX = getX() + (int) (Math.clamp(value, 0, 1) * (getWidth() - 8));
            int opacity = Math.round(alpha * 255) << 24;
            g.fill(handleX + 1, getY() + 2, handleX + 7, getY() + getHeight() - 2,
                    opacity | (active ? 0x7E3045 : 0x333033));
            UiRenderer.outline(g, handleX + 1, getY() + 2, 6, getHeight() - 4,
                    opacity | ((!active ? 0xFF777077 : isHoveredOrFocused() ? VanillaMenuPresentation.HIGHLIGHT : VanillaMenuPresentation.SCROLL_THUMB_EDGE) & 0xFFFFFF));
            renderScrollingString(g, Minecraft.getInstance().font, 2, tint);
        }
        ci.cancel();
    }
}
