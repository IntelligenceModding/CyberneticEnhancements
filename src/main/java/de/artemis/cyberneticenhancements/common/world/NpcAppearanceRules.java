package de.artemis.cyberneticenhancements.common.world;

public final class NpcAppearanceRules {
    private NpcAppearanceRules() {
    }

    public static boolean reserved(String appearance) {
        return appearance != null && (appearance.equals("max_tac.png") || appearance.endsWith("/max_tac.png") || appearance.equals("trauma_team.png") || appearance.endsWith("/trauma_team.png"));
    }
}
