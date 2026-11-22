package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.client.ui.SlideshowOrder;
import de.artemis.cyberneticenhancements.client.ui.TitleMenuSession;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Quiet, nonpositional slideshow ambience for the title session whenever the title music cannot be heard. */
public final class TitleSlideshowAudio {
    private static final int[] CLIPS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final SlideshowOrder ORDER = new SlideshowOrder(CLIPS.length, new Random());
    private static final float VOLUME = 0.16F;
    private static final TitleMenuSession SESSION = new TitleMenuSession();
    private static Loop current;
    private static int slide = -1, resumeAfter = -1;
    private static boolean handedOver;

    private static boolean playable() {
        var mc = Minecraft.getInstance();
        return mc.level == null && SESSION.active() && !LaunchIntro.isPlaying()
                && mc.options.getSoundSourceVolume(SoundSource.MASTER) > 0
                && mc.options.getSoundSourceVolume(SoundSource.AMBIENT) > 0
                && TitleSlideshow.artworkCreditVisible();
    }

    /** Silent while the title music can be heard. */
    private static float gain() {
        return TitleMusic.audible() ? 0 : 1;
    }

    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        SESSION.update(mc.level != null, ChangelogPopup.isTitle(mc.screen) && mc.getOverlay() == null
                && !LaunchIntro.isPlaying() && TitleSlideshow.artworkCreditVisible());
        if (!playable() || gain() <= 0) {
            if (playable()) handedOver = true;
            else if (!SESSION.active()) {
                handedOver = false;
                resumeAfter = -1;
            }
            clear();
            return;
        }
        var frame = TitleSlideshow.advanceAudio(net.minecraft.Util.getMillis());
        // Taking back over from muted music: stay quiet until the next image instead of starting mid-image.
        if (handedOver) {
            handedOver = false;
            resumeAfter = frame.slide();
        }
        if (frame.slide() == resumeAfter) return;
        resumeAfter = -1;
        advance(frame.slide(), mc.options.screenEffectScale().get() <= 0 ? 0 : frame.blend());
    }

    public static boolean isMenuSessionActive() { return SESSION.active(); }

    private static void advance(int visibleSlide, float blend) {
        var mc = Minecraft.getInstance();
        if (current == null || slide != visibleSlide || !mc.getSoundManager().isActive(current)) {
            if (current != null) mc.getSoundManager().stop(current);
            // Start at the actual image advance, with an audible initial gain.
            // Preplaying at zero volume loses the opening of the effect.
            current = play(VOLUME);
            slide = visibleSlide;
        }
        current.gain(VOLUME * gain() * (1 - Math.clamp(blend, 0, 1)));
    }

    private static Loop play(float volume) {
        int number = CLIPS[ORDER.next()];
        var loop = new Loop(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID,
                "menu.screen_sound_" + (number < 10 ? "0" : "") + number), volume);
        Minecraft.getInstance().getSoundManager().play(loop);
        return loop;
    }

    private static void clear() {
        var manager = Minecraft.getInstance().getSoundManager();
        if (current != null) manager.stop(current);
        current = null;
        slide = -1;
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private Loop(ResourceLocation id, float initialVolume) {
            super(SoundEvent.createVariableRangeEvent(id), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = initialVolume;
        }
        @Override public void tick() { }
        private void gain(float value) { volume = value; }
    }

    private TitleSlideshowAudio() { }
}
