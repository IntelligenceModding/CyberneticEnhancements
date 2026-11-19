package de.artemis.cyberneticenhancements.client.ui;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ClientSettingsModelTest {
    @Test public void resetRestoresAllCategoriesAndInvalidDraftsWithoutSavingUntilApply() throws Exception {
        var builder = new ModConfigSpec.Builder();
        var screen = builder.define("customTitleScreen", false);
        var width = builder.defineInRange("compassWidth", 192, 128, 320);
        var order = builder.defineListAllowEmpty("archiveTabOrder", List.of("wiki", "quests"), value -> value instanceof String);
        var spec = builder.build(); load(spec);
        screen.set(true); width.set(256); order.set(List.of("quests", "wiki"));
        var saves = new java.util.concurrent.atomic.AtomicInteger();
        var model = new ClientSettingsModel(spec, saves::incrementAndGet);
        model.entries().stream().filter(entry -> entry.key.equals("compassWidth")).findFirst().orElseThrow().number("invalid");
        model.resetAll();
        assertTrue(model.valid()); assertEquals(3, model.changes());
        for (var entry : model.entries()) assertEquals(entry.value.getDefault(), entry.draft());
        assertTrue(screen.get()); assertEquals(256, width.get().intValue()); assertEquals(0, saves.get());
        model.save();
        assertFalse(screen.get()); assertEquals(192, width.get().intValue());
        assertEquals(List.of("wiki", "quests"), order.get()); assertEquals(1, saves.get());
    }

    @Test public void applyWritesOnlyEditedValuesAndRollsBackWhenSavingFails() throws Exception {
        var builder = new ModConfigSpec.Builder();
        var first = builder.define("first", false);
        var second = builder.define("second", false);
        var spec = builder.build(); load(spec);
        var saves = new java.util.concurrent.atomic.AtomicInteger();
        var model = new ClientSettingsModel(spec, saves::incrementAndGet);
        model.entries().stream().filter(entry -> entry.key.equals("first")).findFirst().orElseThrow().set(true);
        assertFalse(first.get());
        second.set(true); // Another part of the mod updates an untouched setting while this screen is open.
        model.save();
        assertTrue(first.get()); assertTrue(second.get()); assertEquals(1, saves.get()); assertFalse(model.dirty());
        model.save(); assertEquals(1, saves.get());
        var failing = new ClientSettingsModel(spec, () -> { throw new IllegalStateException("disk failure"); });
        failing.entries().stream().filter(entry -> entry.key.equals("first")).findFirst().orElseThrow().set(false);
        try { failing.save(); fail("Save failure was ignored"); } catch (IllegalStateException expected) { }
        assertTrue(first.get()); assertTrue(second.get()); assertTrue(failing.dirty());
    }

    @Test public void aServerEnforcedSettingCannotBeEditedResetOrSavedAndLeavesTheFileAlone() throws Exception {
        var builder = new ModConfigSpec.Builder();
        var forced = builder.define("customDeathScreen", false);
        var free = builder.define("showCompass", true);
        var spec = builder.build(); load(spec);
        var saves = new java.util.concurrent.atomic.AtomicInteger();
        try {
            de.artemis.cyberneticenhancements.client.EnforcedSettings.enforceForTests(spec, List.of("customDeathScreen=true"));
            var model = new ClientSettingsModel(spec, saves::incrementAndGet);
            var locked = model.entries().stream().filter(entry -> entry.key.equals("customDeathScreen")).findFirst().orElseThrow();
            var other = model.entries().stream().filter(entry -> entry.key.equals("showCompass")).findFirst().orElseThrow();
            assertTrue(locked.locked()); assertFalse(other.locked()); assertEquals(1, model.locked());
            locked.set(true); locked.number("5"); locked.reset();
            assertFalse(locked.dirty()); assertTrue(model.valid()); assertFalse(model.dirty());
            other.set(false);
            assertEquals(1, model.changes());
            model.resetAll(); // Resets only what the player may change.
            assertTrue(free.get());
            model.save();
            assertFalse("The enforced value must never be written to the player's own file", forced.get());
        } finally { de.artemis.cyberneticenhancements.client.EnforcedSettings.clearForTests(); }
    }

    private static void load(ModConfigSpec spec) throws Exception {
        var data = CommentedConfig.inMemory();
        spec.correct(data);
        // The loader's sealed adapter is package-private; no file or mod event is needed for draft validation.
        var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
                .getDeclaredConstructor(CommentedConfig.class, Path.class, ModConfig.class);
        constructor.setAccessible(true);
        spec.acceptConfig((net.neoforged.fml.config.IConfigSpec.ILoadedConfig) constructor.newInstance(data, null, null));
    }

    @Test public void numberDraftsRespectTheSpecAndNeverMutateLiveSettings() throws Exception {
        var builder = new ModConfigSpec.Builder();
        var width = builder.defineInRange("width", 192, 128, 320);
        var opacity = builder.defineInRange("opacity", 1.0, 0.4, 1.0);
        var spec = builder.build(); load(spec);
        var draft = new ClientSettingsModel.Entry("width", width);
        draft.number("256");
        assertTrue(draft.valid()); assertTrue(draft.dirty()); assertEquals(192, width.get().intValue());
        draft.number("500"); assertFalse(draft.valid());
        draft.number("12x"); assertFalse(draft.valid());
        draft.reset(); assertTrue(draft.valid()); assertFalse(draft.dirty());
        var decimal = new ClientSettingsModel.Entry("opacity", opacity);
        decimal.number("NaN"); assertFalse(decimal.valid());
        decimal.number("Infinity"); assertFalse(decimal.valid());
        decimal.number("0.25"); assertFalse(decimal.valid());
        decimal.number("0.75"); assertTrue(decimal.valid()); assertEquals(1.0, opacity.get(), 0);
    }

    @Test public void listDraftsAreIndependentAndResetUsesTheSpecDefault() throws Exception {
        var builder = new ModConfigSpec.Builder();
        var values = builder.defineListAllowEmpty("order", List.of("wiki", "quests"), item -> item instanceof String);
        var spec = builder.build(); load(spec);
        var draft = new ClientSettingsModel.Entry("order", values);
        var edit = new ArrayList<>(List.of("quests", "wiki"));
        draft.set(edit); edit.clear();
        assertEquals(List.of("quests", "wiki"), draft.draft());
        assertEquals(List.of("wiki", "quests"), values.get());
        assertTrue(draft.dirty()); draft.reset(); assertFalse(draft.dirty());
        draft.set(List.of()); assertTrue(draft.valid());
    }

    @Test public void everyCurrentSettingHasAnExplicitCategoryAndKeepsItsExistingKey() throws Exception {
        String source = Files.readString(Path.of("src/main/java/de/artemis/cyberneticenhancements/client/ClientConfig.java"));
        var keys = Pattern.compile("\\.define(?:InRange|ListAllowEmpty|List)?\\(\"([^\"]+)\"").matcher(source);
        int checked = 0;
        while (keys.find()) {
            String key = keys.group(1);
            if (!List.of("compassMarkerOverrides", "fabricatorPinnedDesigns", "wikiPinnedEntries", "customCompassMarkers").contains(key))
                assertNotEquals("Uncategorized setting: " + key, ClientSettingsModel.Section.STORAGE, ClientSettingsModel.section(key));
            checked++;
        }
        assertTrue(checked >= 30);
    }
}
