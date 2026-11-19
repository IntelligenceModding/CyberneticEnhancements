package de.artemis.cyberneticenhancements.common.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.TreeMap;

public final class DailyBankingHistory {
    static final String KEY = "cyberneticenhancements.eurodollar_daily_history";
    public record Day(long day, long income, long expense) {}

    private DailyBankingHistory() {}

    public static List<Day> snapshot(CompoundTag data, long currentDay, List<PlayerEurodollarManager.TransactionEntry> legacy) {
        var days = read(data, currentDay, legacy);
        save(data, days);
        return List.copyOf(days.values());
    }

    static void record(CompoundTag data, long day, int amount, List<PlayerEurodollarManager.TransactionEntry> legacy) {
        day = Math.max(0, day);
        var days = read(data, day, legacy);
        if (amount != 0) {
            var previous = days.getOrDefault(day, new Day(day, 0, 0));
            days.put(day, new Day(day, previous.income + Math.max(0L, amount), previous.expense + Math.max(0L, -(long) amount)));
        }
        save(data, days);
    }

    private static TreeMap<Long, Day> read(CompoundTag data, long currentDay, List<PlayerEurodollarManager.TransactionEntry> legacy) {
        var days = new TreeMap<Long, Day>();
        long firstDay = Math.max(0, currentDay - 29);
        if (data.contains(KEY, Tag.TAG_LIST)) {
            var list = data.getList(KEY, Tag.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) {
                var tag = list.getCompound(index);
                long day = tag.getLong("Day");
                if (day >= firstDay && day <= currentDay) days.put(day, new Day(day, Math.max(0, tag.getLong("Income")), Math.max(0, tag.getLong("Expense"))));
            }
        } else {
            for (var entry : legacy) {
                if (entry.day() < firstDay || entry.day() > currentDay || entry.amount() == 0) continue;
                var previous = days.getOrDefault(entry.day(), new Day(entry.day(), 0, 0));
                days.put(entry.day(), new Day(entry.day(), previous.income + Math.max(0L, entry.amount()), previous.expense + Math.max(0L, -(long) entry.amount())));
            }
        }
        return days;
    }

    private static void save(CompoundTag data, TreeMap<Long, Day> days) {
        var list = new ListTag();
        for (var day : days.values()) {
            var tag = new CompoundTag();
            tag.putLong("Day", day.day); tag.putLong("Income", day.income); tag.putLong("Expense", day.expense);
            list.add(tag);
        }
        data.put(KEY, list);
    }
}
