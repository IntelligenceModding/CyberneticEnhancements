package de.artemis.cyberneticenhancements.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_BETA_WELCOME_MESSAGE;
    public static final ModConfigSpec.BooleanValue CHECK_FOR_UPDATES;
    public static final ModConfigSpec.BooleanValue SHOW_ARCHIVE_BOOT_ANIMATION;

    static {
        var builder = new ModConfigSpec.Builder();
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
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
