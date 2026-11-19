package de.artemis.cyberneticenhancements.client.ui;

import java.util.random.RandomGenerator;

/** Shuffled cycles, with no immediate repeats or A-B-A repeats at cycle boundaries. */
public final class SlideshowOrder {
    private final RandomGenerator random;
    private final int[] bag;
    private int cursor;
    private int previous = -1, beforePrevious = -1;

    public SlideshowOrder(int size, RandomGenerator random) {
        if (size < 3) throw new IllegalArgumentException("At least three slides are required");
        this.random = random;
        bag = new int[size];
        cursor = size;
    }

    public int next() {
        if (cursor == bag.length) {
            for (int i = 0; i < bag.length; i++) bag[i] = i;
            for (int i = bag.length - 1; i > 0; i--) {
                int other = random.nextInt(i + 1);
                int value = bag[i]; bag[i] = bag[other]; bag[other] = value;
            }
            cursor = 0;
        }
        int candidate = cursor;
        while (bag[candidate] == previous || bag[candidate] == beforePrevious) candidate++;
        int value = bag[candidate];
        bag[candidate] = bag[cursor];
        bag[cursor++] = value;
        beforePrevious = previous;
        previous = value;
        return value;
    }
}
