package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CyberneticEnhancements.MOD_ID);

    @SuppressWarnings("unused")
    public static final Supplier<CreativeModeTab> CYBERNETIC_ENHANCEMENTS_CREATIVE_TAB = CREATIVE_MODE_TAB.register(
            "cybernetic_enhancements_creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.COPPER_WIRING.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.cyberneticenhancements"))
                    .displayItems((parameters, output) -> {
                        ModItems.MATERIALS.forEach(item -> output.accept(item.get()));
                        output.accept(ModBlocks.CHROMIUM_ORE.get()); output.accept(ModBlocks.DEEPSLATE_CHROMIUM_ORE.get());
                        output.accept(ModBlocks.CYBERWARE_FABRICATOR.get());
                        de.artemis.cyberneticenhancements.common.fabrication.FabricationContent.PARTS.values().forEach(item -> output.accept(item.get()));
                        de.artemis.cyberneticenhancements.common.fabrication.FabricationContent.SCHEMATICS.values().forEach(item -> output.accept(item.get()));
                        ModItems.consumableItems().forEach(item -> output.accept(item.get()));
                        output.accept(ModBlocks.UNCOMMON_RELIC_CACHE.get());
                        output.accept(ModBlocks.RARE_RELIC_CACHE.get());
                        output.accept(ModBlocks.EPIC_RELIC_CACHE.get());
                        output.accept(ModBlocks.LEGENDARY_RELIC_CACHE.get());
                        output.accept(ModBlocks.RIPPER_STATION.get());
                        output.accept(ModBlocks.TECH_STATION.get());
                        output.accept(ModBlocks.UPGRADE_STATION.get());
                        output.accept(ModBlocks.RECYCLER_STATION.get());
                        output.accept(ModBlocks.FAST_TRAVEL.get());
                        output.accept(ModItems.FIXER_SPAWN_EGG.get());
                        output.accept(ModItems.DEV_TEST_SPAWN_EGG.get());
                    })
                    .build()
    );

    @SuppressWarnings("unused")
    public static final Supplier<CreativeModeTab> CYBERWARE_CREATIVE_TAB = CREATIVE_MODE_TAB.register(
            "cyberware_creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.cyberware("basic_kiroshi_optics").get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.cyberneticenhancements.cyberware"))
                    .displayItems((parameters, output) -> {
                        ModItems.chipwareItems().forEach(item -> output.accept(item.get()));
                        ModItems.moduleItems().forEach(item -> output.accept(item.get()));
                        ModItems.standardCyberwareItems().forEach(item -> output.accept(item.get()));
                    })
                    .build()
    );

    @SuppressWarnings("unused")
    public static final Supplier<CreativeModeTab> GAMEPLAY_CYBERWARE_CREATIVE_TAB = CREATIVE_MODE_TAB.register(
            "gameplay_cyberware_creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.cyberware("excavator_arms").get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.cyberneticenhancements.gameplay_cyberware"))
                    .displayItems((parameters, output) -> {
                        ModItems.gameplayCyberwareItems().forEach(item -> output.accept(item.get()));
                    })
                    .build()
    );

    @SuppressWarnings("unused")
    public static final Supplier<CreativeModeTab> QUEST_CREATIVE_TAB = CREATIVE_MODE_TAB.register(
            "quest_creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.COURIER_PACKAGE.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.cyberneticenhancements.quest"))
                    .displayItems((parameters, output) -> {
                        ModItems.QUEST_ITEMS.forEach(item -> output.accept(item.get()));
                        output.accept(ModBlocks.IRON_QUEST_CRATE.get());
                        output.accept(ModBlocks.DROPOFF_POINT.get());
                    })
                    .build()
    );

    @SuppressWarnings("unused")
    public static final Supplier<CreativeModeTab> BUILDING_CREATIVE_TAB = CREATIVE_MODE_TAB.register(
            "building_creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModBlocks.COPPER_CABLE.get().asItem().getDefaultInstance())
                    .title(Component.translatable("itemGroup.cyberneticenhancements.building"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BUILDING_GADGET.get());
                        output.accept(ModBlocks.COPPER_CABLE.get());
                        output.accept(ModBlocks.WATER_PIPE.get());
                        output.accept(ModItems.SHUTOFF_VALVE.get());
                        output.accept(ModBlocks.HVAC_DUCT.get());
                        output.accept(ModBlocks.HVAC_VENT.get());
                        output.accept(ModItems.HANGING_WIRE.get());
                    })
                    .build()
    );

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
