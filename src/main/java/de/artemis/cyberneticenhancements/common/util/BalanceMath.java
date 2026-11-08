package de.artemis.cyberneticenhancements.common.util;

public final class BalanceMath {
    public static final int MAX_BALANCE = 999_999_999;

    private BalanceMath() {
    }

    public static int clamp(long amount) {
        return (int) Math.max(0L, Math.min(MAX_BALANCE, amount));
    }

    public static int adjust(int balance, int delta) {
        return clamp((long) balance + delta);
    }

    public static boolean canReceive(int balance, int amount) {
        return amount > 0 && (long) balance + amount <= MAX_BALANCE;
    }
}
