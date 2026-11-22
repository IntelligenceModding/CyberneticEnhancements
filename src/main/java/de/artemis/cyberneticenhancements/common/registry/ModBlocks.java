package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.block.EpicRelicCacheBlock;
import de.artemis.cyberneticenhancements.common.block.LegendaryRelicCacheBlock;
import de.artemis.cyberneticenhancements.common.block.RareRelicCacheBlock;
import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import de.artemis.cyberneticenhancements.common.block.RecyclerStationBlock;
import de.artemis.cyberneticenhancements.common.block.RipperStationBlock;
import de.artemis.cyberneticenhancements.common.block.TechStationBlock;
import de.artemis.cyberneticenhancements.common.block.UncommonRelicCacheBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CyberneticEnhancements.MOD_ID);
    public static final DeferredBlock<Block> CHROMIUM_ORE = register("chromium_ore", de.artemis.cyberneticenhancements.common.block.ChromiumOreBlock::new,
            p -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> DEEPSLATE_CHROMIUM_ORE = register("deepslate_chromium_ore", de.artemis.cyberneticenhancements.common.block.ChromiumOreBlock::new,
            p -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).requiresCorrectToolForDrops());
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.DropoffPointBlock> DROPOFF_POINT = register(
            "dropoff_point", de.artemis.cyberneticenhancements.common.block.DropoffPointBlock::new,
            properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL));
    /** A fast travel terminal: an enchanting table that projects a hologram. */
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.FastTravelBlock> FAST_TRAVEL = register(
            "fast_travel", de.artemis.cyberneticenhancements.common.block.FastTravelBlock::new,
            properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).lightLevel(state -> 9));
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.HoloBarrierBlock> HOLO_BARRIER = register(
            "holo_barrier", de.artemis.cyberneticenhancements.common.block.HoloBarrierBlock::new,
            properties -> properties.noCollission().noOcclusion().strength(-1F, 3600000F).noLootTable());
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.IronQuestCrateBlock> IRON_QUEST_CRATE = register(
            "iron_quest_crate", de.artemis.cyberneticenhancements.common.block.IronQuestCrateBlock::new,
            properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5F).sound(SoundType.METAL));
    /** The loot crate of generated structures: breaks at once, has no item and is never obtainable. */
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.CrateBlock> CRATE = BLOCKS.registerBlock(
            "crate", de.artemis.cyberneticenhancements.common.block.CrateBlock::new,
            BlockBehaviour.Properties.of().strength(0F).sound(SoundType.WOOD).noOcclusion().noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    /** The invisible source of the bubbles that rise from a broken container. */
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.BubbleVentBlock> BUBBLE_VENT = BLOCKS.registerBlock(
            "bubble_vent", de.artemis.cyberneticenhancements.common.block.BubbleVentBlock::new,
            BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().replaceable().noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    /** A bare copper wire laid on any surface, for decoration only. */
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.CopperCableBlock> COPPER_CABLE = register(
            "copper_cable", de.artemis.cyberneticenhancements.common.block.CopperCableBlock::new,
            properties -> properties.strength(0.2F).sound(SoundType.COPPER).noCollission().noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    /** An iron water pipe that joins its neighbours, for decoration. */
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.WaterPipeBlock> WATER_PIPE = register(
            "water_pipe", de.artemis.cyberneticenhancements.common.block.WaterPipeBlock::new,
            properties -> properties.strength(1.5F, 6F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** A square sheet-metal HVAC duct that joins its neighbours, for decoration. */
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.DuctBlock> HVAC_DUCT = register(
            "hvac_duct", de.artemis.cyberneticenhancements.common.block.DuctBlock::new,
            properties -> properties.strength(1.0F, 4F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** A slatted vent for the end of a duct or the side of a block. */
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.block.VentBlock> HVAC_VENT = register(
            "hvac_vent", de.artemis.cyberneticenhancements.common.block.VentBlock::new,
            properties -> properties.strength(1.0F, 4F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<de.artemis.cyberneticenhancements.common.fabrication.FabricatorBlock> CYBERWARE_FABRICATOR = register(
            "cyberware_fabricator", de.artemis.cyberneticenhancements.common.fabrication.FabricatorBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL));

    public static final DeferredBlock<RipperStationBlock> RIPPER_STATION = register(
            "ripper_station",
            RipperStationBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<TechStationBlock> TECH_STATION = register(
            "tech_station",
            TechStationBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<TechStationBlock> UPGRADE_STATION = register(
            "upgrade_station", TechStationBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<RecyclerStationBlock> RECYCLER_STATION = register(
            "recycler_station",
            RecyclerStationBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<UncommonRelicCacheBlock> RELIC_CACHE = register(
            "relic_cache",
            UncommonRelicCacheBlock::new,
            properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(5.0F, 12.0F).sound(SoundType.GLASS)
    );

    public static final DeferredBlock<UncommonRelicCacheBlock> UNCOMMON_RELIC_CACHE = register(
            "uncommon_relic_cache",
            UncommonRelicCacheBlock::new,
            properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(5.0F, 12.0F).sound(SoundType.GLASS)
    );

    public static final DeferredBlock<RareRelicCacheBlock> RARE_RELIC_CACHE = register(
            "rare_relic_cache",
            RareRelicCacheBlock::new,
            properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(5.0F, 12.0F).sound(SoundType.GLASS)
    );

    public static final DeferredBlock<EpicRelicCacheBlock> EPIC_RELIC_CACHE = register(
            "epic_relic_cache",
            EpicRelicCacheBlock::new,
            properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(5.0F, 12.0F).sound(SoundType.GLASS)
    );

    public static final DeferredBlock<LegendaryRelicCacheBlock> LEGENDARY_RELIC_CACHE = register(
            "legendary_relic_cache",
            LegendaryRelicCacheBlock::new,
            properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(5.0F, 12.0F).sound(SoundType.GLASS)
    );

    private ModBlocks() {
    }

    private static <T extends Block> DeferredBlock<T> register(
            String name,
            Function<BlockBehaviour.Properties, T> blockFactory,
            UnaryOperator<BlockBehaviour.Properties> properties
    ) {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, blockFactory, properties.apply(BlockBehaviour.Properties.of()));
        ModItems.ITEMS.registerSimpleBlockItem(block, new Item.Properties());
        return block;
    }

    public static List<DeferredBlock<? extends RelicCacheBlock>> currentRelicCaches() {
        return List.of(UNCOMMON_RELIC_CACHE, RARE_RELIC_CACHE, EPIC_RELIC_CACHE, LEGENDARY_RELIC_CACHE);
    }

    public static List<DeferredBlock<? extends RelicCacheBlock>> allRelicCaches() {
        return List.of(RELIC_CACHE, UNCOMMON_RELIC_CACHE, RARE_RELIC_CACHE, EPIC_RELIC_CACHE, LEGENDARY_RELIC_CACHE);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
