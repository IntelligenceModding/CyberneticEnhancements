package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record QuestProgressPayload(List<Objective> objectives) implements CustomPacketPayload {
    public static final Type<QuestProgressPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "quest_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestProgressPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {
                buffer.writeVarInt(payload.objectives.size());
                for (var objective : payload.objectives) {
                    buffer.writeUtf(objective.id);
                    ComponentSerialization.STREAM_CODEC.encode(buffer, objective.title);
                    ComponentSerialization.STREAM_CODEC.encode(buffer, objective.detail);
                    buffer.writeVarInt(objective.current);
                    buffer.writeVarInt(objective.required);
                    buffer.writeVarInt(objective.priority);
                    buffer.writeBoolean(objective.counter);
                    buffer.writeBoolean(objective.callTarget != null);
                    if (objective.callTarget != null) {
                        buffer.writeUUID(objective.callTarget.issuerId());
                        ComponentSerialization.STREAM_CODEC.encode(buffer, objective.callTarget.name());
                    }
                }
            }, buffer -> {
                int count = buffer.readVarInt();
                if (count < 0 || count > buffer.readableBytes()) throw new IllegalArgumentException("Invalid quest HUD objective count");
                var objectives = new java.util.ArrayList<Objective>(count);
                for (int i = 0; i < count; i++) objectives.add(new Objective(buffer.readUtf(),
                        ComponentSerialization.STREAM_CODEC.decode(buffer), ComponentSerialization.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readBoolean() ? new CallTarget(buffer.readUUID(), ComponentSerialization.STREAM_CODEC.decode(buffer)) : null));
                return new QuestProgressPayload(objectives);
            });

    public QuestProgressPayload { objectives = List.copyOf(objectives); }
    @Override public Type<QuestProgressPayload> type() { return TYPE; }

    public record CallTarget(java.util.UUID issuerId, Component name) {
        public CallTarget { name = name.copy(); }
    }

    public record Objective(String id, Component title, Component detail, int current, int required, int priority, boolean counter, CallTarget callTarget) {
        public Objective(String id, Component title, Component detail, int current, int required, int priority) {
            this(id, title, detail, current, required, priority, true);
        }
        public Objective(String id, Component title, Component detail, int current, int required, int priority, boolean counter) {
            this(id, title, detail, current, required, priority, counter, null);
        }
        public Objective {
            title = title.copy();
            detail = detail.copy();
            required = Math.max(1, required);
            current = Math.clamp(current, 0, required);
        }
    }
}
