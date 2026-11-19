package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ArchiveButtonPlacementTest {
    @Test public void leavesJeiBottomControlsClearAtDifferentStationScales() {
        for (double scale : new double[]{1, .75, .5}) {
            int width = (int) Math.floor(640 / scale), height = (int) Math.floor(360 / scale);
            int stripHeight = (int) Math.ceil(28 / scale);
            var controls = new ArchiveButtonPlacement.Bounds(0, height - stripHeight, width, stripHeight);
            var machine = new ArchiveButtonPlacement.Bounds(width / 2 - 200, 40, 400, 180);
            var button = ArchiveButtonPlacement.find(width, height, 78, 18, List.of(controls, machine)).orElseThrow();
            assertFalse(button.overlaps(controls, 4));
            assertFalse(button.overlaps(machine, 4));
            assertTrue(button.y() + button.height() <= controls.y() - 4);
        }
    }

    @Test public void prefersBottomLeftOutsideCenteredWindows() {
        for (int width : new int[]{320, 640, 960}) {
            var gui = new ArchiveButtonPlacement.Bounds(width / 2 - 88, 40, 176, 166);
            var button = ArchiveButtonPlacement.find(width, 240, 78, 18, List.of(gui)).orElseThrow();
            assertEquals(8, button.x());
            assertEquals(214, button.y());
            assertFalse(button.overlaps(gui, 4));
        }
    }

    @Test public void avoidsOtherButtonsAndHidesWhenNoEdgeSpaceIsFree() {
        var other = new ArchiveButtonPlacement.Bounds(8, 214, 80, 18);
        var gui = new ArchiveButtonPlacement.Bounds(200, 20, 220, 220);
        var button = ArchiveButtonPlacement.find(640, 240, 78, 18, List.of(other, gui)).orElseThrow();
        assertFalse(button.overlaps(other, 4));
        assertFalse(button.overlaps(gui, 4));
        assertTrue(ArchiveButtonPlacement.find(320, 240, 78, 18,
                List.of(new ArchiveButtonPlacement.Bounds(0, 0, 320, 240))).isEmpty());
        assertTrue(ArchiveButtonPlacement.find(70, 20, 78, 18, List.of()).isEmpty());
    }
}
