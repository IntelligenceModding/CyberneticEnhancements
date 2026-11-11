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
