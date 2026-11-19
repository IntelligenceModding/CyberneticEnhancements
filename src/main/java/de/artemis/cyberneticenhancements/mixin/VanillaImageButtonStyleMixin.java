package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ImageButton.class)
abstract class VanillaImageButtonStyleMixin extends AbstractWidget {
    protected VanillaImageButtonStyleMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void cyberneticenhancements$icon(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height) {
        if (!VanillaMenuPresentation.enabled(this)) { g.blitSprite(sprite, x, y, width, height); return; }
        g.setColor(active ? 1 : 0.6F, active ? 0.6F : 0.4F, active ? 0.7F : 0.4F, alpha);
        try { g.blitSprite(sprite, x, y, width, height); }
        finally { g.setColor(1, 1, 1, 1); }
        int edge = !active ? 0xFF70424D : isHoveredOrFocused() ? VanillaMenuPresentation.HIGHLIGHT : VanillaMenuPresentation.EDGE;
        UiRenderer.outline(g, x, y, width, height, Math.round(alpha * 255) << 24 | edge & 0xFFFFFF);
    }
}
