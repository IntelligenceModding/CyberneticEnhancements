package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerCallPreferencesPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<PlayerCallPreferencesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player_call_preferences"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCallPreferencesPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> b.writeBoolean(p.enabled), b -> new PlayerCallPreferencesPayload(b.readBoolean()));
    public Type<PlayerCallPreferencesPayload> type() { return TYPE; }
}
