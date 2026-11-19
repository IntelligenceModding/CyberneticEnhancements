package de.artemis.cyberneticenhancements.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Seeded, intersecting noise ridges, with sparse ore in tuff like vanilla's large iron veins. */
public final class ChromiumLargeVeins {
    public static final int RADIUS = 40;
    public static final int HEIGHT = 18;
    public enum Material { NONE, TUFF, ORE }
    public record Sample(Material material, BlockPos center) { }
    private static final Sample EMPTY = new Sample(Material.NONE, null);
    private record Key(long seed, BlockPos center) { }
    // Profiles are immutable and safe to sample concurrently during neighboring chunk generation.
    private static final Map<Key, Vein> CACHE = new LinkedHashMap<>(128, 0.75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Vein> eldest) { return size() > 256; }
    };

    public static List<Vein> touchingChunk(long seed, int chunkX, int chunkZ) {
        var veins = new ArrayList<Vein>();
        int reach = (RADIUS + 15) / 16;
        for (int x = chunkX - reach; x <= chunkX + reach; x++) for (int z = chunkZ - reach; z <= chunkZ + reach; z++) {
            var center = ChromiumGeology.deposit(seed, x, z);
            if (center == null || center.getX() + RADIUS < chunkX * 16 || center.getX() - RADIUS > chunkX * 16 + 15
                    || center.getZ() + RADIUS < chunkZ * 16 || center.getZ() - RADIUS > chunkZ * 16 + 15) continue;
            synchronized (CACHE) {
                veins.add(CACHE.computeIfAbsent(new Key(seed, center), key -> new Vein(key.seed(), key.center())));
            }
        }
        veins.sort(Comparator.comparingLong(vein -> vein.center().asLong()));
        return List.copyOf(veins);
    }

    public static BlockPos owner(long seed, BlockPos ore) {
        var sample = sample(touchingChunk(seed, ore.getX() >> 4, ore.getZ() >> 4), ore.getX(), ore.getY(), ore.getZ());
        return sample.material() == Material.ORE ? sample.center() : null;
    }

    public static Sample sample(List<Vein> veins, int x, int y, int z) {
        double closest = Double.MAX_VALUE;
        BlockPos center = null;
        Material material = Material.NONE;
        for (var vein : veins) {
            double distance = vein.center().distToCenterSqr(x + 0.5, y + 0.5, z + 0.5);
            if (distance >= closest) continue;
            var candidate = vein.sample(x, y, z);
            if (candidate == Material.NONE) continue;
            closest = distance; center = vein.center(); material = candidate;
        }
        return center == null ? EMPTY : new Sample(material, center);
    }

    public static final class Vein {
        private final BlockPos center;
        private final long salt;
        private final NormalNoise ridgeA, ridgeB, richness;
        private final double biasA, biasB;

        private Vein(long seed, BlockPos center) {
            this.center = center;
            salt = mix(seed ^ center.asLong() ^ 0x4348524F4D49554DL);
            var random = RandomSource.create(salt);
            ridgeA = NormalNoise.create(random, -4, 1.0);
            ridgeB = NormalNoise.create(random, -4, 1.0);
            richness = NormalNoise.create(random, -3, 1.0);
            biasA = ridgeA.getValue(0, 0, 0);
            biasB = ridgeB.getValue(0, 0, 0);
        }

        public BlockPos center() { return center; }

        public Material sample(int x, int y, int z) {
            if (y < -60 || y > 16) return Material.NONE;
            double dx = x - center.getX(), dy = y - center.getY(), dz = z - center.getZ();
            double edge = (dx * dx + dz * dz) / (RADIUS * RADIUS) + dy * dy / (HEIGHT * HEIGHT);
            if (edge >= 1) return Material.NONE;
            // Keep a small rich core so geological reports always lead into the vein itself.
            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 1) return Material.ORE;
            double ridge = Math.max(Math.abs(ridgeA.getValue(dx, dy, dz) - biasA), Math.abs(ridgeB.getValue(dx, dy, dz) - biasB));
            if (ridge > 0.13 * Math.min(1, (1 - edge) * 4)) return Material.NONE;
            long hash = mix(salt ^ x * 341873128712L ^ y * 42317861L ^ z * 132897987541L);
            // Vanilla vein solidness is 70%; remaining gaps keep these formations irregular.
            if ((hash >>> 40) / 16777216.0 > 0.7) return Material.NONE;
            double chance = Math.clamp(0.2 + richness.getValue(dx, dy, dz) * 0.2, 0.1, 0.3);
            return ((hash >>> 16) & 0xFFFFFFL) / 16777216.0 < chance ? Material.ORE : Material.TUFF;
        }
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private ChromiumLargeVeins() { }
}
