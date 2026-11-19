package de.artemis.cyberneticenhancements.client.ui;

public final class LaunchIntroGate {
    private boolean consumed;

    public void gameplayStarted() { consumed = true; }

    public boolean claim(boolean enabled, boolean inWorld, boolean vanillaTitle) {
        if (inWorld) consumed = true;
        if (consumed || !enabled || !vanillaTitle) return false;
        consumed = true;
        return true;
    }
}
