package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Blocks that belong to a prefab structure (such as Med Tec AV) keep their normal look, sound and hardness, but
 * are only worth one chromium scrap, once per position. That keeps valuable blocks (ender chests, shulker boxes) from
 * being farmed, and breaking and re-placing a block cannot be used to farm scrap either.
 */
public final class PrefabStructureBlocks {
    private static final ResourceLocation MED_TEC_AV = ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "med_tec_av");
    private static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, MED_TEC_AV);

    private PrefabStructureBlocks() {}

    public static void onBlockDrops(BlockDropsEvent event) {
        if (!claim(event.getLevel(), event.getPos(), event.getState())) return;
        event.getDrops().clear();
        event.setDroppedExperience(0);
        event.getDrops().add(scrap(event.getLevel(), event.getPos()));
    }

    /** Explosions drop their loot without a drop event, so structure blocks they hit turn into scrap here instead. */
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        event.getAffectedBlocks().removeIf(pos -> {
            if (!claim(level, pos, level.getBlockState(pos))) return false;
            level.removeBlock(pos, false);
            level.addFreshEntity(scrap(level, pos));
            return true;
        });
    }

    private static ItemEntity scrap(ServerLevel level, BlockPos pos) {
        return new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.CHROMIUM_SCRAP.get()));
    }

    /** True the first time a block that still matches the original structure is broken at this position. */
    private static boolean claim(ServerLevel level, BlockPos pos, BlockState state) {
        var start = level.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(STRUCTURE));
        if (!start.isValid() || !isOriginalBlock(level, start.getPieces(), pos, state)) return false;
        return Harvested.get(level).claim(pos);
    }

    private static boolean isOriginalBlock(ServerLevel level, java.util.List<net.minecraft.world.level.levelgen.structure.StructurePiece> pieces,
                                           BlockPos pos, BlockState state) {
        var template = level.getStructureManager().get(MED_TEC_AV).orElse(null);
        if (template == null) return false;
        String block = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (var piece : pieces) {
            if (!(piece instanceof PoolElementStructurePiece pooled) || !piece.getBoundingBox().isInside(pos)) continue;
            var settings = new StructurePlaceSettings().setRotation(pooled.getRotation());
            var tag = template.save(new CompoundTag());
            var palette = tag.getList("palette", Tag.TAG_COMPOUND);
            for (Tag entry : tag.getList("blocks", Tag.TAG_COMPOUND)) {
                var data = (CompoundTag) entry;
                var at = data.getList("pos", Tag.TAG_INT);
                var world = StructureTemplate.calculateRelativePosition(settings, new BlockPos(at.getInt(0), at.getInt(1), at.getInt(2)))
                        .offset(pooled.getPosition());
                if (world.equals(pos)) return palette.getCompound(data.getInt("state")).getString("Name").equals(block);
            }
        }
        return false;
    }

    /** Positions that already paid out their scrap, so a replaced block is just an ordinary block. */
    private static final class Harvested extends SavedData {
        private static final Factory<Harvested> FACTORY = new Factory<>(Harvested::new, Harvested::load, null);
        private final Set<Long> positions = new HashSet<>();

        static Harvested get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY, CyberneticEnhancements.MOD_ID + "_prefab_blocks"); }

        boolean claim(BlockPos pos) {
            if (!positions.add(pos.asLong())) return false;
            setDirty();
            return true;
        }

        private static Harvested load(CompoundTag tag, HolderLookup.Provider registries) {
            var data = new Harvested();
            for (long position : tag.getLongArray("positions")) data.positions.add(position);
            return data;
        }

        @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("positions", new LongArrayTag(positions.stream().mapToLong(Long::longValue).toArray()));
            return tag;
        }
    }
}
