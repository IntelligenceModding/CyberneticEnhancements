package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.TitleMenuSession;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** The title track: plays through the whole menu session in place of the slideshow ambience. */
public final class TitleMusic {
    private static final float FADE_IN_TICKS = 40, FADE_OUT_TICKS = 20;
    private static final TitleMenuSession SESSION = new TitleMenuSession();
    private static Track current;

    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var manager = mc.getSoundManager();
        // Independent of the slideshow, so the music also plays when the slideshow background is disabled.
        SESSION.update(mc.level != null, mc.screen instanceof TitleScreen && mc.getOverlay() == null && !LaunchIntro.isPlaying());
        boolean wanted = mc.level == null && SESSION.active() && !LaunchIntro.isPlaying() && audible();
        // Resource reloads stop every sound; start again rather than holding a dead instance.
        if (current != null && !manager.isActive(current)) current = null;
        if (!wanted) {
            if (current != null) current.stopping = true;
            return;
        }
        if (current == null) {
            current = new Track();
            manager.play(current);
        }
        current.stopping = false;
    }

    public static boolean isPlaying() { return current != null; }

    /** Whether the track can be heard; if not, the slideshow ambience keeps the whole title session instead. */
    public static boolean audible() {
        var options = Minecraft.getInstance().options;
        return ClientConfig.SPEC.isLoaded() && ClientConfig.CUSTOM_TITLE_SCREEN.get()
                && options.getSoundSourceVolume(SoundSource.MASTER) > 0
                && options.getSoundSourceVolume(SoundSource.MUSIC) > 0;
    }

    private static final class Track extends AbstractTickableSoundInstance {
        private boolean stopping;

        private Track() {
            super(ModSounds.TITLE_SCREEN_MUSIC.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            // Start just audible: a silent start is skipped by the sound engine.
            volume = 0.05F;
        }

        @Override public void tick() {
            if (!stopping) volume = Math.min(1, volume + 1 / FADE_IN_TICKS);
            else if ((volume -= 1 / FADE_OUT_TICKS) <= 0) stop();
        }
    }

    private TitleMusic() { }
}
