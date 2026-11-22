package de.artemis.cyberneticenhancements.common.compat.jade;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.block.CrateBlock;
import de.artemis.cyberneticenhancements.common.block.FastTravelBlock;
import de.artemis.cyberneticenhancements.common.block.IronQuestCrateBlock;
import de.artemis.cyberneticenhancements.common.block.RecyclerStationBlock;
import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import de.artemis.cyberneticenhancements.common.block.TechStationBlock;
import de.artemis.cyberneticenhancements.common.blockentity.FastTravelBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.TechStationBlockEntity;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Shows what a player would otherwise have to open a screen for (trust, aggression, tiers) in Jade's own tooltip. */
@WailaPlugin
public final class CyberneticEnhancementsJadePlugin implements IWailaPlugin {
    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, path); }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(NpcJadeProvider.INSTANCE, AbstractCityNpcEntity.class);
        registration.registerEntityDataProvider(CyberpsychoJadeProvider.INSTANCE, CyberpsychoEntity.class);
        registration.registerBlockDataProvider(RelicCacheJadeProvider.INSTANCE, RelicCacheBlockEntity.class);
        registration.registerBlockDataProvider(StationJadeProvider.INSTANCE, TechStationBlockEntity.class);
        registration.registerBlockDataProvider(StationJadeProvider.INSTANCE, RecyclerStationBlockEntity.class);
        registration.registerBlockDataProvider(CrateJadeProvider.INSTANCE, IronQuestCrateBlockEntity.class);
        registration.registerBlockDataProvider(FastTravelJadeProvider.INSTANCE, FastTravelBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(NpcJadeProvider.INSTANCE, AbstractCityNpcEntity.class);
        registration.registerEntityComponent(CyberpsychoJadeProvider.INSTANCE, CyberpsychoEntity.class);
        registration.registerBlockComponent(RelicCacheJadeProvider.INSTANCE, RelicCacheBlock.class);
        registration.registerBlockComponent(StationJadeProvider.INSTANCE, TechStationBlock.class);
        registration.registerBlockComponent(StationJadeProvider.INSTANCE, RecyclerStationBlock.class);
        registration.registerBlockComponent(CrateJadeProvider.INSTANCE, IronQuestCrateBlock.class);
        registration.registerBlockComponent(FastTravelJadeProvider.INSTANCE, FastTravelBlock.class);
        registration.registerBlockComponent(ContainerCrateJadeProvider.INSTANCE, CrateBlock.class);
        // Only there to make bubbles for the shipping containers: nothing a player should find or look at.
        registration.hideTarget(ModBlocks.BUBBLE_VENT.get());
    }
}
