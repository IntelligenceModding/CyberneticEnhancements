package de.artemis.cyberneticenhancements.common.world;

import net.minecraft.core.BlockPos;
import org.junit.Test;
import java.util.HashMap;
import java.util.HashSet;
import static org.junit.Assert.*;

public class ChromiumLargeVeinsTest {
    @Test public void veinsExtendAcrossChunksWithSparseOreAndVanillaHostRock() {
        for (long seed : new long[]{123, 456, 947231}) {
            var center = ChromiumGeology.nearest(seed, BlockPos.ZERO, 64, 512);
            var vein = ChromiumLargeVeins.touchingChunk(seed, center.getX() >> 4, center.getZ() >> 4).stream()
                    .filter(v -> v.center().equals(center)).findFirst().orElseThrow();
            int ores = 0, host = 0, minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            var chunks = new HashSet<Long>();
            var examples = new HashMap<Long, BlockPos>();
            for (int x = center.getX() - ChromiumLargeVeins.RADIUS; x <= center.getX() + ChromiumLargeVeins.RADIUS; x++)
                for (int z = center.getZ() - ChromiumLargeVeins.RADIUS; z <= center.getZ() + ChromiumLargeVeins.RADIUS; z++)
                    for (int y = Math.max(-60, center.getY() - ChromiumLargeVeins.HEIGHT); y <= Math.min(16, center.getY() + ChromiumLargeVeins.HEIGHT); y++) {
                        var material = vein.sample(x, y, z);
                        if (material == ChromiumLargeVeins.Material.TUFF) host++;
                        if (material != ChromiumLargeVeins.Material.ORE) continue;
                        ores++; minX = Math.min(minX, x); maxX = Math.max(maxX, x); minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                        long chunk = net.minecraft.world.level.ChunkPos.asLong(x >> 4, z >> 4);
                        chunks.add(chunk); examples.putIfAbsent(chunk, new BlockPos(x, y, z));
                    }
            assertTrue("Vein too small for seed " + seed + ": " + ores, ores > 100);
            assertTrue("Host rock must outweigh ore", host > ores * 2);
            assertTrue("Vein remained confined to a small cluster", Math.max(maxX - minX, maxZ - minZ) >= 32);
            assertTrue("Vein did not cross enough chunk borders", chunks.size() >= 4);
            for (var pos : examples.values()) {
                var generated = ChromiumLargeVeins.sample(ChromiumLargeVeins.touchingChunk(seed, pos.getX() >> 4, pos.getZ() >> 4), pos.getX(), pos.getY(), pos.getZ());
                assertEquals(generated.material() == ChromiumLargeVeins.Material.ORE ? generated.center() : null,
                        ChromiumLargeVeins.owner(seed, pos));
            }
            assertEquals(center, ChromiumLargeVeins.owner(seed, center));
        }
    }
}
