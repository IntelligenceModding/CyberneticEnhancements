package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, CyberneticEnhancements.MOD_ID);
    /** Smoke that is pushed out of an engine vent and fades away a few blocks from it. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VENT_SMOKE = PARTICLES.register("vent_smoke", () -> new SimpleParticleType(false));

    private ModParticles() {}

    public static void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }
}
