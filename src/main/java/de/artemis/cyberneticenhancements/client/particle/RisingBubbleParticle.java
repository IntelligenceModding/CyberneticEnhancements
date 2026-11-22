package de.artemis.cyberneticenhancements.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

/**
 * An air bubble that keeps rising until it reaches the surface and pops there. The vanilla bubble only lives a moment,
 * which is too short to show air escaping from a container on the sea floor.
 */
public final class RisingBubbleParticle extends TextureSheetParticle {
    private static final double RISE = 0.14;
    private final float phase;

    private RisingBubbleParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        pickSprite(sprites);
        this.quadSize = 0.07F + random.nextFloat() * 0.07F;
        this.lifetime = 400;
        this.phase = random.nextFloat() * 6.28F;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
    }

    private double wayX, wayY, wayZ;
    private boolean escaping;
    private int trapped;
    private int searched = -100;

    private boolean open(int x, int y, int z) {
        var pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).is(FluidTags.WATER);
    }

    /** Whether the water column above a cell is free for long enough that it can only be open sea, not the inside of a container. */
    private boolean openSky(int x, int y, int z) {
        for (int up = 1; up <= 8; up++) if (!open(x, y + up, z)) return false;
        return true;
    }

    /**
     * Flood-fills through the water around the bubble, up and down as well as sideways, to the nearest cell with open sea above it
     * and steers towards the first step of that way, so a bubble under a roof finds a hole in the wall at any height.
     */
    private void searchWayUp() {
        boolean wasEscaping = escaping;
        if (search(false)) trapped = 0;
        else if (++trapped >= 6) { // Air under a roof cannot sink to a low hole; it pops rather than swim downwards.
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0, 0);
            remove();
        }
        if (wasEscaping && !escaping && yd < RISE) yd = RISE; // Out in the open: straight up again, without sinking first.
    }

    /** One flood fill for a way out that never goes down, since air does not sink. */
    private boolean search(boolean allowDown) {
        int cx = (int) Math.floor(x), cy = (int) Math.floor(y), cz = (int) Math.floor(z);
        wayX = wayY = wayZ = 0;
        escaping = false;
        if (openSky(cx, cy, cz)) return true;
        var seen = new java.util.HashSet<Long>();
        var queue = new java.util.ArrayDeque<int[]>();
        seen.add(BlockPos.asLong(cx, cy, cz));
        int[][] around = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
        for (int[] d : around) {
            if (d[1] < 0 && !allowDown) continue;
            int nx = cx + d[0], ny = cy + d[1], nz = cz + d[2];
            if (open(nx, ny, nz) && seen.add(BlockPos.asLong(nx, ny, nz))) queue.add(new int[]{nx, ny, nz, nx, ny, nz, 1});
        }
        int budget = 2500;
        while (!queue.isEmpty() && budget-- > 0) {
            int[] c = queue.poll();
            if (openSky(c[0], c[1], c[2])) {
                double dx = c[3] + 0.5 - x, dy = c[4] + 0.5 - y, dz = c[5] + 0.5 - z;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (length < 1.0E-4) return true;
                wayX = dx / length;
                wayY = dy / length;
                wayZ = dz / length;
                escaping = true;
                return true;
            }
            if (c[6] >= 40) continue;
            for (int[] d : around) {
                if (d[1] < 0 && !allowDown) continue;
                int nx = c[0] + d[0], ny = c[1] + d[1], nz = c[2] + d[2];
                if (open(nx, ny, nz) && seen.add(BlockPos.asLong(nx, ny, nz))) queue.add(new int[]{nx, ny, nz, c[3], c[4], c[5], c[6] + 1});
            }
        }
        return false;
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }

/** The base particle stops moving for good once it bumps a roof; a bubble has to keep swimming, so it does its own collision. */
@Override
public void move(double dx, double dy, double dz) {
    var moved = net.minecraft.world.entity.Entity.collideBoundingBox(null, new net.minecraft.world.phys.Vec3(dx, dy, dz), getBoundingBox(), level, java.util.List.of());
    if (moved.x != 0 || moved.y != 0 || moved.z != 0) {
        setBoundingBox(getBoundingBox().move(moved));
        setLocationFromBoundingbox();
    }
    if (dx != moved.x) xd = 0;
    if (dz != moved.z) zd = 0;
}

@Override
public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) { remove(); return; }
        if (escaping) {
            xd = xd * 0.7 + wayX * 0.05;
            yd = yd * 0.7 + wayY * 0.05;
            zd = zd * 0.7 + wayZ * 0.05;
        } else {
            yd += (RISE - yd) * 0.12;
            xd = xd * 0.9 + Math.sin(age * 0.35 + phase) * 0.004;
            zd = zd * 0.9 + Math.cos(age * 0.31 + phase) * 0.004;
        }
        move(xd, yd, zd);
        if (!escaping && Math.abs(y - yo) < 1.0E-3 && age - searched > 4) { // Stuck under a roof: look for the way out.
            searchWayUp();
            searched = age;
            if (!escaping) { // No way found: wriggle sideways in a random direction.
                xd += (random.nextDouble() - 0.5) * 0.1;
                zd += (random.nextDouble() - 0.5) * 0.1;
            }
        } else if (escaping && age - searched > 6) {
            searchWayUp();
            searched = age;
        }
        if (!level.getFluidState(BlockPos.containing(x, y + 0.1, z)).is(FluidTags.WATER)) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0, 0);
            remove();
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new RisingBubbleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
