package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareRecycleHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareRecyclePlan;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class RecyclerStationMenu extends AbstractBaseMenu implements NamedBlockMenu {
    private static final int INPUT_SLOT = 0;
    private static final int RESULT_SLOT_START = 1;
    private static final int RESULT_SLOT_COUNT = RecyclerStationLayout.RESULT_SLOT_COUNT;
    private static final int MACHINE_SLOT_COUNT = RESULT_SLOT_START + RESULT_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;

    private final Container inputInventory;
    private final Container resultInventory;
    private final Player player;
    private final RecyclerStationBlockEntity stationBlockEntity;
    private final boolean clientSide;
    private final BlockPos blockPos;
    private final String blockDisplayName;
    private final Level level;
    private int supportedTierClient = CyberwareTier.TIER_1.ordinal();

    public RecyclerStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public RecyclerStationMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenuTypes.RECYCLER_STATION.get(), containerId);
        this.player = playerInventory.player;
        this.level = player.level();
        this.clientSide = this.level.isClientSide();
        this.blockPos = blockPos.immutable();
        this.blockDisplayName = ModBlocks.RECYCLER_STATION.get().getName().getString();
        this.stationBlockEntity = this.level.getBlockEntity(blockPos) instanceof RecyclerStationBlockEntity blockEntity ? blockEntity : null;
        this.inputInventory = new SimpleContainer(1);
        this.resultInventory = new SimpleContainer(RESULT_SLOT_COUNT);

        if (!clientSide && stationBlockEntity != null) {
            PlayerStationUpgradeData.migrateRecyclerTier(player, stationBlockEntity.getSupportedTierOrdinal(RecyclerStationBlockEntity.INPUT_SLOT));
        }

        addStationSlots();
        addPlayerInventorySlots(playerInventory, RecyclerStationLayout.PLAYER_INVENTORY_X, RecyclerStationLayout.PLAYER_INVENTORY_Y);
        addPlayerHotbarSlots(playerInventory, RecyclerStationLayout.PLAYER_INVENTORY_X, RecyclerStationLayout.PLAYER_HOTBAR_Y);
        addTierSlot();
        broadcastChanges();
    }

    private void addStationSlots() {
        this.addSlot(new Slot(inputInventory, INPUT_SLOT, RecyclerStationLayout.INPUT_X, RecyclerStationLayout.INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isRecyclable(stack);
            }
        });

        for (int slotIndex = 0; slotIndex < RESULT_SLOT_COUNT; slotIndex++) {
            final int resultIndex = slotIndex;
            this.addSlot(new Slot(resultInventory, resultIndex, RecyclerStationLayout.RESULT_SLOT_X[resultIndex], RecyclerStationLayout.RESULT_SLOT_Y[resultIndex]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

            });
        }
    }

    private void addTierSlot() {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return !clientSide ? PlayerStationUpgradeData.getRecyclerTier(player).ordinal() : supportedTierClient;
            }

            @Override
            public void set(int value) {
                supportedTierClient = value;
            }
        });
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (clientSide || player != this.player || !stillValid(player)) return false;
        if (button == 1) return tryUpgradeSupportedTier();
        if (button != 0 || !canRecycle()) return false;

        List<ItemStack> outputs = getRecyclePlan().outputs();
        if (outputs.size() > RESULT_SLOT_COUNT) return false;
        inputInventory.removeItem(INPUT_SLOT, 1);
        for (int i = 0; i < outputs.size(); i++) resultInventory.setItem(i, outputs.get(i).copy());
        player.playNotifySound(net.minecraft.sounds.SoundEvents.GRINDSTONE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.0F);
        broadcastChanges();
        return true;
    }

    public boolean canRecycle() {
        return !hasPendingResultStacks() && !getRecyclePlan().outputs().isEmpty()
                && getRecyclePlan().outputs().size() <= RESULT_SLOT_COUNT
                && canAcceptTier(getInputStack(), getSupportedTier());
    }

    public net.minecraft.network.chat.Component status() {
        String key = hasPendingResultStacks() ? "collect_salvage" : getInputStack().isEmpty() ? "insert_scrap"
                : !canAcceptTier(getInputStack(), getSupportedTier()) ? "tier_locked"
                : !getRecyclePlan().isAvailable() ? "no_salvage" : "ready_recycle";
        return net.minecraft.network.chat.Component.translatable("screen.cyberneticenhancements.workshop." + key);
    }

    public boolean hasPendingResultStacks() {
        for (int slotIndex = 0; slotIndex < RESULT_SLOT_COUNT; slotIndex++) {
            if (!resultInventory.getItem(slotIndex).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(sourceStack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (isRecyclable(sourceStack)) {
            if (!this.moveItemStackTo(sourceStack, INPUT_SLOT, INPUT_SLOT + 1, false)
                    && !moveWithinPlayerInventory(index, sourceStack)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveWithinPlayerInventory(index, sourceStack)) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (sourceStack.getCount() == copiedStack.getCount()) {
            return ItemStack.EMPTY;
        }
        sourceSlot.onTake(player, sourceStack);
        return copiedStack;
    }

    private boolean moveWithinPlayerInventory(int index, ItemStack sourceStack) {
        int playerInventoryStart = MACHINE_SLOT_COUNT;
        int playerHotbarStart = playerInventoryStart + PLAYER_INVENTORY_SLOT_COUNT;
        if (index < playerHotbarStart) {
            return this.moveItemStackTo(sourceStack, playerHotbarStart, this.slots.size(), false);
        }
        return this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarStart, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, ModBlocks.RECYCLER_STATION.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, inputInventory);
        clearContainer(player, resultInventory);
    }

    @Override
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public String getBlockDisplayName() {
        return blockDisplayName;
    }

    public ItemStack getInputStack() {
        return inputInventory.getItem(INPUT_SLOT);
    }

    public CyberwareRecyclePlan getRecyclePlan() {
        return CyberwareRecycleHelper.resolve(level, getInputStack());
    }

    public List<ItemStack> getDisplayedResultStacks() {
        if (hasPendingResultStacks()) {
            List<ItemStack> displayed = new ArrayList<>(RESULT_SLOT_COUNT);
            for (int slotIndex = 0; slotIndex < RESULT_SLOT_COUNT; slotIndex++)
                displayed.add(resultInventory.getItem(slotIndex).copy());
            return displayed;
        }
        return getRecyclePlan().outputs();
    }

    private boolean isRecyclable(ItemStack stack) {
        return !stack.isEmpty()
                && level.getRecipeManager().getRecipeFor(
                ModRecipeTypes.RECYCLING.get(),
                new SingleRecipeInput(stack),
                level
        ).isPresent();
    }

    private boolean canAcceptTier(ItemStack stack, CyberwareTier supportedTier) {
        return StationSlotTierHelper.canAccept(stack, supportedTier);
    }

    private int countAccessiblePlayerItems(ItemStack required) {
        int total = 0;
        for (int slotIndex = MACHINE_SLOT_COUNT; slotIndex < this.slots.size(); slotIndex++) {
            ItemStack stack = this.slots.get(slotIndex).getItem();
            if (ItemStack.isSameItemSameComponents(stack, required)) {
                total += stack.getCount();
            }
        }

        ItemStack carried = getCarried();
        if (ItemStack.isSameItemSameComponents(carried, required)) {
            total += carried.getCount();
        }
        return total;
    }

    private boolean consumeAccessiblePlayerItem(ItemStack required) {
        for (int slotIndex = MACHINE_SLOT_COUNT; slotIndex < this.slots.size(); slotIndex++) {
            ItemStack stack = this.slots.get(slotIndex).getItem();
            if (!ItemStack.isSameItemSameComponents(stack, required)) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                this.slots.get(slotIndex).set(ItemStack.EMPTY);
            } else {
                this.slots.get(slotIndex).setChanged();
            }
            return true;
        }

        ItemStack carried = getCarried();
        if (ItemStack.isSameItemSameComponents(carried, required)) {
            carried.shrink(1);
            if (carried.isEmpty()) {
                setCarried(ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    public CyberwareTier getSupportedTier() {
        return !clientSide
                ? PlayerStationUpgradeData.getRecyclerTier(player)
                : RecyclerStationBlockEntity.getTierByOrdinal(supportedTierClient);
    }

    public CyberwareTier getNextSupportedTier() {
        CyberwareTier current = getSupportedTier();
        return current.next();
    }

    public boolean canUpgradeSupportedTier() {
        return getInputStack().isEmpty()
                && getSupportedTier() != CyberwareTier.TIER_5
                && !getRequiredUpgradeComponentStack().isEmpty()
                && (!clientSide || PlayerStationUpgradeData.getRecyclerTier(player) != CyberwareTier.TIER_5);
    }

    public ItemStack getRequiredUpgradeComponentStack() {
        return RecyclerStationBlockEntity.getRequiredUpgradeComponentStack(getSupportedTier());
    }

    public boolean hasRequiredUpgradeComponent() {
        ItemStack required = getRequiredUpgradeComponentStack();
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || countAccessiblePlayerItems(required) > 0);
    }

    public boolean tryUpgradeSupportedTier() {
        ItemStack required = getRequiredUpgradeComponentStack();
        if (!canUpgradeSupportedTier()
                || required.isEmpty()
                || (!hasCreativeUpgradeBypass() && !consumeAccessiblePlayerItem(required))
                || !PlayerStationUpgradeData.tryUpgradeRecyclerTier(player)) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    private boolean hasCreativeUpgradeBypass() {
        return player.getAbilities().instabuild;
    }
}
