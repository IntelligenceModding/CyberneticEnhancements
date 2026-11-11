package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CyberwareUpgradesPayload(String json) implements CustomPacketPayload {
    public static final int MAX_LENGTH = 250000;
    public static final Type<CyberwareUpgradesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "cyberware_upgrades"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CyberwareUpgradesPayload> STREAM_CODEC = new StreamCodec<>() {
        public CyberwareUpgradesPayload decode(RegistryFriendlyByteBuf buffer) { return new CyberwareUpgradesPayload(buffer.readUtf(MAX_LENGTH)); }
        public void encode(RegistryFriendlyByteBuf buffer, CyberwareUpgradesPayload payload) { buffer.writeUtf(payload.json, MAX_LENGTH); }
    };
    public Type<CyberwareUpgradesPayload> type() { return TYPE; }
}
