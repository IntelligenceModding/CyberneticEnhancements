package de.artemis.cyberneticenhancements.common.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.block.CopperCableBlock;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCatalog;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.ChipwareCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.ChipwareDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareDefinition;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleCatalog;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ModModelProvider implements DataProvider {
    private final PackOutput.PathProvider blockstatesPathProvider;
    private final PackOutput.PathProvider blockModelPathProvider;
    private final PackOutput.PathProvider itemModelPathProvider;

    public ModModelProvider(PackOutput output) {
        this.blockstatesPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.blockModelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/block");
        this.itemModelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(saveHoloBarrierBlockstate(output));
        futures.add(saveAliasedItemModel(output, "holo_barrier", "minecraft:item/barrier"));
        futures.add(saveSimpleBlockstate(output, "fast_travel"));
        futures.add(saveBlockItemDefinition(output, "fast_travel"));
        var travelModel = new JsonObject();
        travelModel.addProperty("parent", "minecraft:block/enchanting_table");
        futures.add(DataProvider.saveStable(output, travelModel, blockModelPathProvider.json(id("fast_travel"))));
        futures.add(saveBarrelBlockstate(output, "iron_quest_crate"));
        futures.add(saveBarrelBlockstate(output, "dropoff_point"));
        futures.add(saveBlockItemDefinition(output, "dropoff_point"));
        for (boolean open : new boolean[]{false, true}) {
            var model = new JsonObject();
            model.addProperty("parent", open ? "minecraft:block/barrel_open" : "minecraft:block/barrel");
            futures.add(DataProvider.saveStable(output, model, blockModelPathProvider.json(id(open ? "dropoff_point_open" : "dropoff_point"))));
        }
        futures.add(saveBlockItemDefinition(output, "iron_quest_crate"));
        for (boolean open : new boolean[]{false, true}) {
            var model = new JsonObject();
            model.addProperty("parent", "minecraft:block/barrel");
            var textures = new JsonObject();
            textures.addProperty("side", "minecraft:block/blast_furnace_side");
            textures.addProperty("bottom", "minecraft:block/blast_furnace_top");
            textures.addProperty("top", open ? "minecraft:block/hopper_top" : "minecraft:block/iron_block");
            model.add("textures", textures);
            futures.add(DataProvider.saveStable(output, model, blockModelPathProvider.json(id(open ? "iron_quest_crate_open" : "iron_quest_crate"))));
        }
        futures.add(saveSimpleBlockstate(output, "bubble_vent"));
        JsonObject ventModel = new JsonObject();
        ventModel.addProperty("parent", "minecraft:block/block");
        JsonObject ventTextures = new JsonObject();
        ventTextures.addProperty("particle", "minecraft:block/water_still");
        ventModel.add("textures", ventTextures);
        futures.add(DataProvider.saveStable(output, ventModel, blockModelPathProvider.json(id("bubble_vent"))));
        saveCopperCable(output, futures);
        savePipes(output, futures);
        saveDucts(output, futures);
        futures.add(saveSimpleBlockstate(output, "crate"));
        futures.add(DataProvider.saveStable(output, crateModel(), blockModelPathProvider.json(id("crate"))));
        futures.add(saveSimpleBlockstate(output, "cyberware_fabricator"));
        JsonObject fabricatorModel = new JsonObject();
        fabricatorModel.addProperty("parent", "minecraft:block/smithing_table");
        futures.add(DataProvider.saveStable(output, fabricatorModel, blockModelPathProvider.json(id("cyberware_fabricator"))));
        futures.add(saveAliasedItemModel(output, "cyberware_fabricator", "minecraft:item/smithing_table"));
        de.artemis.cyberneticenhancements.common.fabrication.FabricationContent.PART_MODELS.forEach((name, parent) -> futures.add(saveAliasedItemModel(output, name, parent)));
        de.artemis.cyberneticenhancements.common.fabrication.FabricationContent.SCHEMATICS.keySet().forEach(name -> futures.add(saveAliasedItemModel(output, name + "_schematic", "minecraft:item/paper")));

        futures.add(saveHorizontalFacingBlockstate(output, "relic_cache"));
        futures.add(saveBlockItemDefinition(output, "relic_cache"));
        futures.add(saveRelicCacheModel(output, "relic_cache", "minecraft:block/polished_blackstone_bricks", "minecraft:block/tinted_glass", "minecraft:block/sea_lantern", "minecraft:block/oxidized_copper"));

        futures.add(saveHorizontalFacingBlockstate(output, "uncommon_relic_cache"));
        futures.add(saveBlockItemDefinition(output, "uncommon_relic_cache"));
        futures.add(saveRelicCacheModel(output, "uncommon_relic_cache", "minecraft:block/polished_blackstone_bricks", "minecraft:block/tinted_glass", "minecraft:block/sea_lantern", "minecraft:block/oxidized_copper"));

        futures.add(saveHorizontalFacingBlockstate(output, "rare_relic_cache"));
        futures.add(saveBlockItemDefinition(output, "rare_relic_cache"));
        futures.add(saveRelicCacheModel(output, "rare_relic_cache", "minecraft:block/deepslate_bricks", "minecraft:block/tinted_glass", "minecraft:block/prismarine_bricks", "minecraft:block/warped_planks"));

        futures.add(saveHorizontalFacingBlockstate(output, "epic_relic_cache"));
        futures.add(saveBlockItemDefinition(output, "epic_relic_cache"));
        futures.add(saveRelicCacheModel(output, "epic_relic_cache", "minecraft:block/crying_obsidian", "minecraft:block/tinted_glass", "minecraft:block/amethyst_block", "minecraft:block/purple_concrete"));

        futures.add(saveHorizontalFacingBlockstate(output, "legendary_relic_cache"));
        futures.add(saveBlockItemDefinition(output, "legendary_relic_cache"));
        futures.add(saveRelicCacheModel(output, "legendary_relic_cache", "minecraft:block/netherite_block", "minecraft:block/tinted_glass", "minecraft:block/gilded_blackstone", "minecraft:block/gold_block"));

        futures.add(saveCubeBlockModel(output, "ripper_station", "minecraft:block/iron_block"));
        futures.add(saveSimpleBlockstate(output, "ripper_station"));
        futures.add(saveBlockItemDefinition(output, "ripper_station"));

        futures.add(saveCubeBlockModel(output, "tech_station", "minecraft:block/gray_concrete"));
        futures.add(saveSimpleBlockstate(output, "tech_station"));
        futures.add(saveBlockItemDefinition(output, "tech_station"));
        futures.add(saveCubeBlockModel(output, "upgrade_station", "minecraft:block/smithing_table_top"));
        futures.add(saveSimpleBlockstate(output, "upgrade_station"));
        futures.add(saveBlockItemDefinition(output, "upgrade_station"));

        futures.add(saveCubeBlockModel(output, "recycler_station", "minecraft:block/deepslate_tiles"));
        futures.add(saveSimpleBlockstate(output, "recycler_station"));
        futures.add(saveBlockItemDefinition(output, "recycler_station"));

        for (String npcType : List.of("fixer", "ripperdoc", "techie", "netrunner", "merc", "med_tech", "maxtec", "dev_test")) {
            futures.add(saveAliasedItemModel(output, npcType + "_spawn_egg", "minecraft:item/template_spawn_egg"));
        }

        Map<String, String> chromiumBlocks = Map.of(
                "chromium_ore", "minecraft:block/iron_ore",
                "deepslate_chromium_ore", "minecraft:block/deepslate_iron_ore");
        futures.add(DataProvider.saveStable(output, chromiumOutlineModel(), blockModelPathProvider.json(id("chromium_outline"))));
        for (var entry : chromiumBlocks.entrySet()) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "cyberneticenhancements:block/chromium_outline");
            JsonObject textures = new JsonObject();
            textures.addProperty("all", entry.getValue());
            model.add("textures", textures);
            futures.add(DataProvider.saveStable(output, model, blockModelPathProvider.json(id(entry.getKey()))));
            futures.add(saveSimpleBlockstate(output, entry.getKey()));
            futures.add(saveBlockItemDefinition(output, entry.getKey()));
        }
        Map<String, String> chromiumItems = Map.of(
                "raw_chromium", "minecraft:item/raw_iron",
                "chromium_scrap", "minecraft:item/iron_nugget",
                "chromium_alloy_ingot", "minecraft:item/iron_ingot",
                "chromium_survey", "minecraft:item/paper");
        for (var entry : chromiumItems.entrySet())
            futures.add(saveAliasedItemModel(output, entry.getKey(), entry.getValue()));
        Map<String, String> materials = Map.ofEntries(
                Map.entry("copper_wiring", "minecraft:item/copper_ingot"),
                Map.entry("conductive_paste", "minecraft:item/redstone"),
                Map.entry("servo_screws", "minecraft:item/iron_nugget"),
                Map.entry("sensor_lens", "minecraft:item/amethyst_shard"),
                Map.entry("micro_battery", "minecraft:item/fire_charge"),
                Map.entry("replacement_joint", "minecraft:item/iron_ingot"),
                Map.entry("basic_circuit_plate", "minecraft:item/comparator"),
                Map.entry("common_item_components", "minecraft:item/iron_nugget"),
                Map.entry("uncommon_item_components", "minecraft:item/gold_nugget"),
                Map.entry("rare_item_components", "minecraft:item/diamond"),
                Map.entry("epic_item_components", "minecraft:item/echo_shard"),
                Map.entry("legendary_item_components", "minecraft:item/nether_star"),
                Map.entry("courier_package", "minecraft:item/paper"),
                Map.entry("revive_injector", "minecraft:item/glistering_melon_slice"),
                Map.entry("signal_beacon", "minecraft:item/ender_eye"),
                Map.entry("building_gadget", "minecraft:item/brush"),
                Map.entry("hanging_wire", "minecraft:item/chain"),
                Map.entry("lost_cargo", "minecraft:item/bundle")
        );
        for (Map.Entry<String, String> material : materials.entrySet()) {
            futures.add(saveAliasedItemModel(output, material.getKey(), material.getValue()));
        }

        for (CyberConsumableDefinition definition : CyberConsumableCatalog.definitions()) {
            futures.add(saveAliasedItemModel(
                    output,
                    definition.id(),
                    modelPath(definition.motifItem())
            ));
        }

        for (ChipwareDefinition definition : ChipwareCatalog.definitions()) {
            futures.add(saveAliasedItemModel(
                    output,
                    definition.id(),
                    modelPath(definition.motifItem().asItem())
            ));
        }

        for (CyberwareModuleDefinition definition : CyberwareModuleCatalog.definitions()) {
            futures.add(saveAliasedItemModel(
                    output,
                    definition.id(),
                    modelPath(definition.motifItem().asItem())
            ));
        }

        for (CyberwareDefinition definition : CyberwareCatalog.definitions()) {
            CyberwareDefinition rootDefinition = resolveTextureRoot(definition);
            futures.add(saveAliasedItemModel(
                    output,
                    definition.id(),
                    modelPath(rootDefinition.motifItem().asItem())
            ));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public @NotNull String getName() {
        return "Model Definitions: " + CyberneticEnhancements.MOD_ID;
    }

    /** The copper cable is built from one middle piece per side and one arm per side and direction, which the client puts together (CopperCableModel). */
    private void saveCopperCable(CachedOutput output, List<CompletableFuture<?>> futures) {
        var directions = net.minecraft.core.Direction.values();
        for (var side : directions) {
            futures.add(DataProvider.saveStable(output, cableModel(CopperCableBlock.coreBox(side)), blockModelPathProvider.json(id(CopperCableBlock.modelName("core", side, null)))));
            for (var toward : directions) {
                if (toward.getAxis() == side.getAxis()) continue;
                futures.add(DataProvider.saveStable(output, cableModel(CopperCableBlock.armBox(side, toward)), blockModelPathProvider.json(id(CopperCableBlock.modelName("arm", side, toward)))));
                futures.add(DataProvider.saveStable(output, cableModel(CopperCableBlock.cornerArmBox(side, toward)), blockModelPathProvider.json(id(CopperCableBlock.modelName("corner_arm", side, toward)))));
            }
        }
        // The state's model is only a stand-in, replaced on the client; it carries the particle and the model transforms.
        JsonObject apply = new JsonObject();
        apply.addProperty("model", modPath("block/" + CopperCableBlock.modelName("core", net.minecraft.core.Direction.DOWN, null)));
        JsonObject part = new JsonObject();
        part.add("apply", apply);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject state = new JsonObject();
        state.add("multipart", parts);
        futures.add(DataProvider.saveStable(output, state, blockstatesPathProvider.json(id("copper_cable"))));
        // In the hand it is a short length of wire with an arm each way.
        JsonObject item = cableModel(CopperCableBlock.coreBox(net.minecraft.core.Direction.DOWN));
        JsonArray elements = item.getAsJsonArray("elements");
        for (var toward : List.of(net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST))
            elements.add(cableElement(CopperCableBlock.armBox(net.minecraft.core.Direction.DOWN, toward)));
        for (JsonElement element : elements) { // Lifted from the floor so it sits in the middle of the item's frame.
            JsonObject box = element.getAsJsonObject();
            for (String key : List.of("from", "to")) box.getAsJsonArray(key).set(1, new com.google.gson.JsonPrimitive(box.getAsJsonArray(key).get(1).getAsFloat() + 7));
        }
        futures.add(DataProvider.saveStable(output, item, itemModelPathProvider.json(id("copper_cable"))));
    }

    private static final String PIPE_BODY = "minecraft:block/cauldron_side", PIPE_FLANGE = "minecraft:block/anvil", PIPE_RED = "minecraft:block/red_concrete";

    /** The water pipe is a middle piece with one arm for each connected side; a fitted shutoff valve adds a red handwheel head on one side. */
    private void savePipes(CachedOutput output, List<CompletableFuture<?>> futures) {
        futures.add(DataProvider.saveStable(output, pipeModel(List.of(pipeBox(5, 5, 5, 11, 11, 11, "#body"))), blockModelPathProvider.json(id("water_pipe_core"))));
        futures.add(DataProvider.saveStable(output, pipeModel(List.of(pipeBox(5, 11, 5, 11, 16, 11, "#body"), pipeBox(4, 15, 4, 12, 16, 12, "#flange"))),
                blockModelPathProvider.json(id("water_pipe_arm"))));
        futures.add(DataProvider.saveStable(output, pipeModel(List.of(pipeBox(5, 5, 5, 11, 11, 11, "#body"), pipeBox(0, 5, 5, 5, 11, 11, "#body"), pipeBox(11, 5, 5, 16, 11, 11, "#body"),
                pipeBox(0, 4, 4, 1, 12, 12, "#flange"), pipeBox(15, 4, 4, 16, 12, 12, "#flange"))), blockModelPathProvider.json(id("water_pipe_item"))));
        // Arms are modelled pointing up and turned onto each side the way pistons are.
        JsonArray parts = new JsonArray();
        parts.add(multipart(null, "water_pipe_core", 0, 0));
        int[][] turns = {{0, 0}, {180, 0}, {90, 0}, {90, 180}, {90, 90}, {90, 270}}; // up, down, north, south, east, west (turned like an end rod)
        String[] names = {"up", "down", "north", "south", "east", "west"};
        for (int i = 0; i < names.length; i++) parts.add(multipart(names[i], "water_pipe_arm", turns[i][0], turns[i][1]));
        for (int i = 0; i < names.length; i++) parts.add(multipart("valve", names[i], "shutoff_valve_head", turns[i][0], turns[i][1]));
        JsonObject pipeState = new JsonObject();
        pipeState.add("multipart", parts);
        futures.add(DataProvider.saveStable(output, pipeState, blockstatesPathProvider.json(id("water_pipe"))));
        futures.add(DataProvider.saveStable(output, itemParent("water_pipe_item"), itemModelPathProvider.json(id("water_pipe"))));

        futures.add(DataProvider.saveStable(output, pipeModel(valveBoxes()), blockModelPathProvider.json(id("shutoff_valve_head"))));
        List<JsonObject> held = new java.util.ArrayList<>(valveBoxes());
        held.add(pipeBox(0, 5, 5, 16, 11, 11, "#body"));
        held.add(pipeBox(0, 4, 4, 1, 12, 12, "#flange"));
        held.add(pipeBox(15, 4, 4, 16, 12, 12, "#flange"));
        futures.add(DataProvider.saveStable(output, pipeModel(held), blockModelPathProvider.json(id("shutoff_valve"))));
        futures.add(DataProvider.saveStable(output, itemParent("shutoff_valve"), itemModelPathProvider.json(id("shutoff_valve"))));
    }

    private static final String DUCT_BODY = "minecraft:block/iron_block", DUCT_BAND = "minecraft:block/anvil";

    /** The duct is a square middle piece with an arm for each joined side, banded where ducts meet; the vent is a flat slatted grille. */
    private void saveDucts(CachedOutput output, List<CompletableFuture<?>> futures) {
        futures.add(DataProvider.saveStable(output, ductModel(List.of(pipeBox(4, 4, 4, 12, 12, 12, "#body"))), blockModelPathProvider.json(id("hvac_duct_core"))));
        futures.add(DataProvider.saveStable(output, ductModel(List.of(pipeBox(4, 12, 4, 12, 16, 12, "#body"), pipeBox(3, 15, 3, 13, 16, 13, "#band"))),
                blockModelPathProvider.json(id("hvac_duct_arm"))));
        futures.add(DataProvider.saveStable(output, ductModel(List.of(pipeBox(4, 4, 4, 12, 12, 12, "#body"), pipeBox(0, 4, 4, 4, 12, 12, "#body"), pipeBox(12, 4, 4, 16, 12, 12, "#body"),
                pipeBox(0, 3, 3, 1, 13, 13, "#band"), pipeBox(15, 3, 3, 16, 13, 13, "#band"))), blockModelPathProvider.json(id("hvac_duct_item"))));
        JsonArray parts = new JsonArray();
        parts.add(multipart(null, "hvac_duct_core", 0, 0));
        int[][] turns = {{0, 0}, {180, 0}, {90, 0}, {90, 180}, {90, 90}, {90, 270}}; // up, down, north, south, east, west
        String[] names = {"up", "down", "north", "south", "east", "west"};
        for (int i = 0; i < names.length; i++) parts.add(multipart(names[i], "hvac_duct_arm", turns[i][0], turns[i][1]));
        JsonObject ductState = new JsonObject();
        ductState.add("multipart", parts);
        futures.add(DataProvider.saveStable(output, ductState, blockstatesPathProvider.json(id("hvac_duct"))));
        futures.add(DataProvider.saveStable(output, itemParent("hvac_duct_item"), itemModelPathProvider.json(id("hvac_duct"))));

        // The vent faces north: its back plate is on the south edge, with a frame round it and slats tipped to shed rain.
        List<JsonObject> boxes = new java.util.ArrayList<>();
        boxes.add(ventBox(0, 0, 14, 16, 2, 16, "#body"));
        boxes.add(ventBox(0, 14, 14, 16, 16, 16, "#body"));
        boxes.add(ventBox(0, 2, 14, 2, 14, 16, "#body"));
        boxes.add(ventBox(14, 2, 14, 16, 14, 16, "#body"));
        boxes.add(ventBox(2, 2, 15, 14, 14, 16, "#band"));
        for (double y : new double[]{3, 5.5, 8, 10.5}) {
            JsonObject slat = ventBox(2, y, 13.5, 14, y + 1.5, 15, "#body");
            JsonObject rotation = new JsonObject();
            rotation.add("origin", vector(8, (float) y + 0.75F, 14.25F));
            rotation.addProperty("axis", "x");
            rotation.addProperty("angle", -22.5);
            slat.add("rotation", rotation);
            boxes.add(slat);
        }
        futures.add(DataProvider.saveStable(output, ductModel(boxes), blockModelPathProvider.json(id("hvac_vent"))));
        JsonObject variants = new JsonObject();
        String[] facings = {"north", "east", "south", "west", "up", "down"};
        int[][] ventTurns = {{0, 0}, {0, 90}, {0, 180}, {0, 270}, {270, 0}, {90, 0}};
        for (int i = 0; i < facings.length; i++) {
            JsonObject variant = new JsonObject();
            variant.addProperty("model", modPath("block/hvac_vent"));
            if (ventTurns[i][0] != 0) variant.addProperty("x", ventTurns[i][0]);
            if (ventTurns[i][1] != 0) variant.addProperty("y", ventTurns[i][1]);
            variants.add("facing=" + facings[i], variant);
        }
        JsonObject ventState = new JsonObject();
        ventState.add("variants", variants);
        futures.add(DataProvider.saveStable(output, ventState, blockstatesPathProvider.json(id("hvac_vent"))));
        futures.add(DataProvider.saveStable(output, itemParent("hvac_vent"), itemModelPathProvider.json(id("hvac_vent"))));
    }

    /** A vent part: its faces show a plain patch of the texture. */
    private static JsonObject ventBox(double x0, double y0, double z0, double x1, double y1, double z1, String texture) {
        return pipeBox(x0, y0, z0, x1, y1, z1, texture);
    }

    private static JsonObject ductModel(List<JsonObject> boxes) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("body", DUCT_BODY);
        textures.addProperty("band", DUCT_BAND);
        textures.addProperty("particle", DUCT_BODY);
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        boxes.forEach(elements::add);
        model.add("elements", elements);
        return model;
    }

    private static JsonObject itemParent(String model) {
        JsonObject item = new JsonObject();
        item.addProperty("parent", modPath("block/" + model));
        return item;
    }

    private static JsonObject multipart(String side, String model, int x, int y) {
        return multipart(side, "true", model, x, y);
    }

    private static JsonObject multipart(String side, String value, String model, int x, int y) {
        JsonObject apply = new JsonObject();
        apply.addProperty("model", modPath("block/" + model));
        if (x != 0) apply.addProperty("x", x);
        if (y != 0) apply.addProperty("y", y);
        JsonObject part = new JsonObject();
        if (side != null) {
            JsonObject when = new JsonObject();
            when.addProperty(side, value);
            part.add("when", when);
        }
        part.add("apply", apply);
        return part;
    }

    /** A box whose faces all show the same plain patch of the texture, so the borders of the cauldron and anvil textures do not run across the pipe. */
    private static JsonObject pipeBox(double x0, double y0, double z0, double x1, double y1, double z1, String texture) {
        JsonObject box = element((float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, texture);
        for (var face : box.getAsJsonObject("faces").entrySet()) face.getValue().getAsJsonObject().add("uv", patch());
        return box;
    }

    private static JsonArray patch() {
        JsonArray array = new JsonArray();
        array.add(5); array.add(5); array.add(11); array.add(11);
        return array;
    }

    private static JsonObject pipeModel(List<JsonObject> boxes) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("body", PIPE_BODY);
        textures.addProperty("flange", PIPE_FLANGE);
        textures.addProperty("red", PIPE_RED);
        textures.addProperty("particle", PIPE_BODY);
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        boxes.forEach(elements::add);
        model.add("elements", elements);
        return model;
    }

    /** The valve standing up: a housing round the middle of the pipe, then a stem and a red handwheel on top. */
    private static List<JsonObject> valveBoxes() {
        List<JsonObject> boxes = new java.util.ArrayList<>();
        boxes.add(pipeBox(4, 4, 4, 12, 12, 12, "#flange"));
        boxes.add(pipeBox(7, 12, 7, 9, 14, 9, "#body"));
        boxes.add(pipeBox(4, 14, 4, 12, 15, 5, "#red"));
        boxes.add(pipeBox(4, 14, 11, 12, 15, 12, "#red"));
        boxes.add(pipeBox(4, 14, 5, 5, 15, 11, "#red"));
        boxes.add(pipeBox(11, 14, 5, 12, 15, 11, "#red"));
        boxes.add(pipeBox(5, 14.25, 7.5, 11, 14.75, 8.5, "#red"));
        boxes.add(pipeBox(7.5, 14.25, 5, 8.5, 14.75, 11, "#red"));
        boxes.add(pipeBox(7, 15, 7, 9, 16, 9, "#red"));
        return boxes;
    }

    private static JsonObject cableModel(double[] box) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("wire", "minecraft:block/copper_block");
        textures.addProperty("particle", "minecraft:block/copper_block");
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        elements.add(cableElement(box));
        model.add("elements", elements);
        return model;
    }

    private static JsonObject cableElement(double[] box) {
        JsonObject element = element((float) box[0], (float) box[1], (float) box[2], (float) box[3], (float) box[4], (float) box[5], "#wire");
        // Left to itself a face takes its texture from where it sits in the block, which for an arm running on past the block's
        // edge is off the texture. So those faces are given the same texture area, moved back onto it.
        boolean outside = false;
        for (double value : box) outside |= value < 0 || value > 16;
        if (!outside) return element;
        double x0 = box[0], y0 = box[1], z0 = box[2], x1 = box[3], y1 = box[4], z1 = box[5];
        JsonObject faces = element.getAsJsonObject("faces");
        setUv(faces, "down", x0, 16 - z1, x1, 16 - z0);
        setUv(faces, "up", x0, z0, x1, z1);
        setUv(faces, "north", 16 - x1, 16 - y1, 16 - x0, 16 - y0);
        setUv(faces, "south", x0, 16 - y1, x1, 16 - y0);
        setUv(faces, "west", z0, 16 - y1, z1, 16 - y0);
        setUv(faces, "east", 16 - z1, 16 - y1, 16 - z0, 16 - y0);
        return element;
    }

    private static void setUv(JsonObject faces, String face, double u0, double v0, double u1, double v1) {
        double du = u0 < 0 ? -u0 : Math.min(0, 16 - u1), dv = v0 < 0 ? -v0 : Math.min(0, 16 - v1);
        if (u1 > 16 && u0 >= 0) du = 16 - u1;
        if (v1 > 16 && v0 >= 0) dv = 16 - v1;
        JsonArray uv = new JsonArray();
        uv.add((float) (u0 + du));
        uv.add((float) (v0 + dv));
        uv.add((float) (u1 + du));
        uv.add((float) (v1 + dv));
        faces.getAsJsonObject(face).add("uv", uv);
    }

    private CompletableFuture<?> saveBarrelBlockstate(CachedOutput output, String blockName) {
        var state = new JsonObject();
        var variants = new JsonObject();
        for (var direction : net.minecraft.core.Direction.values()) {
            for (boolean open : new boolean[]{false, true}) {
                var variant = new JsonObject();
                variant.addProperty("model", id(open ? blockName + "_open" : blockName).withPrefix("block/").toString());
                int rotationX = direction == net.minecraft.core.Direction.UP ? 0 : direction == net.minecraft.core.Direction.DOWN ? 180 : 90;
                if (rotationX != 0) variant.addProperty("x", rotationX);
                if (direction.getAxis().isHorizontal()) variant.addProperty("y", ((int) direction.toYRot() + 180) % 360);
                variants.add("facing=" + direction.getSerializedName() + ",open=" + open, variant);
            }
        }
        state.add("variants", variants);
        return DataProvider.saveStable(output, state, blockstatesPathProvider.json(id(blockName)));
    }

    private CompletableFuture<?> saveCubeBlockModel(CachedOutput output, String modelName, String texturePath) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_all");

        JsonObject textures = new JsonObject();
        textures.addProperty("all", texturePath);
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    /** A slatted wooden box, a little smaller than a full block so stacked crates still read as separate boxes. */
    private static JsonObject crateModel() {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/spruce_planks");
        textures.addProperty("side", "minecraft:block/spruce_planks");
        textures.addProperty("end", "minecraft:block/stripped_spruce_log_top");
        model.add("textures", textures);
        JsonObject element = new JsonObject();
        element.add("from", array(1, 0, 1));
        element.add("to", array(15, 14, 15));
        JsonObject faces = new JsonObject();
        for (String face : List.of("north", "south", "east", "west", "up", "down")) {
            JsonObject json = new JsonObject();
            json.addProperty("texture", face.equals("up") || face.equals("down") ? "#end" : "#side");
            faces.add(face, json);
        }
        element.add("faces", faces);
        JsonArray elements = new JsonArray();
        elements.add(element);
        model.add("elements", elements);
        return model;
    }

    private static JsonArray array(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(value);
        return array;
    }

    /** Five non-overlapping quads per face keep the vanilla texture and a one-texel red rim. */
    private static JsonObject chromiumOutlineModel() {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("border", "minecraft:block/red_concrete");
        textures.addProperty("particle", "#all");
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        int[][] regions = {{1, 1, 15, 15}, {0, 0, 16, 1}, {0, 15, 16, 16}, {0, 1, 1, 15}, {15, 1, 16, 15}};
        for (String direction : List.of("north", "south", "west", "east", "down", "up")) {
            for (int index = 0; index < regions.length; index++) {
                int[] r = regions[index];
                JsonObject part = new JsonObject();
                switch (direction) {
                    case "north", "south" -> {
                        int z = direction.equals("north") ? 0 : 16;
                        part.add("from", vector(r[0], r[1], z));
                        part.add("to", vector(r[2], r[3], z));
                    }
                    case "west", "east" -> {
                        int x = direction.equals("west") ? 0 : 16;
                        part.add("from", vector(x, r[1], r[0]));
                        part.add("to", vector(x, r[3], r[2]));
                    }
                    default -> {
                        int y = direction.equals("down") ? 0 : 16;
                        part.add("from", vector(r[0], y, r[1]));
                        part.add("to", vector(r[2], y, r[3]));
                    }
                }
                JsonObject face = face(index == 0 ? "#all" : "#border");
                face.addProperty("cullface", direction);
                if (index == 0) {
                    JsonArray uv = new JsonArray();
                    for (int coordinate : r) uv.add(coordinate);
                    face.add("uv", uv);
                }
                JsonObject faces = new JsonObject(); faces.add(direction, face);
                part.add("faces", faces); elements.add(part);
            }
        }
        model.add("elements", elements);
        return model;
    }

    private CompletableFuture<?> saveRelicCacheModel(CachedOutput output, String modelName, String frameTexture, String panelTexture, String accentTexture, String trimTexture) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");

        JsonObject textures = new JsonObject();
        textures.addProperty("frame", frameTexture);
        textures.addProperty("panel", panelTexture);
        textures.addProperty("accent", accentTexture);
        textures.addProperty("trim", trimTexture);
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(element(1, 1, 1, 15, 15, 15, "#frame"));
        elements.add(element(1, 0, 1, 15, 1, 15, "#trim"));
        elements.add(element(1, 15, 1, 15, 16, 15, "#trim"));
        elements.add(element(4, 5, 0.5F, 12, 11, 1, "#panel"));
        elements.add(element(7, 7, 0, 9, 9, 0.5F, "#accent"));
        json.add("elements", elements);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    /** The barrier draws itself, so every facing uses the empty model. */
    private CompletableFuture<?> saveHoloBarrierBlockstate(CachedOutput output) {
        JsonObject json = new JsonObject();
        JsonObject variants = new JsonObject();
        for (String facing : new String[]{"east", "north", "south", "west"}) {
            JsonObject variant = new JsonObject();
            variant.addProperty("model", "minecraft:block/air");
            variants.add("facing=" + facing, variant);
        }
        json.add("variants", variants);
        return DataProvider.saveStable(output, json, blockstatesPathProvider.json(id("holo_barrier")));
    }

    private CompletableFuture<?> saveSimpleBlockstate(CachedOutput output, String blockName) {
        JsonObject json = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modPath("block/" + blockName));
        variants.add("", variant);
        json.add("variants", variants);
        return DataProvider.saveStable(output, json, blockstatesPathProvider.json(id(blockName)));
    }

    private CompletableFuture<?> saveHorizontalFacingBlockstate(CachedOutput output, String blockName) {
        JsonObject json = new JsonObject();
        JsonObject variants = new JsonObject();
        variants.add("facing=north", rotationVariant(blockName, 0));
        variants.add("facing=south", rotationVariant(blockName, 180));
        variants.add("facing=west", rotationVariant(blockName, 270));
        variants.add("facing=east", rotationVariant(blockName, 90));
        json.add("variants", variants);
        return DataProvider.saveStable(output, json, blockstatesPathProvider.json(id(blockName)));
    }

    private CompletableFuture<?> saveBlockItemDefinition(CachedOutput output, String itemName) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", modPath("block/" + itemName));
        return DataProvider.saveStable(output, json, itemModelPathProvider.json(id(itemName)));
    }

    private CompletableFuture<?> saveAliasedItemModel(CachedOutput output, String itemName, String parent) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", visibleItemModelParent(parent));
        return DataProvider.saveStable(output, json, itemModelPathProvider.json(id(itemName)));
    }

    private static JsonObject rotationVariant(String blockName, int rotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modPath("block/" + blockName));
        if (rotation != 0) {
            variant.addProperty("y", rotation);
        }
        return variant;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, path);
    }

    private static CyberwareDefinition resolveTextureRoot(CyberwareDefinition definition) {
        CyberwareDefinition current = definition;
        while (current.upgradeFromId() != null) {
            current = CyberwareCatalog.get(current.upgradeFromId());
        }
        return current;
    }

    static String visibleItemModelParent(String parent) {

        return switch (parent) {
            case "minecraft:item/shield" -> "minecraft:item/iron_chestplate";
            case "minecraft:item/chest", "minecraft:item/ender_chest" -> "minecraft:item/barrel";
            case "minecraft:item/player_head" -> "minecraft:item/carved_pumpkin";
            default -> parent;
        };
    }

    private static String modelPath(net.minecraft.world.item.Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key.getNamespace() + ":item/" + key.getPath();
    }

    private static String modPath(String path) {
        return CyberneticEnhancements.MOD_ID + ":" + path;
    }

    private static JsonObject element(float fromX, float fromY, float fromZ, float toX, float toY, float toZ, String texture) {
        JsonObject element = new JsonObject();
        element.add("from", vector(fromX, fromY, fromZ));
        element.add("to", vector(toX, toY, toZ));
        element.add("faces", cubeFaces(texture));
        return element;
    }

    private static JsonObject cubeFaces(String texture) {
        JsonObject faces = new JsonObject();
        faces.add("down", face(texture));
        faces.add("up", face(texture));
        faces.add("north", face(texture));
        faces.add("south", face(texture));
        faces.add("west", face(texture));
        faces.add("east", face(texture));
        return faces;
    }

    private static JsonObject face(String texture) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        return face;
    }

    private static JsonArray vector(float x, float y, float z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
