package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.registry.*;
import de.artemis.cyberneticenhancements.common.menu.RecipeSlotTransfer;
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
                    case 0 -> 3;
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

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.PURCHASE.play(player);
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9,
                        FabricatorLayout.PLAYER_INVENTORY_X + col * FabricatorLayout.PLAYER_SLOT_SPACING,
                        FabricatorLayout.PLAYER_INVENTORY_Y + row * FabricatorLayout.PLAYER_SLOT_SPACING));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, FabricatorLayout.PLAYER_INVENTORY_X + col * FabricatorLayout.PLAYER_SLOT_SPACING,
                    FabricatorLayout.HOTBAR_Y));
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
            return station.selectDesign(server, l.get(i).id());
        }
        switch (button) {
            case 0 -> {
                if (!fill(server)) {
                    de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(server);
                    return false;
                }
                de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.PURCHASE.play(server);
            }
            case 1 -> {
                if (!station.start(server)) {
                    de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(server);
                    return false;
                }
                de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.FABRICATE.play(server);
            }
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

    public boolean canFill() {
        if (isWorking()) return false;
        var holder = selected();
        if (holder == null || !holder.value().unlockedFor(player)) return false;
        var recipe = holder.value();
        for (int i = 0; i < 25; i++) {
            int index = i;
            var part = recipe.parts().stream().filter(p -> p.slot() == index).findFirst().orElse(null);
            ItemStack present = station.getItem(i);
            if (!present.isEmpty() && (part == null || !part.ingredient().test(present))) return false;
        }
        var targets = recipe.parts().stream().map(part -> new RecipeSlotTransfer.FillTarget(
                slots.get(part.slot()), part.count(), part.ingredient()::test)).toList();
        return RecipeSlotTransfer.canFill(player.getInventory(), targets, player.isCreative());
    }

    private boolean fill(ServerPlayer p) {
        if (!canFill()) return false;
        var holder = station.selectedRecipe();
        if (holder == null) return false;
        var r = holder.value();
        if (!r.unlockedFor(p)) return false;
        for (var part : r.parts()) {
            var target = station.getItem(part.slot());
            int missing = part.count() - target.getCount();
            if (p.isCreative()) {
                if (missing > 0) {
                    var choices = part.ingredient().getItems();
                    if (target.isEmpty() && choices.length == 0) return false;
                    station.setItem(part.slot(), (target.isEmpty() ? choices[0] : target).copyWithCount(part.count()));
                }
                continue;
            }
            for (int i = 0; i < 36 && missing > 0; i++) {
                var source = p.getInventory().getItem(i);
                if (!part.ingredient().test(source) || !target.isEmpty() && !ItemStack.isSameItemSameComponents(target, source)
                        || part.count() > slots.get(part.slot()).getMaxStackSize(source))
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

    public boolean transferRecipe(ServerPlayer sender, net.minecraft.resources.ResourceLocation recipe, boolean maximum) {
        if (sender != player || sender.containerMenu != this || !stillValid(sender)) return false;
        var plan = FabricatorRecipeTransfer.prepare(this, sender, recipe, maximum);
        if (plan.error() != FabricatorRecipeTransfer.Error.NONE) {
            sender.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "screen.cyberneticenhancements.fabricator.transfer." + plan.error().name().toLowerCase(java.util.Locale.ROOT)), true);
            return false;
        }
        if (!station.selectDesign(sender, plan.recipe())) return false;
        for (int slot = 0; slot < 25; slot++) station.setItem(slot, plan.grid().get(slot));
        for (int slot = 0; slot < 36; slot++) sender.getInventory().setItem(slot, plan.inventory().get(slot));
        station.setChanged();
        sender.getInventory().setChanged();
        broadcastChanges();
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
        } else {
            var r = isWorking() ? null : selected();
            if (r != null) {
                var matching = r.value().parts().stream().filter(part -> part.ingredient().test(source)).toList();
                var targets = matching.stream().map(part -> new RecipeSlotTransfer.Target(slots.get(part.slot()), part.count())).toList();
                RecipeSlotTransfer.distributeRequired(source, targets);
                RecipeSlotTransfer.distributeExcess(source, targets);
            }
            if (source.getCount() == copy.getCount()) {
                // Vanilla fallback: move between the 27-slot inventory and the hotbar.
                if (i < 53 ? !moveItemStackTo(source, 53, slots.size(), false)
                        : !moveItemStackTo(source, 26, 53, false)) return ItemStack.EMPTY;
            }
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(p, source);
        return copy;
    }
}
