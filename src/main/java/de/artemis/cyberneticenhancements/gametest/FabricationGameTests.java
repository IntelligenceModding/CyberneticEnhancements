package de.artemis.cyberneticenhancements.gametest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.fabrication.*;
import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.TechStationBlockEntity;
import de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

import java.util.*;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FabricationGameTests {
    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void normalWearIsExactlyQuarteredAndKeepsFractionalWearThroughSave(GameTestHelper h) {
        var stack = new ItemStack(ModItems.cyberware("subdermal_armor").get());
        var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) stack.getItem()).getDefinition();
        int max = de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getMaxIntegrity(definition);
        for (int i = 0; i < 3; i++) de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.wear(stack, definition, 1);
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(stack, definition) == max, "One-point wear rounded up instead of accumulating");
        stack = ItemStack.parseOptional(h.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) stack.save(h.getLevel().registryAccess()));
        de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.wear(stack, definition, 1);
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(stack, definition) == max - 1, "Save lost fractional wear");
        de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.wear(stack, definition, 16);
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(stack, definition) == max - 5, "Large activation wear was not quartered");
        de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(stack, definition, 20);
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(stack, definition) == max - 25, "Explicit severe damage was accidentally quartered");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 40)
    public static void neuralFaultLocksEffectsAndOnlyCompletedRepairClearsIt(GameTestHelper h) {
        var player = player(h);
        try {
            var inventory = new de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory(player);
            h.assertTrue(inventory.getChromeCapacity() == 100, "Default Chrome capacity is not 100");
            var stack = new ItemStack(ModItems.cyberware("subdermal_armor").get());
            var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) stack.getItem()).getDefinition();
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Personal armor"));
            inventory.setStackInSlot(de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot.INTEGUMENTARY_SYSTEM_1.ordinal(), stack);
            inventory.save();
            int cost = inventory.getInstalledChromeCost();
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffects.refreshPlayerCyberware(player)
                    .getOrDefault(de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType.ARMOR, 0D) > 0, "Fixture has no passive armor");
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.applyNeuralFault(stack, definition, 40);
            inventory.save();
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(stack, definition) == 60
                    && inventory.getInstalledChromeCost() == cost && !inventory.hasInstalledCyberware(definition.id())
                    && inventory.getInstalledCyberwareBySlotType(definition.slotType()) == null, "Fault did not disable mechanical lookup or changed Chrome cost");
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffects.refreshPlayerCyberware(player)
                    .getOrDefault(de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType.ARMOR, 0D) == 0, "Faulted implant still grants armor");
            var upgraded = de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.createUpgradedCopy(stack, definition);
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(upgraded), "Upgrade cleared a fault");
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.fullyRepair(stack, definition);
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(stack)
                    && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getPerformanceRatio(stack, definition) == 0, "Restoring integrity bypassed the lockout");
            var pos = new BlockPos(2, 1, 2);
            h.setBlock(pos, ModBlocks.TECH_STATION.get());
            var station = (TechStationBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            station.setItem(0, stack.copy());
            var plan = station.plan();
            h.assertTrue(plan.isAvailable() && !de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(plan.output())
                    && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(station.getItem(0)), "Fault repair unavailable at full integrity or preview mutated input");
            for (int i = 0; i < plan.materials().size(); i++) station.setItem(i + 1, plan.materials().get(i).stack().copyWithCount(plan.materials().get(i).count()));
            h.assertTrue(station.start(player), "Fault repair did not start");
            station.stop();
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(station.getItem(0)), "Cancelled repair cleared the fault");
            h.assertTrue(station.start(player), "Fault repair did not restart");
            station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
            TechStationBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(station.getItem(0))
                    && station.getItem(0).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME), "Completed repair failed to unlock or lost custom data");
            inventory.setStackInSlot(de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot.INTEGUMENTARY_SYSTEM_1.ordinal(), station.getItem(0).copy()); inventory.save();
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffects.refreshPlayerCyberware(player)
                    .getOrDefault(de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType.ARMOR, 0D) > 0, "Repaired implant did not resume working");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 40)
    public static void psychosisFaultRollsAreSelectiveSevereAndPersistent(GameTestHelper h) {
        var player = player(h);
        try {
            int events = 0, skipped = 0;
            for (int seed = 0; seed < 100; seed++) {
                var inventory = new de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory(player);
                for (int i = 0; i < 3; i++) inventory.setStackInSlot(i, new ItemStack(ModItems.cyberware("memory_boost").get()));
                inventory.save();
                var affected = de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionManager.inducePsychosisFaults(player, net.minecraft.util.RandomSource.create(seed * 912931L + 17));
                if (affected.isEmpty()) skipped++; else events++;
                h.assertTrue(affected.size() <= 2, "Episode faulted all three implants");
                var restored = new de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory(player);
                int faults = 0;
                for (int i = 0; i < 3; i++) {
                    var saved = restored.getStackInSlot(i);
                    if (de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(saved)) {
                        faults++;
                        var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) saved.getItem()).getDefinition();
                        int loss = de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getMaxIntegrity(definition)
                                - de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(saved, definition);
                        h.assertTrue(loss >= 35 && loss <= 60, "Fault damage was not 35-60% of maximum integrity");
                    }
                }
                h.assertTrue(faults == affected.size(), "Fault count changed after inventory save");
            }
            h.assertTrue(events > 0 && skipped > 0, "Fault chance was unconditional or never happened");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void ripperSlotUpgradesRequireFourComponentsAtomicallyAcrossStacks(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.RIPPER_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new de.artemis.cyberneticenhancements.common.menu.RipperStationMenu(1, player.getInventory(), h.absolutePos(pos));
            var slot = de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot.FACE_1;
            for (int step = 0; step < 4; step++) {
                player.getInventory().clearContent();
                var before = menu.getSupportedTier(slot);
                var required = menu.getRequiredUpgradeComponentStack(slot);
                h.assertTrue(required.getCount() == 4, "Slot cost is not four components");
                player.getInventory().setItem(9, required.copyWithCount(2));
                player.getInventory().setItem(10, required.copyWithCount(1));
                h.assertTrue(!menu.hasRequiredUpgradeComponent(slot) && !menu.tryUpgradeSupportedTier(slot)
                        && menu.getSupportedTier(slot) == before && player.getInventory().countItem(required.getItem()) == 3,
                        "Insufficient split stacks were consumed or upgraded the slot");
                player.getInventory().setItem(11, required.copyWithCount(2));
                h.assertTrue(menu.hasRequiredUpgradeComponent(slot) && menu.tryUpgradeSupportedTier(slot)
                        && menu.getSupportedTier(slot) == before.next() && player.getInventory().countItem(required.getItem()) == 1,
                        "Successful slot upgrade did not consume exactly four split components");
            }
            h.assertTrue(!menu.tryUpgradeSupportedTier(slot), "Maximum-tier slot upgraded again");
            player.getInventory().clearContent();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var other = de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot.ARMS_1;
            h.assertTrue(menu.hasRequiredUpgradeComponent(other) && menu.tryUpgradeSupportedTier(other)
                    && player.getInventory().isEmpty(), "Creative upgrades stopped being free");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 40)
    public static void upgradeJeiTransfersEveryStepAndPreservesImplantData(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.UPGRADE_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(pos));
            player.containerMenu = menu;
            int tested = 0;
            for (var entry : ModItems.cyberwareItems()) {
                var item = entry.get();
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                int max = de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getMaxUpgrades(item.getDefinition());
                for (int level = 0; level < max; level++) {
                    menu.station.clearContent(); player.getInventory().clearContent();
                    var input = de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransfer.displayInput(id, level);
                    input.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Do not replace my implant"));
                    de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(input, item.getDefinition(), 7);
                    var plan = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getUpgradePlan(input);
                    player.getInventory().setItem(9, input.copy());
                    for (var cost : plan.materials()) player.getInventory().add(cost.stack().copyWithCount(cost.count()));
                    var preview = de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransfer.prepare(menu, player, id, level, false);
                    h.assertTrue(preview.error() == de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransfer.Error.NONE
                            && menu.getInputStack().isEmpty() && ItemStack.matches(input, player.getInventory().getItem(9)), "JEI preview changed live contents: " + id);
                    h.assertTrue(menu.transferUpgrade(player, id, level, false), "JEI transfer failed at " + id + " level " + level);
                    h.assertTrue(ItemStack.matches(menu.getInputStack(), input) && menu.canService() && !menu.isWorking()
                            && player.getInventory().isEmpty(), "Transfer lost integrity/name, used wrong costs, or started automatically: " + id);
                    h.assertTrue(menu.transferUpgrade(player, id, level, false) && ItemStack.matches(input, menu.getInputStack()), "Repeating transfer changed the implant");
                    tested++;
                }
            }
            h.assertTrue(tested > 100, "Upgrade coverage unexpectedly small");
            h.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 40)
    public static void upgradeJeiTransferIsAtomicAndValidatesStationLevelAndStock(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.UPGRADE_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(pos));
            player.containerMenu = menu;
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.cyberware("memory_boost").get());
            var input = de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransfer.displayInput(id, 0);
            var plan = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getUpgradePlan(input);
            player.getInventory().clearContent();
            h.assertTrue(!menu.transferUpgrade(player, id, 0, false), "Missing implant was accepted");
            player.getInventory().setItem(9, input.copy());
            h.assertTrue(!menu.transferUpgrade(player, id, 0, false) && menu.getInputStack().isEmpty()
                    && ItemStack.matches(input, player.getInventory().getItem(9)), "Missing materials partly moved input");
            for (var cost : plan.materials()) player.getInventory().add(cost.stack().copyWithCount(cost.count() * 3));
            h.assertTrue(!menu.transferUpgrade(player, id, -1, false) && !menu.transferUpgrade(player, id, 999, false), "Invalid levels accepted");
            h.assertTrue(!menu.transferUpgrade(player, net.minecraft.resources.ResourceLocation.withDefaultNamespace("dirt"), 0, false), "Non-implant accepted");
            menu.setCarried(new ItemStack(Items.DIAMOND));
            h.assertTrue(!menu.transferUpgrade(player, id, 0, false), "Occupied cursor was ignored");
            menu.setCarried(ItemStack.EMPTY);
            menu.station.setItem(0, new ItemStack(Items.STONE));
            h.assertTrue(menu.transferUpgrade(player, id, 0, true) && player.getInventory().countItem(Items.STONE) == 1, "Maximum transfer failed or discarded previous input");
            for (int i = 0; i < plan.materials().size(); i++)
                h.assertTrue(menu.getMaterialStack(i).getCount() == plan.materials().get(i).count() * 3, "Maximum transfer did not fill available materials");
            h.assertTrue(menu.clickMenuButton(player, 0) && !menu.transferUpgrade(player, id, 0, false), "Running upgrade accepted a transfer");
            menu.station.stop(); menu.station.clearContent(); player.getInventory().clearContent();
            player.getInventory().setItem(0, input.copy());
            for (int i = 1; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            for (int i = 0; i < plan.materials().size(); i++) player.getInventory().setItem(i + 1, plan.materials().get(i).stack().copyWithCount(64));
            menu.station.setItem(0, new ItemStack(Items.STONE)); menu.station.setItem(1, new ItemStack(Items.COBBLESTONE));
            h.assertTrue(!menu.transferUpgrade(player, id, 0, false) && menu.getInputStack().is(Items.STONE)
                    && menu.getMaterialStack(0).is(Items.COBBLESTONE) && ItemStack.matches(input, player.getInventory().getItem(0)), "Inventory overflow changed live contents");
            menu.station.clearContent(); player.getInventory().clearContent();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            h.assertTrue(menu.transferUpgrade(player, id, 0, false) && menu.canService() && player.getInventory().isEmpty(), "Creative transfer did not fill freely");
            player.containerMenu = player.inventoryMenu;
            h.assertTrue(!menu.transferUpgrade(player, id, 0, false), "Closed menu accepted a stale request");
            var repairPos = new BlockPos(4, 1, 2);
            h.setBlock(repairPos, ModBlocks.TECH_STATION.get());
            var repair = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(2, player.getInventory(), h.absolutePos(repairPos));
            player.containerMenu = repair;
            h.assertTrue(!repair.transferUpgrade(player, id, 0, false), "Repair station accepted an upgrade transfer");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            var payload = new de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransferPayload(menu.containerId, id, 2, true);
            de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransferPayload.STREAM_CODEC.encode(buffer, payload);
            h.assertTrue(payload.equals(de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransferPayload.STREAM_CODEC.decode(buffer)), "Upgrade packet lost item, level, or menu identity");
            buffer.release();
            h.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 40)
    public static void repairJeiTransfersEveryImplantAndKindAndPreservesImplantData(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.TECH_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(pos));
            player.containerMenu = menu;
            var kinds = de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransfer.Kind.values();
            int tested = 0;
            for (var entry : ModItems.cyberwareItems()) {
                var item = entry.get();
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                var definition = item.getDefinition();
                for (var kind : kinds) {
                    if (de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransfer.displayInput(id, kind).isEmpty()) continue;
                    menu.station.clearContent(); player.getInventory().clearContent();
                    // A named, worn implant whose exact integrity is not the recipe's: any copy that needs this repair must work.
                    var input = new ItemStack(item);
                    input.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Do not replace my implant"));
                    int repairStep = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.repairAmount(definition);
                    if (kind == de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransfer.Kind.FAULT)
                        de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.applyNeuralFault(input, definition, 41);
                    else de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(input, definition, repairStep + 2);
                    var plan = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(input);
                    player.getInventory().setItem(9, input.copy());
                    for (var cost : plan.materials()) player.getInventory().add(cost.stack().copyWithCount(cost.count()));
                    var preview = de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransfer.prepare(menu, player, id, kind, false);
                    h.assertTrue(preview.error() == de.artemis.cyberneticenhancements.common.menu.UpgradeRecipeTransfer.Error.NONE
                            && menu.getInputStack().isEmpty() && ItemStack.matches(input, player.getInventory().getItem(9)), "JEI preview changed live contents: " + id + " " + kind);
                    h.assertTrue(menu.transferRepair(player, id, kind, false), "JEI repair transfer failed at " + id + " " + kind);
                    h.assertTrue(ItemStack.matches(menu.getInputStack(), input) && menu.canService() && !menu.isWorking()
                            && player.getInventory().isEmpty(), "Transfer lost integrity/name, used wrong costs, or started automatically: " + id + " " + kind);
                    h.assertTrue(menu.transferRepair(player, id, kind, false) && ItemStack.matches(input, menu.getInputStack()), "Repeating transfer changed the implant");
                    tested++;
                }
            }
            h.assertTrue(tested > 100, "Repair coverage unexpectedly small: " + tested);
            h.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 40)
    public static void repairJeiTransferValidatesStationKindAndStock(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.TECH_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(pos));
            player.containerMenu = menu;
            var integrity = de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransfer.Kind.INTEGRITY;
            var fault = de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransfer.Kind.FAULT;
            var item = ModItems.cyberware("memory_boost").get();
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            var definition = item.getDefinition();
            // A tier-1 implant at 73 integrity needs a repair cycle; a faulted one needs its fault cleared first.
            var worn = new ItemStack(item);
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(worn, definition, 27);
            var faulted = new ItemStack(item);
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.applyNeuralFault(faulted, definition, 40);
            var plan = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(worn);
            h.assertTrue(plan.isAvailable() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(worn, definition) == 73,
                    "The 73-integrity implant is not repairable");
            var faultPlan = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(faulted);
            h.assertTrue(faultPlan.isAvailable() && !de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(faultPlan.output())
                    && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(faultPlan.output(), definition)
                    == de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(faulted, definition), "A fault cycle must only clear the fault");
            player.getInventory().clearContent();
            h.assertTrue(!menu.transferRepair(player, id, integrity, false), "Missing implant was accepted");
            player.getInventory().setItem(9, worn.copy());
            h.assertTrue(!menu.transferRepair(player, id, integrity, false) && menu.getInputStack().isEmpty()
                    && ItemStack.matches(worn, player.getInventory().getItem(9)), "Missing materials partly moved input");
            for (var cost : plan.materials()) player.getInventory().add(cost.stack().copyWithCount(cost.count() * 3));
            h.assertTrue(!menu.transferRepair(player, id, fault, false), "A merely worn implant was accepted for the fault recipe");
            player.getInventory().setItem(10, faulted.copy());
            h.assertTrue(menu.transferRepair(player, id, integrity, false) && ItemStack.matches(worn, menu.getInputStack()), "The worn implant was not chosen for the integrity recipe");
            menu.station.clearContent();
            h.assertTrue(menu.transferRepair(player, id, fault, false) && ItemStack.matches(faulted, menu.getInputStack()), "The faulted implant was not chosen for the fault recipe");
            h.assertTrue(!menu.transferRepair(player, net.minecraft.resources.ResourceLocation.withDefaultNamespace("dirt"), integrity, false), "Non-implant accepted");
            menu.setCarried(new ItemStack(Items.DIAMOND));
            h.assertTrue(!menu.transferRepair(player, id, integrity, false), "Occupied cursor was ignored");
            menu.setCarried(ItemStack.EMPTY);
            menu.station.clearContent(); player.getInventory().clearContent();
            player.getInventory().setItem(9, new ItemStack(item));
            for (var cost : plan.materials()) player.getInventory().add(cost.stack().copyWithCount(cost.count()));
            h.assertTrue(!menu.transferRepair(player, id, integrity, false), "An undamaged implant was accepted for repair");
            player.getInventory().setItem(9, worn.copy());
            h.assertTrue(menu.transferRepair(player, id, integrity, false) && menu.clickMenuButton(player, 0) && !menu.transferRepair(player, id, integrity, false),
                    "Running repair accepted a transfer");
            menu.station.stop(); menu.station.clearContent(); player.getInventory().clearContent();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            h.assertTrue(menu.transferRepair(player, id, integrity, false) && menu.canService() && player.getInventory().isEmpty(), "Creative transfer did not fill freely");
            player.containerMenu = player.inventoryMenu;
            h.assertTrue(!menu.transferRepair(player, id, integrity, false), "Closed menu accepted a stale request");
            var upgradePos = new BlockPos(4, 1, 2);
            h.setBlock(upgradePos, ModBlocks.UPGRADE_STATION.get());
            var upgrade = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(2, player.getInventory(), h.absolutePos(upgradePos));
            player.containerMenu = upgrade;
            h.assertTrue(!upgrade.transferRepair(player, id, integrity, false), "Upgrade station accepted a repair transfer");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            var payload = new de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransferPayload(menu.containerId, id, fault, true);
            de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransferPayload.STREAM_CODEC.encode(buffer, payload);
            h.assertTrue(payload.equals(de.artemis.cyberneticenhancements.common.menu.RepairRecipeTransferPayload.STREAM_CODEC.decode(buffer)), "Repair packet lost item, kind, or menu identity");
            buffer.release();
            h.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void recyclerJeiTransferPreservesDataAndMovesOnlyInput(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.RECYCLER_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            var menu = new RecyclerStationMenu(1, player.getInventory(), h.absolutePos(pos));
            player.containerMenu = menu;
            var item = new ItemStack(ModItems.consumable("maxdoc_mk1").get(), 7);
            item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keep me"));
            var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.RECYCLING.get()).stream()
                    .filter(r -> r.value().getInputIngredient().test(item)).findFirst().orElseThrow();
            player.getInventory().setItem(9, item.copy());
            menu.station.setItem(0, new ItemStack(Items.DIRT, 2));
            menu.station.setItem(1, new ItemStack(Items.DIAMOND, 3));
            var plan = de.artemis.cyberneticenhancements.common.menu.RecyclerRecipeTransfer.prepare(menu, player, recipe.id(), true);
            h.assertTrue(plan.error() == de.artemis.cyberneticenhancements.common.menu.RecyclerRecipeTransfer.Error.NONE
                    && menu.getInputStack().is(Items.DIRT) && player.getInventory().getItem(9).getCount() == 7, "JEI preview mutated live contents");
            h.assertTrue(menu.transferRecipe(player, recipe.id(), true), "Maximum Recycler JEI transfer failed");
            h.assertTrue(ItemStack.matches(menu.getInputStack(), item) && player.getInventory().countItem(Items.DIRT) == 2
                    && menu.station.getItem(1).getCount() == 3 && !menu.isWorking(), "Transfer lost data, touched recovery, or started recycling");
            h.assertTrue(menu.transferRecipe(player, recipe.id(), false) && ItemStack.matches(menu.getInputStack(), item), "Repeated transfer reduced or duplicated existing input");
            menu.station.clearContent(); player.getInventory().clearContent();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            h.assertTrue(menu.transferRecipe(player, recipe.id(), false) && menu.getInputStack().getCount() == 1 && player.getInventory().isEmpty(), "Creative JEI did not provide input freely");
            h.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void recyclerJeiTransferRejectsUnsafeRequestsAtomically(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.RECYCLER_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            var menu = new RecyclerStationMenu(1, player.getInventory(), h.absolutePos(pos));
            player.containerMenu = menu;
            var item = new ItemStack(ModItems.consumable("maxdoc_mk1").get(), 2);
            var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.RECYCLING.get()).stream()
                    .filter(r -> r.value().getInputIngredient().test(item)).findFirst().orElseThrow();
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), false), "Missing input accepted");
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            player.getInventory().setItem(9, item.copy());
            menu.station.setItem(0, new ItemStack(Items.STONE));
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), false) && menu.getInputStack().is(Items.STONE)
                    && player.getInventory().getItem(9).getCount() == 2, "Full inventory rejection partially moved input");
            menu.station.setItem(0, ItemStack.EMPTY);
            menu.setCarried(new ItemStack(Items.DIAMOND));
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), false), "Occupied cursor accepted");
            menu.setCarried(ItemStack.EMPTY);
            h.assertTrue(!menu.transferRecipe(player, net.minecraft.resources.ResourceLocation.withDefaultNamespace("stone"), false), "Invalid recipe accepted");
            h.assertTrue(menu.transferRecipe(player, recipe.id(), false) && menu.station.start(player), "Could not start recycling fixture");
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), true), "Busy station accepted transfer");
            menu.station.stop(); player.containerMenu = player.inventoryMenu;
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), false), "Closed menu accepted stale request");
            h.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void fabricatorFillReadinessRequiresEveryMissingIngredientAndKeepsCreativeFree(GameTestHelper h) {
        var station = station(h);
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            var holder = recipe(h, "basic_kiroshi_optics");
            h.assertTrue(station.selectDesign(player, holder.id()), "Could not select fill readiness fixture");
            var menu = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
            var parts = holder.value().parts();
            h.assertTrue(!menu.canFill() && !menu.clickMenuButton(player, 0), "Survival fill enabled without inventory ingredients");
            for (int i = 0; i < parts.size(); i++) player.getInventory().setItem(i, parts.get(i).ingredient().getItems()[0].copyWithCount(parts.get(i).count()));
            var repeated = parts.getFirst();
            h.assertTrue(parts.stream().filter(part -> part.ingredient().test(repeated.ingredient().getItems()[0])).count() > 1, "Fixture needs a repeated ingredient");
            var withheld = player.getInventory().removeItemNoUpdate(0);
            h.assertTrue(!menu.canFill() && !menu.clickMenuButton(player, 0), "Repeated ingredient was counted twice or incomplete fill was accepted");
            h.assertTrue(station.isEmpty() && player.getInventory().getItem(1).getCount() == parts.get(1).count(), "Rejected fill moved part of a recipe");
            player.getInventory().setItem(0, withheld);
            h.assertTrue(menu.canFill() && menu.clickMenuButton(player, 0) && holder.value().matches(station.input(), h.getLevel()), "Complete inventory did not enable and fill the recipe");
            h.assertTrue(!menu.canFill(), "Already complete Survival grid still offers Fill");
            player.getInventory().clearContent();
            player.getInventory().setItem(9, station.removeItem(repeated.slot(), repeated.count()));
            h.assertTrue(menu.canFill() && menu.clickMenuButton(player, 0), "Existing grid ingredients were ignored when checking missing inventory items");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().clearContent();
            h.assertTrue(menu.canFill() && menu.clickMenuButton(player, 0), "Creative fill was disabled for a complete grid");
            station.clearContent();
            h.assertTrue(menu.canFill() && menu.clickMenuButton(player, 0) && player.getInventory().isEmpty(), "Creative fill required owned ingredients");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void upgradeAndRepairFillReadinessTracksMissingInventoryAndCreativeMode(GameTestHelper h) {
        var player = player(h);
        var relative = new BlockPos(2, 1, 2);
        try {
            for (var block : List.of(ModBlocks.UPGRADE_STATION.get(), ModBlocks.TECH_STATION.get())) {
                h.setBlock(relative, block);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.getInventory().clearContent();
                var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(relative));
                var implant = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
                var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) implant.getItem()).getDefinition();
                de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(implant, definition, 50);
                menu.slots.get(0).set(implant.copy());
                var materials = menu.plan().materials();
                h.assertTrue(materials.size() == 2 && !menu.canFillMaterials(), "Empty Survival inventory enables Fill materials");
                player.getInventory().setItem(0, materials.get(0).stack().copyWithCount(materials.get(0).count()));
                h.assertTrue(!menu.canFillMaterials() && !menu.clickMenuButton(player, 1) && menu.getMaterialStack(0).isEmpty(), "Partial stock enabled or partially executed Fill materials");
                h.assertTrue(player.getInventory().getItem(0).getCount() == materials.get(0).count(), "Rejected fill consumed inventory items");
                player.getInventory().setItem(1, materials.get(1).stack().copyWithCount(materials.get(1).count()));
                h.assertTrue(menu.canFillMaterials() && menu.clickMenuButton(player, 1) && menu.canService(), "Full material stock did not enable the shortcut");
                h.assertTrue(!menu.canFillMaterials(), "Complete Survival material slots still offer Fill");
                player.getInventory().clearContent();
                player.getInventory().setItem(9, menu.station.removeItem(1, 1));
                h.assertTrue(menu.canFillMaterials() && menu.clickMenuButton(player, 1), "Partially supplied machine requires ingredients already in its slots");
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.getInventory().clearContent();
                h.assertTrue(menu.canFillMaterials() && menu.clickMenuButton(player, 1), "Creative Fill materials changed its enabled behavior");
                menu.station.removeItem(1, 64);
                menu.station.removeItem(2, 64);
                h.assertTrue(menu.canFillMaterials() && menu.clickMenuButton(player, 1) && player.getInventory().isEmpty(), "Creative Fill materials required inventory stock");
            }
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void repairPlansRestoreExactlyOneQuarterAndNeverOverRepairAnyImplant(GameTestHelper h) {
        for (var definition : de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog.definitions()) {
            int max = de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getMaxIntegrity(definition);
            int step = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.repairAmount(definition);
            for (int damage : new int[]{0, 1, max / 2, max}) {
                var faulted = new ItemStack(ModItems.cyberware(definition.id()).get());
                de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.applyNeuralFault(faulted, definition, damage);
                var faultPlan = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(faulted);
                h.assertTrue(faultPlan.isAvailable(), "Fault cannot be cleared: " + definition.id());
                h.assertTrue(!de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(faultPlan.output())
                        && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(faultPlan.output(), definition) == max - damage,
                        "Fault clearance restored integrity: " + definition.id());
                h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.hasNeuralFault(faulted), "Fault preview mutated input");
                var next = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(faultPlan.output());
                h.assertTrue(next.isAvailable() == (damage >= step), "Normal repair eligibility changed after fault clearance");
                if (next.isAvailable()) h.assertTrue(
                        de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(next.output(), definition) == max - damage + step,
                        "Second repair did not restore one quarter: " + definition.id());
            }
            h.assertTrue(step * 4 == max, "Repair amount is not exactly 25 percent: " + definition.id());
            var implant = new ItemStack(ModItems.cyberware(definition.id()).get());
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(implant, definition, max);
            for (int cycle = 0; cycle < 4; cycle++) {
                var plan = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(implant);
                h.assertTrue(plan.isAvailable() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(plan.output(), definition) == (cycle + 1) * step,
                        "Repair restored the wrong amount: " + definition.id());
                h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(implant, definition) == cycle * step,
                        "Preview mutated its input: " + definition.id());
                implant = plan.output();
            }
            h.assertTrue(!de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(implant).isAvailable(), "Full integrity still accepts a repair");
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(implant, definition, step - 1);
            h.assertTrue(!de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(implant).isAvailable(), "Repair would exceed missing integrity: " + definition.id());
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(implant, definition, 1);
            var exact = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(implant);
            h.assertTrue(exact.isAvailable() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(exact.output(), definition) == max,
                    "Exactly 25 percent missing did not accept a repair: " + definition.id());
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void repairStationSharesCreativeFillStopPersistenceAndOneStepPulses(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.TECH_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().clearContent();
            var pos = h.absolutePos(relative);
            var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), pos);
            var station = menu.station;
            var base = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
            var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) base.getItem()).getDefinition();
            var implant = de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.createUpgradedCopy(base, definition);
            implant.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Personal repaired implant"));
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(implant, definition, 51);
            menu.slots.get(0).set(implant.copy());
            h.assertTrue(!menu.isUpgrading() && menu.machineSlotCount() == 3 && menu.slots.size() == 39, "Repair did not adopt the shared in-place inventory");
            h.assertTrue(menu.clickMenuButton(player, 1) && menu.canService() && player.getInventory().isEmpty(), "Creative repair fill was not free");
            var materials = menu.plan().materials();
            h.assertTrue(menu.clickMenuButton(player, 0), "Repair cycle did not start");
            station.progress = 20;
            menu.clickMenuButton(player, 4);
            h.assertTrue(!station.working() && ItemStack.matches(implant, menu.getInputStack()), "Stopping repair changed the implant");
            for (int i = 0; i < materials.size(); i++) h.assertTrue(menu.getMaterialStack(i).getCount() == materials.get(i).count(), "Stopping repair consumed materials");
            menu.clickMenuButton(player, 0);
            station.progress = 23;
            menu.removed(player);
            station.loadWithComponents(station.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            var reopened = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(2, player.getInventory(), pos);
            h.assertTrue(reopened.isWorking() && reopened.progress() == 23, "Repair lost progress after closing/reloading");
            station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
            TechStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(reopened.getInputStack(), definition) == 74, "Repair did not add exactly 25 integrity in place");
            reopened.clickMenuButton(player, 1);
            var power = pos.east();
            h.getLevel().setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
            h.assertTrue(station.working() && !station.canExtractImplant(0), "Repair pulse failed or exposed a working implant");
            h.getLevel().setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
            h.assertTrue(station.working(), "Short pulse interrupted the repair");
            h.getLevel().setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState()); // Discard this busy pulse.
            station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
            TechStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
            h.assertTrue(!station.working() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(reopened.getInputStack(), definition) == 99,
                    "Second pulse did not add exactly one repair step");
            for (int i = 0; i < materials.size(); i++) station.setItem(i + 1, materials.get(i).stack().copyWithCount(32));
            h.getLevel().setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
            for (int tick = 0; tick < 90; tick++) TechStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
            h.assertTrue(!reopened.canService() && !reopened.canFillMaterials() && !station.working(), "Repair accepted less than 25 percent missing or queued a pulse");
            for (int i = 0; i < materials.size(); i++) h.assertTrue(station.getItem(i + 1).getCount() == 32, "Rejected repair consumed materials");
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(reopened.getInputStack()) == 1
                    && net.minecraft.network.chat.Component.literal("Personal repaired implant").equals(reopened.getInputStack().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "Repair lost upgrades or custom data");
            var output = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
            h.assertTrue(output != null && output.extractItem(0, 1, true).getCount() == 1 && station.canExtractImplant(0), "Completed repair is not exposed to automation");
            var collected = output.extractItem(0, 1, false);
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(collected, definition) == 99 && station.getItem(0).isEmpty(), "Repair extraction lost or duplicated the implant");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 120)
    public static void repairStationTickerAndRealHoppersProcessOnePulseInPlace(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.TECH_STATION.get());
        var level = h.getLevel();
        var pos = h.absolutePos(relative);
        var station = (TechStationBlockEntity) level.getBlockEntity(pos);
        var implant = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
        var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) implant.getItem()).getDefinition();
        de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(implant, definition, 50);
        var above = pos.above();
        level.setBlockAndUpdate(above, Blocks.HOPPER.defaultBlockState());
        var upper = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(above);
        upper.setItem(0, implant);
        upper.setCooldown(0);
        net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level, above, upper.getBlockState(), upper);
        h.assertTrue(!station.getItem(0).isEmpty(), "Hopper could not insert damaged implant");
        var materials = station.plan().materials();
        int count = 0;
        for (int i = 0; i < materials.size(); i++) {
            upper.setItem(i, materials.get(i).stack().copyWithCount(materials.get(i).count()));
            count += materials.get(i).count();
        }
        for (int i = 0; i < count; i++) {
            upper.setCooldown(0);
            net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level, above, upper.getBlockState(), upper);
        }
        h.assertTrue(station.canStart(), "Hopper did not supply repair materials");
        level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        h.runAfterDelay(100, () -> {
            h.assertTrue(!station.working() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(station.getItem(0), definition) == 75,
                    "Normal repair ticker did not complete one quarter-integrity cycle");
            var below = pos.below();
            level.setBlockAndUpdate(below, Blocks.HOPPER.defaultBlockState());
            var lower = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(below);
            h.assertTrue(net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, lower) && station.getItem(0).isEmpty(), "Bottom hopper did not collect the repaired implant");
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 40)
    public static void allStationsAcceptNormalClicksAndShiftClicksWithoutFillButtons(GameTestHelper h) {
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var station = station(h);
            var holder = recipe(h, "basic_kiroshi_optics");
            h.assertTrue(station.selectDesign(player, holder.id()), "Public design could not be selected");
            var fabricator = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
            for (var part : holder.value().parts())
                putAndPickUpThroughVanillaClicks(h, fabricator, part.slot(), part.ingredient().getItems()[0].copyWithCount(part.count()), player);
            h.assertTrue(station.canStart(player), "Manual Fabricator grid requires Fill grid");
            station.clearContent();
            player.getInventory().clearContent();
            for (var part : holder.value().parts()) shiftIntoStation(fabricator, part.ingredient().getItems()[0].copyWithCount(part.count()), player);
            h.assertTrue(station.canStart(player), "Shift-clicked Fabricator grid requires Fill grid");
            fabricator.removed(player);

            BlockPos relative = new BlockPos(2, 1, 2), pos = h.absolutePos(relative);
            for (var block : List.of(ModBlocks.UPGRADE_STATION.get(), ModBlocks.TECH_STATION.get())) {
                h.setBlock(relative, block);
                player.getInventory().clearContent();
                var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(2, player.getInventory(), pos);
                var implant = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
                var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) implant.getItem()).getDefinition();
                de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(implant, definition, 50);
                putAndPickUpThroughVanillaClicks(h, menu, 0, implant, player);
                var materials = menu.plan().materials();
                for (int i = 0; i < materials.size(); i++)
                    putAndPickUpThroughVanillaClicks(h, menu, i + 1, materials.get(i).stack().copyWithCount(materials.get(i).count()), player);
                h.assertTrue(menu.canService(), "Manual service inputs require Fill materials: " + block);
                for (int i = 0; i <= menu.materialCount(); i++) {
                    menu.clicked(i, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
                    menu.setCarried(ItemStack.EMPTY);
                }
                shiftIntoStation(menu, implant.copy(), player);
                for (var material : materials) shiftIntoStation(menu, material.stack().copyWithCount(material.count()), player);
                h.assertTrue(menu.canService(), "Shift-clicked service inputs require Fill materials: " + block);
                menu.removed(player);
            }

            h.setBlock(relative, ModBlocks.RECYCLER_STATION.get());
            player.getInventory().clearContent();
            var recycler = new RecyclerStationMenu(3, player.getInventory(), pos);
            var scrap = new ItemStack(ModItems.consumable("maxdoc_mk1").get(), 3);
            putAndPickUpThroughVanillaClicks(h, recycler, 0, scrap, player);
            h.assertTrue(recycler.canRecycle(), "Manual Recycler input cannot be processed");
            recycler.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            recycler.setCarried(ItemStack.EMPTY);
            shiftIntoStation(recycler, scrap.copy(), player);
            h.assertTrue(recycler.canRecycle() && recycler.getInputStack().getCount() == 3, "Shift-clicked Recycler input cannot be processed");
            recycler.removed(player);

            h.setBlock(relative, ModBlocks.RIPPER_STATION.get());
            player.getInventory().clearContent();
            var ripper = new de.artemis.cyberneticenhancements.common.menu.RipperStationMenu(4, player.getInventory(), pos);
            int face = de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot.FACE_1.ordinal();
            var optics = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
            putAndPickUpThroughVanillaClicks(h, ripper, face, optics, player);
            ripper.clicked(face, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            ripper.setCarried(ItemStack.EMPTY);
            shiftIntoStation(ripper, optics.copy(), player);
            h.assertTrue(ripper.slots.get(face).getItem().is(optics.getItem()), "Ripper shift-click did not install the implant");
            ripper.removed(player);
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    private static void putAndPickUpThroughVanillaClicks(GameTestHelper h, net.minecraft.world.inventory.AbstractContainerMenu menu,
            int slot, ItemStack stack, ServerPlayer player) {
        menu.setCarried(stack.copy());
        h.assertTrue(menu.slots.get(slot).mayPlace(stack) && menu.slots.get(slot).getMaxStackSize(stack) >= stack.getCount(),
                "Input rejects a valid item: " + menu.getClass().getSimpleName() + ", slot " + slot + ", " + stack);
        menu.clicked(slot, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty() && ItemStack.matches(menu.slots.get(slot).getItem(), stack),
                "Normal click failed: " + menu.getClass().getSimpleName() + ", slot " + slot + ", expected " + stack
                        + ", actual " + menu.slots.get(slot).getItem() + ", cursor " + menu.getCarried());
        menu.clicked(slot, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(menu.slots.get(slot).getItem().isEmpty() && ItemStack.matches(menu.getCarried(), stack), "Normal click failed to retrieve machine input " + slot);
        menu.clicked(slot, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty() && ItemStack.matches(menu.slots.get(slot).getItem(), stack), "Normal click failed to replace machine input " + slot);
    }

    private static void shiftIntoStation(net.minecraft.world.inventory.AbstractContainerMenu menu, ItemStack stack, ServerPlayer player) {
        player.getInventory().setItem(9, stack);
        int source = menu.slots.stream().filter(slot -> slot.container == player.getInventory()).findFirst().orElseThrow().index;
        menu.clicked(source, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void creativeUpgradeFillIsFreeAndTheSameImplantUpgradesInPlace(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().clearContent();
            var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(relative));
            var implant = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
            implant.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Same implant"));
            var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) implant.getItem()).getDefinition();
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(implant, definition, 20);
            int integrity = de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(implant, definition);
            menu.slots.get(0).set(implant);
            h.assertTrue(menu.machineSlotCount() == 3 && menu.slots.size() == 39, "Upgrade screen still exposes a separate output slot");
            for (int upgrade = 1; upgrade <= 2; upgrade++) {
                var materials = menu.plan().materials();
                h.assertTrue(menu.clickMenuButton(player, 1) && menu.canService(), "Creative Fill materials required inventory ingredients");
                menu.clickMenuButton(player, 1);
                for (int i = 0; i < materials.size(); i++) h.assertTrue(menu.getMaterialStack(i).getCount() == materials.get(i).count(), "Creative fill doubled or underfilled a material requirement");
                h.assertTrue(player.getInventory().isEmpty(), "Creative Fill touched the player inventory");
                h.assertTrue(menu.clickMenuButton(player, 0), "In-place upgrade could not start");
                h.assertTrue(!menu.station.canExtractImplant(0), "Automation could extract a working implant");
                menu.station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
                TechStationBlockEntity.tick(h.getLevel(), menu.getBlockPos(), menu.station.getBlockState(), menu.station);
                h.assertTrue(menu.getInputStack().getCount() == 1 && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(menu.getInputStack()) == upgrade, "Upgrade moved or duplicated the implant instead of updating its slot");
                h.assertTrue(menu.station.canExtractImplant(0), "Completed in-place upgrade cannot be extracted");
                h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(menu.getInputStack(), definition) == integrity
                        && net.minecraft.network.chat.Component.literal("Same implant").equals(menu.getInputStack().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "In-place upgrade changed condition or name");
            }
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void upgradeStationRecoversImplantsSavedInItsFormerOutputSlot(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var pos = h.absolutePos(relative);
        var station = (TechStationBlockEntity) h.getLevel().getBlockEntity(pos);
        var previous = net.minecraft.core.NonNullList.withSize(4, ItemStack.EMPTY);
        var implant = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
        var definition = ((de.artemis.cyberneticenhancements.common.item.CyberwareItem) implant.getItem()).getDefinition();
        var upgraded = de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.createUpgradedCopy(implant, definition);
        previous.set(3, upgraded.copy());
        var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
        net.minecraft.world.ContainerHelper.saveAllItems(saved, previous, h.getLevel().registryAccess());
        station.loadWithComponents(saved, h.getLevel().registryAccess());
        station.loadWithComponents(station.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        TechStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
        h.assertTrue(station.getContainerSize() == 3 && ItemStack.matches(station.getItem(0), upgraded) && station.canExtractImplant(0), "Old output was lost instead of moved to the implant slot");
        TechStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
        h.assertTrue(station.getItem(0).getCount() == 1, "Migration duplicated the completed implant");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void upgradeStationStopsSafelyAndPersistsItsInventoryAndTimedJob(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var pos = h.absolutePos(relative);
        var player = player(h);
        try {
            player.getInventory().clearContent();
            var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), pos);
            var station = menu.station;
            var implant = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
            menu.slots.get(0).set(implant.copy());
            var materials = menu.plan().materials();
            for (int i = 0; i < materials.size(); i++) player.getInventory().setItem(i, materials.get(i).stack().copyWithCount(materials.get(i).count()));
            h.assertTrue(menu.clickMenuButton(player, 1) && menu.canService(), "Fill materials failed");
            h.assertTrue(menu.clickMenuButton(player, 2) && java.util.stream.IntStream.range(0, 2).allMatch(i -> menu.getMaterialStack(i).isEmpty()), "Clear materials failed");
            menu.clickMenuButton(player, 1);
            h.assertTrue(menu.clickMenuButton(player, 0), "Timed upgrade did not start");
            h.assertTrue(ItemStack.matches(menu.getInputStack(), implant), "Starting consumed the implant or materialized preview output");
            h.assertTrue(!menu.slots.get(0).mayPickup(player) && !menu.slots.get(1).mayPickup(player)
                    && menu.quickMoveStack(player, 0).isEmpty() && !menu.clickMenuButton(player, 2), "Running inputs could be removed");
            station.progress = 21;
            h.assertTrue(menu.clickMenuButton(player, 4) && !station.working() && station.progress == 0, "Stop did not cancel the cycle");
            for (int i = 0; i < materials.size(); i++) h.assertTrue(menu.getMaterialStack(i).getCount() == materials.get(i).count(), "Stop consumed unfinished materials");
            h.assertTrue(menu.clickMenuButton(player, 0), "Stopped upgrade could not restart");
            station.progress = 23;
            menu.removed(player);
            station.loadWithComponents(station.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            var reopened = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(2, player.getInventory(), pos);
            h.assertTrue(reopened.isWorking() && reopened.progress() == 23 && ItemStack.matches(reopened.getInputStack(), implant), "Closing/reloading lost job or inventory");
            station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
            TechStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
            h.assertTrue(!reopened.isWorking() && !reopened.getInputStack().isEmpty()
                    && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(reopened.getInputStack()) == 1, "Upgrade did not commit exactly one step");
            h.assertTrue(!reopened.quickMoveStack(player, 0).isEmpty() && reopened.quickMoveStack(player, 0).isEmpty(), "Completed upgrade could not be collected once");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void upgradeStationHoppersPipesAndPulsesWorkWithoutAnOpenMenu(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var level = h.getLevel();
        var pos = h.absolutePos(relative);
        var station = (TechStationBlockEntity) level.getBlockEntity(pos);
        var input = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        var output = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        var unsided = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, (Direction) null);
        h.assertTrue(input != null && input.getSlots() == 3 && output != null && output.getSlots() == 1 && unsided != null && unsided.getSlots() == 3, "Upgrade automation capability missing or mapped incorrectly");
        var implant = new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get());
        h.assertTrue(input.insertItem(0, implant, true).isEmpty() && station.isEmpty(), "Simulated insertion modified the station");
        h.assertTrue(input.getSlotLimit(0) == 1 && input.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "Input accepts invalid stacks");
        var above = pos.above();
        var below = pos.below();
        level.setBlockAndUpdate(above, Blocks.HOPPER.defaultBlockState());
        level.setBlockAndUpdate(below, Blocks.HOPPER.defaultBlockState());
        var upper = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(above);
        var lower = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(below);
        upper.setItem(0, implant.copy());
        upper.setCooldown(0);
        net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level, above, upper.getBlockState(), upper);
        h.assertTrue(station.getItem(0).is(implant.getItem()), "Real hopper failed to insert the implant");
        var materials = station.plan().materials();
        int total = 0;
        for (int i = 0; i < materials.size(); i++) {
            upper.setItem(i, materials.get(i).stack().copyWithCount(materials.get(i).count()));
            total += materials.get(i).count();
        }
        for (int i = 0; i < total; i++) {
            upper.setCooldown(0);
            net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level, above, upper.getBlockState(), upper);
        }
        h.assertTrue(upper.isEmpty() && station.canStart(), "Real hopper failed to supply required materials");
        h.assertTrue(input.extractItem(0, 64, false).isEmpty() && unsided.extractItem(1, 64, false).isEmpty()
                && !net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, lower), "Automation extracted reserved inputs");
        h.assertTrue(output.insertItem(0, implant, false).getCount() == 1, "Pipe inserted into the output");
        var power = pos.east();
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertTrue(station.working(), "Pulse failed to start an unattended upgrade");
        station.progress = 20;
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        h.assertTrue(station.working() && station.progress == 20, "Falling edge stopped an upgrade");
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState()); // Busy pulse is discarded.
        station.loadWithComponents(station.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
        TechStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(station.getItem(0)) == 1, "Pulse did not complete exactly one upgrade");
        h.assertTrue(output.extractItem(0, 1, true).getCount() == 1 && !station.getItem(0).isEmpty(), "Simulated extraction changed output");
        h.assertTrue(net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, lower) && station.getItem(0).isEmpty(), "Real hopper did not collect completed upgrade");
        h.assertTrue(input.insertItem(0, implant, false).isEmpty(), "Pipe could not feed the next implant");
        for (int i = 0; i < materials.size(); i++) h.assertTrue(input.insertItem(i + 1, materials.get(i).stack().copyWithCount(materials.get(i).count()), false).isEmpty(), "Pipe could not feed materials");
        for (int tick = 0; tick < 90; tick++) TechStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working() && station.getItem(0).is(implant.getItem()) && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(station.getItem(0)) == 0, "Busy pulse was queued or held power started another upgrade");
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertTrue(station.working(), "A new pulse did not start the next upgrade");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void upgradeStationRejectsIncompleteAndChangedJobsWithoutConsumingInputs(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var level = h.getLevel();
        var pos = h.absolutePos(relative);
        var station = (TechStationBlockEntity) level.getBlockEntity(pos);
        station.setItem(0, new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get()));
        h.assertTrue(!station.start(null), "Upgrade started without material costs");
        var materials = station.plan().materials();
        for (int i = 0; i < materials.size(); i++) station.setItem(i + 1, materials.get(i).stack().copyWithCount(materials.get(i).count()));
        h.assertTrue(station.start(null), "Complete upgrade could not start");
        station.removeItem(1, 64); // Simulate an external inventory mutation.
        TechStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working() && !station.getItem(0).isEmpty() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(station.getItem(0)) == 0, "Missing materials produced an upgrade or destroyed the implant");
        for (int i = 0; i < materials.size(); i++) station.setItem(i + 1, materials.get(i).stack().copyWithCount(materials.get(i).count()));
        h.assertTrue(station.start(null), "Restored job could not restart");
        station.getItem(0).set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Changed implant"));
        TechStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(station.getItem(0)) == 0, "Changed input was upgraded using stale job data");
        for (int i = 0; i < materials.size(); i++) h.assertTrue(station.getItem(i + 1).getCount() == materials.get(i).count(), "Cancelled mutation consumed material costs");
        station.setItem(0, new ItemStack(Items.DIAMOND));
        h.assertTrue(!station.start(null), "Non-implant item could be upgraded");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 120)
    public static void upgradeStationBlockTickerFinishesAndBreakingDropsItsContents(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var level = h.getLevel();
        var pos = h.absolutePos(relative);
        var station = (TechStationBlockEntity) level.getBlockEntity(pos);
        station.setItem(0, new ItemStack(ModItems.cyberware("basic_kiroshi_optics").get()));
        var materials = station.plan().materials();
        for (int i = 0; i < materials.size(); i++) station.setItem(i + 1, materials.get(i).stack().copyWithCount(materials.get(i).count() + 1));
        level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(100, () -> {
            h.assertTrue(!station.working() && !station.getItem(0).isEmpty()
                    && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(station.getItem(0)) == 1, "Block ticker failed to finish the unattended upgrade");
            int total = java.util.stream.IntStream.range(0, station.getContainerSize()).map(i -> station.getItem(i).getCount()).sum();
            var bounds = new net.minecraft.world.phys.AABB(pos).inflate(2);
            int before = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream().mapToInt(e -> e.getItem().getCount()).sum();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            int after = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream().mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(after - before == total, "Breaking the station lost or duplicated its output/materials");
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void recyclerPulsesRunOneCycleAndNeverQueueOrRetry(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.RECYCLER_STATION.get());
        var level = h.getLevel();
        var pos = h.absolutePos(relative);
        var station = (RecyclerStationBlockEntity) level.getBlockEntity(pos);
        var power = pos.east();
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState()); // Empty: reject.
        station.setItem(0, new ItemStack(ModItems.consumable("maxdoc_mk1").get(), 3));
        RecyclerStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working(), "Held signal retried after input arrived");
        int perCycle = station.plan().outputs().stream().mapToInt(ItemStack::getCount).sum();
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertTrue(station.working(), "Pulse did not start recycling");
        station.progress = 20;
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        h.assertTrue(station.working() && station.progress == 20, "Falling edge interrupted the cycle");
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState()); // Busy: reject.
        station.loadWithComponents(station.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        station.progress = RecyclerStationBlockEntity.RECYCLE_TICKS - 1;
        RecyclerStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working() && station.getItem(0).getCount() == 2, "Pulse or saved job processed more than one input");
        for (int tick = 0; tick < 70; tick++) RecyclerStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        int count = java.util.stream.IntStream.range(1, station.getContainerSize()).map(i -> station.getItem(i).getCount()).sum();
        h.assertTrue(count == perCycle && station.getItem(0).getCount() == 2, "Held signal or busy pulse created another cycle");
        station.loadWithComponents(station.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        RecyclerStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working(), "Reload created a new pulse from a held signal");
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertTrue(station.working(), "Next pulse did not start another cycle");
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        station.progress = RecyclerStationBlockEntity.RECYCLE_TICKS - 1;
        RecyclerStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working() && station.getItem(0).getCount() == 1, "Second pulse repeated or failed to complete");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void smallerRecyclerPreservesRecoveryFromTheFormerLargeGrid(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.RECYCLER_STATION.get());
        var pos = h.absolutePos(relative);
        var station = (RecyclerStationBlockEntity) h.getLevel().getBlockEntity(pos);
        var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
        var legacy = net.minecraft.core.NonNullList.withSize(82, ItemStack.EMPTY);
        for (int i = 1; i <= 25; i++) legacy.set(i, new ItemStack(Items.COBBLESTONE, 64));
        legacy.set(81, new ItemStack(Items.DIAMOND, 3));
        net.minecraft.world.ContainerHelper.saveAllItems(saved, legacy, h.getLevel().registryAccess());
        station.loadWithComponents(saved, h.getLevel().registryAccess());
        // A second save before the first tick must preserve the migration leftovers too.
        station.loadWithComponents(station.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        var bounds = new net.minecraft.world.phys.AABB(pos).inflate(2);
        RecyclerStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
        h.assertTrue(station.getContainerSize() == 26 && station.getItem(25).getCount() == 64, "Resizing lost the visible stored recovery");
        int count = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                .filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(count == 3, "Legacy output outside the new grid was lost or duplicated");
        RecyclerStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
        int repeated = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                .filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(repeated == count, "Migration duplicated surplus recovery on the following tick");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void recyclerRepeatsAndCanStopAndResumeSavedProgress(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.RECYCLER_STATION.get());
        var player = player(h);
        try {
            player.getInventory().clearContent();
            var menu = new RecyclerStationMenu(1, player.getInventory(), h.absolutePos(relative));
            var station = menu.station;
            player.getInventory().setItem(9, new ItemStack(ModItems.consumable("maxdoc_mk1").get(), 3));
            h.assertTrue(!menu.clickMenuButton(player, 0) && !menu.clickMenuButton(player, 4), "Removed input actions are still available");
            h.assertTrue(!menu.quickMoveStack(player, de.artemis.cyberneticenhancements.common.menu.RecyclerStationLayout.MACHINE_SLOT_COUNT).isEmpty()
                    && station.getItem(0).getCount() == 3, "Shift-click did not transfer recyclable input");
            int perBatch = station.plan().outputs().stream().mapToInt(ItemStack::getCount).sum();
            h.assertTrue(menu.clickMenuButton(player, 1), "Manual recycling did not start");
            h.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "Running input could be removed");
            station.progress = 20;
            menu.clickMenuButton(player, 2);
            h.assertTrue(!station.working() && station.progress == 0 && station.getItem(0).getCount() == 3 && menu.outputCount() == 0, "Stop consumed unfinished input");
            menu.clickMenuButton(player, 1);
            for (int batch = 0; batch < 3; batch++) {
                h.assertTrue(station.working(), "Recycling stopped before its input emptied");
                station.progress = 19;
                var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
                station.loadWithComponents(saved, h.getLevel().registryAccess());
                h.assertTrue(station.working() && station.progress == 19 && station.getItem(0).getCount() == 3 - batch, "Reload lost the running batch");
                station.progress = RecyclerStationBlockEntity.RECYCLE_TICKS - 1;
                RecyclerStationBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
                h.assertTrue(menu.outputCount() == perBatch * (batch + 1), "Repeated recycling lost or duplicated material");
            }
            h.assertTrue(!station.working() && station.getItem(0).isEmpty(), "Recycler did not stop after depletion");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void recyclerCommitsOnlyWholeBatchesAndMergesIntoExistingRecovery(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.RECYCLER_STATION.get());
        var station = (RecyclerStationBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(relative));
        station.setItem(0, new ItemStack(ModItems.cyberware("memory_boost").get()));
        var batch = station.plan().outputs();
        h.assertTrue(batch.size() > 1, "Fixture needs multiple recovery materials");
        for (int i = 1; i < station.getContainerSize(); i++) station.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        station.setItem(1, ItemStack.EMPTY);
        h.assertTrue(!station.start(null) && station.getItem(0).getCount() == 1 && station.getItem(1).isEmpty(), "Partial batch consumed input or inserted recovery");
        for (int i = 0; i < batch.size(); i++) {
            ItemStack output = batch.get(i);
            station.setItem(i + 1, output.copyWithCount(output.getMaxStackSize() - output.getCount()));
        }
        h.assertTrue(station.start(null), "Existing recovery stacks did not accept a complete matching batch");
        station.progress = RecyclerStationBlockEntity.RECYCLE_TICKS - 1;
        RecyclerStationBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        for (int i = 0; i < batch.size(); i++) h.assertTrue(station.getItem(i + 1).getCount() == batch.get(i).getMaxStackSize(), "Recovery did not merge precisely");
        station.setItem(0, new ItemStack(ModItems.cyberware("memory_boost").get()));
        h.assertTrue(!station.start(null) && station.getItem(0).getCount() == 1, "Full recovery grid consumed another input");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void recyclerSupportsRealHoppersPipesAndRedstoneWithoutAnOpenMenu(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.RECYCLER_STATION.get());
        var level = h.getLevel();
        var pos = h.absolutePos(relative);
        var station = (RecyclerStationBlockEntity) level.getBlockEntity(pos);
        var input = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        var output = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        var unsided = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, (Direction) null);
        h.assertTrue(input != null && output != null && unsided != null && input.getSlots() == 1 && output.getSlots() == 25 && unsided.getSlots() == 26, "Missing or incorrectly mapped automation capability");
        var sample = new ItemStack(ModItems.consumable("maxdoc_mk1").get(), 2);
        h.assertTrue(input.insertItem(0, sample, true).isEmpty() && station.isEmpty() && sample.getCount() == 2, "Simulation mutated input");
        h.assertTrue(input.insertItem(0, sample, false).isEmpty() && station.getItem(0).getCount() == 2, "Pipe could not insert recyclable input");
        h.assertTrue(input.extractItem(0, 64, false).isEmpty() && unsided.extractItem(0, 64, false).isEmpty()
                && output.insertItem(0, sample, false).getCount() == 2, "Pipes extracted input or inserted into output");
        station.clearContent();
        h.assertTrue(input.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "Automation accepted nonrecyclable input");
        var above = pos.above();
        var below = pos.below();
        level.setBlockAndUpdate(above, Blocks.HOPPER.defaultBlockState());
        level.setBlockAndUpdate(below, Blocks.HOPPER.defaultBlockState());
        var upper = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(above);
        var lower = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(below);
        upper.setItem(0, sample.copy());
        for (int i = 0; i < 2; i++) {
            upper.setCooldown(0);
            net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level, above, upper.getBlockState(), upper);
        }
        h.assertTrue(upper.isEmpty() && station.getItem(0).getCount() == 2, "Real hopper could not feed input");
        h.assertTrue(!net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, lower), "Bottom hopper extracted unrecycled input");
        var power = pos.east();
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        RecyclerStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(station.working() && station.progress == 1, "Redstone did not start an unattended recycler");
        station.progress = 30;
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        h.assertTrue(station.working() && station.progress == 30 && station.getItem(0).getCount() == 2, "Signal loss interrupted a pulsed recycling cycle");
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        RecyclerStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        station.progress = RecyclerStationBlockEntity.RECYCLE_TICKS - 1;
        RecyclerStationBlockEntity.tick(level, pos, station.getBlockState(), station);
        h.assertTrue(!station.working() && station.getItem(0).getCount() == 1 && !station.getItem(1).isEmpty(), "One pulse did not produce exactly one recovery batch");
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        h.assertTrue(output.extractItem(0, 1, true).getCount() == 1 && !station.getItem(1).isEmpty(), "Simulated extraction changed recovery");
        h.assertTrue(net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, lower) && !lower.isEmpty(), "Real bottom hopper could not extract recovery");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 110)
    public static void recyclerBlockTickerRunsAndBreakingDropsStoredItems(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.RECYCLER_STATION.get());
        var pos = h.absolutePos(relative);
        var station = (RecyclerStationBlockEntity) h.getLevel().getBlockEntity(pos);
        station.setItem(0, new ItemStack(ModItems.consumable("maxdoc_mk1").get()));
        int expected = station.plan().outputs().stream().mapToInt(ItemStack::getCount).sum();
        h.getLevel().setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(80, () -> {
            h.assertTrue(station.getItem(0).isEmpty() && !station.working(), "Normal block ticker did not finish unattended recycling");
            int total = java.util.stream.IntStream.range(1, station.getContainerSize()).map(i -> station.getItem(i).getCount()).sum();
            h.assertTrue(total == expected, "Unattended cycle created an incorrect recovery batch");
            var bounds = new net.minecraft.world.phys.AABB(pos).inflate(2);
            int before = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream().mapToInt(e -> e.getItem().getCount()).sum();
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            int after = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream().mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(after - before == total, "Breaking recycler lost or duplicated stored recovery");
            h.succeed();
        });
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void everyComponentTierIsUsedToFabricateFinishedCyberware(GameTestHelper h) {
        var components = List.of(ModItems.COMMON_ITEM_COMPONENTS.get(), ModItems.UNCOMMON_ITEM_COMPONENTS.get(),
                ModItems.RARE_ITEM_COMPONENTS.get(), ModItems.EPIC_ITEM_COMPONENTS.get(), ModItems.LEGENDARY_ITEM_COMPONENTS.get());
        int[] uses = new int[components.size()];
        for (var definition : de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog.definitions()) {
            var holder = recipe(h, definition.id());
            int tier = definition.tier().ordinal();
            var ingredient = new ItemStack(components.get(tier));
            h.assertTrue(holder.value().parts().stream().anyMatch(part -> part.ingredient().test(ingredient)),
                    "Finished cyberware has no matching-tier component requirement: " + definition.id());
            uses[tier]++;
        }
        for (int tier = 0; tier < uses.length; tier++)
            h.assertTrue(uses[tier] > 0, "Component tier has no finished equipment crafting use: " + components.get(tier));
        h.succeed();
    }

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

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 40)
    public static void schematicExtractionChargesOnceAndPreservesFirstDiscovery(GameTestHelper h) {
        var player = player(h);
        try {
            String design = "militech_apogee";
            var schematic = FabricationContent.SCHEMATICS.get(design).get();
            int cost = SchematicKnowledge.EXTRACTION_COST;
            h.assertTrue(SchematicKnowledge.learn(player, design), "Could not learn the test design");
            var history = SchematicKnowledge.history(player, design);
            int experience = player.totalExperience;
            de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.setBalance(player, cost - 1);
            SchematicKnowledge.extract(player, design);
            h.assertTrue(SchematicKnowledge.knows(player, design) && !player.getInventory().contains(new ItemStack(schematic)),
                    "An unfunded extraction removed knowledge or gave an item");
            de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.setBalance(player, cost * 2);
            SchematicKnowledge.extract(player, design);
            SchematicKnowledge.extract(player, design);
            h.assertTrue(!SchematicKnowledge.knows(player, design) && player.getInventory().countItem(schematic) == 1,
                    "Extraction did not remove the unlock and return exactly one item");
            h.assertTrue(de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player) == cost,
                    "Extraction charged an incorrect amount");
            h.assertTrue(SchematicKnowledge.learn(player, design) && player.totalExperience == experience
                    && history.equals(SchematicKnowledge.history(player, design)), "Relearning awarded discovery twice or changed its history");
            h.succeed();
        } finally {
            player.server.getPlayerList().remove(player);
        }
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
    public static void everyRecipeCanBeFabricatedWithItsLearnedSchematic(GameTestHelper h) {
        var station = station(h);
        var player = player(h);
        try {
            for (String id : FabricationContent.SCHEMATICS.keySet()) SchematicKnowledge.learn(player, id);
            var menu = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
            var recipes = station.recipes();
            h.assertTrue(!recipes.isEmpty(), "No fabrication recipes loaded");
            for (var recipe : recipes) {
                station.clearContent();
                supply(station, recipe);
                h.assertTrue(menu.availableRecipes().stream().anyMatch(r -> r.id().equals(recipe.id())), "Learned recipe is unavailable: " + recipe.id());
                h.assertTrue(menu.clickMenuButton(player, 100 + menu.recipes().indexOf(recipe)), "Learned recipe cannot be selected: " + recipe.id());
                ItemStack expected = recipe.value().assemble(station.input(), h.getLevel().registryAccess());
                h.assertTrue(menu.clickMenuButton(player, 1), "Recipe cannot start: " + recipe.id());
                station.progress = recipe.value().ticks() - 1;
                FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
                h.assertTrue(ItemStack.matches(expected, station.getItem(26)), "Wrong or missing fabricated output: " + recipe.id());
                for (int slot = 0; slot < 25; slot++)
                    h.assertTrue(station.getItem(slot).isEmpty(), "Recipe did not consume its ingredients: " + recipe.id());
            }
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void fabricationRepeatsUntilIngredientsRunOutOrOutputFills(GameTestHelper h) {
        var station = station(h);
        var player = player(h);
        try {
            var holder = station.recipes().stream().filter(r -> r.value().result().getMaxStackSize() >= 4 * r.value().result().getCount()
                    && r.value().parts().stream().allMatch(p -> p.ingredient().getItems()[0].getMaxStackSize() >= 3 * p.count()))
                    .findFirst().orElseThrow();
            var recipe = holder.value();
            SchematicKnowledge.learn(player, recipe.schematic());
            for (int scenario = 0; scenario < 3; scenario++) {
                station.clearContent();
                supply(station, holder);
                for (var part : recipe.parts()) station.getItem(part.slot()).setCount(part.count() * 3);
                ItemStack result = recipe.assemble(station.input(), h.getLevel().registryAccess());
                int initialOutput = scenario == 1 ? result.getMaxStackSize() - result.getCount() * 2 : 0;
                station.setItem(26, initialOutput == 0 ? ItemStack.EMPTY : result.copyWithCount(initialOutput));
                h.assertTrue(station.start(player), "Repeat fabrication did not start");
                int crafts = scenario == 1 ? 2 : scenario == 2 ? 1 : 3;
                for (int craft = 0; craft < crafts; craft++) {
                    h.assertTrue(station.working(), "Fabricator stopped before exhausting its available batches");
                    station.progress = recipe.ticks() - 1;
                    FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
                    h.assertTrue(station.getItem(26).getCount() == initialOutput + (craft + 1) * result.getCount(),
                            "Repeated fabrication lost or duplicated output");
                    h.assertTrue(station.progress == 0, "Next batch did not reset its progress");
                    if (craft == 0 && scenario == 0) {
                        var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
                        station.loadWithComponents(saved, h.getLevel().registryAccess());
                        h.assertTrue(station.working(), "Saving between batches lost the running job");
                    }
                }
                if (scenario == 2) station.cancel();
                h.assertTrue(!station.working(), "Fabricator kept running after depletion, a full output, or cancellation");
                var before = station.getItem(26).copy();
                for (int tick = 0; tick <= recipe.ticks(); tick++)
                    FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
                h.assertTrue(ItemStack.matches(before, station.getItem(26)), "Stopped fabrication restarted itself");
                for (var part : recipe.parts())
                    h.assertTrue(station.getItem(part.slot()).getCount() == part.count() * (3 - crafts),
                            "Fabrication consumed ingredients for an unfinished batch");
            }
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void fabricatorHoppersAndPipesRespectRecipeSlots(GameTestHelper h) {
        var station = station(h);
        var player = player(h);
        try {
            var holder = recipe(h, "basic_kiroshi_optics");
            h.assertTrue(station.selectDesign(player, holder.id()), "Public design could not be configured for automation");
            var level = h.getLevel();
            var input = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    station.getBlockPos(), net.minecraft.core.Direction.UP);
            var output = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    station.getBlockPos(), net.minecraft.core.Direction.DOWN);
            var unsided = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    station.getBlockPos(), (net.minecraft.core.Direction) null);
            h.assertTrue(input != null && output != null && unsided != null, "Fabricator item capabilities are missing");
            h.assertTrue(input.getSlots() == 25 && output.getSlots() == 1 && unsided.getSlots() == 26,
                    "Automation exposes the wrong input, output, or legacy slots");
            var first = holder.value().parts().getFirst();
            ItemStack sample = first.ingredient().getItems()[0].copyWithCount(2);
            input.insertItem(first.slot(), sample, true);
            h.assertTrue(station.isEmpty() && sample.getCount() == 2, "Simulated pipe insertion changed items");
            for (var part : holder.value().parts()) {
                ItemStack remaining = part.ingredient().getItems()[0].copyWithCount(part.count() * 2);
                remaining = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(input, remaining, false);
                h.assertTrue(remaining.isEmpty(), "Pipe rejected required recipe ingredients");
            }
            for (var part : holder.value().parts())
                h.assertTrue(station.getItem(part.slot()).getCount() == part.count() * 2,
                        "Pipe insertion filled the first matching slot instead of distributing ingredients");
            h.assertTrue(input.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1
                    && input.extractItem(first.slot(), 64, false).isEmpty()
                    && unsided.extractItem(first.slot(), 64, false).isEmpty(), "Pipe corrupted or extracted reserved inputs");
            h.assertTrue(output.insertItem(0, sample, false).getCount() == sample.getCount(), "Pipe inserted into the output");

            // Exercise real vanilla hopper insertion and extraction, not only the capability methods.
            station.clearContent();
            BlockPos above = station.getBlockPos().above(), below = station.getBlockPos().below();
            level.setBlockAndUpdate(above, net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState());
            level.setBlockAndUpdate(below, net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState());
            var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(above);
            hopper.setItem(0, sample.copy());
            for (int n = 0; n < 2; n++) {
                hopper.setCooldown(0);
                net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level, above, hopper.getBlockState(), hopper);
            }
            h.assertTrue(hopper.isEmpty(), "Vanilla hopper did not feed the fabricator");
            var matching = holder.value().parts().stream().filter(p -> p.ingredient().test(sample)).toList();
            h.assertTrue(matching.size() == 2 && matching.stream().allMatch(p -> station.getItem(p.slot()).getCount() == 1),
                    "Vanilla hopper did not balance repeated ingredients");
            var lower = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(below);
            h.assertTrue(!net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, lower),
                    "Hopper extracted recipe inputs");
            station.setItem(26, holder.value().result().copy());
            h.assertTrue(net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, lower)
                    && station.getItem(26).isEmpty() && !lower.getItem(0).isEmpty(), "Hopper did not collect the finished output");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void redstoneFabricationRunsOneCraftPerPulseWithoutQueuing(GameTestHelper h) {
        var station = station(h);
        var player = player(h);
        try {
            var holder = recipe(h, "militech_apogee");
            var recipe = holder.value();
            supply(station, holder);
            for (var part : recipe.parts()) station.getItem(part.slot()).setCount(part.count() * 3);
            var level = h.getLevel();
            BlockPos power = station.getBlockPos().east();
            level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
            FabricatorBlockEntity.tick(level, station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!station.working(), "Pulse bypassed schematic authorization");
            SchematicKnowledge.learn(player, recipe.schematic());
            h.assertTrue(station.selectDesign(player, holder.id()), "Learned design could not be configured");
            FabricatorBlockEntity.tick(level, station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!station.working(), "Held signal retried a previously rejected pulse");
            level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
            FabricatorBlockEntity.tick(level, station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(station.working() && station.progress == 1, "Rising edge did not start fabrication");
            station.progress = recipe.ticks() / 2;
            level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
            h.assertTrue(station.working() && station.progress == recipe.ticks() / 2, "Short pulse cancelled its unfinished craft");
            level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState()); // Busy pulse: discard it.
            var saved = station.saveWithFullMetadata(level.registryAccess());
            player.server.getPlayerList().remove(player);
            station.loadWithComponents(saved, level.registryAccess());
            h.assertTrue(station.working(), "Reload lost an accepted pulse job");
            station.progress = recipe.ticks() - 1;
            FabricatorBlockEntity.tick(level, station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!station.working() && !station.getItem(26).isEmpty(), "Pulse did not finish one craft");
            for (var part : recipe.parts()) h.assertTrue(station.getItem(part.slot()).getCount() == part.count() * 2, "Pulse consumed more than one set of ingredients");
            level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState()); // Full output: discard it.
            h.assertTrue(!station.working(), "Full output accepted a pulse");
            station.removeItem(26, 64);
            FabricatorBlockEntity.tick(level, station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!station.working(), "Busy/full-output pulse was queued or a held signal repeated");
            station.loadWithComponents(station.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            FabricatorBlockEntity.tick(level, station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!station.working(), "Reload treated an already high signal as a new pulse");
            level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
            h.assertTrue(station.working(), "Next pulse after completion did not start another craft");
            level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
            station.progress = recipe.ticks() - 1;
            FabricatorBlockEntity.tick(level, station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!station.working() && !station.getItem(26).isEmpty(), "Second pulse did not complete after its signal ended");
            station.removeItem(26, 64);
            FabricatorBlockEntity.tick(level, station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!station.working(), "Pulse job changed into continuous fabrication");
            h.succeed();
        } finally {
            if (player.server.getPlayerList().getPlayer(player.getUUID()) != null) player.server.getPlayerList().remove(player);
        }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void visibleInventoryKeepsVanillaLayoutAndSafeTransfers(GameTestHelper h) {
        var station = station(h);
        var player = player(h);
        try {
            var menu = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
                var slot = menu.slots.get(26 + row * 9 + column);
                h.assertTrue(slot.isActive() && slot.getContainerSlot() == 9 + row * 9 + column,
                        "Player inventory slot is hidden or mapped incorrectly");
                h.assertTrue(slot.x == FabricatorLayout.INVENTORY_X + 8 + column * 18
                        && slot.y == FabricatorLayout.INVENTORY_Y + 18 + row * 18, "Inventory slots do not align with the supplied texture");
            }
            for (int column = 0; column < 9; column++) {
                var slot = menu.slots.get(53 + column);
                h.assertTrue(slot.isActive() && slot.getContainerSlot() == column
                        && slot.x == FabricatorLayout.INVENTORY_X + 8 + column * 18
                        && slot.y == FabricatorLayout.INVENTORY_Y + 76, "Hotbar slots do not align with the supplied texture");
            }
            player.getInventory().setItem(9, new ItemStack(Items.DIRT, 8));
            h.assertTrue(!menu.quickMoveStack(player, 26).isEmpty() && player.getInventory().getItem(9).isEmpty()
                    && player.getInventory().getItem(0).getCount() == 8, "Inventory-to-hotbar shift-click failed");
            h.assertTrue(!menu.quickMoveStack(player, 53).isEmpty() && player.getInventory().getItem(0).isEmpty()
                    && player.getInventory().getItem(9).getCount() == 8, "Hotbar-to-inventory shift-click failed");
            var recipe = recipe(h, "basic_kiroshi_optics");
            supply(station, recipe);
            h.assertTrue(station.start(player), "Could not start the inventory transfer test");
            h.assertTrue(!menu.slots.get(0).mayPickup(player), "Running fabrication no longer protects its inputs");
            h.assertTrue(!menu.quickMoveStack(player, 26).isEmpty() && player.getInventory().getItem(0).getCount() == 8,
                    "Fabrication blocked normal player inventory transfers");
            station.progress = recipe.value().ticks() - 1;
            FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
            h.assertTrue(!menu.quickMoveStack(player, 25).isEmpty() && station.getItem(26).isEmpty()
                    && player.getInventory().countItem(recipe.value().result().getItem()) == recipe.value().result().getCount(),
                    "Fabricated output was lost or duplicated when shift-clicked");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void workshopInventoriesAreVisibleAndRipperKeepsItsIntegratedLayout(GameTestHelper h) {
        var player = player(h);
        BlockPos relative = new BlockPos(2, 1, 2), absolute = h.absolutePos(relative);
        try {
            var blocks = List.of(ModBlocks.TECH_STATION.get(), ModBlocks.UPGRADE_STATION.get(), ModBlocks.RECYCLER_STATION.get());
            for (int station = 0; station < blocks.size(); station++) {
                h.setBlock(relative, blocks.get(station));
                player.getInventory().clearContent();
                net.minecraft.world.inventory.AbstractContainerMenu menu = station == 2
                        ? new de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu(1, player.getInventory(), absolute)
                        : new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), absolute);
                var layout = station == 2 ? de.artemis.cyberneticenhancements.common.menu.RecyclerStationLayout.PLAYER_INVENTORY
                        : de.artemis.cyberneticenhancements.common.menu.UpgradeStationLayout.PLAYER_INVENTORY;
                int inventoryStart = station == 2 ? de.artemis.cyberneticenhancements.common.menu.RecyclerStationLayout.MACHINE_SLOT_COUNT : 3;
                for (int index = 0; index < 36; index++) {
                    var slot = menu.slots.get(inventoryStart + index);
                    int row = index / 9, column = index % 9;
                    h.assertTrue(slot.isActive() && slot.container == player.getInventory(), "Workshop player slot is not usable");
                    h.assertTrue(slot.x == layout.inventoryX() + column * 18
                            && slot.y == (row < 3 ? layout.inventoryY() + row * 18 : layout.hotbarY()), "Workshop inventory does not match its texture");
                }
                player.getInventory().setItem(9, new ItemStack(Items.DIRT, 8));
                h.assertTrue(!menu.quickMoveStack(player, inventoryStart).isEmpty() && player.getInventory().getItem(0).getCount() == 8,
                        "Workshop inventory-to-hotbar transfer failed");
                h.assertTrue(!menu.quickMoveStack(player, inventoryStart + 27).isEmpty() && player.getInventory().getItem(9).getCount() == 8,
                        "Workshop hotbar-to-inventory transfer failed");
                var implant = ModItems.cyberware("basic_kiroshi_optics").get();
                player.getInventory().setItem(9, new ItemStack(implant));
                h.assertTrue(!menu.quickMoveStack(player, inventoryStart).isEmpty() && menu.slots.get(0).getItem().is(implant),
                        "Workshop shift-click did not accept an implant");
                h.assertTrue(!menu.quickMoveStack(player, 0).isEmpty() && player.getInventory().countItem(implant) == 1,
                        "Workshop input retrieval lost or duplicated an implant");
                menu.removed(player);
            }
            h.setBlock(relative, ModBlocks.RIPPER_STATION.get());
            var ripper = new de.artemis.cyberneticenhancements.common.menu.RipperStationMenu(1, player.getInventory(), absolute);
            var ripperInventory = ripper.slots.stream().filter(s -> s.container == player.getInventory()).toList();
            h.assertTrue(ripperInventory.size() == 36, "Ripper station gained a second player inventory");
            h.assertTrue(ripperInventory.stream().allMatch(net.minecraft.world.inventory.Slot::isActive), "Ripper inventory has inactive slots");
            h.assertTrue(ripperInventory.getFirst().x == de.artemis.cyberneticenhancements.common.menu.RipperStationLayout.PLAYER_INVENTORY_X
                    && ripperInventory.getFirst().y == de.artemis.cyberneticenhancements.common.menu.RipperStationLayout.PLAYER_INVENTORY_Y
                    && ripperInventory.getLast().y == de.artemis.cyberneticenhancements.common.menu.RipperStationLayout.PLAYER_HOTBAR_Y,
                    "Ripper inventory does not match the shared native panel");
            ripper.removed(player);
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void shiftClickDistributesRepeatedIngredientsBeforeStackingExcess(GameTestHelper h) {
        var station = station(h);
        var recipe = recipe(h, "basic_kiroshi_optics");
        var repeated = recipe.value().parts().stream().filter(part -> {
            var item = part.ingredient().getItems()[0];
            return recipe.value().parts().stream().filter(other -> other.ingredient().test(item)).count() >= 2;
        }).findFirst().orElseThrow();
        var sample = repeated.ingredient().getItems()[0];
        var matching = recipe.value().parts().stream().filter(part -> part.ingredient().test(sample)).toList();
        var player = player(h);
        try {
            station.selected = recipe.id().toString();
            var menu = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
            int supplied = matching.stream().mapToInt(FabricationRecipe.Part::count).sum() + 5;
            player.getInventory().setItem(9, sample.copyWithCount(supplied));
            h.assertTrue(!menu.quickMoveStack(player, 26).isEmpty(), "Recipe shift-click failed");
            for (var part : matching)
                h.assertTrue(station.getItem(part.slot()).getCount() >= part.count(), "Excess filled the first slot before all recipe positions were supplied");
            h.assertTrue(matching.stream().mapToInt(part -> station.getItem(part.slot()).getCount()).sum()
                    + player.getInventory().getItem(9).getCount() == supplied, "Shift-click lost or duplicated ingredients");
            int smallest = matching.stream().mapToInt(part -> station.getItem(part.slot()).getCount()).min().orElseThrow();
            int largest = matching.stream().mapToInt(part -> station.getItem(part.slot()).getCount()).max().orElseThrow();
            h.assertTrue(largest - smallest <= 1, "Surplus ingredients were concentrated in the first slot");
            var first = matching.getFirst();
            for (var part : matching) station.setItem(part.slot(), ItemStack.EMPTY);
            station.setItem(first.slot(), sample.copyWithCount(first.count() + 3));
            int neededElsewhere = matching.stream().filter(part -> part != first).mapToInt(FabricationRecipe.Part::count).sum();
            player.getInventory().setItem(9, sample.copyWithCount(neededElsewhere));
            menu.quickMoveStack(player, 26);
            h.assertTrue(station.getItem(first.slot()).getCount() == first.count() + 3, "Existing surplus was topped up before empty recipe slots");
            for (var part : matching)
                h.assertTrue(station.getItem(part.slot()).getCount() >= part.count(), "Shift-click left a matching recipe position empty");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void recipeDistributionRespectsShortagesComponentsAndSlotLimits(GameTestHelper h) {
        var inventory = new net.minecraft.world.SimpleContainer(3);
        var source = new ItemStack(Items.IRON_INGOT, 5);
        var targets = new java.util.ArrayList<de.artemis.cyberneticenhancements.common.menu.RecipeSlotTransfer.Target>();
        for (int index = 0; index < 3; index++) targets.add(new de.artemis.cyberneticenhancements.common.menu.RecipeSlotTransfer.Target(
                new net.minecraft.world.inventory.Slot(inventory, index, 0, 0), 4));
        de.artemis.cyberneticenhancements.common.menu.RecipeSlotTransfer.distributeRequired(source, targets);
        h.assertTrue(source.isEmpty() && inventory.getItem(0).getCount() == 2 && inventory.getItem(1).getCount() == 2
                && inventory.getItem(2).getCount() == 1, "A short stack was not spread across repeated recipe slots");
        inventory.clearContent();
        var named = new ItemStack(Items.IRON_INGOT);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Reserved"));
        inventory.setItem(0, named);
        targets.clear();
        targets.add(new de.artemis.cyberneticenhancements.common.menu.RecipeSlotTransfer.Target(new net.minecraft.world.inventory.Slot(inventory, 0, 0, 0), 2));
        targets.add(new de.artemis.cyberneticenhancements.common.menu.RecipeSlotTransfer.Target(new net.minecraft.world.inventory.Slot(inventory, 1, 0, 0), 2));
        targets.add(new de.artemis.cyberneticenhancements.common.menu.RecipeSlotTransfer.Target(new net.minecraft.world.inventory.Slot(inventory, 2, 0, 0) {
            @Override public int getMaxStackSize() { return 1; }
        }, 2));
        source = new ItemStack(Items.IRON_INGOT, 4);
        de.artemis.cyberneticenhancements.common.menu.RecipeSlotTransfer.distributeRequired(source, targets);
        h.assertTrue(inventory.getItem(0).getCount() == 1 && inventory.getItem(0).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                && inventory.getItem(1).getCount() == 2 && inventory.getItem(2).getCount() == 1 && source.getCount() == 1,
                "Distribution merged incompatible item data, exceeded a slot limit, or lost excess items");
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void jeiTransferSelectsAndFillsEveryResearchedRecipe(GameTestHelper h) {
        var station = station(h);
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
        player.containerMenu = menu;
        try {
            for (String id : FabricationContent.SCHEMATICS.keySet()) SchematicKnowledge.learn(player, id);
            for (var recipe : station.recipes()) {
                station.clearContent(); player.getInventory().clearContent(); station.selected = "";
                for (var part : recipe.value().parts()) {
                    var stack = part.ingredient().getItems()[0].copyWithCount(part.count());
                    h.assertTrue(player.getInventory().add(stack) && stack.isEmpty(), "Test ingredients did not fit the inventory");
                }
                var before = new java.util.ArrayList<ItemStack>();
                for (int slot = 0; slot < 36; slot++) before.add(player.getInventory().getItem(slot).copy());
                var preview = FabricatorRecipeTransfer.prepare(menu, player, recipe.id(), false);
                h.assertTrue(preview.error() == FabricatorRecipeTransfer.Error.NONE, "JEI transfer preview rejected learned recipe: " + recipe.id());
                for (int slot = 0; slot < 36; slot++)
                    h.assertTrue(ItemStack.matches(before.get(slot), player.getInventory().getItem(slot)), "JEI preview moved real ingredients");
                h.assertTrue(menu.transferRecipe(player, recipe.id(), false), "JEI could not transfer recipe: " + recipe.id());
                h.assertTrue(station.selected.equals(recipe.id().toString()) && recipe.value().matches(station.input(), h.getLevel())
                        && station.canStart(player), "JEI did not select a craftable recipe and fill its exact layout: " + recipe.id());
                h.assertTrue(player.getInventory().isEmpty(), "JEI transfer left ingredients behind or duplicated them");
            }
            h.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void jeiTransferHandlesMaximumAndRejectsUnsafeChanges(GameTestHelper h) {
        var station = station(h);
        var recipe = recipe(h, "basic_kiroshi_optics");
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
        player.containerMenu = menu;
        try {
            for (var part : recipe.value().parts()) player.getInventory().add(part.ingredient().getItems()[0].copyWithCount(part.count() * 3));
            station.setItem(0, new ItemStack(Items.DIRT, 2));
            h.assertTrue(menu.transferRecipe(player, recipe.id(), true), "JEI maximum transfer failed");
            for (var part : recipe.value().parts())
                h.assertTrue(station.getItem(part.slot()).getCount() == part.count() * 3, "Maximum transfer did not fill three complete sets");
            h.assertTrue(player.getInventory().countItem(Items.DIRT) == 2 && station.getItem(0).isEmpty(), "Previous grid contents were lost or left in the recipe");
            station.clearContent(); player.getInventory().clearContent(); station.selected = "original selection";
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), false) && station.selected.equals("original selection"), "Missing ingredients changed the selected recipe");
            h.assertTrue(FabricatorRecipeTransfer.prepare(menu, player, recipe(h, "militech_apogee").id(), false).error() == FabricatorRecipeTransfer.Error.LOCKED,
                    "Unknown schematic was accepted by JEI transfer");
            h.assertTrue(!menu.transferRecipe(player, net.minecraft.resources.ResourceLocation.withDefaultNamespace("not_a_fabricator_recipe"), false), "Unknown recipe ID was accepted");
            supply(station, recipe); station.setItem(0, new ItemStack(Items.STONE));
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            h.assertTrue(FabricatorRecipeTransfer.prepare(menu, player, recipe.id(), false).error() == FabricatorRecipeTransfer.Error.INVENTORY_FULL,
                    "JEI ignored lack of room for previous grid contents");
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), false) && station.getItem(0).is(Items.STONE)
                    && recipe.value().parts().stream().allMatch(part -> station.getItem(part.slot()).getCount() == part.count()), "Failed JEI transfer changed the grid");
            menu.setCarried(new ItemStack(Items.DIAMOND));
            h.assertTrue(FabricatorRecipeTransfer.prepare(menu, player, recipe.id(), false).error() == FabricatorRecipeTransfer.Error.CURSOR,
                    "JEI transfer ignored the carried cursor item");
            menu.setCarried(ItemStack.EMPTY); station.clearContent(); supply(station, recipe);
            h.assertTrue(station.start(player), "Could not start busy transfer test");
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), false) && station.working(), "JEI modified a running fabrication");
            station.cancel(); player.containerMenu = player.inventoryMenu;
            h.assertTrue(!menu.transferRecipe(player, recipe.id(), false), "Closed menu accepted a stale JEI request");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            var payload = new FabricatorRecipeTransferPayload(menu.containerId, recipe.id(), true);
            FabricatorRecipeTransferPayload.STREAM_CODEC.encode(buffer, payload);
            h.assertTrue(payload.equals(FabricatorRecipeTransferPayload.STREAM_CODEC.decode(buffer)), "JEI transfer request lost recipe or menu identity");
            buffer.release();
            player.containerMenu = menu;
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            station.clearContent(); player.getInventory().clearContent();
            h.assertTrue(menu.transferRecipe(player, recipe.id(), false), "Creative JEI transfer could not supply a recipe");
            var first = recipe.value().parts().getFirst();
            station.getItem(first.slot()).set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keep this name"));
            h.assertTrue(menu.transferRecipe(player, recipe.id(), false) && player.getInventory().isEmpty()
                    && station.getItem(first.slot()).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME),
                    "Repeating a Creative JEI transfer duplicated grid items or replaced their data");
            h.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void creativeFillSuppliesMaterialsWithoutConsumingInventory(GameTestHelper h) {
        var station = station(h);
        var recipe = recipe(h, "basic_kiroshi_optics");
        station.selected = recipe.id().toString();
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new FabricatorMenu(1, player.getInventory(), station.getBlockPos());
        menu.clickMenuButton(player, 0);
        h.assertTrue(!recipe.value().matches(station.input(), h.getLevel()), "Survival filled without materials");

        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var part = recipe.value().parts().getFirst();
        var owned = part.ingredient().getItems()[0].copyWithCount(7);
        player.getInventory().setItem(9, owned.copy());
        h.assertTrue(menu.clickMenuButton(player, 0), "Creative fill was rejected");
        h.assertTrue(recipe.value().matches(station.input(), h.getLevel()), "Creative fill did not supply the full recipe");
        h.assertTrue(ItemStack.matches(owned, player.getInventory().getItem(9)), "Creative fill consumed owned materials");
        for (var required : recipe.value().parts())
            h.assertTrue(station.getItem(required.slot()).getCount() == required.count(), "Creative fill supplied excess ingredients");
        h.assertTrue(menu.clickMenuButton(player, 0), "Refilling a complete grid failed");
        for (var required : recipe.value().parts())
            h.assertTrue(station.getItem(required.slot()).getCount() == required.count(), "Repeated fill multiplied ingredients");
        h.assertTrue(station.start(player), "Creative-filled grid could not fabricate");
        player.server.getPlayerList().remove(player);
        h.succeed();
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
    public static void schematicsRemainRequiredWithAllMachineTiersAvailable(GameTestHelper h) {
        var station = station(h);
        var recipe = recipe(h, "militech_apogee");
        supply(station, recipe);
        var player = player(h);
        h.assertTrue(!station.start(player), "Restricted design bypassed schematic and machine checks");
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
        h.assertTrue(!menu.quickMoveStack(player, 26).isEmpty(), "Locked design prevented normal inventory-to-hotbar transfer");
        h.assertTrue(station.getItem(missingSlot).isEmpty() && !player.getInventory().getItem(0).isEmpty(), "Shift-click exposed an unlearned design's ingredient layout");
        player.getInventory().setItem(9, player.getInventory().removeItemNoUpdate(0));
        FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        h.assertTrue(station.getItem(25).isEmpty(), "Legacy reader item was trapped");
        var schematic = new ItemStack(FabricationContent.SCHEMATICS.get("militech_apogee").get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, schematic);
        schematic.getItem().use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(SchematicKnowledge.knows(player, "militech_apogee"), "Schematic was not learned");
        h.assertTrue(menu.availableRecipes().stream().anyMatch(r -> r.id().equals(recipe.id())) && menu.clickMenuButton(player, recipeButton),
                "Learning did not immediately reveal and enable selecting the design");
        h.assertTrue(schematic.isEmpty(), "Learning did not consume the schematic");
        menu.clickMenuButton(player, 0);
        h.assertTrue(recipe.value().matches(station.input(), h.getLevel()), "Learning did not unlock filling the recipe");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(station.supportedTier() == 5, "Fresh fabricator does not support every recipe tier");
        h.assertTrue(station.start(player), "Learned tier-five design failed on a fresh fabricator");
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
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(worn, d, de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.repairAmount(d));
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
        var player = player(h);
        h.assertTrue(station.start(player), "Persistent job failed to start");
        for (int i = 0; i < 10; i++)
            FabricatorBlockEntity.tick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
        saved.putInt("capability", 1); // Existing basic machines must also become unrestricted.
        var restored = (FabricatorBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(station.getBlockPos(), station.getBlockState(), saved, h.getLevel().registryAccess());
        h.assertTrue(restored != null && restored.supportedTier() == 5 && restored.progress == 10 && restored.working(), "Legacy capability restricted the machine or save lost its active job");
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
        var schematic = new ItemStack(FabricationContent.SCHEMATICS.get("militech_apogee").get(), 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, schematic);
        schematic.getItem().use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        var original = SchematicKnowledge.history(player, "militech_apogee");
        h.assertTrue(original.epochMillis() > 0 && original.position().equals(player.blockPosition()), "Discovery did not capture time and player location");
        h.assertTrue(original.source().equals("Studied schematic"), "Discovery method was lost");
        h.assertTrue(schematic.getCount() == 1, "Learning did not consume exactly one schematic");
        schematic.getItem().use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(schematic.getCount() == 1, "An already-learned schematic was wasted");
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
    public static void recyclingCommitsWholeRecoveryOnceAndPersistsUncollectedParts(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 1, 2));
        h.setBlock(new BlockPos(2, 1, 2), ModBlocks.RECYCLER_STATION.get());
        var player = player(h);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = new de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu(1, player.getInventory(), pos);
            menu.slots.get(0).set(new ItemStack(ModItems.cyberware("memory_boost").get()));
            var expected = menu.getRecyclePlan().outputs();
            h.assertTrue(expected.size() >= 2, "Fixture must exercise multiple recovered stacks");
            h.assertTrue(menu.slots.get(1).getItem().isEmpty(), "Preview materialized output");
            h.assertTrue(menu.clickMenuButton(player, 1) && menu.getInputStack().getCount() == 1, "Starting consumed input before completion");
            h.assertTrue(!menu.clickMenuButton(player, 1) && !menu.slots.get(0).mayPickup(player), "Running input was not protected");
            var station = menu.station;
            station.progress = de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity.RECYCLE_TICKS - 1;
            de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
            h.assertTrue(menu.getInputStack().isEmpty(), "Completed cycle did not consume its input");
            for (int i = 0; i < expected.size(); i++) h.assertTrue(ItemStack.matches(expected.get(i), menu.slots.get(i + 1).getItem()), "Wrong recovered batch");
            menu.quickMoveStack(player, 1);
            var pending = menu.slots.get(2).getItem().copy();
            menu.removed(player);
            var reopened = new de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu(2, player.getInventory(), pos);
            h.assertTrue(ItemStack.matches(pending, reopened.slots.get(2).getItem()), "Closing lost recovery");
            h.assertTrue(!reopened.quickMoveStack(player, 2).isEmpty() && reopened.slots.get(2).getItem().isEmpty(), "Reopened output could not be collected");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
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
        h.assertTrue(upgrade.canService() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(upgrade.getInputStack()) == 0, "Auto-fill failed or preview created an item");
        h.assertTrue(upgrade.clickMenuButton(player, 0) && !upgrade.clickMenuButton(player, 0), "Upgrade failed or replay duplicated output");
        h.assertTrue(upgrade.isWorking() && de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(upgrade.getInputStack()) == 0, "Upgrade did not wait for its timed cycle");
        upgrade.station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
        TechStationBlockEntity.tick(h.getLevel(), upgrade.getBlockPos(), upgrade.station.getBlockState(), upgrade.station);
        var upgraded = upgrade.getInputStack().copy();
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(upgraded) == 1, "Upgrade level did not advance");
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(upgraded, definition) == integrity, "Upgrade granted a free repair");
        h.assertTrue(net.minecraft.network.chat.Component.literal("Personal optics").equals(upgraded.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "Upgrade discarded custom name");
        h.assertTrue(java.util.stream.IntStream.range(0, upgrade.materialCount()).allMatch(i -> upgrade.getMaterialStack(i).isEmpty()), "Upgrade did not consume exact material costs");
        upgrade.slots.get(0).remove(1);
        upgrade.removed(player);
        h.setBlock(relative, ModBlocks.TECH_STATION.get());
        var repair = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(2, player.getInventory(), h.absolutePos(relative));
        repair.slots.get(0).set(upgraded);
        h.assertTrue(!repair.isUpgrading() && repair.materialCount() == 2, "Repair machine exposed upgrade bay");
        var repairCost = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRequiredMaterials(repair.plan());
        for (int i = 0; i < repairCost.size(); i++)
            repair.slots.get(i + 1).set(repairCost.get(i).stack().copyWithCount(repairCost.get(i).count()));
        h.assertTrue(repair.clickMenuButton(player, 0), "Repair failed with correct materials");
        h.assertTrue(repair.isWorking(), "Repair did not use the timed service cycle");
        repair.station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
        TechStationBlockEntity.tick(h.getLevel(), repair.getBlockPos(), repair.station.getBlockState(), repair.station);
        var repaired = repair.getInputStack();
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrityRatio(repaired, definition) == .75, "Repair did not restore exactly 25 percent integrity");
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(repaired) == 1 && net.minecraft.network.chat.Component.literal("Personal optics").equals(repaired.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "Repair lost upgrade or custom name");
        repair.removed(player);
        player.server.getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void workshopsSupportAllTiersAndEnforceMaterialCosts(GameTestHelper h) {
        var relative = new BlockPos(2, 1, 2);
        h.setBlock(relative, ModBlocks.UPGRADE_STATION.get());
        var player = player(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new de.artemis.cyberneticenhancements.common.menu.TechStationMenu(1, player.getInventory(), h.absolutePos(relative));
        menu.slots.get(0).set(new ItemStack(ModItems.cyberware("militech_apogee").get()));
        var costs = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRequiredMaterials(menu.plan());
        for (int i = 0; i < costs.size(); i++)
            menu.slots.get(i + 1).set(costs.get(i).stack().copyWithCount(costs.get(i).count()));
        h.assertTrue(menu.tier() == de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier.TIER_5,
                "Fresh workshop does not support tier-five implants");
        h.assertTrue(menu.clickMenuButton(player, 0) && menu.isWorking(),
                "High-tier implant could not start upgrading with the required materials");
        menu.station.progress = TechStationBlockEntity.SERVICE_TICKS - 1;
        TechStationBlockEntity.tick(h.getLevel(), menu.getBlockPos(), menu.station.getBlockState(), menu.station);
        h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper.getUpgradeLevel(menu.getInputStack()) == 1, "High-tier upgrade did not finish");
        h.assertTrue(!menu.clickMenuButton(player, 3), "Removed service capacity upgrade is still available");
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

    @GameTest(template = "sandevistan_test", batch = "economy_materials", timeoutTicks = 100)
    public static void repairCyclesCostLessThanReplacementAndRestrictedMaintenanceDoesNotNeedRareCores(GameTestHelper h) {
        var values = new HashMap<Item, Double>();
        double maximum = 0;
        var violations = new ArrayList<String>();
        for (var definition : de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog.definitions()) {
            var stack = new ItemStack(ModItems.cyberware(definition.id()).get());
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(stack, definition,
                    de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getMaxIntegrity(definition));
            var repair = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRequiredMaterials(
                    de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(stack));
            double restore = 0;
            for (var material : repair) {
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(material.stack().getItem()).getPath();
                h.assertTrue(!id.contains("core") && !id.equals("epic_item_components") && !id.equals("legendary_item_components"), "Routine repair required restricted manufacturing materials: " + definition.id());
                restore += workshopMaterialValue(h, material.stack().getItem(), values, new HashSet<>()) * material.count();
            }
            double manufacture = workshopMaterialValue(h, stack.getItem(), values, new HashSet<>());
            if (!(restore > 0 && restore < manufacture)) violations.add(definition.id() + ": " + restore + " vs " + manufacture);
            maximum = Math.max(maximum, restore / manufacture);
        }
        System.out.println("ECONOMY_AUDIT maximum quarter-repair/replacement material ratio=" + maximum);
        h.assertTrue(violations.isEmpty(), "Repair was not cheaper than replacement: " + violations);
        h.succeed();
    }

    private static double workshopMaterialValue(GameTestHelper h, Item item, Map<Item, Double> cache, Set<Item> visiting) {
        if (cache.containsKey(item)) return cache.get(item);
        if (item == ModItems.RAW_CHROMIUM.get()) return 1;
        if (item == ModItems.CHROMIUM_SCRAP.get()) return 1.0 / 9;
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
    public static void repairQuartersStopBeforeOverRepairAndSpecializedUpgradesChangeGameplay(GameTestHelper h) {
        for (var definition : de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog.definitions()) {
            var light = new ItemStack(ModItems.cyberware(definition.id()).get());
            var heavy = light.copy();
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(light, definition, 1);
            de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.damage(heavy, definition, 10000);
            var minor = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(light);
            var major = de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.getRepairPlan(heavy);
            h.assertTrue(!minor.isAvailable() && major.materials().size() == 2, "Repair accepted less than a quarter missing or lost its materials: " + definition.id());
            h.assertTrue(de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.getIntegrity(major.output(), definition) == de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper.repairAmount(definition), "Repair restored an incorrect amount: " + definition.id());
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
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var pos = h.absolutePos(relative);
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            var menu = new de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu(1, player.getInventory(), pos);
            menu.slots.get(0).set(new ItemStack(ModItems.consumable("maxdoc_mk1").get(), 2));
            var expected = menu.getRecyclePlan().outputs();
            h.assertTrue(menu.clickMenuButton(player, 1), "Recycle could not start");
            menu.station.progress = de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity.RECYCLE_TICKS - 1;
            de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity.tick(h.getLevel(), pos, menu.station.getBlockState(), menu.station);
            h.assertTrue(menu.getInputStack().getCount() == 1, "Cycle did not process exactly one input");
            h.assertTrue(menu.quickMoveStack(player, 1).isEmpty(), "Full inventory accepted output or erased it");
            menu.clickMenuButton(player, 2);
            var saved = menu.station.saveWithFullMetadata(h.getLevel().registryAccess());
            menu.removed(player);
            menu.removed(player);
            menu.station.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(menu.getInputStack().getCount() == 1 && menu.outputCount() == expected.stream().mapToInt(ItemStack::getCount).sum(), "Closing/reload lost or duplicated input or recovery");
            player.getInventory().clearContent();
            h.assertTrue(!menu.quickMoveStack(player, 1).isEmpty() && menu.quickMoveStack(player, 1).isEmpty(), "Recovered stack was claimable twice");
            h.succeed();
        } finally { player.server.getPlayerList().remove(player); }
    }

    @GameTest(template = "sandevistan_test", batch = "fabrication", timeoutTicks = 30)
    public static void changedSchematicAuthorizationCancelsSavedJobsWithoutConsumingParts(GameTestHelper h) {
        var station = station(h);
        var recipe = recipe(h, "militech_apogee");
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
            if (!type.supportsShop()) {
                h.assertTrue(de.artemis.cyberneticenhancements.common.dialogue.NpcShopCatalog.offers(type).isEmpty(), "Non-trading NPC unexpectedly has shop stock");
                continue;
            }
            for (int option = de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_BUY_MAXDOC_MK2;
                    option <= de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.OPTION_BUY_IMMUNOBLOCKERS; option++) {
                var stock = de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu.shopItem(type, option);
                h.assertTrue(!stock.isEmpty(), "NPC stock reference resolves to an empty item: " + type);
            }
        }
        h.succeed();
    }}
