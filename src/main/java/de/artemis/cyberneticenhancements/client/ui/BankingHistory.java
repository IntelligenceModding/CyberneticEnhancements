package de.artemis.cyberneticenhancements.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class BankingHistory {
    private BankingHistory() {}

    public record Transaction(long day, long amount) {}
    public record Balance(long day, long amount) {}
    public record Day(long day, long income, long expense) {
        public long net() { return income - expense; }
    }
    public record Summary(List<Day> days, long income, long expense) {
        public long net() { return income - expense; }
        public long maximum() { return days.stream().mapToLong(day -> Math.max(day.income, day.expense)).max().orElse(0); }
    }

    public static Summary summarize(List<Transaction> transactions, long currentDay, int period) {
        if (period < 1 || period > 365) throw new IllegalArgumentException("Invalid banking period");
        currentDay = Math.max(0, currentDay);
        long first = Math.max(0, currentDay - period + 1);
        int count = (int) (currentDay - first + 1);
        long[] income = new long[count], expense = new long[count];
        for (var transaction : transactions) {
            if (transaction.day < first || transaction.day > currentDay) continue;
            int index = (int) (transaction.day - first);
            if (transaction.amount > 0) income[index] += transaction.amount;
            else expense[index] -= (long) transaction.amount;
        }
        long totalIncome = 0, totalExpense = 0;
        var days = new ArrayList<Day>(count);
        for (int index = 0; index < count; index++) {
            days.add(new Day(first + index, income[index], expense[index]));
            totalIncome += income[index];
            totalExpense += expense[index];
        }
        return new Summary(List.copyOf(days), totalIncome, totalExpense);
    }

    public static long axisMaximum(long maximum) {
        if (maximum <= 2) return 2;
        double magnitude = Math.pow(10, Math.floor(Math.log10(maximum)));
        double normalized = maximum / magnitude;
        double step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
        return (long) Math.min(Long.MAX_VALUE, step * magnitude);
    }

    public static List<Balance> balances(Summary history, long currentBalance) {
        var values = new Balance[history.days().size()];
        long balance = currentBalance;
        for (int index = values.length - 1; index >= 0; index--) {
            var day = history.days().get(index);
            values[index] = new Balance(day.day(), balance);
            balance -= day.net();
        }
        return List.of(values);
    }
}
