package de.artemis.cyberneticenhancements.common.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PsychosisIntroPayload(UUID player, long started, float yaw, float pitch, boolean recovery) implements CustomPacketPayload {
    public PsychosisIntroPayload(UUID player,long started,float yaw,float pitch){this(player,started,yaw,pitch,false);}
    public static final Type<PsychosisIntroPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cyberneticenhancements","psychosis_intro"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PsychosisIntroPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p,b)->{b.writeUUID(p.player);b.writeLong(p.started);b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeBoolean(p.recovery);},
            b->new PsychosisIntroPayload(b.readUUID(),b.readLong(),b.readFloat(),b.readFloat(),b.readBoolean()));
    public Type<PsychosisIntroPayload> type(){return TYPE;}
}
