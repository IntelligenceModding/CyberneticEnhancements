package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareServicePlan;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.blockentity.TechStationBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
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

public final class TechStationMenu extends AbstractBaseMenu implements NamedBlockMenu {
    private static final int REPAIR_INPUT_SLOT = 0;
    private static final int REPAIR_PRIMARY_SLOT = 1;
    private static final int REPAIR_SECONDARY_SLOT = 2;
    private static final int REPAIR_TERTIARY_SLOT = 3;
    private static final int UPGRADE_INPUT_SLOT = 4;
    private static final int UPGRADE_PRIMARY_SLOT = 5;
    private static final int UPGRADE_SECONDARY_SLOT = 6;
    private static final int REPAIR_RESULT_SLOT = 7;
    private static final int UPGRADE_RESULT_SLOT = 8;
    private static final int MACHINE_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;

    private static final int[] REPAIR_MATERIAL_SLOTS = {REPAIR_PRIMARY_SLOT, REPAIR_SECONDARY_SLOT, REPAIR_TERTIARY_SLOT};
    private static final int[] UPGRADE_MATERIAL_SLOTS = {UPGRADE_PRIMARY_SLOT, UPGRADE_SECONDARY_SLOT};

    private final Container serviceInventory;
    private final Container resultInventory;
    private final Player player;
    private final TechStationBlockEntity stationBlockEntity;
    private final boolean clientSide;
    private final BlockPos blockPos;
    private final String blockDisplayName;
    private int repairSupportedTierClient = CyberwareTier.TIER_1.ordinal();
    private int upgradeSupportedTierClient = CyberwareTier.TIER_1.ordinal();

