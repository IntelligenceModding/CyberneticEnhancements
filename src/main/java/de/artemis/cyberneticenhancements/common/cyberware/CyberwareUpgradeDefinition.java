package de.artemis.cyberneticenhancements.common.cyberware;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

 public record CyberwareUpgradeDefinition(List<Level> levels, List<Material> repairMaterials) {
    public static final Level BASE = new Level(Map.of(), 0, 0, 0, 1, 1, 1, 1, List.of());
    public CyberwareUpgradeDefinition { levels = List.copyOf(levels); repairMaterials = List.copyOf(repairMaterials); }
    public CyberwareUpgradeDefinition(List<Level> levels) { this(levels, List.of()); }
    public Level at(int level) { return level <= 0 || levels.isEmpty() ? BASE : levels.get(Math.min(level, levels.size()) - 1); }

    public record Material(ResourceLocation item, int count) {}
    public record Level(Map<String, Double> effects, int capacityBonus, int chipSlotBonus,
            int moduleSlotBonus, double durationMultiplier, double cooldownMultiplier,
            double instabilityMultiplier, double functionMultiplier, List<Material> materials) {
        public Level { effects = Map.copyOf(effects); materials = List.copyOf(materials); }
        public long duration(long ticks) { return Math.round(ticks * durationMultiplier); }
        public long cooldown(long ticks) { return Math.max(20, Math.round(ticks * cooldownMultiplier)); }
    }

    public static CyberwareUpgradeDefinition parse(JsonElement json) {
        JsonObject root = json.getAsJsonObject();
        fields(root, Set.of("levels", "repair_materials"));
        JsonArray array = root.getAsJsonArray("levels");
        if (array == null || array.size() > 32) throw new IllegalArgumentException("Expected levels array with at most 32 entries");
        List<Level> levels = new ArrayList<>();
        for (JsonElement entry : array) {
            JsonObject level = entry.getAsJsonObject();
            fields(level, Set.of("effects", "capacity_bonus", "chip_slot_bonus", "module_slot_bonus",
                    "duration_multiplier", "cooldown_multiplier", "instability_multiplier", "function_multiplier", "materials"));
            Map<String, Double> effects = new HashMap<>();
            if (level.has("effects")) for (var effect : level.getAsJsonObject("effects").entrySet()) {
                effects.put(effect.getKey(), number(effect.getValue(), -10000, 10000));
            }
            JsonArray costs = level.getAsJsonArray("materials");
            if (costs == null || costs.isEmpty() || costs.size() > 2) throw new IllegalArgumentException("Expected 1 or 2 upgrade materials");
            List<Material> materials = new ArrayList<>();
            Set<ResourceLocation> items = new HashSet<>();
            for (JsonElement cost : costs) {
                JsonObject material = cost.getAsJsonObject();
                fields(material, Set.of("item", "count"));
                var item = ResourceLocation.parse(material.get("item").getAsString());
                if (!items.add(item)) throw new IllegalArgumentException("Duplicate material: " + item);
                materials.add(new Material(item, integer(material, "count", 1, 64, 1)));
            }
            levels.add(new Level(effects, integer(level, "capacity_bonus", 0, 10000, 0),
                    integer(level, "chip_slot_bonus", 0, 16, 0), integer(level, "module_slot_bonus", 0, 16, 0),
                    optional(level, "duration_multiplier", 0.1, 10, 1), optional(level, "cooldown_multiplier", 0.05, 10, 1),
                    optional(level, "instability_multiplier", 0, 10, 1), optional(level, "function_multiplier", 1, 3, 1), materials));
        }
        List<Material> repairs = new ArrayList<>();
        if (root.has("repair_materials")) {
            JsonArray costs = root.getAsJsonArray("repair_materials");
            if (costs.isEmpty() || costs.size() > 2) throw new IllegalArgumentException("Expected 1 or 2 repair materials");
            Set<ResourceLocation> items = new HashSet<>();
            for (JsonElement cost : costs) {
                JsonObject material = cost.getAsJsonObject();
                fields(material, Set.of("item", "count"));
                var item = ResourceLocation.parse(material.get("item").getAsString());
                if (!items.add(item)) throw new IllegalArgumentException("Duplicate repair material: " + item);
                repairs.add(new Material(item, integer(material, "count", 1, 64, 1)));
            }
        }
        return new CyberwareUpgradeDefinition(levels, repairs);
    }

    private static void fields(JsonObject object, Set<String> allowed) {
        for (String field : object.keySet()) if (!allowed.contains(field)) throw new IllegalArgumentException("Unknown field: " + field);
    }
    private static double number(JsonElement value, double min, double max) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected number");
        double result = value.getAsDouble();
        if (!Double.isFinite(result) || result < min || result > max) throw new IllegalArgumentException("Number outside " + min + ".." + max);
        return result;
    }
    private static double optional(JsonObject object, String key, double min, double max, double fallback) {
        return object.has(key) ? number(object.get(key), min, max) : fallback;
    }
    private static int integer(JsonObject object, String key, int min, int max, int fallback) {
        double value = optional(object, key, min, max, fallback);
        if (value != Math.rint(value)) throw new IllegalArgumentException("Expected integer: " + key);
        return (int) value;
    }
}
