package de.artemis.cyberneticenhancements.client.ui;

/** Presentation time only: never represents or delays resource loading. */
public final class LaunchIntroTimeline {
    public enum Phase { MOJANG, INTELLIGENCE, NEOFORGE, MINECRAFT, DISCLAIMER, THANK_YOU, BETA_NOTICE, ENTERING, FINISHED }
    public static final long MOJANG_END_MILLIS = 3_260;
    public static final long INTELLIGENCE_END_MILLIS = 6_180;
    public static final long NEOFORGE_END_MILLIS = 9_180;
    public static final long MINECRAFT_END_MILLIS = 12_220;
    public static final long THANK_YOU_START_MILLIS = 15_100;
    public static final long BETA_START_MILLIS = 18_000;
    // Keep the complete 20.636-second startup clip, including its final tail.
    public static final long DISCLAIMER_MILLIS = 8_420;
    public static final long PRESENTATION_MILLIS = MINECRAFT_END_MILLIS + DISCLAIMER_MILLIS;
    public static final long MENU_CUT_MILLIS = 220;
    public static final long ENTER_MILLIS = MENU_CUT_MILLIS * 2;
    private long lastTime = -1, elapsed, exitElapsed;
    private boolean entering;

    public void advance(long now, boolean focused) {
        advance(now, focused, -1);
    }

    /** A nonnegative position follows the actual audio source; -1 uses the silent fallback clock. */
    public void advance(long now, boolean focused, long audioMillis) {
        if (lastTime < 0) { lastTime = now; return; }
        long delta = Math.clamp(now - lastTime, 0, 250);
        lastTime = now;
        if (!focused) return;
        if (entering) exitElapsed = Math.min(ENTER_MILLIS, exitElapsed + delta);
        else if (audioMillis >= 0 && elapsed < PRESENTATION_MILLIS)
            elapsed = Math.max(elapsed, Math.min(audioMillis, PRESENTATION_MILLIS));
        else elapsed = Math.min(PRESENTATION_MILLIS, elapsed + delta);
        if (!entering && elapsed >= PRESENTATION_MILLIS) entering = true;
    }

    public Phase phase() {
        if (entering) return exitElapsed >= ENTER_MILLIS ? Phase.FINISHED : Phase.ENTERING;
        if (elapsed < MOJANG_END_MILLIS) return Phase.MOJANG;
        if (elapsed < INTELLIGENCE_END_MILLIS) return Phase.INTELLIGENCE;
        if (elapsed < NEOFORGE_END_MILLIS) return Phase.NEOFORGE;
        if (elapsed < MINECRAFT_END_MILLIS) return Phase.MINECRAFT;
        if (elapsed < THANK_YOU_START_MILLIS) return Phase.DISCLAIMER;
        return elapsed < BETA_START_MILLIS ? Phase.THANK_YOU : Phase.BETA_NOTICE;
    }

    public long phaseMillis() {
        return switch (phase()) {
            case MOJANG -> elapsed;
            case INTELLIGENCE -> elapsed - MOJANG_END_MILLIS;
            case NEOFORGE -> elapsed - INTELLIGENCE_END_MILLIS;
            case MINECRAFT -> elapsed - NEOFORGE_END_MILLIS;
            case DISCLAIMER -> elapsed - MINECRAFT_END_MILLIS;
            case THANK_YOU -> elapsed - THANK_YOU_START_MILLIS;
            case BETA_NOTICE -> elapsed - BETA_START_MILLIS;
            case ENTERING, FINISHED -> exitElapsed;
        };
    }

    public boolean skipPresentation() {
        if (entering) return false;
        entering = true;
        return true;
    }

    public boolean showingLogo() { return phase().ordinal() < Phase.DISCLAIMER.ordinal(); }

    public float logoOpacity() {
        if (!showingLogo()) return 0;
        return Math.clamp((phaseMillis() - 25) / 80.0F, 0, 1)
                * Math.clamp((logoDuration() - 25 - phaseMillis()) / 80.0F, 0, 1);
    }

    public float logoGlitch() {
        if (!showingLogo()) return 0;
        long age = phaseMillis();
        if (age < 220) return 1 - age / 220.0F;
        if (age >= logoDuration() - 220)
            return Math.clamp((age - (logoDuration() - 220)) / 195.0F, 0, 1);
        return 0;
    }

    private long logoDuration() {
        return switch (phase()) {
            case MOJANG -> MOJANG_END_MILLIS;
            case INTELLIGENCE -> INTELLIGENCE_END_MILLIS - MOJANG_END_MILLIS;
            case NEOFORGE -> NEOFORGE_END_MILLIS - INTELLIGENCE_END_MILLIS;
            case MINECRAFT -> MINECRAFT_END_MILLIS - NEOFORGE_END_MILLIS;
            default -> 0;
        };
    }

    public float exitProgress() {
        float progress = Math.clamp(exitElapsed / (float) ENTER_MILLIS, 0, 1);
        return progress * progress * (3 - 2 * progress);
    }

    public boolean menuVisible() { return exitElapsed >= MENU_CUT_MILLIS; }

    /** Match the logo handoffs: tear out, a brief black separator, then tear in. */
    public float menuOpacity() {
        if (!menuVisible()) return Math.clamp((MENU_CUT_MILLIS - 25 - exitElapsed) / 80.0F, 0, 1);
        return Math.clamp((exitElapsed - MENU_CUT_MILLIS - 25) / 80.0F, 0, 1);
    }

    public float menuGlitch() {
        if (phase() != Phase.ENTERING) return 0;
        if (!menuVisible()) return Math.clamp(exitElapsed / 195.0F, 0, 1);
        return Math.clamp(1 - (exitElapsed - MENU_CUT_MILLIS) / 220.0F, 0, 1);
    }
}
