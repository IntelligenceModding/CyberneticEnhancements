package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.block.IronQuestCrateBlock;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class IronQuestCrateBlockEntity extends RandomizableContainerBlockEntity {
    private String questId = "";
    public String questId() { return questId; }
    public boolean bindQuest(String id) {
        if (!questId.isEmpty()) return false;
        questId = id;
        setChanged();
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel server)
            server.getServer().execute(() -> de.artemis.cyberneticenhancements.common.quest.QuestRecovery.cleanupRetiredSite(server, worldPosition));
    }
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) { setOpen(level, pos, state, true); }
        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) { setOpen(level, pos, state, false); }
        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {}
        @Override
        protected boolean isOwnContainer(Player player) { return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == IronQuestCrateBlockEntity.this; }
    };

    public IronQuestCrateBlockEntity(BlockPos pos, BlockState state) { this(ModBlockEntities.IRON_QUEST_CRATE.get(), pos, state); }

    protected IronQuestCrateBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    private void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(IronQuestCrateBlock.OPEN, open), 3);
        level.playSound(null, pos, openingSound(open), SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.1F);
    }

    protected net.minecraft.sounds.SoundEvent openingSound(boolean open) { return open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE; }

    @Override
    public int getContainerSize() { return 27; }
    @Override
    protected NonNullList<ItemStack> getItems() { return items; }
    @Override
    protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override
    protected Component getDefaultName() { return Component.translatable("block.cyberneticenhancements.iron_quest_crate"); }
    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return ChestMenu.threeRows(id, inventory, this); }
    @Override
    public void startOpen(Player player) {
        if (!isRemoved() && !player.isSpectator()) openers.incrementOpeners(player, level, worldPosition, getBlockState());
    }
    @Override
    public void stopOpen(Player player) {
        if (!isRemoved() && !player.isSpectator()) openers.decrementOpeners(player, level, worldPosition, getBlockState());
    }
    public void recheckOpen() {
        if (!isRemoved()) openers.recheckOpeners(level, worldPosition, getBlockState());
    }
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("QuestContract", questId);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, registries);
    }
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        questId = tag.getString("QuestContract");
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items, registries);
    }
}
