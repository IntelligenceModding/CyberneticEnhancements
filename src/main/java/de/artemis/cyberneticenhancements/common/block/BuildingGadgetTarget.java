package de.artemis.cyberneticenhancements.common.block;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/** A building block the Building Gadget can change; the gadget hands the click over and the block decides what it does. */
public interface BuildingGadgetTarget {
    InteractionResult useBuildingGadget(UseOnContext context);
}
