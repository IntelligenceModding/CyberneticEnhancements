package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.blockentity.CrateBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * A freight container that washed up: a template hull plus, chosen once and saved, how badly it is broken, what it carries,
 * what has grown inside and whether the sea got in. Every decision is a pure function of the saved seed and the position,
 * so a container comes out the same whichever chunk is generated first.
 */
public final class ShippingContainerPiece extends TemplateStructurePiece {
    public static final int TEMPLATES = 10;
    /** The template is 7 wide, 7 tall and 28 long; this cell stays put under rotation, so the footprint is centred on it. */
    public static final BlockPos PIVOT = new BlockPos(3, 0, 13);
    private static final int LENGTH = 28;

    public enum Damage { INTACT, SPLIT, HOLE }

    /** What the crates hold; the contents of each kind are in {@link ShippingContainerCargo}. */
    public enum Cargo { FOOD, METALS, ELECTRONICS, WEAPONS, MEDICAL, COMPONENTS }

    public enum Growth { NONE, SEAGRASS, MOSS, OVERGROWN, GLOW_LICHEN }

    private final int variant;
    private final long seed;
    private final Damage damage;
    private final Cargo cargo;
    private final Growth growth;
    private final boolean flooded;
    private final boolean salvage;
    /** 0 for a container that stands upright, +1 or -1 for one that lies on its side, turned a quarter around its length one way or the other. */
    private final int roll;

    public ShippingContainerPiece(StructureTemplateManager manager, int variant, Rotation rotation, BlockPos position, long seed,
                                  Damage damage, Cargo cargo, Growth growth, boolean flooded, boolean salvage) {
        this(manager, variant, rotation, position, seed, damage, cargo, growth, flooded, salvage, 0);
    }

    public ShippingContainerPiece(StructureTemplateManager manager, int variant, Rotation rotation, BlockPos position, long seed,
                                  Damage damage, Cargo cargo, Growth growth, boolean flooded, boolean salvage, int roll) {
        super(ModStructures.SHIPPING_CONTAINER_PIECE.get(), 0, manager, location(variant), location(variant).toString(),
                settings(rotation), position);
        this.roll = Math.clamp(roll, -1, 1);
        this.template = ShippingContainerRoll.of(this.template, location(variant), this.roll);
        this.variant = variant;
        this.seed = seed;
        this.damage = damage;
        this.cargo = cargo;
        this.growth = growth;
        this.flooded = flooded;
        this.salvage = salvage;
    }

