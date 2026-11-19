package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelLoadingScreen.class)
abstract class WorldCreationProgressStyleMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 4)
    private int cyberneticenhancements$progressText(int original) {
        // World loading can draw directly, outside the normal menu text scope.
        return TitleScreenPresentation.menuButtonsEnabled((Screen) (Object) this)
                ? VanillaMenuPresentation.TEXT : original;
    }
}
