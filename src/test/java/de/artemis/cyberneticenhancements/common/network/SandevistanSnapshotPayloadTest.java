package de.artemis.cyberneticenhancements.common.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.Test;

import static org.junit.Assert.*;

public class SandevistanSnapshotPayloadTest {
    private static final ResourceLocation DIMENSION = ResourceLocation.withDefaultNamespace("overworld");

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    @Test
    public void UpgradeDefinitionsRoundTripAsServerDisplayData() {
        var payload = new CyberwareUpgradesPayload("{\"militech_experimental_sandevistan\":{\"levels\":[]}}");
        var buffer = buffer();
        try {
            CyberwareUpgradesPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, CyberwareUpgradesPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void EchoStillFrameRoundTripsWithItsWorldAnchorAndPose() {
        var sample = new SandevistanEchoPayload(DIMENSION, 23, UUID.randomUUID(), 912,
                new net.minecraft.world.phys.Vec3(1234.125, 67.5, -987.75),
                137, 155, -32, 0.75F, true, 12.5F, 0.8F, 0.6F, true, false);
        var buffer = buffer();
        try {
            SandevistanEchoPayload.STREAM_CODEC.encode(buffer, sample);
            assertEquals(sample, SandevistanEchoPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void FullSnapshotRoundTripsWithoutLosingEntityIdentity() {
        var user = new SandevistanSnapshotPayload.User(23, UUID.randomUUID(), "militech_experimental_sandevistan", 900, 120, 1, 0.08F);
        var target = new SandevistanSnapshotPayload.Target(45, UUID.randomUUID(), 0.08F);
        var payload = new SandevistanSnapshotPayload(DIMENSION, List.of(user), List.of(target));
        var buffer = buffer();
        try {
            SandevistanSnapshotPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, SandevistanSnapshotPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void OversizedServerSnapshotsAreRejected() {
        var target = new SandevistanSnapshotPayload.Target(1, UUID.randomUUID(), 0.1F);
        assertThrows(IllegalArgumentException.class, () -> new SandevistanSnapshotPayload(DIMENSION, List.of(), Collections.nCopies(257, target)));
    }

    @Test
    public void MalformedCountsAreRejectedBeforeAllocatingLists() {
        for (int count : new int[]{-1, 33, Integer.MAX_VALUE}) {
            var buffer = buffer();
            try {
                buffer.writeResourceLocation(DIMENSION);
                buffer.writeVarInt(count);
                assertThrows(DecoderException.class, () -> SandevistanSnapshotPayload.STREAM_CODEC.decode(buffer));
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    public void InvalidRatesCannotPoisonClientSimulation() {
        for (float scale : new float[]{Float.NaN, Float.POSITIVE_INFINITY, 0, -1, 1.1F}) {
            var buffer = buffer();
            try {
                buffer.writeResourceLocation(DIMENSION);
                buffer.writeVarInt(0);
                buffer.writeVarInt(1);
                buffer.writeVarInt(2);
                buffer.writeUUID(UUID.randomUUID());
                buffer.writeFloat(scale);
                assertThrows(DecoderException.class, () -> SandevistanSnapshotPayload.STREAM_CODEC.decode(buffer));
            } finally {
                buffer.release();
            }
        }
    }
}
