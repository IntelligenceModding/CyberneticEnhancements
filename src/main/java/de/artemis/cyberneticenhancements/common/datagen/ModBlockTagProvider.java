package de.artemis.cyberneticenhancements.common.datagen;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, CyberneticEnhancements.MOD_ID, null);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.CYBERWARE_FABRICATOR.get())
                .add(ModBlocks.RELIC_CACHE.get())
                .add(ModBlocks.RIPPER_STATION.get())
                .add(ModBlocks.TECH_STATION.get())
                .add(ModBlocks.UPGRADE_STATION.get())
                .add(ModBlocks.RECYCLER_STATION.get());

        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.CYBERWARE_FABRICATOR.get())
                .add(ModBlocks.RELIC_CACHE.get())
                .add(ModBlocks.RIPPER_STATION.get())
                .add(ModBlocks.TECH_STATION.get())
                .add(ModBlocks.UPGRADE_STATION.get())
                .add(ModBlocks.RECYCLER_STATION.get());
    }
}
