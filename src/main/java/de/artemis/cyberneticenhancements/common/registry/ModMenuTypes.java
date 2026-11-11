package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu;
import de.artemis.cyberneticenhancements.common.menu.RelicCacheHackMenu;
import de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu;
import de.artemis.cyberneticenhancements.common.menu.RipperStationMenu;
import de.artemis.cyberneticenhancements.common.menu.TechStationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CyberneticEnhancements.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<de.artemis.cyberneticenhancements.common.fabrication.FabricatorMenu>> FABRICATOR =
            MENU_TYPES.register("cyberware_fabricator", () -> IMenuTypeExtension.create(de.artemis.cyberneticenhancements.common.fabrication.FabricatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RipperStationMenu>> RIPPER_STATION =
            MENU_TYPES.register("ripper_station", () -> IMenuTypeExtension.create(RipperStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<TechStationMenu>> TECH_STATION =
            MENU_TYPES.register("tech_station", () -> IMenuTypeExtension.create(TechStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<TechStationMenu>> UPGRADE_STATION =
            MENU_TYPES.register("upgrade_station", () -> IMenuTypeExtension.create(TechStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RecyclerStationMenu>> RECYCLER_STATION =
            MENU_TYPES.register("recycler_station", () -> IMenuTypeExtension.create(RecyclerStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<FixerDialogMenu>> FIXER_DIALOG =
            MENU_TYPES.register("fixer_dialog", () -> IMenuTypeExtension.create(FixerDialogMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RelicCacheHackMenu>> RELIC_CACHE_HACK =
            MENU_TYPES.register("relic_cache_hack", () -> IMenuTypeExtension.create(RelicCacheHackMenu::new));

    private ModMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
