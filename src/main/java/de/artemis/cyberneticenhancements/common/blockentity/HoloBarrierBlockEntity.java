package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Carries nothing: it only gives the barrier's renderer a place to draw from. */
public final class HoloBarrierBlockEntity extends BlockEntity {
    public HoloBarrierBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.HOLO_BARRIER.get(), pos, state); }
}
