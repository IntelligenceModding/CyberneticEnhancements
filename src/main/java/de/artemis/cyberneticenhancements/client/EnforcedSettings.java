package de.artemis.cyberneticenhancements.client;

import com.mojang.logging.LogUtils;
import de.artemis.cyberneticenhancements.common.config.EnforcedEntries;
import de.artemis.cyberneticenhancements.common.config.EnforcedSettingsConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;

/**
 * Client settings a server has enforced, in force from joining a world until leaving it. Every client setting is read through
 * {@code ConfigValue.get()}, which the enforcement mixin answers from here, so no setting needs its own check and nothing is
 * ever written to the player's own files. Settings kept outside the config file (Sandevistan presentation, the HUD toggle) are
 * asked for by name.
 */
public final class EnforcedSettings {
    public record Resolved(Map<ModConfigSpec.ConfigValue<?>, Object> values, Map<String, Object> extras, Map<String, String> entries, List<String> problems) {
        public boolean isEmpty() { return values.isEmpty() && extras.isEmpty(); }
    }

    public static final String HUD_ENABLED = "hud.enabled", SANDEVISTAN_AFTERIMAGES = "sandevistan.afterimages",
            SANDEVISTAN_EDGE_EFFECTS = "sandevistan.edge_effects", SANDEVISTAN_RESTRAINED_FOV = "sandevistan.restrained_fov",
            SANDEVISTAN_FLASH_INTENSITY = "sandevistan.flash_intensity", SANDEVISTAN_SOUND_VOLUME = "sandevistan.sound_volume";
    /** Settings stored outside the client config file: their kind and, for numbers, the range they are valid in. */
    private record Extra(boolean number, double min, double max) {}
    private static final Map<String, Extra> EXTRAS = Map.of(
            HUD_ENABLED, new Extra(false, 0, 0), SANDEVISTAN_AFTERIMAGES, new Extra(false, 0, 0), SANDEVISTAN_EDGE_EFFECTS, new Extra(false, 0, 0),
            SANDEVISTAN_RESTRAINED_FOV, new Extra(false, 0, 0), SANDEVISTAN_FLASH_INTENSITY, new Extra(true, 0, 0.4), SANDEVISTAN_SOUND_VOLUME, new Extra(true, 0, 1));
    private static final Resolved NONE = new Resolved(Map.of(), Map.of(), Map.of(), List.of());
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean connected;
    private static volatile Object lastRaw;
    private static volatile Resolved resolved = NONE;
    private static volatile Resolved forcedForTests;

    private EnforcedSettings() {}

    /** Turns the server's text entries into typed values for the given settings, naming every entry that cannot be used. */
    public static Resolved resolve(ModConfigSpec spec, Map<String, String> entries) {
        var known = new HashMap<String, ModConfigSpec.ConfigValue<?>>();
        spec.getValues().valueMap().forEach((key, value) -> { if (value instanceof ModConfigSpec.ConfigValue<?> setting) known.put(key, setting); });
        var values = new IdentityHashMap<ModConfigSpec.ConfigValue<?>, Object>();
        var extras = new LinkedHashMap<String, Object>();
        var problems = new ArrayList<String>();
        entries.forEach((key, text) -> {
            var extra = EXTRAS.get(key);
            var setting = known.get(key);
            if (extra != null) {
                Object typed = extra.number() ? number(text) : bool(text);
                if (typed == null || extra.number() && ((Double) typed < extra.min() || (Double) typed > extra.max()))
                    problems.add("\"" + key + "\" cannot be \"" + text + "\"" + (extra.number() ? " (allowed: " + extra.min() + " to " + extra.max() + ")" : " (use true or false)"));
                else extras.put(key, typed);
            } else if (setting == null) problems.add("\"" + key + "\" is not a client setting");
            else {
                Object typed = typed(text, setting.getDefault());
                if (typed == null) problems.add("\"" + key + "\" cannot be set to \"" + text + "\"" + (setting.getDefault() instanceof List<?> ? " (list settings cannot be enforced)" : ""));
                else if (!setting.getSpec().test(typed)) problems.add("\"" + key + "\" cannot be \"" + text + "\" (outside its allowed values)");
                else values.put(setting, typed);
            }
        });
        return new Resolved(Collections.unmodifiableMap(values), Collections.unmodifiableMap(extras), Map.copyOf(entries), List.copyOf(problems));
    }

