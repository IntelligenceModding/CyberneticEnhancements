package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

import java.util.*;

public final class FabricationContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberneticEnhancements.MOD_ID);
    public static final Map<String, String> PART_MODELS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> PARTS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<SchematicItem>> SCHEMATICS = new LinkedHashMap<>();

    static {
        part("alloy_plate", "iron_ingot");
        part("precision_chassis", "iron_trapdoor");
        part("micro_actuator", "piston");
        part("high_torque_servo", "sticky_piston");
        part("neural_interface", "comparator");
        part("neural_processor", "repeater");
        part("optical_array", "spyglass");
        part("polymer_shell", "dried_kelp");
        part("biocompatible_mesh", "honeycomb");
        part("microfluidic_pump", "bucket");
        part("thermal_regulator", "copper_ingot");
        part("high_density_capacitor", "gold_ingot");
        part("electromagnetic_coil", "tripwire_hook");
        part("firmware_module", "prismarine_shard");
        part("reinforced_alloy", "netherite_scrap");
        part("proprietary_neural_core", "heart_of_the_sea");
        part("military_actuator_core", "heavy_core");
        part("experimental_control_core", "echo_shard");
        part("precision_assembly_module", "daylight_detector");
        part("high_spec_fabrication_module", "lodestone");
        for (var definition : CyberwareCatalog.definitions())
            if (requiresSchematic(definition)) {
                String id = definition.id();
                SCHEMATICS.put(id, ITEMS.register(id + "_schematic", () -> new SchematicItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), id)));
            }
    }

    private static void part(String id, String model) {
        PART_MODELS.put(id, "minecraft:item/" + model);
        PARTS.put(id, ITEMS.registerSimpleItem(id));
    }

    public static Item part(String id) {
        var registered = PARTS.get(id);
        if (registered != null) return registered.get();

        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown fabrication material: " + id));
    }

    public static boolean requiresSchematic(CyberwareDefinition definition) {
        return definition.tier().ordinal() >= 2 || definition.slotType() == CyberwareSlotType.OPERATING_SYSTEM && definition.tier().ordinal() >= 1;
    }

    public static Component schematicName(String id) {
        return ModItems.cyberware(id).get().getDescription();
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
