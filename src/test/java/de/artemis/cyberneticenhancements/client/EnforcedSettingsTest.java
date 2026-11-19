package de.artemis.cyberneticenhancements.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.After;
import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class EnforcedSettingsTest {
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    private final ModConfigSpec.BooleanValue screen = builder.define("customDeathScreen", false);
    private final ModConfigSpec.IntValue width = builder.defineInRange("compassWidth", 192, 128, 320);
    private final ModConfigSpec.DoubleValue opacity = builder.defineInRange("compassOpacity", 1.0, 0.4, 1.0);
    private final ModConfigSpec.ConfigValue<List<? extends String>> order = builder.defineListAllowEmpty("archiveTabOrder", List.of("wiki"), value -> value instanceof String);
    private final ModConfigSpec spec = builder.build();

    @After public void disconnect() { EnforcedSettings.clearForTests(); }

    @Test public void entriesBecomeTypedValuesForTheirSettings() {
        var resolved = EnforcedSettings.resolve(spec, Map.of("customDeathScreen", "TRUE", "compassWidth", "224", "compassOpacity", "0.5"));
        assertEquals(Boolean.TRUE, resolved.values().get(screen));
        assertEquals(224, resolved.values().get(width));
        assertEquals(0.5, (Double) resolved.values().get(opacity), 1e-9);
        assertTrue(resolved.problems().isEmpty());
    }

    @Test public void everyKindOfMistakeIsNamedAndSkipped() {
        var resolved = EnforcedSettings.resolve(spec, Map.of("customDeathScreen", "yes", "compassWidth", "999", "compassOpacity", "abc",
                "archiveTabOrder", "a,b", "notASetting", "1", "sandevistan.flash_intensity", "0.9"));
        assertTrue(resolved.values().isEmpty() && resolved.extras().isEmpty());
        assertEquals(6, resolved.problems().size());
        assertTrue(resolved.problems().stream().anyMatch(problem -> problem.contains("archiveTabOrder") && problem.contains("list")));
    }

    @Test public void settingsKeptOutsideTheConfigFileAreEnforcedWithTheirRanges() {
        var resolved = EnforcedSettings.resolve(spec, Map.of("hud.enabled", "false", "sandevistan.afterimages", "true", "sandevistan.sound_volume", "0.25"));
        assertEquals(Boolean.FALSE, resolved.extras().get("hud.enabled"));
        assertEquals(0.25, (Double) resolved.extras().get("sandevistan.sound_volume"), 1e-9);
        assertTrue(resolved.problems().isEmpty());
    }

    @Test public void aSettingIsOnlyForcedWhileConnected() {
        assertNull(EnforcedSettings.forced(screen));
        EnforcedSettings.enforceForTests(spec, List.of("customDeathScreen=true"));
        assertEquals(Boolean.TRUE, EnforcedSettings.forced(screen));
        assertTrue(EnforcedSettings.locked(screen));
        assertFalse(EnforcedSettings.locked(width));
        assertEquals(1, EnforcedSettings.count());
        EnforcedSettings.clearForTests(); // Leaving the server: the synced file may linger, but nothing is enforced.
        assertNull(EnforcedSettings.forced(screen));
        assertFalse(EnforcedSettings.locked(screen));
        assertEquals(0, EnforcedSettings.count());
    }

    @Test public void extrasAreAnsweredByNameOnlyWhileEnforced() {
        assertNull(EnforcedSettings.extraBoolean("hud.enabled"));
        EnforcedSettings.enforceForTests(spec, List.of("hud.enabled=true", "sandevistan.flash_intensity=0.1"));
        assertEquals(Boolean.TRUE, EnforcedSettings.extraBoolean("hud.enabled"));
        assertEquals(0.1, EnforcedSettings.extraNumber("sandevistan.flash_intensity"), 1e-9);
        assertNull(EnforcedSettings.extraBoolean("sandevistan.afterimages"));
    }
}
