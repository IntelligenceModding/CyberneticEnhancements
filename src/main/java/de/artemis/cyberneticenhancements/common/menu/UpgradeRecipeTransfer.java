package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Pure planning; the server resolves all costs from its own upgrade definitions. */
public final class UpgradeRecipeTransfer {
    private UpgradeRecipeTransfer() {}
    public enum Error { NONE, INVALID_RECIPE, WRONG_STATION, BUSY, CURSOR, MISSING, INVENTORY_FULL }
    public record Plan(Error error, List<ItemStack> machine, List<ItemStack> inventory, List<Integer> missing) {}
    private static Plan fail(Error error) { return new Plan(error, List.of(), List.of(), List.of()); }

    public static ItemStack displayInput(ResourceLocation id, int level) {
        var item = BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof CyberwareItem implant) || level < 0 || level >= CyberwareUpgradeHelper.getMaxUpgrades(implant.getDefinition())) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        for (int i = 0; i < level; i++) stack = CyberwareUpgradeHelper.createUpgradedCopy(stack, implant.getDefinition());
        return stack;
    }
    private static boolean matches(ItemStack stack, ItemStack sample, int level) {
        return !stack.isEmpty() && stack.is(sample.getItem()) && CyberwareUpgradeHelper.getUpgradeLevel(stack) == level;
    }
    public static Plan prepare(TechStationMenu menu, Player player, ResourceLocation id, int level, boolean maximum) {
        return assemble(menu, player, true, () -> displayInput(id, level), (stack, sample) -> matches(stack, sample, level),
                CyberwareServiceHelper::getUpgradePlan, maximum);
    }

    /**
     * The shared planning for a station transfer: finds the implant (in the machine, then the inventory), then the materials its
     * plan needs, without touching anything live. Used by the Upgrade and Repair Station transfers.
     */
    static Plan assemble(TechStationMenu menu, Player player, boolean upgrading, java.util.function.Supplier<ItemStack> displayInput,
                         java.util.function.BiPredicate<ItemStack, ItemStack> matches,
                         java.util.function.Function<ItemStack, CyberwareServicePlan> planFor, boolean maximum) {
        if (menu.isUpgrading() != upgrading) return fail(Error.WRONG_STATION);
        if (menu.isWorking()) return fail(Error.BUSY);
        if (!menu.getCarried().isEmpty()) return fail(Error.CURSOR);
        ItemStack sample = displayInput.get();
        if (sample.isEmpty()) return fail(Error.INVALID_RECIPE);
        var inventory = new ArrayList<ItemStack>();
        for (int i = 0; i < 36; i++) inventory.add(player.getInventory().getItem(i).copy());
        ItemStack previous = menu.getInputStack();
        boolean reuse = matches.test(previous, sample);
        ItemStack implant = reuse ? previous.copyWithCount(1) : ItemStack.EMPTY;
        if (!reuse) for (var stack : inventory) if (matches.test(stack, sample)) {
            implant = stack.copyWithCount(1); stack.shrink(1); break;
        }
        if (implant.isEmpty() && player.isCreative()) implant = sample;
        if (implant.isEmpty()) return new Plan(Error.MISSING, List.of(), List.of(), List.of(0));
        var service = planFor.apply(implant);
        if (!service.isAvailable()) return fail(Error.INVALID_RECIPE);
        var pool = new ArrayList<ItemStack>();
        for (int i = 0; i < 2; i++) pool.add(menu.getMaterialStack(i).copy());
        pool.addAll(inventory);
        var machine = new ArrayList<ItemStack>(List.of(implant, ItemStack.EMPTY, ItemStack.EMPTY));
        var missing = new ArrayList<Integer>();
        for (int i = 0; i < service.materials().size(); i++) {
            var required = service.materials().get(i);
            ItemStack present = pool.get(i);
            ItemStack target = ItemStack.isSameItemSameComponents(present, required.stack()) ? present.copy() : ItemStack.EMPTY;
            if (!target.isEmpty()) pool.set(i, ItemStack.EMPTY);
            int need = Math.max(0, required.count() - target.getCount());
            int taken = player.isCreative() ? need : take(pool, required.stack(), need);
            if (taken < need) missing.add(i + 1);
            machine.set(i + 1, required.stack().copyWithCount(target.getCount() + taken));
        }
        if (!missing.isEmpty()) return new Plan(Error.MISSING, List.of(), List.of(), List.copyOf(missing));
        if (maximum) for (int i = 0; i < service.materials().size(); i++) {
            ItemStack target = machine.get(i + 1);
            int need = target.getMaxStackSize() - target.getCount();
            target.grow(player.isCreative() ? need : take(pool, target, need));
        }
        // Pool entries 2..37 are the copied inventory; return displaced machine contents last.
        inventory = new ArrayList<>(pool.subList(2, pool.size()));
        for (int i = 0; i < 2; i++) if (!stow(inventory, pool.get(i))) return fail(Error.INVENTORY_FULL);
        if (!reuse && !stow(inventory, previous.copy())) return fail(Error.INVENTORY_FULL);
        return new Plan(Error.NONE, List.copyOf(machine), List.copyOf(inventory), List.of());
    }
    private static int take(List<ItemStack> pool, ItemStack required, int count) {
        int taken = 0;
        for (var stack : pool) if (ItemStack.isSameItemSameComponents(stack, required)) {
            int amount = Math.min(count - taken, stack.getCount());
            stack.shrink(amount); taken += amount;
            if (taken == count) break;
        }
        return taken;
    }
    private static boolean stow(List<ItemStack> inventory, ItemStack stack) {
        if (stack.isEmpty()) return true;
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
        return false;
    }
}
