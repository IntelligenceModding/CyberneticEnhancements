package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import de.artemis.cyberneticenhancements.mixin.IntroSoundChannelAccess;
import de.artemis.cyberneticenhancements.mixin.IntroSoundEngineAccess;
import de.artemis.cyberneticenhancements.mixin.IntroSoundManagerAccess;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import org.lwjgl.openal.AL10;

/** One full, predecoded clip. Only its own sound source is queried or paused. */
public final class LaunchIntroAudio implements AutoCloseable {
    private SimpleSoundInstance sound;
    private ChannelAccess.ChannelHandle handle;
    private final AtomicBoolean polling = new AtomicBoolean();
    private volatile long positionMillis;
    private boolean closed;

    public long positionMillis(boolean focused) {
        if (closed) return -1;
        if (sound == null) {
            if (!focused) return 0;
            var manager = Minecraft.getInstance().getSoundManager();
            sound = SimpleSoundInstance.forUI(ModSounds.LAUNCH_INTRO.get(), 1, 1);
            manager.play(sound);
            var engine = ((IntroSoundManagerAccess) manager).cyberneticenhancements$soundEngine();
            handle = ((IntroSoundEngineAccess) engine).cyberneticenhancements$soundChannels().get(sound);
        }
        // Muted/unavailable audio and completed playback use the visual fallback clock.
        if (handle == null || handle.isStopped()) return -1;
        if (polling.compareAndSet(false, true)) {
            handle.execute(channel -> {
                try {
                    if (focused) channel.unpause(); else channel.pause();
                    int source = ((IntroSoundChannelAccess) channel).cyberneticenhancements$source();
                    int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                    if (state == AL10.AL_PLAYING || state == AL10.AL_PAUSED)
                        positionMillis = Math.max(positionMillis, Math.round(AL10.alGetSourcef(source, org.lwjgl.openal.AL11.AL_SEC_OFFSET) * 1_000));
                } finally {
                    polling.set(false);
                }
            });
        }
        return positionMillis;
    }

    @Override public void close() {
        closed = true;
        if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound);
    }
}
