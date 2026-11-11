package de.artemis.cyberneticenhancements.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.fabrication.SchematicItem;
import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

 public final class RecyclerRecipe extends SingleItemRecipe {
    public static final double GOOD_CONDITION_MINIMUM = 0.75;
    public static final double WORN_CONDITION_MINIMUM = 0.45;
    private final List<ItemStack> outputs, wornOutputs, damagedOutputs;

    public RecyclerRecipe(String group, Ingredient ingredient, List<ItemStack> outputs,
                          List<ItemStack> wornOutputs, List<ItemStack> damagedOutputs) {
        super(ModRecipeTypes.RECYCLING.get(), ModRecipeSerializers.RECYCLING.get(), group, ingredient, outputs.getFirst());
        this.outputs = validate(outputs);
        this.wornOutputs = validate(wornOutputs);
        this.damagedOutputs = validate(damagedOutputs);
    }

    private static List<ItemStack> validate(List<ItemStack> batch) {
        if (batch.isEmpty() || batch.size() > 3) throw new IllegalArgumentException("Salvage needs 1..3 outputs");
        var combined = new ArrayList<ItemStack>();
        for (var stack : batch) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()
                    || stack.getItem() instanceof CyberwareItem || stack.getItem() instanceof SchematicItem
                    || Set.of("proprietary_neural_core", "military_actuator_core", "experimental_control_core").contains(id))
                throw new IllegalArgumentException("Invalid or restricted salvage output: " + stack);
            var existing = combined.stream().filter(s -> ItemStack.isSameItemSameComponents(s, stack)).findFirst();
            if (existing.isPresent()) {
                existing.get().grow(stack.getCount());
                if (existing.get().getCount() > stack.getMaxStackSize()) throw new IllegalArgumentException("Salvage stack overflow");
            } else combined.add(stack.copy());
        }
        return List.copyOf(combined);
    }

    public List<ItemStack> recover(ItemStack input) {
        double integrity = input.getItem() instanceof CyberwareItem implant
                ? CyberwareConditionHelper.getIntegrityRatio(input, implant.getDefinition()) : 1;
        return (integrity < WORN_CONDITION_MINIMUM ? damagedOutputs : integrity < GOOD_CONDITION_MINIMUM ? wornOutputs : outputs)
                .stream().map(ItemStack::copy).toList();
    }

    public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return recover(input.item()).getFirst(); }
    public ItemStack getToastSymbol() { return new ItemStack(ModBlocks.RECYCLER_STATION.get()); }
    public Ingredient getInputIngredient() { return ingredient; }
    public List<ItemStack> outputs() { return outputs.stream().map(ItemStack::copy).toList(); }
    public List<ItemStack> wornOutputs() { return wornOutputs.stream().map(ItemStack::copy).toList(); }
    public List<ItemStack> damagedOutputs() { return damagedOutputs.stream().map(ItemStack::copy).toList(); }
    public boolean isDamageSensitive() {
        return !sameBatch(outputs, wornOutputs) || !sameBatch(outputs, damagedOutputs);
    }
    private static boolean sameBatch(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) if (!ItemStack.matches(first.get(i), second.get(i))) return false;
        return true;
    }

    public List<List<ItemStack>> displayOutputs() {
        List<List<ItemStack>> slots = new ArrayList<>();
        var materials = new ArrayList<ItemStack>();
        for (var batch : List.of(outputs, wornOutputs, damagedOutputs)) for (var stack : batch)
            if (materials.stream().noneMatch(s -> ItemStack.isSameItemSameComponents(s, stack))) materials.add(stack);

        if (materials.size() > 3) return outputs.stream().map(s -> List.of(s.copy())).toList();
        for (var material : materials) {
            List<ItemStack> alternatives = new ArrayList<>();
            for (var batch : List.of(damagedOutputs, wornOutputs, outputs)) for (var stack : batch)
                if (ItemStack.isSameItemSameComponents(material, stack) && alternatives.stream().noneMatch(s -> ItemStack.matches(s, stack)))
                    alternatives.add(stack.copy());
            slots.add(alternatives);
        }
        return slots;
    }

    public static final class Serializer implements RecipeSerializer<RecyclerRecipe> {
        private static final MapCodec<RecyclerRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(RecyclerRecipe::getInputIngredient),
                ItemStack.STRICT_CODEC.listOf().fieldOf("outputs").forGetter(RecyclerRecipe::outputs),
                ItemStack.STRICT_CODEC.listOf().fieldOf("worn_outputs").forGetter(RecyclerRecipe::wornOutputs),
                ItemStack.STRICT_CODEC.listOf().fieldOf("damaged_outputs").forGetter(RecyclerRecipe::damagedOutputs)
        ).apply(i, RecyclerRecipe::new));
        public MapCodec<RecyclerRecipe> codec() { return CODEC; }
        public StreamCodec<RegistryFriendlyByteBuf, RecyclerRecipe> streamCodec() {
            return new StreamCodec<>() {
                private List<ItemStack> read(RegistryFriendlyByteBuf b) {
                    int size = b.readVarInt();
                    if (size < 1 || size > 3) throw new IllegalArgumentException("Invalid salvage batch");
                    var batch = new ArrayList<ItemStack>();
                    for (int n = 0; n < size; n++) batch.add(ItemStack.STREAM_CODEC.decode(b));
                    return batch;
                }
                private void write(RegistryFriendlyByteBuf b, List<ItemStack> batch) {
                    b.writeVarInt(batch.size());
                    batch.forEach(s -> ItemStack.STREAM_CODEC.encode(b, s));
                }
                public RecyclerRecipe decode(RegistryFriendlyByteBuf b) {
                    return new RecyclerRecipe(b.readUtf(256), Ingredient.CONTENTS_STREAM_CODEC.decode(b), read(b), read(b), read(b));
                }
                public void encode(RegistryFriendlyByteBuf b, RecyclerRecipe r) {
                    b.writeUtf(r.group, 256);
                    Ingredient.CONTENTS_STREAM_CODEC.encode(b, r.ingredient);
                    write(b, r.outputs); write(b, r.wornOutputs); write(b, r.damagedOutputs);
                }
            };
        }
    }
}
