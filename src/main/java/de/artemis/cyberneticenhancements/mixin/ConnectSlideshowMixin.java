package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.JoinAnimation;
import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ConnectScreen.class)
abstract class ConnectSlideshowMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 1)
    private Component cyberneticenhancements$hideStatusOverSlideshow(Component status) {
        return TitleSlideshow.renderedForCurrentScreen() && JoinAnimation.isWorldLoadingScreen((ConnectScreen) (Object) this)
                ? Component.empty() : status;
    }
}
