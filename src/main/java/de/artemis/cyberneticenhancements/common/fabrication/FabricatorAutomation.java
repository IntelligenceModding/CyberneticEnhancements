package de.artemis.cyberneticenhancements.common.fabrication;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Sided pipe access with stable slot indices and balanced recipe inputs. */
public final class FabricatorAutomation implements IItemHandler {
    private final FabricatorBlockEntity station;
    @Nullable private final Direction side;

    public FabricatorAutomation(FabricatorBlockEntity station, @Nullable Direction side) {
        this.station = station;
        this.side = side;
    }

    public int getSlots() { return side == Direction.DOWN ? 1 : side == null ? 26 : 25; }

    private int inventorySlot(int slot) {
        if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException(slot);
        return side == Direction.DOWN || slot == 25 ? 26 : slot;
    }

    public ItemStack getStackInSlot(int slot) { return station.getItem(inventorySlot(slot)); }
    public int getSlotLimit(int slot) { inventorySlot(slot); return 64; }
    public boolean isItemValid(int slot, ItemStack stack) { return station.canPlaceItem(inventorySlot(slot), stack); }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int target = inventorySlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int moved = Math.min(stack.getCount(), station.automationInsertLimit(target, stack));
        if (moved <= 0) return stack;
        if (!simulate) {
            ItemStack current = station.getItem(target);
            station.setItem(target, stack.copyWithCount(current.getCount() + moved));
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int target = inventorySlot(slot);
        if (target != 26 || amount <= 0) return ItemStack.EMPTY;
        if (simulate) return station.getItem(target).copyWithCount(Math.min(amount, station.getItem(target).getCount()));
        return station.removeItem(target, amount);
    }
}
