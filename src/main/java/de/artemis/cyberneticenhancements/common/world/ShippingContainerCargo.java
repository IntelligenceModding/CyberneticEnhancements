package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.List;

/** What the crates of one kind of container hold. Every crate draws only from its container's own category. */
final class ShippingContainerCargo {
    private record Entry(ItemStack proto, int weight, int min, int max) {
        Entry(Item item, int weight, int min, int max) { this(new ItemStack(item), weight, min, max); }

        Entry(Holder<Potion> potion, int weight) { this(PotionContents.createItemStack(Items.POTION, potion), weight, 1, 1); }
    }

    private static final List<Entry> FOOD = List.of(
            new Entry(Items.BREAD, 10, 3, 8), new Entry(Items.COOKED_BEEF, 8, 2, 6), new Entry(Items.COOKED_PORKCHOP, 8, 2, 6),
            new Entry(Items.COOKED_COD, 8, 3, 8), new Entry(Items.COOKED_SALMON, 6, 2, 6), new Entry(Items.COOKED_CHICKEN, 6, 2, 6),
            new Entry(Items.BAKED_POTATO, 8, 4, 10), new Entry(Items.CARROT, 6, 4, 12), new Entry(Items.APPLE, 6, 3, 8),
            new Entry(Items.DRIED_KELP, 6, 6, 16), new Entry(Items.SWEET_BERRIES, 4, 4, 12), new Entry(Items.PUMPKIN_PIE, 4, 1, 4),
            new Entry(Items.COOKIE, 5, 6, 16), new Entry(Items.HONEY_BOTTLE, 3, 1, 3), new Entry(Items.GOLDEN_CARROT, 2, 1, 3),
            new Entry(Items.CAKE, 1, 1, 1));
    /** Raw materials, with a little chromium among them. */
    private static final List<Entry> METALS = List.of(
            new Entry(Items.IRON_INGOT, 12, 3, 9), new Entry(Items.COPPER_INGOT, 12, 4, 12), new Entry(Items.GOLD_INGOT, 6, 2, 5),
            new Entry(Items.IRON_NUGGET, 8, 6, 18), new Entry(Items.GOLD_NUGGET, 6, 6, 18), new Entry(Items.RAW_IRON, 8, 3, 8),
            new Entry(Items.RAW_COPPER, 8, 4, 10), new Entry(Items.RAW_GOLD, 4, 2, 5), new Entry(Items.CHAIN, 3, 2, 6),
            new Entry(ModItems.CHROMIUM_SCRAP.get(), 7, 2, 6), new Entry(ModItems.RAW_CHROMIUM.get(), 4, 1, 4),
            new Entry(ModItems.CHROMIUM_ALLOY.get(), 2, 1, 3),
            new Entry(Items.NETHERITE_SCRAP, 1, 1, 2), new Entry(Items.DIAMOND, 1, 1, 2));
    private static final List<Entry> ELECTRONICS = List.of(
            new Entry(Items.REDSTONE, 10, 6, 20), new Entry(Items.REPEATER, 5, 2, 5), new Entry(Items.COMPARATOR, 3, 1, 3),
            new Entry(Items.OBSERVER, 3, 1, 3), new Entry(Items.PISTON, 3, 1, 4), new Entry(Items.COPPER_INGOT, 6, 3, 8),
            new Entry(Items.QUARTZ, 5, 4, 10), new Entry(Items.REDSTONE_LAMP, 3, 1, 3), new Entry(Items.DAYLIGHT_DETECTOR, 2, 1, 2),
            new Entry(Items.HOPPER, 2, 1, 3), new Entry(Items.TARGET, 2, 1, 2), new Entry(Items.ENDER_PEARL, 1, 1, 2),
            new Entry(ModItems.COPPER_WIRING.get(), 9, 4, 12), new Entry(ModItems.CONDUCTIVE_PASTE.get(), 7, 3, 8),
            new Entry(ModItems.SENSOR_LENS.get(), 5, 1, 4), new Entry(ModItems.MICRO_BATTERY.get(), 5, 1, 4),
            new Entry(ModItems.BASIC_CIRCUIT_PLATE.get(), 5, 1, 4), new Entry(ModItems.SERVO_SCREWS.get(), 6, 4, 12));
    /** An arms cache: whatever a gang, a security firm or a smuggler wanted to move quietly. */
    private static final List<Entry> WEAPONS = List.of(
            new Entry(Items.ARROW, 12, 8, 24), new Entry(Items.SPECTRAL_ARROW, 4, 4, 12), new Entry(Items.BOW, 4, 1, 1),
            new Entry(Items.CROSSBOW, 4, 1, 1), new Entry(Items.IRON_SWORD, 4, 1, 1), new Entry(Items.IRON_AXE, 3, 1, 1),
            new Entry(Items.SHIELD, 4, 1, 1), new Entry(Items.GUNPOWDER, 9, 4, 12), new Entry(Items.TNT, 4, 1, 4),
            new Entry(Items.FIRE_CHARGE, 5, 2, 6), new Entry(Items.FIREWORK_ROCKET, 5, 4, 12), new Entry(Items.WIND_CHARGE, 3, 2, 6),
            new Entry(Items.CHAINMAIL_CHESTPLATE, 3, 1, 1), new Entry(Items.IRON_HELMET, 3, 1, 1), new Entry(Items.IRON_CHESTPLATE, 2, 1, 1),
            new Entry(Items.IRON_LEGGINGS, 2, 1, 1), new Entry(Items.IRON_BOOTS, 3, 1, 1), new Entry(Items.DIAMOND_SWORD, 1, 1, 1),
            new Entry(Items.DIAMOND_CHESTPLATE, 1, 1, 1));
    /** Field medicine, the kind a Trauma Team or a back-alley clinic runs on. */
    private static final List<Entry> MEDICAL = List.of(
            new Entry(Potions.HEALING, 10), new Entry(Potions.STRONG_HEALING, 3), new Entry(Potions.REGENERATION, 6),
            new Entry(Potions.LONG_REGENERATION, 2), new Entry(Potions.SWIFTNESS, 4), new Entry(Potions.FIRE_RESISTANCE, 3),
            new Entry(Items.GLISTERING_MELON_SLICE, 6, 2, 6), new Entry(Items.GOLDEN_APPLE, 3, 1, 2), new Entry(Items.HONEY_BOTTLE, 5, 2, 4),
            new Entry(Items.GLASS_BOTTLE, 6, 4, 12), new Entry(Items.PAPER, 6, 4, 12), new Entry(Items.WHITE_WOOL, 5, 4, 10),
            new Entry(Items.TOTEM_OF_UNDYING, 1, 1, 1));
    /** Parts for cyberware, shipped unassembled; the better grades are rare. */
    private static final List<Entry> COMPONENTS = List.of(
            new Entry(ModItems.COMMON_ITEM_COMPONENTS.get(), 12, 2, 6), new Entry(ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 8, 1, 4),
            new Entry(ModItems.RARE_ITEM_COMPONENTS.get(), 4, 1, 2), new Entry(ModItems.EPIC_ITEM_COMPONENTS.get(), 1, 1, 1),
            new Entry(ModItems.CHROMIUM_ALLOY.get(), 5, 1, 3), new Entry(ModItems.CHROMIUM_SCRAP.get(), 6, 2, 6),
            new Entry(ModItems.REPLACEMENT_JOINT.get(), 6, 1, 4), new Entry(ModItems.SERVO_SCREWS.get(), 6, 4, 12),
            new Entry(ModItems.SENSOR_LENS.get(), 5, 1, 4), new Entry(ModItems.MICRO_BATTERY.get(), 5, 1, 4),
            new Entry(ModItems.BASIC_CIRCUIT_PLATE.get(), 5, 1, 4), new Entry(ModItems.COPPER_WIRING.get(), 6, 4, 12));

