package de.artemis.cyberneticenhancements.client.screen;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Everything the map can be told to show or hide, kept in one small file beside the map's own data. */
public final class MapSettings {
    enum Category { DISPLAY, WAYPOINTS, RADAR, MINIMAP }

    enum Type { TOGGLE, CHOICE, RANGE }

    public static final class Setting {
        final String id;
        final Category category;
        final Type type;
        final int fallback, min, max, step;
        private int value;

        private Setting(String id, Category category, Type type, int fallback, int min, int max, int step) {
            this.id = id; this.category = category; this.type = type;
            this.fallback = fallback; this.min = min; this.max = max; this.step = step;
            this.value = fallback;
            ALL.add(this);
        }

        public boolean on() { if (!loaded) load(); return value != 0; }
        public int value() { if (!loaded) load(); return value; }
        public float scale() { return value() / 100F; }
        void set(int next) { value = Math.max(min, Math.min(max, next)); save(); }
        void toggle() { set(on() ? 0 : 1); }
        void reset() { value = fallback; }

        /** Steps a choice round, or a range along its notches; ranges stop at their ends instead of wrapping. */
        void step(int direction) {
            if (type == Type.RANGE) set(value + direction * step);
            else if (type == Type.CHOICE) set(Math.floorMod(value - min + direction, max - min + 1) + min);
            else toggle();
        }

        Component title() { return Component.translatable("screen.cyberneticenhancements.archive.map.settings." + id); }
        Component description() { return Component.translatable("screen.cyberneticenhancements.archive.map.settings." + id + ".desc"); }

        Component shown() {
            return switch (type) {
                case TOGGLE -> Component.translatable("screen.cyberneticenhancements.archive.map.settings." + (on() ? "on" : "off"));
                case CHOICE -> Component.translatable("screen.cyberneticenhancements.archive.map.settings." + id + "." + value);
                case RANGE -> Component.literal(unit.getOrDefault(id, "%s").replace("%s", String.valueOf(value)));
            };
        }
    }

    private static final List<Setting> ALL = new ArrayList<>();
    private static final java.util.Map<String, String> unit = new java.util.HashMap<>();

    private static Setting toggle(Category category, String id, boolean fallback) { return new Setting(id, category, Type.TOGGLE, fallback ? 1 : 0, 0, 1, 1); }

    private static Setting range(Category category, String id, int fallback, int min, int max, int step, String format) {
        var setting = new Setting(id, category, Type.RANGE, fallback, min, max, step);
        unit.put(id, format);
        return setting;
    }

