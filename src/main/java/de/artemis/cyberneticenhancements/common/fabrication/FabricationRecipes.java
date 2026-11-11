package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.*;

public final class FabricationRecipes {
    public static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.contains(":") ? id : CyberneticEnhancements.MOD_ID + ":" + id));
    }

    private static void save(RecipeOutput output, String id, int tier, int ticks, int count, String schematic, String category, String[] pattern, Map<Character, String> key) {
        List<FabricationRecipe.Part> parts = new ArrayList<>();
        int offset = (5 - pattern.length) / 2;
        for (int row = 0; row < pattern.length; row++) {
            int x = (5 - pattern[row].length()) / 2;
            for (int col = 0; col < pattern[row].length(); col++) {
                char symbol = pattern[row].charAt(col);
                if (symbol == '.' || symbol == ' ') continue;
                Item ingredient = item(key.get(symbol));
                if (ingredient == Items.AIR)
                    throw new IllegalArgumentException("Missing fabrication part " + key.get(symbol));
                parts.add(new FabricationRecipe.Part((row + offset) * 5 + x + col, Ingredient.of(ingredient), 1));
            }
        }
        output.accept(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "fabrication/" + id), new FabricationRecipe(parts, new ItemStack(item(id), count), tier, ticks, schematic, category), null);
    }

    private static Map<Character, String> keys(String... pairs) {
        Map<Character, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i].charAt(0), pairs[i + 1]);
        return map;
    }

    private static void part(RecipeOutput output, String id, int tier, int count, String pattern, String... pairs) {
        save(output, id, tier, 60 + tier * 20, count, "", "Components", pattern.split("/"), keys(pairs));
    }

    public static void generate(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.CYBERWARE_FABRICATOR.get()).pattern("ICI").pattern("PAP").pattern("ISI")
                .define('I', Items.IRON_INGOT).define('C', ModItems.BASIC_CIRCUIT_PLATE.get()).define('P', Items.PISTON).define('A', Items.CRAFTING_TABLE).define('S', Items.SMOOTH_STONE)
                .unlockedBy("has_circuit", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BASIC_CIRCUIT_PLATE.get())).save(out);
        part(out, "alloy_plate", 1, 4, "IC/CI", "I", "minecraft:iron_ingot", "C", "minecraft:copper_ingot");
        part(out, "precision_chassis", 1, 2, "ASA/SCS/ASA", "A", "alloy_plate", "S", "servo_screws", "C", "basic_circuit_plate");
        part(out, "micro_actuator", 1, 4, "SIS/WRW/.S.", "S", "servo_screws", "I", "minecraft:iron_ingot", "W", "copper_wiring", "R", "minecraft:redstone");
        part(out, "high_torque_servo", 2, 2, "AMA/WBW/AMA", "A", "alloy_plate", "M", "micro_actuator", "W", "copper_wiring", "B", "micro_battery");
        part(out, "neural_interface", 1, 4, "GAG/WCW/.P.", "G", "minecraft:gold_nugget", "A", "minecraft:amethyst_shard", "W", "copper_wiring", "C", "basic_circuit_plate", "P", "conductive_paste");
        part(out, "neural_processor", 2, 2, "QNQ/NCN/QNQ", "Q", "minecraft:quartz", "N", "neural_interface", "C", "basic_circuit_plate");
        part(out, "optical_array", 2, 2, "LAL/GCG/.W.", "L", "sensor_lens", "A", "minecraft:amethyst_shard", "G", "minecraft:glass_pane", "C", "basic_circuit_plate", "W", "copper_wiring");
        part(out, "polymer_shell", 1, 4, "SLS/LHL", "S", "minecraft:slime_ball", "L", "minecraft:leather", "H", "minecraft:honeycomb");
        part(out, "biocompatible_mesh", 1, 4, "PHP/WPW/PHP", "P", "polymer_shell", "H", "minecraft:honeycomb", "W", "conductive_paste");
        part(out, "microfluidic_pump", 1, 2, "CGC/PMP/.W.", "C", "minecraft:copper_ingot", "G", "minecraft:glass", "P", "polymer_shell", "M", "micro_actuator", "W", "copper_wiring");
        part(out, "thermal_regulator", 1, 4, "CAC/APA/CAC", "C", "minecraft:copper_ingot", "A", "alloy_plate", "P", "conductive_paste");
        part(out, "high_density_capacitor", 2, 2, "GBG/WCW/GBG", "G", "minecraft:gold_ingot", "B", "micro_battery", "W", "copper_wiring", "C", "basic_circuit_plate");
        part(out, "electromagnetic_coil", 1, 4, "WWW/IRI/WWW", "W", "copper_wiring", "I", "minecraft:iron_ingot", "R", "minecraft:redstone");
        part(out, "firmware_module", 1, 4, "QRQ/WCW/.A.", "Q", "minecraft:quartz", "R", "minecraft:redstone", "W", "copper_wiring", "C", "basic_circuit_plate", "A", "minecraft:amethyst_shard");
        part(out, "reinforced_alloy", 3, 4, "ADA/DAD/ADA", "A", "alloy_plate", "D", "minecraft:diamond");
        part(out, "precision_assembly_module", 2, 1, "PDP/NCN/ATA", "P", "neural_processor", "D", "minecraft:diamond", "N", "neural_interface", "C", "precision_chassis", "A", "micro_actuator", "T", "thermal_regulator");
        part(out, "high_spec_fabrication_module", 4, 1, "RNR/PCP/HTH", "R", "reinforced_alloy", "N", "minecraft:netherite_ingot", "P", "neural_processor", "C", "precision_chassis", "H", "high_density_capacitor", "T", "thermal_regulator");
        for (var d : CyberwareCatalog.definitions()) cyberware(out, d);
        for (var d : ChipwareCatalog.definitions())
            save(out, d.id(), d.tier().ordinal() + 1, 100, 1, "", "Chipware", new String[]{".F.", "NCN", ".W."}, keys("F", "firmware_module", "N", "neural_interface", "C", "basic_circuit_plate", "W", "copper_wiring"));
        for (var d : CyberwareModuleCatalog.definitions())
            save(out, d.id(), d.tier().ordinal() + 1, 160, 1, "", "Modules", new String[]{"AMA", "WFW", ".J."}, keys("A", "alloy_plate", "M", "micro_actuator", "W", "copper_wiring", "F", "firmware_module", "J", "replacement_joint"));
        Map<String, String> salvage = Map.ofEntries(
                Map.entry("alloy_plate", "minecraft:iron_nugget"), Map.entry("precision_chassis", "alloy_plate"),
                Map.entry("micro_actuator", "minecraft:iron_nugget"), Map.entry("high_torque_servo", "micro_actuator"),
                Map.entry("neural_interface", "minecraft:gold_nugget"), Map.entry("neural_processor", "neural_interface"),
                Map.entry("optical_array", "sensor_lens"), Map.entry("polymer_shell", "minecraft:string"),
                Map.entry("biocompatible_mesh", "polymer_shell"), Map.entry("microfluidic_pump", "polymer_shell"),
                Map.entry("thermal_regulator", "minecraft:copper_ingot"), Map.entry("high_density_capacitor", "micro_battery"),
                Map.entry("electromagnetic_coil", "copper_wiring"), Map.entry("firmware_module", "minecraft:redstone"),
                Map.entry("reinforced_alloy", "alloy_plate"));
        salvage.forEach((source, target) -> out.accept(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "recycling/" + source),
                new de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe("fabrication", Ingredient.of(item(source)),
                        List.of(new ItemStack(item(target))), List.of(new ItemStack(item(target))), List.of(new ItemStack(item(target)))), null));
    }

    private static void cyberware(RecipeOutput out, CyberwareDefinition d) {
        int tier = d.tier().ordinal() + 1;
        String shell = tier >= 4 ? "reinforced_alloy" : "alloy_plate";
        String primary = switch (d.slotType()) {
            case FACE -> tier >= 3 ? "optical_array" : "sensor_lens";
            case ARMS, LEGS -> tier >= 3 ? "high_torque_servo" : "micro_actuator";
            case SKELETON -> tier >= 3 ? "reinforced_alloy" : "replacement_joint";
            case CIRCULATORY_SYSTEM -> "microfluidic_pump";
            case INTEGUMENTARY_SYSTEM -> "biocompatible_mesh";
            case HANDS -> "micro_actuator";
            default -> tier >= 2 ? "neural_processor" : "neural_interface";
        };
        String variant = d.id().contains("thermal") ? "thermal_regulator" : d.id().contains("toxic") || d.id().contains("blood") ? "microfluidic_pump" : d.id().contains("electr") || d.id().contains("shock") ? "electromagnetic_coil" : d.id().contains("smart") || d.id().contains("target") ? "optical_array" : "firmware_module";
        String center = d.upgradeFromId() != null ? d.upgradeFromId() : "precision_chassis";
        String core = d.id().equals("militech_experimental_sandevistan") ? "experimental_control_core" : switch (d.slotType()) {
            case ARMS, LEGS, SKELETON, HANDS -> "military_actuator_core";
            default -> "proprietary_neural_core";
        };
        Map<Character, String> key = keys("A", shell, "P", primary, "N", "neural_interface", "W", "copper_wiring", "C", center, "B", "micro_battery", "V", variant, "T", "thermal_regulator", "M", "biocompatible_mesh", "L", "sensor_lens", "R", tier == 5 ? core : "basic_circuit_plate");
        String[] pattern = switch (d.slotType()) {
            case FACE -> new String[]{"..L..", ".APA.", ".WCW.", "..N..", tier >= 3 ? ".VRV." : "..B.."};
            case ARMS, LEGS -> new String[]{"A...A", "PN.NP", "WVCVW", "PN.NP", tier >= 3 ? "ARBRA" : "A.B.A"};
            case SKELETON -> new String[]{"A.A.A", ".PNP.", "AWCWA", ".PNP.", tier >= 3 ? "AVRVA" : "A.B.A"};
            case CIRCULATORY_SYSTEM -> new String[]{".MVM.", "MPNPM", ".WCW.", "MPNPM", tier >= 3 ? ".BRB." : "..B.."};
            case INTEGUMENTARY_SYSTEM ->
                    new String[]{"MAMAM", ".PNP.", "MWCWM", ".PNP.", tier >= 3 ? "AVRVA" : "MABAM"};
            case HANDS -> new String[]{"..P..", ".PNP.", ".WCW.", ".AVA.", tier >= 3 ? ".BRB." : "..B.."};
            default -> new String[]{"..T..", ".PWP.", "ANCNA", ".VBV.", tier >= 3 ? ".ARA." : "....."};
        };
        save(out, d.id(), tier, new int[]{0, 100, 200, 360, 600, 1000}[tier], 1, FabricationContent.requiresSchematic(d) ? d.id() : "", d.slotType().name().replace('_', ' '), pattern, key);
    }
}