    private ShippingContainerCargo() {}

    private static List<Entry> pool(ShippingContainerPiece.Cargo cargo) {
        return switch (cargo) {
            case FOOD -> FOOD;
            case METALS -> METALS;
            case ELECTRONICS -> ELECTRONICS;
            case WEAPONS -> WEAPONS;
            case MEDICAL -> MEDICAL;
            case COMPONENTS -> COMPONENTS;
        };
    }

    /** One crate's worth: one to four different stacks, so crates come in small and large kinds. */
    static List<ItemStack> contents(ShippingContainerPiece.Cargo cargo, RandomSource random) {
        List<Entry> pool = pool(cargo);
        List<ItemStack> stacks = new ArrayList<>();
        int total = pool.stream().mapToInt(Entry::weight).sum();
        int kinds = 1 + random.nextInt(3) + (random.nextInt(5) == 0 ? 1 : 0);
        for (int i = 0; i < kinds; i++) {
            int pick = random.nextInt(total);
            for (Entry entry : pool) {
                pick -= entry.weight();
                if (pick >= 0) continue;
                if (stacks.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, entry.proto()))) {
                    int count = Math.min(entry.proto().getMaxStackSize(), entry.min() + random.nextInt(entry.max() - entry.min() + 1));
                    stacks.add(entry.proto().copyWithCount(count));
                }
                break;
            }
        }
        return stacks;
    }
}
