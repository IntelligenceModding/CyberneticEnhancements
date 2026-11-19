package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.core.BlockPos;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CompassProjectionTest {
    @Test
    public void destinationsUseMinecraftDirectionsAndDoNotLeakAcrossDimensions() {
        var north = target("north", new BlockPos(0, 64, -100), "minecraft:overworld");
        var east = target("east", new BlockPos(100, 64, 0), "minecraft:overworld");
        var other = target("nether", new BlockPos(0, 64, -100), "minecraft:the_nether");
        var markers = CompassProjection.project(List.of(east, other, north), "minecraft:overworld", .5, 64.5, .5, 180);
        assertEquals(2, markers.size());
        assertEquals(north, markers.getFirst().target());
        assertEquals(0, markers.getFirst().angle(), .0001);
        assertEquals(90, markers.get(1).angle(), .0001);
        assertEquals(100, markers.getFirst().distance());
        var netherMarkers = CompassProjection.project(List.of(north, east, other), "minecraft:the_nether", .5, 64.5, .5, 180);
        assertEquals(1, netherMarkers.size());
        assertEquals(other, netherMarkers.getFirst().target());
        assertEquals(0, netherMarkers.getFirst().angle(), .0001);
        var rotated = CompassProjection.project(List.of(north, east), "minecraft:overworld", .5, 64.5, .5, -90);
        assertEquals(east, rotated.getFirst().target());
        assertEquals(0, rotated.getFirst().angle(), .0001);
    }

    @Test
    public void undergroundAndOverheadTargetsStayStableWhenStandingOverThem() {
        var underground = target("under", new BlockPos(0, 24, 0), "minecraft:overworld");
        var overhead = target("above", new BlockPos(0, 84, 0), "minecraft:overworld");
        var markers = CompassProjection.project(List.of(underground, overhead), "minecraft:overworld", .5, 64.5, .5, 720);
        assertEquals(overhead, markers.getFirst().target());
        assertEquals(20, markers.getFirst().distance());
        assertEquals(20, markers.getFirst().verticalDistance());
        assertEquals(-40, markers.get(1).verticalDistance());
        assertEquals(0, markers.get(1).angle(), .0001);
        assertTrue(CompassProjection.project(List.of(underground), "minecraft:the_nether", 0, 0, 0, 0).isEmpty());
    }

    private static CompassLandmark target(String id, BlockPos pos, String dimension) {
        return new CompassLandmark(id, id, dimension, pos, CompassLandmark.Kind.QUEST, 0xFFF0B36A);
    }

    @Test
    public void bannersAndQuestsShareProjectionWithoutLosingNameOrColor() {
        var banner = new CompassLandmark("banner", "Home", "minecraft:overworld", new BlockPos(100, 64, 0), CompassLandmark.Kind.BANNER, 0xFF3C44AA);
        var quest = target("quest", new BlockPos(0, 64, -100), "minecraft:overworld");
        var markers = CompassProjection.project(List.of(quest, banner), "minecraft:overworld", .5, 64.5, .5, -90);
        assertEquals(banner, markers.getFirst().target());
        assertEquals("Home", markers.getFirst().target().title());
        assertEquals(0xFF3C44AA, markers.getFirst().target().color());
        assertEquals(CompassLandmark.Kind.BANNER, markers.getFirst().target().kind());
        assertEquals(100, markers.getFirst().distance());
    }
}
