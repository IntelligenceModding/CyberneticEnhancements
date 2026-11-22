package de.artemis.cyberneticenhancements.common.datagen;

import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class ModBlockLootTableProvider extends BlockLootSubProvider {
    private static final Set<Block> DIRECT_BLOCKS = Set.of(
            ModBlocks.IRON_QUEST_CRATE.get(),
            ModBlocks.DROPOFF_POINT.get(),
            ModBlocks.FAST_TRAVEL.get(),
            ModBlocks.CYBERWARE_FABRICATOR.get(),
            ModBlocks.RELIC_CACHE.get(),
            ModBlocks.UNCOMMON_RELIC_CACHE.get(),
            ModBlocks.RARE_RELIC_CACHE.get(),
            ModBlocks.EPIC_RELIC_CACHE.get(),
            ModBlocks.LEGENDARY_RELIC_CACHE.get(),
            ModBlocks.RIPPER_STATION.get(),
            ModBlocks.TECH_STATION.get(),
            ModBlocks.UPGRADE_STATION.get(),
            ModBlocks.RECYCLER_STATION.get()
    );

    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(ModBlocks.CHROMIUM_ORE.get(), createOreDrop(ModBlocks.CHROMIUM_ORE.get(), de.artemis.cyberneticenhancements.common.registry.ModItems.RAW_CHROMIUM.get()));
        add(ModBlocks.DEEPSLATE_CHROMIUM_ORE.get(), createOreDrop(ModBlocks.DEEPSLATE_CHROMIUM_ORE.get(), de.artemis.cyberneticenhancements.common.registry.ModItems.RAW_CHROMIUM.get()));
        add(ModBlocks.COPPER_CABLE.get(), copperCableDrops());
        dropSelf(ModBlocks.WATER_PIPE.get());
        dropSelf(ModBlocks.HVAC_DUCT.get());
        dropSelf(ModBlocks.HVAC_VENT.get());
        DIRECT_BLOCKS.forEach(block -> {
            if (block instanceof de.artemis.cyberneticenhancements.common.block.RelicCacheBlock)
                add(block, net.minecraft.world.level.storage.loot.LootTable.lootTable());
            else dropSelf(block);
        });
    }

    /** One cable for every side that holds a wire, like glow lichen. */
    private net.minecraft.world.level.storage.loot.LootTable.Builder copperCableDrops() {
        var block = ModBlocks.COPPER_CABLE.get();
        var entry = net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block);
        for (var side : net.minecraft.core.Direction.values())
            entry.apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1), true)
                    .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                    .hasProperty(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(side), true))));
        return net.minecraft.world.level.storage.loot.LootTable.lootTable().withPool(applyExplosionCondition(block,
                net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1)).add(entry)));
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        var blocks = new java.util.ArrayList<>(DIRECT_BLOCKS);
        blocks.addAll(List.of(ModBlocks.CHROMIUM_ORE.get(), ModBlocks.DEEPSLATE_CHROMIUM_ORE.get(), ModBlocks.COPPER_CABLE.get(), ModBlocks.WATER_PIPE.get(), ModBlocks.HVAC_DUCT.get(), ModBlocks.HVAC_VENT.get()));
        return blocks;
    }
}
