package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
abstract class FlatlinedButtonMixin extends AbstractWidget {
    protected FlatlinedButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberneticenhancements$deathButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var screen = Minecraft.getInstance().screen;
        boolean vanillaMenuButton = de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation.menuButtonsEnabled(screen)
                && getClass().getName().startsWith("net.minecraft.client.gui.");
        boolean titleModsButton = de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation.enabled(screen)
                && (Object) this instanceof net.neoforged.neoforge.client.gui.widget.ModsButton;
        if (vanillaMenuButton || titleModsButton) {
            FlatlinedPresentation.button(graphics, (AbstractButton) (Object) this, alpha);
            ci.cancel();
            return;
        }
        if (((Object) this).getClass() != Button.class
                || (!FlatlinedPresentation.enabled(screen) && !FlatlinedPresentation.confirmationEnabled(screen))) return;
        FlatlinedPresentation.button(graphics, (Button) (Object) this);
        ci.cancel();
    }
}
