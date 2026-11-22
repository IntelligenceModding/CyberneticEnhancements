package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.common.item.ChipwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.item.CyberwareModuleItem;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public final class PlayerCyberwareInventory extends ItemStackHandler {
    public static final int SLOT_COUNT = CyberwareSlot.values().length;
    public static final int BASE_CHROME_CAPACITY = 100;
    private static final String PERSISTENT_DATA_KEY = "cyberneticenhancements.cyberware_inventory";
    private static final String ENTRIES_KEY = "Entries";
    private static final String SLOT_KEY = "Slot";
    private static final String STACK_KEY = "Stack";
    private static final String SUPPORTED_TIERS_KEY = "SupportedTiers";
    private static final String TIER_KEY = "Tier";

    private final Player player;
    private final ServerLevel serverLevel;
    private final CyberwareTier[] supportedTiers = new CyberwareTier[SLOT_COUNT];
    private boolean suppressSave;

    public PlayerCyberwareInventory(Player player) {
        super(SLOT_COUNT);
        this.player = player;
        this.serverLevel = player.level() instanceof ServerLevel level ? level : null;
        resetSupportedTiers();
        if (serverLevel != null) {
            CompoundTag storedData = getStoredInventoryTag(player);
            loadStoredInventory(serverLevel.registryAccess(), storedData);
            if (!storedData.isEmpty() && !player.getPersistentData().contains(PERSISTENT_DATA_KEY, CompoundTag.TAG_COMPOUND)) {
                save();
            }
        }
    }

    public static CompoundTag getStoredInventoryTag(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(PERSISTENT_DATA_KEY, CompoundTag.TAG_COMPOUND)) {
            return persistentData.getCompound(PERSISTENT_DATA_KEY).copy();
        }

        if (player.level() instanceof ServerLevel level) {
            return CyberwareSavedData.get(level).getInventory(player.getUUID());
        }

        return new CompoundTag();
    }

    public static void copyStoredInventory(Player fromPlayer, Player toPlayer) {
        CompoundTag storedData = getStoredInventoryTag(fromPlayer);
        CompoundTag targetPersistentData = toPlayer.getPersistentData();
        if (storedData.isEmpty()) {
            targetPersistentData.remove(PERSISTENT_DATA_KEY);
        } else {
            targetPersistentData.put(PERSISTENT_DATA_KEY, storedData.copy());
        }

        if (toPlayer.level() instanceof ServerLevel level) {
            CyberwareSavedData.get(level).setInventory(toPlayer.getUUID(), storedData);
        }
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (suppressSave) {
            return;
        }
        save();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof CyberwareItem cyberwareItem
                && cyberwareItem.getSlotType() == CyberwareSlot.values()[slot].getType()
                && cyberwareItem.getTier().ordinal() <= getSupportedTier(slot).ordinal();
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    public void save() {
        if (serverLevel == null) {
            return;
        }

        CompoundTag serialized = saveStoredInventory(serverLevel.registryAccess());
        de.artemis.cyberneticenhancements.common.journal.JournalEvents.loadoutChanged(player, getStoredInventoryTag(player), serialized);
        CompoundTag persistentData = player.getPersistentData();
        if (serialized.isEmpty()) {
            persistentData.remove(PERSISTENT_DATA_KEY);
        } else {
            persistentData.put(PERSISTENT_DATA_KEY, serialized.copy());
        }

        CyberwareSavedData.get(serverLevel).setInventory(player.getUUID(), serialized);
        CyberwareEffects.refreshPlayerCyberware(player);
    }

    private CompoundTag saveStoredInventory(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt(SLOT_KEY, slot);
            entryTag.put(STACK_KEY, stack.save(registries, new CompoundTag()));
            entries.add(entryTag);
        }

        if (!entries.isEmpty()) {
            tag.put(ENTRIES_KEY, entries);
        }

        ListTag supportedTierEntries = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (supportedTiers[slot] == CyberwareTier.TIER_1) {
                continue;
            }

            CompoundTag tierTag = new CompoundTag();
            tierTag.putInt(SLOT_KEY, slot);
            tierTag.putInt(TIER_KEY, supportedTiers[slot].ordinal());
            supportedTierEntries.add(tierTag);
        }
        if (!supportedTierEntries.isEmpty()) {
            tag.put(SUPPORTED_TIERS_KEY, supportedTierEntries);
        }
        return tag;
    }

    private void loadStoredInventory(HolderLookup.Provider registries, CompoundTag tag) {
        suppressSave = true;
        try {
            clearSlots();
            resetSupportedTiers();
            if (tag.isEmpty()) {
                return;
            }

            if (tag.contains(ENTRIES_KEY, Tag.TAG_LIST)) {
                loadCurrentFormat(registries, tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND));
            } else if (tag.contains("Items", Tag.TAG_LIST)) {
                loadLegacyFormat(registries, tag);
            }

            loadSupportedTiers(tag);
        } finally {
            suppressSave = false;
        }
    }

    private void loadCurrentFormat(HolderLookup.Provider registries, ListTag entries) {
        for (Tag entry : entries) {
            if (!(entry instanceof CompoundTag entryTag) || !entryTag.contains(SLOT_KEY, Tag.TAG_INT)) {
                continue;
            }

            int slot = entryTag.getInt(SLOT_KEY);
            if (slot < 0 || slot >= SLOT_COUNT || !entryTag.contains(STACK_KEY, Tag.TAG_COMPOUND)) {
                continue;
            }

            ItemStack stack = ItemStack.parseOptional(registries, entryTag.getCompound(STACK_KEY));
            if (!stack.isEmpty()) {
                setStackInSlot(slot, stack);
            }
        }
    }

    private void loadLegacyFormat(HolderLookup.Provider registries, CompoundTag tag) {
        int legacySize = tag.contains("Size", Tag.TAG_INT) ? tag.getInt("Size") : SLOT_COUNT;
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (Tag entry : items) {
            if (!(entry instanceof CompoundTag stackTag)) {
                continue;
            }

            int legacySlot = readLegacySlotIndex(stackTag);
            if (legacySlot < 0) {
                continue;
            }

            int targetSlot = mapLegacySlot(legacySlot, legacySize);
            if (targetSlot < 0 || targetSlot >= SLOT_COUNT) {
                continue;
            }

            ItemStack stack = ItemStack.parseOptional(registries, stackTag);
            if (!stack.isEmpty()) {
                setStackInSlot(targetSlot, stack);
            }
        }
    }

    private int readLegacySlotIndex(CompoundTag stackTag) {
        if (stackTag.contains(SLOT_KEY, Tag.TAG_INT)) {
            return stackTag.getInt(SLOT_KEY);
        }
        if (stackTag.contains(SLOT_KEY, Tag.TAG_BYTE)) {
            return Byte.toUnsignedInt(stackTag.getByte(SLOT_KEY));
        }
        return -1;
    }

    private int mapLegacySlot(int legacySlot, int legacySize) {
        if (legacySize <= 8) {
            return switch (legacySlot) {
                case 0 -> CyberwareSlot.FACE_1.ordinal();
                case 1 -> CyberwareSlot.ARMS_1.ordinal();
                case 2 -> CyberwareSlot.LEGS_1.ordinal();
                case 3 -> CyberwareSlot.OPERATING_SYSTEM_1.ordinal();
                case 4 -> CyberwareSlot.INTEGUMENTARY_SYSTEM_1.ordinal();
                case 5 -> CyberwareSlot.NERVOUS_SYSTEM_1.ordinal();
                case 6 -> CyberwareSlot.CIRCULATORY_SYSTEM_1.ordinal();
                case 7 -> CyberwareSlot.FRONTAL_CORTEX_1.ordinal();
                default -> -1;
            };
        }
        return legacySlot >= 0 && legacySlot < SLOT_COUNT ? legacySlot : -1;
    }

    private void clearSlots() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private void resetSupportedTiers() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            supportedTiers[slot] = CyberwareTier.TIER_1;
        }
    }

    private void loadSupportedTiers(CompoundTag tag) {
        if (!tag.contains(SUPPORTED_TIERS_KEY, Tag.TAG_LIST)) {
            return;
        }

        ListTag entries = tag.getList(SUPPORTED_TIERS_KEY, Tag.TAG_COMPOUND);
        for (Tag entry : entries) {
            if (!(entry instanceof CompoundTag tierTag) || !tierTag.contains(SLOT_KEY, Tag.TAG_INT) || !tierTag.contains(TIER_KEY, Tag.TAG_INT)) {
                continue;
            }

            int slot = tierTag.getInt(SLOT_KEY);
            int tierOrdinal = tierTag.getInt(TIER_KEY);
            if (slot < 0 || slot >= SLOT_COUNT || tierOrdinal < 0 || tierOrdinal >= CyberwareTier.values().length) {
                continue;
            }

            supportedTiers[slot] = CyberwareTier.values()[tierOrdinal];
        }
    }

    public CyberwareTier getSupportedTier(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? supportedTiers[slot] : CyberwareTier.TIER_1;
    }

    public boolean canUpgradeSupportedTier(int slot) {
        return slot >= 0 && slot < SLOT_COUNT
                && getStackInSlot(slot).isEmpty()
                && supportedTiers[slot] != CyberwareTier.TIER_5
                && getRequiredUpgradeComponent(supportedTiers[slot]) != null;
    }

    public CyberwareTier getNextSupportedTier(int slot) {
        CyberwareTier currentTier = getSupportedTier(slot);
        return currentTier == CyberwareTier.TIER_5 ? CyberwareTier.TIER_5 : CyberwareTier.values()[currentTier.ordinal() + 1];
    }

    public ItemStack getRequiredUpgradeComponentStack(int slot) {
        return getRequiredUpgradeComponentStack(getSupportedTier(slot));
    }

    public boolean hasRequiredUpgradeComponent(int slot) {
        ItemStack required = getRequiredUpgradeComponentStack(slot);
        return hasRequiredUpgradeComponent(required);
    }

    public boolean tryUpgradeSupportedTier(int slot) {
        if (!canUpgradeSupportedTier(slot)) {
            return false;
        }

        ItemStack required = getRequiredUpgradeComponentStack(slot);
        if (required.isEmpty() || (!hasCreativeUpgradeBypass() && !consumeRequiredUpgradeComponent(required))) {
            return false;
        }

        supportedTiers[slot] = getNextSupportedTier(slot);
        save();
        return true;
    }

    public boolean hasRequiredUpgradeComponent(ItemStack required) {
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || countMatchingInventoryItems(required) >= required.getCount());
    }

    public boolean consumeRequiredUpgradeComponent(ItemStack required) {
        return !required.isEmpty() && (hasCreativeUpgradeBypass() || consumeMatchingInventoryItem(required));
    }

    public static ItemStack getRequiredUpgradeComponentStack(CyberwareTier currentTier) {
        var component = getRequiredUpgradeComponent(currentTier);
        return component != null ? component.get().getDefaultInstance().copyWithCount(4) : ItemStack.EMPTY;
    }

    private int countMatchingInventoryItems(ItemStack required) {
        int total = 0;
        for (int inventorySlot = 0; inventorySlot < player.getInventory().getContainerSize(); inventorySlot++) {
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            if (ItemStack.isSameItemSameComponents(stack, required)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean consumeMatchingInventoryItem(ItemStack required) {
        int missing = required.getCount();
        if (countMatchingInventoryItems(required) < missing) return false;
        for (int inventorySlot = 0; inventorySlot < player.getInventory().getContainerSize() && missing > 0; inventorySlot++) {
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            if (!ItemStack.isSameItemSameComponents(stack, required)) continue;
            int used = Math.min(missing, stack.getCount());
            stack.shrink(used);
            missing -= used;
            if (stack.isEmpty()) player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
        return true;
    }

    private boolean hasCreativeUpgradeBypass() {
        return player.getAbilities().instabuild;
    }

    private static net.neoforged.neoforge.registries.DeferredItem<?> getRequiredUpgradeComponent(CyberwareTier currentTier) {
        return switch (currentTier) {
            case TIER_1 -> ModItems.UNCOMMON_ITEM_COMPONENTS;
            case TIER_2 -> ModItems.RARE_ITEM_COMPONENTS;
            case TIER_3 -> ModItems.EPIC_ITEM_COMPONENTS;
            case TIER_4 -> ModItems.LEGENDARY_ITEM_COMPONENTS;
            case TIER_5 -> null;
        };
    }

    public int getInstalledChromeCost() {
        int total = 0;
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.getItem() instanceof CyberwareItem cyberwareItem) {
                total += cyberwareItem.getChromeCost();
            }
        }
        for (CyberwareModuleItem moduleItem : getInstalledModuleItems()) {
            total += moduleItem.getDefinition().chromeCost();
        }
        for (ChipwareItem chipwareItem : getInstalledChipwareItems()) {
            total += chipwareItem.getDefinition().chromeCost();
        }
        return total;
    }

    public int getInstalledCount() {
        int total = 0;
        for (int slot = 0; slot < getSlots(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                total++;
            }
        }
        return total;
    }

    public List<CyberwareItem> getInstalledCyberwareItems() {
        List<CyberwareItem> installed = new ArrayList<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            if (getStackInSlot(slot).getItem() instanceof CyberwareItem cyberwareItem) {
                installed.add(cyberwareItem);
            }
        }
        return installed;
    }

    public List<CyberwareModuleItem> getInstalledModuleItems() { return moduleItems(false); }
    public List<CyberwareModuleItem> getOperationalModuleItems() { return moduleItems(true); }
    private List<CyberwareModuleItem> moduleItems(boolean operationalOnly) {
        List<CyberwareModuleItem> installed = new ArrayList<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            CyberwareDefinition definition = getInstalledDefinition(slot);
            if (definition == null || operationalOnly && CyberwareConditionHelper.hasNeuralFault(getStackInSlot(slot)) || !definition.supportsModules()) {
                continue;
            }

            CyberwareModuleHandler moduleHandler = new CyberwareModuleHandler(this, player, slot, definition.moduleCategory());
            for (int moduleSlot = 0; moduleSlot < moduleHandler.getUnlockedSlotCount(); moduleSlot++) {
                ItemStack moduleStack = moduleHandler.getStackInSlot(moduleSlot);
                if (moduleStack.getItem() instanceof CyberwareModuleItem moduleItem) {
                    installed.add(moduleItem);
                }
            }
        }
        return installed;
    }

    public List<ChipwareItem> getInstalledChipwareItems() { return chipwareItems(false); }
    public List<ChipwareItem> getOperationalChipwareItems() { return chipwareItems(true); }
    private List<ChipwareItem> chipwareItems(boolean operationalOnly) {
        List<ChipwareItem> installed = new ArrayList<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            CyberwareDefinition definition = getInstalledDefinition(slot);
            if (definition == null || operationalOnly && CyberwareConditionHelper.hasNeuralFault(getStackInSlot(slot)) || !definition.supportsChipware()) {
                continue;
            }

            ChipwareSocketHandler chipHandler = new ChipwareSocketHandler(this, player, slot);
            for (int chipSlot = 0; chipSlot < chipHandler.getUnlockedSlotCount(); chipSlot++) {
                ItemStack chipStack = chipHandler.getStackInSlot(chipSlot);
                if (chipStack.getItem() instanceof ChipwareItem chipwareItem) {
                    installed.add(chipwareItem);
                }
            }
        }
        return installed;
    }

    public CyberwareItem getInstalledCyberwareBySlotType(CyberwareSlotType slotType) {
        for (int slot = 0; slot < getSlots(); slot++) {
            if (getStackInSlot(slot).getItem() instanceof CyberwareItem cyberwareItem && cyberwareItem.getSlotType() == slotType && !CyberwareConditionHelper.hasNeuralFault(getStackInSlot(slot))) {
                return cyberwareItem;
            }
        }
        return null;
    }

    public boolean hasInstalledCyberware(String definitionId) {
        return countInstalledCyberware(definitionId) > 0;
    }

    public int countInstalledCyberware(String definitionId) {
        int count = 0;
        for (int slot = 0; slot < getSlots(); slot++) {
            if (getStackInSlot(slot).getItem() instanceof CyberwareItem cyberwareItem
                    && cyberwareItem.getDefinition().id().equals(definitionId) && !CyberwareConditionHelper.hasNeuralFault(getStackInSlot(slot))) {
                count++;
            }
        }
        return count;
    }

    public int getChromeCapacity() {
        int capacity = BASE_CHROME_CAPACITY + de.artemis.cyberneticenhancements.common.tarot.TarotRewards.chromeHeadroom(player);
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.getItem() instanceof CyberwareItem cyberwareItem) {
                capacity += cyberwareItem.getCapacityBonus(stack);
            }
        }
        return de.artemis.cyberneticenhancements.common.skills.SkillEffects.capacity(player, capacity) + (int) Math.round(getChromeCapacityEffectBonus());
    }

    public int getCyberstrain() {
        return Math.max(0, getInstalledChromeCost() - getChromeCapacity());
    }

    public int getIntegrityRating() {
        int installed = 0;
        double totalRatio = 0.0D;
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (!(stack.getItem() instanceof CyberwareItem cyberwareItem)) {
                continue;
            }

            installed++;
            totalRatio += CyberwareConditionHelper.getIntegrityRatio(stack, cyberwareItem.getDefinition());
        }

        if (installed == 0) {
            return 100;
        }
        return (int) Math.round((totalRatio / installed) * 100.0D);
    }

    public int getChromePercent() {
        if (getChromeCapacity() <= 0) {
            return 0;
        }
        return Math.min(999, getInstalledChromeCost() * 100 / getChromeCapacity());
    }

    public int getStageIndex() {
        double ratio = getInstalledChromeCapacityRatio();
        if (ratio < 0.20D) {
            return 0;
        }
        if (ratio < 0.40D) {
            return 1;
        }
        if (ratio < 0.70D) {
            return 2;
        }
        if (ratio < 0.95D) {
            return 3;
        }
        return 4;
    }

    private double getInstalledChromeCapacityRatio() {
        return getChromeCapacity() <= 0 ? 0.0D : (double) getInstalledChromeCost() / (double) getChromeCapacity();
    }

    private double getChromeCapacityEffectBonus() {
        double capacityBonus = 0.0D;
        long gameTime = player.level().getGameTime();

        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (!(stack.getItem() instanceof CyberwareItem cyberwareItem)) {
                continue;
            }

            double integrityScale = CyberwareConditionHelper.getPerformanceRatio(stack, cyberwareItem.getDefinition());
            capacityBonus += getEffectAmount(cyberwareItem.getEffects(stack), CyberwareEffectType.CHROME_CAPACITY) * integrityScale;
        }
        for (CyberwareModuleItem moduleItem : getOperationalModuleItems()) {
            capacityBonus += getEffectAmount(moduleItem.getDefinition().effects(), CyberwareEffectType.CHROME_CAPACITY);
        }
        for (ChipwareItem chipwareItem : getOperationalChipwareItems()) {
            capacityBonus += getEffectAmount(chipwareItem.getDefinition().effects(), CyberwareEffectType.CHROME_CAPACITY);
        }

        capacityBonus += TemporaryCyberwareEffectManager.getActiveAmount(player, CyberwareEffectType.CHROME_CAPACITY, gameTime);
        return capacityBonus;
    }

    private static double getEffectAmount(Iterable<CyberwareEffect> effects, CyberwareEffectType targetType) {
        double total = 0.0D;
        for (CyberwareEffect effect : effects) {
            if (effect.type() == targetType) {
                total += effect.amount();
            }
        }
        return total;
    }

    private CyberwareDefinition getInstalledDefinition(int slot) {
        ItemStack stack = getStackInSlot(slot);
        return stack.getItem() instanceof CyberwareItem cyberwareItem ? cyberwareItem.getDefinition() : null;
    }
}
