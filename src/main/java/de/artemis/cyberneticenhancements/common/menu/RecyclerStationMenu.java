package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareRecyclePlan;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import static de.artemis.cyberneticenhancements.common.menu.RecyclerStationLayout.*;

public final class RecyclerStationMenu extends AbstractBaseMenu implements NamedBlockMenu {
    public final RecyclerStationBlockEntity station;
    private final Player player;
    private final ContainerData data;

    public RecyclerStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public RecyclerStationMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenuTypes.RECYCLER_STATION.get(), id);
        player = inventory.player;
        if (!(player.level().getBlockEntity(pos) instanceof RecyclerStationBlockEntity found))
            throw new IllegalArgumentException("Recycler missing");
        station = found;
        data = player.level().isClientSide ? new SimpleContainerData(2) : new ContainerData() {
            public int get(int i) { return i == 0 ? station.progress : station.working() ? 1 : 0; }
            public void set(int i, int value) {}
            public int getCount() { return 2; }
        };
        addDataSlots(data);
        addSlot(new Slot(station, 0, INPUT_X, INPUT_Y) {
            public boolean mayPlace(ItemStack stack) { return !isWorking() && station.recyclable(stack); }
            public boolean mayPickup(Player player) { return !isWorking(); }
        });
        for (int i = 0; i < RESULT_SLOT_COUNT; i++)
            addSlot(new Slot(station, i + 1, resultX(i), resultY(i)) {
                public boolean mayPlace(ItemStack stack) { return false; }
            });
        addPlayerInventorySlots(inventory, PLAYER_INVENTORY.inventoryX(), PLAYER_INVENTORY.inventoryY());
        addPlayerHotbarSlots(inventory, PLAYER_INVENTORY.inventoryX(), PLAYER_INVENTORY.hotbarY());
    }
    public boolean isWorking() { return data.get(1) != 0; }
    public int progress() { return data.get(0); }
    public boolean canRecycle() { return !isWorking() && station.canStart(); }
    public ItemStack getInputStack() { return station.getItem(0); }
    public CyberwareRecyclePlan getRecyclePlan() { return station.plan(); }
    public CyberwareTier getSupportedTier() { return CyberwareTier.TIER_5; }
    public boolean hasPendingResultStacks() {
        return java.util.stream.IntStream.range(1, MACHINE_SLOT_COUNT).anyMatch(i -> !station.getItem(i).isEmpty());
    }
    public int outputCount() {
        return java.util.stream.IntStream.range(1, MACHINE_SLOT_COUNT).map(i -> station.getItem(i).getCount()).sum();
    }
    public List<ItemStack> getDisplayedResultStacks() { return getRecyclePlan().outputs(); }
    public Component status() {
        String key = isWorking() ? "recycling" : getInputStack().isEmpty() ? "insert_scrap"
                : !getRecyclePlan().isAvailable() ? "no_salvage" : !canRecycle() ? "recovery_full" : "ready_recycle";
        return Component.translatable("screen.cyberneticenhancements.workshop." + key);
    }
    @Override public boolean clickMenuButton(Player actor, int button) {
        if (actor != player || !(actor instanceof ServerPlayer server) || !stillValid(actor)) return false;
        if (button == 2) { station.stop(); broadcastChanges(); return true; }
        if (isWorking()) return false;
        boolean changed = false;
        if (button == 1) changed = station.start(server);
        broadcastChanges();
        return changed;
    }
    public boolean transferRecipe(ServerPlayer actor, net.minecraft.resources.ResourceLocation recipe, boolean maximum) {
        if (actor != player || actor.containerMenu != this || !stillValid(actor)) return false;
        var plan = RecyclerRecipeTransfer.prepare(this, actor, recipe, maximum);
        if (plan.error() != RecyclerRecipeTransfer.Error.NONE) return false;
        station.setItem(0, plan.input());
        for (int i = 0; i < 36; i++) actor.getInventory().setItem(i, plan.inventory().get(i));
        actor.getInventory().setChanged();
        station.setChanged();
        broadcastChanges();
        return true;
    }
    @Override public ItemStack quickMoveStack(Player actor, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(actor)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved;
        if (index < MACHINE_SLOT_COUNT) moved = moveItemStackTo(stack, MACHINE_SLOT_COUNT, slots.size(), true);
        else if (!isWorking() && station.recyclable(stack) && moveItemStackTo(stack, 0, 1, false)) moved = true;
        else {
            int hotbar = MACHINE_SLOT_COUNT + 27;
            moved = index < hotbar ? moveItemStackTo(stack, hotbar, slots.size(), false)
                    : moveItemStackTo(stack, MACHINE_SLOT_COUNT, hotbar, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(actor, stack);
        return original;
    }
    @Override public boolean stillValid(Player actor) {
        return stillValid(ContainerLevelAccess.create(actor.level(), getBlockPos()), actor, ModBlocks.RECYCLER_STATION.get())
                && actor.level().getBlockEntity(getBlockPos()) == station;
    }
    @Override public BlockPos getBlockPos() { return station.getBlockPos(); }
    @Override public String getBlockDisplayName() { return ModBlocks.RECYCLER_STATION.get().getName().getString(); }
}
