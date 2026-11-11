package de.artemis.cyberneticenhancements.common.datagen;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableCatalog;
import de.artemis.cyberneticenhancements.common.consumable.CyberConsumableDefinition;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        buildMaterialRecipes(recipeOutput);
        buildConsumableRecipes(recipeOutput);


        de.artemis.cyberneticenhancements.common.fabrication.FabricationRecipes.generate(recipeOutput);
        buildRecyclerRecipes(recipeOutput);
        buildStationRecipes(recipeOutput);
    }

    private void buildMaterialRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COPPER_WIRING.get(), 4)
                .pattern(" C ")
                .pattern("CRC")
                .pattern(" C ")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONDUCTIVE_PASTE.get(), 2)
                .pattern(" H ")
                .pattern("SRS")
                .pattern("   ")
                .define('H', Items.HONEY_BOTTLE)
                .define('S', Items.SLIME_BALL)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_slime_ball", has(Items.SLIME_BALL))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SERVO_SCREWS.get(), 4)
                .pattern(" N ")
                .pattern("NIN")
                .pattern(" N ")
                .define('N', Items.IRON_NUGGET)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SENSOR_LENS.get())
                .pattern(" A ")
                .pattern("GRG")
                .pattern(" G ")
                .define('A', Items.AMETHYST_SHARD)
                .define('G', Items.GLASS_PANE)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICRO_BATTERY.get())
                .pattern(" C ")
                .pattern("RIR")
                .pattern(" G ")
                .define('C', ModItems.COPPER_WIRING.get())
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GOLD_NUGGET)
                .unlockedBy("has_copper_wiring", has(ModItems.COPPER_WIRING.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REPLACEMENT_JOINT.get())
                .pattern(" S ")
                .pattern("WIW")
                .pattern(" P ")
                .define('S', ModItems.SERVO_SCREWS.get())
                .define('W', ModItems.COPPER_WIRING.get())
                .define('I', Items.IRON_INGOT)
                .define('P', ModItems.CONDUCTIVE_PASTE.get())
                .unlockedBy("has_servo_screws", has(ModItems.SERVO_SCREWS.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BASIC_CIRCUIT_PLATE.get())
                .pattern("WRW")
                .pattern("GPG")
                .pattern(" W ")
                .define('W', ModItems.COPPER_WIRING.get())
                .define('R', Items.REDSTONE)
                .define('G', Items.GOLD_INGOT)
                .define('P', ModItems.CONDUCTIVE_PASTE.get())
                .unlockedBy("has_copper_wiring", has(ModItems.COPPER_WIRING.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMMON_ITEM_COMPONENTS.get(), 2)
                .pattern(" W ")
                .pattern("CJC")
                .pattern(" P ")
                .define('W', ModItems.COPPER_WIRING.get())
                .define('C', ModItems.CONDUCTIVE_PASTE.get())
                .define('J', ModItems.REPLACEMENT_JOINT.get())
                .define('P', ModItems.BASIC_CIRCUIT_PLATE.get())
                .unlockedBy("has_basic_circuit_plate", has(ModItems.BASIC_CIRCUIT_PLATE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.UNCOMMON_ITEM_COMPONENTS.get(), 2)
                .pattern(" G ")
                .pattern("CBC")
                .pattern(" M ")
                .define('G', Items.GOLD_INGOT)
                .define('C', ModItems.COMMON_ITEM_COMPONENTS.get())
                .define('B', ModItems.MICRO_BATTERY.get())
                .define('M', ModItems.BASIC_CIRCUIT_PLATE.get())
                .unlockedBy("has_common_item_components", has(ModItems.COMMON_ITEM_COMPONENTS.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RARE_ITEM_COMPONENTS.get())
                .pattern(" D ")
                .pattern("UBU")
                .pattern(" C ")
                .define('D', Items.DIAMOND)
                .define('U', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                .define('B', ModItems.MICRO_BATTERY.get())
                .define('C', ModItems.BASIC_CIRCUIT_PLATE.get())
                .unlockedBy("has_uncommon_item_components", has(ModItems.UNCOMMON_ITEM_COMPONENTS.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EPIC_ITEM_COMPONENTS.get())
                .pattern(" E ")
                .pattern("RLR")
                .pattern(" C ")
                .define('E', Items.ECHO_SHARD)
                .define('R', ModItems.RARE_ITEM_COMPONENTS.get())
                .define('L', ModItems.SENSOR_LENS.get())
                .define('C', ModItems.BASIC_CIRCUIT_PLATE.get())
                .unlockedBy("has_rare_item_components", has(ModItems.RARE_ITEM_COMPONENTS.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LEGENDARY_ITEM_COMPONENTS.get())
                .pattern(" N ")
                .pattern("EBE")
                .pattern(" L ")
                .define('N', Items.NETHER_STAR)
                .define('E', ModItems.EPIC_ITEM_COMPONENTS.get())
                .define('B', ModItems.MICRO_BATTERY.get())
                .define('L', ModItems.SENSOR_LENS.get())
                .unlockedBy("has_epic_item_components", has(ModItems.EPIC_ITEM_COMPONENTS.get()))
                .save(recipeOutput);
    }

    private void buildConsumableRecipes(RecipeOutput recipeOutput) {
        for (CyberConsumableDefinition definition : CyberConsumableCatalog.definitions()) {
            buildConsumableRecipe(recipeOutput, definition);
        }
    }

    private void buildConsumableRecipe(RecipeOutput recipeOutput, CyberConsumableDefinition definition) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.consumable(definition.id()).get());

        switch (definition.id()) {
            case "maxdoc_mk1" -> builder.define('G', Items.GLASS_BOTTLE).define('R', Items.REDSTONE).define('P', ModItems.CONDUCTIVE_PASTE.get())
                    .pattern(" G ").pattern(" R ").pattern(" P ");
            case "maxdoc_mk2" -> builder.define('G', Items.GLASS_BOTTLE).define('A', Items.GOLDEN_APPLE).define('P', ModItems.COMMON_ITEM_COMPONENTS.get())
                    .pattern(" G ").pattern(" A ").pattern(" P ");
            case "maxdoc_mk3" -> builder.define('G', Items.GLASS_BOTTLE).define('H', Items.GHAST_TEAR).define('C', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                    .pattern(" G ").pattern(" H ").pattern(" C ");
            case "bounce_back_mk1" -> builder.define('G', Items.GLASS_BOTTLE).define('S', Items.SUGAR).define('P', ModItems.CONDUCTIVE_PASTE.get())
                    .pattern(" G ").pattern(" S ").pattern(" P ");
            case "bounce_back_mk2" -> builder.define('G', Items.GLASS_BOTTLE).define('R', Items.REDSTONE).define('H', Items.GHAST_TEAR).define('C', ModItems.COMMON_ITEM_COMPONENTS.get())
                    .pattern(" G ").pattern(" R ").pattern("HC ");
            case "bounce_back_mk3" -> builder.define('G', Items.GLASS_BOTTLE).define('A', Items.GOLDEN_APPLE).define('C', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                    .pattern(" G ").pattern(" A ").pattern(" C ");
            case "health_booster" -> builder.define('A', Items.GOLDEN_APPLE).define('G', Items.GLASS_BOTTLE).define('P', ModItems.COMMON_ITEM_COMPONENTS.get())
                    .pattern(" A ").pattern(" G ").pattern(" P ");
            case "stamina_booster" -> builder.define('S', Items.SUGAR).define('G', Items.GLASS_BOTTLE).define('R', ModItems.COMMON_ITEM_COMPONENTS.get())
                    .pattern(" S ").pattern(" G ").pattern(" R ");
            case "oxy_booster" -> builder.define('H', Items.GHAST_TEAR).define('G', Items.GLASS_BOTTLE).define('S', ModItems.COMMON_ITEM_COMPONENTS.get())
                    .pattern(" H ").pattern(" G ").pattern(" S ");
            case "capacity_booster" -> builder.define('A', Items.GOLDEN_APPLE).define('M', ModItems.MICRO_BATTERY.get()).define('S', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                    .pattern(" A ").pattern(" M ").pattern(" S ");
            case "ram_jolt" -> builder.define('R', Items.REDSTONE).define('M', ModItems.MICRO_BATTERY.get()).define('C', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                    .pattern(" R ").pattern(" M ").pattern(" C ");
            case "immunoblockers" -> builder.define('F', Items.FERMENTED_SPIDER_EYE).define('H', Items.GHAST_TEAR).define('C', ModItems.RARE_ITEM_COMPONENTS.get())
                    .pattern(" F ").pattern(" H ").pattern(" C ");
            case "chrome_suppressant" -> builder.define('H', Items.GHAST_TEAR).define('P', ModItems.CONDUCTIVE_PASTE.get()).define('C', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                    .pattern(" H ").pattern(" P ").pattern(" C ");
            case "black_lace" -> builder.define('B', Items.BLAZE_POWDER).define('S', Items.SUGAR).define('F', Items.FERMENTED_SPIDER_EYE).define('C', ModItems.RARE_ITEM_COMPONENTS.get())
                    .pattern(" B ").pattern(" S ").pattern("FC ");
            case "asskick" -> builder.define('A', Items.GOLDEN_APPLE).define('S', Items.SUGAR).define('F', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                    .pattern(" A ").pattern(" S ").pattern(" F ");
            case "jellytricity" -> builder.define('S', Items.SUGAR).define('R', Items.REDSTONE).define('F', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                    .pattern(" S ").pattern(" R ").pattern(" F ");
            default -> builder.define('G', Items.GLASS_BOTTLE).define('P', ModItems.CONDUCTIVE_PASTE.get()).define('R', Items.REDSTONE)
                    .pattern(" G ").pattern(" P ").pattern(" R ");
        }

        builder.unlockedBy("has_conductive_paste", has(ModItems.CONDUCTIVE_PASTE.get()))
                .save(recipeOutput);
    }

    private void buildStationRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.RIPPER_STATION.get())
                .pattern("ISI")
                .pattern("CBC")
                .pattern("IPI")
                .define('I', Items.IRON_INGOT)
                .define('S', ModItems.SENSOR_LENS.get())
                .define('C', ModItems.BASIC_CIRCUIT_PLATE.get())
                .define('B', Items.SMOOTH_STONE)
                .define('P', ModItems.CONDUCTIVE_PASTE.get())
                .unlockedBy("has_basic_circuit_plate", has(ModItems.BASIC_CIRCUIT_PLATE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.TECH_STATION.get())
                .pattern("ICI")
                .pattern("RBR")
                .pattern("EPE")
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.BASIC_CIRCUIT_PLATE.get())
                .define('R', ModItems.REPLACEMENT_JOINT.get())
                .define('B', Items.SMOOTH_STONE)
                .define('E', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                .define('P', ModItems.CONDUCTIVE_PASTE.get())
                .unlockedBy("has_uncommon_item_components", has(ModItems.UNCOMMON_ITEM_COMPONENTS.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.UPGRADE_STATION.get())
                .pattern("ICI")
                .pattern("RSR")
                .pattern("EPE")
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.BASIC_CIRCUIT_PLATE.get())
                .define('R', Items.REDSTONE)
                .define('S', Items.SMITHING_TABLE)
                .define('E', ModItems.UNCOMMON_ITEM_COMPONENTS.get())
                .define('P', ModItems.CONDUCTIVE_PASTE.get())
                .unlockedBy("has_uncommon_item_components", has(ModItems.UNCOMMON_ITEM_COMPONENTS.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.RECYCLER_STATION.get())
                .pattern("ISI")
                .pattern("CBC")
                .pattern("RPR")
                .define('I', Items.IRON_INGOT)
                .define('S', ModItems.SERVO_SCREWS.get())
                .define('C', ModItems.BASIC_CIRCUIT_PLATE.get())
                .define('B', Items.SMOOTH_STONE)
                .define('R', ModItems.REPLACEMENT_JOINT.get())
                .define('P', ModItems.CONDUCTIVE_PASTE.get())
                .unlockedBy("has_basic_circuit_plate", has(ModItems.BASIC_CIRCUIT_PLATE.get()))
                .save(recipeOutput);
    }

    private void buildRecyclerRecipes(RecipeOutput recipeOutput) {
        WorkshopDefaults.recycling(recipeOutput);
    }
}
