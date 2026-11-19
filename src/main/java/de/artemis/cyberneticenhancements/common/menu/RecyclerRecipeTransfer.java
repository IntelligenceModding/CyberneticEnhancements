package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.recipe.RecyclerRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Read-only transfer planning shared by JEI previews and authoritative server requests. */
public final class RecyclerRecipeTransfer {
    private RecyclerRecipeTransfer() {}
    public enum Error { NONE, INVALID_RECIPE, BUSY, CURSOR, MISSING, INVENTORY_FULL }
    public record Plan(Error error, ItemStack input, List<ItemStack> inventory) {}
    private static Plan failure(Error error) { return new Plan(error, ItemStack.EMPTY, List.of()); }

    public static Plan prepare(RecyclerStationMenu menu, Player player, ResourceLocation id, boolean maximum) {
        var holder = player.level().getRecipeManager().byKey(id).orElse(null);
        if (holder == null || !(holder.value() instanceof RecyclerRecipe recipe)) return failure(Error.INVALID_RECIPE);
        if (menu.isWorking()) return failure(Error.BUSY);
        if (!menu.getCarried().isEmpty()) return failure(Error.CURSOR);
        var inventory = new ArrayList<ItemStack>(36);
        for (int i = 0; i < 36; i++) inventory.add(player.getInventory().getItem(i).copy());
        ItemStack previous = menu.getInputStack();
        boolean reuse = !previous.isEmpty() && recipe.getInputIngredient().test(previous);
        ItemStack choice = reuse ? previous.copy() : ItemStack.EMPTY;
        if (!reuse) {
            int best = 0;
            for (var candidate : inventory) if (recipe.getInputIngredient().test(candidate)) {
                int count = inventory.stream().filter(s -> ItemStack.isSameItemSameComponents(s, candidate)).mapToInt(ItemStack::getCount).sum();
                if (choice.isEmpty() || maximum && count > best) { choice = candidate.copyWithCount(1); best = count; }
            }
            if (choice.isEmpty() && player.isCreative()) {
                var choices = recipe.getInputIngredient().getItems();
                if (choices.length > 0) choice = choices[0].copyWithCount(1);
            }
        }
        if (choice.isEmpty()) return failure(Error.MISSING);
        if (!menu.slots.getFirst().mayPlace(choice)) return failure(Error.INVALID_RECIPE);
        int limit = menu.slots.getFirst().getMaxStackSize(choice);
        int present = reuse ? previous.getCount() : 0;
        int needed = maximum ? Math.max(0, limit - present) : reuse ? 0 : 1;
        int supplied = 0;
        if (player.isCreative()) supplied = needed;
        else for (var stack : inventory) if (ItemStack.isSameItemSameComponents(choice, stack)) {
            int take = Math.min(needed - supplied, stack.getCount());
            stack.shrink(take);
            supplied += take;
            if (supplied == needed) break;
        }
        if (present + supplied == 0) return failure(Error.MISSING);
        if (!reuse && !previous.isEmpty() && !stow(inventory, previous.copy())) return failure(Error.INVENTORY_FULL);
        return new Plan(Error.NONE, choice.copyWithCount(present + supplied), List.copyOf(inventory));
    }

    private static boolean stow(List<ItemStack> inventory, ItemStack stack) {
        for (var existing : inventory) if (ItemStack.isSameItemSameComponents(existing, stack)) {
            int count = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(count); stack.shrink(count);
            if (stack.isEmpty()) return true;
        }
        for (int i = 0; i < inventory.size(); i++) if (inventory.get(i).isEmpty()) {
            int count = Math.min(stack.getCount(), stack.getMaxStackSize());
            inventory.set(i, stack.copyWithCount(count)); stack.shrink(count);
            if (stack.isEmpty()) return true;
        }
        return stack.isEmpty();
    }
}
