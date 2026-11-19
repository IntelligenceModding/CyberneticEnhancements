package de.artemis.cyberneticenhancements.common.network;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.economy.DailyBankingHistory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public record ArchiveBankingPayload(
        int balance,
        long currentDay,
        List<DailyBankingHistory.Day> dailyFlow,
        List<LedgerEntry> entries,
        List<RequestEntry> requests,
        String actionId,
        String feedbackKey,
        List<Recipient> recipients
) implements CustomPacketPayload {
    public record Recipient(java.util.UUID id, String name, boolean online, long daysAgo) {}
    public ArchiveBankingPayload(int balance, long currentDay, List<DailyBankingHistory.Day> flow, List<LedgerEntry> entries,
                                 List<RequestEntry> requests, String actionId, String feedbackKey) {
        this(balance, currentDay, flow, entries, requests, actionId, feedbackKey, List.of());
    }
    public ArchiveBankingPayload(int balance, long currentDay, List<LedgerEntry> entries, List<RequestEntry> requests) {
        this(balance, currentDay, List.of(), entries, requests, "", "");
    }

    public ArchiveBankingPayload withActionResult(String actionId, String failureKey) {
        return new ArchiveBankingPayload(balance, currentDay, dailyFlow, entries, requests, actionId, failureKey == null ? "" : failureKey, recipients);
    }
    public static final Type<ArchiveBankingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "archive_banking"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveBankingPayload> STREAM_CODEC =
            CustomPacketPayload.codec((payload, buffer) -> {
                buffer.writeVarInt(payload.balance);
                buffer.writeVarLong(payload.currentDay);
                buffer.writeVarInt(payload.dailyFlow.size());
                for (var day : payload.dailyFlow) {
                    buffer.writeVarLong(day.day()); buffer.writeVarLong(day.income()); buffer.writeVarLong(day.expense());
                }
                buffer.writeVarInt(payload.entries.size());
                for (LedgerEntry entry : payload.entries) {
                    buffer.writeVarLong(entry.sequence());
                    buffer.writeVarLong(entry.day());
                    buffer.writeUtf(entry.typeId());
                    buffer.writeVarInt(entry.amount());
                    buffer.writeUtf(entry.counterpartyName());
                    buffer.writeUtf(entry.note());
                }
                buffer.writeVarInt(payload.requests.size());
                for (RequestEntry request : payload.requests) {
                    buffer.writeUtf(request.requestId());
                    buffer.writeUtf(request.fromName());
                    buffer.writeUtf(request.toName());
                    buffer.writeVarInt(request.amount());
                    buffer.writeBoolean(request.incoming());
                    buffer.writeVarLong(request.createdDay());
                }
                buffer.writeUtf(payload.actionId, 32);
                buffer.writeUtf(payload.feedbackKey, 128);
                buffer.writeVarInt(payload.recipients.size());
                for (var recipient : payload.recipients) {
                    buffer.writeUUID(recipient.id); buffer.writeUtf(recipient.name, 36); buffer.writeBoolean(recipient.online); buffer.writeVarLong(recipient.daysAgo);
                }
            }, buffer -> {
                int balance = buffer.readVarInt();
                long currentDay = buffer.readVarLong();
                int flowCount = buffer.readVarInt();
                if (flowCount < 0 || flowCount > 30) throw new IllegalArgumentException("Invalid banking history length");
                List<DailyBankingHistory.Day> flow = new ArrayList<>(flowCount);
                for (int index = 0; index < flowCount; index++) flow.add(new DailyBankingHistory.Day(buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong()));
                int entryCount = buffer.readVarInt();
                List<LedgerEntry> entries = new ArrayList<>(entryCount);
                for (int i = 0; i < entryCount; i++) {
                    entries.add(new LedgerEntry(
                            buffer.readVarLong(),
                            buffer.readVarLong(),
                            buffer.readUtf(),
                            buffer.readVarInt(),
                            buffer.readUtf(),
                            buffer.readUtf()
                    ));
                }
                int requestCount = buffer.readVarInt();
                List<RequestEntry> requests = new ArrayList<>(requestCount);
                for (int i = 0; i < requestCount; i++) {
                    requests.add(new RequestEntry(
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readVarLong()
                    ));
                }
                String action = buffer.readUtf(32), feedback = buffer.readUtf(128);
                int recipientCount = buffer.readVarInt();
                if (recipientCount < 0 || recipientCount > 100000) throw new IllegalArgumentException("Invalid banking recipient count");
                List<Recipient> recipients = new ArrayList<>(recipientCount);
                for (int index = 0; index < recipientCount; index++) recipients.add(new Recipient(buffer.readUUID(), buffer.readUtf(36), buffer.readBoolean(), buffer.readVarLong()));
                return new ArchiveBankingPayload(balance, currentDay, List.copyOf(flow), List.copyOf(entries), List.copyOf(requests), action, feedback, List.copyOf(recipients));
            });

    @Override
    public Type<ArchiveBankingPayload> type() {
        return TYPE;
    }

    public static ArchiveBankingPayload capture(ServerPlayer player) {
        List<LedgerEntry> entries = new ArrayList<>();
        for (PlayerEurodollarManager.TransactionEntry entry : PlayerEurodollarManager.ledger(player)) {
            entries.add(new LedgerEntry(
                    entry.sequence(),
                    entry.day(),
                    entry.typeId(),
                    entry.amount(),
                    entry.counterpartyName(),
                    entry.note()
            ));
        }
        List<RequestEntry> requests = new ArrayList<>();
        for (PlayerEurodollarManager.PaymentRequest request : PlayerEurodollarManager.requests(player)) {
            requests.add(new RequestEntry(
                    request.requestId().toString(),
                    request.fromPlayerName(),
                    request.toPlayerName(),
                    request.amount(),
                    request.toPlayerId().equals(player.getUUID()),
                    request.createdDay()
            ));
        }
        return new ArchiveBankingPayload(
                PlayerEurodollarManager.balance(player),
                Math.max(0, player.server.overworld().getDayTime() / 24000L),
                DailyBankingHistory.snapshot(player.getPersistentData(), Math.max(0, player.server.overworld().getDayTime() / 24000L), PlayerEurodollarManager.ledger(player)),
                List.copyOf(entries),
                List.copyOf(requests), "", "",
                de.artemis.cyberneticenhancements.common.economy.BankingPlayerDirectory.get(player.server).entries().stream()
                        .filter(entry -> !entry.id().equals(player.getUUID()))
                        .map(entry -> new Recipient(entry.id(), entry.name(), player.server.getPlayerList().getPlayer(entry.id()) != null,
                                de.artemis.cyberneticenhancements.common.economy.BankingPlayerDirectory.daysAgo(entry.lastSeen(), System.currentTimeMillis())))
                        .sorted(java.util.Comparator.comparing(Recipient::online).reversed().thenComparing(Recipient::name, String.CASE_INSENSITIVE_ORDER)).toList()
        );
    }

    public record LedgerEntry(
            long sequence,
            long day,
            String typeId,
            int amount,
            String counterpartyName,
            String note
    ) {
    }

    public record RequestEntry(
            String requestId,
            String fromName,
            String toName,
            int amount,
            boolean incoming,
            long createdDay
    ) {
    }
}
