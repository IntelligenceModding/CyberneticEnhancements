package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;

public final class RipperStationBlockEntity extends BlockEntity {
    public RipperStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIPPER_STATION.get(), pos, state);
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            var section = chunk.getSection(index);
            if (!section.maybeHas(state -> state.is(ModBlocks.RIPPER_STATION.get()))) {
                continue;
            }
            int sectionY = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (!section.getBlockState(x, y, z).is(ModBlocks.RIPPER_STATION.get())) {
                            continue;
                        }
                        pos.set(chunk.getPos().getMinBlockX() + x, sectionY + y, chunk.getPos().getMinBlockZ() + z);
                        if (chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) == null) {
                            chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
                            chunk.setUnsaved(true);
                        }
                    }
                }
            }
        }
    }
}
