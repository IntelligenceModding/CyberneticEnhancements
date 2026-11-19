package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Checkbox.class)
abstract class VanillaCheckboxStyleMixin extends AbstractWidget {
    protected VanillaCheckboxStyleMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void cyberneticenhancements$checkbox(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height) {
        if (!VanillaMenuPresentation.enabled(this)) { g.blitSprite(sprite, x, y, width, height); return; }
        VanillaMenuPresentation.field(g, x, y, width, height, isHoveredOrFocused(), active);
        if (((Checkbox) (Object) this).selected()) {
            int left = x + width / 2 - 4, middle = y + height / 2;
            int color = active ? VanillaMenuPresentation.TEXT : 0xFF9B7C83;
            g.fill(left, middle, left + 2, middle + 2, color);
            g.fill(left + 2, middle + 2, left + 4, middle + 4, color);
            for (int step = 0; step < 4; step++)
                g.fill(left + 4 + step, middle + 1 - step, left + 5 + step, middle + 3 - step, color);
        }
    }
}
