package de.artemis.cyberneticenhancements.common.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Top/side access inserts implants and required materials; bottom access retrieves the implant after a service cycle completes. */
public final class TechStationAutomation implements IItemHandler {
    private final TechStationBlockEntity station;
    @Nullable private final Direction side;
    public TechStationAutomation(TechStationBlockEntity station, @Nullable Direction side) { this.station = station; this.side = side; }
    public int getSlots() { return side == Direction.DOWN ? 1 : 3; }
    private int inventorySlot(int slot) {
        if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException(slot);
        return side == Direction.DOWN ? TechStationBlockEntity.IMPLANT_SLOT : slot;
    }
    public ItemStack getStackInSlot(int slot) { return station.getItem(inventorySlot(slot)); }
    public int getSlotLimit(int slot) { return inventorySlot(slot) == 0 ? 1 : 64; }
    public boolean isItemValid(int slot, ItemStack stack) { return side != Direction.DOWN && station.canPlaceItem(inventorySlot(slot), stack); }
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int target = inventorySlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (side == Direction.DOWN || !station.canPlaceItem(target, stack)) return stack;
        ItemStack current = station.getItem(target);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack;
        int count = Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()) - current.getCount());
        if (count <= 0) return stack;
        if (!simulate) station.setItem(target, stack.copyWithCount(current.getCount() + count));
        return stack.copyWithCount(stack.getCount() - count);
    }
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int target = inventorySlot(slot);
        if ((side != Direction.DOWN && side != null) || !station.canExtractImplant(target) || amount <= 0) return ItemStack.EMPTY;
        if (simulate) return station.getItem(target).copyWithCount(Math.min(amount, station.getItem(target).getCount()));
        return station.removeItem(target, amount);
    }
}
