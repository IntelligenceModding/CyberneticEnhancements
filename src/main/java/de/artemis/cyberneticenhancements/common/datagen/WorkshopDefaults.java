package de.artemis.cyberneticenhancements.common.datagen;

import com.google.gson.*;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.fabrication.FabricationContent;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe;
import net.minecraft.data.*;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;
import java.util.concurrent.CompletableFuture;

 public final class WorkshopDefaults implements DataProvider {
    private final PackOutput output;
    public WorkshopDefaults(PackOutput output) { this.output = output; }
    private static String id(String path) { return CyberneticEnhancements.MOD_ID + ":" + path; }

    public static String repairPart(CyberwareDefinition d) {
        return switch (d.slotType()) {
            case FACE -> "sensor_lens";
            case ARMS, LEGS -> "micro_actuator";
            case HANDS, SKELETON -> "replacement_joint";
            case CIRCULATORY_SYSTEM -> "biocompatible_mesh";
            case INTEGUMENTARY_SYSTEM -> "polymer_shell";
            default -> "basic_circuit_plate";
        };
    }
    public static String upgradePart(CyberwareDefinition d) {
        int tier = d.tier().ordinal() + 1;
        return switch (d.slotType()) {
            case FACE -> tier >= 3 ? "optical_array" : "sensor_lens";
            case ARMS, LEGS -> tier >= 3 ? "high_torque_servo" : "micro_actuator";
            case HANDS -> d.id().contains("smart") ? "optical_array" : "micro_actuator";
            case SKELETON -> tier >= 3 ? "reinforced_alloy" : "alloy_plate";
            case CIRCULATORY_SYSTEM -> "microfluidic_pump";
            case INTEGUMENTARY_SYSTEM -> "biocompatible_mesh";
            default -> tier >= 3 ? "neural_processor" : "neural_interface";
        };
    }
    private static JsonObject material(String item, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("item", id(item)); json.addProperty("count", count);
        return json;
    }

    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> saves = new ArrayList<>();
        var paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "cyberware_upgrades");
        for (var d : CyberwareCatalog.definitions()) {
            int tier = d.tier().ordinal() + 1;
            int cap = Math.min(5, tier + 1);
            String components = List.of("common", "uncommon", "rare", "epic", "legendary").get(tier - 1) + "_item_components";
            JsonObject root = new JsonObject();
            JsonArray repairs = new JsonArray();
            repairs.add(material(components, tier <= 2 ? 3 : 2));
            repairs.add(material(repairPart(d), tier >= 3 ? 2 : 1));
            root.add("repair_materials", repairs);
            JsonArray levels = new JsonArray();
            for (int level = 1; level <= cap; level++) {
                JsonObject json = benefit(d, level);
                JsonArray costs = new JsonArray();
                int bundles = tier == 1 ? 2 + level : tier <= 3 ? 1 + (level - 1) / 2 : 1 + (level - 1) / 4;
                int parts = tier == 1 ? 1 : tier == 5 ? 1 + (level - 1) / 2 : 1 + (level - 1) / 3;
                costs.add(material(components, bundles));
                costs.add(material(upgradePart(d), parts));
                json.add("materials", costs);
                levels.add(json);
            }
            root.add("levels", levels);
            saves.add(DataProvider.saveStable(cache, root, paths.json(ResourceLocation.parse(id(d.id())))));
        }
        return CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new));
    }

    private static JsonObject benefit(CyberwareDefinition d, int level) {
        JsonObject json = new JsonObject();
        String name = d.id();
        if (name.contains("sandevistan") || Set.of("militech_apogee", "militech_falcon", "qiant_warp_dancer").contains(name)) {
            json.addProperty("duration_multiplier", 1 + level * 0.05);
            json.addProperty("cooldown_multiplier", 1 - level * 0.04);
            json.addProperty("instability_multiplier", 1 - level * 0.05);
        } else if (d.slotType() == CyberwareSlotType.FACE || Set.of("blood_pump", "biomonitor", "second_heart").contains(name)) {
            json.addProperty("function_multiplier", 1 + level * 0.10);
        } else if (d.capacityBonus() > 0 || d.effects().stream().anyMatch(e -> e.type() == CyberwareEffectType.CHROME_CAPACITY)) {
            json.addProperty("capacity_bonus", level * 2);
        } else if (d.chipSlotCount() > 0) {
            json.addProperty("capacity_bonus", level);
        } else {
            var effect = d.effects().stream().filter(e -> !e.type().isMobEffect() && e.amount() != 0).findFirst();
            JsonObject effects = new JsonObject();
            if (name.equals("reinforced_tendons")) effects.addProperty("jump_power", level * 0.04);
            else if (effect.isPresent()) {
                var e = effect.get();
                double increment = switch (e.type()) {
                    case MAX_HEALTH, ARMOR, ATTACK_DAMAGE -> 0.25;
                    case MOVEMENT_SPEED, DAMAGE_REDUCTION, FALL_DAMAGE_REDUCTION, KNOCKBACK_RESISTANCE -> 0.01;
                    case HEALTH_REGEN -> 0.05;
                    case BLOCK_BREAK_SPEED, JUMP_POWER -> 0.025;
                    default -> Math.max(0.025, Math.abs(e.amount()) * 0.1);
                };
                effects.addProperty(e.type().id(), Math.copySign(increment * level, e.amount()));
            } else if (name.equals("harvester_hands")) effects.addProperty("block_break_speed", level * 0.025);
            else if (CyberwareUpgradeHelper.supportsFunction(name)) json.addProperty("function_multiplier", 1 + level * 0.10);
            else throw new IllegalArgumentException("Missing device-specific upgrade for " + name);
            if (!effects.isEmpty()) json.add("effects", effects);
        }
        return json;
    }

    public static void recycling(RecipeOutput out) {
        for (var d : CyberwareCatalog.definitions()) {
            String primary = switch (d.slotType()) {
                case FACE -> d.tier().ordinal() >= 2 ? "optical_array" : "sensor_lens";
                case ARMS, LEGS -> d.tier().ordinal() >= 2 ? "high_torque_servo" : "micro_actuator";
                case SKELETON -> "alloy_plate";
                case HANDS -> "micro_actuator";
                case CIRCULATORY_SYSTEM -> "microfluidic_pump";
                case INTEGUMENTARY_SYSTEM -> "biocompatible_mesh";
                default -> d.tier().ordinal() >= 1 ? "neural_processor" : "neural_interface";
            };
            String secondary = switch (d.slotType()) {
                case FACE -> "copper_wiring";
                case ARMS, LEGS, SKELETON, HANDS -> "alloy_plate";
                case CIRCULATORY_SYSTEM, INTEGUMENTARY_SYSTEM -> "polymer_shell";
                default -> "basic_circuit_plate";
            };
            var healthy = batch(primary, secondary, d.tier().ordinal() >= 2 ? "thermal_regulator" : null);
            var worn = batch(primary, secondary, null);
            var damaged = batch(secondary, null, null);
            save(out, d.id(), ModItems.cyberware(d.id()).get(), healthy, worn, damaged);
        }
        for (var d : ChipwareCatalog.definitions()) constant(out, d.id(), ModItems.chipware(d.id()).get(), "firmware_module");
        for (var d : CyberwareModuleCatalog.definitions()) constant(out, d.id(), ModItems.module(d.id()).get(), "micro_actuator");
        for (var d : de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCatalog.definitions()) {
            String part = switch (d.category()) {
                case MEDICAL, SUPPRESSANT -> "conductive_paste";
                case NEURAL -> "basic_circuit_plate";
                case BOOSTER, STREET -> "copper_wiring";
            };
            constant(out, d.id(), ModItems.consumable(d.id()).get(), part);
        }
    }
    private static List<ItemStack> batch(String first, String second, String third) {
        var batch = new ArrayList<ItemStack>();
        for (String part : new String[]{first, second, third})
            if (part != null && batch.stream().noneMatch(s -> s.is(FabricationContent.part(part))))
                batch.add(new ItemStack(FabricationContent.part(part)));
        return batch;
    }
    public static void constant(RecipeOutput out, String id, Item input, String part) {
        var batch = batch(part, null, null);
        save(out, id, input, batch, batch, batch);
    }
    private static void save(RecipeOutput out, String name, Item input, List<ItemStack> healthy, List<ItemStack> worn, List<ItemStack> damaged) {
        out.accept(ResourceLocation.parse(id("recycling/" + name)), new RecyclerRecipe("", Ingredient.of(input), healthy, worn, damaged), null);
    }
    public String getName() { return "Cyberware workshop defaults"; }
}
