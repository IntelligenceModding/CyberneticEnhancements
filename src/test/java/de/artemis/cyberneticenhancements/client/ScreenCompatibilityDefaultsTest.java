package de.artemis.cyberneticenhancements.client;

import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenCompatibilityDefaultsTest {
    @Test public void configCorrectionPreservesExplicitChoicesAndOnlyFillsMissingValues() {
        for (boolean compatibleDefault : new boolean[]{false, true}) {
            var builder = new net.neoforged.neoforge.common.ModConfigSpec.Builder();
            builder.define("customTitleScreen", compatibleDefault);
            builder.define("customTitleBackground", compatibleDefault);
            var spec = builder.build();
            for (boolean savedChoice : new boolean[]{false, true}) {
                var config = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
                config.set("customTitleScreen", savedChoice);
                spec.correct(config);
                assertEquals(savedChoice, config.get("customTitleScreen"));
                assertEquals(compatibleDefault, config.get("customTitleBackground"));
            }
        }
    }

    @Test public void ordinaryModsAndSharedLibrariesKeepCustomScreensEnabled() {
        var defaults = ScreenCompatibilityDefaults.forMods(Set.of("jei", "konkrete", "melody", "modmenu")::contains);
        assertTrue(defaults.deathScreen());
        assertTrue(defaults.loadingScreen());
        assertTrue(defaults.titlePresentation());
    }

    @Test public void fancyMenuKeepsDeathScreenEnabledButDefaultsOtherOverlappingScreensOff() {
        var defaults = ScreenCompatibilityDefaults.forMods(Set.of("fancymenu")::contains);
        assertTrue(defaults.deathScreen());
        assertFalse(defaults.loadingScreen());
        assertFalse(defaults.titlePresentation());
    }

    @Test public void loadingReplacementAloneOnlyDisablesLoadingVisuals() {
        var defaults = ScreenCompatibilityDefaults.forMods(Set.of("drippyloadingscreen")::contains);
        assertTrue(defaults.deathScreen());
        assertFalse(defaults.loadingScreen());
        assertTrue(defaults.titlePresentation());
    }

    @Test public void titleReplacementsOnlyDisableTitlePresentation() {
        for (var mod : Set.of("packmenu", "custommainmenu")) {
            var defaults = ScreenCompatibilityDefaults.forMods(Set.of(mod)::contains);
            assertTrue(defaults.deathScreen());
            assertTrue(defaults.loadingScreen());
            assertFalse(defaults.titlePresentation());
        }
    }

    @Test public void combinedReplacementsDisableBothAreas() {
        var defaults = ScreenCompatibilityDefaults.forMods(Set.of("packmenu", "drippyloadingscreen")::contains);
        assertTrue(defaults.deathScreen());
        assertFalse(defaults.loadingScreen());
        assertFalse(defaults.titlePresentation());
    }
}
