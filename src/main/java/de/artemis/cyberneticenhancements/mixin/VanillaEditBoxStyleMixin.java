package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
abstract class VanillaEditBoxStyleMixin extends AbstractWidget {
    protected VanillaEditBoxStyleMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void cyberneticenhancements$field(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height) {
        if (VanillaMenuPresentation.enabled(this)) {
            VanillaMenuPresentation.field(g, x, y, width, height, isFocused(), active);
        } else g.blitSprite(sprite, x, y, width, height);
    }
}
