package de.artemis.cyberneticenhancements.common.reliccache;

import net.minecraft.util.RandomSource;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class RelicCacheHackPuzzleTest {
    @Test
    public void generatedPuzzlesHaveLegalFivePickSolutionsAcrossAllDifficulties() {
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
        int[] grid = puzzle.gridTokens();
        int[] targets = puzzle.targetTokens();
        int[] lengths = puzzle.targetLengths();
        int[] tokens = puzzle.targetCount() == 2
                ? new int[]{targets[0], targets[1], targets[2], targets[6], targets[7]}
                : new int[]{targets[0], targets[1], targets[2], targets[7], targets[12]};
        int[] path = new int[5];
        assertTrue("No solution for generated difficulty " + puzzle.difficulty(), findPath(grid, puzzle.gridSize(), tokens, path, 0));
        for (int target = 0; target < puzzle.targetCount(); target++) {
            int offset = target * RelicCacheHackPuzzle.MAX_SEQUENCE_LENGTH;
            assertEquals(3, lengths[target]);
            assertTrue(RelicCacheHackRules.containsSequence(5, i -> grid[path[i]], lengths[target], i -> targets[offset + i]));
        }
        assertEquals(1200, puzzle.timeLimitTicks());
        assertTrue(puzzle.gridSize() == 5 || puzzle.gridSize() == 6);
        for (int i = 0; i < puzzle.gridSize() * puzzle.gridSize(); i++) {
            assertTrue(grid[i] >= 0 && grid[i] < RelicCacheHackPuzzle.TOKEN_COUNT);
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
        assertEquals(3, puzzle.targetLengths()[0]);
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
