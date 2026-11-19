package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FocusableTextWidget.class)
abstract class VanillaMessagePanelStyleMixin extends AbstractWidget {
    protected VanillaMessagePanelStyleMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void cyberneticenhancements$messageFill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, VanillaMenuPresentation.enabled(this) ? VanillaMenuPresentation.FIELD : color);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderOutline(IIIII)V"))
    private void cyberneticenhancements$messageEdge(GuiGraphics g, int x, int y, int width, int height, int color) {
        if (VanillaMenuPresentation.enabled(this)) {
            UiRenderer.outline(g, x, y, width, height, isFocused() ? VanillaMenuPresentation.ENTRY_SELECTED : VanillaMenuPresentation.PANEL_EDGE);
        } else g.renderOutline(x, y, width, height, color);
    }
}
