package de.artemis.cyberneticenhancements.common.reliccache;

import net.minecraft.util.RandomSource;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class RelicCacheHackPuzzleTest {
    @Test
    public void generatedPuzzlesHaveLegalSolutionsWithinTheirBuffers() {
        Set<Integer> difficulties = new HashSet<>();
        for (int seed = 0; seed < 5000; seed++) {
            RelicCacheHackPuzzle puzzle = RelicCacheHackPuzzle.generate(RandomSource.create(seed));
            difficulties.add(puzzle.difficulty());
            assertSolvable(puzzle);
        }
        assertEquals(Set.of(1, 2, 3, 4, 5), difficulties);
    }

    @Test
    public void emergencyFallbackIsAlsoSolvable() throws Exception {
        var fallback = RelicCacheHackPuzzle.class.getDeclaredMethod("fallbackPuzzle");
        fallback.setAccessible(true);
        assertSolvable((RelicCacheHackPuzzle) fallback.invoke(null));
    }

    @Test
    public void savedBrokenFallbackIsRepairedExactlyOnce() throws Exception {
        var method = RelicCacheHackPuzzle.class.getDeclaredMethod("fallbackPuzzle");
        method.setAccessible(true);
        RelicCacheHackPuzzle fallback = (RelicCacheHackPuzzle) method.invoke(null);
        int[] legacy = fallback.gridTokens();
        legacy[17] = 2;
        legacy[18] = 5;
        legacy[23] = 6;
        assertTrue(RelicCacheHackPuzzle.repairLegacyFallback(5, legacy, fallback.targetLengths(), fallback.targetTokens()));
        assertArrayEquals(fallback.gridTokens(), legacy);
        assertFalse(RelicCacheHackPuzzle.repairLegacyFallback(5, legacy, fallback.targetLengths(), fallback.targetTokens()));
    }

    private static void assertSolvable(RelicCacheHackPuzzle puzzle) {
        int[][] words = new int[puzzle.targetCount()][];
        for (int i = 0; i < words.length; i++) words[i] = java.util.Arrays.copyOfRange(puzzle.targetTokens(), i * 5, i * 5 + puzzle.targetLengths()[i]);
        assertTrue("Unsolvable difficulty " + puzzle.difficulty(), solveOrders(puzzle, words, new boolean[words.length], new int[0], 0));
        for (int i = 0; i < puzzle.gridSize() * puzzle.gridSize(); i++) assertTrue(puzzle.gridTokens()[i] >= 0 && puzzle.gridTokens()[i] < 8);
    }

    private static boolean solveOrders(RelicCacheHackPuzzle puzzle, int[][] words, boolean[] used, int[] merged, int depth) {
        if (depth == words.length) return merged.length <= RelicCacheHackPuzzle.bufferSize(puzzle.difficulty()) && findPath(puzzle.gridTokens(), puzzle.gridSize(), merged, new int[merged.length], 0);
        for (int i = 0; i < words.length; i++) {
            if (used[i]) continue;
            int[] word = words[i];
            int overlap = Math.min(merged.length, word.length);
            while (overlap > 0 && !java.util.Arrays.equals(merged, merged.length - overlap, merged.length, word, 0, overlap)) overlap--;
            int[] next = java.util.Arrays.copyOf(merged, merged.length + word.length - overlap);
            System.arraycopy(word, overlap, next, merged.length, word.length - overlap);
            used[i] = true;
            boolean solved = solveOrders(puzzle, words, used, next, depth + 1);
            used[i] = false;
            if (solved) return true;
        }
        return false;
    }

    @Test
    public void higherTiersHaveLongerTargetsAndLessTimeWithLimitedBuffers() {
        int previousTime = Integer.MAX_VALUE;
        for (int difficulty = 1; difficulty <= 5; difficulty++) {
            var puzzle = RelicCacheHackPuzzle.generate(RandomSource.create(357), difficulty);
            assertEquals(difficulty, puzzle.difficulty());
            assertTrue(puzzle.timeLimitTicks() < previousTime);
            previousTime = puzzle.timeLimitTicks();
            assertSolvable(puzzle);
            assertTrue(RelicCacheHackPuzzle.bufferSize(difficulty) < puzzle.gridSize() * puzzle.gridSize());
            if (difficulty >= 4) for (int i = 0; i < puzzle.targetCount(); i++) assertTrue(puzzle.targetLengths()[i] >= 4);
        }
    }
    private static boolean findPath(int[] grid, int size, int[] tokens, int[] path, int depth) {
        if (depth == tokens.length) return true;
        for (int cell = 0; cell < size * size; cell++) {
            if (grid[cell] != tokens[depth]) continue;
            if (depth == 0 && cell / size != 0) continue;
            if (depth > 0) {
                int last = path[depth - 1];
                if ((depth & 1) == 1 ? cell % size != last % size : cell / size != last / size) continue;
            }
            boolean used = false;
            for (int i = 0; i < depth; i++) if (path[i] == cell) used = true;
            if (used) continue;
            path[depth] = cell;
            assertTrue(RelicCacheHackRules.isSelectable(size, depth, i -> path[i], cell));
            if (findPath(grid, size, tokens, path, depth + 1)) return true;
        }
        return false;
    }

    @Test
    public void puzzleArraySnapshotsCannotMutateThePuzzle() {
        RelicCacheHackPuzzle puzzle = RelicCacheHackPuzzle.generate(RandomSource.create(42));
        int[] grid = puzzle.gridTokens();
        int original = grid[0];
        grid[0] = -100;
        assertEquals(original, puzzle.gridTokens()[0]);
        int[] lengths = puzzle.targetLengths();
        lengths[0] = -100;
        assertTrue(puzzle.targetLengths()[0] >= 3);
    }

    @Test
    public void movesStartAtTopAndAlternateColumnThenRowWithoutReuse() {
        int[] path = {1, 11};
        assertTrue(RelicCacheHackRules.isSelectable(5, 0, i -> path[i], 1));
        assertFalse(RelicCacheHackRules.isSelectable(5, 0, i -> path[i], 6));
        assertTrue(RelicCacheHackRules.isSelectable(5, 1, i -> path[i], 11));
        assertFalse(RelicCacheHackRules.isSelectable(5, 1, i -> path[i], 2));
        assertTrue(RelicCacheHackRules.isSelectable(5, 2, i -> path[i], 12));
        assertFalse(RelicCacheHackRules.isSelectable(5, 2, i -> path[i], 16));
        assertFalse(RelicCacheHackRules.isSelectable(5, 2, i -> path[i], 11));
        assertFalse(RelicCacheHackRules.isSelectable(5, 2, i -> path[i], -1));
        assertFalse(RelicCacheHackRules.isSelectable(5, 2, i -> path[i], 25));
    }

    @Test
    public void sequencesRequireConsecutiveTokensButCanOverlap() {
        int[] picked = {1, 2, 3, 4, 5};
        int[] first = {1, 2, 3};
        int[] overlap = {3, 4, 5};
        int[] gaps = {1, 3, 5};
        assertTrue(RelicCacheHackRules.containsSequence(5, i -> picked[i], 3, i -> first[i]));
        assertTrue(RelicCacheHackRules.containsSequence(5, i -> picked[i], 3, i -> overlap[i]));
        assertFalse(RelicCacheHackRules.containsSequence(5, i -> picked[i], 3, i -> gaps[i]));
        assertFalse(RelicCacheHackRules.containsSequence(2, i -> picked[i], 3, i -> first[i]));
    }

    @Test
    public void prefixFeedbackUsesCurrentSuffixRatherThanOldMatches() {
        int[] picked = {1, 2, 1, 2};
        int[] target = {1, 2, 3};
        assertEquals(2, RelicCacheHackRules.matchingPrefix(4, i -> picked[i], 3, i -> target[i]));
        assertEquals(1, RelicCacheHackRules.matchingPrefix(3, i -> picked[i], 3, i -> target[i]));
        assertEquals(0, RelicCacheHackRules.matchingPrefix(0, i -> picked[i], 3, i -> target[i]));
    }

    @Test
    public void exhaustedLineHasNoLegalMoveEvenBeforeWholeGridIsUsed() {
        int[] path = {0, 3, 4, 1, 2, 5};
        for (int cell = 0; cell < 9; cell++) {
            assertFalse(RelicCacheHackRules.isSelectable(3, path.length, i -> path[i], cell));
        }
    }
}
