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
        for (int attempt = 0; attempt < 32; attempt++) {
            RelicCacheHackPuzzle puzzle = tryGenerate(random);
            if (puzzle != null) {
                return puzzle;
            }
        }
        return fallbackPuzzle();
    }

    private static RelicCacheHackPuzzle tryGenerate(RandomSource random) {
        int difficulty = 1 + random.nextInt(5);
        int gridSize = difficulty >= 4 ? 6 : 5;
        int targetCount = difficulty <= 2 ? 2 : 3;
        int timeLimitTicks = 60 * 20;

        int pathLength = 5;
        int[] path = generatePath(random, gridSize, pathLength);
        if (path == null) {
            return null;
        }

        int[] tokenOrder = new int[TOKEN_COUNT];
        for (int index = 0; index < TOKEN_COUNT; index++) {
            tokenOrder[index] = index;
        }
        shuffleInts(tokenOrder, random);
        int[] pathTokens = Arrays.copyOf(tokenOrder, pathLength);
        int[] fillerTokens = Arrays.copyOfRange(tokenOrder, pathLength, TOKEN_COUNT);

        int[] gridTokens = new int[MAX_GRID_CELLS];
        Arrays.fill(gridTokens, -1);
        int cellCount = gridSize * gridSize;
        for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
            gridTokens[cellIndex] = fillerTokens[random.nextInt(fillerTokens.length)];
        }
        for (int step = 0; step < path.length; step++) {
            gridTokens[path[step]] = pathTokens[step];
        }
        sanitizeCanonicalRoute(gridSize, path, pathTokens, gridTokens, random);

        int[] targetLengths = new int[MAX_TARGET_COUNT];
        int[] targetTokens = new int[MAX_TARGET_COUNT * MAX_SEQUENCE_LENGTH];
        Arrays.fill(targetLengths, 0);
        Arrays.fill(targetTokens, -1);

        for (int targetIndex = 0; targetIndex < targetCount; targetIndex++) {
            int start = targetCount == 2
                    ? (targetIndex == 0 ? 0 : 2)
                    : targetIndex;
            int length = 3;
            targetLengths[targetIndex] = length;
            for (int tokenIndex = 0; tokenIndex < length; tokenIndex++) {
                targetTokens[targetIndex * MAX_SEQUENCE_LENGTH + tokenIndex] = pathTokens[start + tokenIndex];
            }
        }

        return new RelicCacheHackPuzzle(
                difficulty,
                gridSize,
                timeLimitTicks,
                targetCount,
                gridTokens,
                targetLengths,
                targetTokens
        );
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

    private static void sanitizeCanonicalRoute(int gridSize, int[] path, int[] pathTokens, int[] gridTokens, RandomSource random) {
        sanitizeTopRowEntry(gridSize, path, pathTokens, gridTokens, random);
        for (int step = 0; step < path.length - 1; step++) {
            int currentCell = path[step];
            int nextCell = path[step + 1];
            int nextToken = pathTokens[step + 1];
            sanitizeNextMoveLine(gridSize, path, pathTokens, gridTokens, currentCell, nextCell, nextToken, random, (step & 1) == 0);
        }
    }

    private static void sanitizeTopRowEntry(int gridSize, int[] path, int[] pathTokens, int[] gridTokens, RandomSource random) {
        int entryToken = pathTokens[0];
        for (int column = 0; column < gridSize; column++) {
            int cellIndex = column;
            if (cellIndex == path[0]) {
                continue;
            }
            if (gridTokens[cellIndex] == entryToken) {
                gridTokens[cellIndex] = replacementToken(entryToken, pathTokens, random);
            }
        }
    }

    private static void sanitizeNextMoveLine(
            int gridSize,
            int[] path,
            int[] pathTokens,
            int[] gridTokens,
            int currentCell,
            int nextCell,
            int nextToken,
            RandomSource random,
            boolean nextMoveUsesColumn
    ) {
        int row = currentCell / gridSize;
        int column = currentCell % gridSize;
        if (nextMoveUsesColumn) {
            for (int candidateRow = 0; candidateRow < gridSize; candidateRow++) {
                int cellIndex = candidateRow * gridSize + column;
                sanitizeCandidate(path, pathTokens, gridTokens, nextCell, nextToken, random, cellIndex);
            }
            return;
        }

        for (int candidateColumn = 0; candidateColumn < gridSize; candidateColumn++) {
            int cellIndex = row * gridSize + candidateColumn;
            sanitizeCandidate(path, pathTokens, gridTokens, nextCell, nextToken, random, cellIndex);
        }
    }

    private static void sanitizeCandidate(int[] path, int[] pathTokens, int[] gridTokens, int nextCell, int nextToken, RandomSource random, int cellIndex) {
        if (cellIndex == nextCell || contains(path, cellIndex)) {
            return;
        }
        if (gridTokens[cellIndex] == nextToken) {
            gridTokens[cellIndex] = replacementToken(nextToken, pathTokens, random);
        }
    }

    private static int replacementToken(int blockedToken, int[] preferredAvoid, RandomSource random) {
        int token = random.nextInt(TOKEN_COUNT - 1);
        if (token >= blockedToken) {
            token++;
        }
        if (preferredAvoid == null) {
            return token;
        }
        for (int attempt = 0; attempt < TOKEN_COUNT; attempt++) {
            boolean conflicts = false;
            for (int avoid : preferredAvoid) {
                if (avoid == token) {
                    conflicts = true;
                    break;
                }
            }
            if (!conflicts) {
                return token;
            }
            token = (token + 1) % TOKEN_COUNT;
            if (token == blockedToken) {
                token = (token + 1) % TOKEN_COUNT;
            }
        }
        return token;
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
