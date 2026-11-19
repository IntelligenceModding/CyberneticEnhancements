package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public record ArchiveQuestsPayload(List<QuestEntry> quests, boolean dropoffPlaced, boolean setupItemGiven) implements CustomPacketPayload {
    public ArchiveQuestsPayload(List<QuestEntry> quests) { this(quests, false, false); }
    public static final Type<ArchiveQuestsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "archive_quests"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveQuestsPayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buffer) -> {
                buffer.writeVarInt(payload.quests.size());
                for (QuestEntry quest : payload.quests) {
                    buffer.writeUtf(quest.questId());
                    buffer.writeUtf(quest.issuerId());
                    buffer.writeUtf(quest.issuerName());
                    buffer.writeUtf(quest.issuerType());
                    buffer.writeUtf(quest.typeId());
                    buffer.writeUtf(quest.title());
                    buffer.writeUtf(quest.objective());
                    buffer.writeUtf(quest.reward());
                    buffer.writeUtf(quest.statusId());
                    buffer.writeUtf(quest.statusLabel());
                    buffer.writeVarInt(quest.tier());
                    buffer.writeBoolean(quest.hasLocation());
                    buffer.writeUtf(quest.dimensionId());
                    buffer.writeInt(quest.x());
                    buffer.writeInt(quest.y());
                    buffer.writeInt(quest.z());
                    buffer.writeVarInt(quest.sortGroup());
                }
                buffer.writeBoolean(payload.dropoffPlaced);
                buffer.writeBoolean(payload.setupItemGiven);
            }, buffer -> {
                int count = buffer.readVarInt();
                List<QuestEntry> quests = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    quests.add(new QuestEntry(
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readUtf(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readVarInt()
                    ));
                }
                return new ArchiveQuestsPayload(List.copyOf(quests), buffer.readBoolean(), buffer.readBoolean());
            });

    @Override
    public Type<ArchiveQuestsPayload> type() {
        return TYPE;
    }

    public static ArchiveQuestsPayload capture(ServerPlayer player) {
        var deliveries = de.artemis.cyberneticenhancements.common.quest.QuestRewardDeliveries.get(player.server);
        if (!deliveries.hasPlacedDropoff(player.getUUID())) return new ArchiveQuestsPayload(List.of(), false, deliveries.setupItemGiven(player.getUUID()));
        List<QuestEntry> quests = new ArrayList<>();
        for (PlayerQuestManager.QuestLogEntry quest : PlayerQuestManager.questLog(player)) {
            quests.add(new QuestEntry(
                    quest.questId(),
                    quest.issuerId(),
                    quest.issuerName(),
                    quest.issuerType(),
                    quest.typeId(),
                    quest.title(),
                    quest.objective(),
                    quest.reward(),
                    quest.statusId(),
                    quest.statusLabel(),
                    quest.tier(),
                    quest.hasLocation(),
                    quest.dimensionId(),
                    quest.x(),
                    quest.y(),
                    quest.z(),
                    quest.sortGroup()
            ));
        }
        int legacy = 0;
        for (var items : deliveries.unlinkedPendingRewards(player.getUUID())) {
            String rewards = items.stream().map(stack -> stack.getCount() + "x " + stack.getHoverName().getString())
                    .collect(java.util.stream.Collectors.joining(", "));
            quests.add(new QuestEntry("pending_delivery_legacy_" + legacy++, "",
                    Component.translatableWithFallback("screen.cyberneticenhancements.archive.quests.pending.legacy_issuer", "Remote delivery").getString(),
                    "", "", Component.translatableWithFallback("screen.cyberneticenhancements.archive.quests.pending.legacy_title", "Pending reward shipment").getString(),
                    Component.translatableWithFallback("screen.cyberneticenhancements.archive.quests.pending.legacy_note", "This shipment has no saved contract link. Its remaining rewards are still awaiting delivery.").getString(),
                    rewards, "pending_delivery", Component.translatableWithFallback("quest.cyberneticenhancements.status.pending_delivery", "Pending delivery").getString(),
                    0, false, "", 0, 0, 0, 2));
        }
        return new ArchiveQuestsPayload(List.copyOf(quests), true, deliveries.setupItemGiven(player.getUUID()));
    }

    public record QuestEntry(
            String questId,
            String issuerId,
            String issuerName,
            String issuerType,
            String typeId,
            String title,
            String objective,
            String reward,
            String statusId,
            String statusLabel,
            int tier,
            boolean hasLocation,
            String dimensionId,
            int x,
            int y,
            int z,
            int sortGroup
    ) {
    }
}
