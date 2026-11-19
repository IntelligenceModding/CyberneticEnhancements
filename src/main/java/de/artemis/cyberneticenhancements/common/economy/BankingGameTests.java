package de.artemis.cyberneticenhancements.common.economy;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.network.ArchiveBankingPayload;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(CyberneticEnhancements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BankingGameTests {
    @GameTest(template = "sandevistan_test", batch = "banking_graph", timeoutTicks = 100)
    public static void graphsUseRealTransactionsAcrossDaysAndSurviveLedgerTruncation(GameTestHelper h) {
        var account = player(h, "bank-graph");
        var world = account.player.server.overworld();
        long originalTime = world.getDayTime();
        try {
            world.setDayTime(0);
            PlayerEurodollarManager.setBalance(account.player, 100);
            world.setDayTime(2 * 24000L);
            PlayerEurodollarManager.add(account.player, 50);
            PlayerEurodollarManager.remove(account.player, 20);
            world.setDayTime(10 * 24000L);
            PlayerEurodollarManager.add(account.player, 200);
            PlayerEurodollarManager.remove(account.player, 30);
            world.setDayTime(20 * 24000L);
            PlayerEurodollarManager.remove(account.player, 40);
            world.setDayTime(31 * 24000L);
            PlayerEurodollarManager.add(account.player, 10);
            for (int index = 0; index < 150; index++) PlayerEurodollarManager.add(account.player, 1);
            var snapshot = ArchiveBankingPayload.capture(account.player);
            roundTrip(h, snapshot);
            h.assertTrue(snapshot.balance() == 420 && snapshot.entries().size() == 120, "Graph fixture lost actual balance or ledger truncation");
            var transactions = snapshot.dailyFlow().stream().flatMap(day -> java.util.stream.Stream.of(
                    new de.artemis.cyberneticenhancements.client.ui.BankingHistory.Transaction(day.day(), day.income()),
                    new de.artemis.cyberneticenhancements.client.ui.BankingHistory.Transaction(day.day(), -day.expense()))).toList();
            for (int period : new int[]{7, 14, 30}) {
                var history = de.artemis.cyberneticenhancements.client.ui.BankingHistory.summarize(transactions, snapshot.currentDay(), period);
                var balances = de.artemis.cyberneticenhancements.client.ui.BankingHistory.balances(history, snapshot.balance());
                h.assertTrue(history.days().size() == period && history.days().getFirst().day() == 32 - period, "Selected graph period did not change its date range");
                h.assertTrue(history.income() == (period == 30 ? 410 : 160) && history.expense() == (period == 30 ? 90 : period == 14 ? 40 : 0), "Graph income or spending disagreed with actual transactions");
                h.assertTrue(balances.getLast().amount() == 420, "Balance graph did not end at the live balance");
                long expectedOpening = period == 30 ? 130 : period == 14 ? 300 : 260;
                h.assertTrue(balances.getFirst().amount() == expectedOpening, "Balance history reconstructed an incorrect earlier balance");
                h.assertTrue(history.days().stream().anyMatch(day -> day.income() == 0 && day.expense() == 0), "Quiet days disappeared from the graph");
            }
            var persisted = DailyBankingHistory.snapshot(account.player.getPersistentData().copy(), 31, PlayerEurodollarManager.ledger(account.player));
            h.assertTrue(persisted.equals(snapshot.dailyFlow()) && persisted.stream().anyMatch(day -> day.day() == 2 && day.income() == 50 && day.expense() == 20), "Saving lost chart totals older than the retained ledger");
        } finally {
            world.setDayTime(originalTime);
            account.player.server.getPlayerList().remove(account.player);
            account.channel.finishAndReleaseAll();
        }
        h.succeed();
    }

    @GameTest(template = "sandevistan_test", batch = "banking_archive", timeoutTicks = 100)
    public static void accountSnapshotsKeepTransfersRequestsAndActionResultsConsistent(GameTestHelper h) {
        var from = player(h, "bank-sender");
        var to = player(h, "bank-recipient");
        try {
            PlayerEurodollarManager.setBalance(from.player, 100);
            PlayerEurodollarManager.setBalance(to.player, 0);
            h.assertTrue(PlayerEurodollarManager.sendToPlayer(from.player, to.player, 25) == PlayerEurodollarManager.TransferResult.OK, "Transfer failed");
            var sender = ArchiveBankingPayload.capture(from.player).withActionResult("send", null);
            h.assertTrue(sender.recipients().stream().anyMatch(recipient -> recipient.id().equals(to.player.getUUID()) && recipient.online())
                    && sender.recipients().stream().noneMatch(recipient -> recipient.id().equals(from.player.getUUID())), "Recipient list missed an online player or included the account owner");
            h.assertTrue(sender.balance() == 75 && sender.currentDay() == Math.max(0, from.player.server.overworld().getDayTime() / 24000L), "Snapshot balance or day did not match the server");
            h.assertTrue(sender.entries().stream().anyMatch(entry -> entry.amount() == -25 && entry.counterpartyName().equals("bank-recipient")), "History lost the signed transfer amount or recipient");
            h.assertTrue(sender.dailyFlow().stream().anyMatch(day -> day.expense() == 25), "Chart snapshot lost daily spending");
            roundTrip(h, sender);
            roundTrip(h, new ArchiveBankingPayload(sender.balance(), sender.currentDay(), java.util.List.of(new DailyBankingHistory.Day(sender.currentDay(), 5000000000L, 4000000000L)),
                    sender.entries(), sender.requests(), "", ""));
            h.assertTrue(PlayerEurodollarManager.createRequest(from.player, to.player, 10) == PlayerEurodollarManager.TransferResult.OK, "Payment request failed");
            var requested = ArchiveBankingPayload.capture(from.player).withActionResult("request", null);
            var incoming = ArchiveBankingPayload.capture(to.player);
            h.assertTrue(requested.requests().size() == 1 && !requested.requests().getFirst().incoming(), "Requester cannot see an outgoing request");
            h.assertTrue(incoming.requests().size() == 1 && incoming.requests().getFirst().incoming(), "Recipient cannot see an incoming request");
            roundTrip(h, incoming);
            var requestId = UUID.fromString(incoming.requests().getFirst().requestId());
            h.assertTrue(PlayerEurodollarManager.approveRequest(to.player, requestId) == PlayerEurodollarManager.TransferResult.OK, "Request could not be paid");
            h.assertTrue(PlayerEurodollarManager.balance(from.player) == 85 && PlayerEurodollarManager.balance(to.player) == 15, "Approval debited or credited the wrong account");
            h.assertTrue(ArchiveBankingPayload.capture(from.player).requests().isEmpty() && ArchiveBankingPayload.capture(to.player).requests().isEmpty(), "Paid request remained visible");
            h.assertTrue(PlayerEurodollarManager.approveRequest(to.player, requestId) != PlayerEurodollarManager.TransferResult.OK
                    && PlayerEurodollarManager.balance(to.player) == 15, "Request could be paid twice");
            var failure = ArchiveBankingPayload.capture(to.player).withActionResult("approve", "message.cyberneticenhancements.archive.banking.request_missing");
            roundTrip(h, failure);
            h.assertTrue(!failure.feedbackKey().isEmpty() && failure.actionId().equals("approve"), "Inline feedback lost its action or failure reason");
            PlayerEurodollarManager.createRequest(from.player, to.player, 50);
            requestId = UUID.fromString(ArchiveBankingPayload.capture(from.player).requests().getFirst().requestId());
            h.assertTrue(PlayerEurodollarManager.declineRequest(from.player, requestId) == PlayerEurodollarManager.TransferResult.OK
                    && ArchiveBankingPayload.capture(to.player).requests().isEmpty(), "Outgoing cancellation did not remove both copies");
            var beforeClone = ArchiveBankingPayload.capture(from.player);
            PlayerEurodollarManager.copyState(from.player, to.player);
            h.assertTrue(ArchiveBankingPayload.capture(to.player).dailyFlow().equals(beforeClone.dailyFlow()), "Respawn lost the daily chart history");
            to.player.server.getPlayerList().remove(to.player);
            BankingPlayerDirectory.get(from.player.server).record(to.player.getUUID(), to.player.getGameProfile().getName(), System.currentTimeMillis() - 24 * 86400000L);
            var offline = ArchiveBankingPayload.capture(from.player);
            h.assertTrue(offline.recipients().stream().anyMatch(recipient -> recipient.id().equals(to.player.getUUID()) && !recipient.online() && recipient.daysAgo() == 24), "Offline recipient or real-day last-seen time was lost");
            roundTrip(h, offline);
        } finally {
            from.player.server.getPlayerList().remove(from.player);
            if (to.player.server.getPlayerList().getPlayer(to.player.getUUID()) != null) to.player.server.getPlayerList().remove(to.player);
            from.channel.finishAndReleaseAll(); to.channel.finishAndReleaseAll();
        }
        h.succeed();
    }

    private static void roundTrip(GameTestHelper h, ArchiveBankingPayload payload) {
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            ArchiveBankingPayload.STREAM_CODEC.encode(buffer, payload);
            h.assertTrue(payload.equals(ArchiveBankingPayload.STREAM_CODEC.decode(buffer)) && buffer.readableBytes() == 0, "Banking snapshot failed its network round trip");
        } finally { buffer.release(); }
    }

    private static BankPlayer player(GameTestHelper h, String name) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), name);
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, net.minecraft.server.level.ClientInformation.createDefault(), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new BankPlayer(player, channel);
    }

    private record BankPlayer(ServerPlayer player, io.netty.channel.embedded.EmbeddedChannel channel) {}
}