    // Display
    static final Setting GRID = toggle(Category.DISPLAY, "grid", true);
    static final Setting SELF = toggle(Category.DISPLAY, "self", true);
    static final Setting MOUSE_LOCATION = toggle(Category.DISPLAY, "mouse_location", true);
    static final Setting SCALE_BAR = toggle(Category.DISPLAY, "scale_bar", true);
    static final Setting DIMENSION = toggle(Category.DISPLAY, "dimension", true);
    static final Setting SMOOTH = toggle(Category.DISPLAY, "smooth", true);
    static final Setting BRIGHTNESS = range(Category.DISPLAY, "brightness", 100, 50, 150, 10, "%s%%");
    static final Setting FONT_SCALE = range(Category.DISPLAY, "font_scale", 100, 60, 160, 10, "%s%%");
    // Waypoints
    static final Setting MARKERS = toggle(Category.WAYPOINTS, "markers", true);
    static final Setting LABELS = toggle(Category.WAYPOINTS, "labels", true);
    static final Setting EDGE_ARROWS = toggle(Category.WAYPOINTS, "edge_arrows", true);
    static final Setting ICON_SCALE = range(Category.WAYPOINTS, "icon_scale", 100, 50, 200, 25, "%s%%");
    // Radar
    static final Setting PEOPLE = toggle(Category.RADAR, "people", true);
    static final Setting ANIMALS = toggle(Category.RADAR, "animals", true);
    static final Setting AMBIENT = toggle(Category.RADAR, "ambient", false);
    static final Setting HOSTILES = toggle(Category.RADAR, "hostiles", true);
    static final Setting VILLAGERS = toggle(Category.RADAR, "villagers", true);
    static final Setting PETS = toggle(Category.RADAR, "pets", true);
    static final Setting PLAYERS = toggle(Category.RADAR, "players", true);
    static final Setting NAMES = toggle(Category.RADAR, "names", true);
    static final Setting HEADINGS = toggle(Category.RADAR, "headings", false);
    static final Setting FADE = toggle(Category.RADAR, "fade", true);
    static final Setting HIDE_SNEAKING = toggle(Category.RADAR, "hide_sneaking", true);
    static final Setting RADAR_STYLE = new Setting("display", Category.RADAR, Type.CHOICE, 1, 0, 1, 1);
    static final Setting RADAR_SCALE = range(Category.RADAR, "radar_scale", 100, 50, 300, 25, "%s%%");
    static final Setting RANGE = range(Category.RADAR, "range", 128, 16, 512, 16, "%s m");
    static final Setting MAXIMUM = range(Category.RADAR, "maximum", 48, 8, 128, 8, "%s");
    // Minimap: the small map on the HUD, with everything about it kept here.
    public static final Setting MINIMAP = toggle(Category.MINIMAP, "minimap", true);
    public static final Setting MINIMAP_SIZE = range(Category.MINIMAP, "minimap_size", 160, 64, 256, 4, "%s px");
    public static final Setting MINIMAP_RANGE = range(Category.MINIMAP, "minimap_range", 96, 32, 256, 16, "%s m");
    public static final Setting MINIMAP_OPACITY = range(Category.MINIMAP, "minimap_opacity", 100, 40, 100, 10, "%s%%");
    /** Automatic keeps it opposite the call window; the others fix it to a side. */
    public static final Setting MINIMAP_CORNER = new Setting("minimap_corner", Category.MINIMAP, Type.CHOICE, 0, 0, 2, 1);
    public static final Setting MINIMAP_FRAME = toggle(Category.MINIMAP, "minimap_frame", true);
    public static final Setting MINIMAP_MARKERS = toggle(Category.MINIMAP, "minimap_markers", true);
    public static final Setting MINIMAP_NORTH = toggle(Category.MINIMAP, "minimap_north", true);
    public static final Setting MINIMAP_SMOOTH = toggle(Category.MINIMAP, "minimap_smooth", true);
    public static final Setting MINIMAP_CAVES = toggle(Category.MINIMAP, "minimap_caves", false);

    private static volatile boolean loaded;

    private MapSettings() {}

    static List<Setting> all() { load(); return ALL; }

    /** Switches the minimap on or off; returns whether it is now on. */
    public static boolean toggleMinimap() { MINIMAP.toggle(); return MINIMAP.on(); }

    static List<Setting> of(Category category) { return all().stream().filter(setting -> setting.category == category).toList(); }

    static void reset(Category category) {
        for (var setting : of(category)) setting.reset();
        save();
    }

    private static Path file() { return FMLPaths.GAMEDIR.get().resolve("cyberneticenhancements").resolve("map_settings.properties"); }

    static synchronized void load() {
        if (loaded) return;
        loaded = true;
        var file = file();
        if (!Files.isRegularFile(file)) return;
        var properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException ignored) {
            return;
        }
        for (var setting : ALL) {
            try {
                String stored = properties.getProperty(setting.id);
                if (stored != null) setting.value = Math.max(setting.min, Math.min(setting.max, Integer.parseInt(stored.trim())));
            } catch (NumberFormatException ignored) {
                // A hand-edited value that is not a number keeps its default.
            }
        }
    }

    private static void save() {
        if (!loaded) return;
        var properties = new Properties();
        for (var setting : ALL) properties.setProperty(setting.id, Integer.toString(setting.value));
        try {
            Files.createDirectories(file().getParent());
            try (OutputStream out = Files.newOutputStream(file())) {
                properties.store(out, "Cybernetic Enhancements map settings");
            }
        } catch (IOException ignored) {
            // Settings are a convenience; failing to write them must not break the map.
        }
    }
}
