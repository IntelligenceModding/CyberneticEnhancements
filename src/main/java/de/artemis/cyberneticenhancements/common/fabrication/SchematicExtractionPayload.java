package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SchematicExtractionPayload(String design) implements CustomPacketPayload {
    public static final Type<SchematicExtractionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "schematic_extraction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SchematicExtractionPayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buffer) -> buffer.writeUtf(payload.design, 256),
                    buffer -> new SchematicExtractionPayload(buffer.readUtf(256)));

    @Override public Type<SchematicExtractionPayload> type() { return TYPE; }
}
