package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestDropoffClaimResultPayload(boolean granted) implements CustomPacketPayload {
    public static final Type<QuestDropoffClaimResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "quest_dropoff_claim_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestDropoffClaimResultPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, QuestDropoffClaimResultPayload::granted, QuestDropoffClaimResultPayload::new);

    @Override
    public Type<QuestDropoffClaimResultPayload> type() { return TYPE; }
}
