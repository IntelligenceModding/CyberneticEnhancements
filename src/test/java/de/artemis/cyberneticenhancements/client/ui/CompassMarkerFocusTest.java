package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.core.BlockPos;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class CompassMarkerFocusTest {
    private static CompassProjection.Marker marker(String id, double angle, CompassLandmark.Kind kind) {
        return new CompassProjection.Marker(new CompassLandmark(id, id, "minecraft:overworld", BlockPos.ZERO, kind, 0xFFFFFFFF), angle, 20, 0);
    }

    @Test public void hoveringUsesTheSamePositionAsDrawingAndOnlyLinksQuestMarkers() {
        var quest = marker("quest", 30, CompassLandmark.Kind.QUEST);
        int x = CompassMarkerFocus.x(quest, 160, 180);
        assertSame(quest, CompassMarkerFocus.hovered(List.of(quest), 160, 8, 180, x + 3, 21, true));
        assertNull(CompassMarkerFocus.hovered(List.of(quest), 160, 8, 180, x + 9, 21, true));
        var banner = marker("banner", 30, CompassLandmark.Kind.BANNER);
        assertNull(CompassMarkerFocus.hovered(List.of(banner), 160, 8, 180, x, 21, true));
        assertTrue(CompassMarkerFocus.isQuest(marker("turn-in", 0, CompassLandmark.Kind.TURN_IN).target()));
    }

    @Test public void hiddenEdgeArrowsCannotBeHoveredAndOverlappingMarkersRespectDrawOrder() {
        var behind = marker("behind", 170, CompassLandmark.Kind.QUEST);
        int edge = CompassMarkerFocus.x(behind, 160, 180);
        assertNull(CompassMarkerFocus.hovered(List.of(behind), 160, 8, 180, edge, 21, false));
        assertSame(behind, CompassMarkerFocus.hovered(List.of(behind), 160, 8, 180, edge, 21, true));
        var top = marker("top", 0, CompassLandmark.Kind.QUEST);
        var lower = marker("lower", 0, CompassLandmark.Kind.QUEST);
        assertSame(top, CompassMarkerFocus.hovered(List.of(top, lower), 160, 8, 180, 160, 21, true));
    }

    @Test public void gameplayFocusMatchesTheCaptionEvenWhenTheMarkerIsOnlySomewhatCentered() {
        var centered = marker("quest", 8, CompassLandmark.Kind.QUEST);
        assertSame(centered, CompassMarkerFocus.focused(List.of(centered), 12));
        assertTrue(CompassMarkerFocus.captionAlpha(centered, 12) > 0);
        var outside = marker("outside", 12, CompassLandmark.Kind.QUEST);
        assertNull(CompassMarkerFocus.focused(List.of(outside), 12));
        assertNull(CompassMarkerFocus.focused(List.of(), 12));
        var banner = marker("banner", 0, CompassLandmark.Kind.BANNER);
        var focus = CompassMarkerFocus.focused(List.of(banner, centered), 12);
        assertSame(banner, focus);
        assertFalse(CompassMarkerFocus.isQuest(focus.target()));
    }
}
