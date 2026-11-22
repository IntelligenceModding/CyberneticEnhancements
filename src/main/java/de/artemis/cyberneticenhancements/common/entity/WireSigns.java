package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.entity.SignText;

/**
 * Hanging signs on a hanging wire keep what is written on them, their colour and their wax in the item itself, in the same block
 * entity data a placed sign would have, so the sign keeps it all when it is taken down again.
 */
public final class WireSigns {
    private WireSigns() {}

    public static boolean isSign(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CeilingHangingSignBlock;
    }

    /** Whether the item changes a sign when used on it: dye, ink, honeycomb, or an axe taking the wax off. */
    public static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof DyeItem || stack.is(Items.GLOW_INK_SAC) || stack.is(Items.INK_SAC) || stack.is(Items.HONEYCOMB) || stack.getItem() instanceof AxeItem;
    }

    /** The sign's block entity data, with the id it needs to be read back. */
    public static CompoundTag data(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        tag.putString("id", "minecraft:hanging_sign");
        return tag;
    }

    public static SignText text(ItemStack stack, boolean front, HolderLookup.Provider access) {
        Tag text = data(stack).get(front ? "front_text" : "back_text");
        if (text == null) return new SignText();
        return SignText.DIRECT_CODEC.parse(access.createSerializationContext(NbtOps.INSTANCE), text).result().orElseGet(SignText::new);
    }

    public static boolean waxed(ItemStack stack) {
        return data(stack).getBoolean("is_waxed");
    }

    /** A copy of the sign with the text on one side replaced. */
    public static ItemStack withText(ItemStack stack, boolean front, SignText text, HolderLookup.Provider access) {
        CompoundTag tag = data(stack);
        SignText.DIRECT_CODEC.encodeStart(access.createSerializationContext(NbtOps.INSTANCE), text).result().ifPresent(encoded -> tag.put(front ? "front_text" : "back_text", encoded));
        return store(stack, tag, access);
    }

    public static ItemStack withWax(ItemStack stack, boolean waxed, HolderLookup.Provider access) {
        CompoundTag tag = data(stack);
        tag.putBoolean("is_waxed", waxed);
        return store(stack, tag, access);
    }

    /** The sign with everything that is just the default left off, so a sign that was written on and cleared stacks with a fresh one. */
    public static ItemStack tidy(ItemStack stack, HolderLookup.Provider access) {
        return isSign(stack) && stack.has(DataComponents.BLOCK_ENTITY_DATA) ? store(stack, data(stack), access) : stack;
    }

    private static boolean blank(Tag side, HolderLookup.Provider access) {
        SignText text = SignText.DIRECT_CODEC.parse(access.createSerializationContext(NbtOps.INSTANCE), side).result().orElse(null);
        if (text == null || text.getColor() != DyeColor.BLACK || text.hasGlowingText()) return false;
        for (int i = 0; i < 4; i++) if (!text.getMessage(i, false).getString().isEmpty() || !text.getMessage(i, true).getString().isEmpty()) return false;
        return true;
    }

    private static ItemStack store(ItemStack stack, CompoundTag old, HolderLookup.Provider access) {
        CompoundTag tag = new CompoundTag();
        for (String side : new String[]{"front_text", "back_text"}) if (old.contains(side) && !blank(old.get(side), access)) tag.put(side, old.get(side));
        if (old.getBoolean("is_waxed")) tag.putBoolean("is_waxed", true);
        ItemStack copy = stack.copy();
        tag.putString("id", "minecraft:hanging_sign");
        if (tag.size() == 1) copy.remove(DataComponents.BLOCK_ENTITY_DATA);
        else copy.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return copy;
    }
}
