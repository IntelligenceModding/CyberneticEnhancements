package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation;
import net.minecraft.client.gui.components.PlainTextButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PlainTextButton.class)
abstract class TitleCreditStyleMixin {
    @ModifyArgs(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    private void cyberneticenhancements$creditColor(Args args) {
        args.set(4, TitleScreenPresentation.footerTextColor(args.get(4)));
    }
}
