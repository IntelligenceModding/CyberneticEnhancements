package de.artemis.cyberneticenhancements.common.config;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A server owner's list of client settings every connecting player must use. It is a SERVER config, so NeoForge already sends the file to
 * each player while they connect; the client applies it for exactly as long as they stay connected (see the client's EnforcedSettings).
 */
public final class EnforcedSettingsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTRIES;

    static {
        var builder = new ModConfigSpec.Builder();
        ENTRIES = builder
                .comment("Client settings that every player must use while connected to this server, written as \"key=value\".",
                        "Example: [\"customDeathScreen=true\", \"showJoinAnimation=false\", \"compassWidth=224\"]",
                        "Any key of config/cyberneticenhancements-client.toml works, plus sandevistan.afterimages, sandevistan.edge_effects,",
                        "sandevistan.restrained_fov, sandevistan.flash_intensity, sandevistan.sound_volume and hud.enabled.",
                        "Settings that are lists (pinned designs, markers, tab order) are personal and cannot be enforced.",
                        "Enforced settings are locked on the player's config screen. They apply when a player joins and are never written to their files.",
                        "An invalid line is skipped and reported in the log; the others still apply.")
                .defineListAllowEmpty("enforcedClientSettings", List.of(), value -> value instanceof String);
        SPEC = builder.build();
    }

    private EnforcedSettingsConfig() {}

    /** Reports what is being enforced, and every line that is not valid, whenever the file is loaded or edited. */
    public static void onConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        var logger = LogUtils.getLogger();
        var parsed = EnforcedEntries.parse(ENTRIES.get());
        var problems = new ArrayList<>(parsed.problems());
        try {
            problems.addAll(de.artemis.cyberneticenhancements.client.EnforcedSettings.validate(parsed.entries()));
        } catch (LinkageError | RuntimeException unavailable) {
            // Only the keys' shape could be checked here; players' clients validate the rest.
        }
        if (!parsed.entries().isEmpty()) logger.info("Enforcing {} client settings on connecting players: {}", parsed.entries().size(), parsed.entries());
        problems.forEach(problem -> logger.warn("Enforced client settings: {}", problem));
    }
}
