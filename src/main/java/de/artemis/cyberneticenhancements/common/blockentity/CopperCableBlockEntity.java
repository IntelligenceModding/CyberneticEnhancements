package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.block.CopperCableBlock;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Which joins of the wires in this space the Building Gadget has cut, one bit for each side and direction. */
public class CopperCableBlockEntity extends BlockEntity {
    private volatile long cut;

    public CopperCableBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.COPPER_CABLE.get(), pos, state); }

    public boolean isCut(Direction side, Direction toward) { return (cut & CopperCableBlock.bit(side, toward)) != 0; }

    public void setCut(Direction side, Direction toward, boolean value) {
        long bit = CopperCableBlock.bit(side, toward);
        cut = value ? cut | bit : cut & ~bit;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Cut", cut);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cut = tag.getLong("Cut");
        // On the client the wires have to be drawn again, and so do the ones round them that reach into this space.
        if (level != null && level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
