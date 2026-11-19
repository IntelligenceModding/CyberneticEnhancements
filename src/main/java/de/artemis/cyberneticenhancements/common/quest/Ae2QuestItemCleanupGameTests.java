package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Ae2QuestItemCleanupGameTests {
    @GameTest(template = "sandevistan_test", batch = "ae2_quest_cell_cleanup", timeoutTicks = 100)
    public static void oldQuestItemsAreDeletedFromPortableMeCells(GameTestHelper h) {
        if (!ModList.get().isLoaded("ae2")) { h.succeed(); return; }
        WithAe2.cell(h);
    }

    @GameTest(template = "sandevistan_test", batch = "ae2_quest_network_cleanup", timeoutTicks = 300)
    public static void oldQuestItemsAreDeletedFromLoadedMeNetworks(GameTestHelper h) {
        if (!ModList.get().isLoaded("ae2")) { h.succeed(); return; }
        WithAe2.network(h);
    }

    /** Isolate optional API signatures so discovery also works with AE2 absent. */
    private static final class WithAe2 {
        private static ItemStack evidence() {
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("QuestContractId", java.util.UUID.randomUUID().toString());
            tag.putString("QuestItemType", "exfil_shard");
            var item = new ItemStack(Items.AMETHYST_SHARD);
            item.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            return item;
        }

        private static ItemStack cellStack(GameTestHelper h) {
            var cell = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", "item_storage_cell_1k")));
            h.assertTrue(!cell.isEmpty(), "AE2 test cell is not registered");
            return cell;
        }

        static void cell(GameTestHelper h) {
            var level = h.getLevel();
            var versions = QuestItemVersions.get(level);
            var original = evidence();
            versions.issue(original);
            var cell = cellStack(h);
            var storage = appeng.api.storage.StorageCells.getCellInventory(cell, null);
            var key = appeng.api.stacks.AEItemKey.of(original);
            var gold = appeng.api.stacks.AEItemKey.of(Items.GOLD_INGOT);
            h.assertTrue(storage.insert(key, 1, appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty()) == 1,
                    "Could not store evidence in ME cell");
            storage.insert(gold, 7, appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty());
            storage.persist();
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            level.setBlockAndUpdate(pos, Blocks.BARREL.defaultBlockState());
            var barrel = (net.minecraft.world.Container) level.getBlockEntity(pos);
            barrel.setItem(0, cell);
            var replacement = original.copy();
            versions.issue(replacement);
            var cleaned = appeng.api.storage.StorageCells.getCellInventory(barrel.getItem(0), null).getAvailableStacks();
            h.assertTrue(cleaned.get(key) == 0 && cleaned.get(gold) == 7 && versions.current(replacement),
                    "Cell cleanup failed or damaged unrelated items/the replacement");
            level.removeBlock(pos, false);
            h.succeed();
        }

        static void network(GameTestHelper h) {
            var level = h.getLevel();
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ae2", "creative_energy_cell"));
            h.assertTrue(block != Blocks.AIR, "AE2 network test block is not registered");
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            h.startSequence().thenWaitUntil(() -> {
                var host = level.getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
                h.assertTrue(host != null && host.getGridNode(net.minecraft.core.Direction.UP) != null
                        && host.getGridNode(net.minecraft.core.Direction.UP).hasGridBooted(), "AE2 test grid is still booting");
            }).thenExecute(() -> {
                var host = level.getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
                var service = host.getGridNode(net.minecraft.core.Direction.UP).getGrid().getStorageService();
                var versions = QuestItemVersions.get(level);
                var original = evidence();
                versions.issue(original);
                var cell = appeng.api.storage.StorageCells.getCellInventory(cellStack(h), null);
                var key = appeng.api.stacks.AEItemKey.of(original);
                var gold = appeng.api.stacks.AEItemKey.of(Items.GOLD_INGOT);
                cell.insert(key, 1, appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty());
                cell.insert(gold, 9, appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty());
                appeng.api.storage.IStorageProvider provider = mounts -> mounts.mount(cell);
                service.addGlobalStorageProvider(provider);
                try {
                    h.assertTrue(service.getInventory().getAvailableStacks().get(key) == 1, "Evidence was not mounted on the test network");
                    var replacement = original.copy();
                    versions.issue(replacement);
                    var cleaned = service.getInventory().getAvailableStacks();
                    h.assertTrue(cleaned.get(key) == 0 && cleaned.get(gold) == 9 && versions.current(replacement),
                            "Network cleanup left old evidence or damaged unrelated items/the replacement");
                } finally {
                    service.removeGlobalStorageProvider(provider);
                    level.removeBlock(pos, false);
                }
                h.succeed();
            });
        }
    }
}
