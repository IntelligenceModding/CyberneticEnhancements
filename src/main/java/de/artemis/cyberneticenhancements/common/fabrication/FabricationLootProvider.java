package de.artemis.cyberneticenhancements.common.fabrication;

import com.google.gson.*;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import net.minecraft.data.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class FabricationLootProvider implements DataProvider {
    private final PackOutput output;

    public FabricationLootProvider(PackOutput output) {
        this.output = output;
    }

    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int tier = 2; tier <= 5; tier++) {
            JsonArray entries = new JsonArray();
            for (String id : FabricationContent.SCHEMATICS.keySet()) {
                var d = CyberwareCatalog.get(id);
                if (d.tier().ordinal() + 1 <= tier && d.acquisitionMethod() != CyberwareAcquisitionMethod.QUEST)
                    entry(entries, "cyberneticenhancements:" + id + "_schematic", d.tier().ordinal() + 1 == tier ? 3 : 1);
            }
            futures.add(save(cache, "schematics_" + tier, entries));
            var parts = new JsonArray();
            entry(parts, "cyberneticenhancements:chromium_scrap", 5);
            entry(parts, "cyberneticenhancements:neural_processor", 8);
            entry(parts, "cyberneticenhancements:high_torque_servo", 8);
            entry(parts, "cyberneticenhancements:optical_array", 8);
            if (tier >= 3) {
                entry(parts, "cyberneticenhancements:reinforced_alloy", 6);
                entry(parts, "cyberneticenhancements:rare_item_components", 6);
            }
            if (tier >= 4) entry(parts, "cyberneticenhancements:epic_item_components", 4);
            if (tier == 5) entry(parts, "cyberneticenhancements:legendary_item_components", 3);
            futures.add(save(cache, "parts_" + tier, parts));
            var rewards = new JsonArray();
            reference(rewards, "schematics_" + tier, tier == 2 ? 65 : tier == 3 ? 60 : tier == 4 ? 55 : 50);
            reference(rewards, "parts_" + tier, tier == 2 ? 35 : tier == 3 ? 40 : tier == 4 ? 35 : 30);
            if (tier >= 4) {
                var cores = new JsonArray();
                entry(cores, "cyberneticenhancements:proprietary_neural_core", 4);
                entry(cores, "cyberneticenhancements:military_actuator_core", 4);
                if (tier == 5) entry(cores, "cyberneticenhancements:experimental_control_core", 2);
                futures.add(save(cache, "cores_" + tier, cores));
                reference(rewards, "cores_" + tier, tier == 5 ? 20 : 10);
            }
            futures.add(save(cache, "discoveries_" + tier, rewards));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput cache, String name, JsonArray entries) {
        var root = new JsonObject();
        root.addProperty("type", "minecraft:chest");
        var pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.add("entries", entries);
        var pools = new JsonArray();
        pools.add(pool);
        root.add("pools", pools);
        return DataProvider.saveStable(cache, root, output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table")
                .json(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "fabrication/" + name)));
    }

    private static void reference(JsonArray entries, String name, int weight) {
        var entry = new JsonObject();
        entry.addProperty("type", "minecraft:loot_table");
        entry.addProperty("value", "cyberneticenhancements:fabrication/" + name);
        entry.addProperty("weight", weight);
        entries.add(entry);
    }

    private static void entry(JsonArray entries, String name, int weight) {
        JsonObject e = new JsonObject();
        e.addProperty("type", "minecraft:item");
        e.addProperty("name", name);
        e.addProperty("weight", weight);
        entries.add(e);
    }

    public String getName() {
        return "Fabrication discoveries";
    }
}
