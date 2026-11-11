package de.artemis.cyberneticenhancements.common.reliccache;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RelicCacheHackPuzzle {
    public static final int MAX_GRID_SIZE = 6;
    public static final int MAX_GRID_CELLS = MAX_GRID_SIZE * MAX_GRID_SIZE;
    public static final int MAX_TARGET_COUNT = 3;
    public static final int MAX_SEQUENCE_LENGTH = 5;
    public static final int MAX_SELECTED_CELLS = MAX_GRID_CELLS;
    public static final int TOKEN_COUNT = 8;
    public static final String[] TOKEN_LABELS = {"1C", "55", "7A", "BD", "E9", "FF", "0A", "4C"};

    private final int difficulty;
    private final int gridSize;
    private final int timeLimitTicks;
    private final int targetCount;
    private final int[] gridTokens;
    private final int[] targetLengths;
    private final int[] targetTokens;

    private RelicCacheHackPuzzle(
            int difficulty,
            int gridSize,
            int timeLimitTicks,
            int targetCount,
            int[] gridTokens,
            int[] targetLengths,
            int[] targetTokens
    ) {
        this.difficulty = difficulty;
        this.gridSize = gridSize;
        this.timeLimitTicks = timeLimitTicks;
        this.targetCount = targetCount;
        this.gridTokens = gridTokens;
        this.targetLengths = targetLengths;
        this.targetTokens = targetTokens;
    }

    public static RelicCacheHackPuzzle generate(RandomSource random) {
        return generate(random, 1 + random.nextInt(5));
    }

    public static int bufferSize(int difficulty) {
        return difficulty <= 2 ? 7 : difficulty <= 4 ? 8 : 9;
    }

    public static RelicCacheHackPuzzle generate(RandomSource random, int difficulty) {
        if (difficulty < 1 || difficulty > 5) throw new IllegalArgumentException("Difficulty must be 1..5");
        int size = difficulty >= 4 ? 6 : 5;
        int count = difficulty <= 2 ? 2 : 3;
        int length = difficulty + 4;
        int[] path = generatePath(random, size, length);
        if (path == null) path = Arrays.copyOf(new int[]{0, size, size+1, 2*size+1, 2*size+2, 3*size+2, 3*size+3, 4*size+3, 4*size+4}, length);
        int[] palette = new int[TOKEN_COUNT];
        for (int i = 0; i < TOKEN_COUNT; i++) palette[i] = i;
        shuffleInts(palette, random);
        int[] route = new int[length];
        for (int i = 0; i < length; i++) route[i] = palette[i % TOKEN_COUNT];
        int[] grid = new int[MAX_GRID_CELLS];
        Arrays.fill(grid, -1);

        for (int i = 0; i < size * size; i++) grid[i] = palette[random.nextInt(Math.min(length, TOKEN_COUNT))];
        for (int i = 0; i < length; i++) grid[path[i]] = route[i];

        for (int step = 0; step < length - 1; step++) {
            List<Integer> candidates = new ArrayList<>();
            for (int cell = 0; cell < size * size; cell++) {
                if (!contains(path, cell) && ((step & 1) == 0 ? cell % size == path[step] % size : cell / size == path[step] / size)) candidates.add(cell);
            }
            if (!candidates.isEmpty()) grid[candidates.get(random.nextInt(candidates.size()))] = route[step + 1];
        }
        int[] starts = difficulty == 1 ? new int[]{0,2} : difficulty == 2 ? new int[]{0,3} : difficulty == 3 ? new int[]{0,1,3} : new int[]{0,2,4};
        int[] lengths = difficulty <= 2 ? new int[]{3,3} : difficulty == 3 ? new int[]{3,4,4} : difficulty == 4 ? new int[]{4,4,4} : new int[]{4,5,5};
        int[] order = count == 2 ? new int[]{0,1} : new int[]{0,1,2};
        shuffleInts(order, random);
        int[] targetLengths = new int[MAX_TARGET_COUNT];
        int[] targets = new int[MAX_TARGET_COUNT * MAX_SEQUENCE_LENGTH];
        Arrays.fill(targets, -1);
        for (int i = 0; i < count; i++) {
            int word = order[i];
            targetLengths[i] = lengths[word];
            System.arraycopy(route, starts[word], targets, i * MAX_SEQUENCE_LENGTH, lengths[word]);
        }
        int seconds = switch (difficulty) { case 1 -> 60; case 2 -> 50; case 3 -> 40; case 4 -> 32; default -> 25; };
        return new RelicCacheHackPuzzle(difficulty, size, seconds * 20, count, grid, targetLengths, targets);
    }
    private static int[] generatePath(RandomSource random, int gridSize, int length) {
        boolean[] used = new boolean[gridSize * gridSize];
        int[] path = new int[length];
        for (int attempt = 0; attempt < 64; attempt++) {
            Arrays.fill(used, false);
            path[0] = random.nextInt(gridSize);
            used[path[0]] = true;
            if (fillPath(path, used, gridSize, 1, false, random)) {
                return path.clone();
            }
        }
        return null;
    }

    private static boolean fillPath(int[] path, boolean[] used, int gridSize, int depth, boolean selectFromRow, RandomSource random) {
        if (depth >= path.length) {
            return true;
        }

        int previous = path[depth - 1];
        int previousRow = previous / gridSize;
        int previousColumn = previous % gridSize;
        List<Integer> candidates = new ArrayList<>();
        if (selectFromRow) {
            for (int column = 0; column < gridSize; column++) {
                int index = previousRow * gridSize + column;
                if (!used[index]) {
                    candidates.add(index);
                }
            }
        } else {
            for (int row = 0; row < gridSize; row++) {
                int index = row * gridSize + previousColumn;
                if (!used[index]) {
                    candidates.add(index);
                }
            }
        }

        shuffle(candidates, random);
        candidates.sort((left, right) -> Integer.compare(
                futureDegree(right, used, gridSize, !selectFromRow),
                futureDegree(left, used, gridSize, !selectFromRow)
        ));

        for (int candidate : candidates) {
            used[candidate] = true;
            path[depth] = candidate;
            if (fillPath(path, used, gridSize, depth + 1, !selectFromRow, random)) {
                return true;
            }
            used[candidate] = false;
        }
        return false;
    }

    private static int futureDegree(int cell, boolean[] used, int gridSize, boolean selectFromRow) {
        int row = cell / gridSize;
        int column = cell % gridSize;
        int count = 0;
        if (selectFromRow) {
            for (int nextColumn = 0; nextColumn < gridSize; nextColumn++) {
                int index = row * gridSize + nextColumn;
                if (!used[index]) {
                    count++;
                }
            }
        } else {
            for (int nextRow = 0; nextRow < gridSize; nextRow++) {
                int index = nextRow * gridSize + column;
                if (!used[index]) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void shuffle(List<Integer> values, RandomSource random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            Integer value = values.get(index);
            values.set(index, values.get(swapIndex));
            values.set(swapIndex, value);
        }
    }

    private static boolean contains(int[] values, int target) {
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    private static void shuffleInts(int[] values, RandomSource random) {
        for (int index = values.length - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            int value = values[index];
            values[index] = values[swapIndex];
            values[swapIndex] = value;
        }
    }

    private static RelicCacheHackPuzzle fallbackPuzzle() {
        int[] gridTokens = new int[MAX_GRID_CELLS];
        Arrays.fill(gridTokens, -1);
        int[] targetLengths = new int[MAX_TARGET_COUNT];
        Arrays.fill(targetLengths, 0);
        int[] targetTokens = new int[MAX_TARGET_COUNT * MAX_SEQUENCE_LENGTH];
        Arrays.fill(targetTokens, -1);

        int[] fallbackGrid = {
                0, 1, 0, 2, 0,
                2, 2, 0, 2, 0,
                2, 3, 4, 2, 0,
                0, 0, 5, 6, 2,
                2, 0, 2, 2, 0
        };
        System.arraycopy(fallbackGrid, 0, gridTokens, 0, fallbackGrid.length);
        targetLengths[0] = 3;
        targetLengths[1] = 3;
        targetTokens[0] = 1;
        targetTokens[1] = 3;
        targetTokens[2] = 4;
        targetTokens[5] = 4;
        targetTokens[6] = 5;
        targetTokens[7] = 6;

        return new RelicCacheHackPuzzle(2, 5, 60 * 20, 2, gridTokens, targetLengths, targetTokens);
    }

    public static boolean repairLegacyFallback(int size, int[] grid, int[] lengths, int[] targets) {
        int[] legacy = {0, 1, 0, 2, 0, 2, 2, 0, 2, 0, 2, 3, 4, 2, 0, 0, 0, 2, 5, 2, 2, 0, 2, 6, 0};
        if (size != 5 || grid.length < legacy.length || lengths.length < 2 || targets.length < 8
                || lengths[0] != 3 || lengths[1] != 3
                || targets[0] != 1 || targets[1] != 3 || targets[2] != 4
                || targets[5] != 4 || targets[6] != 5 || targets[7] != 6) return false;
        for (int i = 0; i < legacy.length; i++) if (grid[i] != legacy[i]) return false;
        grid[17] = 5;
        grid[18] = 6;
        grid[23] = 2;
        return true;
    }

    public int difficulty() {
        return difficulty;
    }

    public int gridSize() {
        return gridSize;
    }

    public int timeLimitTicks() {
        return timeLimitTicks;
    }

    public int targetCount() {
        return targetCount;
    }

    public int[] gridTokens() {
        return gridTokens.clone();
    }

    public int[] targetLengths() {
        return targetLengths.clone();
    }

    public int[] targetTokens() {
        return targetTokens.clone();
    }
}
