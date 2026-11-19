package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerDeploymentPayload(int entityId, long started, boolean departing) implements CustomPacketPayload {
    public static final Type<PlayerDeploymentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player_deployment"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerDeploymentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerDeploymentPayload::entityId,
            ByteBufCodecs.VAR_LONG, PlayerDeploymentPayload::started,
            ByteBufCodecs.BOOL, PlayerDeploymentPayload::departing,
            PlayerDeploymentPayload::new);

    @Override public Type<PlayerDeploymentPayload> type() { return TYPE; }
}
