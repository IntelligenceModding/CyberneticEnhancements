package de.artemis.cyberneticenhancements.common.menu;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

/** Supply every recipe position, then balance surplus for repeated crafts. */
public final class RecipeSlotTransfer {
    public record Target(Slot slot, int requiredCount) {}
    public record FillTarget(Slot slot, int requiredCount, java.util.function.Predicate<ItemStack> accepts) {}

    /** Read-only simulation of a complete fill, including shared ingredients and item components. */
    public static boolean canFill(Inventory inventory, List<FillTarget> targets, boolean creative) {
        int[] available = java.util.stream.IntStream.range(0, 36).map(i -> inventory.getItem(i).getCount()).toArray();
        boolean missingAnything = false;
        for (var target : targets) {
            ItemStack kind = target.slot().getItem();
            if (!kind.isEmpty() && !target.accepts().test(kind)) return false;
            int missing = target.requiredCount() - kind.getCount();
            if (missing <= 0) continue;
            missingAnything = true;
            if (creative) continue;
            for (int i = 0; i < available.length && missing > 0; i++) {
                ItemStack source = inventory.getItem(i);
                if (available[i] <= 0 || !target.accepts().test(source) || !target.slot().mayPlace(source)
                        || (!kind.isEmpty() && !ItemStack.isSameItemSameComponents(kind, source))) continue;
                if (target.requiredCount() > target.slot().getMaxStackSize(source)) continue;
                if (kind.isEmpty()) kind = source;
                int used = Math.min(missing, available[i]);
                available[i] -= used;
                missing -= used;
            }
            if (missing > 0) return false;
        }
        return creative || missingAnything;
    }

    public static void distributeRequired(ItemStack source, List<Target> targets) {
        // One item per slot per pass also spreads short stacks across repeated ingredients.
        while (!source.isEmpty()) {
            boolean moved = false;
            for (var target : targets) {
                Slot slot = target.slot();
                ItemStack existing = slot.getItem();
                if (!canInsert(source, slot) || existing.getCount() >= target.requiredCount()) continue;
                insertOne(source, slot);
                moved = true;
                if (source.isEmpty()) break;
            }
            if (!moved) break;
        }
    }

    public static void distributeExcess(ItemStack source, List<Target> targets) {
        while (!source.isEmpty()) {
            Target next = null;
            double lowest = Double.POSITIVE_INFINITY;
            for (var target : targets) {
                if (target.requiredCount() <= 0 || !canInsert(source, target.slot())) continue;
                double batches = target.slot().getItem().getCount() / (double) target.requiredCount();
                if (batches < lowest) { lowest = batches; next = target; }
            }
            if (next == null) break;
            insertOne(source, next.slot());
        }
    }

    private static boolean canInsert(ItemStack source, Slot slot) {
        ItemStack existing = slot.getItem();
        return slot.isActive() && slot.mayPlace(source)
                && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, source))
                && existing.getCount() < slot.getMaxStackSize(source);
    }

    private static void insertOne(ItemStack source, Slot slot) {
        ItemStack existing = slot.getItem();
        if (existing.isEmpty()) slot.setByPlayer(source.split(1));
        else { existing.grow(1); source.shrink(1); }
        slot.setChanged();
    }

    private RecipeSlotTransfer() {}
}
