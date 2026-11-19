package de.artemis.cyberneticenhancements.common.feedback;

import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public enum PlayerActionFeedback {
    ACCEPTED(ModSounds.NEW_CONTRACT::get, 0.12F, 1.0F),
    READY(SoundEvents.AMETHYST_BLOCK_CHIME, 0.4F, 1.3F),
    COMPLETED(ModSounds.COMPLETE_CONTRACT::get, 0.12F, 1.0F),
    CANCELLED(SoundEvents.BEACON_DEACTIVATE, 0.3F, 1.1F),
    FAILED(SoundEvents.BEACON_DEACTIVATE, 0.45F, 0.8F),
    PURCHASE(SoundEvents.ITEM_PICKUP, 0.35F, 1.15F),
    TRANSFER(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.35F, 1.3F),
    CASH_TRANSFER(ModSounds.CASH_TRANSFER::get, 0.35F, 1.0F),
    SERVICE(SoundEvents.BEACON_ACTIVATE, 0.3F, 1.35F),
    FABRICATE(SoundEvents.PISTON_EXTEND, 0.3F, 1.2F),
    DENIED(SoundEvents.NOTE_BLOCK_BASS.value(), 0.3F, 0.8F);

    private final Supplier<SoundEvent> sound;
    private final float volume, pitch;

    PlayerActionFeedback(SoundEvent sound, float volume, float pitch) {
        this(() -> sound, volume, pitch);
    }

    PlayerActionFeedback(Supplier<SoundEvent> sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void play(Player player) {
        if (!(player instanceof ServerPlayer server)) return;
        server.playNotifySound(sound.get(), SoundSource.PLAYERS, volume, pitch);
        if (this == COMPLETED) server.serverLevel().sendParticles(server, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, false,
                server.getX(), server.getY() + 0.9, server.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
    }
}
