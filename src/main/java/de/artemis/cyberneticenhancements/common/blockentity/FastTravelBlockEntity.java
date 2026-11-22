package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Only carries the terminal's hologram: what it shows is drawn by its renderer. */
public final class FastTravelBlockEntity extends BlockEntity {
    public FastTravelBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FAST_TRAVEL.get(), pos, state); }
}
