package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.world.ShippingContainerPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import de.artemis.cyberneticenhancements.common.blockentity.CrateBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/** The world side of the salvage quests: finding a lost container and hiding the commissioned cargo in it. */
final class SalvageQuests {
    static final TagKey<Structure> CONTAINERS = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "shipping_container"));
    private static final int SEARCH_CHUNKS = 48;

    private SalvageQuests() {}

    /** Centre of the nearest generated container, or null where the sea has none to offer. */
    static @Nullable BlockPos nearest(ServerLevel level, BlockPos origin) {
        return level.findNearestMapStructure(CONTAINERS, origin, SEARCH_CHUNKS, false);
    }

    /** The container's hull, once its chunks exist. */
    static @Nullable ShippingContainerPiece hull(ServerLevel level, BlockPos container) {
        level.getChunk(container);
        for (var start : level.structureManager().startsForStructure(new ChunkPos(container), structure -> true))
            for (var piece : start.getPieces())
                if (piece instanceof ShippingContainerPiece hull) return hull;
        return null;
    }

    /** Hides the cargo in the container's stash crate, replacing the container's own loose cargo. */
    static boolean stash(ServerLevel level, BlockPos container, ItemStack cargo) {
        var hull = hull(level, container);
        return hull != null && stash(level, hull, cargo);
    }

    static boolean stash(ServerLevel level, ShippingContainerPiece hull, ItemStack cargo) {
        BlockPos cell = hull.stashCell();
        level.getChunk(cell);
        boolean wet = hull.flooded() && cell.getY() < level.getSeaLevel();
        if (!level.getBlockState(cell).is(ModBlocks.CRATE.get())) {
            level.setBlock(cell, ModBlocks.CRATE.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet), 3);
            for (int above = 1; above <= 2; above++) // Nothing may cover the "!" over the crate.
                level.setBlock(cell.above(above), (wet ? Blocks.WATER : Blocks.AIR).defaultBlockState(), 3);
        }
        if (!(level.getBlockEntity(cell) instanceof CrateBlockEntity crate)) return false;
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            var stack = crate.getItem(slot);
            if (stack.is(ModItems.LOST_CARGO.get()) && QuestItemVersions.key(stack).isEmpty()) crate.setItem(slot, ItemStack.EMPTY);
        }
        crate.insertLoot(cargo);
        crate.setChanged();
        return true;
    }
}
