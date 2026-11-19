package de.artemis.cyberneticenhancements.mixin;

import de.artemis.cyberneticenhancements.client.ui.TextInputFocus;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
abstract class ContainerTextInputMixin {
    // Let Screen handle editing, Escape and focus navigation before suppressing inventory shortcuts.
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/KeyMapping;isActiveAndMatches(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Z"),
            cancellable = true)
    private void cyberneticenhancements$keepTyping(int key, int scanCode, int modifiers,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (TextInputFocus.isTyping((Screen) (Object) this)) cir.setReturnValue(true);
    }

    // Some screens check hotbar bindings themselves before calling the parent key handler.
    @Inject(method = "checkHotbarKeyPressed", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$protectHotbar(int key, int scanCode,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (TextInputFocus.isTyping((Screen) (Object) this)) cir.setReturnValue(false);
    }
}
