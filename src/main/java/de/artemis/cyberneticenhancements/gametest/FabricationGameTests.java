package de.artemis.cyberneticenhancements.gametest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.fabrication.*;
import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

import java.util.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FabricationGameTests {
    private static FabricatorBlockEntity station(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), ModBlocks.CYBERWARE_FABRICATOR.get());
        return (FabricatorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 1, 2)));
    }

    private static RecipeHolder<FabricationRecipe> recipe(GameTestHelper h, String id) {
        return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.FABRICATION.get()).stream().filter(r -> r.id().getPath().equals("fabrication/" + id)).findFirst().orElseThrow();
    }

    private static void supply(FabricatorBlockEntity station, RecipeHolder<FabricationRecipe> recipe) {
        station.selected = recipe.id().toString();
        for (var p : recipe.value().parts())
            station.setItem(p.slot(), p.ingredient().getItems()[0].copyWithCount(p.count()));
    }

    private static ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "fabrication-test");
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return player;
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void completionConsumesPartsOnceAndCancellationKeepsThem(GameTestHelper h) {
        var station = station(h);
        var recipe = recipe(h, "basic_kiroshi_optics");
        supply(station, recipe);
        var player = player(h);
        h.assertTrue(station.start(player), "Known basic design did not start");
        for (int i = 0; i < 10; i++)
            FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        station.cancel();
        h.assertTrue(recipe.value().matches(station.input(), h.getLevel()), "Cancel consumed reserved inputs");
        h.assertTrue(station.getItem(26).isEmpty(), "Cancel created output");
        h.assertTrue(station.start(player), "Restart failed");
        for (int i = 0; i < recipe.value().ticks() + 2; i++)
            FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        h.assertTrue(station.getItem(26).is(ModItems.cyberware("basic_kiroshi_optics").get()) && station.getItem(26).getCount() == 1, "Completion did not create exactly one implant");
        for (int i = 0; i < 25; i++) h.assertTrue(station.getItem(i).isEmpty(), "Completion left consumed inputs");
        h.assertTrue(!station.start(player), "Missing materials allowed second output");
        player.server.getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void schematicAndMachineRequirementsAreServerEnforced(GameTestHelper h) {
        var station = station(h);
        var recipe = recipe(h, "militech_apogee");
        supply(station, recipe);
        var player = player(h);
        h.assertTrue(!station.start(player), "Restricted design bypassed schematic and machine checks");
        station.capability = 3;
        station.selected = "";
        h.assertTrue(!station.start(player) && station.selected.isEmpty(), "Automatic recipe matching exposed an unlearned design");
        station.selected = recipe.id().toString();
        SchematicKnowledge.receive(List.of("militech_apogee"));
        h.assertTrue(recipe.value().clientUnlocked(), "Synchronized knowledge did not reveal the recipe to client viewers");
        h.assertTrue(!recipe(h, "militech_falcon").value().clientUnlocked(), "Learning one design exposed another gated recipe");
        h.assertTrue(!station.start(player), "Client display knowledge granted server fabrication access");
        SchematicKnowledge.clearClient();
        h.assertTrue(!recipe.value().clientUnlocked() && recipe(h, "basic_kiroshi_optics").value().clientUnlocked(),
                "Clearing client knowledge leaked gated recipes or hid public recipes");
        station.capability = 1;
        station.setItem(25, new ItemStack(FabricationContent.SCHEMATICS.get("militech_apogee").get()));
        h.assertTrue(!station.start(player) && !SchematicKnowledge.knows(player, "militech_apogee"), "Fabrication learned a schematic from the removed reader");
        h.assertTrue(!station.canPlaceItem(25, station.getItem(25)), "Automation can still insert into the removed reader");
        var menu = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
        int recipeButton = 100 + menu.recipes().indexOf(recipe);
        h.assertTrue(menu.availableRecipes().stream().noneMatch(r -> r.id().equals(recipe.id())) && menu.selected() == null,
                "Unlearned recipe is visible in the Fabricator menu");
        h.assertTrue(!menu.clickMenuButton(player, recipeButton), "Forged selection accepted a locked design");
        h.assertTrue(menu.slots.size() == 62 && !menu.clickMenuButton(player, 3), "Removed reader slot or learning action is still available");
        int missingSlot = recipe.value().parts().getFirst().slot();
        var missingParts = station.removeItemNoUpdate(missingSlot);
        player.getInventory().setItem(9, missingParts);
        h.assertTrue(!menu.clickMenuButton(player, 0), "Fill request accepted an unlearned design");
        h.assertTrue(menu.quickMoveStack(player, 26).isEmpty(), "Shift-click exposed locked recipe slot mapping");
        h.assertTrue(station.getItem(missingSlot).isEmpty() && !player.getInventory().getItem(9).isEmpty(), "Fill grid revealed an unlearned design's ingredients");
        FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        h.assertTrue(station.getItem(25).isEmpty(), "Legacy reader item was trapped");
        var schematic = new ItemStack(FabricationContent.SCHEMATICS.get("militech_apogee").get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, schematic);
        schematic.getItem().use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(SchematicKnowledge.knows(player, "militech_apogee"), "Schematic was not learned");
        h.assertTrue(menu.availableRecipes().stream().anyMatch(r -> r.id().equals(recipe.id())) && menu.clickMenuButton(player, recipeButton),
                "Learning did not immediately reveal and enable selecting the design");
        h.assertTrue(schematic.getCount() == 1, "Learning consumed reusable schematic");
        menu.clickMenuButton(player, 0);
        h.assertTrue(recipe.value().matches(station.input(), h.getLevel()), "Learning did not unlock filling the recipe");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(!station.start(player), "Learned design bypassed machine tier");
        h.assertTrue(!station.upgrade(new ItemStack(FabricationContent.part("high_spec_fabrication_module"))), "Machine skipped precision tier");
        station.upgrade(new ItemStack(FabricationContent.part("precision_assembly_module")));
        station.upgrade(new ItemStack(FabricationContent.part("high_spec_fabrication_module")));
        h.assertTrue(station.start(player), "Learned schematic required carrying its item or upgraded machine failed");
        h.assertTrue(!station.canTakeItem(new net.minecraft.world.SimpleContainer(1), 12, station.getItem(12)), "Automation can remove locked inputs");
        station.cancel();
        player.server.getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void everyImplantUsesFabricationAndRecipesSurviveNetworkRoundTrip(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        for (var d : de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog.definitions()) {
            var expected = ModItems.cyberware(d.id()).get();
            var salvage = de.artemis.cyberneticenhancements.common.cyberware.CyberwareRecycleHelper.resolve(h.getLevel(), new ItemStack(expected)).outputs().getFirst();
            h.assertTrue(!salvage.isEmpty(), "Implant has no valid recovered part: " + d.id());
            var worn = new ItemStack(expected);
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(worn, d, 1);
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(worn).isAvailable(), "Missing function-specific repair material: " + d.id());
            recipe(h, d.id());
            h.assertTrue(recipes.getAllRecipesFor(RecipeType.CRAFTING).stream().noneMatch(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(expected)), "Obsolete crafting-table implant recipe: " + d.id());
        }
        var original = recipe(h, "militech_apogee").value();
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        var codec = new FabricationRecipe.Serializer().streamCodec();
        codec.encode(buffer, original);
        var decoded = codec.decode(buffer);
        buffer.release();
        h.assertTrue(decoded.parts().size() == original.parts().size() && decoded.schematic().equals(original.schematic()) && decoded.tier() == 5, "Fabrication recipe lost progression or grid data on sync");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void savedJobsResumeAndCountedIngredientsCannotBeBypassed(GameTestHelper h) {
        var counted = new FabricationRecipe(List.of(new FabricationRecipe.Part(12, Ingredient.of(Items.IRON_INGOT), 4)),
                new ItemStack(FabricationContent.part("alloy_plate")), 1, 100, "", "Components");
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(25, ItemStack.EMPTY));
        grid.set(12, new ItemStack(Items.IRON_INGOT, 3));
        h.assertTrue(!counted.matches(new FabricationRecipe.Input(grid), h.getLevel()), "Partial stack met a counted requirement");
        grid.get(12).grow(1);
        h.assertTrue(counted.matches(new FabricationRecipe.Input(grid), h.getLevel()), "Exact counted stack was rejected");
        grid.set(0, new ItemStack(Items.DIRT));
        h.assertTrue(!counted.matches(new FabricationRecipe.Input(grid), h.getLevel()), "Extraneous items bypassed fixed layout");

        var station = station(h);
        var recipe = recipe(h, "basic_kiroshi_optics");
        supply(station, recipe);
        station.capability = 2;
        var player = player(h);
        h.assertTrue(station.start(player), "Persistent job failed to start");
        for (int i = 0; i < 10; i++)
            FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = (FabricatorBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(station.getBlockPos(), station.getBlockState(), saved, h.getLevel().registryAccess());
        h.assertTrue(restored != null && restored.capability == 2 && restored.progress == 10 && restored.working(), "Save lost job, upgrades or progress");
        restored.setLevel(h.getLevel());
        h.getLevel().setBlockEntity(restored);
        for (int i = 0; i < recipe.value().ticks(); i++)
            FabricatorBlockEntity.tick(h.getLevel(), restored.getBlockPos(), restored.getBlockState(), restored);
        h.assertTrue(restored.getItem(26).is(ModItems.cyberware("basic_kiroshi_optics").get()) && restored.getItem(26).getCount() == 1, "Restored job did not complete exactly once");
        player.server.getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void firstDiscoveryIsPermanentAndSurvivesPlayerClone(GameTestHelper h) {
        var station = station(h);
        var player = player(h);
        var schematic = new ItemStack(FabricationContent.SCHEMATICS.get("militech_apogee").get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, schematic);
        schematic.getItem().use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        var original = SchematicKnowledge.history(player, "militech_apogee");
        h.assertTrue(original.epochMillis() > 0 && original.position().equals(player.blockPosition()), "Discovery did not capture time and player location");
        h.assertTrue(original.source().equals("Studied schematic"), "Discovery method was lost");
        schematic.getItem().use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(original.equals(SchematicKnowledge.history(player, "militech_apogee")), "Repeated item use overwrote discovery history");
        h.assertTrue(!SchematicKnowledge.learn(player, "militech_apogee", "Later attempt", BlockPos.ZERO), "Repeated learning overwrote original discovery");
        h.assertTrue(original.equals(SchematicKnowledge.history(player, "militech_apogee")), "Discovery history changed after repeat unlock");
        SchematicKnowledge.receiveDetails(List.of(new SchematicKnowledge.Unlock("militech_apogee", 1, 1, "fake", BlockPos.ZERO, "Forged client display")));
        h.assertTrue(original.equals(SchematicKnowledge.history(player, "militech_apogee")), "Client display changed server history");
        SchematicKnowledge.clearClient();
        var clone = player(h);
        SchematicKnowledge.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone, player, true));
        h.assertTrue(SchematicKnowledge.knows(clone, "militech_apogee") && original.equals(SchematicKnowledge.history(clone, "militech_apogee")), "Respawn lost schematic or original discovery history");
        player.server.getPlayerList().remove(player);
        clone.server.getPlayerList().remove(clone);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void recyclingCommitsWholeRecoveryOnceAndReturnsUncollectedParts(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 1, 2));
        h.setBlock(new BlockPos(2, 1, 2), ModBlocks.RECYCLER_STATION.get());
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu(1, player.getInventory(), pos);
        menu.slots.get(0).set(new ItemStack(ModItems.cyberware("memory_boost").get()));
        var expected = menu.getRecyclePlan().outputs();
        h.assertTrue(expected.size() == 2, "Fixture must exercise multiple recovered stacks");
        h.assertTrue(menu.slots.get(1).getItem().isEmpty() && menu.getInputStack().getCount() == 1, "Preview materialized output or consumed an input");
        h.assertTrue(menu.clickMenuButton(player, 0) && menu.getInputStack().isEmpty(), "Recycle did not consume exactly one input");
        h.assertTrue(!menu.clickMenuButton(player, 0), "Recycling overwrote pending recovery");
        menu.slots.get(0).set(new ItemStack(ModItems.cyberware("memory_boost").get()));
        menu.quickMoveStack(player, 1);
        h.assertTrue(menu.getInputStack().getCount() == 1, "Collecting output consumed another input");
        var pending = menu.slots.get(2).getItem().copy();
        menu.slots.get(0).set(ItemStack.EMPTY);
        h.assertTrue(ItemStack.matches(pending, menu.slots.get(2).getItem()), "Changing input erased committed recovery");
        menu.removed(player);
        h.assertTrue(player.getInventory().items.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, pending) && stack.getCount() >= pending.getCount()), "Closing discarded uncollected recovery");
        player.server.getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void dedicatedServicesPreserveConditionUpgradesAndCustomData(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var upgrade = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(relative));
        var input = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
        input.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Personal optics"));
        var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) input.getItem()).getDefinition();
        de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(input, definition, 50);
        int integrity = de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(input, definition);
        upgrade.slots.get(0).set(input);
        h.assertTrue(upgrade.isUpgrading() && upgrade.materialCount() == 2, "Upgrade machine exposed repair bay");
        var cost = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRequiredMaterials(upgrade.plan());
        for (int i = 0; i < cost.size(); i++)
            player.getInventory().setItem(i, cost.get(i).stack().copyWithCount(cost.get(i).count()));
        upgrade.clickMenuButton(player, 1);
        h.assertTrue(upgrade.canService() && upgrade.getOutputStack().isEmpty(), "Auto-fill failed or preview created an item");
        h.assertTrue(upgrade.clickMenuButton(player, 0) && !upgrade.clickMenuButton(player, 0), "Upgrade failed or replay duplicated output");
        var upgraded = upgrade.getOutputStack().copy();
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(upgraded) == 1, "Upgrade level did not advance");
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(upgraded, definition) == integrity, "Upgrade granted a free repair");
        h.assertTrue(net.minecraft.network.chat.Component.literal("Personal optics").equals(upgraded.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "Upgrade discarded custom name");
        h.assertTrue(java.util.stream.IntStream.range(0, upgrade.materialCount()).allMatch(i -> upgrade.getMaterialStack(i).isEmpty()), "Upgrade did not consume exact material costs");
        upgrade.slots.get(upgrade.materialCount() + 1).remove(1);
        upgrade.removed(player);
        h.setBlock(relative, ModBlocks.TECH_STATION.get());
        var repair = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(2, player.getInventory(), h.absolutePos(relative));
        repair.slots.get(0).set(upgraded);
        h.assertTrue(!repair.isUpgrading() && repair.materialCount() == 2, "Repair machine exposed upgrade bay");
        var repairCost = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRequiredMaterials(repair.plan());
        for (int i = 0; i < repairCost.size(); i++)
            repair.slots.get(i + 1).set(repairCost.get(i).stack().copyWithCount(repairCost.get(i).count()));
        h.assertTrue(repair.clickMenuButton(player, 0), "Repair failed with correct materials");
        var repaired = repair.getOutputStack();
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrityRatio(repaired, definition) == 1.0, "Repair did not restore integrity");
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(repaired) == 1 && net.minecraft.network.chat.Component.literal("Personal optics").equals(repaired.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "Repair lost upgrade or custom name");
        repair.removed(player);
        player.server.getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void workshopTierChecksAndSplitMaterialStacksAreServerEnforced(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(relative));
        menu.slots.get(0).set(new ItemStack(ModItems.cyberware("militech_apogee").get()));
        var costs = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRequiredMaterials(menu.plan());
        for (int i = 0; i < costs.size(); i++)
            menu.slots.get(i + 1).set(costs.get(i).stack().copyWithCount(costs.get(i).count()));
        h.assertTrue(!menu.clickMenuButton(player, 0) && menu.getOutputStack().isEmpty(), "Injected high-tier input bypassed server capacity check");
        menu.slots.get(0).set(ItemStack.EMPTY);
        var component = menu.capacityCost();
        player.getInventory().setItem(0, component.copyWithCount(2));
        h.assertTrue(menu.clickMenuButton(player, 3) && menu.tier().ordinal() == 1 && player.getInventory().getItem(0).getCount() == 1, "Capacity upgrade did not consume exactly one component");
        var plan = new de.artemis.cyberneticenhancements.common.cyberware.CyberwareServicePlan(
                new ItemStack(Items.DIAMOND), List.of(
                new de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.MaterialRequirement(new ItemStack(Items.IRON_INGOT), 5),
                new de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.MaterialRequirement(new ItemStack(Items.IRON_INGOT), 2)));
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.matchesMaterials(plan, new ItemStack(Items.IRON_INGOT, 3), new ItemStack(Items.IRON_INGOT, 4)), "Split material stacks were rejected");
        h.assertTrue(!de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.matchesMaterials(plan, new ItemStack(Items.IRON_INGOT, 6)), "One stack was counted twice for repeated material requirements");
        menu.removed(player);
        player.server.getPlayerList().remove(player);
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void cacheTierBufferAndTimerAreEnforcedOnServer(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.LEGENDARY_RELIC_CACHE.get());
        var pos = h.absolutePos(relative);
        var cache = (de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = player(h);
        var menu = new de.artemis.cyberneticenhancements.common.menu.RelicCacheHackMenu(1, player.getInventory(), pos);
        player.containerMenu = menu;
        h.assertTrue(cache.getHackDifficulty() == 5 && cache.getHackBufferSize() == 9 && cache.getHackTimeLimitTicks() == 500, "Legendary cache did not use its difficulty profile");
        for (int i = 0; i < 50; i++) de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity.tick(h.getLevel(), pos, cache.getBlockState(), cache);
        h.assertTrue(cache.getHackTimeRemainingTicks() == 500, "Planning consumed execution time");
        var saved = cache.saveWithFullMetadata(h.getLevel().registryAccess());
        saved.putIntArray("HackGridTokens", new int[36]);
        int[] impossibleTargets = new int[15];
        java.util.Arrays.fill(impossibleTargets, 1);
        saved.putIntArray("HackTargetTokens", impossibleTargets);
        var restored = (de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, cache.getBlockState(), saved, h.getLevel().registryAccess());
        restored.setLevel(h.getLevel());
        h.getLevel().setBlockEntity(restored);
        restored.trySelectHackCell(player, 0);
        de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity.tick(h.getLevel(), pos, restored.getBlockState(), restored);
        h.assertTrue(restored.getHackTimeRemainingTicks() == 499, "First pick did not start execution time");
        for (int cell : new int[]{6,7,13,14,20,21,27}) restored.trySelectHackCell(player, cell);
        h.assertTrue(restored.hasActiveHackSession() && restored.getHackSelectedCount() == 8, "Session ended before buffer was exhausted");
        restored.trySelectHackCell(player, 28);
        h.assertTrue(!restored.hasActiveHackSession() && !restored.isAnimating(), "Full buffer allowed further picks or awarded failed breach loot");
        player.server.getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 100)
    public static void salvageIsDeterministicConditionSensitiveAndLosesManufacturingValue(GameTestHelper h) {
        var cache = new HashMap<Item, Double>();
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.RECYCLING.get())) {
            var recipe = holder.value();
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            var serializer = new de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe.Serializer();
            serializer.streamCodec().encode(buffer, recipe);
            var decoded = serializer.streamCodec().decode(buffer);
            buffer.release();
            for (var sample : recipe.getInputIngredient().getItems()) {
                double manufacture = workshopMaterialValue(h, sample.getItem(), cache, new HashSet<>());
                for (int wear : new int[]{0, 30, 80}) {
                    ItemStack input = sample.copyWithCount(1);
                    if (input.getItem() instanceof de.artemis.cyberneticenhancements.common.item.CyberwareItem item) {
                        int max = de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getMaxIntegrity(item.getDefinition());
                        de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(input, item.getDefinition(), max * wear / 100);
                    }
                    var outputs = recipe.recover(input);
                    var roundTrip = decoded.recover(input);
                    h.assertTrue(outputs.size() <= 3 && outputs.size() == roundTrip.size(), "Invalid salvage batch: " + holder.id());
                    double recovered = 0;
                    for (int i = 0; i < outputs.size(); i++) {
                        h.assertTrue(ItemStack.matches(outputs.get(i), roundTrip.get(i)), "Network preview differs from server recovery: " + holder.id());
                        h.assertTrue(ItemStack.matches(outputs.get(i), recipe.recover(input).get(i)), "Random salvage: " + holder.id());
                        recovered += workshopMaterialValue(h, outputs.get(i).getItem(), cache, new HashSet<>()) * outputs.get(i).getCount();
                    }
                    h.assertTrue(recovered < manufacture, "Salvage returns manufacturing value: " + holder.id() + " recovery=" + recovered + ", manufacture=" + manufacture);
                }
            }
        }
        h.succeed();
    }

    private static double workshopMaterialValue(GameTestHelper h, Item item, Map<Item, Double> cache, Set<Item> visiting) {
        if (cache.containsKey(item)) return cache.get(item);
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (!id.getNamespace().equals(CyberneticEnhancements.MOD_ID))
            return id.getPath().endsWith("_nugget") ? 1.0 / 9 : 1;
        if (!visiting.add(item)) return Double.POSITIVE_INFINITY;
        double minimum = Double.POSITIVE_INFINITY;
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.FABRICATION.get())) {
            var recipe = holder.value();
            if (!recipe.result().is(item)) continue;
            double cost = 0;
            for (var part : recipe.parts())
                cost += Arrays.stream(part.ingredient().getItems()).mapToDouble(s -> workshopMaterialValue(h, s.getItem(), cache, visiting)).min().orElse(Double.POSITIVE_INFINITY) * part.count();
            minimum = Math.min(minimum, cost / recipe.result().getCount());
        }
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            var recipe = holder.value();
            var result = recipe.getResultItem(h.getLevel().registryAccess());
            if (!result.is(item)) continue;
            double cost = 0;
            for (var ingredient : recipe.getIngredients())
                if (!ingredient.isEmpty())
                    cost += Arrays.stream(ingredient.getItems()).mapToDouble(s -> workshopMaterialValue(h, s.getItem(), cache, visiting)).min().orElse(Double.POSITIVE_INFINITY);
            minimum = Math.min(minimum, cost / result.getCount());
        }
        visiting.remove(item);
        double value = Double.isFinite(minimum) ? minimum : 100;
        cache.put(item, value);
        return value;
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void repairPricesScaleAndSpecializedUpgradesChangeGameplay(GameTestHelper h) {
        for (var definition : de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog.definitions()) {
            var light = new ItemStack(ModItems.cyberware(definition.id()).get());
            var heavy = light.copy();
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(light, definition, 1);
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(heavy, definition, 10000);
            var minor = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(light);
            var major = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(heavy);
            h.assertTrue(minor.materials().size() == 2 && major.materials().size() == 2, "Service must use two supplies: " + definition.id());
            for (int i = 0; i < 2; i++)
                h.assertTrue(minor.materials().get(i).count() == 1 && minor.materials().get(i).count() <= major.materials().get(i).count(), "Minor damage has excessive or inverted repair cost: " + definition.id());
        }
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var inventory = new de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory(player);
        var pump = new ItemStack(ModItems.cyberware("blood_pump").get());
        var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) pump.getItem()).getDefinition();
        pump = de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.createUpgradedCopy(pump, definition);
        inventory.setStackInSlot(de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot.CIRCULATORY_SYSTEM_1.ordinal(), pump);
        player.setHealth(1);
        de.artemis.cyberneticenhancements.common.cyberware.CyberwareAbilities.applyTriggeredAbilities(player, inventory);
        h.assertTrue(Math.abs(player.getHealth() - 7.6) < 0.01, "Blood Pump upgrade did not increase its actual healing");
        player.server.getPlayerList().remove(player);
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void completedRecoverySurvivesFullInventoryAndCannotBeClaimedTwice(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.RECYCLER_STATION.get());
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var pos = h.absolutePos(relative);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        var menu = new de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu(1, player.getInventory(), pos);
        var input = new ItemStack(ModItems.consumable("maxdoc_mk1").get());
        menu.slots.get(0).set(input.copyWithCount(2));
        var expected = menu.getRecyclePlan().outputs();
        h.assertTrue(menu.clickMenuButton(player, 0) && menu.getInputStack().getCount() == 1, "Recycle did not process exactly one stacked input");
        h.assertTrue(!menu.clickMenuButton(player, 0), "Repeated action overwrote the completed batch");
        h.assertTrue(menu.quickMoveStack(player, 1).isEmpty(), "Full inventory accepted output or erased it");
        var bounds = player.getBoundingBox().inflate(4);
        int before = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
        menu.removed(player);
        int after = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
        h.assertTrue(after - before == 1 + expected.stream().mapToInt(ItemStack::getCount).sum(), "Closing with a full inventory lost or duplicated unconsumed input/recovery");
        h.assertTrue(menu.getInputStack().isEmpty() && !menu.hasPendingResultStacks(), "Returned items remain claimable");
        menu.removed(player);
        int repeated = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
        h.assertTrue(repeated == after, "Repeated close duplicated returned items");
        player.server.getPlayerList().remove(player);
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void changedSchematicAuthorizationCancelsSavedJobsWithoutConsumingParts(GameTestHelper h) {
        var station = station(h);
        var recipe = recipe(h, "militech_apogee");
        station.capability = 3;
        supply(station, recipe);
        var player = player(h);
        SchematicKnowledge.learn(player, "militech_apogee");
        h.assertTrue(station.start(player), "Learned job could not start");
        var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.getString("authorized_schematic").equals("militech_apogee"), "Job authorization was not saved");
        station.loadWithComponents(saved, h.getLevel().registryAccess());
        FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        h.assertTrue(station.working() && station.progress == 1, "Saved authorized job did not resume");
        saved.putString("authorized_schematic", "different_design");
        station.loadWithComponents(saved, h.getLevel().registryAccess());
        FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        h.assertTrue(!station.working() && recipe.value().matches(station.input(), h.getLevel()) && station.getItem(26).isEmpty(),
                "Changed schematic requirement completed an unauthorized job or destroyed reserved inputs");
        player.server.getPlayerList().remove(player);
        h.succeed();
    }
    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 100)
    public static void wikiRecipeDiagramsMatchLiveServerRecipes(GameTestHelper h) {
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.FABRICATION.get())) {
            var grid = de.artemis.cyberneticenhancements.client.screen.WikiRecipeLayouts.grid(holder.value());
            h.assertTrue(holder.value().matches(new FabricationRecipe.Input(Arrays.asList(grid)), h.getLevel()),
                    "Wiki's 5x5 example does not match the server recipe: " + holder.id());
        }
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(holder.value() instanceof ShapedRecipe || holder.value() instanceof ShapelessRecipe)) continue;
            if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(holder.value().getResultItem(h.getLevel().registryAccess()).getItem())
                    .getNamespace().equals(CyberneticEnhancements.MOD_ID)) continue;
            var grid = de.artemis.cyberneticenhancements.client.screen.WikiRecipeLayouts.grid(holder.value());
            h.assertTrue(holder.value().matches(CraftingInput.of(3, 3, Arrays.asList(grid)), h.getLevel()),
                    "Wiki's crafting-table example does not match the server recipe: " + holder.id());
        }
        var custom = new FabricationRecipe(List.of(new FabricationRecipe.Part(12, Ingredient.of(Items.IRON_INGOT), 4)),
                new ItemStack(Items.DIAMOND, 3), 1, 20, "", "Datapack example");
        h.assertTrue(custom.matches(new FabricationRecipe.Input(Arrays.asList(
                de.artemis.cyberneticenhancements.client.screen.WikiRecipeLayouts.grid(custom))), h.getLevel()),
                "Wiki lost counted ingredients from a custom datapack recipe");
        for (var type : de.artemis.cyberneticenhancements.common.entity.NpcType.values()) {
            for (int option = de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_BUY_MAXDOC_MK2;
                    option <= de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_BUY_IMMUNOBLOCKERS; option++) {
                var stock = de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.shopItem(type, option);
                h.assertTrue(!stock.isEmpty(), "NPC stock reference resolves to an empty item: " + type);
            }
        }
        h.succeed();
    }}
