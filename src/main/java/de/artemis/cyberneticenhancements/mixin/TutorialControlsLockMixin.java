package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.TutorialClient;
import de.artemis.cyberneticenhancements.client.ui.TutorialKeyRules;
import de.artemis.cyberneticenhancements.client.ui.LockIcon;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A padlock with a tooltip beside every action that is currently switched off, evaluated live as the tutorial unlocks things. */
@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
abstract class TutorialControlsLockMixin {
    @Shadow @Final private KeyMapping key;
    @Shadow @Final private Button changeButton;

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void cyberneticenhancements$lockIcon(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                                 int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
        if (!TutorialClient.blocks(key)) return;
        // Left of the key button, clear of the collision marker.
        int x = changeButton.getX() - LockIcon.WIDTH - 8, y = top + height / 2 - LockIcon.HEIGHT / 2;
        boolean over = hovering && LockIcon.contains(x, y, mouseX, mouseY);
        LockIcon.render(graphics, x, y, over);
        var screen = Minecraft.getInstance().screen;
        if (over && screen != null) screen.setTooltipForNextRenderPass(Component.translatable(TutorialKeyRules.unlockable(key.getName())
                ? "tutorial.cyberneticenhancements.key_locked" : "tutorial.cyberneticenhancements.key_unavailable"));
    }
}
