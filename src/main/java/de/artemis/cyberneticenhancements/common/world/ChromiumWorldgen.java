package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ChromiumWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, CyberneticEnhancements.MOD_ID);
    static {
        FEATURES.register("chromium_vein", () -> new Deposit(false));
        FEATURES.register("chromium_deposit", () -> new Deposit(true));
    }

    private static final class Deposit extends Feature<NoneFeatureConfiguration> {
        private final boolean large;
        Deposit(boolean large) { super(NoneFeatureConfiguration.CODEC); this.large = large; }
        @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            var level = context.level();
            if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
            if (large) return placeLarge(level, context.origin());
            var random = context.random();
            var center = context.origin();
            int placed = 0;
            int pockets = 1;
            for (int pocket = 0; pocket < pockets; pocket++) {
                var pos = center;
                var pocketBlocks = new java.util.ArrayList<BlockPos>();
                if (replaceable(level, pos, center)) pocketBlocks.add(pos.immutable());
                int count = 3 + random.nextInt(5);
                for (int attempt = 0; pocketBlocks.size() < count && attempt < 80; attempt++) {
                    var next = pocketBlocks.isEmpty() ? pos.relative(Direction.getRandom(random))
                            : pocketBlocks.get(random.nextInt(pocketBlocks.size())).relative(Direction.getRandom(random));
                    if (replaceable(level, next, center) && !pocketBlocks.contains(next)) pocketBlocks.add(next.immutable());
                }
                if (pocketBlocks.size() < 3) continue;
                for (var ore : pocketBlocks) {
                    boolean deep = level.getBlockState(ore).is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
                    setBlock(level, ore, (deep ? ModBlocks.DEEPSLATE_CHROMIUM_ORE : ModBlocks.CHROMIUM_ORE).get().defaultBlockState()
                            .setValue(de.artemis.cyberneticenhancements.common.block.ChromiumOreBlock.LARGE_DEPOSIT, false));
                    placed++;
                }
            }
            return placed > 0;
        }
        private boolean placeLarge(WorldGenLevel level, BlockPos origin) {
            int chunkX = origin.getX() >> 4, chunkZ = origin.getZ() >> 4;
            var veins = ChromiumLargeVeins.touchingChunk(level.getSeed(), chunkX, chunkZ);
            if (veins.isEmpty()) return false;
            int bottom = Math.max(level.getMinBuildHeight() + 4, veins.stream().mapToInt(v -> v.center().getY() - ChromiumLargeVeins.HEIGHT).min().orElse(-60));
            int top = Math.min(16, veins.stream().mapToInt(v -> v.center().getY() + ChromiumLargeVeins.HEIGHT).max().orElse(16));
            int ores = 0;
            var pos = new BlockPos.MutableBlockPos();
            // Each invocation writes only its own chunk. Neighboring chunks independently sample the same ridges.
            for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
                for (int y = bottom; y <= top; y++) {
                    var sample = ChromiumLargeVeins.sample(veins, x, y, z);
                    if (sample.material() == ChromiumLargeVeins.Material.NONE) continue;
                    pos.set(x, y, z);
                    if (!natural(level, pos)) continue;
                    if (sample.material() == ChromiumLargeVeins.Material.TUFF) {
                        setBlock(level, pos, net.minecraft.world.level.block.Blocks.TUFF.defaultBlockState());
                    } else {
                        boolean deep = level.getBlockState(pos).is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
                        setBlock(level, pos, (deep ? ModBlocks.DEEPSLATE_CHROMIUM_ORE : ModBlocks.CHROMIUM_ORE).get().defaultBlockState()
                                .setValue(de.artemis.cyberneticenhancements.common.block.ChromiumOreBlock.LARGE_DEPOSIT, true));
                        ores++;
                    }
                }
            }
            return ores > 0;
        }
        private boolean replaceable(WorldGenLevel level, BlockPos pos, BlockPos center) {
            return (pos.getX() >> 4) == (center.getX() >> 4) && (pos.getZ() >> 4) == (center.getZ() >> 4)
                    && pos.getY() >= level.getMinBuildHeight() + 4 && pos.getY() <= 16 && natural(level, pos);
        }
        private boolean natural(WorldGenLevel level, BlockPos pos) { return level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD); }
    }
    private ChromiumWorldgen() { }
}