    public TechStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public TechStationMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenuTypes.TECH_STATION.get(), containerId);
        this.player = playerInventory.player;
        this.blockPos = blockPos.immutable();
        this.blockDisplayName = ModBlocks.TECH_STATION.get().getName().getString();
        this.clientSide = player.level().isClientSide();
        this.stationBlockEntity = player.level().getBlockEntity(blockPos) instanceof TechStationBlockEntity blockEntity ? blockEntity : null;
        this.serviceInventory = new SimpleContainer(7) {
            @Override
            public void setChanged() {
                super.setChanged();
                TechStationMenu.this.slotsChanged(this);
            }
        };
        this.resultInventory = new SimpleContainer(2);

        if (!clientSide && stationBlockEntity != null) {
            PlayerStationUpgradeData.migrateTechRepairTier(player, stationBlockEntity.getSupportedTierOrdinal(TechStationBlockEntity.REPAIR_INPUT_SLOT));
            PlayerStationUpgradeData.migrateTechUpgradeTier(player, stationBlockEntity.getSupportedTierOrdinal(TechStationBlockEntity.UPGRADE_INPUT_SLOT));
        }

        addStationSlots();
        addPlayerInventorySlots(playerInventory, TechStationLayout.PLAYER_INVENTORY_X, TechStationLayout.PLAYER_INVENTORY_Y);
        addPlayerHotbarSlots(playerInventory, TechStationLayout.PLAYER_INVENTORY_X, TechStationLayout.PLAYER_HOTBAR_Y);
        addTierSlots();
        updateResults();
    }

    private void addStationSlots() {
        this.addSlot(new Slot(serviceInventory, REPAIR_INPUT_SLOT, TechStationLayout.REPAIR_INPUT_X, TechStationLayout.REPAIR_INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CyberwareItem && canAcceptTier(stack, getRepairSupportedTier());
            }
        });
        this.addSlot(new Slot(serviceInventory, REPAIR_PRIMARY_SLOT, TechStationLayout.REPAIR_MATERIAL_X, TechStationLayout.REPAIR_PRIMARY_Y));
        this.addSlot(new Slot(serviceInventory, REPAIR_SECONDARY_SLOT, TechStationLayout.REPAIR_MATERIAL_X, TechStationLayout.REPAIR_MATERIAL_Y));
        this.addSlot(new Slot(serviceInventory, REPAIR_TERTIARY_SLOT, TechStationLayout.REPAIR_MATERIAL_X, TechStationLayout.REPAIR_TERTIARY_Y));
        this.addSlot(new Slot(serviceInventory, UPGRADE_INPUT_SLOT, TechStationLayout.UPGRADE_INPUT_X, TechStationLayout.UPGRADE_INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CyberwareItem && canAcceptTier(stack, getUpgradeSupportedTier());
            }
        });
        this.addSlot(new Slot(serviceInventory, UPGRADE_PRIMARY_SLOT, TechStationLayout.UPGRADE_PRIMARY_X, TechStationLayout.UPGRADE_PRIMARY_Y));
        this.addSlot(new Slot(serviceInventory, UPGRADE_SECONDARY_SLOT, TechStationLayout.UPGRADE_SECONDARY_X, TechStationLayout.UPGRADE_SECONDARY_Y));
        this.addSlot(new Slot(resultInventory, 0, TechStationLayout.REPAIR_RESULT_X, TechStationLayout.REPAIR_RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                CyberwareServicePlan plan = getRepairResolvedPlan();
                if (plan.isAvailable()) {
                    consumeRepairInputs(plan);
                }
                super.onTake(player, stack);
                updateResults();
            }
        });
        this.addSlot(new Slot(resultInventory, 1, TechStationLayout.UPGRADE_RESULT_X, TechStationLayout.UPGRADE_RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                CyberwareServicePlan plan = getUpgradeResolvedPlan();
                if (plan.isAvailable()) {
                    consumeUpgradeInputs(plan);
                }
                super.onTake(player, stack);
                updateResults();
            }
        });
    }

    private void addTierSlots() {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return !clientSide ? PlayerStationUpgradeData.getTechRepairTier(player).ordinal() : repairSupportedTierClient;
            }

            @Override
            public void set(int value) {
                repairSupportedTierClient = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return !clientSide ? PlayerStationUpgradeData.getTechUpgradeTier(player).ordinal() : upgradeSupportedTierClient;
            }

            @Override
            public void set(int value) {
                upgradeSupportedTierClient = value;
            }
        });
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateResults();
    }

    private void updateResults() {
        CyberwareServicePlan repairPlan = getRepairResolvedPlan();
        CyberwareServicePlan upgradePlan = getUpgradeResolvedPlan();
        resultInventory.setItem(0, repairPlan.isAvailable() ? repairPlan.output().copy() : ItemStack.EMPTY);
        resultInventory.setItem(1, upgradePlan.isAvailable() ? upgradePlan.output().copy() : ItemStack.EMPTY);
        broadcastChanges();
    }

    private void consumeRepairInputs(CyberwareServicePlan plan) {
        serviceInventory.removeItem(REPAIR_INPUT_SLOT, 1);
        consumeMatchedRequirements(plan, REPAIR_MATERIAL_SLOTS);
    }

    private void consumeMatchedRequirements(CyberwareServicePlan plan, int[] materialSlots) {
        var requirements = CyberwareServiceHelper.getRequiredMaterials(plan);
        boolean[] consumedSlots = new boolean[materialSlots.length];
        for (var requirement : requirements) {
            for (int index = 0; index < materialSlots.length; index++) {
                if (consumedSlots[index]) {
                    continue;
                }
                int slotIndex = materialSlots[index];
                ItemStack candidate = serviceInventory.getItem(slotIndex);
                if (!isMatchingMaterial(candidate, requirement.stack()) || candidate.getCount() < requirement.count()) {
                    continue;
                }
                candidate.shrink(requirement.count());
                if (candidate.isEmpty()) {
                    serviceInventory.setItem(slotIndex, ItemStack.EMPTY);
                }
                consumedSlots[index] = true;
                break;
            }
        }
    }

    private void consumeUpgradeInputs(CyberwareServicePlan plan) {
        serviceInventory.removeItem(UPGRADE_INPUT_SLOT, 1);
        consumeMatchedRequirements(plan, UPGRADE_MATERIAL_SLOTS);
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

        if (index == REPAIR_RESULT_SLOT || index == UPGRADE_RESULT_SLOT) {
            if (!this.moveItemStackTo(sourceStack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onTake(player, sourceStack);
            return copiedStack;
        }

        if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(sourceStack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() instanceof CyberwareItem) {
            if (!moveCyberwareIntoBays(sourceStack) && !moveWithinPlayerInventory(index, sourceStack)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveMaterialIntoBays(sourceStack) && !moveWithinPlayerInventory(index, sourceStack)) {
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

    private boolean moveCyberwareIntoBays(ItemStack sourceStack) {
        if (canRepair(sourceStack) && moveToMenuSlot(sourceStack, REPAIR_INPUT_SLOT)) {
            return true;
        }
        if (canUpgrade(sourceStack) && moveToMenuSlot(sourceStack, UPGRADE_INPUT_SLOT)) {
            return true;
        }
        if (moveToMenuSlot(sourceStack, REPAIR_INPUT_SLOT)) {
            return true;
        }
        return moveToMenuSlot(sourceStack, UPGRADE_INPUT_SLOT);
    }

    private boolean moveMaterialIntoBays(ItemStack sourceStack) {
        int preferredSlot = findPreferredMaterialSlot(sourceStack);
        if (preferredSlot >= 0 && this.moveItemStackTo(sourceStack, preferredSlot, preferredSlot + 1, false)) {
            return true;
        }
        if (this.moveItemStackTo(sourceStack, REPAIR_PRIMARY_SLOT, REPAIR_TERTIARY_SLOT + 1, false)) {
            return true;
        }
        return this.moveItemStackTo(sourceStack, UPGRADE_PRIMARY_SLOT, UPGRADE_SECONDARY_SLOT + 1, false);
    }

    private int findPreferredMaterialSlot(ItemStack sourceStack) {
        CyberwareServicePlan repairPlan = getRepairPlan();
        if (repairPlan.isAvailable()) {
            int repairSlot = findMatchingRequirementSlot(sourceStack, repairPlan, REPAIR_MATERIAL_SLOTS);
            if (repairSlot >= 0) {
                return repairSlot;
            }
        }

        CyberwareServicePlan upgradePlan = getUpgradePlan();
        if (!upgradePlan.isAvailable()) {
            return -1;
        }
        return findMatchingRequirementSlot(sourceStack, upgradePlan, UPGRADE_MATERIAL_SLOTS);
    }

    private int findMatchingRequirementSlot(ItemStack sourceStack, CyberwareServicePlan plan, int[] materialSlots) {
        var requirements = CyberwareServiceHelper.getRequiredMaterials(plan);
        for (int slotOffset = 0; slotOffset < requirements.size() && slotOffset < materialSlots.length; slotOffset++) {
            var requirement = requirements.get(slotOffset);
            int slotIndex = materialSlots[slotOffset];
            if (isMatchingMaterial(sourceStack, requirement.stack()) && canMergeInto(slotIndex, sourceStack)) {
                return slotIndex;
            }
        }
        return -1;
    }

    private boolean moveToMenuSlot(ItemStack sourceStack, int slotIndex) {
        return this.moveItemStackTo(sourceStack, slotIndex, slotIndex + 1, false);
    }

    private boolean canRepair(ItemStack sourceStack) {
        return canAcceptTier(sourceStack, getRepairSupportedTier()) && CyberwareServiceHelper.getRepairPlan(sourceStack).isAvailable();
    }

    private boolean canUpgrade(ItemStack sourceStack) {
        return canAcceptTier(sourceStack, getUpgradeSupportedTier()) && CyberwareServiceHelper.getUpgradePlan(sourceStack).isAvailable();
    }

    private boolean isMatchingMaterial(ItemStack sourceStack, ItemStack expectedStack) {
        return !expectedStack.isEmpty() && ItemStack.isSameItemSameComponents(sourceStack.copyWithCount(1), expectedStack.copyWithCount(1));
    }

    private boolean canMergeInto(int slotIndex, ItemStack sourceStack) {
        ItemStack existing = serviceInventory.getItem(slotIndex);
        return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, sourceStack);
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, ModBlocks.TECH_STATION.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, serviceInventory);
        resultInventory.clearContent();
    }

    @Override
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public String getBlockDisplayName() {
        return blockDisplayName;
    }

    public ItemStack getRepairInputStack() {
        return serviceInventory.getItem(REPAIR_INPUT_SLOT);
    }

    public ItemStack getRepairMaterialStack(int slot) {
        return serviceInventory.getItem(REPAIR_MATERIAL_SLOTS[slot]);
    }

    public ItemStack[] getRepairMaterialStacks() {
        ItemStack[] stacks = new ItemStack[REPAIR_MATERIAL_SLOTS.length];
        for (int index = 0; index < REPAIR_MATERIAL_SLOTS.length; index++) {
            stacks[index] = serviceInventory.getItem(REPAIR_MATERIAL_SLOTS[index]);
        }
        return stacks;
    }

    public ItemStack getUpgradeInputStack() {
        return serviceInventory.getItem(UPGRADE_INPUT_SLOT);
    }

    public ItemStack getUpgradePrimaryMaterialStack() {
        return serviceInventory.getItem(UPGRADE_PRIMARY_SLOT);
    }

    public ItemStack getUpgradeSecondaryMaterialStack() {
        return serviceInventory.getItem(UPGRADE_SECONDARY_SLOT);
    }

    public CyberwareTier getRepairSupportedTier() {
        return !clientSide
                ? PlayerStationUpgradeData.getTechRepairTier(player)
                : TechStationBlockEntity.getTierByOrdinal(repairSupportedTierClient);
    }

    public CyberwareTier getNextRepairSupportedTier() {
        CyberwareTier current = getRepairSupportedTier();
        return current.next();
    }

    public boolean canUpgradeRepairSupportedTier() {
        return getRepairInputStack().isEmpty()
                && getRepairSupportedTier() != CyberwareTier.TIER_5
                && !getRequiredRepairUpgradeComponentStack().isEmpty()
                && (!clientSide || PlayerStationUpgradeData.getTechRepairTier(player) != CyberwareTier.TIER_5);
    }

    public ItemStack getRequiredRepairUpgradeComponentStack() {
        return TechStationBlockEntity.getRequiredUpgradeComponentStack(getRepairSupportedTier());
    }

    public boolean hasRequiredRepairUpgradeComponent() {
        ItemStack required = getRequiredRepairUpgradeComponentStack();
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || countAccessiblePlayerItems(required) > 0);
    }

    public boolean tryUpgradeRepairSupportedTier() {
        ItemStack required = getRequiredRepairUpgradeComponentStack();
        if (!canUpgradeRepairSupportedTier()
                || required.isEmpty()
                || (!hasCreativeUpgradeBypass() && !consumeAccessiblePlayerItem(required))
                || !PlayerStationUpgradeData.tryUpgradeTechRepairTier(player)) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    public CyberwareTier getUpgradeSupportedTier() {
        return !clientSide
                ? PlayerStationUpgradeData.getTechUpgradeTier(player)
                : TechStationBlockEntity.getTierByOrdinal(upgradeSupportedTierClient);
    }

    public CyberwareTier getNextUpgradeSupportedTier() {
        CyberwareTier current = getUpgradeSupportedTier();
        return current.next();
    }

    public boolean canUpgradeUpgradeSupportedTier() {
        return getUpgradeInputStack().isEmpty()
                && getUpgradeSupportedTier() != CyberwareTier.TIER_5
                && !getRequiredUpgradeInputComponentStack().isEmpty()
                && (!clientSide || PlayerStationUpgradeData.getTechUpgradeTier(player) != CyberwareTier.TIER_5);
    }

    public ItemStack getRequiredUpgradeInputComponentStack() {
        return TechStationBlockEntity.getRequiredUpgradeComponentStack(getUpgradeSupportedTier());
    }

    public boolean hasRequiredUpgradeInputComponent() {
        ItemStack required = getRequiredUpgradeInputComponentStack();
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || countAccessiblePlayerItems(required) > 0);
    }

    public boolean tryUpgradeUpgradeSupportedTier() {
        ItemStack required = getRequiredUpgradeInputComponentStack();
        if (!canUpgradeUpgradeSupportedTier()
                || required.isEmpty()
                || (!hasCreativeUpgradeBypass() && !consumeAccessiblePlayerItem(required))
                || !PlayerStationUpgradeData.tryUpgradeTechUpgradeTier(player)) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    public CyberwareServicePlan getRepairPlan() {
        return CyberwareServiceHelper.getRepairPlan(getRepairInputStack());
    }

    public CyberwareServicePlan getUpgradePlan() {
        return CyberwareServiceHelper.getUpgradePlan(getUpgradeInputStack());
    }

    public CyberwareServicePlan getRepairResolvedPlan() {
        return CyberwareServiceHelper.matchesMaterials(getRepairPlan(), getRepairMaterialStacks())
                ? getRepairPlan()
                : CyberwareServicePlan.empty();
    }

    public CyberwareServicePlan getUpgradeResolvedPlan() {
        return CyberwareServiceHelper.matchesMaterials(getUpgradePlan(), getUpgradePrimaryMaterialStack(), getUpgradeSecondaryMaterialStack())
                ? getUpgradePlan()
                : CyberwareServicePlan.empty();
    }

    private boolean hasCreativeUpgradeBypass() {
        return player.getAbilities().instabuild;
    }
}
