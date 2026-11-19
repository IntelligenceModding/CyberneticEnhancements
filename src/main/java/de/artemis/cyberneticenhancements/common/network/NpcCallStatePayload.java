package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public record NpcCallStatePayload(UUID callId, UUID npcId, String role, String name, String category, String appearance,
                                  String nameColor, Component dialogue, List<Choice> choices, boolean ended, boolean incoming) implements CustomPacketPayload {
    public Component displayName() {
        var color = net.minecraft.ChatFormatting.getByName(nameColor);
        return Component.literal(name).withStyle(color != null && color.isColor() ? color
                : "hidden".equals(nameColor) ? net.minecraft.ChatFormatting.WHITE : net.minecraft.ChatFormatting.AQUA);
    }

    public NpcCallStatePayload(UUID callId, UUID npcId, String role, String name, String category, String appearance,
                               String nameColor, Component dialogue, List<Choice> choices, boolean ended) {
        this(callId, npcId, role, name, category, appearance, nameColor, dialogue, choices, ended, false);
    }
    public record Choice(int action, Component label) {}
    public static final Type<NpcCallStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "npc_call_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcCallStatePayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {
                buffer.writeUUID(payload.callId);
                buffer.writeUUID(payload.npcId);
                buffer.writeUtf(payload.role, 32);
                buffer.writeUtf(payload.name, 128);
                buffer.writeUtf(payload.category, 32);
                buffer.writeUtf(payload.appearance, 256);
                buffer.writeUtf(payload.nameColor, 32);
                ComponentSerialization.STREAM_CODEC.encode(buffer, payload.dialogue);
                buffer.writeVarInt(payload.choices.size());
                for (Choice choice : payload.choices) {
                    buffer.writeVarInt(choice.action);
                    ComponentSerialization.STREAM_CODEC.encode(buffer, choice.label);
                }
                buffer.writeBoolean(payload.ended);
                buffer.writeBoolean(payload.incoming);
            }, buffer -> {
                UUID call = buffer.readUUID(), npc = buffer.readUUID();
                String role = buffer.readUtf(32), name = buffer.readUtf(128), category = buffer.readUtf(32);
                String appearance = buffer.readUtf(256), nameColor = buffer.readUtf(32);
                Component dialogue = ComponentSerialization.STREAM_CODEC.decode(buffer);
                int count = buffer.readVarInt();
                if (count < 0 || count > 5) throw new IllegalArgumentException("Invalid call choice count");
                List<Choice> choices = new ArrayList<>(count);
                for (int index = 0; index < count; index++) choices.add(new Choice(buffer.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(buffer)));
                return new NpcCallStatePayload(call, npc, role, name, category, appearance, nameColor, dialogue, List.copyOf(choices), buffer.readBoolean(), buffer.readBoolean());
            });

    @Override
    public Type<NpcCallStatePayload> type() { return TYPE; }
}
