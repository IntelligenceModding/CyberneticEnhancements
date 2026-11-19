package de.artemis.cyberneticenhancements.mixin;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No toasts in the tutorial world: vanilla's movement hints, advancement, recipe or system popups would talk over it. */
@Mixin(ToastComponent.class)
abstract class TutorialToastMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$noTutorialToasts(Toast toast, CallbackInfo ci) {
        if (de.artemis.cyberneticenhancements.client.TutorialClient.inTutorialWorld()) ci.cancel();
    }
}
