package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SandevistanSnapshotPayload(ResourceLocation dimension, List<User> users, List<Target> targets)
        implements CustomPacketPayload {
    public static final int MAX_USERS = 32;
    public static final int MAX_TARGETS = 256;
    public static final Type<SandevistanSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "sandevistan_snapshot"));

    public SandevistanSnapshotPayload {
        users = List.copyOf(users);
        targets = List.copyOf(targets);
        if (users.size() > MAX_USERS || targets.size() > MAX_TARGETS) throw new IllegalArgumentException("Sandevistan snapshot exceeds bounds");
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SandevistanSnapshotPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SandevistanSnapshotPayload decode(RegistryFriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            int count = readCount(buffer, MAX_USERS);
            List<User> users = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                users.add(new User(buffer.readVarInt(), buffer.readUUID(), buffer.readUtf(64), buffer.readLong(),
                        buffer.readVarInt(), buffer.readUnsignedByte(), readScale(buffer)));
            }
            count = readCount(buffer, MAX_TARGETS);
            List<Target> targets = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                targets.add(new Target(buffer.readVarInt(), buffer.readUUID(), readScale(buffer)));
            }
            return new SandevistanSnapshotPayload(dimension, users, targets);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SandevistanSnapshotPayload payload) {
            buffer.writeResourceLocation(payload.dimension);
            buffer.writeVarInt(payload.users.size());
            for (User user : payload.users) {
                buffer.writeVarInt(user.entityId);
                buffer.writeUUID(user.uuid);
                buffer.writeUtf(user.model, 64);
                buffer.writeLong(user.activationTick);
                buffer.writeVarInt(user.remainingTicks);
                buffer.writeByte(user.mode);
                buffer.writeFloat(user.timeScale);
            }
            buffer.writeVarInt(payload.targets.size());
            for (Target target : payload.targets) {
                buffer.writeVarInt(target.entityId);
                buffer.writeUUID(target.uuid);
                buffer.writeFloat(target.timeScale);
            }
        }
    };

    private static int readCount(RegistryFriendlyByteBuf buffer, int maximum) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) throw new DecoderException("Invalid Sandevistan snapshot size");
        return count;
    }

    private static float readScale(RegistryFriendlyByteBuf buffer) {
        float scale = buffer.readFloat();
        if (!Float.isFinite(scale) || scale < 0.05F || scale > 1) throw new DecoderException("Invalid Sandevistan time scale");
        return scale;
    }

    public record User(int entityId, UUID uuid, String model, long activationTick, int remainingTicks, int mode, float timeScale) {
    }

    public record Target(int entityId, UUID uuid, float timeScale) {
    }

    @Override
    public Type<SandevistanSnapshotPayload> type() { return TYPE; }
}
