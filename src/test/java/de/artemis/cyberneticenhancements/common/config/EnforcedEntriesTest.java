package de.artemis.cyberneticenhancements.common.config;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class EnforcedEntriesTest {
    @Test public void keyValueLinesBecomeEntriesInOrderWithWhitespaceTrimmed() {
        var parsed = EnforcedEntries.parse(List.of("customDeathScreen=true", "  compassWidth = 224 ", "sandevistan.sound_volume=0.25"));
        assertEquals(List.of("customDeathScreen", "compassWidth", "sandevistan.sound_volume"), List.copyOf(parsed.entries().keySet()));
        assertEquals("224", parsed.entries().get("compassWidth"));
        assertTrue(parsed.problems().isEmpty());
    }

    @Test public void aBadLineIsReportedAndSkippedWithoutLosingTheRest() {
        var parsed = EnforcedEntries.parse(List.of("showCompass=false", "no equals sign", "=true", "empty=", "", "  ", "customDeathScreen=true"));
        assertEquals(List.of("showCompass", "customDeathScreen"), List.copyOf(parsed.entries().keySet()));
        assertEquals(3, parsed.problems().size());
    }

    @Test public void theLastDuplicateWinsAndIsReported() {
        var parsed = EnforcedEntries.parse(List.of("compassWidth=200", "compassWidth=250"));
        assertEquals("250", parsed.entries().get("compassWidth"));
        assertEquals(1, parsed.problems().size());
    }

    @Test public void valuesMayContainEqualsSigns() {
        assertEquals("a=b", EnforcedEntries.parse(List.of("key=a=b")).entries().get("key"));
    }
}