    /** What is wrong with these entries, for the server's log; empty when all of them are usable. */
    public static List<String> validate(Map<String, String> entries) { return resolve(ClientConfig.SPEC, entries).problems(); }

    private static Object typed(String text, Object like) {
        if (like instanceof Boolean) return bool(text);
        if (like instanceof Integer) { try { return Integer.valueOf(text); } catch (NumberFormatException invalid) { return null; } }
        if (like instanceof Long) { try { return Long.valueOf(text); } catch (NumberFormatException invalid) { return null; } }
        if (like instanceof Double) return number(text);
        if (like instanceof String) return text;
        if (like instanceof Enum<?> constant) {
            for (var option : constant.getDeclaringClass().getEnumConstants()) if (option.name().equalsIgnoreCase(text)) return option;
        }
        return null;
    }

    private static Boolean bool(String text) {
        return text.equalsIgnoreCase("true") ? Boolean.TRUE : text.equalsIgnoreCase("false") ? Boolean.FALSE : null;
    }

    private static Double number(String text) {
        try {
            double parsed = Double.parseDouble(text);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException invalid) { return null; }
    }

    private static Resolved current() {
        var testing = forcedForTests;
        if (testing != null) return testing;
        if (!EnforcedSettingsConfig.SPEC.isLoaded()) return NONE;
        // The list is cached by the config, so an unchanged file is one identity comparison.
        Object raw = EnforcedSettingsConfig.ENTRIES.get();
        if (raw == lastRaw) return resolved;
        var parsed = EnforcedEntries.parse(EnforcedSettingsConfig.ENTRIES.get());
        var next = resolve(ClientConfig.SPEC, parsed.entries());
        var problems = new ArrayList<>(parsed.problems());
        problems.addAll(next.problems());
        resolved = new Resolved(next.values(), next.extras(), next.entries(), List.copyOf(problems));
        lastRaw = raw;
        problems.forEach(problem -> LOGGER.warn("Server-enforced client settings: {}", problem));
        return resolved;
    }

    /** The enforced value of a client setting, or null when it is not enforced. Called for every config read in the game. */
    public static Object forced(ModConfigSpec.ConfigValue<?> value) {
        if (!connected || value == EnforcedSettingsConfig.ENTRIES) return null;
        var current = current();
        return current.values().isEmpty() ? null : current.values().get(value);
    }

    public static boolean locked(ModConfigSpec.ConfigValue<?> value) { return forced(value) != null; }

    /** How many settings the server enforces right now, for the config screen's notice. */
    public static int count() {
        if (!connected) return 0;
        var current = current();
        return current.values().size() + current.extras().size();
    }

    public static Boolean extraBoolean(String key) { return connected && current().extras().get(key) instanceof Boolean value ? value : null; }

    public static Double extraNumber(String key) { return connected && current().extras().get(key) instanceof Double value ? value : null; }

    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        connected = true;
        var current = current();
        if (current.isEmpty()) return;
        var applied = new TreeMap<String, Object>(current.extras());
        current.values().forEach((setting, forced) -> applied.put(String.join(".", setting.getPath()), forced));
        LOGGER.info("This server enforces {} client settings: {}", applied.size(), applied);
        // getRaw() bypasses enforcement and reads the player's own file, so the log shows exactly what the server changed.
        current.values().forEach((setting, forced) -> {
            Object own = setting.getRaw();
            if (!Objects.equals(own, forced)) LOGGER.info("Server-enforced setting {}: your value {} is replaced by {} while connected", String.join(".", setting.getPath()), own, forced);
        });
    }

    /** Enforcement ends with the connection. The synced file stays in memory afterwards, so it must never be consulted outside one. */
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { connected = false; }

    /** For tests and tools: enforce these lines against the given settings as if connected to an enforcing server. */
    public static void enforceForTests(ModConfigSpec spec, List<String> lines) {
        forcedForTests = resolve(spec, EnforcedEntries.parse(lines).entries());
        connected = true;
    }

    public static void clearForTests() {
        forcedForTests = null;
        connected = false;
        lastRaw = null;
        resolved = NONE;
    }
}
