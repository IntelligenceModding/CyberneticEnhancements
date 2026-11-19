package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FabricatorRecipeTransferPayload(int containerId, ResourceLocation recipe, boolean maximum) implements CustomPacketPayload {
    public static final Type<FabricatorRecipeTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "fabricator_recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FabricatorRecipeTransferPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.containerId); b.writeResourceLocation(p.recipe); b.writeBoolean(p.maximum); },
            b -> new FabricatorRecipeTransferPayload(b.readVarInt(), b.readResourceLocation(), b.readBoolean()));
    @Override public Type<FabricatorRecipeTransferPayload> type() { return TYPE; }
}
