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
    private final Container inputs;
    public final TechStationBlockEntity station;
    private final ContainerData progressData;
    private final int materialCount, machineSlots;

    public TechStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public TechStationMenu(int id, Inventory inventory, BlockPos pos) {
        super(inventory.player.level().getBlockState(pos).is(ModBlocks.UPGRADE_STATION.get()) ? ModMenuTypes.UPGRADE_STATION.get() : ModMenuTypes.TECH_STATION.get(), id);
        player = inventory.player;
        blockPos = pos.immutable();
        upgrading = player.level().getBlockState(pos).is(ModBlocks.UPGRADE_STATION.get());
        materialCount = 2;
        machineSlots = materialCount + 1;
        station = player.level().getBlockEntity(pos) instanceof TechStationBlockEntity found ? found : null;
        if (station == null) throw new IllegalArgumentException("Service station missing");
        inputs = station;
        progressData = player.level().isClientSide ? new SimpleContainerData(2) : new ContainerData() {
            public int get(int i) { return i == 0 ? station.progress : station.working() ? 1 : 0; }
            public void set(int i, int value) {}
            public int getCount() { return 2; }
        };
        addDataSlots(progressData);
        addSlot(new Slot(inputs, 0, UpgradeStationLayout.INPUT_X, UpgradeStationLayout.INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !isWorking() && stack.getItem() instanceof CyberwareItem;
            }

            @Override public boolean mayPickup(Player player) { return !isWorking(); }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < materialCount; i++) {
            final int materialIndex = i;
            addSlot(new Slot(inputs, i + 1, UpgradeStationLayout.MATERIAL_X[i], UpgradeStationLayout.MATERIAL_Y[i]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    var requirements = plan().materials();
                    return !isWorking() && materialIndex < requirements.size()
                            && ItemStack.isSameItemSameComponents(stack, requirements.get(materialIndex).stack());
                }
                @Override public boolean mayPickup(Player player) { return !isWorking(); }
            });
        }
        var layout = UpgradeStationLayout.PLAYER_INVENTORY;
        addPlayerInventorySlots(inventory, layout.inventoryX(), layout.inventoryY());
        addPlayerHotbarSlots(inventory, layout.inventoryX(), layout.hotbarY());
    }

    public boolean isWorking() { return progressData.get(1) != 0; }
    public int progress() { return progressData.get(0); }

    public boolean isUpgrading() {
        return upgrading;
    }

    public ItemStack getInputStack() {
        return inputs.getItem(0);
    }

    public boolean transferUpgrade(net.minecraft.server.level.ServerPlayer actor, net.minecraft.resources.ResourceLocation item, int level, boolean maximum) {
        if (actor != player || actor.containerMenu != this || !stillValid(actor)) return false;
        return apply(actor, UpgradeRecipeTransfer.prepare(this, actor, item, level, maximum));
    }

    public boolean transferRepair(net.minecraft.server.level.ServerPlayer actor, net.minecraft.resources.ResourceLocation item, RepairRecipeTransfer.Kind kind, boolean maximum) {
        if (actor != player || actor.containerMenu != this || !stillValid(actor)) return false;
        return apply(actor, RepairRecipeTransfer.prepare(this, actor, item, kind, maximum));
    }

    private boolean apply(net.minecraft.server.level.ServerPlayer actor, UpgradeRecipeTransfer.Plan transfer) {
        if (transfer.error() != UpgradeRecipeTransfer.Error.NONE) return false;
        for (int i = 0; i < machineSlots; i++) inputs.setItem(i, transfer.machine().get(i));
        for (int i = 0; i < 36; i++) actor.getInventory().setItem(i, transfer.inventory().get(i));
        inputs.setChanged();
        actor.getInventory().setChanged();
        broadcastChanges();
        return true;
    }

    public int machineSlotCount() { return machineSlots; }

    public int materialCount() {
        return materialCount;
    }

    public ItemStack getMaterialStack(int index) {
        return inputs.getItem(index + 1);
    }

    public CyberwareTier tier() {
        return CyberwareTier.TIER_5;
    }

    public CyberwareServicePlan plan() {
        return station.plan();
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
        return !isWorking() && acceptsTier() && CyberwareServiceHelper.matchesMaterials(plan(), materials());
    }

    public boolean canFillMaterials() {
        if (isWorking() || !acceptsTier()) return false;
        var service = plan();
        if (!service.isAvailable()) return false;
        var targets = new java.util.ArrayList<RecipeSlotTransfer.FillTarget>();
        for (int i = 0; i < service.materials().size(); i++) {
            var required = service.materials().get(i);
            targets.add(new RecipeSlotTransfer.FillTarget(slots.get(i + 1), required.count(),
                    stack -> ItemStack.isSameItemSameComponents(stack, required.stack())));
        }
        return RecipeSlotTransfer.canFill(player.getInventory(), targets, player.isCreative());
    }

    public net.minecraft.network.chat.Component status() {
        String key;
        if (isWorking()) key = upgrading ? "upgrading" : "repairing";
        else if (getInputStack().isEmpty()) key = upgrading ? "insert_upgrade" : "insert_repair";
        else if (!acceptsTier()) key = "tier_locked";
        else if (!plan().isAvailable()) key = upgrading ? "max_upgrade" : "no_repair";
        else if (!canService()) key = "missing_materials";
        else key = upgrading ? "ready_upgrade" : CyberwareConditionHelper.hasNeuralFault(getInputStack()) ? "repair_fault_gain" : "ready_repair";
        return net.minecraft.network.chat.Component.translatable("screen.cyberneticenhancements.workshop." + key);
    }
    @Override
    public boolean clickMenuButton(Player sender, int button) {
        if (player.level().isClientSide || sender != player || !stillValid(sender)) return false;
        if (button == 4) {
            station.stop();
            broadcastChanges();
            return true;
        }
        if (button == 3 || isWorking()) return false;
        if (button == 1) {
            if (!canFillMaterials()) return false;
            fillMaterials();
            return true;
        }
        if (button == 2) {
            for (int i = 1; i <= materialCount; i++)
                player.getInventory().placeItemBackInInventory(inputs.removeItemNoUpdate(i));
            inputs.setChanged();
            broadcastChanges();
            return true;
        }
        if (button != 0 || !canService()) return false;
        boolean started = sender instanceof net.minecraft.server.level.ServerPlayer server && station.start(server);
        broadcastChanges();
        return started;
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
            if (player.isCreative()) {
                if (missing > 0) inputs.setItem(index + 1, (target.isEmpty() ? requirement.stack() : target).copyWithCount(requirement.count()));
                continue;
            }
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
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var copy = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (stack.getItem() instanceof CyberwareItem) moved = moveItemStackTo(stack, 0, 1, false);
            else {
                var materials = plan().materials();
                var targets = new java.util.ArrayList<RecipeSlotTransfer.Target>();
                for (int material = 0; material < Math.min(materialCount, materials.size()); material++)
                    if (ItemStack.isSameItemSameComponents(stack, materials.get(material).stack()))
                        targets.add(new RecipeSlotTransfer.Target(slots.get(material + 1), materials.get(material).count()));
                RecipeSlotTransfer.distributeRequired(stack, targets);
                RecipeSlotTransfer.distributeExcess(stack, targets);
                moved = stack.getCount() != copy.getCount();
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
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, upgrading ? ModBlocks.UPGRADE_STATION.get() : ModBlocks.TECH_STATION.get())
                && player.level().getBlockEntity(blockPos) == station;
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
