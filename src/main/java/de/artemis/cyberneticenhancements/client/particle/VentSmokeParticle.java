package de.artemis.cyberneticenhancements.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Dark smoke like the vanilla kind, but it shrinks away once it has travelled a set distance from where it was released. */
public final class VentSmokeParticle extends BaseAshSmokeParticle {
    /** The distance at which it has vanished, and how much of it is spent shrinking. */
    private static final double REACH = 4.5, FADE = 2.0;
    private final double startX, startY, startZ;

    private VentSmokeParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, 0.1F, 0.1F, 0.1F, xSpeed, ySpeed, zSpeed, 3.6F, sprites, 0.3F, 8, -0.1F, false);
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.lifetime = 40 + level.random.nextInt(20); // Only a backstop; the distance ends it first.
    }

    private double travelled() {
        double dx = x - startX, dy = y - startY, dz = z - startZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float shrink = (float) Mth.clamp((REACH - travelled()) / FADE, 0, 1);
        return super.getQuadSize(scaleFactor) * shrink;
    }

    @Override
    public void tick() {
        super.tick();
        if (travelled() >= REACH) remove();
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new VentSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
