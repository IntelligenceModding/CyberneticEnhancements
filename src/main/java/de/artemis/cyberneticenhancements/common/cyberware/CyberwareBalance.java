package de.artemis.cyberneticenhancements.common.cyberware;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class CyberwareBalance {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(CyberneticEnhancements.MOD_ID + "-balance.properties");

    private static final Map<String, CyberwareEntry> DEFAULT_CYBERWARE = createCyberwareDefaults();
    private static final Map<String, List<CyberwareEffect>> DEFAULT_MODULE_EFFECTS = createModuleDefaults();
    private static final Map<String, ChipwareEntry> DEFAULT_CHIPWARE = createChipwareDefaults();
    private static final Map<String, OperatingSystemEntry> DEFAULT_OPERATING_SYSTEMS = createOperatingSystemDefaults();
    private static final Map<String, Integer> DEFAULT_TRIGGERED_COOLDOWNS = createTriggeredCooldownDefaults();
    private static final Map<String, Integer> DEFAULT_INT_VALUES = createIntDefaults();
    private static final Map<String, Double> DEFAULT_DOUBLE_VALUES = createDoubleDefaults();

    private static Map<String, CyberwareEntry> configuredCyberware = DEFAULT_CYBERWARE;
    private static Map<String, List<CyberwareEffect>> configuredModuleEffects = DEFAULT_MODULE_EFFECTS;
    private static Map<String, ChipwareEntry> configuredChipware = DEFAULT_CHIPWARE;
    private static Map<String, OperatingSystemEntry> configuredOperatingSystems = DEFAULT_OPERATING_SYSTEMS;
    private static Map<String, Integer> configuredTriggeredCooldowns = DEFAULT_TRIGGERED_COOLDOWNS;
    private static Map<String, Integer> configuredIntValues = DEFAULT_INT_VALUES;
    private static Map<String, Double> configuredDoubleValues = DEFAULT_DOUBLE_VALUES;
    private static boolean initialized;

    private CyberwareBalance() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        Properties properties = loadProperties();
        configuredCyberware = createConfiguredCyberware(properties);
        configuredModuleEffects = createConfiguredModuleEffects(properties);
        configuredChipware = createConfiguredChipware(properties);
        configuredOperatingSystems = createConfiguredOperatingSystems(properties);
        configuredTriggeredCooldowns = createConfiguredTriggeredCooldowns(properties);
        configuredIntValues = createConfiguredIntValues(properties);
        configuredDoubleValues = createConfiguredDoubleValues(properties);
        writeConfig();
        initialized = true;
    }

    public static CyberwareEntry resolveCyberware(String id) {
        ensureInitialized();
        CyberwareEntry entry = configuredCyberware.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown cyberware balance id: " + id);
        }
        return entry;
    }

    public static List<CyberwareEffect> resolveModuleEffects(String id) {
        ensureInitialized();
        List<CyberwareEffect> effects = configuredModuleEffects.get(id);
        if (effects == null) {
            throw new IllegalArgumentException("Unknown cyberware module balance id: " + id);
        }
        return effects;
    }

    public static ChipwareEntry resolveChipware(String id) {
        ensureInitialized();
        ChipwareEntry entry = configuredChipware.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown chipware balance id: " + id);
        }
        return entry;
    }

    public static OperatingSystemEntry resolveOperatingSystem(String id) {
        ensureInitialized();
        OperatingSystemEntry entry = configuredOperatingSystems.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown operating system balance id: " + id);
        }
        return entry;
    }

    public static int resolveTriggeredCooldownSeconds(String id) {
        ensureInitialized();
        Integer value = configuredTriggeredCooldowns.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown triggered cooldown balance id: " + id);
        }
        return value;
    }

    public static int intValue(String key) {
        ensureInitialized();
        Integer value = configuredIntValues.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown integer balance key: " + key);
        }
        return value;
    }

    public static double doubleValue(String key) {
        ensureInitialized();
        Double value = configuredDoubleValues.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown numeric balance key: " + key);
        }
        return value;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        if (!Files.exists(CONFIG_PATH)) {
            return properties;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load balance config: " + CONFIG_PATH, exception);
        }
    }

    private static void writeConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                writer.write("# Cybernetic Enhancements balance config\n");
                writer.write("# Edit values and restart the game/server to apply them.\n\n");

                writeCyberwareSection(writer);
                writeModuleSection(writer);
                writeChipwareSection(writer);
                writeOperatingSystemSection(writer);
                writeTriggeredCooldownSection(writer);
                writeIntSection(writer);
                writeDoubleSection(writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write balance config: " + CONFIG_PATH, exception);
        }
    }

    private static void writeCyberwareSection(Writer writer) throws IOException {
        writer.write("# Cyberware\n");
        for (Map.Entry<String, CyberwareEntry> entry : configuredCyberware.entrySet()) {
            String id = entry.getKey();
            CyberwareEntry value = entry.getValue();
            writeProperty(writer, cyberwareKey(id, "capacity_bonus"), Integer.toString(value.capacityBonus()));
            writeProperty(writer, cyberwareKey(id, "chip_slot_count"), Integer.toString(value.chipSlotCount()));
            for (CyberwareEffect effect : value.effects()) {
                writeEffectProperty(writer, cyberwareEffectKey(id, effect.type()), effect);
            }
            writer.write("\n");
        }
    }

    private static void writeModuleSection(Writer writer) throws IOException {
        writer.write("# Modules\n");
        for (Map.Entry<String, List<CyberwareEffect>> entry : configuredModuleEffects.entrySet()) {
            String id = entry.getKey();
            for (CyberwareEffect effect : entry.getValue()) {
                writeEffectProperty(writer, moduleEffectKey(id, effect.type()), effect);
            }
            writer.write("\n");
        }
    }

    private static void writeChipwareSection(Writer writer) throws IOException {
        writer.write("# Chipware\n");
        for (Map.Entry<String, ChipwareEntry> entry : configuredChipware.entrySet()) {
            String id = entry.getKey();
            ChipwareEntry value = entry.getValue();
            writeProperty(writer, chipwareKey(id, "chrome_cost"), Integer.toString(value.chromeCost()));
            for (CyberwareEffect effect : value.effects()) {
                writeEffectProperty(writer, chipwareEffectKey(id, effect.type()), effect);
            }
            writer.write("\n");
        }
    }

    private static void writeOperatingSystemSection(Writer writer) throws IOException {
        writer.write("# Operating systems\n");
        for (Map.Entry<String, OperatingSystemEntry> entry : configuredOperatingSystems.entrySet()) {
            String id = entry.getKey();
            OperatingSystemEntry value = entry.getValue();
            writeProperty(writer, operatingSystemKey(id, "family"), value.family().name().toLowerCase(Locale.ROOT));
            writeProperty(writer, operatingSystemKey(id, "hud_key"), value.hudKey());
            writeProperty(writer, operatingSystemKey(id, "duration_seconds"), Integer.toString(value.durationSeconds()));
            writeProperty(writer, operatingSystemKey(id, "cooldown_seconds"), Integer.toString(value.cooldownSeconds()));
            writer.write("\n");
        }
    }

    private static void writeTriggeredCooldownSection(Writer writer) throws IOException {
        writer.write("# Triggered implant cooldowns\n");
        for (Map.Entry<String, Integer> entry : configuredTriggeredCooldowns.entrySet()) {
            writeProperty(writer, triggeredCooldownKey(entry.getKey()), Integer.toString(entry.getValue()));
        }
        writer.write("\n");
    }

    private static void writeIntSection(Writer writer) throws IOException {
        writeGroupedIntValues(writer, "Cyberstrain thresholds", "cyberstrain.");
        writeGroupedIntValues(writer, "Consumables", "consumable.");
        writeGroupedIntValues(writer, "Face ability timings", "face.");
        writeGroupedIntValues(writer, "Hands timings", "hands.");
        writeGroupedIntValues(writer, "Legs timings", "legs.");
        writeGroupedIntValues(writer, "Frontal cortex timings", "frontal.");
        writeGroupedIntValues(writer, "Circulatory timings", "circulatory.");
        writeUngroupedIntValues(writer, "Other integer values",
                "cyberstrain.", "consumable.", "face.", "hands.", "legs.", "frontal.", "circulatory.");
    }

    private static void writeDoubleSection(Writer writer) throws IOException {
        writeGroupedDoubleValues(writer, "Combat statuses", "combat_status.");
        writeGroupedDoubleValues(writer, "Cyberstrain behavior", "cyberstrain.");
        writeGroupedDoubleValues(writer, "Consumables", "consumable.");
        writeGroupedDoubleValues(writer, "Face behavior", "face.");
        writeGroupedDoubleValues(writer, "Hands behavior", "hands.");
        writeGroupedDoubleValues(writer, "Legs behavior", "legs.");
        writeGroupedDoubleValues(writer, "Frontal cortex behavior", "frontal.");
        writeGroupedDoubleValues(writer, "Circulatory behavior", "circulatory.");
        writeUngroupedDoubleValues(writer, "Other numeric values",
                "combat_status.", "cyberstrain.", "consumable.", "face.", "hands.", "legs.", "frontal.", "circulatory.");
    }

    private static void writeGroupedIntValues(Writer writer, String title, String prefix) throws IOException {
        boolean wroteAny = false;
        for (Map.Entry<String, Integer> entry : configuredIntValues.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            if (!wroteAny) {
                writer.write("# " + title + "\n");
                wroteAny = true;
            }
            writeProperty(writer, intKey(entry.getKey()), Integer.toString(entry.getValue()));
        }
        if (wroteAny) {
            writer.write("\n");
        }
    }

    private static void writeGroupedDoubleValues(Writer writer, String title, String prefix) throws IOException {
        boolean wroteAny = false;
        for (Map.Entry<String, Double> entry : configuredDoubleValues.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            if (!wroteAny) {
                writer.write("# " + title + "\n");
                wroteAny = true;
            }
            writeProperty(writer, doubleKey(entry.getKey()), formatNumber(entry.getValue()));
        }
        if (wroteAny) {
            writer.write("\n");
        }
    }

    private static void writeUngroupedIntValues(Writer writer, String title, String... prefixes) throws IOException {
        boolean wroteAny = false;
        outer:
        for (Map.Entry<String, Integer> entry : configuredIntValues.entrySet()) {
            for (String prefix : prefixes) {
                if (entry.getKey().startsWith(prefix)) {
                    continue outer;
                }
            }
            if (!wroteAny) {
                writer.write("# " + title + "\n");
                wroteAny = true;
            }
            writeProperty(writer, intKey(entry.getKey()), Integer.toString(entry.getValue()));
        }
        if (wroteAny) {
            writer.write("\n");
        }
    }

    private static void writeUngroupedDoubleValues(Writer writer, String title, String... prefixes) throws IOException {
        boolean wroteAny = false;
        outer:
        for (Map.Entry<String, Double> entry : configuredDoubleValues.entrySet()) {
            for (String prefix : prefixes) {
                if (entry.getKey().startsWith(prefix)) {
                    continue outer;
                }
            }
            if (!wroteAny) {
                writer.write("# " + title + "\n");
                wroteAny = true;
            }
            writeProperty(writer, doubleKey(entry.getKey()), formatNumber(entry.getValue()));
        }
        if (wroteAny) {
            writer.write("\n");
        }
    }

    private static void writeEffectProperty(Writer writer, String key, CyberwareEffect effect) throws IOException {
        if (effect.type().isMobEffect()) {
            writeProperty(writer, key, Boolean.toString(true));
            return;
        }
        writeProperty(writer, key, formatNumber(effect.amount()));
    }

    private static void writeProperty(Writer writer, String key, String value) throws IOException {
        writer.write(key + "=" + value + "\n");
    }

    private static Map<String, CyberwareEntry> createConfiguredCyberware(Properties properties) {
        Map<String, CyberwareEntry> configured = new LinkedHashMap<>();
        for (Map.Entry<String, CyberwareEntry> entry : DEFAULT_CYBERWARE.entrySet()) {
            String id = entry.getKey();
            CyberwareEntry defaults = entry.getValue();
            configured.put(id, new CyberwareEntry(
                    readInt(properties, cyberwareKey(id, "capacity_bonus"), defaults.capacityBonus()),
                    readInt(properties, cyberwareKey(id, "chip_slot_count"), defaults.chipSlotCount()),
                    resolveEffects(properties, defaults.effects(), type -> cyberwareEffectKey(id, type))
            ));
        }
        return Map.copyOf(configured);
    }

    private static Map<String, List<CyberwareEffect>> createConfiguredModuleEffects(Properties properties) {
        Map<String, List<CyberwareEffect>> configured = new LinkedHashMap<>();
        for (Map.Entry<String, List<CyberwareEffect>> entry : DEFAULT_MODULE_EFFECTS.entrySet()) {
            String id = entry.getKey();
            configured.put(id, resolveEffects(properties, entry.getValue(), type -> moduleEffectKey(id, type)));
        }
        return Map.copyOf(configured);
    }

    private static Map<String, ChipwareEntry> createConfiguredChipware(Properties properties) {
        Map<String, ChipwareEntry> configured = new LinkedHashMap<>();
        for (Map.Entry<String, ChipwareEntry> entry : DEFAULT_CHIPWARE.entrySet()) {
            String id = entry.getKey();
            ChipwareEntry defaults = entry.getValue();
            configured.put(id, new ChipwareEntry(
                    readInt(properties, chipwareKey(id, "chrome_cost"), defaults.chromeCost()),
                    resolveEffects(properties, defaults.effects(), type -> chipwareEffectKey(id, type))
            ));
        }
        return Map.copyOf(configured);
    }

    private static Map<String, OperatingSystemEntry> createConfiguredOperatingSystems(Properties properties) {
        Map<String, OperatingSystemEntry> configured = new LinkedHashMap<>();
        for (Map.Entry<String, OperatingSystemEntry> entry : DEFAULT_OPERATING_SYSTEMS.entrySet()) {
            String id = entry.getKey();
            OperatingSystemEntry defaults = entry.getValue();
            configured.put(id, new OperatingSystemEntry(
                    readEnum(properties, operatingSystemKey(id, "family"), defaults.family(), OperatingSystemFamily.class),
                    properties.getProperty(operatingSystemKey(id, "hud_key"), defaults.hudKey()),
                    readInt(properties, operatingSystemKey(id, "duration_seconds"), defaults.durationSeconds()),
                    readInt(properties, operatingSystemKey(id, "cooldown_seconds"), defaults.cooldownSeconds())
            ));
        }
        return Map.copyOf(configured);
    }

    private static Map<String, Integer> createConfiguredTriggeredCooldowns(Properties properties) {
        Map<String, Integer> configured = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : DEFAULT_TRIGGERED_COOLDOWNS.entrySet()) {
            configured.put(entry.getKey(), readInt(properties, triggeredCooldownKey(entry.getKey()), entry.getValue()));
        }
        return Map.copyOf(configured);
    }

    private static Map<String, Integer> createConfiguredIntValues(Properties properties) {
        Map<String, Integer> configured = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : DEFAULT_INT_VALUES.entrySet()) {
            configured.put(entry.getKey(), readInt(properties, intKey(entry.getKey()), entry.getValue()));
        }
        return Map.copyOf(configured);
    }

    private static Map<String, Double> createConfiguredDoubleValues(Properties properties) {
        Map<String, Double> configured = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : DEFAULT_DOUBLE_VALUES.entrySet()) {
            configured.put(entry.getKey(), readDouble(properties, doubleKey(entry.getKey()), entry.getValue()));
        }
        return Map.copyOf(configured);
    }

    private static List<CyberwareEffect> resolveEffects(Properties properties, List<CyberwareEffect> defaults, EffectKeyResolver keyResolver) {
        List<CyberwareEffect> effects = new ArrayList<>();
        for (CyberwareEffect effect : defaults) {
            CyberwareEffectType type = effect.type();
            String key = keyResolver.resolve(type);
            if (type.isMobEffect()) {
                if (readBoolean(properties, key, true)) {
                    effects.add(new CyberwareEffect(type, 0.0D));
                }
                continue;
            }

            double amount = readDouble(properties, key, effect.amount());
            if (Math.abs(amount) > 0.0000001D) {
                effects.add(new CyberwareEffect(type, amount));
            }
        }
        return List.copyOf(effects);
    }

    private static String cyberwareKey(String id, String suffix) {
        return "cyberware." + id + "." + suffix;
    }

    private static String cyberwareEffectKey(String id, CyberwareEffectType type) {
        return cyberwareKey(id, "effect." + type.id());
    }

    private static String moduleEffectKey(String id, CyberwareEffectType type) {
        return "module." + id + ".effect." + type.id();
    }

    private static String chipwareKey(String id, String suffix) {
        return "chipware." + id + "." + suffix;
    }

    private static String chipwareEffectKey(String id, CyberwareEffectType type) {
        return chipwareKey(id, "effect." + type.id());
    }

    private static String operatingSystemKey(String id, String suffix) {
        return "operating_system." + id + "." + suffix;
    }

    private static String triggeredCooldownKey(String id) {
        return "triggered_cooldown." + id + ".seconds";
    }

    private static String intKey(String key) {
        return "value.int." + key;
    }

    private static String doubleKey(String key) {
        return "value.double." + key;
    }

    private static int readInt(Properties properties, String key, int defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static double readDouble(Properties properties, String key, double defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static boolean readBoolean(Properties properties, String key, boolean defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static <E extends Enum<E>> E readEnum(Properties properties, String key, E defaultValue, Class<E> enumType) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static Map<String, CyberwareEntry> createCyberwareDefaults() {
        Map<String, CyberwareEntry> defaults = new LinkedHashMap<>();

        defaults.put("camillo_ram_manager", cyberware(4, 0, CyberwareEffect.chromeCapacity(4.0D)));
        defaults.put("ram_reallocator", cyberware(8, 0, CyberwareEffect.chromeCapacity(8.0D)));
        defaults.put("bioconductor", cyberware());
        defaults.put("cox_2_cybersomatic_optimizer", cyberware());
        defaults.put("ex_disk", cyberware(6, 0, CyberwareEffect.chromeCapacity(6.0D)));
        defaults.put("kerenzikov_boost_system", cyberware());
        defaults.put("mechatronic_core", cyberware(0, 0, CyberwareEffect.breakSpeed(0.15D)));
        defaults.put("memory_boost", cyberware());
        defaults.put("newton_module", cyberware());
        defaults.put("axolotl", cyberware());
        defaults.put("quantum_tuner", cyberware());
        defaults.put("ram_upgrade", cyberware(0, 0, CyberwareEffect.chromeCapacity(4.0D)));
        defaults.put("self_ice", cyberware());
        defaults.put("chipware_socket", cyberware(0, 1));
        defaults.put("chipware_socket_mk2", cyberware(0, 2));
        defaults.put("chipware_socket_mk3", cyberware(0, 3, CyberwareEffect.chromeCapacity(2.0D)));

        defaults.put("biodyn_berserk", cyberware(0, 0, CyberwareEffect.damage(0.5D), CyberwareEffect.bonusAbsorption(2.0D), CyberwareEffect.damageReduction(0.05D)));
        defaults.put("militech_berserk", cyberware(0, 0, CyberwareEffect.damage(1.0D), CyberwareEffect.bonusAbsorption(2.0D), CyberwareEffect.damageReduction(0.05D), CyberwareEffect.attackSpeed(0.05D)));
        defaults.put("moore_tech_berserk", cyberware(0, 0, CyberwareEffect.damage(1.0D), CyberwareEffect.moveSpeed(0.10D), CyberwareEffect.damageReduction(0.03D)));
        defaults.put("zetatech_berserk", cyberware(0, 0, CyberwareEffect.damage(1.0D), CyberwareEffect.damageReduction(0.08D), CyberwareEffect.fallReduction(-0.10D)));
        defaults.put("arasaka_shadow", cyberware(2, 0, CyberwareEffect.moveSpeed(0.05D)));
        defaults.put("biotech_sigma", cyberware(4, 0, CyberwareEffect.healthRegen(0.25D)));
        defaults.put("militech_paraline", cyberware(5, 0, CyberwareEffect.attackSpeed(0.10D)));
        defaults.put("netwatch_netdriver", cyberware(6, 0, CyberwareEffect.damageReduction(0.05D)));
        defaults.put("raven_microcyber", cyberware(8, 0, CyberwareEffect.moveSpeed(0.06D), CyberwareEffect.attackSpeed(0.08D)));
        defaults.put("tetratronic_rippler", cyberware(10, 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.damageReduction(0.06D)));
        defaults.put("militech_canto", cyberware(12, 0, CyberwareEffect.damage(1.0D), CyberwareEffect.damageReduction(0.08D)));
        defaults.put("dynalar_sandevistan", cyberware(0, 0, CyberwareEffect.moveSpeed(0.10D), CyberwareEffect.attackSpeed(0.08D)));
        defaults.put("militech_apogee", cyberware(0, 0, CyberwareEffect.moveSpeed(0.18D), CyberwareEffect.attackSpeed(0.16D)));
        defaults.put("militech_experimental_sandevistan", cyberware(0, 0, CyberwareEffect.moveSpeed(0.18D), CyberwareEffect.attackSpeed(0.16D)));
        defaults.put("militech_falcon", cyberware(0, 0, CyberwareEffect.moveSpeed(0.14D), CyberwareEffect.attackSpeed(0.12D)));
        defaults.put("qiant_warp_dancer", cyberware(0, 0, CyberwareEffect.moveSpeed(0.16D), CyberwareEffect.attackSpeed(0.14D)));
        defaults.put("zetatech_sandevistan", cyberware(0, 0, CyberwareEffect.moveSpeed(0.10D), CyberwareEffect.attackSpeed(0.08D)));
        defaults.put("chrome_compressor", cyberware(10, 0));

        defaults.put("gorilla_arms", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.breakSpeed(0.30D), CyberwareEffect.knockbackResistance(0.10D)));
        defaults.put("electrifying_gorilla_arms", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.breakSpeed(0.30D), CyberwareEffect.attackSpeed(0.10D)));
        defaults.put("thermal_gorilla_arms", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.breakSpeed(0.30D), CyberwareEffect.damageReduction(0.05D)));
        defaults.put("chemical_gorilla_arms", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.breakSpeed(0.30D), CyberwareEffect.entityReach(0.5D)));
        defaults.put("mantis_blades", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.15D)));
        defaults.put("electrifying_mantis_blades", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.15D), CyberwareEffect.moveSpeed(0.05D)));
        defaults.put("thermal_mantis_blades", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.15D), CyberwareEffect.damageReduction(0.04D)));
        defaults.put("toxic_mantis_blades", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.15D), CyberwareEffect.entityReach(0.5D)));
        defaults.put("maxtac_mantis_blades", cyberware(0, 0, CyberwareEffect.damage(4.0D), CyberwareEffect.attackSpeed(0.25D), CyberwareEffect.moveSpeed(0.08D)));
        defaults.put("monowire", cyberware(0, 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.entityReach(1.0D)));
        defaults.put("electrifying_monowire", cyberware(0, 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.entityReach(1.0D), CyberwareEffect.moveSpeed(0.04D)));
        defaults.put("thermal_monowire", cyberware(0, 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.entityReach(1.0D), CyberwareEffect.damageReduction(0.03D)));
        defaults.put("toxic_monowire", cyberware(0, 0, CyberwareEffect.attackSpeed(0.10D), CyberwareEffect.entityReach(1.0D), CyberwareEffect.damage(1.0D)));
        defaults.put("projectile_launch_system", cyberware(0, 0, CyberwareEffect.damage(2.0D)));
        defaults.put("electrifying_projectile_launch_system", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.attackSpeed(0.05D)));
        defaults.put("thermal_projectile_launch_system", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.damageReduction(0.04D)));
        defaults.put("toxic_projectile_launch_system", cyberware(0, 0, CyberwareEffect.damage(2.0D), CyberwareEffect.entityReach(0.5D)));
        defaults.put("excavator_arms", cyberware());

        defaults.put("basic_kiroshi_optics", cyberware(0, 0, CyberwareEffect.nightVision()));
        defaults.put("clairvoyant", cyberware(0, 0, CyberwareEffect.nightVision(), CyberwareEffect.blockReach(0.5D)));
        defaults.put("doomsayer", cyberware(0, 0, CyberwareEffect.nightVision()));
        defaults.put("sentry", cyberware(0, 0, CyberwareEffect.nightVision()));
        defaults.put("stalker", cyberware(0, 0, CyberwareEffect.nightVision(), CyberwareEffect.entityReach(1.0D)));
        defaults.put("the_oracle", cyberware(0, 0, CyberwareEffect.nightVision(), CyberwareEffect.blockReach(1.0D), CyberwareEffect.entityReach(1.0D)));
        defaults.put("cockatrice", cyberware(0, 0, CyberwareEffect.nightVision(), CyberwareEffect.damage(1.0D)));
        defaults.put("behavioral_imprint_synced_faceplate", cyberware());
        defaults.put("forager_lens", cyberware());
        defaults.put("vein_reader_optics", cyberware());
        defaults.put("relic_scanner", cyberware());

        defaults.put("bionic_joints", cyberware(0, 0, CyberwareEffect.armor(2.0D)));
        defaults.put("dense_marrow", cyberware(0, 0, CyberwareEffect.damage(1.0D)));
        defaults.put("epimorphic_skeleton", cyberware(0, 0, CyberwareEffect.hearts(2.0D)));
        defaults.put("feen_x", cyberware());
        defaults.put("kinetic_frame", cyberware(0, 0, CyberwareEffect.knockbackResistance(0.10D)));
        defaults.put("para_bellum", cyberware(0, 0, CyberwareEffect.armor(4.0D)));
        defaults.put("rara_avis", cyberware(0, 0, CyberwareEffect.armor(6.0D)));
        defaults.put("ram_recoup", cyberware());
        defaults.put("scar_coalescer", cyberware(0, 0, CyberwareEffect.armor(2.0D), CyberwareEffect.hearts(2.0D)));
        defaults.put("scarab", cyberware(0, 0, CyberwareEffect.armor(2.0D), CyberwareEffect.safeFall(1.0D)));
        defaults.put("spring_joints", cyberware(0, 0, CyberwareEffect.fallReduction(-0.15D), CyberwareEffect.safeFall(3.0D)));
        defaults.put("titanium_bones", cyberware(0, 0, CyberwareEffect.knockbackResistance(0.15D), CyberwareEffect.hearts(2.0D)));
        defaults.put("universal_booster", cyberware(0, 0, CyberwareEffect.healthRegen(0.4D)));
        defaults.put("cargo_spine", cyberware());

        defaults.put("ballistic_coprocessor", cyberware(0, 0, CyberwareEffect.damage(1.0D)));
        defaults.put("handle_wrap", cyberware(0, 0, CyberwareEffect.attackSpeed(0.12D)));
        defaults.put("microgenerator", cyberware());
        defaults.put("shock_absorber", cyberware(0, 0, CyberwareEffect.knockbackResistance(0.10D)));
        defaults.put("immovable_force", cyberware(0, 0, CyberwareEffect.knockbackResistance(0.15D)));
        defaults.put("smart_link", cyberware(0, 0, CyberwareEffect.damage(0.5D), CyberwareEffect.entityReach(0.5D)));
        defaults.put("precision_miner", cyberware());
        defaults.put("masons_grip", cyberware(0, 0, CyberwareEffect.blockReach(0.5D)));
        defaults.put("harvester_hands", cyberware());

        defaults.put("adrenaline_converter", cyberware(0, 0, CyberwareEffect.moveSpeed(0.12D)));
        defaults.put("adreno_trigger", cyberware(0, 0, CyberwareEffect.moveSpeed(0.15D), CyberwareEffect.attackSpeed(0.08D)));
        defaults.put("atomic_sensors", cyberware(0, 0, CyberwareEffect.nightVision()));
        defaults.put("kerenzikov", cyberware(0, 0, CyberwareEffect.moveSpeed(0.15D), CyberwareEffect.attackSpeed(0.08D)));
        defaults.put("neofiber", cyberware(0, 0, CyberwareEffect.knockbackResistance(0.15D)));
        defaults.put("reflex_tuner", cyberware(0, 0, CyberwareEffect.moveSpeed(0.18D), CyberwareEffect.attackSpeed(0.10D)));
        defaults.put("revulsor", cyberware(0, 0, CyberwareEffect.damageReduction(0.10D)));
        defaults.put("stabber", cyberware(0, 0, CyberwareEffect.damage(1.0D), CyberwareEffect.attackSpeed(0.10D)));
        defaults.put("synaptic_accelerator", cyberware(0, 0, CyberwareEffect.moveSpeed(0.15D), CyberwareEffect.attackSpeed(0.10D)));
        defaults.put("tyrosine_injector", cyberware(0, 0, CyberwareEffect.damage(2.0D)));
        defaults.put("visual_cortex_support", cyberware(0, 0, CyberwareEffect.entityReach(0.5D)));
        defaults.put("deep_field_visual_interface", cyberware(0, 0, CyberwareEffect.entityReach(1.0D), CyberwareEffect.blockReach(1.0D)));
        defaults.put("kiroshi_retrieval_suite", cyberware());

        defaults.put("adrenaline_booster", cyberware(0, 0, CyberwareEffect.healthRegen(0.35D)));
        defaults.put("biomonitor", cyberware(0, 0, CyberwareEffect.healthRegen(0.5D)));
        defaults.put("black_mamba", cyberware());
        defaults.put("blood_pump", cyberware(0, 0, CyberwareEffect.hearts(1.0D), CyberwareEffect.bonusAbsorption(4.0D)));
        defaults.put("clutch_padding", cyberware(0, 0, CyberwareEffect.knockbackResistance(0.10D)));
        defaults.put("isometric_stabilizer", cyberware(0, 0, CyberwareEffect.knockbackResistance(0.12D), CyberwareEffect.hearts(1.0D)));
        defaults.put("feedback_circuit", cyberware());
        defaults.put("electromag_recycler", cyberware());
        defaults.put("heal_on_kill", cyberware());
        defaults.put("microrotors", cyberware(0, 0, CyberwareEffect.attackSpeed(0.08D)));
        defaults.put("second_heart", cyberware(0, 0, CyberwareEffect.hearts(2.0D), CyberwareEffect.bonusAbsorption(2.0D)));
        defaults.put("threatevac", cyberware(0, 0, CyberwareEffect.moveSpeed(0.08D)));
        defaults.put("hydrolung", cyberware(0, 0, CyberwareEffect.waterBreathing(), CyberwareEffect.dolphinsGrace()));

        defaults.put("carapace", cyberware(0, 0, CyberwareEffect.armor(3.0D)));
        defaults.put("cellular_adapter", cyberware(0, 0, CyberwareEffect.healthRegen(0.4D)));
        defaults.put("cogito_lattice", cyberware(0, 0, CyberwareEffect.damageReduction(0.06D)));
        defaults.put("countershell", cyberware(0, 0, CyberwareEffect.damageReduction(0.06D)));
        defaults.put("defenzikov", cyberware(0, 0, CyberwareEffect.damageReduction(0.10D)));
        defaults.put("nano_plating", cyberware(0, 0, CyberwareEffect.armor(4.0D)));
        defaults.put("optical_camo", cyberware());
        defaults.put("pain_editor", cyberware(0, 0, CyberwareEffect.damageReduction(0.10D)));
        defaults.put("painducer", cyberware(0, 0, CyberwareEffect.hearts(1.0D), CyberwareEffect.damageReduction(0.05D)));
        defaults.put("proxishield", cyberware(0, 0, CyberwareEffect.armor(2.0D)));
        defaults.put("peripheral_inverse", cyberware(0, 0, CyberwareEffect.armor(4.0D)));
        defaults.put("rangeguard", cyberware(0, 0, CyberwareEffect.armor(2.0D)));
        defaults.put("shock_n_awe", cyberware());
        defaults.put("subdermal_armor", cyberware(0, 0, CyberwareEffect.armor(3.0D)));
        defaults.put("chitin", cyberware(0, 0, CyberwareEffect.armor(6.0D), CyberwareEffect.hearts(1.0D)));

        defaults.put("fortified_ankles", cyberware(0, 0, CyberwareEffect.jumpPower(0.35D), CyberwareEffect.safeFall(4.0D)));
        defaults.put("jenkins_tendons", cyberware(0, 0, CyberwareEffect.moveSpeed(0.10D)));
        defaults.put("leeroy_ligament_system", cyberware(0, 0, CyberwareEffect.moveSpeed(0.15D)));
        defaults.put("lynx_paws", cyberware(0, 0, CyberwareEffect.moveSpeed(0.05D), CyberwareEffect.safeFall(4.0D), CyberwareEffect.stepHeight(0.5D)));
        defaults.put("reinforced_tendons", cyberware(0, 0, CyberwareEffect.safeFall(6.0D)));
        defaults.put("brushstep_legs", cyberware());

        return Map.copyOf(defaults);
    }

    private static Map<String, List<CyberwareEffect>> createModuleDefaults() {
        Map<String, List<CyberwareEffect>> defaults = new LinkedHashMap<>();
        defaults.put("mining_accelerator", effects(CyberwareEffect.breakSpeed(0.15D)));
        defaults.put("reach_extender", effects(CyberwareEffect.blockReach(1.0D), CyberwareEffect.entityReach(0.5D)));
        defaults.put("combat_actuator", effects(CyberwareEffect.damage(1.0D), CyberwareEffect.attackSpeed(0.10D)));
        defaults.put("builder_grip", effects(CyberwareEffect.blockReach(0.5D)));
        defaults.put("shock_palm", effects(CyberwareEffect.damage(2.0D)));
        defaults.put("dash_piston", effects(CyberwareEffect.moveSpeed(0.10D)));
        defaults.put("fall_damper", effects(CyberwareEffect.safeFall(4.0D), CyberwareEffect.fallReduction(-0.10D)));
        defaults.put("sprint_motor", effects(CyberwareEffect.moveSpeed(0.12D)));
        defaults.put("climbing_servo", effects(CyberwareEffect.stepHeight(0.5D)));
        defaults.put("stealth_foot", effects(CyberwareEffect.moveSpeed(0.05D), CyberwareEffect.safeFall(2.0D)));
        return Map.copyOf(defaults);
    }

    private static Map<String, ChipwareEntry> createChipwareDefaults() {
        Map<String, ChipwareEntry> defaults = new LinkedHashMap<>();
        defaults.put("miner_skillchip", chipware(1, CyberwareEffect.breakSpeed(0.15D)));
        defaults.put("builder_skillchip", chipware(1, CyberwareEffect.blockReach(0.5D)));
        defaults.put("scout_skillchip", chipware(2, CyberwareEffect.moveSpeed(0.08D), CyberwareEffect.entityReach(0.5D)));
        defaults.put("combat_skillchip", chipware(2, CyberwareEffect.damage(1.0D), CyberwareEffect.attackSpeed(0.10D)));
        defaults.put("runner_skillchip", chipware(2, CyberwareEffect.moveSpeed(0.10D), CyberwareEffect.safeFall(2.0D)));
        defaults.put("trader_skillchip", chipware(2, CyberwareEffect.entityReach(0.5D), CyberwareEffect.blockReach(0.5D)));
        return Map.copyOf(defaults);
    }

    private static Map<String, OperatingSystemEntry> createOperatingSystemDefaults() {
        Map<String, OperatingSystemEntry> defaults = new LinkedHashMap<>();
        defaults.put("biodyn_berserk", operatingSystem(OperatingSystemFamily.BERSERK, "hud.cyberneticenhancements.ability.biodyn_berserk", 10, 34));
        defaults.put("militech_berserk", operatingSystem(OperatingSystemFamily.BERSERK, "hud.cyberneticenhancements.ability.militech_berserk", 10, 36));
        defaults.put("moore_tech_berserk", operatingSystem(OperatingSystemFamily.BERSERK, "hud.cyberneticenhancements.ability.moore_tech_berserk", 12, 35));
        defaults.put("zetatech_berserk", operatingSystem(OperatingSystemFamily.BERSERK, "hud.cyberneticenhancements.ability.zetatech_berserk", 10, 33));
        defaults.put("arasaka_shadow", operatingSystem(OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.arasaka_shadow", 8, 30));
        defaults.put("biotech_sigma", operatingSystem(OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.biotech_sigma", 9, 32));
        defaults.put("militech_paraline", operatingSystem(OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.militech_paraline", 10, 34));
        defaults.put("netwatch_netdriver", operatingSystem(OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.netwatch_netdriver", 10, 36));
        defaults.put("raven_microcyber", operatingSystem(OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.raven_microcyber", 10, 38));
        defaults.put("tetratronic_rippler", operatingSystem(OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.tetratronic_rippler", 10, 38));
        defaults.put("militech_canto", operatingSystem(OperatingSystemFamily.CYBERDECK, "hud.cyberneticenhancements.ability.militech_canto", 12, 45));
        defaults.put("dynalar_sandevistan", operatingSystem(OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.dynalar_sandevistan", 8, 28));
        defaults.put("zetatech_sandevistan", operatingSystem(OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.zetatech_sandevistan", 8, 28));
        defaults.put("militech_falcon", operatingSystem(OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.militech_falcon", 10, 30));
        defaults.put("qiant_warp_dancer", operatingSystem(OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.qiant_warp_dancer", 8, 32));
        defaults.put("militech_apogee", operatingSystem(OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.militech_apogee", 12, 34));
        defaults.put("militech_experimental_sandevistan", operatingSystem(OperatingSystemFamily.SANDEVISTAN, "hud.cyberneticenhancements.ability.militech_experimental_sandevistan", 8, 30));
        defaults.put("chrome_compressor", operatingSystem(OperatingSystemFamily.NONE, "", 0, 0));
        return Map.copyOf(defaults);
    }

    private static Map<String, Integer> createTriggeredCooldownDefaults() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("biomonitor", 45);
        defaults.put("blood_pump", 60);
        defaults.put("second_heart", 180);
        defaults.put("reflex_tuner", 40);
        return Map.copyOf(defaults);
    }

    private static Map<String, Integer> createIntDefaults() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("cyberstrain.threshold.psychosis", 50);
        defaults.put("cyberstrain.threshold.critical", 35);
        defaults.put("cyberstrain.threshold.unstable", 20);
        defaults.put("cyberstrain.threshold.strained", 10);

        defaults.put("consumable.maxdoc_mk1.cooldown_seconds", 50);
        defaults.put("consumable.maxdoc_mk1.overdose_points", 0);
        defaults.put("consumable.maxdoc_mk2.cooldown_seconds", 70);
        defaults.put("consumable.maxdoc_mk2.overdose_points", 0);
        defaults.put("consumable.maxdoc_mk3.cooldown_seconds", 90);
        defaults.put("consumable.maxdoc_mk3.overdose_points", 0);
        defaults.put("consumable.bounce_back_mk1.cooldown_seconds", 60);
        defaults.put("consumable.bounce_back_mk1.overdose_points", 0);
        defaults.put("consumable.bounce_back_mk1.duration_ticks", 180);
        defaults.put("consumable.bounce_back_mk2.cooldown_seconds", 80);
        defaults.put("consumable.bounce_back_mk2.overdose_points", 0);
        defaults.put("consumable.bounce_back_mk2.duration_ticks", 220);
        defaults.put("consumable.bounce_back_mk3.cooldown_seconds", 100);
        defaults.put("consumable.bounce_back_mk3.overdose_points", 0);
        defaults.put("consumable.bounce_back_mk3.duration_ticks", 260);
        defaults.put("consumable.health_booster.cooldown_seconds", 160);
        defaults.put("consumable.health_booster.overdose_points", 1);
        defaults.put("consumable.health_booster.duration_ticks", 4_800);
        defaults.put("consumable.stamina_booster.cooldown_seconds", 160);
        defaults.put("consumable.stamina_booster.overdose_points", 1);
        defaults.put("consumable.stamina_booster.duration_ticks", 4_800);
        defaults.put("consumable.oxy_booster.cooldown_seconds", 120);
        defaults.put("consumable.oxy_booster.overdose_points", 0);
        defaults.put("consumable.oxy_booster.duration_ticks", 6_000);
        defaults.put("consumable.capacity_booster.cooldown_seconds", 200);
        defaults.put("consumable.capacity_booster.overdose_points", 1);
        defaults.put("consumable.capacity_booster.duration_ticks", 12_000);
        defaults.put("consumable.ram_jolt.cooldown_seconds", 120);
        defaults.put("consumable.ram_jolt.overdose_points", 2);
        defaults.put("consumable.ram_jolt.duration_ticks", 3_600);
        defaults.put("consumable.immunoblockers.cooldown_seconds", 240);
        defaults.put("consumable.immunoblockers.overdose_points", 3);
        defaults.put("consumable.immunoblockers.duration_ticks", 12_000);
        defaults.put("consumable.immunoblockers.suppression_amount", 12);
        defaults.put("consumable.chrome_suppressant.cooldown_seconds", 140);
        defaults.put("consumable.chrome_suppressant.overdose_points", 1);
        defaults.put("consumable.chrome_suppressant.duration_ticks", 7_200);
        defaults.put("consumable.chrome_suppressant.suppression_amount", 6);
        defaults.put("consumable.black_lace.cooldown_seconds", 180);
        defaults.put("consumable.black_lace.overdose_points", 4);
        defaults.put("consumable.black_lace.duration_ticks", 2_400);
        defaults.put("consumable.asskick.cooldown_seconds", 150);
        defaults.put("consumable.asskick.overdose_points", 3);
        defaults.put("consumable.asskick.duration_ticks", 3_600);
        defaults.put("consumable.jellytricity.cooldown_seconds", 150);
        defaults.put("consumable.jellytricity.overdose_points", 3);
        defaults.put("consumable.jellytricity.duration_ticks", 3_600);

        defaults.put("face.active_seconds.basic_kiroshi_optics", 6);
        defaults.put("face.cooldown_seconds.basic_kiroshi_optics", 16);
        defaults.put("face.active_seconds.clairvoyant", 8);
        defaults.put("face.cooldown_seconds.clairvoyant", 20);
        defaults.put("face.active_seconds.doomsayer", 8);
        defaults.put("face.cooldown_seconds.doomsayer", 18);
        defaults.put("face.active_seconds.sentry", 8);
        defaults.put("face.cooldown_seconds.sentry", 18);
        defaults.put("face.active_seconds.stalker", 8);
        defaults.put("face.cooldown_seconds.stalker", 22);
        defaults.put("face.active_seconds.the_oracle", 10);
        defaults.put("face.cooldown_seconds.the_oracle", 24);
        defaults.put("face.active_seconds.cockatrice", 8);
        defaults.put("face.cooldown_seconds.cockatrice", 24);
        defaults.put("face.active_seconds.behavioral_imprint_synced_faceplate", 10);
        defaults.put("face.cooldown_seconds.behavioral_imprint_synced_faceplate", 40);
        defaults.put("face.active_seconds.vein_reader_optics", 8);
        defaults.put("face.cooldown_seconds.vein_reader_optics", 20);
        defaults.put("face.active_seconds.relic_scanner", 9);
        defaults.put("face.cooldown_seconds.relic_scanner", 24);
        defaults.put("face.oracle.max_hostiles", 12);
        defaults.put("face.oracle.vertical_scan", 6);
        defaults.put("face.oracle.max_blocks", 18);
        defaults.put("face.cockatrice.lock_ticks", 160);
        defaults.put("face.cockatrice.track_refresh_ticks", 50);
        defaults.put("face.faceplate.duration_ticks", 200);
        defaults.put("face.forager.range", 10);
        defaults.put("face.forager.vertical_scan", 4);
        defaults.put("face.forager.max_blocks", 20);
        defaults.put("face.vein_reader.radius", 8);
        defaults.put("face.vein_reader.vertical_scan", 5);
        defaults.put("face.vein_reader.max_blocks", 18);
        defaults.put("face.relic_scanner.radius", 12);
        defaults.put("face.relic_scanner.vertical_scan", 5);
        defaults.put("face.relic_scanner.max_blocks", 18);
        defaults.put("face.passive_highlight_ttl_ticks", 26);

        defaults.put("hands.microgenerator.cooldown_ticks", 80);
        defaults.put("hands.microgenerator.surge_ticks", 70);
        defaults.put("hands.microgenerator.max_arc_targets", 2);
        defaults.put("hands.microgenerator.primary_shock_ticks", 60);
        defaults.put("hands.microgenerator.arc_shock_ticks", 40);
        defaults.put("hands.microgenerator.highlight_ttl_ticks", 28);
        defaults.put("hands.smart_link.mark_ticks", 70);
        defaults.put("hands.smart_link.highlight_ttl_ticks", 50);
        defaults.put("hands.ballistic.chain_window_ticks", 60);
        defaults.put("hands.ballistic.trauma_base_ticks", 60);
        defaults.put("hands.ballistic.trauma_long_ticks", 90);
        defaults.put("hands.ballistic.highlight_ttl_ticks", 40);
        defaults.put("hands.ballistic.handling_buff_ticks", 24);
        defaults.put("hands.harvester.replant_highlight_ttl_ticks", 18);

        defaults.put("legs.fortified_ankles.max_charge_ticks", 24);
        defaults.put("legs.fortified_ankles.release_window_ticks", 14);
        defaults.put("legs.dash.cooldown_with_module_ticks", 120);
        defaults.put("legs.dash.cooldown_without_module_ticks", 160);
        defaults.put("legs.leeroy.impact_cooldown_ticks", 8);

        defaults.put("arms.excavator.active_seconds", 10);
        defaults.put("arms.excavator.cooldown_seconds", 16);

        defaults.put("frontal.camillo_ram_manager.emergency_refund_cooldown_ticks", 700);
        defaults.put("frontal.ram_reallocator.emergency_refund_cooldown_ticks", 500);
        defaults.put("frontal.kerenzikov_boost_system.reflex_extension_ticks", 40);
        defaults.put("frontal.self_ice.effect_ticks", 160);
        defaults.put("frontal.self_ice.cooldown_seconds", 70);
        defaults.put("frontal.quantum_tuner.cooldown_seconds", 90);

        defaults.put("circulatory.heal_on_kill.regen_ticks", 80);
        defaults.put("circulatory.heal_on_kill.elite_regen_ticks", 120);
        defaults.put("circulatory.adrenaline_booster.kill_buff_ticks", 80);
        defaults.put("circulatory.adrenaline_booster.proc_ticks", 100);
        defaults.put("circulatory.adrenaline_booster.cooldown_ticks", 160);
        defaults.put("circulatory.isometric_stabilizer.proc_ticks", 80);
        defaults.put("circulatory.isometric_stabilizer.cooldown_ticks", 360);
        defaults.put("circulatory.threatevac.proc_ticks", 120);
        defaults.put("circulatory.threatevac.cooldown_ticks", 480);
        defaults.put("circulatory.feedback_circuit.proc_ticks", 60);
        defaults.put("circulatory.feedback_circuit.cooldown_ticks", 120);
        defaults.put("circulatory.microrotors.proc_ticks", 70);
        defaults.put("circulatory.microrotors.cooldown_ticks", 80);
        return Map.copyOf(defaults);
    }

    private static Map<String, Double> createDoubleDefaults() {
        Map<String, Double> defaults = new LinkedHashMap<>();
        de.artemis.cyberneticenhancements.common.cyberware.sandevistan.SandevistanProfile.addBalanceDefaults(defaults);
        defaults.put("combat_status.shock.move_speed_per_stack", -0.08D);
        defaults.put("combat_status.shock.attack_speed_per_stack", -0.12D);
        defaults.put("combat_status.shock.ability_cooldown_per_stack", 0.30D);
        defaults.put("combat_status.overheat.damage_taken_per_stack", 0.05D);
        defaults.put("combat_status.trauma.damage_taken_per_stack", 0.08D);
        defaults.put("combat_status.bleed.damage_taken_per_stack", 0.05D);
        defaults.put("combat_status.mark.damage_taken_per_stack", 0.12D);
        defaults.put("combat_status.trauma.knockback_taken_per_stack", 0.35D);
        defaults.put("cyberstrain.psychosis.minor.step_height_bonus", 0.9D);
        defaults.put("cyberstrain.psychosis.major.step_height_bonus", 1.5D);
        defaults.put("cyberstrain.psychosis.hidden_target_max_direct_distance", 14.0D);
        defaults.put("cyberstrain.psychosis.hidden_target_max_path_distance", 26.0D);

        defaults.put("consumable.maxdoc_mk1.heal", 6.0D);
        defaults.put("consumable.maxdoc_mk2.heal", 9.0D);
        defaults.put("consumable.maxdoc_mk3.heal", 12.0D);
        defaults.put("consumable.bounce_back_mk1.heal", 3.0D);
        defaults.put("consumable.bounce_back_mk1.regen", 0.5D);
        defaults.put("consumable.bounce_back_mk2.heal", 5.0D);
        defaults.put("consumable.bounce_back_mk2.regen", 0.8D);
        defaults.put("consumable.bounce_back_mk3.heal", 7.0D);
        defaults.put("consumable.bounce_back_mk3.regen", 1.1D);
        defaults.put("consumable.health_booster.hearts", 4.0D);
        defaults.put("consumable.stamina_booster.move_speed", 0.12D);
        defaults.put("consumable.stamina_booster.break_speed", 0.15D);
        defaults.put("consumable.capacity_booster.chrome_capacity", 6.0D);
        defaults.put("consumable.ram_jolt.cooldown_factor", 0.55D);
        defaults.put("consumable.black_lace.health_fraction_cost", 0.20D);
        defaults.put("consumable.black_lace.instability", 10.0D);
        defaults.put("consumable.black_lace.move_speed", 0.18D);
        defaults.put("consumable.black_lace.damage", 2.0D);
        defaults.put("consumable.black_lace.damage_reduction", 0.06D);
        defaults.put("consumable.black_lace.break_speed", 0.12D);
        defaults.put("consumable.asskick.instability", 5.0D);
        defaults.put("consumable.asskick.hearts", 4.0D);
        defaults.put("consumable.jellytricity.health_fraction_cost", 0.10D);
        defaults.put("consumable.jellytricity.instability", 7.0D);
        defaults.put("consumable.jellytricity.move_speed", 0.22D);
        defaults.put("consumable.jellytricity.break_speed", 0.20D);

        defaults.put("face.oracle.range", 24.0D);
        defaults.put("face.cockatrice.target_range", 22.0D);
        defaults.put("face.cockatrice.fallback_target_range", 18.0D);
        defaults.put("face.cockatrice.damage_bonus", 1.5D);
        defaults.put("face.cockatrice.move_speed_bonus", 0.12D);
        defaults.put("face.cockatrice.attack_speed_bonus", 0.75D);
        defaults.put("face.cockatrice.entity_reach_bonus", 1.0D);
        defaults.put("face.faceplate.scramble_radius", 22.0D);
        defaults.put("face.faceplate.safe_bubble_sqr", 25.0D);
        defaults.put("face.faceplate.move_speed_bonus", 0.14D);
        defaults.put("face.faceplate.step_height_bonus", 0.5D);
        defaults.put("face.faceplate.damage_reduction_bonus", 0.20D);
        defaults.put("face.survey.safe_light_level", 0.0D);

        defaults.put("hands.microgenerator.arc_radius", 3.5D);
        defaults.put("hands.microgenerator.extra_damage", 0.75D);
        defaults.put("hands.microgenerator.absorption_bonus", 3.0D);
        defaults.put("hands.microgenerator.attack_speed_bonus", 0.08D);
        defaults.put("hands.microgenerator.move_speed_bonus", 0.06D);
        defaults.put("hands.microgenerator.arc_radius_per_extra", 0.5D);
        defaults.put("hands.smart_link.damage_bonus", 0.75D);
        defaults.put("hands.ballistic.base_damage_bonus", 1.0D);
        defaults.put("hands.ballistic.mid_range_damage_bonus", 0.6D);
        defaults.put("hands.ballistic.long_range_damage_bonus", 0.6D);
        defaults.put("hands.ballistic.chain_damage_bonus", 0.8D);
        defaults.put("hands.ballistic.unaware_damage_bonus", 0.5D);
        defaults.put("hands.ballistic.mid_range_threshold", 8.0D);
        defaults.put("hands.ballistic.long_range_threshold", 14.0D);
        defaults.put("hands.ballistic.trauma_long_range_threshold", 12.0D);
        defaults.put("hands.ballistic.attack_speed_bonus", 0.03D);
        defaults.put("hands.ballistic.knockback_resistance_bonus", 0.04D);
        defaults.put("hands.shock_absorber.projectile_min_factor", 0.55D);
        defaults.put("hands.shock_absorber.projectile_per_stack", 0.12D);
        defaults.put("hands.shock_absorber.explosion_min_factor", 0.60D);
        defaults.put("hands.shock_absorber.explosion_per_stack", 0.10D);
        defaults.put("hands.shock_absorber.impact_min_factor", 0.70D);
        defaults.put("hands.shock_absorber.impact_per_stack", 0.08D);
        defaults.put("hands.shock_absorber.knockback_min_factor", 0.20D);
        defaults.put("hands.shock_absorber.knockback_per_stack", 0.30D);
        defaults.put("hands.immovable_force.knockback_factor", 0.25D);
        defaults.put("hands.immovable_force.crouch_knockback_factor", 0.05D);
        defaults.put("hands.precision_miner.break_speed_bonus", 0.45D);
        defaults.put("arms.excavator.break_speed_bonus", 0.35D);

        defaults.put("legs.movement.min_sqr", 0.0025D);
        defaults.put("legs.movement.charge_min_sqr", 0.01D);
        defaults.put("legs.fortified_ankles.vertical_base", 0.14D);
        defaults.put("legs.fortified_ankles.vertical_scale", 0.30D);
        defaults.put("legs.fortified_ankles.vertical_extra_per_install", 0.05D);
        defaults.put("legs.fortified_ankles.forward_base", 0.06D);
        defaults.put("legs.fortified_ankles.forward_scale", 0.12D);
        defaults.put("legs.jenkins_tendons.takeoff_forward", 0.10D);
        defaults.put("legs.jenkins_tendons.takeoff_vertical", 0.03D);
        defaults.put("legs.leeroy_ligament_system.takeoff_forward", 0.30D);
        defaults.put("legs.leeroy_ligament_system.takeoff_vertical", 0.08D);
        defaults.put("legs.dash.base_burst", 0.42D);
        defaults.put("legs.dash.leeroy_bonus", 0.20D);
        defaults.put("legs.dash.module_bonus", 0.26D);
        defaults.put("legs.dash.leeroy_speed_bonus", 0.05D);
        defaults.put("legs.dash.module_speed_bonus", 0.08D);
        defaults.put("legs.leeroy.impact_required_speed_sqr", 0.06D);
        defaults.put("legs.leeroy.impact_hitbox", 1.1D);
        defaults.put("legs.leeroy.impact_forward_expand", 1.15D);
        defaults.put("legs.leeroy.impact_knockback", 0.45D);
        defaults.put("legs.leeroy.impact_knockback_per_install", 0.16D);
        defaults.put("legs.leeroy.impact_damage", 1.5D);
        defaults.put("legs.climbing_servo.crouch_speed", 0.16D);
        defaults.put("legs.climbing_servo.base_speed", 0.22D);
        defaults.put("legs.climbing_servo.extra_speed_per_install", 0.02D);
        defaults.put("legs.lynx_paws.aggro_radius_base", 5.0D);
        defaults.put("legs.lynx_paws.aggro_radius_per_install", 2.0D);
        defaults.put("legs.stealth_foot.aggro_radius_per_install", 1.5D);
        defaults.put("legs.lynx_paws.close_threat_crouch", 2.5D);
        defaults.put("legs.lynx_paws.close_threat_standing", 4.0D);
        defaults.put("legs.lynx_paws.max_silent_speed", 0.11D);
        defaults.put("legs.stealth_foot.max_silent_speed", 0.16D);
        defaults.put("legs.lynx_paws.soft_landing_fall_distance", 7.0D);
        defaults.put("legs.brushstep_speed_bonus", 0.18D);
        defaults.put("legs.brushstep_step_height_bonus", 0.3D);

        defaults.put("frontal.ram_upgrade.cooldown_reduction", 0.04D);
        defaults.put("frontal.ex_disk.cooldown_reduction", 0.02D);
        defaults.put("frontal.bioconductor.cooldown_reduction", 0.08D);
        defaults.put("frontal.cox_2_cybersomatic_optimizer.cooldown_reduction", 0.15D);
        defaults.put("frontal.quantum_tuner.cooldown_reduction", 0.10D);
        defaults.put("frontal.max_cooldown_reduction", 0.45D);
        defaults.put("frontal.ram_upgrade.status_duration_bonus", 0.05D);
        defaults.put("frontal.ex_disk.status_duration_bonus", 0.08D);
        defaults.put("frontal.bioconductor.status_duration_bonus", 0.10D);
        defaults.put("frontal.cox_2_cybersomatic_optimizer.status_duration_bonus", 0.20D);
        defaults.put("frontal.max_status_duration_bonus", 0.60D);
        defaults.put("frontal.ram_reallocator.emergency_refund_percent", 0.40D);
        defaults.put("frontal.camillo_ram_manager.emergency_refund_percent", 0.20D);
        defaults.put("frontal.self_ice.instability_reduction", 18.0D);
        defaults.put("frontal.self_ice.damage_reduction_bonus", 0.10D);
        defaults.put("frontal.self_ice.knockback_resistance_bonus", 0.10D);
        defaults.put("frontal.self_ice.move_speed_bonus", 0.10D);
        defaults.put("frontal.memory_boost.flat_refund_ticks", 40.0D);
        defaults.put("frontal.newton_module.percent_refund", 0.02D);
        defaults.put("frontal.axolotl.elite_percent_refund", 0.10D);
        defaults.put("frontal.axolotl.percent_refund", 0.06D);
        defaults.put("frontal.max_kill_refund_percent", 0.75D);
        defaults.put("frontal.quantum_tuner.activation_load", 4.0D);

        defaults.put("circulatory.heal_on_kill.max_heal_percent", 0.20D);
        defaults.put("circulatory.heal_on_kill.flat_heal_per_install", 2.0D);
        defaults.put("circulatory.heal_on_kill.regen_bonus", 0.25D);
        defaults.put("circulatory.adrenaline_booster.kill_move_speed_bonus", 0.04D);
        defaults.put("circulatory.adrenaline_booster.kill_regen_bonus", 0.20D);
        defaults.put("circulatory.clutch_padding.knockback_min_factor", 0.25D);
        defaults.put("circulatory.clutch_padding.knockback_per_install", 0.20D);
        defaults.put("circulatory.isometric_stabilizer.knockback_factor", 0.35D);
        defaults.put("circulatory.isometric_stabilizer.crouch_knockback_factor", 0.10D);
        defaults.put("circulatory.adrenaline_booster.proc_min_damage", 2.0D);
        defaults.put("circulatory.adrenaline_booster.proc_health_threshold", 0.70D);
        defaults.put("circulatory.adrenaline_booster.proc_regen_bonus", 0.35D);
        defaults.put("circulatory.adrenaline_booster.proc_move_speed_bonus", 0.05D);
        defaults.put("circulatory.isometric_stabilizer.proc_min_damage", 4.0D);
        defaults.put("circulatory.isometric_stabilizer.proc_health_threshold", 0.55D);
        defaults.put("circulatory.isometric_stabilizer.damage_reduction_bonus", 0.08D);
        defaults.put("circulatory.isometric_stabilizer.knockback_resistance_bonus", 0.12D);
        defaults.put("circulatory.threatevac.proc_min_damage", 3.0D);
        defaults.put("circulatory.threatevac.proc_health_threshold", 0.40D);
        defaults.put("circulatory.threatevac.move_speed_bonus", 0.10D);
        defaults.put("circulatory.threatevac.step_height_bonus", 0.3D);
        defaults.put("circulatory.threatevac.safe_fall_bonus", 3.0D);
        defaults.put("circulatory.threatevac.damage_reduction_bonus", 0.04D);
        defaults.put("circulatory.feedback_circuit.flat_refund_ticks", 20.0D);
        defaults.put("circulatory.electromag_recycler.flat_refund_ticks", 50.0D);
        defaults.put("circulatory.electromag_recycler.percent_refund", 0.06D);
        defaults.put("circulatory.feedback_circuit.absorption_bonus", 1.5D);
        defaults.put("circulatory.microrotors.attack_speed_bonus", 0.05D);
        defaults.put("circulatory.microrotors.move_speed_bonus", 0.15D);
        defaults.put("circulatory.electromag_recycler.ranged_flat_refund_ticks", 10.0D);
        defaults.put("circulatory.electromag_recycler.ranged_percent_refund", 0.02D);
        defaults.put("circulatory.black_mamba.bonus_damage", 0.75D);
        defaults.put("circulatory.hydrolung.break_speed_bonus", 0.30D);
        defaults.put("skeleton.cargo_spine.pickup_radius", 2.0D);
        defaults.put("nervous.kiroshi_retrieval_suite.pickup_radius", 2.25D);

        return Map.copyOf(defaults);
    }

    private static CyberwareEntry cyberware() {
        return cyberware(0, 0);
    }

    private static CyberwareEntry cyberware(int capacityBonus, int chipSlotCount, CyberwareEffect... effects) {
        return new CyberwareEntry(capacityBonus, chipSlotCount, effects(effects));
    }

    private static ChipwareEntry chipware(int chromeCost, CyberwareEffect... effects) {
        return new ChipwareEntry(chromeCost, effects(effects));
    }

    private static OperatingSystemEntry operatingSystem(OperatingSystemFamily family, String hudKey, int durationSeconds, int cooldownSeconds) {
        return new OperatingSystemEntry(family, hudKey, durationSeconds, cooldownSeconds);
    }

    private static List<CyberwareEffect> effects(CyberwareEffect... effects) {
        return List.of(effects);
    }

    private interface EffectKeyResolver {
        String resolve(CyberwareEffectType type);
    }

    public record CyberwareEntry(int capacityBonus, int chipSlotCount, List<CyberwareEffect> effects) {
    }

    public record ChipwareEntry(int chromeCost, List<CyberwareEffect> effects) {
    }

    public record OperatingSystemEntry(
            OperatingSystemFamily family,
            String hudKey,
            int durationSeconds,
            int cooldownSeconds
    ) {
    }
}
