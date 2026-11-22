package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareRecycleHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareRecyclePlan;
import de.artemis.cyberneticenhancements.common.journal.PlayerJournal;
import de.artemis.cyberneticenhancements.common.registry.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/** Persistent recycling. A batch commits only when every recovered item fits. */
public final class RecyclerStationBlockEntity extends UpgradeableStationBlockEntity implements WorldlyContainer {
    public static final int INPUT_SLOT = 0, OUTPUT_START = 1, OUTPUT_COUNT = 25, SLOT_COUNT = OUTPUT_START + OUTPUT_COUNT;
    public static final int RECYCLE_TICKS = 60;
    private static final int[] OUTPUT_SLOTS = IntStream.range(OUTPUT_START, SLOT_COUNT).toArray();
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private ItemStack runningInput = ItemStack.EMPTY;
    private NonNullList<ItemStack> legacyOverflow = NonNullList.create();
    private UUID journalOwner;
    private boolean wasPowered;
    private boolean singleCraft;
    public int progress;

    public RecyclerStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECYCLER_STATION.get(), pos, state, 1);
    }
    public CyberwareRecyclePlan plan() {
        return level == null ? CyberwareRecyclePlan.empty() : CyberwareRecycleHelper.resolve(level, getItem(INPUT_SLOT));
    }
    public boolean working() { return !runningInput.isEmpty(); }
    public boolean recyclable(ItemStack stack) {
        return level != null && !CyberwareRecycleHelper.resolve(level, stack).outputs().isEmpty();
    }
    public boolean canStart() { return !working() && mergedOutput(plan().outputs()) != null; }
    public boolean start(ServerPlayer player) {
        if (!canStart()) return false;
        journalOwner = player == null ? null : player.getUUID();
        singleCraft = false;
        runningInput = getItem(INPUT_SLOT).copyWithCount(1);
        progress = 0;
        setChanged();
        return true;
    }
    public void stop() {
        runningInput = ItemStack.EMPTY;
        singleCraft = false;
        progress = 0;
        setChanged();
    }
    public void updateRedstone() {
        if (level == null || level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered != wasPowered) {
            wasPowered = powered;
            if (powered && start(null)) singleCraft = true;
            setChanged();
        }
    }
    public static void tick(Level level, BlockPos pos, BlockState state, RecyclerStationBlockEntity station) {
        // Preserve surplus items from the earlier 81-output layout rather than truncating its saved inventory.
        if (!station.legacyOverflow.isEmpty()) {
            var merged = station.mergedOutput(station.legacyOverflow);
            if (merged != null) {
                for (int i = 0; i < OUTPUT_COUNT; i++) station.items.set(OUTPUT_START + i, merged.get(i));
            } else for (ItemStack stack : station.legacyOverflow)
                Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, stack);
            station.legacyOverflow.clear();
            station.setChanged();
        }
        station.updateRedstone();
        if (!station.working()) return;
        // Condition and installed components must still match the item the cycle started with.
        if (!ItemStack.matches(station.runningInput, station.getItem(INPUT_SLOT).copyWithCount(1))) {
            station.stop();
            return;
        }
        station.progress += 1 + de.artemis.cyberneticenhancements.common.skills.SkillEffects.extraProgress(level, station.journalOwner);
        if (station.progress < RECYCLE_TICKS) { station.setChanged(); return; }
        var merged = station.mergedOutput(station.plan().outputs());
        if (merged == null) { station.stop(); return; }
        var boosted = station.mergedOutput(de.artemis.cyberneticenhancements.common.skills.SkillEffects.salvage(level, station.journalOwner, station.plan().outputs()));
        if (boosted != null) merged = boosted;
        ItemStack recycled = station.runningInput;
        station.getItem(INPUT_SLOT).shrink(1);
        for (int i = 0; i < OUTPUT_COUNT; i++) station.items.set(OUTPUT_START + i, merged.get(i));
        if (station.journalOwner != null && level instanceof ServerLevel server)
            PlayerJournal.recordFor(server.getServer(), station.journalOwner, level.dimension().location().toString(), pos,
                    "workshop", "recycled", 1, recycled.getHoverName());
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, .4F, 1F);
        station.runningInput = ItemStack.EMPTY;
        station.progress = 0;
        // GUI starts repeat; a redstone edge processes just one input.
        if (!station.singleCraft && station.mergedOutput(station.plan().outputs()) != null)
            station.runningInput = station.getItem(INPUT_SLOT).copyWithCount(1);
        if (!station.working()) station.singleCraft = false;
        station.setChanged();
    }
    /** Simulate the entire merge first, preventing partial batches and input loss. */
    private List<ItemStack> mergedOutput(List<ItemStack> outputs) {
        if (outputs.isEmpty()) return null;
        var merged = new java.util.ArrayList<ItemStack>(OUTPUT_COUNT);
        for (int i = OUTPUT_START; i < SLOT_COUNT; i++) merged.add(items.get(i).copy());
        for (ItemStack output : outputs) {
            ItemStack remaining = output.copy();
            for (ItemStack existing : merged) {
                if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue;
                int count = Math.min(remaining.getCount(), Math.max(0, existing.getMaxStackSize() - existing.getCount()));
                existing.grow(count);
                remaining.shrink(count);
                if (remaining.isEmpty()) break;
            }
            for (int i = 0; !remaining.isEmpty() && i < merged.size(); i++) if (merged.get(i).isEmpty()) {
                int count = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                merged.set(i, remaining.copyWithCount(count));
                remaining.shrink(count);
            }
            if (!remaining.isEmpty()) return null;
        }
        return merged;
    }
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        if (!legacyOverflow.isEmpty()) {
            var overflow = new CompoundTag();
            ContainerHelper.saveAllItems(overflow, legacyOverflow, lookup);
            tag.put("LegacyRecovery", overflow);
        }
        if (!runningInput.isEmpty()) tag.put("RunningInput", runningInput.save(lookup));
        tag.putInt("Progress", progress);
        tag.putBoolean("WasPowered", wasPowered);
        tag.putBoolean("SingleCraft", singleCraft);
        if (journalOwner != null) tag.putUUID("JournalOwner", journalOwner);
    }
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        var previous = NonNullList.withSize(82, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, previous, lookup);
        for (int i = 0; i < SLOT_COUNT; i++) items.set(i, previous.get(i));
        legacyOverflow = NonNullList.create();
        for (int i = SLOT_COUNT; i < previous.size(); i++) if (!previous.get(i).isEmpty()) legacyOverflow.add(previous.get(i));
        var overflow = NonNullList.withSize(82, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("LegacyRecovery"), overflow, lookup);
        for (ItemStack stack : overflow) if (!stack.isEmpty()) legacyOverflow.add(stack);
        runningInput = ItemStack.parseOptional(lookup, tag.getCompound("RunningInput"));
        progress = Math.clamp(tag.getInt("Progress"), 0, RECYCLE_TICKS - 1);
        wasPowered = tag.getBoolean("WasPowered");
        singleCraft = tag.getBoolean("SingleCraft");
        journalOwner = tag.hasUUID("JournalOwner") ? tag.getUUID("JournalOwner") : null;
    }
    public void dropLegacyRecovery() {
        if (level == null || level.isClientSide) return;
        for (ItemStack stack : legacyOverflow) Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5, stack);
        legacyOverflow.clear();
    }
    public int getContainerSize() { return SLOT_COUNT; }
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    public ItemStack getItem(int slot) { return items.get(slot); }
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        setChanged();
        return result;
    }
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    public void clearContent() { items.clear(); legacyOverflow.clear(); stop(); }
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64;
    }
    public boolean canPlaceItem(int slot, ItemStack stack) { return slot == INPUT_SLOT && recyclable(stack); }
    public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? OUTPUT_SLOTS.clone() : new int[]{INPUT_SLOT}; }
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= OUTPUT_START && slot < SLOT_COUNT; }
    public boolean canTakeItem(Container destination, int slot, ItemStack stack) { return slot != INPUT_SLOT || !working(); }
}
