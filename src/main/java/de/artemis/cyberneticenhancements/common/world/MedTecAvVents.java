package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.registry.ModParticles;
import de.artemis.cyberneticenhancements.common.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The engines of Med Tec AV breathe: in each corner a pair of deepslate columns capped with slabs is the exhaust, and
 * smoke rises from the seam between them, with a hiss and a heavier puff every few seconds. The prefab itself stays
 * plain blocks, so the effect follows the structure's rotation and stops once its vent has been broken.
 */
public final class MedTecAvVents {
    /** Corner vents in template space: the x of the column pair, and the two z positions it spans. */
    private static final int[] VENT_X = {2, 17};
    private static final int[][] VENT_Z = {{1, 2}, {8, 9}};
    /** The slab that caps each column. */
    private static final int CAP_Y = 3;
    private static final int SCAN_CHUNKS = 3, RANGE = 40, SCAN_INTERVAL = 40, PERIOD = 70, BURST = 10, CRACKLE_PERIOD = 30;
    /** The row of glass panes above each side wall, in template space: from x 7 to 12 at the height of the vents' slabs, on both long sides. */
    private static final int PANES_FROM = 7, PANES_TO = 12, PANES_Y = 3;
    private static final int[] PANES_Z = {1, 9};
    /** How many ticks the red pane stays on one pane before moving on. */
    private static final int PANE_STEP = 6;
    /**
     * Now and then a vent malfunctions: it sputters, cuts out, then restarts with puffs of white smoke. Time is cut into
     * epochs; in each, a vent has a one in {@code GLITCH_ODDS_PERCENT} chance of one malfunction at a random point of it,
     * all worked out from the time and the vent's position so that nothing has to be stored.
     */
    private static final int GLITCH_EPOCH = 900, GLITCH_LENGTH = 110, GLITCH_ODDS_PERCENT = 40;
    /** Within a malfunction: sputtering until here, dead until the restart, then the puffs of white smoke. */
    private static final int GLITCH_DEAD_FROM = 32, GLITCH_RESTART_FROM = 70;
    private static final Map<ServerLevel, Cache> CACHE = new HashMap<>();

    private MedTecAvVents() {}

    /** The outlet is the seam between the two columns; the push points away from the structure, the way the engine's trapdoors face. */
    private record Vent(Vec3 outlet, Vec3 push, BlockPos capA, BlockPos capB, int phase) {}

    private static final class Cache {
        long scannedAt = -SCAN_INTERVAL;
        List<Vent> vents = List.of();
        List<List<BlockPos>> paneRows = List.of();
    }

