package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.cyberware.ChipwareSocketHandler;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffect;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleCategory;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareModuleHandler;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlotType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import de.artemis.cyberneticenhancements.common.item.ChipwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareModuleItem;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.EnumMap;

public final class RipperStationMenu extends AbstractBaseMenu implements NamedBlockMenu {
    private static final int CHIP_HANDLER_COUNT = 3;
    private static final int CHIP_SLOT_COUNT = 3;

    private final PlayerCyberwareInventory cyberwareInventory;
    private final Player player;
    private final ChipwareSocketHandler[] chipwareInventories;
    private final CyberwareModuleHandler armModuleInventory;
    private final CyberwareModuleHandler legModuleInventory;
    private final BlockPos blockPos;
    private final String blockDisplayName;
    private final int cyberwareSlotCount;
    private final int containerSlotCount;
    private final int playerInventoryStart;
    private final int playerHotbarStart;
    private int installedChromeClient;
    private int chromeCapacityClient;
    private int cyberstrainClient;
    private int integrityClient;
    private int installedCountClient;
    private int stageIndexClient;
    private final int[] supportedTierClient = new int[PlayerCyberwareInventory.SLOT_COUNT];
    private final int[][] chipSupportedTierClient = new int[CHIP_HANDLER_COUNT][CHIP_SLOT_COUNT];
    private final int[] armModuleSupportedTierClient = new int[CyberwareModuleHandler.MAX_MODULE_SLOTS];
    private final int[] legModuleSupportedTierClient = new int[CyberwareModuleHandler.MAX_MODULE_SLOTS];

