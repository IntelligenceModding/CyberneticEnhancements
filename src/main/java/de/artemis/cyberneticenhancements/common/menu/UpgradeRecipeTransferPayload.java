package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpgradeRecipeTransferPayload(int containerId, ResourceLocation recipe, int level, boolean maximum) implements CustomPacketPayload {
    public static final Type<UpgradeRecipeTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "upgrade_recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeRecipeTransferPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.containerId); b.writeResourceLocation(p.recipe); b.writeVarInt(p.level); b.writeBoolean(p.maximum); },
            b -> new UpgradeRecipeTransferPayload(b.readVarInt(), b.readResourceLocation(), b.readVarInt(), b.readBoolean()));
    @Override public Type<UpgradeRecipeTransferPayload> type() { return TYPE; }
}
