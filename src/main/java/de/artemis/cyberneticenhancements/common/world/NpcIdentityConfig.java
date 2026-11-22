package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

public final class NpcIdentityConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(CyberneticEnhancements.MOD_ID + "-npc-identities.properties");
    private static final Path LEGACY_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(CyberneticEnhancements.MOD_ID + "-fixer-names.properties");
    private static final List<String> LEGACY_MALE_NAMES = List.of(
            "Dexter DeShawn",
            "Sebastian \"Padre\" Ibarra",
            "Dino Dinovic",
            "Muamar \"El Capitan\" Reyes",
            "Mr. Hands"
    );
    private static final List<String> LEGACY_FEMALE_NAMES = List.of(
            "Rogue Amendiares",
            "Regina Jones",
            "Wakako Okada",
            "Dakota Smith"
    );
    private static final List<String> LEGACY_GENERIC_NAMES = List.of();
    private static final List<String> DEFAULT_MALE_NAMES = List.of(
            "Dexter DeShawn",
            "Sebastian \"Padre\" Ibarra",
            "Dino Dinovic",
            "Muamar \"El Capitan\" Reyes",
            "Mr. Hands",
            "Johnny Silverhand",
            "Jackie Welles",
            "Viktor Vektor",
            "Kerry Eurodyne",
            "River Ward",
            "Saburo Arasaka",
            "Yorinobu Arasaka",
            "Goro Takemura",
            "Saul Bright",
            "Adam Smasher",
            "Solomon \"Sol\" Reed",
            "Kurt Hansen",
            "Aymeric Cassel",
            "Aaron McCarlson",
            "Aaron Waines",
            "Alan Crank",
            "Alan Noel",
            "Albert Murphy",
            "Alec Johnson",
            "Aleksander Pushkin",
            "Alois Daquin",
            "Anders Hellman",
            "Andrew Jones",
            "Andrew Moore",
            "Andrew Newman",
            "Andrew Weyland",
            "Anthony Anderson",
            "Anthony Gilchrist",
            "Anthony Harris",
            "Anton Kolev",
            "Antonio Luccessi",
            "Archibald Crane",
            "Arif Iqbal",
            "Arif Koneczko",
            "Arnold Haponik",
            "Arthur Jenkins",
            "Asher Peterson",
            "Axel Mankievicz",
            "Baird",
            "Barry Alken",
            "Barry Lewis",
            "Bartolomeo Mordellini",
            "Ben DeBaillon",
            "Benedict McAdams",
            "Big Joe",
            "Big Pete",
            "Bill Jablonsky",
            "Bill Mitchel",
            "Bob Sagan",
            "Boris Ribakov",
            "Boz",
            "Brad Stacy",
            "Brandon Frost",
            "Brendan",
            "Bruce Ward",
            "Bruce Welby",
            "Bryce Mosley",
            "Bryce Stone",
            "Buck Arnold",
            "Campo Orta",
            "Carl Robinson",
            "Carlos Rubio",
            "Carter Smith",
            "Cedric Muller",
            "Chang-Hoon Nam",
            "Charles Bucks",
            "Charles Graham",
            "Charles Wilson",
            "Chester Bennett",
            "Chester Hamilton",
            "Coach Fred",
            "Colver",
            "Commissioner J. Hammerman",
            "Commissioner Ramos",
            "Costin Lahovary",
            "Crispin Weyland",
            "Darius Clarke",
            "Darius Miles",
            "Darrell Zhou",
            "David Beemer",
            "David Walker",
            "Dean Russell",
            "Declan Griffin",
            "Dennis Cranmer",
            "Diego Ramirez",
            "Donald Lundee",
            "Donald Lundee Jr.",
            "Driss Meriana",
            "Dum Dum",
            "Earl Conway",
            "Edgar Tool",
            "Emilio Gutierrez",
            "Emmerick Bronson",
            "Ernesto Munoz",
            "Eron Acedo",
            "Euralio Alma",
            "Evan McRay",
            "Falco",
            "Finn Gerstatt",
            "Flavio dos Santos",
            "Frank Nostra",
            "Fredrik Persson",
            "Futoshi Yamada",
            "Gaston Phillips",
            "Gaston Slayton",
            "Gauge Harris",
            "Gerald Winkler",
            "Gottfrid Persson",
            "Gustavo Orta",
            "Hajime Taki",
            "Hal Cantos",
            "Harold Han",
            "Harry",
            "Haruyoshi Nishikata",
            "Mitch Anderson",
            "Jefferson Peralez",
            "Oda Sandayu",
            "Spider Murphy",
            "Morgan Blackhand",
            "Rache Bartmoss",
            "Santiago Aldecaldo",
            "Dante Caruso",
            "Jotaro Shobo",
            "Joshua Stephenson",
            "Randy Kutcher",
            "Lucius Rhyne",
            "Weldon Holt",
            "Cassius Ryder",
            "Oswald Forrest",
            "Kirk Sawyer"
    );
    private static final List<String> DEFAULT_FEMALE_NAMES = List.of(
            "Rogue Amendiares",
            "Regina Jones",
            "Wakako Okada",
            "Dakota Smith",
            "Judy Alvarez",
            "Panam Palmer",
            "Misty Olszewski",
            "Evelyn Parker",
            "Hanako Arasaka",
            "Alt Cunningham",
            "Song \"Songbird\" So Mi",
            "Rosalind Myers",
            "Alena \"Alex\" Xenakis",
            "Aurore Cassel",
            "Lina Malina",
            "T-Bug",
            "Ainara Alvarez",
            "Akari Yamakage",
            "Alamini Davis",
            "Alma Maria",
            "Ana Friedman",
            "Anel Duffault",
            "Angelica Whelan",
            "Angie Mielech",
            "Anna Hamill",
            "Anna Nox",
            "Annie Capolino",
            "Arabelle Luvasha",
            "Ayo Zarin",
            "Bara Nova",
            "Barbara Okoye",
            "Beatrice Ellen Trieste",
            "Bes Isis",
            "Blue Moon",
            "Bree Whitney",
            "Briana Dolson",
            "Brigitte",
            "Brittany Hayes",
            "Camila Martinez",
            "Carol Emeka",
            "Cassidy Righter",
            "Charlene Fox",
            "Cheri Nowlin",
            "Christine Markov",
            "Claire Russell",
            "Claudia Feldman",
            "Cynthia Najarro",
            "Cesar Diego Ruiz",
            "Darline Boucicault",
            "Diana Cuno",
            "Dollie Radcliff",
            "Elizabeth Borden",
            "Elizabeth Kress",
            "Elizabeth Peralez",
            "Emilia Morton",
            "Emilie Massenat",
            "Emily Scooter",
            "Ertha Ojasma",
            "Eva Cole",
            "Fae Nesheim",
            "Farah Bleus",
            "Farida Nazeri",
            "Faye Russo",
            "Fiona Vargas",
            "Gale Gibbs",
            "Georgina Zembinsky",
            "Gillean Jordan",
            "Griselda Martinez",
            "Guadalupe Alejandra Welles",
            "Hallie Coggins",
            "Meredith Stout",
            "Lizzy Wizzy",
            "Sandra Dorsett",
            "Maiko Maeda",
            "Susan Abernathy",
            "Nancy Hartley",
            "Rachel Casich",
            "Matilda K. Rose"
    );
    private static final List<String> DEFAULT_GENERIC_NAMES = List.of(
            "Songbird",
            "Altiera Cunningham",
            "Delamain",
            "Delamain (AI)",
            "Skippy",
            "1R-0NC-LAD",
            "Akira",
            "Angel",
            "Ash",
            "Ashlay",
            "B@d",
            "Denny",
            "Dietlinde",
            "Goldhand",
            "So Mi"
    );
    private static final CategoryNames DEFAULT_CATEGORY_NAMES = new CategoryNames(DEFAULT_MALE_NAMES, DEFAULT_FEMALE_NAMES, DEFAULT_GENERIC_NAMES);

    private static CategoryNames configuredNames = DEFAULT_CATEGORY_NAMES;
    private static boolean initialized;

    private NpcIdentityConfig() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        Properties properties = loadProperties();
        if (looksLikeLegacyDefaultConfig(properties)) {
            configuredNames = DEFAULT_CATEGORY_NAMES;
            writeConfig();
            initialized = true;
            return;
        }
        configuredNames = new CategoryNames(
                resolveConfiguredNames(properties.getProperty("male_names"), DEFAULT_MALE_NAMES),
                resolveConfiguredNames(properties.getProperty("female_names"), DEFAULT_FEMALE_NAMES),
                resolveConfiguredNames(properties.getProperty("generic_names"), DEFAULT_GENERIC_NAMES)
        );
        writeConfig();
        initialized = true;
    }

    public static CategoryNames categories() {
        ensureInitialized();
        return configuredNames;
    }

    public static CategoryNames defaultCategories() {
        return DEFAULT_CATEGORY_NAMES;
    }

    public static NpcCategory categoryForName(String name) {
        ensureInitialized();
        if (configuredNames.male().contains(name)) {
            return NpcCategory.MALE;
        }
        if (configuredNames.female().contains(name)) {
            return NpcCategory.FEMALE;
        }
        if (configuredNames.generic().contains(name)) {
            return NpcCategory.GENERIC;
        }
        if (DEFAULT_MALE_NAMES.contains(name)) {
            return NpcCategory.MALE;
        }
        if (DEFAULT_FEMALE_NAMES.contains(name)) {
            return NpcCategory.FEMALE;
        }
        return NpcCategory.GENERIC;
    }

    public static String textureFolder(NpcCategory category) {
        return "textures/entity/npc/" + category.id();
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        Path source = Files.exists(CONFIG_PATH) ? CONFIG_PATH : LEGACY_CONFIG_PATH;
        if (!Files.exists(source)) {
            return properties;
        }

        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load NPC identity config: " + source, exception);
        }
    }

    private static void writeConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                writer.write("# Cybernetic Enhancements NPC identity config\n");
                writer.write("# Edit values and restart the game/server to apply them.\n");
                writer.write("# Names are comma-separated. Existing spawned NPCs keep their saved name and category.\n");
                writer.write("# Categories also choose the skin folder under assets/cyberneticenhancements/textures/entity/npc/.\n");
                writer.write("# male names use npc/male, female names use npc/female, generic names use npc/generic.\n\n");
                writer.write("# Default built-in male names:\n");
                writer.write("# " + String.join(", ", DEFAULT_MALE_NAMES) + "\n");
                writer.write("# Default built-in female names:\n");
                writer.write("# " + String.join(", ", DEFAULT_FEMALE_NAMES) + "\n");
                writer.write("# Default built-in generic names:\n");
                writer.write("# " + String.join(", ", DEFAULT_GENERIC_NAMES) + "\n\n");
                writer.write("male_names=" + String.join(", ", configuredNames.male()) + "\n");
                writer.write("female_names=" + String.join(", ", configuredNames.female()) + "\n");
                writer.write("generic_names=" + String.join(", ", configuredNames.generic()) + "\n");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write NPC identity config: " + CONFIG_PATH, exception);
        }
    }

    private static List<String> resolveConfiguredNames(String raw, List<String> defaults) {
        if (raw == null) {
            return defaults;
        }
        List<String> parsed = parseNameList(raw);
        return parsed.isEmpty() ? List.of() : parsed;
    }

    private static boolean looksLikeLegacyDefaultConfig(Properties properties) {
        if (properties.isEmpty()) {
            return false;
        }
        return resolveConfiguredNames(properties.getProperty("male_names"), DEFAULT_MALE_NAMES).equals(LEGACY_MALE_NAMES)
                && resolveConfiguredNames(properties.getProperty("female_names"), DEFAULT_FEMALE_NAMES).equals(LEGACY_FEMALE_NAMES)
                && resolveConfiguredNames(properties.getProperty("generic_names"), DEFAULT_GENERIC_NAMES).equals(LEGACY_GENERIC_NAMES);
    }

    private static List<String> parseNameList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(values::add);
        return List.copyOf(values);
    }

    public enum NpcCategory {
        MALE("male"),
        FEMALE("female"),
        GENERIC("generic");

        private final String id;

        NpcCategory(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static NpcCategory fromId(String id) {
            for (NpcCategory category : values()) {
                if (category.id.equalsIgnoreCase(id)) {
                    return category;
                }
            }
            return GENERIC;
        }
    }

    public record NameEntry(String name, NpcCategory category) {
    }

    public record CategoryNames(List<String> male, List<String> female, List<String> generic) {
        public List<NameEntry> flattened() {
            java.util.ArrayList<NameEntry> entries = new java.util.ArrayList<>();
            male.forEach(name -> entries.add(new NameEntry(name, NpcCategory.MALE)));
            female.forEach(name -> entries.add(new NameEntry(name, NpcCategory.FEMALE)));
            generic.forEach(name -> entries.add(new NameEntry(name, NpcCategory.GENERIC)));
            return List.copyOf(entries);
        }
    }
}
