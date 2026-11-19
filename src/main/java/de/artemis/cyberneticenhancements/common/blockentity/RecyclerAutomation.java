package de.artemis.cyberneticenhancements.common.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Top/sides insert input; bottom extracts recovery. Unsided access exposes both. */
public final class RecyclerAutomation implements IItemHandler {
    private final RecyclerStationBlockEntity station;
    @Nullable private final Direction side;
    public RecyclerAutomation(RecyclerStationBlockEntity station, @Nullable Direction side) {
        this.station = station;
        this.side = side;
    }
    public int getSlots() { return side == Direction.DOWN ? RecyclerStationBlockEntity.OUTPUT_COUNT : side == null ? RecyclerStationBlockEntity.SLOT_COUNT : 1; }
    private int inventorySlot(int slot) {
        if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException(slot);
        return side == Direction.DOWN ? slot + 1 : slot;
    }
    public ItemStack getStackInSlot(int slot) { return station.getItem(inventorySlot(slot)); }
    public int getSlotLimit(int slot) { inventorySlot(slot); return 64; }
    public boolean isItemValid(int slot, ItemStack stack) { return station.canPlaceItem(inventorySlot(slot), stack); }
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int target = inventorySlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!station.canPlaceItem(target, stack)) return stack;
        ItemStack current = station.getItem(target);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack;
        int moved = Math.min(stack.getCount(), stack.getMaxStackSize() - current.getCount());
        if (moved <= 0) return stack;
        if (!simulate) station.setItem(target, stack.copyWithCount(current.getCount() + moved));
        return stack.copyWithCount(stack.getCount() - moved);
    }
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int target = inventorySlot(slot);
        if (target == 0 || amount <= 0) return ItemStack.EMPTY;
        if (simulate) return station.getItem(target).copyWithCount(Math.min(amount, station.getItem(target).getCount()));
        return station.removeItem(target, amount);
    }
}