    public ShippingContainerPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.SHIPPING_CONTAINER_PIECE.get(), tag, manager, ignored -> settings(Rotation.valueOf(tag.getString("Rot"))));
        this.variant = tag.getInt("Variant");
        this.seed = tag.getLong("Seed");
        this.damage = Damage.values()[Math.clamp(tag.getInt("Damage"), 0, Damage.values().length - 1)];
        this.cargo = Cargo.values()[Math.clamp(tag.getInt("Cargo"), 0, Cargo.values().length - 1)];
        this.growth = Growth.values()[Math.clamp(tag.getInt("Growth"), 0, Growth.values().length - 1)];
        this.flooded = tag.getBoolean("Flooded");
        this.salvage = tag.getBoolean("Salvage");
        this.roll = Math.clamp(tag.getInt("Roll"), -1, 1);
        this.template = ShippingContainerRoll.of(this.template, location(this.variant), this.roll);
    }


    private java.util.Map<Long, String> hullBlocks;

    /** The block the template itself puts at this world position, or null where it has none; later additions of the generator do not count. */
    public String templateBlockAt(BlockPos pos) {
        if (hullBlocks == null) {
            var map = new java.util.HashMap<Long, String>();
            var tag = template.save(new CompoundTag());
            var palette = tag.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (var entry : tag.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                var data = (CompoundTag) entry;
                var at = data.getList("pos", net.minecraft.nbt.Tag.TAG_INT);
                var world = templatePosition.offset(StructureTemplate.calculateRelativePosition(placeSettings, new BlockPos(at.getInt(0), at.getInt(1), at.getInt(2))));
                map.put(world.asLong(), palette.getCompound(data.getInt("state")).getString("Name"));
            }
            hullBlocks = map;
        }
        return hullBlocks.get(pos.asLong());
    }
    private static ResourceLocation location(int template) {
        return ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "shipping_container/container_%02d".formatted(template));
    }

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE).setRotationPivot(PIVOT)
                .setLiquidSettings(LiquidSettings.APPLY_WATERLOGGING);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
        tag.putInt("Variant", variant);
        tag.putLong("Seed", seed);
        tag.putInt("Damage", damage.ordinal());
        tag.putInt("Cargo", cargo.ordinal());
        tag.putInt("Growth", growth.ordinal());
        tag.putBoolean("Flooded", flooded);
        tag.putBoolean("Salvage", salvage);
        tag.putInt("Roll", roll);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {}

    // ---- placement ----

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, box, chunkPos, pivot);
        fit(level, generator.getSeaLevel(), box);
    }

    void fit(WorldGenLevel level, int seaLevel, BoundingBox box) {
        var hull = getBoundingBox();
        Set<BlockPos> interior = new HashSet<>();
        for (int x = 2; x <= 4; x++) for (int y = 1; y <= 5; y++) for (int z = 2; z <= LENGTH - 3; z++)
            interior.add(templatePosition.offset(StructureTemplate.calculateRelativePosition(placeSettings, ShippingContainerRoll.rolled(new BlockPos(x, y, z), roll))));
        var pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(hull.minX(), box.minX()); x <= Math.min(hull.maxX(), box.maxX()); x++) {
            for (int y = hull.minY(); y <= Math.min(hull.maxY(), level.getMaxBuildHeight() - 1); y++) {
                for (int z = Math.max(hull.minZ(), box.minZ()); z <= Math.min(hull.maxZ(), box.maxZ()); z++) {
                    pos.set(x, y, z);
                    var here = level.getBlockState(pos);
                    if (y < seaLevel && (here.is(Blocks.LEVER) || here.is(Blocks.END_ROD))) level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2); // Neither can hold water, so they cannot stay in the sea.
                    else if (roll != 0 && y == hull.minY() && (here.isAir() || here.is(Blocks.WATER)) && level.getBlockState(pos.below()).isSolid()) level.setBlock(pos, level.getBlockState(pos.below()), 2); // A container on its side rests on its wall, not on the margin of the template.
                    else if (removed(pos)) level.setBlock(pos, y < seaLevel && seeps(pos)
                            ? ModBlocks.BUBBLE_VENT.get().defaultBlockState() : fluid(y, seaLevel, true), 2);
                    else if (damage != Damage.INTACT && isAttachment(level.getBlockState(pos)) && besideRemoved(pos)) level.setBlock(pos, fluid(y, seaLevel, true), 2);
                    else if (interior.contains(pos)) furnish(level, pos.immutable(), interior, seaLevel);
                    else if (y < seaLevel && level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2); // The template's margin of air.
                    if (y < seaLevel) soak(level, pos, interior);
                }
            }
        }
    }

    /** Whatever can hold water and sits in the sea holds it: the decorations outside, and everything in a flooded container. A dry container keeps its own walls dry. */
    private void soak(WorldGenLevel level, BlockPos pos, Set<BlockPos> interior) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.SimpleWaterloggedBlock) || !state.hasProperty(BlockStateProperties.WATERLOGGED)
                || state.getValue(BlockStateProperties.WATERLOGGED)) return;
        if (!flooded) for (Direction side : Direction.values()) if (interior.contains(pos.relative(side)) || interior.contains(pos)) return;
        level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 2);
    }

    /** Trapdoors and signs are fixed to the hull from outside; where the hull they hang on is gone, they go too. */
    private static boolean isAttachment(BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock || state.getBlock() instanceof net.minecraft.world.level.block.WallSignBlock;
    }

    private boolean besideRemoved(BlockPos pos) {
        for (Direction side : Direction.values()) if (removed(pos.relative(side))) return true;
        return false;
    }

    /** Some cells on the rim of the broken part let a thin stream of bubbles out, which shows from afar where a container is open. */
    private boolean seeps(BlockPos pos) {
        if (roll(pos, 30) >= 0.07) return false;
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (getBoundingBox().isInside(next) && !removed(next)) return true;
        }
        return false;
    }

    private BlockState fluid(int y, int seaLevel, boolean open) {
        return open && y < seaLevel ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    // ---- geometry in container space: along the length, across the width, up from the floor ----

    private boolean lengthwise() { return getBoundingBox().getZSpan() >= getBoundingBox().getXSpan(); }

    private int along(BlockPos pos) {
        var hull = getBoundingBox();
        return lengthwise() ? pos.getZ() - hull.minZ() : pos.getX() - hull.minX();
    }

    private int across(BlockPos pos) {
        var hull = getBoundingBox();
        return lengthwise() ? pos.getX() - hull.minX() : pos.getZ() - hull.minZ();
    }

    /** Height above the hull's lowest row, whichever way the container lies. */
    private int rise(BlockPos pos) { return pos.getY() - getBoundingBox().minY(); }

    /** The first row of the inside: the container's floor, or the wall it lies on. */
    private int floorRow() { return roll == 0 ? 1 : 2; }

    /** Height above the floor of the inside, which is where the cargo and the growth settle. */
    private int up(BlockPos pos) { return rise(pos) - floorRow() + 1; }

    private int ceilingUp() { return roll == 0 ? 5 : 3; }

    private static int mix(long seed, long a, long b) {
        long h = seed ^ a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (int) (h >>> 33);
    }

    private double roll(BlockPos pos, int salt) { return (mix(seed, pos.asLong(), salt) & 0xFFFF) / 65536.0; }

    /** Whether the damage has taken this cell, hull and cargo alike. */
    private boolean removed(BlockPos pos) {
        if (damage == Damage.INTACT || pos.equals(cargoCell())) return false; // The stash survives the damage.
        int along = along(pos), across = across(pos), up = rise(pos);
        // The margin outside the hull is air already: at the sides of an upright container, above and below one that lies down.
        if (roll == 0 ? across < 1 || across > 5 : up < 1 || up > 5) return false;
        int jag = mix(seed, across, up) & 1;
        if (damage == Damage.SPLIT) {
            int gap = 9 + (int) (seed >>> 8 & 7), half = 1 + (int) (seed >>> 12 & 1);
            return Math.abs(along - gap) <= half + jag;
        }
        int centre = 6 + (int) (seed >>> 20 & 15);
        int side = (int) (seed >>> 16 & 3);
        double cx = side == 1 ? 1 : side == 2 ? 5 : 3, cy = side == 0 ? 6 : side == 3 ? 5 : 3;
        double radius = 2.3 + (seed >>> 24 & 7) / 6.0 + jag * 0.7 - 0.35;
        double dx = along - centre, dy = up - cy, dz = across - cx;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    // ---- what is inside ----

    /** Crates stand in groups along the container with gaps between them, a walkway down the middle, and stacks of up to three. */
    private boolean crateAt(BlockPos pos) {
        int up = up(pos), across = across(pos), along = along(pos);
        if (up < 1 || up > 3 || along < 2 || along > LENGTH - 3) return false;
        if (up == 1 && isCargoCell(pos)) return true;
        if (aboveCargo(pos)) return false;
        if (up == 1) return (mix(seed, along / 3, 77) & 0xFFFF) / 65536.0 < 0.65 && roll(pos, 1) < (across == 3 ? 0.4 : 0.9);
        return crateAt(pos.below()) && roll(pos, up) < (up == 2 ? 0.55 : 0.3);
    }

    /** The two cells over the cargo's crate stay empty, so the "!" floating there is not covered by anything. */
    private boolean aboveCargo(BlockPos pos) {
        if (!salvage) return false;
        BlockPos cell = cargoCell();
        return pos.getX() == cell.getX() && pos.getZ() == cell.getZ() && pos.getY() > cell.getY() && pos.getY() <= cell.getY() + 2;
    }

    private BlockPos cargoCell() {
        var hull = getBoundingBox();
        int along = 7 + (int) (seed >>> 4 & 7), across = 2 + 2 * (int) (seed >>> 2 & 1);
        return lengthwise() ? new BlockPos(hull.minX() + across, hull.minY() + floorRow(), hull.minZ() + along)
                : new BlockPos(hull.minX() + along, hull.minY() + floorRow(), hull.minZ() + across);
    }

    private boolean isCargoCell(BlockPos pos) { return salvage && pos.equals(cargoCell()); }

    private boolean solidNeighbour(BlockPos pos, Direction side, Set<BlockPos> interior) {
        BlockPos next = pos.relative(side);
        return getBoundingBox().isInside(next) ? !interior.contains(next) && !removed(next) : true;
    }

    private void furnish(WorldGenLevel level, BlockPos pos, Set<BlockPos> interior, int seaLevel) {
        boolean wet = flooded && pos.getY() < seaLevel;
        BlockState state = wet ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        int up = up(pos);
        if (crateAt(pos)) {
            level.setBlock(pos, ModBlocks.CRATE.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet), 2);
            if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
                var random = RandomSource.create(mix(seed, pos.asLong(), 9));
                crate.fill(ShippingContainerCargo.contents(cargo, random), random);
                if (isCargoCell(pos)) crate.insertLoot(new ItemStack(ModItems.LOST_CARGO.get()));
            }
            return;
        }
        if (aboveCargo(pos)) {
            if (wet) level.setBlock(pos, state, 2);
            return;
        }
        switch (growth) {
            case SEAGRASS -> {
                if (!wet) break;
                double r = roll(pos, 4);
                if (up == 1 && r < 0.3) state = Blocks.SEAGRASS.defaultBlockState();
                else if (up == 1 && r < 0.45 && interior.contains(pos.above()))
                    state = Blocks.TALL_SEAGRASS.defaultBlockState().setValue(net.minecraft.world.level.block.TallSeagrassBlock.HALF, DoubleBlockHalf.LOWER);
                else if (up == 2 && roll(pos.below(), 4) >= 0.3 && roll(pos.below(), 4) < 0.45 && !crateAt(pos.below()))
                    state = Blocks.TALL_SEAGRASS.defaultBlockState().setValue(net.minecraft.world.level.block.TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER);
            }
            case MOSS, OVERGROWN -> {
                double limit = growth == Growth.OVERGROWN ? 0.75 : 0.5;
                boolean mossBelow = up == 2 && roll(pos.below(), 5) < limit && !crateAt(pos.below());
                if (up == 1) {
                    double r = roll(pos, 5);
                    if (r < limit) state = Blocks.MOSS_BLOCK.defaultBlockState();
                    else if (!wet && r < limit + 0.2) state = Blocks.MOSS_CARPET.defaultBlockState();
                } else if (mossBelow && !wet && roll(pos, 6) < 0.4) state = Blocks.MOSS_CARPET.defaultBlockState();
                if (growth == Growth.OVERGROWN) {
                    if (mossBelow && roll(pos, 10) < 0.2)
                        state = (roll(pos, 11) < 0.5 ? Blocks.AZALEA : Blocks.FLOWERING_AZALEA).defaultBlockState();
                    else if (up == ceilingUp() && roll(pos, 12) < 0.25)
                        state = Blocks.HANGING_ROOTS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet);
                }
            }
            case GLOW_LICHEN -> {
                if (roll(pos, 14) < 0.25) {
                    BlockState lichen = Blocks.GLOW_LICHEN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet);
                    boolean any = false;
                    for (Direction side : Direction.values()) {
                        if (!solidNeighbour(pos, side, interior)) continue;
                        lichen = lichen.setValue(MultifaceBlock.getFaceProperty(side), true);
                        any = true;
                    }
                    if (any) state = lichen;
                }
            }
            default -> {}
        }
        if (state.isAir() && !wet) return;
        level.setBlock(pos, state, 2);
    }

    /** What the container carries, for tests and the locate hooks. */
    public Cargo cargo() { return cargo; }

    public Damage damage() { return damage; }

    public Growth growth() { return growth; }

    public boolean flooded() { return flooded; }

    public boolean hasCargo() { return salvage; }

    /** The cell at floor level where a commissioned cargo is hidden. */
    public BlockPos stashCell() { return cargoCell(); }
}
