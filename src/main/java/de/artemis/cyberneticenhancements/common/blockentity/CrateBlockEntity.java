package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.menu.CrateMenu;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The loot of a crate in a generated structure. It has exactly as many rows of slots as the loot put into it needs, items can be
 * taken out but never put in, and a crate that still holds a quest cargo tells the client so it can show a "!" above it.
 */
public class CrateBlockEntity extends BaseContainerBlockEntity {
    public static final int MAX_ROWS = 6;
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private boolean marked;

    public CrateBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.CRATE.get(), pos, state); }

    /** Replaces the contents; the crate gets as many rows as the stacks need, and they land in random slots. */
    public void fill(List<ItemStack> stacks, RandomSource random) {
        List<ItemStack> kept = new ArrayList<>(stacks.stream().filter(stack -> !stack.isEmpty()).toList());
        int rows = Math.clamp((kept.size() + 8) / 9, 1, MAX_ROWS);
        items = NonNullList.withSize(rows * 9, ItemStack.EMPTY);
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < items.size(); slot++) slots.add(slot);
        for (ItemStack stack : kept) {
            if (slots.isEmpty()) break;
            items.set(slots.remove(random.nextInt(slots.size())), stack);
        }
        syncMark();
    }

    /** Puts a stack into a free slot, or into a new row when the crate is full. */
    public void insertLoot(ItemStack stack) {
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) continue;
            items.set(slot, stack);
            syncMark();
            return;
        }
        if (items.size() / 9 >= MAX_ROWS) return;
        var grown = NonNullList.withSize(items.size() + 9, ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) grown.set(slot, items.get(slot));
        grown.set(items.size(), stack);
        items = grown;
        syncMark();
    }

    /** Whether the crate holds a quest cargo, so it should show the "!" marker. */
    public boolean marked() { return marked; }

    private boolean holdsCargo() { return items.stream().anyMatch(stack -> stack.is(ModItems.LOST_CARGO.get())); }

    private void syncMark() {
        boolean now = holdsCargo();
        boolean changed = now != marked;
        marked = now;
        setChanged();
        if (changed && level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide && holdsCargo() != marked) {
            marked = !marked;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return false; }

    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> items) { this.items = items; syncMark(); }

    @Override
    protected Component getDefaultName() { return Component.translatable("block.cyberneticenhancements.crate"); }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new CrateMenu(id, inventory, this); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Rows", items.size() / 9);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(Math.clamp(tag.getInt("Rows"), 1, MAX_ROWS) * 9, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        marked = tag.contains("Marked") ? tag.getBoolean("Marked") : holdsCargo(); // The client only gets the mark, not the items.
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putBoolean("Marked", marked);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { marked = tag.getBoolean("Marked"); }
}
