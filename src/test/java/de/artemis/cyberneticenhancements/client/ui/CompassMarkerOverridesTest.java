package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CompassMarkerOverridesTest {
    @Test public void deletedLiveEntriesStayRemovedWhenHiddenEntriesAreRestored() {
        var preferences = new CompassMarkerOverrides();
        preferences.set("a", "quest", new CompassMarkerOverrides.Entry(false, "", null, true));
        preferences.set("a", "banner", new CompassMarkerOverrides.Entry(true, "Home", null));
        var loaded = CompassMarkerOverrides.parse(preferences.serialize());
        loaded.clear("a");
        assertTrue(loaded.get("a", "quest").deleted());
        assertFalse(loaded.get("a", "banner").hidden());
        assertFalse(loaded.get("b", "quest").deleted());
        assertTrue(CompassMarkerOverrides.parse(loaded.serialize()).get("a", "quest").deleted());
    }

    @Test
    public void markerPreferencesSurviveSavingAndRemainIsolatedBetweenWorlds() {
        var preferences = new CompassMarkerOverrides();
        String banner = "minecraft:overworld/12345";
        preferences.set("server-a", banner, new CompassMarkerOverrides.Entry(true, "Home \"Alpha\"", 0xFF64D99C));
        var loaded = CompassMarkerOverrides.parse(preferences.serialize());
        assertTrue(loaded.get("server-a", banner).hidden());
        assertEquals("Home \"Alpha\"", loaded.get("server-a", banner).label());
        assertEquals(Integer.valueOf(0xFF64D99C), loaded.get("server-a", banner).color());
        assertFalse(loaded.get("server-b", banner).hidden());
        assertEquals("", loaded.get("server-b", banner).label());
        assertNull(loaded.get("server-b", banner).color());
    }

    @Test
    public void restoringMarkersOnlyResetsTheCurrentWorldAndUnusedOverridesAreRemoved() {
        var preferences = new CompassMarkerOverrides();
        preferences.set("a", "quest", new CompassMarkerOverrides.Entry(true, "", null));
        preferences.set("b", "quest", new CompassMarkerOverrides.Entry(false, "Supplies", null));
        preferences.clear("a");
        assertFalse(preferences.get("a", "quest").hidden());
        assertEquals("Supplies", preferences.get("b", "quest").label());
        preferences.set("b", "quest", new CompassMarkerOverrides.Entry(false, "", null));
        assertTrue(preferences.serialize().isEmpty());
    }

    @Test
    public void malformedSettingsAreIgnoredAndLabelsAndColorsAreNormalized() {
        var preferences = CompassMarkerOverrides.parse(List.of("broken", "{}", "null", "[]",
                "{\"scope\":\"a\",\"id\":\"banner\",\"hidden\":true,\"label\":\"  Base\\n \",\"color\":255}"));
        assertTrue(preferences.get("a", "banner").hidden());
        assertEquals("Base", preferences.get("a", "banner").label());
        assertEquals(Integer.valueOf(0xFF0000FF), preferences.get("a", "banner").color());
        preferences.set("a", "long", new CompassMarkerOverrides.Entry(false, "x".repeat(100), null));
        assertEquals(80, preferences.get("a", "long").label().length());
        assertEquals(2, preferences.serialize().size());
    }
}
