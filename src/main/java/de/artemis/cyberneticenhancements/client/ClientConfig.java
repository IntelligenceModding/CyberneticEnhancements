package de.artemis.cyberneticenhancements.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> CUSTOM_COMPASS_MARKERS;
    public static final ModConfigSpec.BooleanValue ANIMATED_DEATH_BODIES;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ARCHIVE_TAB_ORDER;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HUD_VISIBILITY_OVERRIDES;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> FABRICATOR_PINNED_DESIGNS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> WIKI_PINNED_ENTRIES;
    public static final ModConfigSpec.BooleanValue SHOW_BETA_WELCOME_MESSAGE;
    public static final ModConfigSpec.BooleanValue CHECK_FOR_UPDATES;
    public static final ModConfigSpec.BooleanValue SHOW_CHANGELOG_POPUP;
    public static final ModConfigSpec.ConfigValue<String> LAST_SEEN_CHANGELOG;
    public static final ModConfigSpec.BooleanValue SHOW_ARCHIVE_BOOT_ANIMATION;
    public static final ModConfigSpec.BooleanValue CUSTOM_DEATH_SCREEN;
    public static final ModConfigSpec.BooleanValue CUSTOM_LOADING_SCREEN;
    public static final ModConfigSpec.BooleanValue CUSTOM_TITLE_BACKGROUND;
    public static final ModConfigSpec.BooleanValue CUSTOM_TITLE_SCREEN;
    public static final ModConfigSpec.BooleanValue SHOW_LAUNCH_INTRO;
    public static final ModConfigSpec.BooleanValue SHOW_JOIN_ANIMATION;
    public static final ModConfigSpec.BooleanValue CALL_WINDOW_ON_LEFT;
    public static final ModConfigSpec.BooleanValue ENABLE_PLAYER_CALLS;
    public static final ModConfigSpec.BooleanValue SHOW_DROPOFF_DELIVERY_MESSAGES;
    public static final ModConfigSpec.BooleanValue SHOW_COMPASS;
    public static final ModConfigSpec.BooleanValue SHOW_CYBERPSYCHOSIS_TIMER;
    public static final ModConfigSpec.BooleanValue COMPASS_QUESTS;
    public static final ModConfigSpec.BooleanValue COMPASS_TURN_INS;
    public static final ModConfigSpec.BooleanValue COMPASS_BANNERS;
    public static final ModConfigSpec.BooleanValue COMPASS_CITIES;
    public static final ModConfigSpec.BooleanValue COMPASS_MAX_TAC;
    public static final ModConfigSpec.BooleanValue COMPASS_MEDTEC;
    public static final ModConfigSpec.BooleanValue COMPASS_CARDINALS;
    public static final ModConfigSpec.BooleanValue COMPASS_TICKS;
    public static final ModConfigSpec.BooleanValue COMPASS_CENTER;
    public static final ModConfigSpec.BooleanValue COMPASS_NAMES;
    public static final ModConfigSpec.BooleanValue COMPASS_DISTANCES;
    public static final ModConfigSpec.BooleanValue COMPASS_HEIGHT_ARROWS;
    public static final ModConfigSpec.BooleanValue COMPASS_EDGE_ARROWS;
    public static final ModConfigSpec.IntValue COMPASS_WIDTH;
    public static final ModConfigSpec.IntValue COMPASS_TOP_MARGIN;
    public static final ModConfigSpec.IntValue COMPASS_FOCUS_ANGLE;
    public static final ModConfigSpec.DoubleValue COMPASS_OPACITY;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> COMPASS_MARKER_OVERRIDES;

    static {
        var builder = new ModConfigSpec.Builder();
        var screens = ScreenCompatibilityDefaults.detected();
        CUSTOM_DEATH_SCREEN = builder.translation("cyberneticenhancements.configuration.customDeathScreen")
                .comment("Enabled by default regardless of installed menu mods. Explicit saved choices are preserved.")
                .define("customDeathScreen", screens.deathScreen());
        CUSTOM_LOADING_SCREEN = builder.translation("cyberneticenhancements.configuration.customLoadingScreen")
                .comment("Defaults off when FancyMenu or Drippy Loading Screen is installed. Explicit saved choices are preserved.")
                .define("customLoadingScreen", screens.loadingScreen());
        ANIMATED_DEATH_BODIES = builder.translation("cyberneticenhancements.configuration.animatedDeathBodies").define("animatedDeathBodies", true);
        CUSTOM_TITLE_BACKGROUND = builder.translation("cyberneticenhancements.configuration.customTitleBackground")
                .comment("Defaults off when FancyMenu, PackMenu or Custom Main Menu is installed. Explicit saved choices are preserved.")
                .define("customTitleBackground", screens.titlePresentation());
        CUSTOM_TITLE_SCREEN = builder.translation("cyberneticenhancements.configuration.customTitleScreen")
                .comment("Defaults off when FancyMenu, PackMenu or Custom Main Menu is installed. Explicit saved choices are preserved.")
                .define("customTitleScreen", screens.titlePresentation());
        SHOW_LAUNCH_INTRO = builder.translation("cyberneticenhancements.configuration.showLaunchIntro")
                .comment("Defaults off when FancyMenu, PackMenu or Custom Main Menu is installed. Explicit saved choices are preserved.")
                .define("showLaunchIntro", screens.titlePresentation());
        SHOW_JOIN_ANIMATION = builder.translation("cyberneticenhancements.configuration.showJoinAnimation")
                .comment("Play the neural-link animations when joining a world or server and when leaving through the Game Menu. Screen Effects scales their motion.")
                .define("showJoinAnimation", true);
        ENABLE_PLAYER_CALLS = builder.translation("cyberneticenhancements.configuration.enablePlayerCalls").define("enablePlayerCalls", true);
        SHOW_CYBERPSYCHOSIS_TIMER = builder.translation("cyberneticenhancements.configuration.showCyberpsychosisTimer")
                .comment("Show an anonymous cyberpsychosis episode timer at the top of the screen.")
                .define("showCyberpsychosisTimer", false);
        SHOW_COMPASS = builder
                .translation("cyberneticenhancements.configuration.showCompass")
                .define("showCompass", true);
        COMPASS_QUESTS = builder.translation("cyberneticenhancements.configuration.compassQuests").define("compassQuests", true);
        COMPASS_TURN_INS = builder.translation("cyberneticenhancements.configuration.compassTurnIns").define("compassTurnIns", true);
        COMPASS_BANNERS = builder.translation("cyberneticenhancements.configuration.compassBanners").define("compassBanners", true);
        COMPASS_CITIES = builder.translation("cyberneticenhancements.configuration.compassCities").define("compassCities", true);
        COMPASS_MAX_TAC = builder.translation("cyberneticenhancements.configuration.compassMaxTac").define("compassMaxTac", true);
        COMPASS_MEDTEC = builder.translation("cyberneticenhancements.configuration.compassMedtec").define("compassMedtec", true);
        COMPASS_CARDINALS = builder.translation("cyberneticenhancements.configuration.compassCardinals").define("compassCardinals", true);
        COMPASS_TICKS = builder.translation("cyberneticenhancements.configuration.compassTicks").define("compassTicks", true);
        COMPASS_CENTER = builder.translation("cyberneticenhancements.configuration.compassCenter").define("compassCenter", true);
        COMPASS_NAMES = builder.translation("cyberneticenhancements.configuration.compassNames").define("compassNames", true);
        COMPASS_DISTANCES = builder.translation("cyberneticenhancements.configuration.compassDistances").define("compassDistances", true);
        COMPASS_HEIGHT_ARROWS = builder.translation("cyberneticenhancements.configuration.compassHeightArrows").define("compassHeightArrows", true);
        COMPASS_EDGE_ARROWS = builder.translation("cyberneticenhancements.configuration.compassEdgeArrows").define("compassEdgeArrows", true);
        COMPASS_WIDTH = builder.translation("cyberneticenhancements.configuration.compassWidth").defineInRange("compassWidth", 192, 128, 320);
        COMPASS_TOP_MARGIN = builder.translation("cyberneticenhancements.configuration.compassTopMargin").defineInRange("compassTopMargin", 6, 0, 60);
        COMPASS_FOCUS_ANGLE = builder.translation("cyberneticenhancements.configuration.compassFocusAngle").defineInRange("compassFocusAngle", 10, 3, 30);
        COMPASS_OPACITY = builder.translation("cyberneticenhancements.configuration.compassOpacity").defineInRange("compassOpacity", 1.0, 0.4, 1.0);
        COMPASS_MARKER_OVERRIDES = builder.translation("cyberneticenhancements.configuration.compassMarkerOverrides")
                .defineListAllowEmpty("compassMarkerOverrides", java.util.List.of(), value -> value instanceof String);
        SHOW_DROPOFF_DELIVERY_MESSAGES = builder
                .translation("cyberneticenhancements.configuration.showDropoffDeliveryMessages")
                .define("showDropoffDeliveryMessages", true);
        CALL_WINDOW_ON_LEFT = builder
                .translation("cyberneticenhancements.configuration.callWindowOnLeft")
                .define("callWindowOnLeft", true);
        SHOW_ARCHIVE_BOOT_ANIMATION = builder
                .comment("Play the Neural Archive opening sequence. Disable to show the archive immediately with no input delay. Applies next time the archive opens.")
                .translation("cyberneticenhancements.configuration.showArchiveBootAnimation")
                .define("showArchiveBootAnimation", true);
        SHOW_BETA_WELCOME_MESSAGE = builder
                .comment("Show the local beta welcome message and its sound once per world or server login.")
                .translation("cyberneticenhancements.configuration.showBetaWelcomeMessage")
                .define("showBetaWelcomeMessage", true);
        CHECK_FOR_UPDATES = builder
                .comment("Check CurseForge release/beta metadata via CFWidget when the beta welcome message is shown. No downloads are installed.")
                .translation("cyberneticenhancements.configuration.checkForUpdates")
                .define("checkForUpdates", true);
        SHOW_CHANGELOG_POPUP = builder.translation("cyberneticenhancements.configuration.showChangelogPopup")
                .comment("Show the What's New popup on the title screen the first time a new release starts. The link at the bottom left of the title screen still opens it.")
                .define("showChangelogPopup", true);
        LAST_SEEN_CHANGELOG = builder
                .comment("The release whose changelog popup has been shown on the title screen. Managed by the mod; clear it to see the popup again.")
                .define("lastSeenChangelog", "");
        ARCHIVE_TAB_ORDER = builder
                .translation("cyberneticenhancements.configuration.archiveTabOrder")
                .defineList("archiveTabOrder", java.util.List.of("wiki", "schematics", "banking", "contacts", "compass", "map", "lore", "quests", "journal", "hud"),
                        value -> value instanceof String);
        HUD_VISIBILITY_OVERRIDES = builder
                .comment("Per-screen HUD visibility choices saved by the Neural Archive HUD tab.")
                .defineListAllowEmpty("hudVisibilityOverrides", java.util.List.of(), value -> value instanceof String);
        FABRICATOR_PINNED_DESIGNS = builder.translation("cyberneticenhancements.configuration.fabricatorPinnedDesigns")
                .comment("Design IDs pinned to the top of the Fabricator list; managed by its pin controls.")
                .defineListAllowEmpty("fabricatorPinnedDesigns", java.util.List.of(),
                        value -> value instanceof String id && net.minecraft.resources.ResourceLocation.tryParse(id) != null);
        WIKI_PINNED_ENTRIES = builder.translation("cyberneticenhancements.configuration.wikiPinnedEntries")
                .comment("Wiki entry IDs pinned through the archive's list controls.")
                .defineListAllowEmpty("wikiPinnedEntries", java.util.List.of(), value -> value instanceof String);
        CUSTOM_COMPASS_MARKERS = builder.translation("cyberneticenhancements.configuration.customCompassMarkers")
                .comment("Personal markers saved per world/server and player. Manage them in the Compass tab.")
                .defineListAllowEmpty("customCompassMarkers",java.util.List.of(),value->value instanceof String);
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
