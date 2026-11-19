package de.artemis.cyberneticenhancements.common.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Keeps inventory synchronization and automatic transfers without a visible or clickable slot. */
public final class HiddenPlayerInventorySlot extends Slot {
    public HiddenPlayerInventorySlot(Inventory inventory, int index) {
        super(inventory, index, -10000, -10000);
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
