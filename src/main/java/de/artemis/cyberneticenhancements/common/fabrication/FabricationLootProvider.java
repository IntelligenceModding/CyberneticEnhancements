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
            JsonObject root = new JsonObject();
            root.addProperty("type", "minecraft:chest");
            JsonArray entries = new JsonArray();
            for (String id : FabricationContent.SCHEMATICS.keySet()) {
                var d = CyberwareCatalog.get(id);
                if (d.tier().ordinal() + 1 <= tier && d.acquisitionMethod() != CyberwareAcquisitionMethod.QUEST)
                    entry(entries, "cyberneticenhancements:" + id + "_schematic", d.tier().ordinal() + 1 == tier ? 3 : 1);
            }
            entry(entries, "cyberneticenhancements:neural_processor", 8);
            entry(entries, "cyberneticenhancements:high_torque_servo", 8);
            entry(entries, "cyberneticenhancements:optical_array", 8);
            if (tier >= 4) {
                entry(entries, "cyberneticenhancements:proprietary_neural_core", tier == 5 ? 4 : 1);
                entry(entries, "cyberneticenhancements:military_actuator_core", tier == 5 ? 4 : 1);
            }
            if (tier == 5) entry(entries, "cyberneticenhancements:experimental_control_core", 1);
            JsonObject pool = new JsonObject();
            pool.addProperty("rolls", 1);
            pool.add("entries", entries);
            JsonArray pools = new JsonArray();
            pools.add(pool);
            root.add("pools", pools);
            futures.add(DataProvider.saveStable(cache, root, output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table").json(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "fabrication/discoveries_" + tier))));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
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
