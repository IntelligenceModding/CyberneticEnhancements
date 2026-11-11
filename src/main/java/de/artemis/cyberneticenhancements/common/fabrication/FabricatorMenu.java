package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.*;

public final class FabricatorMenu extends AbstractContainerMenu {
    public final FabricatorBlockEntity station;
    private final ContainerData data;
    private final Player player;

    public FabricatorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buffer) {
        this(id, inv, buffer.readBlockPos());
    }

    public FabricatorMenu(int id, Inventory inv, BlockPos pos) {
        super(ModMenuTypes.FABRICATOR.get(), id);
        player = inv.player;
        if (!(inv.player.level().getBlockEntity(pos) instanceof FabricatorBlockEntity found))
            throw new IllegalArgumentException("Fabricator missing");
        station = found;
        data = inv.player.level().isClientSide ? new SimpleContainerData(4) : new ContainerData() {
            public int get(int i) {
                return switch (i) {
                    case 0 -> station.capability;
                    case 1 -> station.progress;
                    case 2 -> selectedIndex();
                    default -> station.working() ? 1 : 0;
                };
            }

            public void set(int i, int v) {
            }

            public int getCount() {
                return 4;
            }
        };
        addDataSlots(data);
        for (int row = 0; row < 5; row++)
            for (int col = 0; col < 5; col++)
                addSlot(new Slot(station, row * 5 + col, FabricatorLayout.GRID_X + col * FabricatorLayout.GRID_STEP, FabricatorLayout.GRID_Y + row * FabricatorLayout.GRID_STEP) {
                    public boolean mayPlace(ItemStack stack) {
                        return !isWorking();
                    }

                    public boolean mayPickup(Player p) {
                        return !isWorking();
                    }
                });
        addSlot(new Slot(station, 26, FabricatorLayout.OUTPUT_X, FabricatorLayout.OUTPUT_Y) {
            public boolean mayPlace(ItemStack s) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, FabricatorLayout.PLAYER_X + col * 18, FabricatorLayout.PLAYER_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, FabricatorLayout.PLAYER_X + col * 18, FabricatorLayout.HOTBAR_Y));
    }

    private int selectedIndex() {
        var list = station.recipes();
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().toString().equals(station.selected)) return i;
        return -1;
    }

    public List<RecipeHolder<FabricationRecipe>> recipes() {
        return station.recipes();
    }

    public List<RecipeHolder<FabricationRecipe>> availableRecipes() {
        return recipes().stream().filter(holder -> holder.value().unlockedFor(player)).toList();
    }

    public RecipeHolder<FabricationRecipe> selected() {
        var l = recipes();
        int i = data.get(2);
        return i >= 0 && i < l.size() && l.get(i).value().unlockedFor(player) ? l.get(i) : null;
    }

    public boolean isWorking() {
        return data.get(3) == 1;
    }

    public int progress() {
        return data.get(1);
    }

    public int capability() {
        return data.get(0);
    }

    public boolean stillValid(Player p) {
        return station.stillValid(p);
    }

    public boolean clickMenuButton(Player p, int button) {
        if (p != player || !(p instanceof ServerPlayer server) || !stillValid(p)) return false;
        if (button == 2) {
            station.cancel();
            return true;
        }
        if (isWorking()) return false;
        if (button >= 100) {
            var l = recipes();
            int i = button - 100;
            if (i >= l.size() || !l.get(i).value().unlockedFor(p)) return false;
            station.selected = l.get(i).id().toString();
            station.setChanged();
            return true;
        }
        switch (button) {
            case 0 -> { if (!fill(server)) return false; }
            case 1 -> { if (!station.start(server)) return false; }
            case 4 -> {
                for (int i = 0; i < 25; i++) {
                    var s = station.removeItemNoUpdate(i);
                    if (!s.isEmpty()) p.getInventory().placeItemBackInInventory(s);
                }
                station.setChanged();
            }
            default -> {
                return false;
            }
        }
        broadcastChanges();
        return true;
    }

    private boolean fill(ServerPlayer p) {
        var holder = station.selectedRecipe();
        if (holder == null) return false;
        var r = holder.value();
        if (!r.unlockedFor(p)) return false;
        for (int slot = 0; slot < 25; slot++) {
            int index = slot;
            var part = r.parts().stream().filter(v -> v.slot() == index).findFirst().orElse(null);
            var s = station.getItem(slot);
            if (!s.isEmpty() && (part == null || !part.ingredient().test(s))) {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("Clear incompatible grid items first."), true);
                return false;
            }
        }
        for (var part : r.parts()) {
            var target = station.getItem(part.slot());
            int missing = part.count() - target.getCount();
            for (int i = 0; i < 36 && missing > 0; i++) {
                var source = p.getInventory().getItem(i);
                if (!part.ingredient().test(source) || !target.isEmpty() && !ItemStack.isSameItemSameComponents(target, source))
                    continue;
                int moved = Math.min(missing, Math.min(source.getCount(), source.getMaxStackSize() - target.getCount()));
                if (moved <= 0) continue;
                if (target.isEmpty()) {
                    target = source.copyWithCount(moved);
                    station.setItem(part.slot(), target);
                } else target.grow(moved);
                source.shrink(moved);
                missing -= moved;
            }
        }
        station.setChanged();
        p.getInventory().setChanged();
        return true;
    }

    public ItemStack quickMoveStack(Player p, int i) {
        if (i < 0 || i >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(i);
        if (!slot.hasItem() || !slot.mayPickup(p)) return ItemStack.EMPTY;
        var source = slot.getItem();
        var copy = source.copy();
        if (i < 26) {
            if (!moveItemStackTo(source, 26, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!isWorking()) {
            var r = selected();
            if (r == null) return ItemStack.EMPTY;
            for (var part : r.value().parts())
                if (part.ingredient().test(source)) moveItemStackTo(source, part.slot(), part.slot() + 1, false);
            if (source.getCount() == copy.getCount()) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(p, source);
        return copy;
    }
}
