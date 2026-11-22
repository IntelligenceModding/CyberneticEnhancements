package de.artemis.cyberneticenhancements.client;

import com.mojang.blaze3d.platform.NativeImage;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The explored world as a top-down picture. Loaded chunks are painted into 512x512 region images a few at a time, so the
 * cost is a handful of chunks per tick and nothing at all while nothing around you changes. Regions only become GPU
 * textures once the map is looked at, and are kept on disk per world, character and dimension.
 */
public final class WorldMapCache {
    public static final int REGION_CHUNKS = 32, REGION_SIZE = REGION_CHUNKS * 16;
    private static final int MAX_REGIONS = 40, SCAN_BUDGET = 3, QUEUE_LIMIT = 40, SAVE_INTERVAL = 600, SCAN_RADIUS = 10;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "cyberware-map-io");
        thread.setDaemon(true);
        return thread;
    });
    private static final Long2ObjectOpenHashMap<Region> REGIONS = new Long2ObjectOpenHashMap<>();
    private static final LongOpenHashSet ABSENT = new LongOpenHashSet(), LOADING = new LongOpenHashSet();
    private static final ConcurrentLinkedQueue<Loaded> LOADED = new ConcurrentLinkedQueue<>();
    private static final Long2LongOpenHashMap SCANNED = new Long2LongOpenHashMap();
    private static final ArrayDeque<Long> QUEUE = new ArrayDeque<>();
    private static final int[] TOP = new int[256], RGB = new int[256];
    private static final boolean[] WATER = new boolean[256], WALL = new boolean[256];
    private static String key = "";
    private static Path dir;
    private static long ticks;

    static { SCANNED.defaultReturnValue(-1L); }

    private WorldMapCache() {}

    public static final class Region {
        public final int rx, rz;
        private final NativeImage image;
        private DynamicTexture texture;
        private ResourceLocation id;
        private boolean upload, dirty, smooth;
        private String tag = "r";
        private long used = System.nanoTime();

        private Region(int rx, int rz, NativeImage image) {
            this.rx = rx; this.rz = rz; this.image = image;
        }

        /** The texture to draw, created on first use and re-uploaded only when scanning changed something. */
        public ResourceLocation texture(boolean smooth) {
            used = System.nanoTime();
            var textures = Minecraft.getInstance().getTextureManager();
            if (texture == null) {
                texture = new DynamicTexture(image);
                id = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "map/" + tag + "." + rx + "." + rz);
                textures.register(id, texture);
                // Repeating would blend the far edge of the picture into this one when it is drawn smoothed, as a line of stray pixels.
                texture.bind();
                com.mojang.blaze3d.platform.GlStateManager._texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
                com.mojang.blaze3d.platform.GlStateManager._texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
                this.smooth = !smooth;
                upload = false;
            }
            if (this.smooth != smooth) {
                texture.setFilter(smooth, false);
                this.smooth = smooth;
            }
            if (upload) {
                texture.upload();
                upload = false;
            }
            return id;
        }
    }

    // ---- other dimensions: read-only pictures of what was saved, drawn without being in them ----

    private static final Long2ObjectOpenHashMap<Region> OTHER = new Long2ObjectOpenHashMap<>();
    private static final LongOpenHashSet OTHER_ABSENT = new LongOpenHashSet();
    private static String otherKey = "";
    private static String viewDimension = "";
    private static int viewLayer = Integer.MIN_VALUE;

    private static int layerOf(java.io.File folder) {
        try { return Integer.parseInt(folder.getName().substring(1)); } catch (RuntimeException exception) { return Integer.MIN_VALUE; }
    }

    /** The lowest and highest layer that has been mapped in another dimension, or null when it is not a layered one. */
    public static int[] savedLayers(String dimension) {
        var base = base(dimension);
        if (base == null) return null;
        var layers = base.toFile().listFiles(file -> file.isDirectory() && file.getName().startsWith("y") && layerOf(file) != Integer.MIN_VALUE);
        if (layers == null || layers.length == 0) return null;
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (var file : layers) { low = Math.min(low, layerOf(file)); high = Math.max(high, layerOf(file)); }
        // Where the surface was mapped too, it is one more step above the highest layer.
        return new int[] {low, hasSurface(base) ? high + LAYER_SPAN : high};
    }

    private static boolean hasSurface(Path base) {
        var names = base.toFile().list((folder, name) -> name.endsWith(".png"));
        return names != null && names.length > 0;
    }

    /** The layer of another dimension that is on show. */
    public static int viewedLayer(String dimension) {
        var path = saved(dimension);
        if (path == null) return Integer.MIN_VALUE;
        return path.getFileName().toString().startsWith("y") ? layerOf(path.toFile()) : SURFACE;
    }

    public static void viewLayer(String dimension, int layer) {
        viewDimension = dimension;
        var span = savedLayers(dimension);
        // The top step of the slider is the surface, where there is one.
        viewLayer = span != null && layer >= span[1] && hasSurface(base(dimension)) ? SURFACE : layer;
        releaseView();
    }

    private static Path base(String dimension) {
        String scope = CompassPreferences.scope();
        if (scope.isEmpty()) return null;
        return FMLPaths.GAMEDIR.get().resolve(CyberneticEnhancements.MOD_ID).resolve("map").resolve(scope)
                .resolve(dimension.replace(':', '_').replace('/', '_'));
    }

    private static Path saved(String dimension) {
        var base = base(dimension);
        if (base == null) return null;
        // A dimension with caves keeps a folder per layer next to its surface: the one picked on the slider, else the surface, else the one written to last.
        var layers = base.toFile().listFiles(file -> file.isDirectory() && file.getName().startsWith("y"));
        if (layers == null || layers.length == 0) return base;
        if (dimension.equals(viewDimension) && viewLayer != Integer.MIN_VALUE) return viewLayer == SURFACE ? base : base.resolve("y" + viewLayer);
        if (hasSurface(base)) return base;
        java.io.File latest = null;
        for (var file : layers) if (latest == null || file.lastModified() > latest.lastModified()) latest = file;
        return latest.toPath();
    }

    /** Whether anything was ever mapped there. */
    public static boolean hasSaved(String dimension) {
        var path = saved(dimension);
        if (path == null) return false;
        var names = path.toFile().list((folder, name) -> name.endsWith(".png"));
        return names != null && names.length > 0;
    }

    /** The middle of what was mapped there, in blocks, or null when nothing was. */
    public static double[] center(String dimension) {
        var path = saved(dimension);
        var names = path == null ? null : path.toFile().list((folder, name) -> name.endsWith(".png"));
        if (names == null) return null;
        double sx = 0, sz = 0; int count = 0;
        for (var name : names) {
            var parts = name.split("\\.");
            try {
                sx += (Integer.parseInt(parts[1]) + 0.5) * REGION_SIZE; sz += (Integer.parseInt(parts[2]) + 0.5) * REGION_SIZE; count++;
            } catch (RuntimeException ignored) {
            }
        }
        return count == 0 ? null : new double[] {sx / count, sz / count};
    }

    /** A region of another dimension as it was last saved; the current dimension keeps using {@link #peek}. */
    public static Region peek(String dimension, int rx, int rz) {
        String wanted = CompassPreferences.scope() + "/" + dimension;
        if (!wanted.equals(otherKey)) { releaseView(); otherKey = wanted; }
        long id = pack(rx, rz);
        var region = OTHER.get(id);
        if (region != null) { region.used = System.nanoTime(); return region; }
        var path = saved(dimension);
        if (path == null || OTHER_ABSENT.contains(id)) return null;
        var image = read(path.resolve(file(rx, rz)));
        if (image == null) { OTHER_ABSENT.add(id); return null; }
        region = new Region(rx, rz, image);
        region.tag = "o";
        region.upload = true;
        OTHER.put(id, region);
        if (OTHER.size() > MAX_REGIONS) {
            for (var old : OTHER.values()) if (old != region) release(old, false);
            OTHER.clear(); OTHER.put(id, region);
        }
        return region;
    }

    private static final Long2ObjectOpenHashMap<Region> SURFACE_VIEW = new Long2ObjectOpenHashMap<>();
    private static final LongOpenHashSet SURFACE_ABSENT = new LongOpenHashSet();
    private static String surfaceKey = "";

    /**
     * A region of the surface of the dimension being mapped, as last saved, for a small map that stays on the surface while the
     * player is down in a cave. Null where the dimension has no surface or nothing was mapped there.
     */
    public static Region surface(int rx, int rz) {
        var level = Minecraft.getInstance().level;
        if (level == null || level.dimensionType().hasCeiling()) return null;
        String dimension = level.dimension().location().toString(), wanted = CompassPreferences.scope() + "/" + dimension;
        if (!wanted.equals(surfaceKey)) {
            for (var region : SURFACE_VIEW.values()) release(region, false);
            SURFACE_VIEW.clear(); SURFACE_ABSENT.clear(); surfaceKey = wanted;
        }
        long id = pack(rx, rz);
        var region = SURFACE_VIEW.get(id);
        if (region != null) { region.used = System.nanoTime(); return region; }
        var base = base(dimension);
        if (base == null || SURFACE_ABSENT.contains(id)) return null;
        var image = read(base.resolve(file(rx, rz)));
        if (image == null) { SURFACE_ABSENT.add(id); return null; }
        region = new Region(rx, rz, image);
        region.tag = "s";
        region.upload = true;
        SURFACE_VIEW.put(id, region);
        if (SURFACE_VIEW.size() > MAX_REGIONS) {
            for (var old : SURFACE_VIEW.values()) if (old != region) release(old, false);
            SURFACE_VIEW.clear(); SURFACE_VIEW.put(id, region);
        }
        return region;
    }

    /** Lets the surface pictures be read again, as when the player has been up there mapping. */
    public static void refreshSurface() { surfaceKey = ""; }

    /** Drops the other dimension's pictures; they are read again from disk if it is looked at again. */
    public static void releaseView() {
        for (var region : OTHER.values()) release(region, false);
        OTHER.clear(); OTHER_ABSENT.clear(); otherKey = "";
    }

    private record Loaded(String key, int rx, int rz, NativeImage image) {}

    private static long pack(int rx, int rz) { return ChunkPos.asLong(rx, rz); }

    private static String file(int rx, int rz) { return "r." + rx + "." + rz + ".png"; }

    /** A region for drawing: only what is in memory, with anything on disk requested in the background. */
    public static Region peek(int rx, int rz) {
        adopt();
        long id = pack(rx, rz);
        var region = REGIONS.get(id);
        if (region != null) { region.used = System.nanoTime(); return region; }
        if (dir == null || ABSENT.contains(id) || LOADING.contains(id)) return null;
        if (!Files.exists(dir.resolve(file(rx, rz)))) { ABSENT.add(id); return null; }
        LOADING.add(id);
        Path path = dir.resolve(file(rx, rz));
        String owner = key;
        IO.execute(() -> LOADED.add(new Loaded(owner, rx, rz, read(path))));
        return null;
    }

    private static NativeImage read(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            var image = NativeImage.read(in);
            if (image.getWidth() == REGION_SIZE && image.getHeight() == REGION_SIZE) return image;
            image.close();
        } catch (IOException | RuntimeException ignored) {
        }
        return null;
    }

    private static void adopt() {
        Loaded loaded;
        while ((loaded = LOADED.poll()) != null) {
            long id = pack(loaded.rx, loaded.rz);
            if (loaded.key.equals(key)) LOADING.remove(id);
            if (loaded.image == null) { if (loaded.key.equals(key)) ABSENT.add(id); continue; }
            if (!loaded.key.equals(key) || REGIONS.containsKey(id)) { loaded.image.close(); continue; }
            var region = new Region(loaded.rx, loaded.rz, loaded.image);
            region.upload = true;
            REGIONS.put(id, region);
            evict(id);
        }
    }

    /** The scanner's region: read from disk right away if it exists, so that what is painted merges with what was there. */
    private static Region region(int rx, int rz) {
        long id = pack(rx, rz);
        var region = REGIONS.get(id);
        if (region != null) return region;
        NativeImage image = dir == null ? null : read(dir.resolve(file(rx, rz)));
        if (image == null) image = new NativeImage(NativeImage.Format.RGBA, REGION_SIZE, REGION_SIZE, true);
        region = new Region(rx, rz, image);
        REGIONS.put(id, region);
        ABSENT.remove(id);
        evict(id);
        return region;
    }

    private static void evict(long keep) {
        while (REGIONS.size() > MAX_REGIONS) {
            long oldest = 0; long age = Long.MAX_VALUE; boolean found = false;
            for (var entry : REGIONS.long2ObjectEntrySet()) {
                if (entry.getLongKey() == keep || entry.getValue().used >= age) continue;
                age = entry.getValue().used; oldest = entry.getLongKey(); found = true;
            }
            if (!found) return;
            long evicted = oldest;
            release(REGIONS.remove(evicted), false);
            // What was in it will be painted again if it comes back into range.
            SCANNED.keySet().removeIf(chunk -> pack(Math.floorDiv(ChunkPos.getX(chunk), REGION_CHUNKS), Math.floorDiv(ChunkPos.getZ(chunk), REGION_CHUNKS)) == evicted);
        }
    }

    private static void save(Region region, boolean sync) {
        if (!region.dirty || dir == null) return;
        region.dirty = false;
        Path target = dir.resolve(file(region.rx, region.rz));
        var copy = new NativeImage(NativeImage.Format.RGBA, REGION_SIZE, REGION_SIZE, false);
        copy.copyFrom(region.image);
        Runnable job = () -> {
            try {
                Files.createDirectories(target.getParent());
                Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
                copy.writeToFile(temporary);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | RuntimeException ignored) {
            } finally {
                copy.close();
            }
        };
        if (sync) job.run(); else IO.execute(job);
    }

    private static void release(Region region, boolean sync) {
        save(region, sync);
        if (region.texture != null) Minecraft.getInstance().getTextureManager().release(region.id);
        else region.image.close();
    }

    /** Writes everything unsaved and drops every texture; used when the world, character or dimension changes. */
    public static void flush() {
        adopt();
        releaseView();
        for (var region : REGIONS.values()) release(region, true);
        REGIONS.clear(); ABSENT.clear(); LOADING.clear(); SCANNED.clear(); QUEUE.clear();
        key = ""; dir = null;
    }

    /**
     * Under a roof (the Nether) there is no top-down surface, so the map is a slice: every chunk is read from the same
     * height, down, and each layer of {@link #LAYER_SPAN} blocks is kept as its own set of pictures. The layer follows the
     * player unless it was picked by hand.
     */
    public static final int LAYER_SPAN = 8;
    /** The layer of a dimension with a sky when it is the surface that is on show, not a slice of it. */
    public static final int SURFACE = Integer.MAX_VALUE / 2;
    private static int layer = Integer.MIN_VALUE;
    private static boolean autoLayer = true, layered;
    private static String layerDimension = "";

    /** Whether the dimension being mapped is a cave, so that it has layers to choose from. */
    public static boolean layered() { return layered; }

    /** The lowest block of the layer on show; it reaches {@link #LAYER_SPAN} blocks up from there. */
    public static int layer() { return layer; }

    public static boolean autoLayer() { return autoLayer; }

    public static void autoLayer(boolean on) { autoLayer = on; }

    /** Moves the layer by whole steps and takes it off automatic. */
    public static void shiftLayer(int steps) {
        var level = Minecraft.getInstance().level;
        if (level == null || layer == Integer.MIN_VALUE) return;
        setLayer((layer == SURFACE ? layerHigh() : layer) + steps * LAYER_SPAN);
    }

    /** The lowest and highest layer there is, for a slider to span. */
    public static int layerLow() { return Minecraft.getInstance().level.getMinBuildHeight(); }

    /** Where there is a surface to look down on, the slider has one more step above the highest layer for it. */
    public static int layerHigh() {
        var level = Minecraft.getInstance().level;
        return clampLayer(level, Integer.MAX_VALUE / 2) + (level.dimensionType().hasCeiling() ? 0 : LAYER_SPAN);
    }

    /** Picks a layer by hand. */
    public static void setLayer(int value) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        autoLayer = false;
        layer = !level.dimensionType().hasCeiling() && value > clampLayer(level, Integer.MAX_VALUE / 2) ? SURFACE : clampLayer(level, value);
    }

    private static int clampLayer(Level level, int value) {
        int low = level.getMinBuildHeight(), high = low + level.dimensionType().logicalHeight() - LAYER_SPAN;
        return Mth.clamp(Math.floorDiv(value, LAYER_SPAN) * LAYER_SPAN, low, Math.max(low, high / LAYER_SPAN * LAYER_SPAN));
    }

    public static void tick() {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) return;
        String scope = CompassPreferences.scope();
        if (scope.isEmpty()) return;
        String dimension = level.dimension().location().toString();
        layered = true;
        boolean roofed = level.dimensionType().hasCeiling();
        if (!dimension.equals(layerDimension)) { layerDimension = dimension; layer = Integer.MIN_VALUE; autoLayer = true; }
        int feet = player.getBlockY();
        if (roofed) {
            // Follows the player with some give, so standing on a ledge does not flip the map back and forth.
            if (layer == Integer.MIN_VALUE || autoLayer && (feet < layer - 3 || feet >= layer + LAYER_SPAN + 3)) layer = clampLayer(level, feet);
        } else if (layer == Integer.MIN_VALUE || autoLayer) {
            // With a sky, the surface is shown until the player is well below it, and a cave layer from then on until they are back near it.
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ());
            boolean under = layer == SURFACE || layer == Integer.MIN_VALUE ? feet < top - 10 : feet < top - 3;
            if (!under) layer = SURFACE;
            else if (layer == SURFACE || layer == Integer.MIN_VALUE || feet < layer - 3 || feet >= layer + LAYER_SPAN + 3) layer = clampLayer(level, feet);
        }
        String current = scope + "/" + dimension + (layer == SURFACE ? "" : "/y" + layer);
        if (!current.equals(key)) {
            flush();
            key = current;
            dir = FMLPaths.GAMEDIR.get().resolve(CyberneticEnhancements.MOD_ID).resolve("map").resolve(scope)
                    .resolve(dimension.replace(':', '_').replace('/', '_'));
            if (layer != SURFACE) dir = dir.resolve("y" + layer);
        }
        adopt();
        ticks++;
        if (ticks % 10 == 0) refill(level, player.chunkPosition(), minecraft.options.getEffectiveRenderDistance());
        for (int budget = SCAN_BUDGET; budget > 0 && !QUEUE.isEmpty(); budget--) {
            long chunk = QUEUE.poll();
            if (scan(level, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), layer)) SCANNED.put(chunk, ticks);
        }
        if (ticks % SAVE_INTERVAL == 0) for (var region : REGIONS.values()) save(region, false);
    }

    private record Candidate(long chunk, int score) {}

    /** Nearest unmapped chunks first, then a slow refresh of what is close, so building and flooding show up. */
    private static void refill(Level level, ChunkPos center, int renderDistance) {
        QUEUE.clear();
        int radius = Math.min(SCAN_RADIUS, renderDistance);
        var due = new ArrayList<Candidate>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = center.x + dx, cz = center.z + dz, distance = dx * dx + dz * dz;
                long chunk = ChunkPos.asLong(cx, cz);
                long last = SCANNED.get(chunk);
                boolean wanted = last < 0 || distance <= 9 && ticks - last >= 100 || distance <= 64 && ticks - last >= 1200;
                if (wanted && level.getChunkSource().hasChunk(cx, cz)) due.add(new Candidate(chunk, (last < 0 ? 0 : 1000) + distance));
            }
        }
        due.sort(Comparator.comparingInt(Candidate::score));
        for (int index = 0; index < Math.min(QUEUE_LIMIT, due.size()); index++) QUEUE.add(due.get(index).chunk());
    }

    private static boolean scan(Level level, int cx, int cz, int layer) {
        if (!(level.getChunk(cx, cz, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) return false;
        // Biome tints blend across chunk borders; with a neighbour still missing the border pixels come out in the default colour.
        for (int dz = -1; dz <= 1; dz++)
            for (int dx = -1; dx <= 1; dx++)
                if ((dx != 0 || dz != 0) && !level.getChunkSource().hasChunk(cx + dx, cz + dz)) return false;
        boolean ceiling = layer != SURFACE;
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
        var pos = new BlockPos.MutableBlockPos();
        var tints = net.minecraft.client.Minecraft.getInstance().getBlockColors();
        int baseX = cx << 4, baseZ = cz << 4;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int index = lz << 4 | lx, wx = baseX + lx, wz = baseZ + lz;
                int y;
                boolean wall = false;
                if (ceiling) {
                    // Under a roof: look down from the top of the layer. Open space is searched for through a little rock, the
                    // floor under it is what shows, and rock that is too thick to see through is drawn as a dark wall.
                    y = Mth.clamp(layer + LAYER_SPAN - 1, minY + 1, maxY - 1);
                    var probe = chunk.getBlockState(pos.set(wx, y, wz));
                    for (int rock = 0; rock < 12 && y > minY && !(probe.isAir() || probe.getMapColor(level, pos) == MapColor.NONE); rock++) probe = chunk.getBlockState(pos.set(wx, --y, wz));
                    wall = !(probe.isAir() || probe.getMapColor(level, pos) == MapColor.NONE);
                } else {
                    y = Mth.clamp(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz), minY, maxY - 1);
                }
                var state = chunk.getBlockState(pos.set(wx, y, wz));
                while (y > minY && (state.isAir() || state.getMapColor(level, pos) == MapColor.NONE)) state = chunk.getBlockState(pos.set(wx, --y, wz));
                if (y <= minY) { RGB[index] = -1; TOP[index] = minY; WATER[index] = false; continue; }
                // Thin snow is not solid, so the heightmap looks straight through it; the snow is what the ground looks like.
                if (!ceiling && y + 1 < maxY) {
                    var above = chunk.getBlockState(pos.set(wx, y + 1, wz));
                    if (above.is(Blocks.SNOW)) state = above;
                }
                TOP[index] = y;
                WALL[index] = wall;
                var biome = level.getBiome(pos).value();
                boolean water = state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.WATER);
                int color;
                if (water) {
                    int depth = 0, bottom = y;
                    while (bottom > minY && depth < 16 && !chunk.getBlockState(pos.set(wx, bottom - 1, wz)).getFluidState().isEmpty()) { bottom--; depth++; }
                    var bed = chunk.getBlockState(pos.set(wx, bottom - 1, wz));
                    int bedColor = bed.getMapColor(level, pos).col;
                    int tint = tints.getColor(state, level, pos.set(wx, y, wz), 0);
                    color = mix(bedColor, tint != -1 ? tint & 0xFFFFFF : biome.getWaterColor() & 0xFFFFFF, 0.35F + 0.6F * Math.min(1F, depth / 10F));
                } else {
                    var look = look(state);
                    color = look.color() >= 0 ? look.color() : state.getMapColor(level, pos).col;
                    // The game's own tint (grass, foliage, vines, lily pads...), multiplied onto the texture as the renderer does.
                    if (look.tint() >= 0) {
                        int tint = tints.getColor(state, level, pos.set(wx, y, wz), look.tint());
                        if (tint != -1) color = multiply(color, tint);
                    }
                }
                if (WALL[index]) {
                    // Solid rock is plain dark grey whatever it is made of, so it never reads as a floor; only its texture's brightness is kept.
                    int grey = 0x20 + ((color >> 16 & 255) * 3 + (color >> 8 & 255) * 6 + (color & 255)) / 10 / 6;
                    color = grey << 16 | grey << 8 | Math.min(255, grey + 6);
                }
                RGB[index] = color; WATER[index] = water;
            }
        }
        var region = region(Math.floorDiv(cx, REGION_CHUNKS), Math.floorDiv(cz, REGION_CHUNKS));
        int originX = Math.floorMod(cx, REGION_CHUNKS) * 16, originY = Math.floorMod(cz, REGION_CHUNKS) * 16;
        boolean changed = false;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int index = lz << 4 | lx, argb = 0;
                if (RGB[index] >= 0) {
                    int height = TOP[index];
                    float light = 1F;
                    if (!WATER[index]) {
                        int north = lz > 0 ? TOP[index - 16] : edge(level, ceiling, cx, cz - 1, lx, 15, height);
                        int west = lx > 0 ? TOP[index - 1] : edge(level, ceiling, cx - 1, cz, 15, lz, height);
                        light += Mth.clamp(((height - north) + 0.7F * (height - west)) * 0.07F, -0.32F, 0.3F);
                    }
                    if (ceiling && !WALL[index]) light *= 1F - Math.min(0.45F, (layer + LAYER_SPAN - 1 - height) * 0.012F);
                    int color = RGB[index];
                    int red = Mth.clamp((int) ((color >> 16 & 255) * light), 0, 255);
                    int green = Mth.clamp((int) ((color >> 8 & 255) * light), 0, 255);
                    int blue = Mth.clamp((int) ((color & 255) * light), 0, 255);
                    argb = 0xFF000000 | blue << 16 | green << 8 | red;
                }
                if (region.image.getPixelRGBA(originX + lx, originY + lz) != argb) {
                    region.image.setPixelRGBA(originX + lx, originY + lz, argb);
                    changed = true;
                }
            }
        }
        if (changed) { region.upload = true; region.dirty = true; }
        return true;
    }

    private static int edge(Level level, boolean ceiling, int cx, int cz, int lx, int lz, int own) {
        if (ceiling || !(level.getChunk(cx, cz, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) return own;
        return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
    }

    private record Look(int color, int tint) {}

    private static final java.util.HashMap<BlockState, Look> LOOKS = new java.util.HashMap<>();

    /** The average colour of the texture a block shows from above, and whether that face is biome-tinted. */
    private static Look look(BlockState state) {
        return LOOKS.computeIfAbsent(state, WorldMapCache::sample);
    }

    private static Look sample(BlockState state) {
        try {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var model = minecraft.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
            var random = net.minecraft.util.RandomSource.create(42L);
            var quads = model.getQuads(state, net.minecraft.core.Direction.UP, random);
            if (quads.isEmpty()) quads = model.getQuads(state, null, random).stream().filter(quad -> quad.getDirection() == net.minecraft.core.Direction.UP).toList();
            var sprite = quads.isEmpty() ? model.getParticleIcon() : quads.get(0).getSprite();
            int tint = !quads.isEmpty() && quads.get(0).isTinted() ? quads.get(0).getTintIndex() : -1;
            var image = sprite.contents().getOriginalImage();
            long red = 0, green = 0, blue = 0, weight = 0;
            for (int py = 0; py < Math.min(image.getHeight(), sprite.contents().height()); py++) {
                for (int px = 0; px < image.getWidth(); px++) {
                    int pixel = image.getPixelRGBA(px, py), alpha = pixel >>> 24;
                    if (alpha < 16) continue;
                    red += (pixel & 255) * alpha; green += (pixel >> 8 & 255) * alpha; blue += (pixel >> 16 & 255) * alpha; weight += alpha;
                }
            }
            if (weight == 0) return new Look(-1, tint);
            return new Look((int) (red / weight) << 16 | (int) (green / weight) << 8 | (int) (blue / weight), tint);
        } catch (RuntimeException exception) {
            return new Look(-1, -1);
        }
    }

    private static int multiply(int color, int tint) {
        return (color >> 16 & 255) * (tint >> 16 & 255) / 255 << 16 | (color >> 8 & 255) * (tint >> 8 & 255) / 255 << 8 | (color & 255) * (tint & 255) / 255;
    }

    /** Resource reloads change the textures, so what was sampled has to be looked at again. */
    public static void invalidateLooks() { LOOKS.clear(); }

    private static int mix(int first, int second, float amount) {
        int red = (int) ((first >> 16 & 255) * (1 - amount) + (second >> 16 & 255) * amount);
        int green = (int) ((first >> 8 & 255) * (1 - amount) + (second >> 8 & 255) * amount);
        int blue = (int) ((first & 255) * (1 - amount) + (second & 255) * amount);
        return red << 16 | green << 8 | blue;
    }

    /** Whether anything has been mapped in this dimension yet, so an empty map can say so. */
    public static boolean empty() { return REGIONS.isEmpty() && LOADING.isEmpty(); }

    public static List<Region> loaded() { return List.copyOf(REGIONS.values()); }
}
