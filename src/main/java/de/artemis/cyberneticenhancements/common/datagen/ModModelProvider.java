package de.artemis.cyberneticenhancements.common.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
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

        for (String npcType : List.of("fixer", "ripperdoc", "techie", "netrunner", "merc")) {
            futures.add(saveAliasedItemModel(output, npcType + "_spawn_egg", "minecraft:item/template_spawn_egg"));
        }

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
                Map.entry("courier_package", "minecraft:item/paper")
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

    private CompletableFuture<?> saveCubeBlockModel(CachedOutput output, String modelName, String texturePath) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_all");

        JsonObject textures = new JsonObject();
        textures.addProperty("all", texturePath);
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
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
