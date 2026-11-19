package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecyclerRecipeTransferPayload(int containerId, ResourceLocation recipe, boolean maximum) implements CustomPacketPayload {
    public static final Type<RecyclerRecipeTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "recycler_recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecyclerRecipeTransferPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.containerId); b.writeResourceLocation(p.recipe); b.writeBoolean(p.maximum); },
            b -> new RecyclerRecipeTransferPayload(b.readVarInt(), b.readResourceLocation(), b.readBoolean()));
    @Override public Type<RecyclerRecipeTransferPayload> type() { return TYPE; }
}
