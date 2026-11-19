package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.client.ClientConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;

/** Draft edits keep cancel, defaults and validation independent of the on-disk configuration. */
public final class ClientSettingsModel {
    public enum Section {
        SCREENS("presentation"), ANIMATION("presentation"),
        ARCHIVE("interface"), CALLS("interface"), NOTIFICATIONS("interface"),
        COMPASS_LAYOUT("compass"), COMPASS_MARKERS("compass"), COMPASS_DISPLAY("compass"), STORAGE("advanced");
        public final String group;
        Section(String group) { this.group = group; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    public static Section section(String key) {
        return switch (key) {
            case "customDeathScreen", "customLoadingScreen", "customTitleBackground", "customTitleScreen", "showLaunchIntro" -> Section.SCREENS;
            case "animatedDeathBodies", "showArchiveBootAnimation", "showJoinAnimation" -> Section.ANIMATION;
            case "archiveTabOrder", "hudVisibilityOverrides" -> Section.ARCHIVE;
            case "enablePlayerCalls", "callWindowOnLeft" -> Section.CALLS;
            case "showBetaWelcomeMessage", "checkForUpdates", "showDropoffDeliveryMessages", "showCyberpsychosisTimer" -> Section.NOTIFICATIONS;
            case "showCompass", "compassWidth", "compassTopMargin", "compassFocusAngle", "compassOpacity" -> Section.COMPASS_LAYOUT;
            case "compassQuests", "compassTurnIns", "compassBanners", "compassCities", "compassMaxTac", "compassMedtec" -> Section.COMPASS_MARKERS;
            case "compassCardinals", "compassTicks", "compassCenter", "compassNames", "compassDistances", "compassHeightArrows", "compassEdgeArrows" -> Section.COMPASS_DISPLAY;
            case "compassMarkerOverrides", "fabricatorPinnedDesigns", "wikiPinnedEntries", "customCompassMarkers" -> Section.STORAGE;
            default -> Section.STORAGE;
        };
    }

    public static final class Entry {
        public final String key;
        public final ModConfigSpec.ConfigValue<?> value;
        public final ModConfigSpec.ValueSpec spec;
        private Object original, draft;
        private boolean valid = true;

        public Entry(String key, ModConfigSpec.ConfigValue<?> value) {
            this.key = key; this.value = value; this.spec = value.getSpec();
            original = draft = copy(value.get());
        }
        public Object draft() { return draft; }
        public boolean valid() { return valid; }
        /** A server enforces this value while connected: it is shown, but cannot be edited, reset or saved. */
        public boolean locked() { return de.artemis.cyberneticenhancements.client.EnforcedSettings.locked(value); }
        public boolean dirty() { return !locked() && (!valid || !Objects.equals(original, draft)); }
        public void set(Object candidate) { if (locked()) return; draft = copy(candidate); valid = spec.test(candidate); }
        public void number(String text) {
            if (locked()) return;
            try {
                Object number;
                if (value.getDefault() instanceof Integer) number = Integer.valueOf(text);
                else { double parsed = Double.parseDouble(text); if (!Double.isFinite(parsed)) throw new NumberFormatException(); number = parsed; }
                set(number);
            } catch (NumberFormatException invalid) { draft = text; valid = false; }
        }
        public void reset() { if (!locked()) set(value.getDefault()); }
        public String translation() { return spec.getTranslationKey() == null ? "cyberneticenhancements.configuration." + key : spec.getTranslationKey(); }
        private void refresh() { original = draft = copy(value.get()); valid = true; }
        @SuppressWarnings({"rawtypes", "unchecked"}) private void write(Object next) { ((ModConfigSpec.ConfigValue) value).set(copy(next)); }
        private static Object copy(Object value) { return value instanceof List<?> list ? List.copyOf(list) : value; }
    }

    private final List<Entry> entries;
    private final Runnable persist;

    public ClientSettingsModel() {
        this(ClientConfig.SPEC, ClientConfig.SPEC::save);
    }

    ClientSettingsModel(ModConfigSpec spec, Runnable persist) {
        this.persist = persist;
        entries = spec.isLoaded() ? spec.getValues().valueMap().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof ModConfigSpec.ConfigValue<?>)
                .map(entry -> new Entry(entry.getKey(), (ModConfigSpec.ConfigValue<?>) entry.getValue())).toList() : List.of();
    }
    public List<Entry> entries() { return entries; }
    public boolean dirty() { return entries.stream().anyMatch(Entry::dirty); }
    public boolean valid() { return entries.stream().allMatch(entry -> entry.locked() || entry.valid()); }
    public long changes() { return entries.stream().filter(Entry::dirty).count(); }
    public void resetAll() { entries.forEach(Entry::reset); }
    /** How many of these settings the connected server currently enforces. */
    public long locked() { return entries.stream().filter(Entry::locked).count(); }
    public void save() {
        if (!valid()) throw new IllegalStateException("Invalid configuration draft");
        Map<Entry, Object> previous = new LinkedHashMap<>();
        try {
            for (var entry : entries) if (entry.dirty()) { previous.put(entry, entry.value.get()); entry.write(entry.draft()); }
            if (!previous.isEmpty()) persist.run();
        } catch (RuntimeException failure) {
            previous.forEach(Entry::write);
            throw failure;
        }
        entries.forEach(Entry::refresh);
    }
}
