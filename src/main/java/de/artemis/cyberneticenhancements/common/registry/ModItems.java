package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCatalog;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.ChipwareCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.ChipwareDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.item.CyberConsumableItem;
import de.artemis.cyberneticenhancements.common.item.ChipwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareModuleItem;
import de.artemis.cyberneticenhancements.common.item.StyledNameItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberneticEnhancements.MOD_ID);

    public static final DeferredItem<Item> COPPER_WIRING = register("copper_wiring", Item::new, UnaryOperator.identity());
    public static final DeferredItem<Item> CONDUCTIVE_PASTE = register("conductive_paste", Item::new, UnaryOperator.identity());
    public static final DeferredItem<Item> SERVO_SCREWS = register("servo_screws", Item::new, UnaryOperator.identity());
    public static final DeferredItem<Item> SENSOR_LENS = register("sensor_lens", Item::new, UnaryOperator.identity());
    public static final DeferredItem<Item> MICRO_BATTERY = register("micro_battery", Item::new, properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> REPLACEMENT_JOINT = register("replacement_joint", Item::new, UnaryOperator.identity());
    public static final DeferredItem<Item> BASIC_CIRCUIT_PLATE = register("basic_circuit_plate", Item::new, properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> COMMON_ITEM_COMPONENTS = register("common_item_components", properties -> new StyledNameItem(properties, CyberwareTier.TIER_1.getColor()), UnaryOperator.identity());
    public static final DeferredItem<Item> UNCOMMON_ITEM_COMPONENTS = register("uncommon_item_components", properties -> new StyledNameItem(properties.rarity(Rarity.UNCOMMON), CyberwareTier.TIER_2.getColor()), UnaryOperator.identity());
    public static final DeferredItem<Item> RARE_ITEM_COMPONENTS = register("rare_item_components", properties -> new StyledNameItem(properties.rarity(Rarity.RARE), CyberwareTier.TIER_3.getColor()), UnaryOperator.identity());
    public static final DeferredItem<Item> EPIC_ITEM_COMPONENTS = register("epic_item_components", properties -> new StyledNameItem(properties.rarity(Rarity.EPIC), CyberwareTier.TIER_4.getColor()), UnaryOperator.identity());
    public static final DeferredItem<Item> LEGENDARY_ITEM_COMPONENTS = register("legendary_item_components", properties -> new StyledNameItem(properties.rarity(Rarity.EPIC), CyberwareTier.TIER_5.getColor()), UnaryOperator.identity());
    public static final DeferredItem<Item> COURIER_PACKAGE = register(
            "courier_package",
            Item::new,
            properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON)
    );
    public static final DeferredItem<Item> FIXER_SPAWN_EGG = register(
            "fixer_spawn_egg",
            properties -> new SpawnEggItem(ModEntityTypes.FIXER.get(), 0x24323E, 0x1EE2B5, properties),
            UnaryOperator.identity()
    );
    public static final DeferredItem<Item> RIPPERDOC_SPAWN_EGG = register(
            "ripperdoc_spawn_egg",
            properties -> new SpawnEggItem(ModEntityTypes.RIPPERDOC.get(), 0x2E4052, 0x7FFFD4, properties),
            UnaryOperator.identity()
    );
    public static final DeferredItem<Item> TECHIE_SPAWN_EGG = register(
            "techie_spawn_egg",
            properties -> new SpawnEggItem(ModEntityTypes.TECHIE.get(), 0x66452E, 0xF8C471, properties),
            UnaryOperator.identity()
    );
    public static final DeferredItem<Item> NETRUNNER_SPAWN_EGG = register(
            "netrunner_spawn_egg",
            properties -> new SpawnEggItem(ModEntityTypes.NETRUNNER.get(), 0x24303C, 0x8E44AD, properties),
            UnaryOperator.identity()
    );
    public static final DeferredItem<Item> MERC_SPAWN_EGG = register(
            "merc_spawn_egg",
            properties -> new SpawnEggItem(ModEntityTypes.MERC.get(), 0x3B3B3B, 0xC0392B, properties),
            UnaryOperator.identity()
    );

    private static final Map<String, DeferredItem<CyberwareItem>> CYBERWARE_BY_ID = createCyberwareRegistry();

    public static final List<DeferredItem<Item>> MATERIALS = List.of(
            COPPER_WIRING,
            CONDUCTIVE_PASTE,
            SERVO_SCREWS,
            SENSOR_LENS,
            MICRO_BATTERY,
            REPLACEMENT_JOINT,
            BASIC_CIRCUIT_PLATE,
            COMMON_ITEM_COMPONENTS,
            UNCOMMON_ITEM_COMPONENTS,
            RARE_ITEM_COMPONENTS,
            EPIC_ITEM_COMPONENTS,
            LEGENDARY_ITEM_COMPONENTS,
            COURIER_PACKAGE,
            FIXER_SPAWN_EGG,
            RIPPERDOC_SPAWN_EGG,
            TECHIE_SPAWN_EGG,
            NETRUNNER_SPAWN_EGG,
            MERC_SPAWN_EGG
    );

    private static final List<DeferredItem<CyberwareItem>> CYBERWARE_ITEMS = List.copyOf(CYBERWARE_BY_ID.values());
    private static final Set<String> GAMEPLAY_CYBERWARE_IDS = Set.of(
            "excavator_arms",
            "forager_lens",
            "vein_reader_optics",
            "relic_scanner",
            "cargo_spine",
            "precision_miner",
            "masons_grip",
            "harvester_hands",
            "kiroshi_retrieval_suite",
            "hydrolung",
            "brushstep_legs"
    );
    private static final List<DeferredItem<CyberwareItem>> GAMEPLAY_CYBERWARE_ITEMS = CYBERWARE_ITEMS.stream()
            .filter(item -> GAMEPLAY_CYBERWARE_IDS.contains(item.getId().getPath()))
            .toList();
    private static final List<DeferredItem<CyberwareItem>> STANDARD_CYBERWARE_ITEMS = CYBERWARE_ITEMS.stream()
            .filter(item -> !GAMEPLAY_CYBERWARE_IDS.contains(item.getId().getPath()))
            .toList();
    private static final Map<String, DeferredItem<ChipwareItem>> CHIPWARE_BY_ID = createChipwareRegistry();
    private static final List<DeferredItem<ChipwareItem>> CHIPWARE_ITEMS = List.copyOf(CHIPWARE_BY_ID.values());
    private static final Map<String, DeferredItem<CyberwareModuleItem>> MODULES_BY_ID = createModuleRegistry();
    private static final List<DeferredItem<CyberwareModuleItem>> MODULE_ITEMS = List.copyOf(MODULES_BY_ID.values());
    private static final Map<String, DeferredItem<CyberConsumableItem>> CONSUMABLES_BY_ID = createConsumableRegistry();
    private static final List<DeferredItem<CyberConsumableItem>> CONSUMABLE_ITEMS = List.copyOf(CONSUMABLES_BY_ID.values());

    private ModItems() {
    }

    private static Map<String, DeferredItem<CyberwareItem>> createCyberwareRegistry() {
        Map<String, DeferredItem<CyberwareItem>> entries = new LinkedHashMap<>();
        for (CyberwareDefinition definition : CyberwareCatalog.definitions()) {
            entries.put(definition.id(), registerCyberware(definition));
        }
        return entries;
    }

    private static Map<String, DeferredItem<CyberConsumableItem>> createConsumableRegistry() {
        Map<String, DeferredItem<CyberConsumableItem>> entries = new LinkedHashMap<>();
        for (CyberConsumableDefinition definition : CyberConsumableCatalog.definitions()) {
            entries.put(definition.id(), registerConsumable(definition));
        }
        return entries;
    }

    private static Map<String, DeferredItem<ChipwareItem>> createChipwareRegistry() {
        Map<String, DeferredItem<ChipwareItem>> entries = new LinkedHashMap<>();
        for (ChipwareDefinition definition : ChipwareCatalog.definitions()) {
            entries.put(definition.id(), registerChipware(definition));
        }
        return entries;
    }

    private static Map<String, DeferredItem<CyberwareModuleItem>> createModuleRegistry() {
        Map<String, DeferredItem<CyberwareModuleItem>> entries = new LinkedHashMap<>();
        for (CyberwareModuleDefinition definition : CyberwareModuleCatalog.definitions()) {
            entries.put(definition.id(), registerModule(definition));
        }
        return entries;
    }

    private static <T extends Item> DeferredItem<T> register(
            String name,
            Function<Item.Properties, T> itemFactory,
            UnaryOperator<Item.Properties> properties
    ) {
        return ITEMS.registerItem(name, itemFactory, properties.apply(new Item.Properties()));
    }

    private static DeferredItem<CyberwareItem> registerCyberware(CyberwareDefinition definition) {
        return register(
                definition.id(),
                properties -> new CyberwareItem(properties.rarity(definition.tier().getRarity()), definition),
                UnaryOperator.identity()
        );
    }

    private static DeferredItem<CyberConsumableItem> registerConsumable(CyberConsumableDefinition definition) {
        return register(
                definition.id(),
                properties -> new CyberConsumableItem(properties.rarity(definition.rarity()), definition),
                UnaryOperator.identity()
        );
    }

    private static DeferredItem<CyberwareModuleItem> registerModule(CyberwareModuleDefinition definition) {
        return register(
                definition.id(),
                properties -> new CyberwareModuleItem(properties.rarity(definition.tier().getRarity()), definition),
                UnaryOperator.identity()
        );
    }

    private static DeferredItem<ChipwareItem> registerChipware(ChipwareDefinition definition) {
        return register(
                definition.id(),
                properties -> new ChipwareItem(properties.rarity(definition.tier().getRarity()), definition),
                UnaryOperator.identity()
        );
    }

    public static DeferredItem<CyberwareItem> cyberware(String id) {
        DeferredItem<CyberwareItem> item = CYBERWARE_BY_ID.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown cyberware item id: " + id);
        }
        return item;
    }

    public static List<DeferredItem<CyberwareItem>> cyberwareItems() {
        return CYBERWARE_ITEMS;
    }

    public static List<DeferredItem<CyberwareItem>> standardCyberwareItems() {
        return STANDARD_CYBERWARE_ITEMS;
    }

    public static List<DeferredItem<CyberwareItem>> gameplayCyberwareItems() {
        return GAMEPLAY_CYBERWARE_ITEMS;
    }

    public static DeferredItem<ChipwareItem> chipware(String id) {
        DeferredItem<ChipwareItem> item = CHIPWARE_BY_ID.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown chipware item id: " + id);
        }
        return item;
    }

    public static List<DeferredItem<ChipwareItem>> chipwareItems() {
        return CHIPWARE_ITEMS;
    }

    public static DeferredItem<CyberwareModuleItem> module(String id) {
        DeferredItem<CyberwareModuleItem> item = MODULES_BY_ID.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown cyberware module item id: " + id);
        }
        return item;
    }

    public static List<DeferredItem<CyberwareModuleItem>> moduleItems() {
        return MODULE_ITEMS;
    }

    public static DeferredItem<CyberConsumableItem> consumable(String id) {
        DeferredItem<CyberConsumableItem> item = CONSUMABLES_BY_ID.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown consumable item id: " + id);
        }
        return item;
    }

    public static List<DeferredItem<CyberConsumableItem>> consumableItems() {
        return CONSUMABLE_ITEMS;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
