package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LockIconButton.class)
abstract class VanillaLockButtonStyleMixin extends AbstractWidget {
    protected VanillaLockButtonStyleMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$difficultyLock(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
        if (!VanillaMenuPresentation.enabled(this)) return;
        int color = FlatlinedPresentation.buttonFrame(g, this, alpha, false);
        if (alpha > 0.02F) {
            int x = getX() + getWidth() / 2, y = getY() + getHeight() / 2;
            g.fill(x - 4, y - 1, x + 4, y + 6, color);
            int shackle = ((LockIconButton) (Object) this).isLocked() ? x - 3 : x;
            g.fill(shackle + 1, y - 5, shackle + 5, y - 4, color);
            g.fill(shackle, y - 4, shackle + 1, y, color);
            g.fill(shackle + 5, y - 4, shackle + 6, y - 1, color);
            g.fill(x - 1, y + 1, x + 1, y + 4, Math.round(alpha * 255) << 24 | 0x200B13);
        }
        ci.cancel();
    }
}
