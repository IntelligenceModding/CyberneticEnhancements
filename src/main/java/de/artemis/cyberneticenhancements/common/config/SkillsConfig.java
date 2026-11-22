package de.artemis.cyberneticenhancements.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings of the skill system. */
public final class SkillsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue MAX_RANK;
    public static final ModConfigSpec.BooleanValue RESPEC_ENABLED;
    public static final ModConfigSpec.IntValue RESPEC_COST;

    static {
        var builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Whether players can spend points, respec, receive bonuses, and see skill dialogue. Turning this off preserves saved ranks.")
                .define("skillsEnabled", true);
        MAX_RANK = builder.comment("The highest rank one skill can reach. Bonuses are still scaled against rank 10, so a lower cap also lowers the top bonus.")
                .defineInRange("maxRank", 10, 1, 10);
        RESPEC_ENABLED = builder.comment("Whether a player can take back their spent ranks and spend the points again.")
                .define("respecEnabled", true);
        RESPEC_COST = builder.comment("Eurodollars a respec costs for every rank that is spent. 0 makes it free.")
                .defineInRange("respecBaseCost", 25, 0, 100_000);
        SPEC = builder.build();
    }

    private SkillsConfig() {}

    public static boolean enabled() { return !SPEC.isLoaded() || ENABLED.get(); }
    public static int maxRank() { return SPEC.isLoaded() ? MAX_RANK.get() : MAX_RANK.getDefault(); }
    public static boolean respecEnabled() { return !SPEC.isLoaded() || RESPEC_ENABLED.get(); }
    public static int respecBaseCost() { return SPEC.isLoaded() ? RESPEC_COST.get() : RESPEC_COST.getDefault(); }
}
