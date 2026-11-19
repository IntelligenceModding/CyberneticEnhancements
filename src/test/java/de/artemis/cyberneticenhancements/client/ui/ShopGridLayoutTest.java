package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import java.util.HashSet;
import static org.junit.Assert.*;

public class ShopGridLayoutTest {
    private static ShopGridLayout grid(int count) {
        return new ShopGridLayout(count, 592, 327, 136, 156, 10, 6);
    }

    @Test
    public void scrollbarAppearsAtTheFifthItemAndDisappearsWhenEverythingFits() {
        assertEquals(0, grid(4).maxScroll());
        assertEquals(592, grid(4).contentWidth());
        assertEquals(1, grid(5).maxScroll());
        assertEquals(576, grid(5).contentWidth());
        assertEquals(8, grid(20).maxScroll());
    }

    @Test
    public void scrollingReachesEveryItemWithoutExposingRowsOutsideTheViewport() {
        for (int count : new int[]{0, 1, 4, 5, 6, 19, 20, 101}) {
            var layout = grid(count);
            var reached = new HashSet<Integer>();
            for (int scroll = 0; scroll <= layout.maxScroll(); scroll++) {
                for (int slot = layout.firstSlot(scroll); slot < layout.lastSlot(scroll); slot++) {
                    assertTrue(slot >= 0 && slot < count);
                    assertTrue(layout.tileY(slot, scroll) >= 0);
                    assertTrue(layout.tileY(slot, scroll) + layout.tileHeight() <= layout.height());
                    assertTrue(layout.tileX(slot) >= 0 && layout.tileX(slot) + layout.tileWidth() <= layout.contentWidth());
                    reached.add(slot);
                }
            }
            assertEquals(count, reached.size());
            assertEquals(0, layout.clampScroll(-100));
            assertEquals(layout.maxScroll(), layout.clampScroll(Integer.MAX_VALUE));
        }
    }

    @Test
    public void oddRowsStayCenteredAndNarrowGridsUseOneColumn() {
        var odd = grid(5);
        int left = odd.tileX(4);
        int right = odd.contentWidth() - left - odd.tileWidth();
        assertTrue(Math.abs(left - right) <= 1);
        var narrow = new ShopGridLayout(9, 180, 327, 136, 156, 10, 6);
        assertEquals(1, narrow.columns());
        assertEquals(9, narrow.rows());
        assertEquals(7, narrow.maxScroll());
    }
}
