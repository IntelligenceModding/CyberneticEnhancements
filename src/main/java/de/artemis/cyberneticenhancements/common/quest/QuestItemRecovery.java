package de.artemis.cyberneticenhancements.common.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class QuestItemRecovery {
    private static final int GRACE_TICKS = 200;
    private record Key(String contract, String type) {}
    private final Map<Key, Integer> missingSince = new HashMap<>();
    private Key cursor;
    private int cursorAt;

    void cursor(String contract, String type, int tick) {
        cursor = contract.isEmpty() || type.isEmpty() ? null : new Key(contract, type);
        cursorAt = tick;
    }

    boolean missingLongEnough(String contract, String type, boolean present, boolean creative, int tick) {
        var key = new Key(contract, type);
        boolean onCursor = creative && key.equals(cursor) && tick - cursorAt >= 0 && tick - cursorAt < 40;
        if (present || onCursor) {
            missingSince.remove(key);
            return false;
        }
        int first = missingSince.computeIfAbsent(key, ignored -> tick);
        if (tick < first || tick - first < GRACE_TICKS) return false;
        missingSince.remove(key);
        return true;
    }

    void retainContracts(Set<String> contracts) {
        missingSince.keySet().removeIf(key -> !contracts.contains(key.contract));
        if (cursor != null && !contracts.contains(cursor.contract)) cursor = null;
    }
}
