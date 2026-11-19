package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class BankingHistoryTest {
    @Test
    public void sameDayIncomeAndExpensesStayVisibleEvenWhenNetIsZero() {
        var history = BankingHistory.summarize(List.of(new BankingHistory.Transaction(10, 400), new BankingHistory.Transaction(10, -400)), 10, 7);
        assertEquals(400, history.income());
        assertEquals(400, history.expense());
        assertEquals(0, history.net());
        assertEquals(400, history.maximum());
        assertEquals(new BankingHistory.Day(10, 400, 400), history.days().getLast());
    }

    @Test
    public void daysWithoutTransactionsStayZeroAndPeriodsExcludeOlderOrFutureRecords() {
        var history = BankingHistory.summarize(List.of(new BankingHistory.Transaction(0, 200), new BankingHistory.Transaction(5, 30),
                new BankingHistory.Transaction(8, -12), new BankingHistory.Transaction(11, 900)), 10, 7);
        assertEquals(7, history.days().size());
        assertEquals(4, history.days().getFirst().day());
        assertEquals(10, history.days().getLast().day());
        assertEquals(30, history.income());
        assertEquals(12, history.expense());
        assertEquals(new BankingHistory.Day(6, 0, 0), history.days().get(2));
        assertEquals(new BankingHistory.Day(10, 0, 0), history.days().getLast());
    }

    @Test
    public void aNewWorldNeverInventsNegativeWorldDaysOrZeroValueActivity() {
        var history = BankingHistory.summarize(List.of(new BankingHistory.Transaction(0, 0)), 0, 30);
        assertEquals(List.of(new BankingHistory.Day(0, 0, 0)), history.days());
        assertEquals(0, history.maximum());
        assertEquals(0, history.income());
        assertEquals(0, history.expense());
    }

    @Test
    public void largeBalancesAndMinimumIntegerExpensesDoNotOverflow() {
        var history = BankingHistory.summarize(List.of(new BankingHistory.Transaction(0, Integer.MAX_VALUE), new BankingHistory.Transaction(0, Integer.MAX_VALUE),
                new BankingHistory.Transaction(0, Integer.MIN_VALUE)), 0, 14);
        assertEquals(4294967294L, history.income());
        assertEquals(2147483648L, history.expense());
        assertEquals(2147483646L, history.net());
        assertEquals(5000000000L, BankingHistory.axisMaximum(history.maximum()));
    }

    @Test
    public void axisRoundsUpToReadableBoundariesAndDoesNotClipLargeValues() {
        assertEquals(2, BankingHistory.axisMaximum(0));
        assertEquals(2, BankingHistory.axisMaximum(1));
        assertEquals(50, BankingHistory.axisMaximum(37));
        assertEquals(100, BankingHistory.axisMaximum(51));
        assertEquals(2000, BankingHistory.axisMaximum(1330));
        for (long value : new long[]{1, 2, 5, 20, 101, 2000000, Integer.MAX_VALUE, Long.MAX_VALUE}) assertTrue(BankingHistory.axisMaximum(value) >= value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aZeroLengthPeriodIsRejected() { BankingHistory.summarize(List.of(), 1, 0); }

    @Test
    public void balanceHistoryWorksBackwardsFromTheCurrentBalanceAndKeepsQuietDays() {
        var history = BankingHistory.summarize(List.of(new BankingHistory.Transaction(0, 100),
                new BankingHistory.Transaction(2, -30), new BankingHistory.Transaction(3, 50)), 3, 7);
        assertEquals(List.of(new BankingHistory.Balance(0, 100), new BankingHistory.Balance(1, 100),
                new BankingHistory.Balance(2, 70), new BankingHistory.Balance(3, 120)), BankingHistory.balances(history, 120));
    }

    @Test
    public void shorterBalancePeriodsKeepTheSameValuesForOverlappingDays() {
        var transactions = List.of(new BankingHistory.Transaction(1, 1000), new BankingHistory.Transaction(12, -200),
                new BankingHistory.Transaction(19, 50));
        var week = BankingHistory.balances(BankingHistory.summarize(transactions, 20, 7), 850);
        var month = BankingHistory.balances(BankingHistory.summarize(transactions, 20, 30), 850);
        assertEquals(month.subList(month.size() - week.size(), month.size()), week);
        assertEquals(new BankingHistory.Balance(20, 850), week.getLast());
        assertEquals(List.of(new BankingHistory.Balance(0, 0)), BankingHistory.balances(BankingHistory.summarize(List.of(), 0, 30), 0));
    }
}
