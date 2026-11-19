package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record QuestNavigationPayload(List<Target> targets, List<TurnIn> turnIns) implements CustomPacketPayload {
    public static final Type<QuestNavigationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            CyberneticEnhancements.MOD_ID, "quest_navigation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestNavigationPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {
                buffer.writeCollection(payload.targets, (out, target) -> {
                out.writeUtf(target.questId);
                out.writeUtf(target.title);
                out.writeUtf(target.dimension);
                out.writeBlockPos(target.pos);
                out.writeBoolean(target.turnIn);
                out.writeUtf(target.itemId);
            });
                buffer.writeCollection(payload.turnIns, (out, turnIn) -> {
                    out.writeUtf(turnIn.questId);
                    out.writeUUID(turnIn.issuerId);
                });
            }, buffer -> new QuestNavigationPayload(buffer.readList(in -> new Target(
                    in.readUtf(), in.readUtf(), in.readUtf(), in.readBlockPos(), in.readBoolean(), in.readUtf())),
                    buffer.readList(in -> new TurnIn(in.readUtf(), in.readUUID()))));

    public QuestNavigationPayload {
        targets = List.copyOf(targets);
        turnIns = List.copyOf(turnIns);
    }

    public QuestNavigationPayload(List<Target> targets) {
        this(targets, List.of());
    }

    @Override
    public Type<QuestNavigationPayload> type() {
        return TYPE;
    }

    public record Target(String questId, String title, String dimension, BlockPos pos, boolean turnIn, String itemId) {
        public Target(String questId, String title, String dimension, BlockPos pos, boolean turnIn) {
            this(questId, title, dimension, pos, turnIn, "");
        }
    }

    public record TurnIn(String questId, UUID issuerId) {}
}
