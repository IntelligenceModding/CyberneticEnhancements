package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.RipperStationBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.TechStationBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CyberneticEnhancements.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<de.artemis.cyberneticenhancements.common.fabrication.FabricatorBlockEntity>> FABRICATOR =
            BLOCK_ENTITIES.register("cyberware_fabricator", () -> BlockEntityType.Builder.of(de.artemis.cyberneticenhancements.common.fabrication.FabricatorBlockEntity::new, ModBlocks.CYBERWARE_FABRICATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RipperStationBlockEntity>> RIPPER_STATION =
            BLOCK_ENTITIES.register("ripper_station", () -> BlockEntityType.Builder.of(RipperStationBlockEntity::new, ModBlocks.RIPPER_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TechStationBlockEntity>> TECH_STATION =
            BLOCK_ENTITIES.register("tech_station", () -> BlockEntityType.Builder.of(TechStationBlockEntity::new, ModBlocks.TECH_STATION.get(), ModBlocks.UPGRADE_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecyclerStationBlockEntity>> RECYCLER_STATION =
            BLOCK_ENTITIES.register("recycler_station", () -> BlockEntityType.Builder.of(RecyclerStationBlockEntity::new, ModBlocks.RECYCLER_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RelicCacheBlockEntity>> RELIC_CACHE =
            BLOCK_ENTITIES.register("relic_cache", () -> BlockEntityType.Builder.of(
                    RelicCacheBlockEntity::new,
                    ModBlocks.RELIC_CACHE.get(),
                    ModBlocks.UNCOMMON_RELIC_CACHE.get(),
                    ModBlocks.RARE_RELIC_CACHE.get(),
                    ModBlocks.EPIC_RELIC_CACHE.get(),
                    ModBlocks.LEGENDARY_RELIC_CACHE.get()
            ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
