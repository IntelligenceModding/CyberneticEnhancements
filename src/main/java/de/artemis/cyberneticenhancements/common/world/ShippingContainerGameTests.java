package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.world.ShippingContainerPiece.Cargo;
import de.artemis.cyberneticenhancements.common.world.ShippingContainerPiece.Damage;
import de.artemis.cyberneticenhancements.common.world.ShippingContainerPiece.Growth;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import de.artemis.cyberneticenhancements.common.blockentity.CrateBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShippingContainerGameTests {
    private static ShippingContainerPiece lastPiece;

    private record Count(int barrels, int cargos, int blocks, int water) {}

    private static BoundingBox place(GameTestHelper h, int template, Rotation rotation, long seed, Damage damage, Cargo cargo, Growth growth,
                                     boolean flooded, boolean salvage) {
        return place(h, template, rotation, seed, damage, cargo, growth, flooded, salvage, 0);
    }

    private static BoundingBox place(GameTestHelper h, int template, Rotation rotation, long seed, Damage damage, Cargo cargo, Growth growth,
                                     boolean flooded, boolean salvage, int roll) {
        var level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(0, 1, 0));
        var piece = new ShippingContainerPiece(level.getServer().getStructureManager(), template, rotation, origin, seed, damage, cargo, growth, flooded, salvage, roll);
        var box = piece.getBoundingBox().inflatedBy(1);
        level.getChunk(origin);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box, new ChunkPos(origin), origin);
        piece.fit(level, 100, box); // The test world has no sea of its own, so this one is imagined above the container.
        lastPiece = piece;
        return piece.getBoundingBox();
    }

    /** Makes the placed piece known to the structure manager, as it is in a real world, so code that looks up the structure at a position finds it. */
    private static void register(net.minecraft.server.level.ServerLevel level, ShippingContainerPiece piece) {
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "shipping_container"));
        var box = piece.getBoundingBox();
        var origin = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
        var start = new net.minecraft.world.level.levelgen.structure.StructureStart(structure, origin, 0,
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
        level.getChunk(origin.x, origin.z).setStartForStructure(structure, start);
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz).addReferenceForStructure(structure, origin.toLong());
    }

    private static Count count(GameTestHelper h, BoundingBox box, Block block) {
        int barrels = 0, cargos = 0, blocks = 0, water = 0;
        for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var state = h.getLevel().getBlockState(pos);
            if (state.is(ModBlocks.CRATE.get())) {
                barrels++;
                if (h.getLevel().getBlockEntity(pos) instanceof CrateBlockEntity crate) {
                    for (int slot = 0; slot < crate.getContainerSize(); slot++)
                        if (crate.getItem(slot).is(ModItems.LOST_CARGO.get())) cargos++;
                }
            }
            if (state.is(block)) blocks++;
            if (state.is(Blocks.WATER)) water++;
        }
        return new Count(barrels, cargos, blocks, water);
    }

    @GameTest(template = "sandevistan_test", batch = "shipping_containers", timeoutTicks = 200)
    public static void anIntactContainerCarriesCratesAndTheCargo(GameTestHelper h) {
        var box = place(h, 1, Rotation.NONE, 1234L, Damage.INTACT, Cargo.FOOD, Growth.NONE, false, true);
        var found = count(h, box, ModBlocks.CRATE.get());
        h.assertTrue(found.barrels() > 5, "A container of food should be full of crates, found " + found.barrels());
        h.assertTrue(found.cargos() == 1, "Exactly one cargo is hidden in a crate, found " + found.cargos());
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "shipping_containers", timeoutTicks = 200)
    public static void aSavedContainerKeepsItsTemplateAfterReloading(GameTestHelper h) {
        var level = h.getLevel();
        var manager = level.getServer().getStructureManager();
        var piece = new ShippingContainerPiece(manager, 4, Rotation.CLOCKWISE_90, h.absolutePos(new BlockPos(0, 1, 0)), 3L,
                Damage.INTACT, Cargo.FOOD, Growth.NONE, false, true);
        var reloaded = new ShippingContainerPiece(manager, piece.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level)));
        h.assertTrue(reloaded.getBoundingBox().equals(piece.getBoundingBox()),
                "A reloaded container lost its shape: " + reloaded.getBoundingBox() + " instead of " + piece.getBoundingBox());
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "shipping_containers", timeoutTicks = 600)
    public static void everyContainerKindCanBeSearchedFor(GameTestHelper h) {
        h.assertTrue(ShippingContainerLocator.KINDS.keySet().containsAll(java.util.List.of("intact", "split", "hole", "food", "metals", "electronics",
                "weapons", "medical", "components", "none", "seagrass", "moss", "overgrown", "glow_lichen", "flooded", "sealed", "beached", "underwater", "cargo")),
                "A container kind is missing from the locate command: " + ShippingContainerLocator.KINDS.keySet());
        // The test world may hold no containers at all; the search must still finish and answer sensibly.
        var found = ShippingContainerLocator.nearest(h.getLevel(), h.absolutePos(BlockPos.ZERO), "hole");
        h.assertTrue(found == null || found.distSqr(h.absolutePos(BlockPos.ZERO)) > 0, "The search returned nonsense");
        h.assertTrue(ShippingContainerLocator.nearest(h.getLevel(), h.absolutePos(BlockPos.ZERO), "nonsense") == null, "An unknown kind found something");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "shipping_containers", timeoutTicks = 200)
    public static void breakingTheHullOfASealedContainerLetsTheAirOutAndTheSeaIn(GameTestHelper h) {
        var box = place(h, 1, Rotation.NONE, 77L, Damage.INTACT, Cargo.FOOD, Growth.NONE, false, false);
        var piece = lastPiece;
        var level = h.getLevel();
        int sea = box.maxY() + 1;
        int before = count(h, box, Blocks.AIR).water();
        boolean started = false;
        for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var old = level.getBlockState(pos);
            if (old.isAir() || !old.getFluidState().isEmpty()) continue;
            boolean water = false, air = false;
            for (var side : net.minecraft.core.Direction.values()) {
                var next = pos.relative(side);
                water |= level.getBlockState(next).is(Blocks.WATER);
                air |= level.getBlockState(next).isAir();
            }
            if (!water || !air) continue;
            BlockPos hole = pos.immutable();
            level.setBlock(hole, Blocks.AIR.defaultBlockState(), 3);
            if (ShippingContainerLeaks.release(level, piece, hole, sea)) { started = true; break; }
            level.setBlock(hole, old, 3);
        }
        h.assertTrue(started, "Breaking the hull did not start the leak");
        h.runAfterDelay(150, () -> {
            h.assertTrue(count(h, box, Blocks.AIR).water() > before + 50, "The sea did not flood the container");
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "shipping_containers", timeoutTicks = 600)
    public static void nothingInTheSeaStaysDryAndEveryContainerCanLieOnItsSide(GameTestHelper h) {
        var log = new StringBuilder();
        for (int roll = -1; roll <= 1; roll++) {
            for (int template = 1; template <= ShippingContainerPiece.TEMPLATES; template++) {
                for (boolean flooded : new boolean[]{false, true}) {
                    var box = place(h, template, Rotation.NONE, 5L, Damage.INTACT, Cargo.FOOD, Growth.NONE, flooded, false, roll);
                    var level = h.getLevel();
                    int fittings = count(h, box, Blocks.LEVER).blocks() + count(h, box, Blocks.END_ROD).blocks();
                    if (fittings > 0) log.append("t").append(template).append(" roll ").append(roll).append(": ").append(fittings).append(" levers/rods left; ");
                    int dry = 0;
                    for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                        var state = level.getBlockState(pos);
                        // A sealed container keeps the water out of its own walls, so only the flooded ones and the outside are checked.
                        if (flooded && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                                && !state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) dry++;
                    }
                    if (dry > 0) log.append("t").append(template).append(" roll ").append(roll).append(": ").append(dry).append(" dry blocks; ");
                }
            }
        }
        h.assertTrue(log.isEmpty(), "Problems: " + log);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "shipping_containers", timeoutTicks = 200)
    public static void everyKindOfCargoFillsCratesWithItems(GameTestHelper h) {
        var log = new StringBuilder();
        for (var cargo : Cargo.values()) {
            var box = place(h, 2, Rotation.CLOCKWISE_90, 99L, Damage.INTACT, cargo, Growth.NONE, false, true);
            int crates = 0, empty = 0;
            for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (!h.getLevel().getBlockState(pos).is(ModBlocks.CRATE.get())) continue;
                crates++;
                if (h.getLevel().getBlockEntity(pos) instanceof CrateBlockEntity crate && crate.isEmpty()) empty++;
            }
            if (crates < 5 || empty > 0) log.append(cargo).append(": ").append(crates).append(" crates, ").append(empty).append(" empty; ");
        }
        h.assertTrue(log.isEmpty(), "Every cargo should come in filled crates: " + log);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "shipping_containers", timeoutTicks = 200)
    public static void theHullDropsOneScrapAndTheCargoStaysNormal(GameTestHelper h) {
        var box = place(h, 1, Rotation.NONE, 31L, Damage.INTACT, Cargo.FOOD, Growth.NONE, false, false);
        register(h.getLevel(), lastPiece);
        var level = h.getLevel();
        BlockPos hull = null, ore = null; // "ore" is the first crate
        for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var name = lastPiece.templateBlockAt(pos);
            if (hull == null && name != null && !level.getBlockState(pos).isAir() && name.equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString())) hull = pos.immutable();
            if (ore == null && level.getBlockState(pos).is(ModBlocks.CRATE.get())) ore = pos.immutable();
        }
        h.assertTrue(hull != null && ore != null, "No hull block or crate found");
        var kind = level.getBlockState(hull).getBlock();
        level.destroyBlock(hull, true);
        var first = scrapNear(level, hull);
        h.assertTrue(first.size() == 1 && first.getFirst().getItem().is(ModItems.CHROMIUM_SCRAP.get()), "A hull block should drop one scrap, got " + first);
        first.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
        level.setBlockAndUpdate(hull, kind.defaultBlockState());
        level.destroyBlock(hull, true);
        var again = scrapNear(level, hull);
        h.assertTrue(again.stream().noneMatch(e -> e.getItem().is(ModItems.CHROMIUM_SCRAP.get())), "A replaced hull block must be ordinary, got " + again);
        again.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
        level.destroyBlock(ore, true);
        var cargo = scrapNear(level, ore);
        h.assertTrue(cargo.stream().noneMatch(e -> e.getItem().is(ModItems.CHROMIUM_SCRAP.get())), "Cargo must stay ordinary, got " + cargo);
        cargo.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
        h.succeed();
    }

    private static java.util.List<net.minecraft.world.entity.item.ItemEntity> scrapNear(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2));
    }

    @GameTest(template = "sandevistan_test", batch = "shipping_containers", timeoutTicks = 200)
    public static void damageOpensTheHullAndEveryGrowthPlaces(GameTestHelper h) {
        int template = 1;
        for (var damage : Damage.values()) {
            for (var growth : Growth.values()) {
                var box = place(h, template, Rotation.COUNTERCLOCKWISE_90, 7L + template, damage, Cargo.METALS, growth, true, false);
                var found = count(h, box, Blocks.MOSS_BLOCK);
                if (damage != Damage.INTACT) h.assertTrue(found.water() > 0, damage + " should let the water in");
                if (growth == Growth.MOSS) h.assertTrue(found.blocks() > 0, "Moss should grow in the container");
                template = template % ShippingContainerPiece.TEMPLATES + 1;
            }
        }
        h.succeed();
    }
}
