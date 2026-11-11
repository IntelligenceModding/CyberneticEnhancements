package de.artemis.cyberneticenhancements.common.fabrication;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SchematicHistoryTest {
    @Test
    public void DiscoveryHistorySurvivesSaveAndNetworkSync() {
        var learned = new SchematicKnowledge.Unlock("militech_apogee", 1794333300123L, 82951, "minecraft:the_nether", new BlockPos(-123, 67, 890), "Reverse engineered intact implant");
        assertEquals(learned, SchematicKnowledge.Unlock.load(learned.id(), learned.save()));
        var payload = new SchematicKnowledgePayload(List.of(learned, SchematicKnowledge.Unlock.legacy("dynalar_sandevistan")));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            SchematicKnowledgePayload.STREAM_CODEC.encode(buffer, payload);
            var decoded = SchematicKnowledgePayload.STREAM_CODEC.decode(buffer);
            assertEquals(payload, decoded);
            assertEquals(0, buffer.readableBytes());
            SchematicKnowledge.receiveDetails(decoded.records());
            assertTrue(SchematicKnowledge.clientKnows(learned.id()));
            assertEquals(learned, SchematicKnowledge.clientHistory(learned.id()));
            assertFalse(SchematicKnowledge.clientKnows("not_learned"));
            assertNull(SchematicKnowledge.clientHistory("not_learned"));
        } finally {
            buffer.release();
            SchematicKnowledge.clearClient();
        }
    }

    @Test
    public void LegacyUnlocksDoNotInventDiscoveryDatesOrLocations() {
        SchematicKnowledge.receive(List.of("dynalar_sandevistan"));
        try {
            var old = SchematicKnowledge.clientHistory("dynalar_sandevistan");
            assertTrue(SchematicKnowledge.clientKnows(old.id()));
            assertEquals(0, old.epochMillis());
            assertEquals(-1, old.gameTime());
            assertEquals("", old.dimension());
        } finally {
            SchematicKnowledge.clearClient();
        }
    }

    @Test
    public void InvalidHistoryPacketCountsAreRejected() {
        for (int count : new int[]{-1, 4097, Integer.MAX_VALUE}) {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                buffer.writeVarInt(count);
                assertThrows(IllegalArgumentException.class, () -> SchematicKnowledgePayload.STREAM_CODEC.decode(buffer));
            } finally {
                buffer.release();
            }
        }
    }
}
