package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RepairRecipeTransferPayload(int containerId, ResourceLocation recipe, RepairRecipeTransfer.Kind kind, boolean maximum) implements CustomPacketPayload {
    public static final Type<RepairRecipeTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "repair_recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RepairRecipeTransferPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.containerId); b.writeResourceLocation(p.recipe); b.writeEnum(p.kind); b.writeBoolean(p.maximum); },
            b -> new RepairRecipeTransferPayload(b.readVarInt(), b.readResourceLocation(), b.readEnum(RepairRecipeTransfer.Kind.class), b.readBoolean()));
    @Override public Type<RepairRecipeTransferPayload> type() { return TYPE; }
}
