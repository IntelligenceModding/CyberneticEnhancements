package de.artemis.cyberneticenhancements.common.blockentity;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class UpgradeableStationBlockEntity extends BlockEntity {
    private static final String SUPPORTED_TIERS_TAG = "SupportedTiers";

    private final int[] supportedTierOrdinals;

    protected UpgradeableStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int slotCount) {
        super(type, pos, blockState);
        this.supportedTierOrdinals = new int[slotCount];
    }

    public int getSlotCount() {
        return supportedTierOrdinals.length;
    }

    public CyberwareTier getSupportedTier(int slotIndex) {
        return getTierByOrdinal(getSupportedTierOrdinal(slotIndex));
    }

    public int getSupportedTierOrdinal(int slotIndex) {
        return isValidSlot(slotIndex) ? supportedTierOrdinals[slotIndex] : CyberwareTier.TIER_1.ordinal();
    }

    public CyberwareTier getNextSupportedTier(int slotIndex) {
        CyberwareTier current = getSupportedTier(slotIndex);
        return current.next();
    }

    public boolean canUpgradeSupportedTier(int slotIndex) {
        return isValidSlot(slotIndex) && getSupportedTier(slotIndex) != CyberwareTier.TIER_5;
    }

    public boolean tryUpgradeSupportedTier(int slotIndex) {
        if (!canUpgradeSupportedTier(slotIndex)) {
            return false;
        }

        supportedTierOrdinals[slotIndex] = getNextSupportedTier(slotIndex).ordinal();
        setChanged();
        return true;
    }

    public static CyberwareTier getTierByOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= CyberwareTier.values().length) {
            return CyberwareTier.TIER_1;
        }
        return CyberwareTier.values()[ordinal];
    }

    public static net.minecraft.world.item.ItemStack getRequiredUpgradeComponentStack(CyberwareTier currentTier) {
        return PlayerCyberwareInventory.getRequiredUpgradeComponentStack(currentTier);
    }

    private boolean isValidSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < supportedTierOrdinals.length;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray(SUPPORTED_TIERS_TAG, supportedTierOrdinals);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] stored = tag.getIntArray(SUPPORTED_TIERS_TAG);
        for (int index = 0; index < supportedTierOrdinals.length; index++) {
            supportedTierOrdinals[index] = index < stored.length ? stored[index] : CyberwareTier.TIER_1.ordinal();
        }
    }
}
