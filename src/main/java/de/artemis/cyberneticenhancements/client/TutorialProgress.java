package de.artemis.cyberneticenhancements.client;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Per-instance tutorial state, kept in the instance's config folder rather than in any world or server. */
public final class TutorialProgress {
    public enum State { OPEN, DONE, SKIPPED }

    /** The tutorial is not ready yet: no start-up offer, and the title button stays greyed out. Flip when it ships. */
    public static final boolean AVAILABLE = false;

    private static State state;

    private TutorialProgress() {
    }

    public static State state() {
        if (state != null) return state;
        // Set only by the dev client run in build.gradle, never by a real installation.
        if (Boolean.getBoolean("cyberneticenhancements.resetTutorial")) set(State.OPEN);
        else state = load();
        return state;
    }

    public static boolean open() { return AVAILABLE && state() == State.OPEN; }

    public static void set(State next) {
        state = next;
        Properties values = new Properties();
        values.setProperty("state", next.name().toLowerCase(Locale.ROOT));
        try {
            Files.createDirectories(file().getParent());
            try (var output = Files.newOutputStream(file())) { values.store(output, "Tutorial progress for this instance: open, done or skipped."); }
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Could not save tutorial progress", exception);
        }
    }

    private static State load() {
        Properties values = new Properties();
        try {
            if (Files.exists(file())) {
                try (var input = Files.newInputStream(file())) { values.load(input); }
            }
            return State.valueOf(values.getProperty("state", "open").trim().toUpperCase(Locale.ROOT));
        } catch (IOException | IllegalArgumentException exception) {
            LogUtils.getLogger().warn("Could not read tutorial progress; treating it as open", exception);
            return State.OPEN;
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("cyberneticenhancements").resolve("tutorial.properties");
    }
}
