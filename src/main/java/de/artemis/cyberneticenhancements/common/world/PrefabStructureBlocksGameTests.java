package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PrefabStructureBlocksGameTests {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "med_tec_av");

    private static BlockPos find(CompoundTag template, String block, BlockPos origin) {
        var palette = template.getList("palette", Tag.TAG_COMPOUND);
        for (Tag entry : template.getList("blocks", Tag.TAG_COMPOUND)) {
            var data = (CompoundTag) entry;
            if (!palette.getCompound(data.getInt("state")).getString("Name").equals(block)) continue;
            var at = data.getList("pos", Tag.TAG_INT);
            return origin.offset(at.getInt(0), at.getInt(1), at.getInt(2));
        }
        throw new AssertionError("Template has no " + block);
    }

    private static List<ItemEntity> drops(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
    }

    @GameTest(template = "sandevistan_test", batch = "prefab_structure_blocks", timeoutTicks = 100)
    public static void structureBlocksOnlyPayOutOneScrapOnce(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(0, 40, 0));
        var template = level.getStructureManager().get(ID).orElseThrow();
        var settings = new StructurePlaceSettings().setRotation(Rotation.NONE);
        template.placeInWorld(level, origin, origin, settings, level.random, 2);

        var key = ResourceKey.create(Registries.STRUCTURE, ID);
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(key);
        var element = net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement.single(ID.toString())
                .apply(StructureTemplatePool.Projection.RIGID);
        var box = template.getBoundingBox(settings, origin);
        var piece = new PoolElementStructurePiece(level.getStructureManager(), element, origin, 0, Rotation.NONE, box, LiquidSettings.IGNORE_WATERLOGGING);
        var start = new StructureStart(structure, new ChunkPos(origin), 0, new PiecesContainer(List.of(piece)));
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            var chunk = level.getChunk(x, z);
            if (x == origin.getX() >> 4 && z == origin.getZ() >> 4) chunk.setStartForStructure(structure, start);
            chunk.addReferenceForStructure(structure, new ChunkPos(origin).toLong());
        }

        var tag = template.save(new CompoundTag());
        var concrete = find(tag, "minecraft:gray_concrete", origin);
        var stone = find(tag, "minecraft:polished_blackstone", origin);

        level.destroyBlock(concrete, true);
        var first = drops(level, concrete);
        h.assertTrue(first.size() == 1 && first.getFirst().getItem().is(ModItems.CHROMIUM_SCRAP.get()), "A structure block should drop exactly one chromium scrap, got " + first);
        first.forEach(ItemEntity::discard);

        level.setBlockAndUpdate(concrete, Blocks.GRAY_CONCRETE.defaultBlockState());
        level.destroyBlock(concrete, true);
        var again = drops(level, concrete);
        h.assertTrue(again.size() == 1 && again.getFirst().getItem().is(Items.GRAY_CONCRETE), "A replaced block must drop like a normal block, got " + again);
        again.forEach(ItemEntity::discard);

        level.destroyBlock(stone, true);
        var stoneDrops = drops(level, stone);
        h.assertTrue(stoneDrops.size() == 1 && stoneDrops.getFirst().getItem().is(ModItems.CHROMIUM_SCRAP.get()), "Polished blackstone must drop one scrap, got " + stoneDrops);
        stoneDrops.forEach(ItemEntity::discard);

        var outside = origin.offset(-5, 0, -5);
        level.setBlockAndUpdate(outside, Blocks.DIRT.defaultBlockState());
        level.destroyBlock(outside, true);
        var plain = drops(level, outside);
        h.assertTrue(plain.size() == 1 && plain.getFirst().getItem().is(Items.DIRT), "Blocks outside the structure must be unchanged, got " + plain);
        plain.forEach(ItemEntity::discard);
        h.succeed();
    }
}
