package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.artemis.cyberneticenhancements.client.DisconnectAnimation;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PauseScreen.class)
abstract class PauseDisconnectMixin {
    @WrapMethod(method = "onDisconnect")
    private void cyberneticenhancements$animateDisconnect(Operation<Void> original) {
        DisconnectAnimation.begin(original::call);
    }
}
