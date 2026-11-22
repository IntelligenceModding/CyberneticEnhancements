package de.artemis.cyberneticenhancements.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings of the fast travel terminals. */
public final class FastTravelConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue COST;

    static {
        var builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Whether fast travel terminals can be used at all. When off they still generate and can be placed, but do nothing.")
                .define("fastTravelEnabled", true);
        COST = builder.comment("What one fast travel costs, in eurodollars. 0 makes it free.")
                .defineInRange("fastTravelCost", 100, 0, 1_000_000);
        SPEC = builder.build();
    }

    private FastTravelConfig() {}

    public static boolean enabled() { return !SPEC.isLoaded() || ENABLED.get(); }

    /** The cost now, or the default while the config is not loaded yet. */
    public static int cost() { return SPEC.isLoaded() ? COST.get() : COST.getDefault(); }
}
