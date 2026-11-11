package de.artemis.cyberneticenhancements.common.fabrication;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record SchematicKnowledgePayload(List<SchematicKnowledge.Unlock> records) implements CustomPacketPayload {
    public SchematicKnowledgePayload {
        records = List.copyOf(records);
        if (records.size() > 4096) throw new IllegalArgumentException("Too many schematics");
    }

    public static final Type<SchematicKnowledgePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "schematics"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SchematicKnowledgePayload> STREAM_CODEC = new StreamCodec<>() {
        public SchematicKnowledgePayload decode(RegistryFriendlyByteBuf b) {
            int count = b.readVarInt();
            if (count < 0 || count > 4096) throw new IllegalArgumentException("Too many schematics");
            List<SchematicKnowledge.Unlock> records = new ArrayList<>();
            for (int i = 0; i < count; i++)
                records.add(new SchematicKnowledge.Unlock(b.readUtf(256), b.readLong(), b.readLong(), b.readUtf(256), b.readBlockPos(), b.readUtf(512)));
            return new SchematicKnowledgePayload(records);
        }

        public void encode(RegistryFriendlyByteBuf b, SchematicKnowledgePayload p) {
            b.writeVarInt(p.records.size());
            for (var record : p.records) {
                b.writeUtf(record.id(), 256);
                b.writeLong(record.epochMillis());
                b.writeLong(record.gameTime());
                b.writeUtf(record.dimension(), 256);
                b.writeBlockPos(record.position());
                b.writeUtf(record.source(), 512);
            }
        }
    };

    public Type<SchematicKnowledgePayload> type() {
        return TYPE;
    }
}
