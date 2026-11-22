package de.artemis.cyberneticenhancements.common.world;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Finds the nearest Med Tec AV of one particular kind, which the vanilla locate command cannot tell apart.
 * A kind is the name of a structure in the Med Tec AV structure set, of a template in one of their start pools, or of a biome
 * a site stands in, so a biome-specific site is told apart just by adding it as its own structure or pool element, with
 * nothing to register here.
 * It walks the structure set's placement grid outwards from the player and reads each site's saved pieces. Generating the
 * chunks that hold the structure starts is slow, so the walk runs in short slices across server ticks.
 */
public final class MedTecAvLocator {
    private static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET,
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "med_tec_av"));
    /** The kind that matches every site. */
    public static final String ANY = "any";
    /** How many placement cells out the search goes before it gives up. */
    private static final int MAX_RINGS = 8;
    /** The share of every tick the search may use, in milliseconds, so the game keeps running while it works. */
    private static final long SLICE_MILLIS = 15;

    private MedTecAvLocator() {}

    /** Every kind this world's data knows, in the order it suggests them: any, then each structure, each template and each biome. */
    public static Set<String> kinds(ServerLevel level) {
        var kinds = new LinkedHashSet<String>();
        kinds.add(ANY);
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(SET);
        if (set == null) return kinds;
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (var entry : set.structures()) {
            var key = structures.getKey(entry.structure().value());
            if (key != null) kinds.add(key.getPath());
        }
        for (var entry : set.structures()) {
            if (!(entry.structure().value() instanceof MedTecAvStructure site)) continue;
            for (var element : site.startPool().value().getShuffledTemplates(RandomSource.create(0))) {
                var template = template(element);
                if (template != null) kinds.add(template.getPath());
            }
        }
        for (var entry : set.structures())
            entry.structure().value().biomes().forEach(biome -> biome.unwrapKey().ifPresent(key -> kinds.add(key.location().getPath())));
        return kinds;
    }

    /** The template a pool element places, or null for one that does not place a single template. */
    private static @Nullable ResourceLocation template(StructurePoolElement element) {
        return StructurePoolElement.CODEC.encodeStart(JsonOps.INSTANCE, element).result()
                .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
                .filter(json -> json.has("location")).map(json -> ResourceLocation.tryParse(json.get("location").getAsString())).orElse(null);
    }

    private static boolean matches(String kind, String structure, String biome, List<PoolElementStructurePiece> pieces) {
        if (kind.equals(ANY) || kind.equals(structure) || kind.equals(biome)) return true;
        for (var piece : pieces) {
            var template = template(piece.getElement());
            if (template != null && template.getPath().equals(kind)) return true;
        }
        return false;
    }

    private record Cell(int dx, int dz, int ring) {}

    /** One walk outwards over the placement grid, resumable between ticks. */
    private static final class Search {
        private final ServerLevel level;
        private final BlockPos origin;
        private final String kind;
        private final StructureSet set;
        private final RandomSpreadStructurePlacement placement;
        private final List<Cell> cells = new ArrayList<>();
        private final int originX, originZ;
        private int next;
        private BlockPos best;
        private double bestDistance = Double.MAX_VALUE;

        Search(ServerLevel level, BlockPos origin, String kind, StructureSet set, RandomSpreadStructurePlacement placement) {
            this.level = level;
            this.origin = origin;
            this.kind = kind;
            this.set = set;
            this.placement = placement;
            this.originX = Math.floorDiv(origin.getX() >> 4, placement.spacing());
            this.originZ = Math.floorDiv(origin.getZ() >> 4, placement.spacing());
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

        /** The biome the site stands in. The chunk's own biomes are not made yet at this stage, so it asks the generator, as the structure did. */
        private String biomeAt(BlockPos pos) {
            var generator = level.getChunkSource().getGenerator();
            return generator.getBiomeSource().getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2,
                    level.getChunkSource().randomState().sampler()).unwrapKey().map(key -> key.location().getPath()).orElse("");
        }

        private void inspect(Cell cell) {
            var state = level.getChunkSource().getGeneratorState();
            int spacing = placement.spacing();
            ChunkPos chunk = placement.getPotentialStructureChunk(state.getLevelSeed(), (originX + cell.dx()) * spacing, (originZ + cell.dz()) * spacing);
            if (!placement.isStructureChunk(state, chunk.x, chunk.z)) return;
            var access = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
            var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            for (var entry : set.structures()) {
                var start = level.structureManager().getStartForStructure(SectionPos.bottomOf(access), entry.structure().value(), access);
                if (start == null || !start.isValid()) continue;
                var key = structures.getKey(entry.structure().value());
                var builds = start.getPieces().stream().filter(PoolElementStructurePiece.class::isInstance).map(PoolElementStructurePiece.class::cast).toList();
                if (builds.isEmpty()) continue;
                BlockPos centre = builds.get(0).getBoundingBox().getCenter();
                if (!matches(kind, key == null ? "" : key.getPath(), biomeAt(centre), builds)) continue;
                double distance = centre.distSqr(origin);
                if (distance < bestDistance) { best = centre; bestDistance = distance; }
            }
        }
    }

    private static @Nullable Search start(ServerLevel level, BlockPos origin, String kind) {
        if (!kinds(level).contains(kind)) return null;
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(SET);
        if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement spread)) return null;
        return new Search(level, origin, kind, set, spread);
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
