package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TraumaRescuePayload(int phase, float distance, int seconds, int treatmentTicks) implements CustomPacketPayload {
    public static final Type<TraumaRescuePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "trauma_rescue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TraumaRescuePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> {
                b.writeVarInt(p.phase);
                b.writeFloat(p.distance);
                b.writeVarInt(p.seconds);
                b.writeVarInt(p.treatmentTicks);
            },
            b -> new TraumaRescuePayload(b.readVarInt(), b.readFloat(), b.readVarInt(), b.readVarInt()));

    @Override
    public Type<TraumaRescuePayload> type() {
        return TYPE;
    }
}
