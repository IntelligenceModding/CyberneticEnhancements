package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Finds the nearest shipping container of one particular kind, which the vanilla locate command cannot tell apart.
 * It walks the structure's placement grid outwards from the player and reads each container's saved decisions.
 * Generating the chunks that hold the structure starts is slow, so the walk runs in short slices across server ticks.
 */
public final class ShippingContainerLocator {
    private static final ResourceKey<Structure> CONTAINER = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "shipping_container"));
    /** How many placement cells out the search goes before it gives up. */
    private static final int MAX_RINGS = 12;
    /** The share of every tick the search may use, in milliseconds, so the game keeps running while it works. */
    private static final long SLICE_MILLIS = 15;
    /** A hull this far above the sea floor line counts as lying on the shore rather than on the sea floor. */
    private static final int SHORE_MARGIN = 2;

    /** Every kind the command knows, in the order it suggests them. */
    public static final Map<String, Predicate<Found>> KINDS = new LinkedHashMap<>();

    /** A generated container and the sea level it was placed against. */
    public record Found(ShippingContainerPiece piece, int seaLevel) {
        boolean beached() { return piece.getBoundingBox().minY() >= seaLevel - SHORE_MARGIN; }
    }

    static {
        KINDS.put("any", found -> true);
        for (var damage : ShippingContainerPiece.Damage.values()) KINDS.put(name(damage), found -> found.piece().damage() == damage);
        for (var cargo : ShippingContainerPiece.Cargo.values()) KINDS.put(name(cargo), found -> found.piece().cargo() == cargo);
        for (var growth : ShippingContainerPiece.Growth.values()) KINDS.put(name(growth), found -> found.piece().growth() == growth);
        KINDS.put("flooded", found -> found.piece().flooded() && !found.beached());
        KINDS.put("sealed", found -> !found.piece().flooded());
        KINDS.put("beached", Found::beached);
        KINDS.put("underwater", found -> !found.beached());
        KINDS.put("cargo", found -> found.piece().hasCargo());
    }

    private ShippingContainerLocator() {}

    private static String name(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }

    private record Cell(int dx, int dz, int ring) {}

    /** One walk outwards over the placement grid, resumable between ticks. */
    private static final class Search {
        private final ServerLevel level;
        private final BlockPos origin;
        private final Predicate<Found> wanted;
        private final Holder<Structure> holder;
        private final RandomSpreadStructurePlacement placement;
        private final List<Cell> cells = new ArrayList<>();
        private final int originX, originZ, sea;
        private int next;
        private BlockPos best;
        private double bestDistance = Double.MAX_VALUE;

        Search(ServerLevel level, BlockPos origin, Predicate<Found> wanted, Holder<Structure> holder, RandomSpreadStructurePlacement placement) {
            this.level = level;
            this.origin = origin;
            this.wanted = wanted;
            this.holder = holder;
            this.placement = placement;
            this.originX = Math.floorDiv(origin.getX() >> 4, placement.spacing());
            this.originZ = Math.floorDiv(origin.getZ() >> 4, placement.spacing());
            this.sea = level.getChunkSource().getGenerator().getSeaLevel();
            for (int dx = -MAX_RINGS; dx <= MAX_RINGS; dx++)
                for (int dz = -MAX_RINGS; dz <= MAX_RINGS; dz++) cells.add(new Cell(dx, dz, Math.max(Math.abs(dx), Math.abs(dz))));
            cells.sort(Comparator.comparingInt(Cell::ring));
        }

        /** Works for at most the given time; returns true once the search is over. */
        boolean step(long millis) {
            long end = Util.getMillis() + millis;
            do {
                if (next >= cells.size()) return true;
                var cell = cells.get(next);
                // A hit one ring out can still be beaten by a nearer one on the next, so a ring is finished before stopping.
                if (best != null && cell.ring() > 1 && cell.ring() != cells.get(next - 1).ring()) return true;
                next++;
                inspect(cell);
            } while (Util.getMillis() < end);
            return false;
        }

        private void inspect(Cell cell) {
            var state = level.getChunkSource().getGeneratorState();
            int spacing = placement.spacing();
            ChunkPos chunk = placement.getPotentialStructureChunk(state.getLevelSeed(), (originX + cell.dx()) * spacing, (originZ + cell.dz()) * spacing);
            if (!placement.isStructureChunk(state, chunk.x, chunk.z)) return;
            var access = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
            var start = level.structureManager().getStartForStructure(SectionPos.bottomOf(access), holder.value(), access);
            if (start == null || !start.isValid()) return;
            for (var piece : start.getPieces()) {
                if (!(piece instanceof ShippingContainerPiece hull) || !wanted.test(new Found(hull, sea))) continue;
                BlockPos centre = hull.getBoundingBox().getCenter();
                double distance = centre.distSqr(origin);
                if (distance < bestDistance) { best = centre; bestDistance = distance; }
            }
        }
    }

    private static @Nullable Search start(ServerLevel level, BlockPos origin, String kind) {
        var wanted = KINDS.get(kind);
        if (wanted == null) return null;
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(CONTAINER).orElse(null);
        if (holder == null) return null;
        for (var placement : level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder))
            if (placement instanceof RandomSpreadStructurePlacement spread) return new Search(level, origin, wanted, holder, spread);
        return null;
    }

    /** Searches in slices over the next ticks and then hands the centre of the nearest match, or null, to the callback. */
    public static void locate(ServerLevel level, BlockPos origin, String kind, Consumer<@Nullable BlockPos> done) {
        var search = start(level, origin, kind);
        if (search == null) { done.accept(null); return; }
        var server = level.getServer();
        Runnable[] slice = new Runnable[1];
        slice[0] = () -> {
            if (search.step(SLICE_MILLIS)) done.accept(search.best);
            else server.tell(new TickTask(server.getTickCount() + 1, slice[0]));
        };
        server.tell(new TickTask(server.getTickCount() + 1, slice[0]));
    }

    /** The whole search in one go, for tests; commands use {@link #locate} so the game keeps ticking. */
    public static @Nullable BlockPos nearest(ServerLevel level, BlockPos origin, String kind) {
        var search = start(level, origin, kind);
        if (search == null) return null;
        while (!search.step(Long.MAX_VALUE / 4)) { /* runs to the end */ }
        return search.best;
    }
}
