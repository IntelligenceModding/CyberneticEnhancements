package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

 public record SandevistanEchoPayload(ResourceLocation dimension, int entityId, UUID owner, long tick,
        Vec3 position, float yaw, float headYaw, float pitch, float scale, boolean crouching,
        float stride, float speed, float attack, boolean mainHandItem, boolean offHandItem) implements CustomPacketPayload {
    public static final Type<SandevistanEchoPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "sandevistan_echo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SandevistanEchoPayload> STREAM_CODEC = new StreamCodec<>() {
        public SandevistanEchoPayload decode(RegistryFriendlyByteBuf b) {
            return new SandevistanEchoPayload(b.readResourceLocation(), b.readVarInt(), b.readUUID(), b.readLong(),
                    new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readFloat(), b.readFloat(), b.readFloat(),
                    b.readFloat(), b.readBoolean(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean());
        }
        public void encode(RegistryFriendlyByteBuf b, SandevistanEchoPayload p) {
            b.writeResourceLocation(p.dimension); b.writeVarInt(p.entityId); b.writeUUID(p.owner); b.writeLong(p.tick);
            b.writeDouble(p.position.x); b.writeDouble(p.position.y); b.writeDouble(p.position.z);
            b.writeFloat(p.yaw); b.writeFloat(p.headYaw); b.writeFloat(p.pitch); b.writeFloat(p.scale);
            b.writeBoolean(p.crouching); b.writeFloat(p.stride); b.writeFloat(p.speed); b.writeFloat(p.attack);
            b.writeBoolean(p.mainHandItem); b.writeBoolean(p.offHandItem);
        }
    };
    public Type<SandevistanEchoPayload> type() { return TYPE; }
}
