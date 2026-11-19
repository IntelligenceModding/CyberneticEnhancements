package de.artemis.cyberneticenhancements.mixin;

import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface IntroSoundEngineAccess {
    @Accessor("instanceToChannel") Map<SoundInstance, ChannelAccess.ChannelHandle> cyberneticenhancements$soundChannels();
}
