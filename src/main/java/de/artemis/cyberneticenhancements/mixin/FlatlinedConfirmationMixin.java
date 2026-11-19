package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.FlatlinedLayout;
import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ConfirmScreen.class)
abstract class FlatlinedConfirmationMixin extends Screen {
    @Shadow @Final private List<Button> exitButtons;

    protected FlatlinedConfirmationMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void cyberneticenhancements$layout(CallbackInfo ci) {
        if (!FlatlinedPresentation.confirmationEnabled(this)) return;
        var layout = FlatlinedLayout.of(width, height);
        for (int index = 0; index < exitButtons.size(); index++) {
            Button button = exitButtons.get(index);
            button.setX(layout.left());
            button.setY(layout.buttonTop() + index * 24);
            button.setWidth(layout.columnWidth());
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 1, require = 0)
    private Component cyberneticenhancements$wrappedTitle(Component original) {
        return FlatlinedPresentation.confirmationEnabled(this) ? Component.empty() : original;
    }
}
