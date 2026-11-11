package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareBalance;
import java.util.List;
import java.util.Map;

public record SandevistanProfile(String id, double timeScale, double movement, double attackSpeed,
                                 double activationStrain, double runningStrain, int wear, double radius) {
    public static final String EXPERIMENTAL = "militech_experimental_sandevistan";
    public static final List<SandevistanProfile> DEFAULTS = List.of(
            new SandevistanProfile("dynalar_sandevistan", 0.55, 0.60, 0.35, 6, 0.5, 1, 12),
            new SandevistanProfile("zetatech_sandevistan", 0.45, 0.75, 0.45, 8, 0.7, 1, 14),
            new SandevistanProfile("militech_falcon", 0.30, 1.10, 0.65, 11, 1.0, 2, 16),
            new SandevistanProfile("qiant_warp_dancer", 0.20, 1.25, 0.80, 13, 1.2, 2, 16),
            new SandevistanProfile("militech_apogee", 0.15, 1.60, 1.00, 16, 1.5, 3, 18),
            new SandevistanProfile(EXPERIMENTAL, 0.08, 2.20, 1.30, 24, 3.5, 5, 20));

    public static boolean supports(String id) {
        return DEFAULTS.stream().anyMatch(profile -> profile.id.equals(id));
    }

    public static SandevistanProfile configured(String id) {
        SandevistanProfile base = DEFAULTS.stream().filter(profile -> profile.id.equals(id)).findFirst().orElseThrow();
        String key = "sandevistan." + id + ".";
        return new SandevistanProfile(id,
                bounded(CyberwareBalance.doubleValue(key + "time_scale"), 0.05, 1, base.timeScale),
                bounded(CyberwareBalance.doubleValue(key + "movement"), 0, 3, base.movement),
                bounded(CyberwareBalance.doubleValue(key + "attack_speed"), 0, 2, base.attackSpeed),
                bounded(CyberwareBalance.doubleValue(key + "activation_strain"), 0, 50, base.activationStrain),
                bounded(CyberwareBalance.doubleValue(key + "running_strain"), 0, 10, base.runningStrain),
                (int) bounded(CyberwareBalance.doubleValue(key + "wear"), 0, 30, base.wear),
                bounded(CyberwareBalance.doubleValue(key + "radius"), 4, 24, base.radius));
    }

    public static void addBalanceDefaults(Map<String, Double> values) {
        for (SandevistanProfile profile : DEFAULTS) {
            String key = "sandevistan." + profile.id + ".";
            values.put(key + "time_scale", profile.timeScale);
            values.put(key + "movement", profile.movement);
            values.put(key + "attack_speed", profile.attackSpeed);
            values.put(key + "activation_strain", profile.activationStrain);
            values.put(key + "running_strain", profile.runningStrain);
            values.put(key + "wear", (double) profile.wear);
            values.put(key + "radius", profile.radius);
        }
        values.put("sandevistan.recovery_window_seconds", 120.0);
        values.put("sandevistan.pvp_damage_multiplier", 0.8);
    }

    public static double bounded(double value, double minimum, double maximum, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, minimum, maximum) : fallback;
    }
}