    private record Scan(List<Vent> vents, List<List<BlockPos>> paneRows) {}

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.players().isEmpty()) return;
        long time = level.getGameTime();
        if (time % 2 != 0) return;
        var cache = CACHE.computeIfAbsent(level, key -> new Cache());
        if (time - cache.scannedAt >= SCAN_INTERVAL) {
            cache.scannedAt = time;
            var scan = scan(level);
            cache.vents = scan.vents();
            cache.paneRows = scan.paneRows();
        }
        for (var vent : cache.vents) emit(level, vent, time);
        if (time % PANE_STEP == 0) for (var row : cache.paneRows) movePane(level, row, time / PANE_STEP);
    }

    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) CACHE.remove(level);
    }

    /** Every vent of every loaded Med Tec AV build near a player. */
    private static Scan scan(ServerLevel level) {
        var found = new HashMap<BlockPos, Vent>();
        var rows = new HashMap<BlockPos, List<BlockPos>>();
        for (var player : level.players()) {
            var centre = player.chunkPosition();
            for (int dx = -SCAN_CHUNKS; dx <= SCAN_CHUNKS; dx++) for (int dz = -SCAN_CHUNKS; dz <= SCAN_CHUNKS; dz++) {
                var chunk = new ChunkPos(centre.x + dx, centre.z + dz);
                if (!level.hasChunk(chunk.x, chunk.z)) continue; // Looking must never be what loads or generates a chunk.
                for (var start : level.structureManager().startsForStructure(chunk, structure -> structure.type() == ModStructures.MED_TEC_AV.get()))
                    for (var piece : start.getPieces())
                        if (piece instanceof PoolElementStructurePiece pooled) { addVents(pooled, found); addPaneRows(pooled, rows); }
            }
        }
        return new Scan(new ArrayList<>(found.values()), new ArrayList<>(rows.values()));
    }

    private static void addVents(PoolElementStructurePiece piece, Map<BlockPos, Vent> into) {
        var settings = new StructurePlaceSettings().setRotation(piece.getRotation());
        for (int x : VENT_X) for (int[] z : VENT_Z) {
            var a = StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, CAP_Y, z[0])).offset(piece.getPosition());
            var b = StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, CAP_Y, z[1])).offset(piece.getPosition());
            // The seam between the two columns, at the height of the walls and just inside their outer face.
            var out = StructureTemplate.calculateRelativePosition(settings, new BlockPos(x == VENT_X[0] ? -1 : 1, 0, 0));
            var push = new Vec3(out.getX(), 0, out.getZ());
            var outlet = new Vec3((a.getX() + b.getX()) / 2D + 0.5, a.getY() - 1 + 0.7, (a.getZ() + b.getZ()) / 2D + 0.5).add(push.scale(0.3));
            into.putIfAbsent(a, new Vent(outlet, push, a, b, Math.floorMod(a.hashCode(), PERIOD / 2) * 2));
        }
    }

    private static void addPaneRows(PoolElementStructurePiece piece, Map<BlockPos, List<BlockPos>> into) {
        var settings = new StructurePlaceSettings().setRotation(piece.getRotation());
        for (int z : PANES_Z) {
            var row = new ArrayList<BlockPos>();
            for (int x = PANES_FROM; x <= PANES_TO; x++)
                row.add(StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, PANES_Y, z)).offset(piece.getPosition()));
            into.putIfAbsent(row.get(0), List.copyOf(row));
        }
    }

    /**
     * The one red pane in each row of white ones sweeps back and forth along it, like a scanner. Which pane is red follows
     * from the time alone, so nothing is stored; only panes that are still there are repainted, and a broken one stays gone.
     */
    private static void movePane(ServerLevel level, List<BlockPos> row, long step) {
        int last = row.size() - 1, cycle = last * 2;
        int red = (int) (step % cycle);
        if (red > last) red = cycle - red;
        var along = row.get(row.size() - 1).subtract(row.get(0));
        var side = new BlockPos(Integer.signum(along.getZ()), 0, -Integer.signum(along.getX())); // Across the row.
        for (int i = 0; i < row.size(); i++) {
            var pos = row.get(i);
            if (!level.isLoaded(pos)) continue;
            var state = level.getBlockState(pos);
            boolean pane = state.is(Blocks.WHITE_STAINED_GLASS_PANE) || state.is(Blocks.RED_STAINED_GLASS_PANE);
            // The red pane glows: an invisible light sits in the air next to it and travels along with it.
            var spots = new BlockPos[] {pos.above(), pos.offset(side), pos.subtract(side)};
            boolean glow = pane && i == red;
            boolean lit = false;
            for (var spot : spots) {
                var there = level.getBlockState(spot);
                if (there.is(Blocks.LIGHT)) {
                    if (glow && !lit) lit = true;
                    else level.setBlock(spot, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            if (glow && !lit) for (var spot : spots) if (level.getBlockState(spot).isAir()) {
                level.setBlock(spot, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 12), 2);
                break;
            }
            if (!pane) continue;
            var wanted = (i == red ? Blocks.RED_STAINED_GLASS_PANE : Blocks.WHITE_STAINED_GLASS_PANE).withPropertiesOf(state);
            if (wanted != state) level.setBlock(pos, wanted, 2); // The panes keep their connections, so nothing around them needs updating.
        }
    }

    /** One column of a vent: its cap slab, and the deepslate wall right under it. */
    private static boolean intact(ServerLevel level, BlockPos cap) {
        return level.getBlockState(cap).is(Blocks.POLISHED_DEEPSLATE_SLAB) && level.getBlockState(cap.below()).is(Blocks.POLISHED_DEEPSLATE_WALL);
    }

    private static void emit(ServerLevel level, Vent vent, long time) {
        var at = vent.outlet();
        if (!level.isLoaded(vent.capA()) || !level.isLoaded(vent.capB())) return;
        // It only breathes while both of its columns stand whole: a wall or a cap broken out of either one ends it.
        if (!intact(level, vent.capA()) || !intact(level, vent.capB())) return;
        if (level.getNearestPlayer(at.x, at.y, at.z, RANGE, false) == null) return;
        var random = level.getRandom();
        int glitch = glitchTick(vent, time);
        if (glitch >= 0) {
            glitch(level, vent, random, glitch);
            return;
        }
        // The crackle of a fire and the hiss of smoke, at an even rhythm and a steady pitch.
        if ((time + vent.phase()) % CRACKLE_PERIOD == 0) level.playSound(null, at.x, at.y, at.z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 0.8F);
        long phase = (time + vent.phase()) % PERIOD;
        if (phase < BURST) {
            for (int i = 0; i < 3; i++) jet(level, vent, random, 0.28, 0.36);
        } else {
            jet(level, vent, random, 0.18, 0.26);
        }
    }

    /** How far into a malfunction this vent is right now, or -1 if it is running normally. */
    private static int glitchTick(Vent vent, long time) {
        long epoch = Math.floorDiv(time, GLITCH_EPOCH);
        long seed = (vent.capA().asLong() ^ epoch * 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
        seed ^= seed >>> 31;
        if (Math.floorMod(seed, 100) >= GLITCH_ODDS_PERCENT) return -1;
        long start = epoch * GLITCH_EPOCH + Math.floorMod(seed >>> 8, GLITCH_EPOCH - GLITCH_LENGTH);
        long local = time - start;
        return local >= 0 && local < GLITCH_LENGTH ? (int) local : -1;
    }

    /** The engine coughs, dies and comes back: sparks and crackles at first, silence, then white puffs as it restarts. */
    private static void glitch(ServerLevel level, Vent vent, net.minecraft.util.RandomSource random, int tick) {
        var at = vent.outlet();
        if (tick < GLITCH_DEAD_FROM) {
            if (random.nextInt(5) < 2) jet(level, vent, random, 0.1, 0.3); // Smoke in fits and starts.
            if (random.nextInt(4) == 0) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.1F + random.nextFloat() * 0.5F);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 3, 0.15, 0.15, 0.15, 0.2);
            }
            if (tick >= GLITCH_DEAD_FROM - 2) level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.6F);
        } else if (tick >= GLITCH_RESTART_FROM) {
            int since = tick - GLITCH_RESTART_FROM;
            if (since % 12 < 2) { // A puff of white smoke each time it catches.
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 0.8F + random.nextFloat() * 0.2F);
                for (int i = 0; i < 8; i++) jet(level, vent, random, ParticleTypes.CLOUD, 0.12, 0.22);
            } else if (since % 12 == 3 || since % 12 == 4 || since % 12 == 6 || since % 12 == 7) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F + random.nextFloat() * 0.4F);
            }
            if (tick >= GLITCH_LENGTH - 2) { // It catches: a heavy blast of black smoke thrown high, with flames.
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.5F, 0.7F);
                for (int i = 0; i < 14; i++) jet(level, vent, random, ModParticles.VENT_SMOKE.get(), 0.3, 0.5, 0.45, 0.45);
                for (int i = 0; i < 8; i++) jet(level, vent, random, ParticleTypes.FLAME, 0.15, 0.3, 0.2, 0.2);
            }
        }
    }

    private static void jet(ServerLevel level, Vent vent, net.minecraft.util.RandomSource random, double minSpeed, double maxSpeed) {
        jet(level, vent, random, ModParticles.VENT_SMOKE.get(), minSpeed, maxSpeed, 0.15, 0.15);
    }

    private static void jet(ServerLevel level, Vent vent, net.minecraft.util.RandomSource random,
                            net.minecraft.core.particles.ParticleOptions type, double minSpeed, double maxSpeed) {
        jet(level, vent, random, type, minSpeed, maxSpeed, 0.15, 0.15);
    }

    /**
     * One puff squeezed out of the seam: it leaves sideways, away from the build, with a little lift and a little scatter,
     * then slows and drifts until it has gone a few blocks and fades out. A count of zero makes the offsets a direction and
     * the speed their strength.
     */
    private static void jet(ServerLevel level, Vent vent, net.minecraft.util.RandomSource random,
                            net.minecraft.core.particles.ParticleOptions type, double minSpeed, double maxSpeed, double lift, double liftRange) {
        var push = vent.push();
        double across = (random.nextDouble() - 0.5) * 0.5; // Sideways scatter, across the direction of the push.
        double dx = push.x + push.z * across, dz = push.z + push.x * across;
        double speed = minSpeed + random.nextDouble() * (maxSpeed - minSpeed);
        var at = vent.outlet();
        level.sendParticles(type, at.x + (random.nextDouble() - 0.5) * 0.1, at.y + (random.nextDouble() - 0.5) * 0.3,
                at.z + (random.nextDouble() - 0.5) * 0.1, 0, dx, lift + random.nextDouble() * liftRange, dz, speed);
    }
}
