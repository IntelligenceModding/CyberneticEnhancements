package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.quest.QuestRewardDeliveries;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public final class DropoffPointBlockEntity extends IronQuestCrateBlockEntity {
    private UUID owner;
    private boolean unreadDelivery;

    public DropoffPointBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.DROPOFF_POINT.get(), pos, state); }

    public UUID owner() { return owner; }

    public boolean hasUnreadDelivery() { return unreadDelivery; }

    public void markDeliveryArrived() { setUnreadDelivery(true); }
    public void acknowledgeDelivery() { setUnreadDelivery(false); }

    private void setUnreadDelivery(boolean unread) {
        if (unreadDelivery == unread) return;
        unreadDelivery = unread;
        syncDeliveryState();
    }

    private void syncDeliveryState() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    @Override
    public void startOpen(Player player) {
        super.startOpen(player);
        if (!level.isClientSide() && !player.isSpectator() && player.getUUID().equals(owner)) setUnreadDelivery(false);
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);
        if (!level.isClientSide() && !player.isSpectator() && player.getUUID().equals(owner)) setUnreadDelivery(false);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent openingSound(boolean open) { return open ? net.minecraft.sounds.SoundEvents.BARREL_OPEN : net.minecraft.sounds.SoundEvents.BARREL_CLOSE; }

    public boolean bind(ServerPlayer player) {
        if (owner != null && !owner.equals(player.getUUID())) return false;
        owner = player.getUUID();
        syncDeliveryState();
        var deliveries = QuestRewardDeliveries.get(player.server);
        boolean firstPlacement = !deliveries.hasPlacedDropoff(owner);
        if (deliveries.bind(owner, level.dimension(), worldPosition)) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.dropoff.bound"), true);
        }
        if (firstPlacement) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.capture(player));
            player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 1.2F);
        }
        return true;
    }

    @Override
    public boolean canOpen(Player player) { return owner != null && owner.equals(player.getUUID()) && super.canOpen(player); }

    @Override
    protected Component getDefaultName() { return Component.translatable("block.cyberneticenhancements.dropoff_point"); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("UnreadDelivery", unreadDelivery);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        unreadDelivery = tag.getBoolean("UnreadDelivery");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("UnreadDelivery", unreadDelivery);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

}
