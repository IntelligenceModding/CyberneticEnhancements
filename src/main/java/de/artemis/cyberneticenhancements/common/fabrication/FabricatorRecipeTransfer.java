package de.artemis.cyberneticenhancements.common.fabrication;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Simulate the whole transfer before changing either inventory. Shared by the JEI preview and server. */
public final class FabricatorRecipeTransfer {
    public enum Error { NONE, INVALID_RECIPE, LOCKED, BUSY, CURSOR, MISSING, INVENTORY_FULL }
    public record Plan(Error error, ResourceLocation recipe, List<ItemStack> grid, List<ItemStack> inventory, List<Integer> missing) {
        static Plan failed(Error error, ResourceLocation recipe) { return new Plan(error, recipe, List.of(), List.of(), List.of()); }
    }

    public static Plan prepare(FabricatorMenu menu, Player player, ResourceLocation id, boolean maximum) {
        var holder = menu.recipes().stream().filter(r -> r.id().equals(id)).findFirst().orElse(null);
        if (holder == null) return Plan.failed(Error.INVALID_RECIPE, id);
        if (!holder.value().unlockedFor(player)) return Plan.failed(Error.LOCKED, id);
        if (menu.isWorking()) return Plan.failed(Error.BUSY, id);
        if (!menu.getCarried().isEmpty()) return Plan.failed(Error.CURSOR, id);
        int limit = maximum ? 64 : 1;
        for (var part : holder.value().parts()) {
            int capacity = Arrays.stream(part.ingredient().getItems())
                    .mapToInt(stack -> menu.slots.get(part.slot()).getMaxStackSize(stack)).max().orElse(0);
            limit = Math.min(limit, capacity / part.count());
        }
        if (maximum && !player.isCreative()) {
            Map<net.minecraft.world.item.Item, Integer> exactNeeds = new HashMap<>();
            for (var part : holder.value().parts()) {
                var choices = part.ingredient().getItems();
                if (choices.length == 1) exactNeeds.merge(choices[0].getItem(), part.count(), Integer::sum);
            }
            for (var entry : exactNeeds.entrySet()) {
                int available = player.getInventory().countItem(entry.getKey());
                for (int slot = 0; slot < 25; slot++) {
                    ItemStack stack = menu.station.getItem(slot);
                    if (stack.is(entry.getKey())) available += stack.getCount();
                }
                limit = Math.min(limit, available / entry.getValue());
            }
            // Still simulate one set on failure so JEI can highlight its missing positions.
            limit = Math.max(1, limit);
        }
        Plan failure = Plan.failed(Error.MISSING, id);
        for (int batches = limit; batches >= 1; batches--) {
            Plan plan = arrange(menu, player, id, holder.value(), batches);
            if (plan.error() == Error.NONE) return plan;
            failure = plan;
        }
        return failure;
    }

    private static Plan arrange(FabricatorMenu menu, Player player, ResourceLocation id, FabricationRecipe recipe, int batches) {
        List<ItemStack> pool = new ArrayList<>();
        for (int slot = 0; slot < 25; slot++) pool.add(menu.station.getItem(slot).copy());
        for (int slot = 0; slot < 36; slot++) pool.add(player.getInventory().getItem(slot).copy());
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(25, ItemStack.EMPTY));
        List<Integer> missing = new ArrayList<>();
        var parts = new ArrayList<>(recipe.parts());
        // Reserve narrowly matched ingredients before flexible tags; counted inputs go first on ties.
        parts.sort(Comparator.comparingInt((FabricationRecipe.Part part) -> variants(pool, part).size())
                .thenComparing(Comparator.comparingInt(FabricationRecipe.Part::count).reversed()));
        for (var part : parts) {
            int needed = part.count() * batches;
            ItemStack chosen;
            if (player.isCreative()) {
                var choices = part.ingredient().getItems();
                ItemStack existing = pool.get(part.slot());
                chosen = !existing.isEmpty() && part.ingredient().test(existing)
                        && menu.slots.get(part.slot()).getMaxStackSize(existing) >= needed ? existing
                        : variants(pool.subList(0, 25), part).stream().filter(stack -> menu.slots.get(part.slot()).getMaxStackSize(stack) >= needed)
                            .findFirst().orElseGet(() -> Arrays.stream(choices)
                                    .filter(stack -> menu.slots.get(part.slot()).getMaxStackSize(stack) >= needed)
                                    .findFirst().orElse(ItemStack.EMPTY));
            } else {
                ItemStack existing = pool.get(part.slot());
                chosen = !existing.isEmpty() && part.ingredient().test(existing) && available(pool, existing) >= needed
                        && menu.slots.get(part.slot()).getMaxStackSize(existing) >= needed ? existing
                        : variants(pool, part).stream().filter(stack -> available(pool, stack) >= needed)
                        .filter(stack -> menu.slots.get(part.slot()).getMaxStackSize(stack) >= needed)
                        .max(Comparator.comparingInt(stack -> available(pool, stack))).orElse(ItemStack.EMPTY);
            }
            if (chosen.isEmpty()) { missing.add(part.slot()); continue; }
            chosen = chosen.copyWithCount(1); // Keep the match key stable while source stacks shrink.
            grid.set(part.slot(), chosen.copyWithCount(needed));
            int remaining = needed;
            var consumedPool = player.isCreative() ? pool.subList(0, 25) : pool;
            // Creative supplies only the missing quantity for free; it reuses existing grid items.
            for (ItemStack stack : consumedPool) {
                if (!ItemStack.isSameItemSameComponents(stack, chosen)) continue;
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
                if (remaining == 0) break;
            }
        }
        if (!missing.isEmpty()) return new Plan(Error.MISSING, id, List.of(), List.of(), List.copyOf(missing));
        List<ItemStack> inventory = new ArrayList<>(pool.subList(25, 61));
        for (int slot = 0; slot < 25; slot++)
            if (!stow(pool.get(slot), inventory)) return Plan.failed(Error.INVENTORY_FULL, id);
        return new Plan(Error.NONE, id, List.copyOf(grid), List.copyOf(inventory), List.of());
    }

    private static List<ItemStack> variants(List<ItemStack> pool, FabricationRecipe.Part part) {
        List<ItemStack> variants = new ArrayList<>();
        for (ItemStack stack : pool)
            if (!stack.isEmpty() && part.ingredient().test(stack)
                    && variants.stream().noneMatch(other -> ItemStack.isSameItemSameComponents(stack, other))) variants.add(stack.copyWithCount(1));
        return variants;
    }

    private static int available(List<ItemStack> pool, ItemStack variant) {
        return pool.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, variant)).mapToInt(ItemStack::getCount).sum();
    }

    private static boolean stow(ItemStack source, List<ItemStack> inventory) {
        if (source.isEmpty()) return true;
        for (ItemStack target : inventory) {
            if (target.isEmpty() || !ItemStack.isSameItemSameComponents(source, target)) continue;
            int moved = Math.min(source.getCount(), Math.max(0, target.getMaxStackSize() - target.getCount()));
            target.grow(moved); source.shrink(moved);
            if (source.isEmpty()) return true;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (!inventory.get(slot).isEmpty()) continue;
            inventory.set(slot, source.split(Math.min(source.getCount(), source.getMaxStackSize())));
            if (source.isEmpty()) return true;
        }
        return false;
    }
    private FabricatorRecipeTransfer() {}
}
