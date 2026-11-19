package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.fabrication.FabricationContent;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class NpcShopCatalog {
    private static final Map<NpcType, List<Offer>> STOCK = Map.of(
            NpcType.FIXER, List.of(
                    offer("fixer", "maxdoc", 0, () -> consumable("maxdoc_mk2", 1)),
                    offer("fixer", "ram_jolt", 1, () -> consumable("ram_jolt", 1)),
                    offer("fixer", "memory_boost", 2, () -> cyberware("memory_boost")),
                    offer("fixer", "immuno", 3, () -> consumable("immunoblockers", 1))),
            NpcType.RIPPERDOC, List.of(
                    offer("ripperdoc", "maxdoc", 0, () -> consumable("maxdoc_mk2", 2)),
                    offer("ripperdoc", "bounce_back", 1, () -> consumable("bounce_back_mk2", 1)),
                    offer("ripperdoc", "kiroshi", 2, () -> cyberware("basic_kiroshi_optics")),
                    offer("ripperdoc", "bionic_joints", 3, () -> cyberware("bionic_joints")),
                    offer("ripperdoc", "chromium_scrap", 4, () -> new ItemStack(ModItems.CHROMIUM_SCRAP.get(), 6))),
            NpcType.TECHIE, List.of(
                    offer("techie", "components", 0, () -> new ItemStack(FabricationContent.part("neural_processor"), 2)),
                    offer("techie", "precision_miner", 1, () -> cyberware("precision_miner")),
                    offer("techie", "masons_grip", 2, () -> cyberware("masons_grip")),
                    offer("techie", "cargo_spine", 3, () -> cyberware("cargo_spine")),
                    offer("techie", "common_components", 4, () -> new ItemStack(FabricationContent.part("common_item_components"), 4)),
                    offer("techie", "uncommon_components", 5, () -> new ItemStack(FabricationContent.part("uncommon_item_components"), 2)),
                    offer("techie", "rare_components", 6, () -> new ItemStack(FabricationContent.part("rare_item_components"), 2)),
                    offer("techie", "epic_components", 7, () -> new ItemStack(FabricationContent.part("epic_item_components"))),
                    offer("techie", "legendary_components", 8, () -> new ItemStack(FabricationContent.part("legendary_item_components"))),
                    offer("techie", "chromium_alloy", 9, () -> new ItemStack(ModItems.CHROMIUM_ALLOY.get(), 4)),
                    offer("techie", "chromium_survey", 10, () -> new ItemStack(ModItems.CHROMIUM_SURVEY.get()))),
            NpcType.NETRUNNER, List.of(
                    offer("netrunner", "ram_jolt", 0, () -> consumable("ram_jolt", 2)),
                    offer("netrunner", "memory_boost", 1, () -> cyberware("memory_boost")),
                    offer("netrunner", "ram_upgrade", 2, () -> cyberware("ram_upgrade")),
                    offer("netrunner", "chipware_socket", 3, () -> cyberware("chipware_socket"))),
            NpcType.MERC, List.of(
                    offer("merc", "maxdoc", 0, () -> consumable("maxdoc_mk2", 1)),
                    offer("merc", "handle_wrap", 1, () -> cyberware("handle_wrap")),
                    offer("merc", "fortified_ankles", 2, () -> cyberware("fortified_ankles")),
                    offer("merc", "subdermal_armor", 3, () -> cyberware("subdermal_armor")),
                    offer("merc", "chromium_scrap", 4, () -> new ItemStack(ModItems.CHROMIUM_SCRAP.get(), 8))),
            // A wandering doctor carries real implants, but only the everyday tiers; nothing top-shelf travels in a coat.
            NpcType.STREET_RIPPERDOC, List.of(
                    offer("street_ripperdoc", "maxdoc", 0, () -> consumable("maxdoc_mk2", 3)),
                    offer("street_ripperdoc", "bounce_back", 1, () -> consumable("bounce_back_mk2", 2)),
                    offer("street_ripperdoc", "immuno", 2, () -> consumable("immunoblockers", 1)),
                    offer("street_ripperdoc", "kiroshi", 3, () -> cyberware("basic_kiroshi_optics")),
                    offer("street_ripperdoc", "bionic_joints", 4, () -> cyberware("bionic_joints")),
                    offer("street_ripperdoc", "titanium_bones", 5, () -> cyberware("titanium_bones")),
                    offer("street_ripperdoc", "scar_coalescer", 6, () -> cyberware("scar_coalescer")),
                    offer("street_ripperdoc", "ex_disk", 7, () -> cyberware("ex_disk")),
                    offer("street_ripperdoc", "dense_marrow", 8, () -> cyberware("dense_marrow")),
                    offer("street_ripperdoc", "spring_joints", 9, () -> cyberware("spring_joints")),
                    offer("street_ripperdoc", "vein_reader", 10, () -> cyberware("vein_reader_optics")),
                    offer("street_ripperdoc", "para_bellum", 11, () -> cyberware("para_bellum")),
                    offer("street_ripperdoc", "universal_booster", 12, () -> cyberware("universal_booster"))));
    private static final List<Offer> TEST_STOCK = Stream.of(NpcType.NETRUNNER, NpcType.FIXER, NpcType.RIPPERDOC, NpcType.TECHIE, NpcType.MERC)
            .flatMap(type -> STOCK.get(type).stream()).toList();

    private NpcShopCatalog() {}

    public static List<Offer> offers(NpcType type) {
        return STOCK.getOrDefault(type == NpcType.MED_TECH ? NpcType.RIPPERDOC : type, List.of());
    }

    public static List<Offer> testOffers() {
        return TEST_STOCK;
    }

    private static Offer offer(String role, String label, int index, Supplier<ItemStack> stack) {
        return new Offer("screen.cyberneticenhancements.npc.shop." + role + "." + label,
                "screen.cyberneticenhancements.npc.shopdesc." + role + "." + index, "shop." + role + "." + label, stack);
    }

    private static ItemStack consumable(String id, int count) {
        return new ItemStack(ModItems.consumable(id).get(), count);
    }

    private static ItemStack cyberware(String id) {
        return new ItemStack(ModItems.cyberware(id).get());
    }

    public record Offer(String labelKey, String descriptionKey, String priceKey, Supplier<ItemStack> item) {
        public int requiredTrust() { return de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("access." + priceKey); }
        public int basePrice() { return de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value(priceKey); }
        public ItemStack stack() { return item.get().copy(); }
        public Component label() { return Component.translatable(labelKey); }
        public Component description() { return Component.translatable(descriptionKey); }
    }
}
