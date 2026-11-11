package de.artemis.cyberneticenhancements.common.fabrication;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.artemis.cyberneticenhancements.common.registry.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;

public record FabricationRecipe(List<Part> parts, ItemStack result, int tier, int ticks, String schematic,
                                String category) implements Recipe<FabricationRecipe.Input> {
    public record Part(int slot, Ingredient ingredient, int count) {
        public Part {
            if (slot < 0 || slot > 24 || count < 1 || count > 64 || ingredient.isEmpty())
                throw new IllegalArgumentException("Invalid fabrication ingredient position or count");
        }

        public static final Codec<Part> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 24).fieldOf("slot").forGetter(Part::slot),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Part::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Part::count)).apply(i, Part::new));
    }

    public record Input(List<ItemStack> items) implements RecipeInput {
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        public int size() {
            return items.size();
        }
    }

    public FabricationRecipe {
        parts = List.copyOf(parts);
        if (parts.isEmpty() || parts.size() > 25 || parts.stream().map(Part::slot).distinct().count() != parts.size())
            throw new IllegalArgumentException("Fabrication requires 1..25 distinct grid positions");
        if (result.isEmpty() || tier < 1 || tier > 5 || ticks < 1 || ticks > 72000)
            throw new IllegalArgumentException("Invalid fabrication output, tier or duration");
    }

    public boolean matches(Input input, Level level) {
        if (input.size() != 25) return false;
        for (int slot = 0; slot < 25; slot++) {
            final int index = slot;
            Part part = parts.stream().filter(p -> p.slot == index).findFirst().orElse(null);
            ItemStack stack = input.getItem(slot);
            if (part == null ? !stack.isEmpty() : !part.ingredient.test(stack) || stack.getCount() < part.count)
                return false;
        }
        return true;
    }

    public boolean unlockedFor(net.minecraft.world.entity.player.Player player) {
        return player.level().isClientSide ? clientUnlocked() : SchematicKnowledge.knows(player, schematic);
    }

    public boolean clientUnlocked() {
        return SchematicKnowledge.clientKnows(schematic);
    }

    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        ItemStack output = result.copy();

        if (output.getItem() instanceof de.artemis.cyberneticenhancements.common.item.CyberwareItem item) {
            for (ItemStack stack : input.items)
                if (stack.getItem() instanceof de.artemis.cyberneticenhancements.common.item.CyberwareItem) {
                    output = de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper.createServiceCopy(stack, item.getDefinition(), true);
                    break;
                }
        }
        return output;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width >= 5 && height >= 5;
    }

    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.FABRICATION.get();
    }

    public RecipeType<?> getType() {
        return ModRecipeTypes.FABRICATION.get();
    }

    public boolean isSpecial() {
        return true;
    }

    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.CYBERWARE_FABRICATOR.get());
    }

    public static final class Serializer implements RecipeSerializer<FabricationRecipe> {
        private static final MapCodec<FabricationRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Part.CODEC.listOf().fieldOf("parts").forGetter(FabricationRecipe::parts),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FabricationRecipe::result),
                Codec.intRange(1, 5).optionalFieldOf("tier", 1).forGetter(FabricationRecipe::tier),
                Codec.intRange(1, 72000).optionalFieldOf("ticks", 100).forGetter(FabricationRecipe::ticks),
                Codec.STRING.optionalFieldOf("schematic", "").forGetter(FabricationRecipe::schematic),
                Codec.STRING.optionalFieldOf("category", "Components").forGetter(FabricationRecipe::category)).apply(i, FabricationRecipe::new));

        public MapCodec<FabricationRecipe> codec() {
            return CODEC;
        }

        public StreamCodec<RegistryFriendlyByteBuf, FabricationRecipe> streamCodec() {
            return new StreamCodec<>() {
                public FabricationRecipe decode(RegistryFriendlyByteBuf b) {
                    int size = b.readVarInt();
                    if (size < 1 || size > 25) throw new IllegalArgumentException("Invalid fabrication grid");
                    List<Part> parts = new ArrayList<>();
                    for (int n = 0; n < size; n++)
                        parts.add(new Part(b.readVarInt(), Ingredient.CONTENTS_STREAM_CODEC.decode(b), b.readVarInt()));
                    return new FabricationRecipe(parts, ItemStack.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt(), b.readUtf(256), b.readUtf(128));
                }

                public void encode(RegistryFriendlyByteBuf b, FabricationRecipe r) {
                    b.writeVarInt(r.parts.size());
                    for (Part p : r.parts) {
                        b.writeVarInt(p.slot);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(b, p.ingredient);
                        b.writeVarInt(p.count);
                    }
                    ItemStack.STREAM_CODEC.encode(b, r.result);
                    b.writeVarInt(r.tier);
                    b.writeVarInt(r.ticks);
                    b.writeUtf(r.schematic, 256);
                    b.writeUtf(r.category, 128);
                }
            };
        }
    }
}
