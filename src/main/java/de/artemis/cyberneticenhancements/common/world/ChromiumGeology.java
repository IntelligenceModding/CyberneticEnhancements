package de.artemis.cyberneticenhancements.common.world;

import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;

/** Large prospects are fixed by the world seed, independently of NPCs and contracts. */
public final class ChromiumGeology {
    public static BlockPos deposit(long seed, int chunkX, int chunkZ) {
        var random = new SplittableRandom(seed ^ chunkX * 341873128712L ^ chunkZ * 132897987541L ^ 0x4348524F4D45L);
        if (random.nextInt(64) != 0) return null;
        return new BlockPos(chunkX * 16 + 6 + random.nextInt(5), -56 + random.nextInt(25) + random.nextInt(24), chunkZ * 16 + 6 + random.nextInt(5));
    }

    public static BlockPos nearest(long seed, BlockPos origin, int minBlocks, int maxBlocks) {
        BlockPos best = null; double distance = Double.MAX_VALUE;
        int radius = (maxBlocks + 15) / 16;
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            var pos = deposit(seed, (origin.getX() >> 4) + x, (origin.getZ() >> 4) + z);
            if (pos == null) continue;
            double d = Math.pow((double) pos.getX() - origin.getX(), 2) + Math.pow((double) pos.getZ() - origin.getZ(), 2);
            if (d >= (double) minBlocks * minBlocks && d <= (double) maxBlocks * maxBlocks && d < distance) { best = pos; distance = d; }
        }
        return best;
    }
    private ChromiumGeology() { }
}
