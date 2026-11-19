package de.artemis.cyberneticenhancements.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiFadeLayer;
import de.artemis.cyberneticenhancements.client.ui.UiVisibilityTransition;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class HudVisibilityController {
    private static final String FILE_NAME = "cyberneticenhancements-client.properties";
    private static final String HUD_ENABLED_KEY = "hud.enabled";

    private static boolean loaded;
    private static boolean hudEnabled = true;
    private static UiVisibilityTransition visibility;
    private static final UiFadeLayer FADE = new UiFadeLayer();

    private HudVisibilityController() {
    }

    static boolean isHudEnabled() {
        ensureLoaded();
        return effective();
    }

    /** A server can keep the HUD fixed on or off while the player is connected; their own choice is left untouched. */
    private static boolean effective() {
        Boolean forced = EnforcedSettings.extraBoolean(EnforcedSettings.HUD_ENABLED);
        return forced != null ? forced : hudEnabled;
    }

    static boolean enforced() { return EnforcedSettings.extraBoolean(EnforcedSettings.HUD_ENABLED) != null; }

    static boolean toggleHud() {
        ensureLoaded();
        if (enforced()) return effective();
        opacity();
        hudEnabled = !hudEnabled;
        opacity();
        save();
        return hudEnabled;
    }

    private static float opacity() {
        ensureLoaded();
        if (visibility == null) visibility = new UiVisibilityTransition(effective());
        return visibility.sample(effective(), System.nanoTime() / 1_000_000_000D);
    }

    static boolean isHudVisible() { return opacity() > .001F; }

    static void render(GuiGraphics graphics, Runnable content) {
        float opacity = opacity();
        if (opacity <= .001F) return;
        if (opacity >= .999F) content.run();
        else FADE.render(graphics, opacity, 1, 0, content);
    }

    static void clear() {
        FADE.close();
        visibility = null;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        Path configPath = getConfigPath();
        if (configPath == null || !Files.exists(configPath)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
            hudEnabled = Boolean.parseBoolean(properties.getProperty(HUD_ENABLED_KEY, Boolean.TRUE.toString()));
        } catch (IOException ignored) {
            hudEnabled = true;
        }
    }

    private static void save() {
        Path configPath = getConfigPath();
        if (configPath == null) {
            return;
        }

        try {
            Files.createDirectories(configPath.getParent());
            Properties properties = new Properties();
            properties.setProperty(HUD_ENABLED_KEY, Boolean.toString(hudEnabled));
            try (OutputStream outputStream = Files.newOutputStream(configPath)) {
                properties.store(outputStream, "Cybernetic Enhancements client settings");
            }
        } catch (IOException ignored) {
        }
    }

    private static Path getConfigPath() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameDirectory == null) {
            return null;
        }
        return minecraft.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }
}
