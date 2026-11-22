package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.entity.HangingWireEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Asks the server to act on a hanging wire where the player looks, {@code t} of the way along it: hang, take an item off, or break it. */
public record HangingWireDecoratePayload(int wire, double t, HangingWireEntity.Action action) implements CustomPacketPayload {
    public static final Type<HangingWireDecoratePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            CyberneticEnhancements.MOD_ID, "hanging_wire_decorate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HangingWireDecoratePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> { buffer.writeVarInt(payload.wire()); buffer.writeDouble(payload.t()); buffer.writeEnum(payload.action()); },
            buffer -> new HangingWireDecoratePayload(buffer.readVarInt(), buffer.readDouble(), buffer.readEnum(HangingWireEntity.Action.class)));

    @Override public Type<HangingWireDecoratePayload> type() { return TYPE; }
}
