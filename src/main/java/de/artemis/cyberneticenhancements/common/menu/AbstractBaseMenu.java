package de.artemis.cyberneticenhancements.common.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public abstract class AbstractBaseMenu extends AbstractContainerMenu {
    public static final int SLOT_SPACING = 18;

    protected AbstractBaseMenu(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    protected void addPlayerInventorySlots(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * SLOT_SPACING, top + row * SLOT_SPACING));
            }
        }
    }

    protected void addPlayerHotbarSlots(Inventory inventory, int left, int top) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(inventory, slot, left + slot * SLOT_SPACING, top));
        }
    }

    protected void addHiddenPlayerInventorySlots(Inventory inventory) {
        for (int index = 9; index < 36; index++) addSlot(new HiddenPlayerInventorySlot(inventory, index));
        for (int index = 0; index < 9; index++) addSlot(new HiddenPlayerInventorySlot(inventory, index));
    }

    protected int findSlotIndexForContainerSlot(int machineSlotCount, int containerSlot) {
        for (int slotIndex = 0; slotIndex < machineSlotCount; slotIndex++) {
            Slot slot = this.slots.get(slotIndex);
            if (slot instanceof SlotItemHandler slotItemHandler && slotItemHandler.getContainerSlot() == containerSlot) {
                return slotIndex;
            }
        }
        return -1;
    }

    protected boolean moveToContainerSlot(ItemStack sourceStack, int machineSlotCount, int containerSlot) {
        int menuSlotIndex = findSlotIndexForContainerSlot(machineSlotCount, containerSlot);
        return menuSlotIndex >= 0 && this.moveItemStackTo(sourceStack, menuSlotIndex, menuSlotIndex + 1, false);
    }
}
