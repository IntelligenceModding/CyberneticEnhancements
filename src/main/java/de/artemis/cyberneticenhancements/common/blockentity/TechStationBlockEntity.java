package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.journal.PlayerJournal;
import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

/** Persistent, atomic in-place upgrade and repair cycles share inventory, timing and automation. */
public final class TechStationBlockEntity extends UpgradeableStationBlockEntity implements WorldlyContainer {
    public static final int IMPLANT_SLOT = 0, SLOT_COUNT = 3, SERVICE_TICKS = 80;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private ItemStack runningInput = ItemStack.EMPTY;
    private UUID journalOwner;
    private boolean wasPowered;
    private boolean completed;
    private NonNullList<ItemStack> legacyRecovery = NonNullList.create();
    public int progress;

    public TechStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TECH_STATION.get(), pos, blockState, 2);
    }
    public boolean isUpgradeStation() { return getBlockState().is(ModBlocks.UPGRADE_STATION.get()); }
    public boolean isServiceStation() { return isUpgradeStation() || getBlockState().is(ModBlocks.TECH_STATION.get()); }
    public CyberwareServicePlan plan() {
        return isUpgradeStation() ? CyberwareServiceHelper.getUpgradePlan(items.get(IMPLANT_SLOT))
                : CyberwareServiceHelper.getRepairPlan(items.get(IMPLANT_SLOT));
    }
    public boolean working() { return !runningInput.isEmpty(); }
    public boolean canStart() {
        return isServiceStation() && !working()
                && CyberwareServiceHelper.matchesMaterials(plan(), items.get(1), items.get(2));
    }
    public boolean start(ServerPlayer player) {
        if (!canStart()) return false;
        runningInput = items.get(IMPLANT_SLOT).copyWithCount(1);
        journalOwner = player == null ? null : player.getUUID();
        progress = 0;
        setChanged();
        return true;
    }
    public void stop() { runningInput = ItemStack.EMPTY; progress = 0; setChanged(); }
    public void updateRedstone() {
        if (level == null || level.isClientSide || !isServiceStation()) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (wasPowered != powered) {
            wasPowered = powered;
            if (powered) start(null); // Busy/invalid pulses are consumed, never queued.
            setChanged();
        }
    }
    public static void tick(Level level, BlockPos pos, BlockState state, TechStationBlockEntity station) {
        if (!station.legacyRecovery.isEmpty()) {
            for (ItemStack stack : station.legacyRecovery) {
                if (station.items.get(IMPLANT_SLOT).isEmpty()) {
                    station.items.set(IMPLANT_SLOT, stack);
                    station.completed = true;
                } else Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, stack);
            }
            station.legacyRecovery.clear();
            station.setChanged();
        }
        station.updateRedstone();
        if (!station.working()) return;
        var plan = station.plan();
        if (!ItemStack.matches(station.runningInput, station.items.get(IMPLANT_SLOT).copyWithCount(1))
                || !CyberwareServiceHelper.matchesMaterials(plan, station.items.get(1), station.items.get(2))) {
            station.stop();
            return;
        }
        if (++station.progress < SERVICE_TICKS) { station.setChanged(); return; }
        // Validate the whole plan before consuming either the implant or any material.
        for (var requirement : plan.materials()) {
            int remaining = requirement.count();
            for (int slot = 1; slot <= 2 && remaining > 0; slot++) {
                ItemStack stack = station.items.get(slot);
                if (!ItemStack.isSameItemSameComponents(stack, requirement.stack())) continue;
                int count = Math.min(remaining, stack.getCount());
                stack.shrink(count);
                remaining -= count;
            }
        }
        station.items.set(IMPLANT_SLOT, plan.output().copy());
        station.completed = true;
        if (station.journalOwner != null && level instanceof ServerLevel server)
            PlayerJournal.recordFor(server.getServer(), station.journalOwner, level.dimension().location().toString(), pos,
                    "workshop", station.isUpgradeStation() ? "upgraded" : "repaired", 1, plan.output().getHoverName());
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, .4F, station.isUpgradeStation() ? 1.2F : 1F);
        station.stop();
    }
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        tag.putBoolean("Completed", completed);
        if (!legacyRecovery.isEmpty()) {
            var legacy = new CompoundTag();
            ContainerHelper.saveAllItems(legacy, legacyRecovery, lookup);
            tag.put("LegacyRecovery", legacy);
        }
        if (!runningInput.isEmpty()) tag.put("RunningInput", runningInput.save(lookup));
        tag.putInt("Progress", progress);
        tag.putBoolean("WasPowered", wasPowered);
        if (journalOwner != null) tag.putUUID("JournalOwner", journalOwner);
    }
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        var previous = NonNullList.withSize(4, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, previous, lookup);
        for (int i = 0; i < SLOT_COUNT; i++) items.set(i, previous.get(i));
        legacyRecovery = NonNullList.create();
        if (!previous.get(3).isEmpty()) legacyRecovery.add(previous.get(3));
        var legacy = NonNullList.withSize(4, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("LegacyRecovery"), legacy, lookup);
        for (ItemStack stack : legacy) if (!stack.isEmpty()) legacyRecovery.add(stack);
        completed = tag.getBoolean("Completed");
        runningInput = ItemStack.parseOptional(lookup, tag.getCompound("RunningInput"));
        progress = Math.clamp(tag.getInt("Progress"), 0, SERVICE_TICKS - 1);
        wasPowered = tag.getBoolean("WasPowered");
        journalOwner = tag.hasUUID("JournalOwner") ? tag.getUUID("JournalOwner") : null;
    }
    public int getContainerSize() { return SLOT_COUNT; }
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    public ItemStack getItem(int slot) { return items.get(slot); }
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (slot == IMPLANT_SLOT && items.get(slot).isEmpty()) completed = false;
        setChanged();
        return result;
    }
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == IMPLANT_SLOT) completed = false;
        return ContainerHelper.takeItem(items, slot);
    }
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (slot == IMPLANT_SLOT) completed = false;
        setChanged();
    }
    public void clearContent() { items.clear(); completed = false; legacyRecovery.clear(); stop(); }
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64;
    }
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!isServiceStation() || stack.isEmpty()) return false;
        if (slot == IMPLANT_SLOT) return stack.getItem() instanceof CyberwareItem && (isUpgradeStation()
                ? CyberwareUpgradeHelper.canUpgrade(stack) : CyberwareServiceHelper.getRepairPlan(stack).isAvailable());
        var materials = plan().materials();
        return slot >= 1 && slot <= materials.size() && ItemStack.isSameItemSameComponents(stack, materials.get(slot - 1).stack());
    }
    public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{IMPLANT_SLOT} : new int[]{0, 1, 2}; }
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack) && items.get(slot).getCount() < (slot == IMPLANT_SLOT ? 1 : stack.getMaxStackSize());
    }
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return canExtractImplant(slot); }
    public boolean canTakeItem(Container destination, int slot, ItemStack stack) { return canExtractImplant(slot); }
    public boolean canExtractImplant(int slot) { return slot == IMPLANT_SLOT && completed && !working(); }
    public void dropLegacyRecovery() {
        if (level == null || level.isClientSide) return;
        for (ItemStack stack : legacyRecovery) Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5, stack);
        legacyRecovery.clear();
    }
}
