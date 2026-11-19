package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabButton.class)
abstract class VanillaTabStyleMixin extends AbstractWidget {
    protected VanillaTabStyleMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$menuTab(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        if (!TitleScreenPresentation.menuButtonsEnabled(minecraft.screen)
                || ((Object) this).getClass() != TabButton.class) return;
        var tab = (TabButton) (Object) this;
        int textColor = FlatlinedPresentation.buttonFrame(graphics, tab, alpha, tab.isSelected());
        tab.renderString(graphics, minecraft.font, textColor);
        ci.cancel();
    }
}
