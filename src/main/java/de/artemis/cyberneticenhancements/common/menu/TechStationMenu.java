package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.blockentity.TechStationBlockEntity;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class TechStationMenu extends AbstractBaseMenu implements NamedBlockMenu {
    private final Player player;
    private final BlockPos blockPos;
    private final boolean upgrading;
    private final Container inputs, output = new SimpleContainer(1);
    private final int materialCount, machineSlots;
    private int supportedTierClient;

    public TechStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public TechStationMenu(int id, Inventory inventory, BlockPos pos) {
        super(inventory.player.level().getBlockState(pos).is(ModBlocks.UPGRADE_STATION.get()) ? ModMenuTypes.UPGRADE_STATION.get() : ModMenuTypes.TECH_STATION.get(), id);
        player = inventory.player;
        blockPos = pos.immutable();
        upgrading = player.level().getBlockState(pos).is(ModBlocks.UPGRADE_STATION.get());
        materialCount = 2;
        machineSlots = materialCount + 2;
        inputs = new SimpleContainer(materialCount + 1);
        if (!player.level().isClientSide && player.level().getBlockEntity(pos) instanceof TechStationBlockEntity station) {

            PlayerStationUpgradeData.migrateTechRepairTier(player, station.getSupportedTierOrdinal(TechStationBlockEntity.REPAIR_INPUT_SLOT));
            PlayerStationUpgradeData.migrateTechUpgradeTier(player, station.getSupportedTierOrdinal(TechStationBlockEntity.UPGRADE_INPUT_SLOT));
        }
        addSlot(new Slot(inputs, 0, TechStationLayout.INPUT_X, TechStationLayout.INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CyberwareItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < materialCount; i++) {
            final int index = i;
            addSlot(new Slot(inputs, i + 1, TechStationLayout.MATERIAL_X, TechStationLayout.MATERIAL_Y[i]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    var requirements = plan().materials();
                    return index < requirements.size() && ItemStack.isSameItemSameComponents(stack, requirements.get(index).stack());
                }
            });
        }
        addSlot(new Slot(output, 0, TechStationLayout.RESULT_X, TechStationLayout.RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventorySlots(inventory, TechStationLayout.PLAYER_INVENTORY_X, TechStationLayout.PLAYER_INVENTORY_Y);
        addPlayerHotbarSlots(inventory, TechStationLayout.PLAYER_INVENTORY_X, TechStationLayout.PLAYER_HOTBAR_Y);
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return player.level().isClientSide ? supportedTierClient : tier().ordinal();
            }

            @Override
            public void set(int value) {
                supportedTierClient = value;
            }
        });
    }

    public boolean isUpgrading() {
        return upgrading;
    }

    public ItemStack getInputStack() {
        return inputs.getItem(0);
    }

    public ItemStack getOutputStack() {
        return output.getItem(0);
    }

    public int materialCount() {
        return materialCount;
    }

    public ItemStack getMaterialStack(int index) {
        return inputs.getItem(index + 1);
    }

    public CyberwareTier tier() {
        return player.level().isClientSide ? TechStationBlockEntity.getTierByOrdinal(supportedTierClient)
                : upgrading ? PlayerStationUpgradeData.getTechUpgradeTier(player) : PlayerStationUpgradeData.getTechRepairTier(player);
    }

    public CyberwareServicePlan plan() {
        return upgrading ? CyberwareServiceHelper.getUpgradePlan(getInputStack()) : CyberwareServiceHelper.getRepairPlan(getInputStack());
    }

    public boolean hasPendingOutput() {
        return !getOutputStack().isEmpty();
    }

    private boolean acceptsTier() {
        return StationSlotTierHelper.canAccept(getInputStack(), tier());
    }

    private ItemStack[] materials() {
        ItemStack[] stacks = new ItemStack[materialCount];
        for (int i = 0; i < materialCount; i++) stacks[i] = getMaterialStack(i);
        return stacks;
    }

    public boolean canService() {
        return !hasPendingOutput() && acceptsTier() && CyberwareServiceHelper.matchesMaterials(plan(), materials());
    }

    public boolean canFillMaterials() {
        return !hasPendingOutput() && acceptsTier() && plan().isAvailable();
    }

    public net.minecraft.network.chat.Component status() {
        String key;
        if (hasPendingOutput()) key = "collect_implant";
        else if (getInputStack().isEmpty()) key = upgrading ? "insert_upgrade" : "insert_repair";
        else if (!acceptsTier()) key = "tier_locked";
        else if (!plan().isAvailable()) key = upgrading ? "max_upgrade" : "no_repair";
        else if (!canService()) key = "missing_materials";
        else key = upgrading ? "ready_upgrade" : "ready_repair";
        return net.minecraft.network.chat.Component.translatable("screen.cyberneticenhancements.workshop." + key);
    }
    @Override
    public boolean clickMenuButton(Player sender, int button) {
        if (player.level().isClientSide || sender != player || !stillValid(sender)) return false;
        if (button == 3) return tryUpgradeCapacity();
        if (button == 1) {
            fillMaterials();
            return true;
        }
        if (button == 2) {
            for (int i = 1; i <= materialCount; i++)
                player.getInventory().placeItemBackInInventory(inputs.removeItemNoUpdate(i));
            broadcastChanges();
            return true;
        }
        if (button != 0 || !canService()) return false;
        CyberwareServicePlan service = plan();

        for (var requirement : CyberwareServiceHelper.getRequiredMaterials(service)) {
            int remaining = requirement.count();
            for (int i = 1; i <= materialCount && remaining > 0; i++) {
                ItemStack stack = inputs.getItem(i);
                if (!ItemStack.isSameItemSameComponents(stack, requirement.stack())) continue;
                int take = Math.min(remaining, stack.getCount());
                inputs.removeItem(i, take);
                remaining -= take;
            }
        }
        inputs.removeItem(0, 1);
        output.setItem(0, service.output().copy());
        player.playNotifySound(net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, upgrading ? 1.2F : 1.0F);
        broadcastChanges();
        return true;
    }

    public int materialTotal(ItemStack required) {
        int total = 0;
        for (int i = 1; i <= materialCount; i++)
            if (ItemStack.isSameItemSameComponents(inputs.getItem(i), required)) total += inputs.getItem(i).getCount();
        return total;
    }

    private void fillMaterials() {
        if (!canFillMaterials()) return;
        var requirements = plan().materials();
        for (int index = 0; index < requirements.size(); index++) {
            var requirement = requirements.get(index);
            ItemStack target = inputs.getItem(index + 1);
            if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, requirement.stack())) continue;
            int missing = requirement.count() - target.getCount();
            for (int slot = 0; slot < 36 && missing > 0; slot++) {
                ItemStack source = player.getInventory().getItem(slot);
                if (!ItemStack.isSameItemSameComponents(source, requirement.stack())) continue;
                int moved = Math.min(missing, Math.min(source.getCount(), source.getMaxStackSize() - target.getCount()));
                if (moved <= 0) continue;
                if (target.isEmpty()) {
                    target = source.copyWithCount(moved);
                    inputs.setItem(index + 1, target);
                } else target.grow(moved);
                source.shrink(moved);
                missing -= moved;
            }
        }
        inputs.setChanged();
        player.getInventory().setChanged();
        broadcastChanges();
    }
    public ItemStack capacityCost() {
        return TechStationBlockEntity.getRequiredUpgradeComponentStack(tier());
    }

    public boolean canUpgradeCapacity() {
        return getInputStack().isEmpty() && tier() != CyberwareTier.TIER_5;
    }

    public boolean hasCapacityMaterial() {
        var required = capacityCost();
        if (required.isEmpty()) return false;
        if (player.getAbilities().instabuild) return true;
        for (int i = 0; i < 36; i++)
            if (ItemStack.isSameItemSameComponents(player.getInventory().getItem(i), required)) return true;
        return false;
    }

    public boolean tryUpgradeCapacity() {
        if (player.level().isClientSide || !canUpgradeCapacity() || !hasCapacityMaterial()) return false;
        boolean upgraded = upgrading ? PlayerStationUpgradeData.tryUpgradeTechUpgradeTier(player) : PlayerStationUpgradeData.tryUpgradeTechRepairTier(player);
        if (!upgraded) return false;
        if (!player.getAbilities().instabuild) {

            ItemStack required = TechStationBlockEntity.getRequiredUpgradeComponentStack(TechStationBlockEntity.getTierByOrdinal(tier().ordinal() - 1));
            for (int i = 0; i < 36; i++) {
                var stack = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(stack, required)) {
                    stack.shrink(1);
                    break;
                }
            }
            player.getInventory().setChanged();
        }
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var copy = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof CyberwareItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (var requirement : CyberwareServiceHelper.getRequiredMaterials(plan())) {
                if (ItemStack.isSameItemSameComponents(stack, requirement.stack()))
                    moved |= moveItemStackTo(stack, 1, materialCount + 1, false);
            }
            if (!moved) {
                int hotbar = machineSlots + 27;
                if (!(index < hotbar ? moveItemStackTo(stack, hotbar, slots.size(), false) : moveItemStackTo(stack, machineSlots, hotbar, false)))
                    return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, upgrading ? ModBlocks.UPGRADE_STATION.get() : ModBlocks.TECH_STATION.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, inputs);
        clearContainer(player, output);
    }

    @Override
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public String getBlockDisplayName() {
        return (upgrading ? ModBlocks.UPGRADE_STATION : ModBlocks.TECH_STATION).get().getName().getString();
    }
}
