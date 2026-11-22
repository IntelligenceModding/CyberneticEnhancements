package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The chest layout, with every crate slot changed so that items can be taken out but never put in. */
public final class CrateMenu extends ChestMenu {
    public CrateMenu(int id, Inventory inventory, Container crate) {
        super(ModMenuTypes.CRATE.get(), id, inventory, crate, crate.getContainerSize() / 9);
        for (int i = 0; i < crate.getContainerSize(); i++) {
            Slot old = slots.get(i);
            Slot output = new Slot(crate, old.getContainerSlot(), old.x, old.y) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
            };
            output.index = i;
            slots.set(i, output);
        }
    }

    public CrateMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(Math.clamp(buffer.readVarInt(), 1, 6) * 9));
    }
}