    public RipperStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public RipperStationMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenuTypes.RIPPER_STATION.get(), containerId);
        this.player = playerInventory.player;
        this.cyberwareInventory = new PlayerCyberwareInventory(player);
        this.chipwareInventories = new ChipwareSocketHandler[] {
                new ChipwareSocketHandler(cyberwareInventory, player, CyberwareSlot.FRONTAL_CORTEX_1.ordinal()),
                new ChipwareSocketHandler(cyberwareInventory, player, CyberwareSlot.FRONTAL_CORTEX_2.ordinal()),
                new ChipwareSocketHandler(cyberwareInventory, player, CyberwareSlot.FRONTAL_CORTEX_3.ordinal())
        };
        this.armModuleInventory = new CyberwareModuleHandler(cyberwareInventory, player, CyberwareSlot.ARMS_1.ordinal(), CyberwareModuleCategory.ARMS);
        this.legModuleInventory = new CyberwareModuleHandler(cyberwareInventory, player, CyberwareSlot.LEGS_1.ordinal(), CyberwareModuleCategory.LEGS);
        this.blockPos = blockPos.immutable();
        this.blockDisplayName = ModBlocks.RIPPER_STATION.get().getName().getString();

        addCyberwareSlots();
        this.cyberwareSlotCount = this.slots.size();
        addChipSlots();
        addModuleSlots();
        this.containerSlotCount = this.slots.size();
        this.playerInventoryStart = containerSlotCount;
        addRipperPlayerInventorySlots(playerInventory);
        this.playerHotbarStart = this.slots.size();
        addRipperPlayerHotbarSlots(playerInventory);
        addStatSlots();
    }

    private void addCyberwareSlots() {
        for (int slot = 0; slot < PlayerCyberwareInventory.SLOT_COUNT; slot++) {
            this.addSlot(new SlotItemHandler(cyberwareInventory, slot, RipperStationLayout.CYBERWARE_SLOT_X[slot], RipperStationLayout.CYBERWARE_SLOT_Y[slot]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return cyberwareInventory.isItemValid(getContainerSlot(), stack);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    cyberwareInventory.save();
                }

                @Override
                public void onTake(Player player, ItemStack stack) {
                    super.onTake(player, stack);
                    cyberwareInventory.save();
                }
            });
        }
    }

    private void addChipSlots() {
        for (int handlerIndex = 0; handlerIndex < CHIP_HANDLER_COUNT; handlerIndex++) {
            ChipwareSocketHandler chipHandler = chipwareInventories[handlerIndex];
            for (int slot = 0; slot < CHIP_SLOT_COUNT; slot++) {
                final int chipSlot = slot;
                this.addSlot(new SlotItemHandler(
                        chipHandler,
                        chipSlot,
                        RipperStationLayout.CHIP_CLUSTER_X[handlerIndex] + RipperStationLayout.CHIP_SLOT_OFFSET_X[slot],
                        RipperStationLayout.CHIP_SLOT_Y
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return chipHandler.isItemValid(chipSlot, stack);
                    }

                    @Override
                    public boolean isActive() {
                        return chipHandler.isSlotUnlocked(chipSlot);
                    }

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        cyberwareInventory.save();
                    }

                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        super.onTake(player, stack);
                        cyberwareInventory.save();
                    }
                });
            }
        }
    }

    private void addModuleSlots() {
        addModuleSlotBank(armModuleInventory, RipperStationLayout.ARM_MODULE_X, RipperStationLayout.ARM_MODULE_Y);
        addModuleSlotBank(legModuleInventory, RipperStationLayout.LEG_MODULE_X, RipperStationLayout.LEG_MODULE_Y);
    }

    private void addModuleSlotBank(CyberwareModuleHandler moduleHandler, int[] slotX, int y) {
        for (int slot = 0; slot < slotX.length; slot++) {
            final int moduleSlot = slot;
            this.addSlot(new SlotItemHandler(moduleHandler, moduleSlot, slotX[slot], y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return moduleHandler.isItemValid(moduleSlot, stack);
                }

                @Override
                public boolean isActive() {
                    return moduleHandler.isSlotUnlocked(moduleSlot);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    cyberwareInventory.save();
                }

                @Override
                public void onTake(Player player, ItemStack stack) {
                    super.onTake(player, stack);
                    cyberwareInventory.save();
                }
            });
        }
    }

    private void addRipperPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        RipperStationLayout.PLAYER_INVENTORY_X + column * RipperStationLayout.PLAYER_SLOT_SPACING,
                        RipperStationLayout.PLAYER_INVENTORY_Y + row * RipperStationLayout.PLAYER_SLOT_SPACING
                ));
            }
        }
    }

    private void addRipperPlayerHotbarSlots(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(
                    inventory,
                    slot,
                    RipperStationLayout.PLAYER_INVENTORY_X + slot * RipperStationLayout.PLAYER_SLOT_SPACING,
                    RipperStationLayout.PLAYER_HOTBAR_Y
            ));
        }
    }

    private void addStatSlots() {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return cyberwareInventory.getInstalledChromeCost();
            }

            @Override
            public void set(int value) {
                installedChromeClient = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return cyberwareInventory.getChromeCapacity();
            }

            @Override
            public void set(int value) {
                chromeCapacityClient = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return cyberwareInventory.getCyberstrain();
            }

            @Override
            public void set(int value) {
                cyberstrainClient = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return cyberwareInventory.getIntegrityRating();
            }

            @Override
            public void set(int value) {
                integrityClient = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return cyberwareInventory.getInstalledCount();
            }

            @Override
            public void set(int value) {
                installedCountClient = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return cyberwareInventory.getStageIndex();
            }

            @Override
            public void set(int value) {
                stageIndexClient = value;
            }
        });
        for (int slot = 0; slot < PlayerCyberwareInventory.SLOT_COUNT; slot++) {
            final int cyberwareSlot = slot;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return cyberwareInventory.getSupportedTier(cyberwareSlot).ordinal();
                }

                @Override
                public void set(int value) {
                    supportedTierClient[cyberwareSlot] = value;
                }
            });
        }
        for (int handlerIndex = 0; handlerIndex < CHIP_HANDLER_COUNT; handlerIndex++) {
            final int chipHandlerIndex = handlerIndex;
            for (int slot = 0; slot < CHIP_SLOT_COUNT; slot++) {
                final int chipSlot = slot;
                addDataSlot(new DataSlot() {
                    @Override
                    public int get() {
                        return chipwareInventories[chipHandlerIndex].getSupportedTier(chipSlot).ordinal();
                    }

                    @Override
                    public void set(int value) {
                        chipSupportedTierClient[chipHandlerIndex][chipSlot] = value;
                    }
                });
            }
        }
        for (int slot = 0; slot < CyberwareModuleHandler.MAX_MODULE_SLOTS; slot++) {
            final int moduleSlot = slot;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return armModuleInventory.getSupportedTier(moduleSlot).ordinal();
                }

                @Override
                public void set(int value) {
                    armModuleSupportedTierClient[moduleSlot] = value;
                }
            });
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return legModuleInventory.getSupportedTier(moduleSlot).ordinal();
                }

                @Override
                public void set(int value) {
                    legModuleSupportedTierClient[moduleSlot] = value;
                }
            });
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        cyberwareInventory.save();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, ModBlocks.RIPPER_STATION.get());
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

        if (index < containerSlotCount) {
            if (!this.moveItemStackTo(sourceStack, playerInventoryStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() instanceof ChipwareItem) {
            if (!moveIntoChipMenuSlots(sourceStack)) {
                return moveWithinPlayerInventory(index, sourceStack) ? copiedStack : ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() instanceof CyberwareModuleItem moduleItem) {
            if (!moveIntoModuleMenuSlots(sourceStack, moduleItem.getDefinition().category())) {
                return moveWithinPlayerInventory(index, sourceStack) ? copiedStack : ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() instanceof CyberwareItem cyberwareItem) {
            if (!moveIntoCyberwareMenuSlots(sourceStack, cyberwareItem.getSlotType())) {
                return moveWithinPlayerInventory(index, sourceStack) ? copiedStack : ItemStack.EMPTY;
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
        if (index < playerHotbarStart) {
            return this.moveItemStackTo(sourceStack, playerHotbarStart, this.slots.size(), false);
        }
        return this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarStart, false);
    }

    private boolean moveIntoCyberwareMenuSlots(ItemStack sourceStack, CyberwareSlotType slotType) {
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            if (slot.getType() != slotType) {
                continue;
            }

            int menuIndex = findSlotIndexForContainerSlot(cyberwareSlotCount, slot.ordinal());
            if (menuIndex < 0) {
                continue;
            }
            Slot targetSlot = this.slots.get(menuIndex);
            if (targetSlot.mayPlace(sourceStack) && this.moveItemStackTo(sourceStack, menuIndex, menuIndex + 1, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean moveIntoChipMenuSlots(ItemStack sourceStack) {
        for (int handlerIndex = 0; handlerIndex < chipwareInventories.length; handlerIndex++) {
            int startIndex = cyberwareSlotCount + handlerIndex * CHIP_SLOT_COUNT;
            for (int slot = 0; slot < CHIP_SLOT_COUNT; slot++) {
                int menuIndex = startIndex + slot;
                Slot targetSlot = this.slots.get(menuIndex);
                if (targetSlot.mayPlace(sourceStack) && this.moveItemStackTo(sourceStack, menuIndex, menuIndex + 1, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean moveIntoModuleMenuSlots(ItemStack sourceStack, CyberwareModuleCategory category) {
        CyberwareModuleHandler handler = category == CyberwareModuleCategory.ARMS ? armModuleInventory : legModuleInventory;
        int startIndex = category == CyberwareModuleCategory.ARMS
                ? cyberwareSlotCount + CHIP_HANDLER_COUNT * CHIP_SLOT_COUNT
                : cyberwareSlotCount + CHIP_HANDLER_COUNT * CHIP_SLOT_COUNT + CyberwareModuleHandler.MAX_MODULE_SLOTS;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            int menuIndex = startIndex + slot;
            Slot targetSlot = this.slots.get(menuIndex);
            if (targetSlot.mayPlace(sourceStack) && this.moveItemStackTo(sourceStack, menuIndex, menuIndex + 1, false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public String getBlockDisplayName() {
        return blockDisplayName;
    }

    public ItemStack getCyberwareStack(CyberwareSlot slot) {
        return cyberwareInventory.getStackInSlot(slot.ordinal());
    }

    public int getInstalledCount(CyberwareSlotType type) {
        int count = 0;
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            if (slot.getType() == type && !getCyberwareStack(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public ItemStack getChipwareStack(int handlerIndex, int slot) {
        return chipwareInventories[handlerIndex].getStackInSlot(slot);
    }

    public boolean isChipSlotUnlocked(int handlerIndex, int slot) {
        return chipwareInventories[handlerIndex].isSlotUnlocked(slot);
    }

    public Component getChipwareHostName(int handlerIndex) {
        return chipwareInventories[handlerIndex].getHostDisplayName();
    }

    public ItemStack getArmModuleStack(int slot) {
        return armModuleInventory.getStackInSlot(slot);
    }

    public ItemStack getLegModuleStack(int slot) {
        return legModuleInventory.getStackInSlot(slot);
    }

    public boolean isArmModuleSlotUnlocked(int slot) {
        return armModuleInventory.isSlotUnlocked(slot);
    }

    public boolean isLegModuleSlotUnlocked(int slot) {
        return legModuleInventory.isSlotUnlocked(slot);
    }

    public Component getArmModuleHostName() {
        return armModuleInventory.getHostDisplayName();
    }

    public Component getLegModuleHostName() {
        return legModuleInventory.getHostDisplayName();
    }

    public int getInstalledChrome() {
        return installedChromeClient;
    }

    public int getChromeCapacity() {
        return chromeCapacityClient;
    }

    public int getCyberstrain() {
        return cyberstrainClient;
    }

    public int getIntegrity() {
        return integrityClient;
    }

    public int getInstalledCount() {
        return installedCountClient;
    }

    public int getStageIndex() {
        return stageIndexClient;
    }

    public int getInstalledCyberwareCount() {
        return installedCountClient;
    }

    public int getTotalCyberwareSlotCount() {
        return PlayerCyberwareInventory.SLOT_COUNT;
    }

    public int getInstalledChipwareCount() {
        int total = 0;
        for (int handlerIndex = 0; handlerIndex < chipwareInventories.length; handlerIndex++) {
            int unlockedSlots = chipwareInventories[handlerIndex].getUnlockedSlotCount();
            for (int slot = 0; slot < unlockedSlots; slot++) {
                if (!getChipwareStack(handlerIndex, slot).isEmpty()) {
                    total++;
                }
            }
        }
        return total;
    }

    public int getTotalChipwareSlotCount() {
        int total = 0;
        for (ChipwareSocketHandler chipwareInventory : chipwareInventories) {
            total += chipwareInventory.getUnlockedSlotCount();
        }
        return total;
    }

    public int getInstalledArmModuleCount() {
        int total = 0;
        int unlockedSlots = armModuleInventory.getUnlockedSlotCount();
        for (int slot = 0; slot < unlockedSlots; slot++) {
            if (!getArmModuleStack(slot).isEmpty()) {
                total++;
            }
        }
        return total;
    }

    public int getTotalArmModuleSlotCount() {
        return armModuleInventory.getUnlockedSlotCount();
    }

    public int getInstalledLegModuleCount() {
        int total = 0;
        int unlockedSlots = legModuleInventory.getUnlockedSlotCount();
        for (int slot = 0; slot < unlockedSlots; slot++) {
            if (!getLegModuleStack(slot).isEmpty()) {
                total++;
            }
        }
        return total;
    }

    public int getTotalLegModuleSlotCount() {
        return legModuleInventory.getUnlockedSlotCount();
    }

    public int getChromePercent() {
        if (getChromeCapacity() <= 0) {
            return 0;
        }
        return getInstalledChrome() * 100 / getChromeCapacity();
    }

    public EnumMap<CyberwareEffectType, Double> getInstalledEffectTotals() {
        EnumMap<CyberwareEffectType, Double> totals = new EnumMap<>(CyberwareEffectType.class);
        for (int slot = 0; slot < cyberwareInventory.getSlots(); slot++) {
            ItemStack stack = cyberwareInventory.getStackInSlot(slot);
            if (!(stack.getItem() instanceof CyberwareItem cyberwareItem)) {
                continue;
            }

            double integrityScale = CyberwareConditionHelper.getIntegrityRatio(stack, cyberwareItem.getDefinition());
            mergeEffects(totals, cyberwareItem.getEffects(stack), integrityScale);
        }
        for (CyberwareModuleItem moduleItem : cyberwareInventory.getInstalledModuleItems()) {
            mergeEffects(totals, moduleItem.getDefinition().effects(), 1.0D);
        }
        for (ChipwareItem chipwareItem : cyberwareInventory.getInstalledChipwareItems()) {
            mergeEffects(totals, chipwareItem.getDefinition().effects(), 1.0D);
        }
        return totals;
    }

    public int getChromeHeadroomBonus() {
        return getChromeCapacity() - PlayerCyberwareInventory.BASE_CHROME_CAPACITY;
    }

    public CyberwareTier getSupportedTier(CyberwareSlot slot) {
        if (!player.level().isClientSide()) {
            return cyberwareInventory.getSupportedTier(slot.ordinal());
        }
        int ordinal = supportedTierClient[slot.ordinal()];
        return ordinal >= 0 && ordinal < CyberwareTier.values().length ? CyberwareTier.values()[ordinal] : CyberwareTier.TIER_1;
    }

    public CyberwareTier getNextSupportedTier(CyberwareSlot slot) {
        CyberwareTier current = getSupportedTier(slot);
        return current.next();
    }

    public boolean canUpgradeSupportedTier(CyberwareSlot slot) {
        int slotIndex = slot.ordinal();
        return slotIndex >= 0
                && slotIndex < cyberwareSlotCount
                && this.slots.get(slotIndex).getItem().isEmpty()
                && getSupportedTier(slot) != CyberwareTier.TIER_5
                && !getRequiredUpgradeComponentStack(slot).isEmpty();
    }

    public boolean hasRequiredUpgradeComponent(CyberwareSlot slot) {
        ItemStack required = getRequiredUpgradeComponentStack(slot);
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || countAccessiblePlayerItems(required) > 0);
    }

    public ItemStack getRequiredUpgradeComponentStack(CyberwareSlot slot) {
        return getRequiredUpgradeComponentStack(getSupportedTier(slot));
    }

    public boolean tryUpgradeSupportedTier(CyberwareSlot slot) {
        boolean upgraded = cyberwareInventory.tryUpgradeSupportedTier(slot.ordinal());
        if (upgraded) {
            broadcastChanges();
        }
        return upgraded;
    }

    private static void mergeEffects(EnumMap<CyberwareEffectType, Double> totals, Iterable<CyberwareEffect> effects, double scale) {
        if (scale <= 0.0001D) {
            return;
        }

        for (CyberwareEffect effect : effects) {
            if (effect.type().isMobEffect()) {
                if (scale >= 0.35D) {
                    totals.merge(effect.type(), effect.amount(), Math::max);
                }
                continue;
            }
            totals.merge(effect.type(), effect.amount() * scale, Double::sum);
        }
    }

    public CyberwareTier getChipSupportedTier(int handlerIndex, int slot) {
        if (!player.level().isClientSide()) {
            return chipwareInventories[handlerIndex].getSupportedTier(slot);
        }
        return getTierByOrdinal(chipSupportedTierClient[handlerIndex][slot]);
    }

    public CyberwareTier getNextChipSupportedTier(int handlerIndex, int slot) {
        CyberwareTier current = getChipSupportedTier(handlerIndex, slot);
        return current.next();
    }

    public boolean canUpgradeChipSupportedTier(int handlerIndex, int slot) {
        return chipwareInventories[handlerIndex].isSlotUnlocked(slot)
                && getChipwareStack(handlerIndex, slot).isEmpty()
                && getChipSupportedTier(handlerIndex, slot) != CyberwareTier.TIER_5
                && !getRequiredChipUpgradeComponentStack(handlerIndex, slot).isEmpty();
    }

    public boolean hasRequiredChipUpgradeComponent(int handlerIndex, int slot) {
        ItemStack required = getRequiredChipUpgradeComponentStack(handlerIndex, slot);
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || countAccessiblePlayerItems(required) > 0);
    }

    public ItemStack getRequiredChipUpgradeComponentStack(int handlerIndex, int slot) {
        return getRequiredUpgradeComponentStack(getChipSupportedTier(handlerIndex, slot));
    }

    public boolean tryUpgradeChipSupportedTier(int handlerIndex, int slot) {
        boolean upgraded = chipwareInventories[handlerIndex].tryUpgradeSupportedTier(slot);
        if (upgraded) {
            broadcastChanges();
        }
        return upgraded;
    }

    public CyberwareTier getArmModuleSupportedTier(int slot) {
        if (!player.level().isClientSide()) {
            return armModuleInventory.getSupportedTier(slot);
        }
        return getTierByOrdinal(armModuleSupportedTierClient[slot]);
    }

    public CyberwareTier getNextArmModuleSupportedTier(int slot) {
        CyberwareTier current = getArmModuleSupportedTier(slot);
        return current.next();
    }

    public boolean canUpgradeArmModuleSupportedTier(int slot) {
        return armModuleInventory.isSlotUnlocked(slot)
                && getArmModuleStack(slot).isEmpty()
                && getArmModuleSupportedTier(slot) != CyberwareTier.TIER_5
                && !getRequiredArmModuleUpgradeComponentStack(slot).isEmpty();
    }

    public boolean hasRequiredArmModuleUpgradeComponent(int slot) {
        ItemStack required = getRequiredArmModuleUpgradeComponentStack(slot);
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || countAccessiblePlayerItems(required) > 0);
    }

    public ItemStack getRequiredArmModuleUpgradeComponentStack(int slot) {
        return getRequiredUpgradeComponentStack(getArmModuleSupportedTier(slot));
    }

    public boolean tryUpgradeArmModuleSupportedTier(int slot) {
        boolean upgraded = armModuleInventory.tryUpgradeSupportedTier(slot);
        if (upgraded) {
            broadcastChanges();
        }
        return upgraded;
    }

    public CyberwareTier getLegModuleSupportedTier(int slot) {
        if (!player.level().isClientSide()) {
            return legModuleInventory.getSupportedTier(slot);
        }
        return getTierByOrdinal(legModuleSupportedTierClient[slot]);
    }

    public CyberwareTier getNextLegModuleSupportedTier(int slot) {
        CyberwareTier current = getLegModuleSupportedTier(slot);
        return current.next();
    }

    public boolean canUpgradeLegModuleSupportedTier(int slot) {
        return legModuleInventory.isSlotUnlocked(slot)
                && getLegModuleStack(slot).isEmpty()
                && getLegModuleSupportedTier(slot) != CyberwareTier.TIER_5
                && !getRequiredLegModuleUpgradeComponentStack(slot).isEmpty();
    }

    public boolean hasRequiredLegModuleUpgradeComponent(int slot) {
        ItemStack required = getRequiredLegModuleUpgradeComponentStack(slot);
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || countAccessiblePlayerItems(required) > 0);
    }

    public ItemStack getRequiredLegModuleUpgradeComponentStack(int slot) {
        return getRequiredUpgradeComponentStack(getLegModuleSupportedTier(slot));
    }

    public boolean tryUpgradeLegModuleSupportedTier(int slot) {
        boolean upgraded = legModuleInventory.tryUpgradeSupportedTier(slot);
        if (upgraded) {
            broadcastChanges();
        }
        return upgraded;
    }

    private ItemStack getRequiredUpgradeComponentStack(CyberwareTier currentTier) {
        return switch (currentTier) {
            case TIER_1 -> ModItems.UNCOMMON_ITEM_COMPONENTS.get().getDefaultInstance();
            case TIER_2 -> ModItems.RARE_ITEM_COMPONENTS.get().getDefaultInstance();
            case TIER_3 -> ModItems.EPIC_ITEM_COMPONENTS.get().getDefaultInstance();
            case TIER_4 -> ModItems.LEGENDARY_ITEM_COMPONENTS.get().getDefaultInstance();
            case TIER_5 -> ItemStack.EMPTY;
        };
    }

    private int countAccessiblePlayerItems(ItemStack required) {
        int total = 0;
        for (int slotIndex = playerInventoryStart; slotIndex < this.slots.size(); slotIndex++) {
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

    private boolean hasCreativeUpgradeBypass() {
        return player.getAbilities().instabuild;
    }

    private static CyberwareTier getTierByOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < CyberwareTier.values().length ? CyberwareTier.values()[ordinal] : CyberwareTier.TIER_1;
    }
}
