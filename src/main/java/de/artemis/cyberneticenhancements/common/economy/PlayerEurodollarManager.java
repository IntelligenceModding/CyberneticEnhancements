package de.artemis.cyberneticenhancements.common.economy;

import de.artemis.cyberneticenhancements.common.util.BalanceMath;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PlayerEurodollarManager {
    private static final String EURODOLLARS_KEY = "cyberneticenhancements.eurodollars";
    private static final String LEDGER_KEY = "cyberneticenhancements.eurodollar_ledger";
    private static final String REQUESTS_KEY = "cyberneticenhancements.eurodollar_requests";
    private static final String SEQUENCE_KEY = "cyberneticenhancements.eurodollar_sequence";
    private static final int MAX_LEDGER_ENTRIES = 120;
    private static final int MAX_PENDING_REQUESTS = 24;

    private PlayerEurodollarManager() {
    }

    public static int balance(Player player) {
        return balance(player.getPersistentData());
    }

    public static void setBalance(Player player, int amount) {
        setBalance(player, amount, "admin_set", "Administrative balance set", null, null);
    }

    public static void setBalance(
            Player player,
            int amount,
            String typeId,
            String note,
            @Nullable UUID counterpartyId,
            @Nullable String counterpartyName
    ) {
        int previous = balance(player);
        int updated = clampBalance(amount);
        setStoredBalance(player.getPersistentData(), updated);
        int delta = updated - previous;
        if (delta != 0) {
            appendTransaction(player, typeId, delta, note, counterpartyId, counterpartyName);
        }
    }

    public static int add(Player player, int amount) {
        return add(player, amount, "income", "Account credit", null, null);
    }

    public static int add(
            Player player,
            int amount,
            String typeId,
            String note,
            @Nullable UUID counterpartyId,
            @Nullable String counterpartyName
    ) {
        if (amount <= 0) {
            return balance(player);
        }
        int previous = balance(player);
        int updated = BalanceMath.adjust(previous, amount);
        setStoredBalance(player.getPersistentData(), updated);
        int delta = updated - previous;
        if (delta != 0) {
            appendTransaction(player, typeId, delta, note, counterpartyId, counterpartyName);
        }
        return updated;
    }

    public static int remove(Player player, int amount) {
        return remove(player, amount, "expense", "Account debit", null, null);
    }

    public static int remove(
            Player player,
            int amount,
            String typeId,
            String note,
            @Nullable UUID counterpartyId,
            @Nullable String counterpartyName
    ) {
        if (amount <= 0) {
            return balance(player);
        }
        int previous = balance(player);
        int updated = Math.max(0, previous - amount);
        setStoredBalance(player.getPersistentData(), updated);
        int delta = updated - previous;
        if (delta != 0) {
            appendTransaction(player, typeId, delta, note, counterpartyId, counterpartyName);
        }
        return updated;
    }

    public static int change(Player player, int delta) {
        return change(player, delta, delta >= 0 ? "income" : "expense", "Account adjustment", null, null);
    }

    public static int change(
            Player player,
            int delta,
            String typeId,
            String note,
            @Nullable UUID counterpartyId,
            @Nullable String counterpartyName
    ) {
        if (delta == 0) {
            return balance(player);
        }
        if (delta > 0) {
            return add(player, delta, typeId, note, counterpartyId, counterpartyName);
        }
        return remove(player, (int) Math.min(-(long) delta, Integer.MAX_VALUE), typeId, note, counterpartyId, counterpartyName);
    }

    public static boolean canAfford(Player player, int amount) {
        return amount <= 0 || balance(player) >= amount;
    }

    public static boolean tryWithdraw(Player player, int amount) {
        return tryWithdraw(player, amount, "expense", "Account debit", null, null);
    }

    public static boolean tryWithdraw(
            Player player,
            int amount,
            String typeId,
            String note,
            @Nullable UUID counterpartyId,
            @Nullable String counterpartyName
    ) {
        if (amount <= 0) {
            return true;
        }
        if (!canAfford(player, amount)) {
            return false;
        }
        remove(player, amount, typeId, note, counterpartyId, counterpartyName);
        return true;
    }

    public static TransferResult sendToPlayer(ServerPlayer from, ServerPlayer to, int amount) {
        if (amount <= 0) {
            return TransferResult.INVALID_AMOUNT;
        }
        if (from.getUUID().equals(to.getUUID())) {
            return TransferResult.SELF_TARGET;
        }
        if (!BalanceMath.canReceive(balance(to), amount)) {
            return TransferResult.INVALID_AMOUNT;
        }
        if (!tryWithdraw(from, amount, "player_sent", "Sent funds", to.getUUID(), to.getGameProfile().getName())) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }
        add(to, amount, "player_received", "Received funds", from.getUUID(), from.getGameProfile().getName());
        return TransferResult.OK;
    }

    public static TransferResult createRequest(ServerPlayer from, ServerPlayer to, int amount) {
        return createRequest(from, to.getUUID(), to.getGameProfile().getName(), amount);
    }

    public static TransferResult createRequest(ServerPlayer from, UUID toPlayerId, String toPlayerName, int amount) {
        if (amount <= 0) {
            return TransferResult.INVALID_AMOUNT;
        }
        if (from.getUUID().equals(toPlayerId)) {
            return TransferResult.SELF_TARGET;
        }
        PaymentRequest request = new PaymentRequest(
                UUID.randomUUID(),
                from.getUUID(),
                from.getGameProfile().getName(),
                toPlayerId,
                toPlayerName,
                amount,
                currentDay(from)
        );
        ServerPlayer onlineTarget = from.server.getPlayerList().getPlayer(toPlayerId);
        if (onlineTarget != null) {
            appendRequest(from, request);
            appendRequest(onlineTarget, request);
            appendTransaction(from, "request_sent", 0, "Requested funds", toPlayerId, toPlayerName);
            appendTransaction(onlineTarget, "request_received", 0, "Incoming payment request", from.getUUID(), from.getGameProfile().getName());
            return TransferResult.OK;
        }
        if (!updateOfflinePlayerData(from.server, toPlayerId, data -> {
            appendRequest(data, request);
            appendTransaction(data, currentDay(from), "request_received", 0, "Incoming payment request", from.getUUID(), from.getGameProfile().getName());
        })) {
            return TransferResult.TARGET_NOT_FOUND;
        }
        appendRequest(from, request);
        appendTransaction(from, "request_sent", 0, "Requested funds", toPlayerId, toPlayerName);
        return TransferResult.OK;
    }

    public static TransferResult approveRequest(ServerPlayer approver, UUID requestId) {
        PaymentRequest request = findRequest(approver, requestId);
        if (request == null) {
            return TransferResult.REQUEST_NOT_FOUND;
        }
        if (!request.toPlayerId().equals(approver.getUUID())) {
            return TransferResult.REQUEST_NOT_INCOMING;
        }
        if (request.amount() <= 0 || request.fromPlayerId().equals(approver.getUUID())) {
            return TransferResult.INVALID_AMOUNT;
        }
        if (!canAfford(approver, request.amount())) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }
        ServerPlayer requester = approver.server.getPlayerList().getPlayer(request.fromPlayerId());
        if (requester != null) {
            if (!BalanceMath.canReceive(balance(requester), request.amount())) {
                return TransferResult.INVALID_AMOUNT;
            }
            add(requester, request.amount(), "request_received_paid", "Request payment received", approver.getUUID(), approver.getGameProfile().getName());
            removeRequest(requester, requestId);
        } else {
            OfflinePlayerData data = loadOfflinePlayerData(approver.server, request.fromPlayerId());
            if (data == null) {
                return TransferResult.TARGET_NOT_FOUND;
            }
            if (!BalanceMath.canReceive(balance(data.persistentData()), request.amount())) {
                return TransferResult.INVALID_AMOUNT;
            }
            setStoredBalance(data.persistentData(), BalanceMath.adjust(balance(data.persistentData()), request.amount()));
            appendTransaction(data.persistentData(), currentDay(approver), "request_received_paid", request.amount(), "Request payment received", approver.getUUID(), approver.getGameProfile().getName());
            removeRequest(data.persistentData(), requestId);
            if (!saveOfflinePlayerData(data)) {
                return TransferResult.TARGET_NOT_FOUND;
            }
        }

        remove(approver, request.amount(), "request_paid", "Paid requested funds", request.fromPlayerId(), request.fromPlayerName());
        removeRequest(approver, requestId);
        return TransferResult.OK;
    }

    public static TransferResult declineRequest(ServerPlayer actor, UUID requestId) {
        PaymentRequest request = findRequest(actor, requestId);
        if (request == null) {
            return TransferResult.REQUEST_NOT_FOUND;
        }
        boolean incoming = request.toPlayerId().equals(actor.getUUID());
        boolean outgoing = request.fromPlayerId().equals(actor.getUUID());
        if (!incoming && !outgoing) {
            return TransferResult.REQUEST_NOT_FOUND;
        }
        ServerPlayer counterpart = actor.server.getPlayerList().getPlayer(incoming ? request.fromPlayerId() : request.toPlayerId());
        if (counterpart != null) {
            removeRequest(counterpart, requestId);
            appendTransaction(counterpart, "request_declined", 0, "Payment request closed", actor.getUUID(), actor.getGameProfile().getName());
        } else if (offlinePlayerDataExists(actor.server, incoming ? request.fromPlayerId() : request.toPlayerId())
                && !updateOfflinePlayerData(actor.server, incoming ? request.fromPlayerId() : request.toPlayerId(), data -> {
            removeRequest(data, requestId);
            appendTransaction(data, currentDay(actor), "request_declined", 0, "Payment request closed", actor.getUUID(), actor.getGameProfile().getName());
        })) {
            return TransferResult.TARGET_NOT_FOUND;
        }
        removeRequest(actor, requestId);
        appendTransaction(actor, "request_declined", 0, "Payment request closed", counterpart == null ? null : counterpart.getUUID(), counterpart == null ? (incoming ? request.fromPlayerName() : request.toPlayerName()) : counterpart.getGameProfile().getName());
        return TransferResult.OK;
    }

    public static List<TransactionEntry> ledger(Player player) {
        List<TransactionEntry> entries = readLedger(player.getPersistentData());
        entries.sort(Comparator.comparingLong(TransactionEntry::sequence).reversed());
        return List.copyOf(entries);
    }

    public static List<PaymentRequest> requests(Player player) {
        List<PaymentRequest> requests = readRequests(player.getPersistentData());
        requests.sort(Comparator.comparingLong(PaymentRequest::createdDay).reversed().thenComparing(request -> request.requestId().toString()));
        return List.copyOf(requests);
    }

    public static int totalIncome(Player player) {
        long total = 0;
        for (TransactionEntry entry : ledger(player)) {
            if (entry.amount() > 0) {
                total += entry.amount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static int totalExpense(Player player) {
        long total = 0;
        for (TransactionEntry entry : ledger(player)) {
            if (entry.amount() < 0) {
                total -= (long) entry.amount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static int[] graphNet(Player player, int buckets) {
        long[] totals = new long[Math.max(1, buckets)];
        long currentDay = currentDay(player);
        for (TransactionEntry entry : ledger(player)) {
            long delta = currentDay - entry.day();
            if (delta < 0 || delta >= totals.length) {
                continue;
            }
            int index = totals.length - 1 - (int) delta;
            totals[index] += entry.amount();
        }
        int[] values = new int[totals.length];
        for (int i = 0; i < totals.length; i++) {
            values[i] = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, totals[i]));
        }
        return values;
    }

    public static void copyState(Player fromPlayer, Player toPlayer) {
        CompoundTag fromData = fromPlayer.getPersistentData();
        CompoundTag toData = toPlayer.getPersistentData();
        copyTagIfPresent(fromData, toData, EURODOLLARS_KEY);
        copyTagIfPresent(fromData, toData, LEDGER_KEY);
        copyTagIfPresent(fromData, toData, REQUESTS_KEY);
        copyTagIfPresent(fromData, toData, SEQUENCE_KEY);
    }

    private static void copyTagIfPresent(CompoundTag fromData, CompoundTag toData, String key) {
        if (fromData.contains(key)) {
            toData.put(key, fromData.get(key).copy());
        } else {
            toData.remove(key);
        }
    }

    private static int balance(CompoundTag persistentData) {
        return clampBalance(persistentData.getInt(EURODOLLARS_KEY));
    }

    private static void setStoredBalance(CompoundTag persistentData, int amount) {
        persistentData.putInt(EURODOLLARS_KEY, clampBalance(amount));
    }

    private static void appendTransaction(
            Player player,
            String typeId,
            int amount,
            String note,
            @Nullable UUID counterpartyId,
            @Nullable String counterpartyName
    ) {
        appendTransaction(player.getPersistentData(), currentDay(player), typeId, amount, note, counterpartyId, counterpartyName);
    }

    private static void appendTransaction(CompoundTag persistentData, long day, String typeId, int amount, String note,
                                          @Nullable UUID counterpartyId, @Nullable String counterpartyName) {
        List<TransactionEntry> entries = readLedger(persistentData);
        entries.add(0, new TransactionEntry(
                nextSequence(persistentData),
                day,
                sanitizeType(typeId),
                amount,
                counterpartyId == null ? "" : counterpartyId.toString(),
                counterpartyName == null ? "" : counterpartyName,
                note == null ? "" : note
        ));
        if (entries.size() > MAX_LEDGER_ENTRIES) {
            entries = new ArrayList<>(entries.subList(0, MAX_LEDGER_ENTRIES));
        }
        writeLedger(persistentData, entries);
    }

    private static void appendRequest(Player player, PaymentRequest request) {
        appendRequest(player.getPersistentData(), request);
    }

    private static void appendRequest(CompoundTag persistentData, PaymentRequest request) {
        List<PaymentRequest> requests = readRequests(persistentData);
        requests.removeIf(existing -> existing.requestId().equals(request.requestId()));
        requests.add(0, request);
        if (requests.size() > MAX_PENDING_REQUESTS) {
            requests = new ArrayList<>(requests.subList(0, MAX_PENDING_REQUESTS));
        }
        writeRequests(persistentData, requests);
    }

    private static void removeRequest(Player player, UUID requestId) {
        removeRequest(player.getPersistentData(), requestId);
    }

    private static void removeRequest(CompoundTag persistentData, UUID requestId) {
        List<PaymentRequest> requests = readRequests(persistentData);
        if (requests.removeIf(request -> request.requestId().equals(requestId))) {
            writeRequests(persistentData, requests);
        }
    }

    private static @Nullable PaymentRequest findRequest(Player player, UUID requestId) {
        for (PaymentRequest request : readRequests(player.getPersistentData())) {
            if (request.requestId().equals(requestId)) {
                return request;
            }
        }
        return null;
    }

    private static List<TransactionEntry> readLedger(CompoundTag persistentData) {
        List<TransactionEntry> entries = new ArrayList<>();
        ListTag listTag = persistentData.getList(LEDGER_KEY, Tag.TAG_COMPOUND);
        for (Tag tag : listTag) {
            if (tag instanceof CompoundTag compoundTag) {
                entries.add(TransactionEntry.fromTag(compoundTag));
            }
        }
        return entries;
    }

    private static void writeLedger(CompoundTag persistentData, List<TransactionEntry> entries) {
        ListTag listTag = new ListTag();
        for (TransactionEntry entry : entries) {
            listTag.add(entry.toTag());
        }
        persistentData.put(LEDGER_KEY, listTag);
    }

    private static List<PaymentRequest> readRequests(CompoundTag persistentData) {
        List<PaymentRequest> requests = new ArrayList<>();
        ListTag listTag = persistentData.getList(REQUESTS_KEY, Tag.TAG_COMPOUND);
        for (Tag tag : listTag) {
            if (tag instanceof CompoundTag compoundTag) {
                try {
                    PaymentRequest request = PaymentRequest.fromTag(compoundTag);
                    if (request.amount() > 0 && !request.fromPlayerId().equals(request.toPlayerId())) {
                        requests.add(request);
                    }
                } catch (IllegalArgumentException ignored) {

                }
            }
        }
        return requests;
    }

    private static void writeRequests(CompoundTag persistentData, List<PaymentRequest> requests) {
        ListTag listTag = new ListTag();
        for (PaymentRequest request : requests) {
            listTag.add(request.toTag());
        }
        persistentData.put(REQUESTS_KEY, listTag);
    }

    private static long nextSequence(CompoundTag persistentData) {
        long next = persistentData.getLong(SEQUENCE_KEY) + 1L;
        persistentData.putLong(SEQUENCE_KEY, next);
        return next;
    }

    private static long currentDay(Player player) {
        return (player.getServer() == null ? player.level().getDayTime() : player.getServer().overworld().getDayTime()) / 24000L;
    }

    private static String sanitizeType(String raw) {
        return raw == null || raw.isBlank()
                ? "generic"
                : raw.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static int clampBalance(int amount) {
        return BalanceMath.clamp(amount);
    }

    private static boolean updateOfflinePlayerData(MinecraftServer server, UUID playerId, java.util.function.Consumer<CompoundTag> mutator) {
        OfflinePlayerData data = loadOfflinePlayerData(server, playerId);
        if (data == null) {
            return false;
        }
        mutator.accept(data.persistentData());
        return saveOfflinePlayerData(data);
    }

    private static @Nullable OfflinePlayerData loadOfflinePlayerData(MinecraftServer server, UUID playerId) {
        Path file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerId + ".dat");
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return null;
        }
        try {
            CompoundTag rootTag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            if (rootTag == null) {
                return null;
            }
            CompoundTag persistentData = rootTag.contains("ForgeData", Tag.TAG_COMPOUND)
                    ? rootTag.getCompound("ForgeData").copy()
                    : new CompoundTag();
            return new OfflinePlayerData(file, rootTag, persistentData);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static boolean saveOfflinePlayerData(OfflinePlayerData data) {
        try {
            data.rootTag().put("ForgeData", data.persistentData());
            Path parent = data.file().getParent();
            if (parent != null) {
                parent.toFile().mkdirs();
            }
            File tempBackingFile = File.createTempFile(data.file().getFileName().toString() + "-", ".dat", parent == null ? null : parent.toFile());
            Path tempFile = tempBackingFile.toPath();
            NbtIo.writeCompressed(data.rootTag(), tempFile);
            Path backupFile = data.file().resolveSibling(data.file().getFileName().toString() + "_old");
            return Util.safeReplaceOrMoveFile(data.file(), tempFile, backupFile, false);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean offlinePlayerDataExists(MinecraftServer server, UUID playerId) {
        Path file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerId + ".dat");
        return Files.exists(file) && Files.isRegularFile(file);
    }

    private record OfflinePlayerData(Path file, CompoundTag rootTag, CompoundTag persistentData) {
    }

    public enum TransferResult {
        OK,
        INVALID_AMOUNT,
        SELF_TARGET,
        INSUFFICIENT_FUNDS,
        TARGET_NOT_FOUND,
        TARGET_OFFLINE,
        REQUEST_NOT_FOUND,
        REQUEST_NOT_INCOMING
    }

    public record TransactionEntry(
            long sequence,
            long day,
            String typeId,
            int amount,
            String counterpartyId,
            String counterpartyName,
            String note
    ) {
        private static final String SEQUENCE_TAG = "Sequence";
        private static final String DAY_TAG = "Day";
        private static final String TYPE_TAG = "Type";
        private static final String AMOUNT_TAG = "Amount";
        private static final String COUNTERPARTY_ID_TAG = "CounterpartyId";
        private static final String COUNTERPARTY_NAME_TAG = "CounterpartyName";
        private static final String NOTE_TAG = "Note";

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong(SEQUENCE_TAG, sequence);
            tag.putLong(DAY_TAG, day);
            tag.putString(TYPE_TAG, typeId);
            tag.putInt(AMOUNT_TAG, amount);
            tag.putString(COUNTERPARTY_ID_TAG, counterpartyId);
            tag.putString(COUNTERPARTY_NAME_TAG, counterpartyName);
            tag.putString(NOTE_TAG, note);
            return tag;
        }

        public static TransactionEntry fromTag(CompoundTag tag) {
            return new TransactionEntry(
                    tag.getLong(SEQUENCE_TAG),
                    tag.getLong(DAY_TAG),
                    tag.getString(TYPE_TAG),
                    tag.getInt(AMOUNT_TAG),
                    tag.getString(COUNTERPARTY_ID_TAG),
                    tag.getString(COUNTERPARTY_NAME_TAG),
                    tag.getString(NOTE_TAG)
            );
        }
    }

    public record PaymentRequest(
            UUID requestId,
            UUID fromPlayerId,
            String fromPlayerName,
            UUID toPlayerId,
            String toPlayerName,
            int amount,
            long createdDay
    ) {
        private static final String REQUEST_ID_TAG = "RequestId";
        private static final String FROM_ID_TAG = "FromPlayerId";
        private static final String FROM_NAME_TAG = "FromPlayerName";
        private static final String TO_ID_TAG = "ToPlayerId";
        private static final String TO_NAME_TAG = "ToPlayerName";
        private static final String AMOUNT_TAG = "Amount";
        private static final String CREATED_DAY_TAG = "CreatedDay";

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString(REQUEST_ID_TAG, requestId.toString());
            tag.putString(FROM_ID_TAG, fromPlayerId.toString());
            tag.putString(FROM_NAME_TAG, fromPlayerName);
            tag.putString(TO_ID_TAG, toPlayerId.toString());
            tag.putString(TO_NAME_TAG, toPlayerName);
            tag.putInt(AMOUNT_TAG, amount);
            tag.putLong(CREATED_DAY_TAG, createdDay);
            return tag;
        }

        public static PaymentRequest fromTag(CompoundTag tag) {
            return new PaymentRequest(
                    UUID.fromString(tag.getString(REQUEST_ID_TAG)),
                    UUID.fromString(tag.getString(FROM_ID_TAG)),
                    tag.getString(FROM_NAME_TAG),
                    UUID.fromString(tag.getString(TO_ID_TAG)),
                    tag.getString(TO_NAME_TAG),
                    tag.getInt(AMOUNT_TAG),
                    tag.getLong(CREATED_DAY_TAG)
            );
        }
    }
}
