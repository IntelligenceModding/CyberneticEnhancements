package de.artemis.cyberneticenhancements.common.compat;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Predicate;

/** Loaded only when AE2 is installed; no AE2 classes are bundled with the mod. */
public final class Ae2QuestItemCleanup {
    private Ae2QuestItemCleanup() {}

    public static void network(ServerLevel level, BlockPos pos, Set<Object> visited, Predicate<ItemStack> obsolete) {
        var host = level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
        if (host == null) return;
        for (var side : Direction.values()) {
            var node = host.getGridNode(side);
            if (node == null) continue;
            appeng.api.networking.IGrid grid;
            try { grid = node.getGrid(); }
            catch (IllegalStateException notInitialized) { continue; }
            if (visited.add(grid)) purge(grid.getStorageService().getInventory(), obsolete);
        }
    }

    public static boolean cell(ItemStack stack, Predicate<ItemStack> obsolete) {
        var inventory = StorageCells.getCellInventory(stack, null);
        if (inventory == null || !purge(inventory, obsolete)) return false;
        inventory.persist();
        return true;
    }

    private static boolean purge(MEStorage inventory, Predicate<ItemStack> obsolete) {
        boolean changed = false;
        // Snapshot before extracting so storage listeners can safely update the live index.
        var available = inventory.getAvailableStacks();
        var entries = new java.util.ArrayList<Entry>();
        for (var entry : available) {
            if (entry.getKey() instanceof AEItemKey key && obsolete.test(key.toStack())) {
                entries.add(new Entry(key, entry.getLongValue()));
            }
        }
        for (var entry : entries) {
            changed |= inventory.extract(entry.key(), entry.count(), Actionable.MODULATE, IActionSource.empty()) > 0;
        }
        return changed;
    }

    private record Entry(AEItemKey key, long count) {}
}
