package de.artemis.cyberneticenhancements.common.origin;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** One page of a lifepath's roster, with how many players hold it in all. */
public record OriginRosterPayload(String origin, int page, int total, List<String> names) implements CustomPacketPayload {
    public static final Type<OriginRosterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "origin_roster"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OriginRosterPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> {
                b.writeUtf(p.origin, 32);
                b.writeVarInt(p.page);
                b.writeVarInt(p.total);
                b.writeVarInt(p.names.size());
                for (String name : p.names) b.writeUtf(name, 64);
            },
            b -> {
                String origin = b.readUtf(32);
                int page = b.readVarInt(), total = b.readVarInt();
                int size = Math.min(b.readVarInt(), OriginRegistry.PAGE);
                List<String> names = new ArrayList<>(size);
                for (int i = 0; i < size; i++) names.add(b.readUtf(64));
                return new OriginRosterPayload(origin, page, total, List.copyOf(names));
            });
    @Override public Type<OriginRosterPayload> type() { return TYPE; }
}
