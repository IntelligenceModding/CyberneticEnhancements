package de.artemis.cyberneticenhancements.client;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public final class SandevistanClientOptions {
    public static boolean afterimages = true;
    public static boolean edgeEffects = true;
    public static boolean restrainedFov = true;
    public static float flashIntensity = 0.20F;
    public static float soundVolume = 0.50F;

    private SandevistanClientOptions() {
    }

    public static void initialize() {
        var file = FMLPaths.CONFIGDIR.get().resolve("cyberneticenhancements").resolve("sandevistan-client.properties");
        Properties values = new Properties();
        try {
            if (Files.exists(file)) {
                try (var input = Files.newInputStream(file)) { values.load(input); }
            }
            afterimages = Boolean.parseBoolean(values.getProperty("afterimages", "true"));
            edgeEffects = Boolean.parseBoolean(values.getProperty("edge_effects", "true"));
            restrainedFov = Boolean.parseBoolean(values.getProperty("restrained_fov", "true"));
            flashIntensity = number(values, "flash_intensity", 0.20F, 0.40F);
            soundVolume = number(values, "sound_volume", 0.50F, 1.0F);
            values.setProperty("afterimages", Boolean.toString(afterimages));
            values.setProperty("edge_effects", Boolean.toString(edgeEffects));
            values.setProperty("restrained_fov", Boolean.toString(restrainedFov));
            values.setProperty("flash_intensity", Float.toString(flashIntensity));
            values.setProperty("sound_volume", Float.toString(soundVolume));
            Files.createDirectories(file.getParent());
            try (var output = Files.newOutputStream(file)) { values.store(output, "Sandevistan presentation only; gameplay is controlled by the server."); }
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Could not read Sandevistan presentation settings", exception);
        }
    }

    private static float number(Properties values, String key, float fallback, float maximum) {
        try {
            float value = Float.parseFloat(values.getProperty(key, Float.toString(fallback)));
            return Float.isFinite(value) ? Math.clamp(value, 0, maximum) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
