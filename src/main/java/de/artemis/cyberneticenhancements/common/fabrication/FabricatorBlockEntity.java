package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class FabricatorBlockEntity extends BlockEntity implements Container {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    public int capability = 1, progress = 0;
    public String selected = "";
    private String running = "";
    private String authorizedSchematic = "";

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FABRICATOR.get(), pos, state);
    }

    public int supportedTier() {
        return capability == 1 ? 2 : capability == 2 ? 4 : 5;
    }

    public List<RecipeHolder<FabricationRecipe>> recipes() {
        return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.FABRICATION.get()).stream().sorted(Comparator.comparing(r -> r.id().toString())).toList();
    }

    private RecipeHolder<FabricationRecipe> recipe(String id) {
        var key = ResourceLocation.tryParse(id);
        if (level == null || key == null) return null;
        var holder = level.getRecipeManager().byKey(key).orElse(null);
        return holder != null && holder.value() instanceof FabricationRecipe r ? new RecipeHolder<>(holder.id(), r) : null;
    }

    public RecipeHolder<FabricationRecipe> selectedRecipe() {
        return recipe(selected);
    }

    public FabricationRecipe.Input input() {
        return new FabricationRecipe.Input(List.copyOf(items.subList(0, 25)));
    }

    public boolean working() {
        return !running.isEmpty();
    }

    public boolean canStart(ServerPlayer player) {
        var holder = selectedRecipe();
        if (holder == null || working()) return false;
        var r = holder.value();
        return supportedTier() >= r.tier() && r.unlockedFor(player) && r.matches(input(), level) && outputFits(r.assemble(input(), level.registryAccess()));
    }

    private boolean outputFits(ItemStack result) {
        ItemStack output = items.get(26);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    public String status(Player player) {
        var holder = selectedRecipe();
        if (holder == null) return "Select a design";
        var r = holder.value();
        if (working()) return "Fabricating";
        if (supportedTier() < r.tier())
            return "Upgrade machine to " + (r.tier() == 5 ? "Restricted" : "Precision") + " Assembly";
        boolean known = level.isClientSide ? SchematicKnowledge.clientKnows(r.schematic()) : SchematicKnowledge.knows(player, r.schematic());
        if (!known) return "Schematic not learned";
        if (!r.matches(input(), level)) return "Missing or misplaced parts";
        if (!outputFits(r.result())) return "Output bay full";
        return "Ready";
    }

    public boolean start(ServerPlayer player) {
        if (selectedRecipe() == null)
            recipes().stream().filter(r -> r.value().unlockedFor(player) && r.value().matches(input(), level)).findFirst().ifPresent(r -> selected = r.id().toString());
        if (!canStart(player)) return false;
        running = selected;
        authorizedSchematic = selectedRecipe().value().schematic();
        progress = 0;
        setChanged();
        return true;
    }

    public void cancel() {
        running = "";
        authorizedSchematic = "";
        progress = 0;
        setChanged();
    }

    public boolean upgrade(ItemStack module) {
        boolean valid = capability == 1 && module.is(FabricationContent.part("precision_assembly_module")) || capability == 2 && module.is(FabricationContent.part("high_spec_fabrication_module"));
        if (!valid || working()) return false;
        capability++;
        module.shrink(1);
        setChanged();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FabricatorBlockEntity station) {

        if (!station.items.get(25).isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, station.removeItemNoUpdate(25));
            station.setChanged();
        }
        if (!station.working()) return;
        var holder = station.recipe(station.running);
        if (holder == null || !holder.value().schematic().equals(station.authorizedSchematic)
                || !holder.value().matches(station.input(), level) || holder.value().tier() > station.supportedTier()) {
            station.cancel();
            return;
        }
        var recipe = holder.value();
        ItemStack result = recipe.assemble(station.input(), level.registryAccess());
        if (!station.outputFits(result)) return;
        if (++station.progress < recipe.ticks()) {
            station.setChanged();
            return;
        }
        List<ItemStack> remainders = new ArrayList<>();
        for (var part : recipe.parts()) {
            ItemStack stack = station.items.get(part.slot());
            for (int n = 0; n < part.count(); n++)
                if (stack.hasCraftingRemainingItem()) remainders.add(stack.getCraftingRemainingItem());
            stack.shrink(part.count());
        }
        if (station.items.get(26).isEmpty()) station.items.set(26, result);
        else station.items.get(26).grow(result.getCount());
        for (ItemStack remainder : remainders)
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, remainder);
        station.cancel();
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        tag.putInt("capability", capability);
        tag.putInt("progress", progress);
        tag.putString("selected", selected);
        tag.putString("running", running);
        tag.putString("authorized_schematic", authorizedSchematic);
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        items = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, lookup);
        capability = Math.clamp(tag.getInt("capability"), 1, 3);
        progress = Math.max(0, tag.getInt("progress"));
        selected = tag.getString("selected");
        running = tag.getString("running");
        authorizedSchematic = tag.getString("authorized_schematic");
    }

    public int getContainerSize() {
        return 27;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        var result = ContainerHelper.removeItem(items, slot, amount);
        setChanged();
        return result;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    public void clearContent() {
        items.clear();
        setChanged();
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !working() && slot >= 0 && slot < 25;
    }

    public boolean canTakeItem(Container destination, int slot, ItemStack stack) {
        return !working() || slot == 26;
    }
}
