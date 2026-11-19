package de.artemis.cyberneticenhancements.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
abstract class PauseMenuSoundMixin {
    @WrapMethod(method = "pauseGame")
    private void cyberneticenhancements$playPauseSound(boolean pauseOnly, Operation<Void> original) {
        var minecraft = (Minecraft) (Object) this;
        boolean opening = !pauseOnly && minecraft.level != null && minecraft.screen == null;
        original.call(pauseOnly);
        // In singleplayer, pauseGame pauses existing sound channels after opening the screen.
        if (opening && minecraft.screen instanceof PauseScreen pause && pause.showsPauseMenu()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAUSE.get(), 1.0F, 1.0F));
        }
    }

    @WrapMethod(method = "setScreen")
    private void cyberneticenhancements$playResumeSound(Screen next, Operation<Void> original) {
        var minecraft = (Minecraft) (Object) this;
        boolean resuming = next == null && minecraft.level != null
                && minecraft.screen instanceof PauseScreen pause && pause.showsPauseMenu();
        original.call(next);
        if (resuming && minecraft.screen == null && minecraft.level != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.RESUME.get(), 1.0F, 1.0F));
        }
    }
}
