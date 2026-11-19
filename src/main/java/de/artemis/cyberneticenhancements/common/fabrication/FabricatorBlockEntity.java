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

public final class FabricatorBlockEntity extends BlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    public int progress = 0;
    public String selected = "";
    private String running = "";
    private String authorizedSchematic = "";
    private UUID journalOwner;
    private String automationRecipe = "";
    private String automationSchematic = "";
    private boolean wasPowered;
    private boolean singleCraft;

    public boolean selectDesign(ServerPlayer player, ResourceLocation id) {
        var holder = recipe(id.toString());
        if (working() || holder == null || !holder.value().unlockedFor(player)) return false;
        selected = id.toString();
        automationRecipe = selected;
        automationSchematic = holder.value().schematic();
        journalOwner = player.getUUID();
        setChanged();
        return true;
    }

    public void updateRedstone() {
        if (level == null || level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (wasPowered != powered) {
            wasPowered = powered;
            setChanged();
            if (powered) startFromRedstone();
        }
    }

    private void startFromRedstone() {
        var holder = selectedRecipe();
        if (working() || holder == null) return;
        var recipe = holder.value();
        if (!recipe.schematic().isEmpty() && (!selected.equals(automationRecipe)
                || !recipe.schematic().equals(automationSchematic))) return;
        if (recipe.tier() > supportedTier() || !recipe.matches(input(), level)
                || !outputFits(recipe.assemble(input(), level.registryAccess()))) return;
        running = selected;
        authorizedSchematic = recipe.schematic();
        singleCraft = true;
        progress = 0;
        setChanged();
    }

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FABRICATOR.get(), pos, state);
    }

    public int supportedTier() {
        return 5;
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
        selectDesign(player, selectedRecipe().id());
        running = selected;
        journalOwner = player.getUUID();
        singleCraft = false;
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.recordAt(player, worldPosition, "workshop", "fabrication_started", selectedRecipe().value().result().getHoverName());
        authorizedSchematic = selectedRecipe().value().schematic();
        progress = 0;
        setChanged();
        return true;
    }

    public void cancel() {
        running = "";
        singleCraft = false;
        authorizedSchematic = "";
        progress = 0;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FabricatorBlockEntity station) {
        station.updateRedstone();
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
        if (!station.outputFits(result)) {
            station.cancel();
            return;
        }
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
        if (station.journalOwner != null && level instanceof net.minecraft.server.level.ServerLevel server)
            de.artemis.cyberneticenhancements.common.journal.PlayerJournal.recordFor(server.getServer(), station.journalOwner, level.dimension().location().toString(), pos,
                    "workshop", "fabricated", result.getCount(), result.getHoverName());
        // GUI starts repeat; a redstone edge authorizes only one complete batch.
        // Reassemble against the remaining inputs because service recipes can preserve item data.
        if (!station.singleCraft && recipe.matches(station.input(), level)
                && station.outputFits(recipe.assemble(station.input(), level.registryAccess()))) {
            station.progress = 0;
            station.setChanged();
        } else station.cancel();
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        tag.putInt("progress", progress);
        tag.putString("selected", selected);
        tag.putString("running", running);
        tag.putString("authorized_schematic", authorizedSchematic);
        tag.putString("automation_recipe", automationRecipe);
        tag.putString("automation_schematic", automationSchematic);
        tag.putBoolean("was_powered", wasPowered);
        tag.putBoolean("single_craft", singleCraft);
        if (journalOwner != null) tag.putUUID("JournalOwner", journalOwner);
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        items = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, lookup);
        progress = Math.max(0, tag.getInt("progress"));
        selected = tag.getString("selected");
        running = tag.getString("running");
        authorizedSchematic = tag.getString("authorized_schematic");
        automationRecipe = tag.getString("automation_recipe");
        automationSchematic = tag.getString("automation_schematic");
        wasPowered = tag.getBoolean("was_powered");
        singleCraft = tag.getBoolean("single_craft");
        journalOwner = tag.hasUUID("JournalOwner") ? tag.getUUID("JournalOwner") : null;
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
        var holder = selectedRecipe();
        return holder != null && slot >= 0 && slot < 25 && holder.value().parts().stream()
                .anyMatch(part -> part.slot() == slot && part.ingredient().test(stack));
    }

    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[]{26} : java.util.stream.IntStream.range(0, 25).toArray();
    }

    /** Limit each automated insertion to the least-filled matching recipe positions. */
    public int automationInsertLimit(int slot, ItemStack stack) {
        var holder = selectedRecipe();
        if (holder == null || !canPlaceItem(slot, stack)) return 0;
        var part = holder.value().parts().stream().filter(p -> p.slot() == slot).findFirst().orElseThrow();
        ItemStack current = items.get(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return 0;
        double least = holder.value().parts().stream().filter(p -> p.slot() != slot && p.ingredient().test(stack))
                .filter(p -> items.get(p.slot()).isEmpty() || ItemStack.isSameItemSameComponents(items.get(p.slot()), stack))
                .mapToDouble(p -> (double) items.get(p.slot()).getCount() / p.count()).min().orElse(Double.POSITIVE_INFINITY);
        int target = Double.isInfinite(least) ? stack.getMaxStackSize()
                : Math.min(stack.getMaxStackSize(), (int) Math.floor(least) * part.count() + part.count());
        return Math.max(0, target - current.getCount());
    }

    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN && automationInsertLimit(slot, stack) > 0;
    }

    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 26;
    }

    public boolean canTakeItem(Container destination, int slot, ItemStack stack) {
        return !working() || slot == 26;
    }
}
