package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** What a player wrote on the front or back of the sign hung {@code t} of the way along a hanging wire. */
public record HangingSignTextPayload(int wire, double t, boolean front, List<String> lines) implements CustomPacketPayload {
    public static final Type<HangingSignTextPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "hanging_sign_text"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HangingSignTextPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {
                buffer.writeVarInt(payload.wire()); buffer.writeDouble(payload.t()); buffer.writeBoolean(payload.front());
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4)).encode(buffer, payload.lines());
            },
            buffer -> new HangingSignTextPayload(buffer.readVarInt(), buffer.readDouble(), buffer.readBoolean(), ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4)).decode(buffer)));

    @Override public Type<HangingSignTextPayload> type() { return TYPE; }
}
