package de.artemis.cyberneticenhancements.common.economy;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DailyBankingHistoryTest {
    @Test
    public void dailyTotalsSurviveMoreTransactionsThanTheLedgerCanRetain() throws Exception {
        var data = new CompoundTag();
        var append = PlayerEurodollarManager.class.getDeclaredMethod("appendTransaction", CompoundTag.class, long.class, String.class, int.class, String.class, java.util.UUID.class, String.class);
        append.setAccessible(true);
        for (int index = 0; index < 150; index++) append.invoke(null, data, 5L, "payment", index % 2 == 0 ? 10 : -10, "test", null, null);
        assertEquals(120, data.getList("cyberneticenhancements.eurodollar_ledger", 10).size());
        assertEquals(List.of(new DailyBankingHistory.Day(5, 750, 750)), DailyBankingHistory.snapshot(data, 5, List.of()));
    }

    @Test
    public void legacyHistoryMigratesExactlyOnceAndDoesNotDoubleCountTheNextPayment() {
        var data = new CompoundTag();
        var legacy = List.of(new PlayerEurodollarManager.TransactionEntry(1, 10, "income", 50, "", "", ""),
                new PlayerEurodollarManager.TransactionEntry(2, 10, "expense", -20, "", "", ""));
        DailyBankingHistory.record(data, 10, 10, legacy);
        assertEquals(List.of(new DailyBankingHistory.Day(10, 60, 20)), DailyBankingHistory.snapshot(data, 10, legacy));
        assertEquals(List.of(new DailyBankingHistory.Day(10, 60, 20)), DailyBankingHistory.snapshot(data.copy(), 10, legacy));
    }

    @Test
    public void zeroAmountRequestsDoNotBecomeMoneyMovementAndOldDaysExpire() {
        var data = new CompoundTag();
        DailyBankingHistory.record(data, 1, 100, List.of());
        DailyBankingHistory.record(data, 30, -5, List.of());
        DailyBankingHistory.record(data, 30, 0, List.of());
        assertEquals(2, DailyBankingHistory.snapshot(data, 30, List.of()).size());
        assertEquals(List.of(new DailyBankingHistory.Day(30, 0, 5)), DailyBankingHistory.snapshot(data, 31, List.of()));
    }

    @Test
    public void incomeAndExpensesAboveTheBalanceLimitStayAccurateAfterSaving() {
        var data = new CompoundTag();
        DailyBankingHistory.record(data, 0, Integer.MAX_VALUE, List.of());
        DailyBankingHistory.record(data, 0, Integer.MAX_VALUE, List.of());
        DailyBankingHistory.record(data, 0, Integer.MIN_VALUE, List.of());
        assertEquals(List.of(new DailyBankingHistory.Day(0, 4294967294L, 2147483648L)), DailyBankingHistory.snapshot(data.copy(), 0, List.of()));
    }
}
