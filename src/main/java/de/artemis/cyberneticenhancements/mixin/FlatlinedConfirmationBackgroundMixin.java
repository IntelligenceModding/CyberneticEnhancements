package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.TitleConfirmScreen.class)
abstract class FlatlinedConfirmationBackgroundMixin extends Screen {
    protected FlatlinedConfirmationBackgroundMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberneticenhancements$quietBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!FlatlinedPresentation.confirmationEnabled(this)) return;
        FlatlinedPresentation.confirmationBackground(graphics, this);
        ci.cancel();
    }
}
