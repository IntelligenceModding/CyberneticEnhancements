package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Breaking the hull of a sealed container that lies under water lets the sea in: the trapped air escapes in a rush of bubbles that
 * rises to the surface while the water floods the inside, spreading from the hole.
 */
public final class ShippingContainerLeaks {
    private static final TagKey<Structure> CONTAINERS = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "shipping_container"));
    /** The most cells of trapped air that are looked at. */
    private static final int MAX_POCKET = 4000;
    /** Smaller pockets are left to the normal flow of water, which copes with them. */
    private static final int MIN_BURST = 24;
    /** How long the rush of bubbles lasts, in ticks, and how fast bubbles rise, in blocks per tick. */
    private static final int MIN_TICKS = 100, MAX_TICKS = 260;
    private static final double RISE = 0.14;

    private ShippingContainerLeaks() {}

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos().immutable();
        if (pos.getY() >= level.getSeaLevel()) return;
        var hull = hullAt(level, pos);
        if (hull == null || hull.flooded()) return;
        var server = level.getServer();
        server.tell(new TickTask(server.getTickCount() + 1, () -> release(level, hull, pos, level.getSeaLevel())));
    }

    private static @Nullable ShippingContainerPiece hullAt(ServerLevel level, BlockPos pos) {
        var start = level.structureManager().getStructureWithPieceAt(pos, CONTAINERS);
        if (!start.isValid()) return null;
        for (var piece : start.getPieces())
            if (piece instanceof ShippingContainerPiece hull && hull.getBoundingBox().isInside(pos)) return hull;
        return null;
    }

    /** The way out of the hole: towards the sea it opens onto, so the bubbles leave the container before they rise. */
    private static net.minecraft.world.phys.Vec3 outward(ServerLevel level, BlockPos hole) {
        double x = 0, y = 0, z = 0;
        for (Direction side : Direction.values()) {
            if (!level.getFluidState(hole.relative(side)).is(FluidTags.WATER)) continue;
            // Water on that side only counts when it is the open sea, not more of the flooded inside: the far side of the hole is air.
            if (level.getBlockState(hole.relative(side, -1)).isAir() || side == Direction.UP) { x += side.getStepX(); y += side.getStepY(); z += side.getStepZ(); }
        }
        var vector = new net.minecraft.world.phys.Vec3(x, y, z);
        return vector.lengthSqr() < 1.0E-4 ? new net.minecraft.world.phys.Vec3(0, 1, 0) : vector.normalize();
    }

    /** Starts the escape of the air behind a broken hull block, if it opens onto the sea and there is a lot of air behind it. */
    static boolean release(ServerLevel level, ShippingContainerPiece hull, BlockPos hole, int sea) {
        if (!level.getBlockState(hole).isAir()) return false;
        boolean wet = false;
        for (Direction side : Direction.values()) wet |= level.getFluidState(hole.relative(side)).is(FluidTags.WATER);
        if (!wet) return false;
        var box = hull.getBoundingBox();
        List<BlockPos> pocket = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(hole);
        seen.add(hole);
        while (!queue.isEmpty() && pocket.size() < MAX_POCKET) {
            BlockPos at = queue.poll();
            pocket.add(at);
            for (Direction side : Direction.values()) {
                BlockPos next = at.relative(side);
                if (next.getY() >= sea || !box.isInside(next) || !seen.add(next) || !level.getBlockState(next).isAir()) continue;
                queue.add(next);
            }
        }
        if (pocket.size() < MIN_BURST) return false;
        int top = hole.getY();
        while (top < level.getMaxBuildHeight() - 1 && level.getFluidState(new BlockPos(hole.getX(), top + 1, hole.getZ())).is(FluidTags.WATER)) top++;
        int surface = top;
        var centre = hole.getCenter();
        var out = outward(level, hole);
        var random = level.getRandom();
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.BLOCKS, 2.0F, 0.6F);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.2F, 0.5F);
        var server = level.getServer();
        // The pressure drops as the air escapes: the rush starts strong and then eases off, slower and slower, while the water follows it in.
        int ticks = Math.clamp(MIN_TICKS + pocket.size() / 8, MIN_TICKS, MAX_TICKS);
        double decay = ticks / 4.0, total = 1 - Math.exp(-ticks / decay);
        int arrival = (int) ((surface - hole.getY()) / RISE);
        Runnable[] step = new Runnable[1];
        int[] tick = {0, 0, 0}; // The tick, how many cells are flooded, and when the next gurgle plays.
        step[0] = () -> {
            int t = tick[0];
            double strength = Math.exp(-t / decay);
            int flooded = (int) Math.ceil(pocket.size() * (1 - Math.exp(-(t + 1) / decay)) / total);
            for (int i = tick[1]; i < Math.min(pocket.size(), flooded); i++) {
                BlockPos cell = pocket.get(i);
                if (!level.getBlockState(cell).isAir()) continue;
                level.setBlock(cell, Blocks.WATER.defaultBlockState(), 2);
                if (i % 9 == 0) level.sendParticles(ModParticles.RISING_BUBBLE.get(), cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5, 2, 0.4, 0.3, 0.4, 0.02);
            }
            tick[1] = Math.max(tick[1], Math.min(pocket.size(), flooded));
            if (t < ticks) { // The rush: a thick stream of bubbles that shoots out of the hole and then rises, thinning out over time.
                int streams = Math.min(2 + (int) (14 * strength), 16);
                for (int i = 0; i < streams; i++) {
                    double speed = (0.12 + 0.3 * strength) * (0.5 + random.nextDouble());
                    level.sendParticles(ModParticles.RISING_BUBBLE.get(), centre.x + out.x * 0.5 + (random.nextDouble() - 0.5) * 0.6, centre.y + out.y * 0.5 + (random.nextDouble() - 0.5) * 0.6,
                            centre.z + out.z * 0.5 + (random.nextDouble() - 0.5) * 0.6, 0, out.x * speed + (random.nextDouble() - 0.5) * 0.06, out.y * speed + (random.nextDouble() - 0.5) * 0.06, out.z * speed + (random.nextDouble() - 0.5) * 0.06, 1.0);
                }
                level.sendParticles(ModParticles.RISING_BUBBLE.get(), centre.x + out.x * 1.5, centre.y + out.y * 1.5, centre.z + out.z * 1.5, 1 + (int) (10 * strength), 0.4, 0.3, 0.4, 0.02);
            }
            int since = t - arrival;
            if (since >= 0 && since < ticks) { // And the same air breaking the surface above it.
                double eased = Math.exp(-since / decay);
                level.sendParticles(ParticleTypes.BUBBLE_POP, centre.x, surface + 0.9, centre.z, 2 + (int) (8 * eased), 0.5 + 0.5 * eased, 0.05, 0.5 + 0.5 * eased, 0.02);
                level.sendParticles(ParticleTypes.SPLASH, centre.x, surface + 1.0, centre.z, 1 + (int) (10 * eased), 0.4 + 0.4 * eased, 0.05, 0.4 + 0.4 * eased, 0.1);
                if (since == 0) level.playSound(null, centre.x, surface + 1.0, centre.z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.BLOCKS, 1.5F, 1.0F);
            }
            if (t < ticks && t >= tick[2]) {
                level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, (float) (0.4 + 0.9 * strength), (float) (0.6 + 0.3 * (1 - strength)));
                tick[2] = t + 5 + (int) (t / 10.0); // The gurgles come further and further apart.
            }
            tick[0]++;
            if (tick[0] < ticks + arrival || tick[1] < pocket.size()) server.tell(new TickTask(server.getTickCount() + 1, step[0]));
        };
        step[0].run();
        return true;
    }
}
