package de.artemis.cyberneticenhancements.common.tutorial;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The current tutorial objective and the interface elements introduced so far.
 * Translation keys and arguments are resolved on the client, including key binds.
 */
public record TutorialStatePayload(String chapter, String objective, List<String> args, int step, int total,
                                   Optional<BlockPos> target, List<String> revealed, boolean finished) implements CustomPacketPayload {
    public static final Type<TutorialStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "tutorial_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialStatePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> {
                b.writeUtf(p.chapter, 256);
                b.writeUtf(p.objective, 256);
                writeList(b, p.args);
                b.writeVarInt(p.step);
                b.writeVarInt(p.total);
                b.writeOptional(p.target, (buffer, pos) -> buffer.writeBlockPos(pos));
                writeList(b, p.revealed);
                b.writeBoolean(p.finished);
            }, b -> new TutorialStatePayload(b.readUtf(256), b.readUtf(256), readList(b), b.readVarInt(), b.readVarInt(),
                    b.readOptional(buffer -> buffer.readBlockPos()), readList(b), b.readBoolean()));

    private static void writeList(FriendlyByteBuf buffer, List<String> values) {
        buffer.writeVarInt(values.size());
        for (String value : values) buffer.writeUtf(value, 256);
    }

    private static List<String> readList(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid tutorial list size");
        var values = new ArrayList<String>(count);
        for (int index = 0; index < count; index++) values.add(buffer.readUtf(256));
        return List.copyOf(values);
    }

    @Override public Type<TutorialStatePayload> type() { return TYPE; }
}
