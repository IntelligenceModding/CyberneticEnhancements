package de.artemis.cyberneticenhancements.common.reliccache;

import java.util.function.IntUnaryOperator;

public final class RelicCacheHackRules {
    private RelicCacheHackRules() {
    }

    public static boolean isSelectable(int size, int count, IntUnaryOperator selectedCell, int candidate) {
        if (size < 1 || size > RelicCacheHackPuzzle.MAX_GRID_SIZE || count < 0 || count >= size * size
                || candidate < 0 || candidate >= size * size) return false;
        for (int i = 0; i < count; i++) {
            if (selectedCell.applyAsInt(i) == candidate) return false;
        }
        if (count == 0) return candidate / size == 0;
        int last = selectedCell.applyAsInt(count - 1);
        if (last < 0 || last >= size * size) return false;
        return (count & 1) == 1 ? candidate % size == last % size : candidate / size == last / size;
    }

    public static boolean containsSequence(int count, IntUnaryOperator selectedToken, int length, IntUnaryOperator targetToken) {
        if (length <= 0 || length > RelicCacheHackPuzzle.MAX_SEQUENCE_LENGTH) return false;
        for (int start = 0; start <= count - length; start++) {
            boolean match = true;
            for (int i = 0; i < length; i++) {
                int token = selectedToken.applyAsInt(start + i);
                if (token < 0 || token != targetToken.applyAsInt(i)) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }

    public static int matchingPrefix(int count, IntUnaryOperator selectedToken, int length, IntUnaryOperator targetToken) {
        for (int matched = Math.min(count, length); matched > 0; matched--) {
            boolean match = true;
            for (int i = 0; i < matched; i++) {
                int token = selectedToken.applyAsInt(count - matched + i);
                if (token < 0 || token != targetToken.applyAsInt(i)) {
                    match = false;
                    break;
                }
            }
            if (match) return matched;
        }
        return 0;
    }
}
