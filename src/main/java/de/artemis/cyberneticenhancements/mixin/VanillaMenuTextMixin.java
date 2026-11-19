package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
abstract class VanillaMenuTextMixin {
    @ModifyVariable(method = {
            "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int cyberneticenhancements$readableMenuText(int color) {
        return VanillaMenuPresentation.textEnabled()
                ? VanillaMenuPresentation.textColor(color) : color;
    }
}
