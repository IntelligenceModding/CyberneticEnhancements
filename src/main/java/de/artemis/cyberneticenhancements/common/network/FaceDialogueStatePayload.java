package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/** What the person in front of you is saying, and what you can answer. An empty {@code speaker} with no line closes it. */
public record FaceDialogueStatePayload(int npcId, Component speaker, Component line, List<Option> options, boolean open, @Nullable Header header) implements CustomPacketPayload {
    /** {@code locked} is why an answer cannot be given yet, or {@code null} when it can. */
    public record Option(Component label, @Nullable Component locked) {
        public boolean available() { return locked == null; }
    }

    /** What sits in the corner of the speaker's box: who they are and how far they trust you. */
    /** {@code windows} and {@code locked} are bit masks over the window numbers of {@link FaceDialogueWindowPayload}, from 1. */
    public record Header(Component type, Component trustLabel, int trustPercent, int windows, int locked) {}

    public static final int MAX_OPTIONS = 6;
    public static final Type<FaceDialogueStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "face_dialogue_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FaceDialogueStatePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {
                buffer.writeVarInt(payload.npcId);
                ComponentSerialization.STREAM_CODEC.encode(buffer, payload.speaker);
                ComponentSerialization.STREAM_CODEC.encode(buffer, payload.line);
                buffer.writeVarInt(payload.options.size());
                for (var option : payload.options) {
                    ComponentSerialization.STREAM_CODEC.encode(buffer, option.label);
                    buffer.writeBoolean(option.locked != null);
                    if (option.locked != null) ComponentSerialization.STREAM_CODEC.encode(buffer, option.locked);
                }
                buffer.writeBoolean(payload.open);
                buffer.writeBoolean(payload.header != null);
                if (payload.header != null) {
                    ComponentSerialization.STREAM_CODEC.encode(buffer, payload.header.type);
                    ComponentSerialization.STREAM_CODEC.encode(buffer, payload.header.trustLabel);
                    buffer.writeVarInt(payload.header.trustPercent);
                    buffer.writeVarInt(payload.header.windows);
                    buffer.writeVarInt(payload.header.locked);
                }
            }, buffer -> {
                int npc = buffer.readVarInt();
                var speaker = ComponentSerialization.STREAM_CODEC.decode(buffer);
                var line = ComponentSerialization.STREAM_CODEC.decode(buffer);
                int count = buffer.readVarInt();
                if (count < 0 || count > MAX_OPTIONS) throw new IllegalArgumentException("Invalid dialogue option count");
                var options = new ArrayList<Option>(count);
                for (int index = 0; index < count; index++) {
                    var label = ComponentSerialization.STREAM_CODEC.decode(buffer);
                    options.add(new Option(label, buffer.readBoolean() ? ComponentSerialization.STREAM_CODEC.decode(buffer) : null));
                }
                boolean open = buffer.readBoolean();
                var header = buffer.readBoolean() ? new Header(ComponentSerialization.STREAM_CODEC.decode(buffer),
                        ComponentSerialization.STREAM_CODEC.decode(buffer), Math.clamp(buffer.readVarInt(), 0, 100), buffer.readVarInt(), buffer.readVarInt()) : null;
                return new FaceDialogueStatePayload(npc, speaker, line, List.copyOf(options), open, header);
            });

    public static FaceDialogueStatePayload closed() {
        return new FaceDialogueStatePayload(0, Component.empty(), Component.empty(), List.of(), false, null);
    }

    @Override
    public Type<FaceDialogueStatePayload> type() { return TYPE; }
}
