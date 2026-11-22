package de.artemis.cyberneticenhancements.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
abstract class TitleMusicMixin {
    @Shadow private SoundInstance currentMusic;
    @Shadow public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cyberneticenhancements$silentTitle(CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != null || (!de.artemis.cyberneticenhancements.client.ChangelogPopup.isTitle(minecraft.screen)
                && !de.artemis.cyberneticenhancements.client.TitleSlideshowAudio.isMenuSessionActive()
                && !de.artemis.cyberneticenhancements.client.TitleMusic.isPlaying())) return;
        // Stop an existing menu track and prevent scheduling another. Other
        // sounds, including the startup sequence, use their own sound instances.
        if (currentMusic != null) stopPlaying();
        ci.cancel();
    }
}
