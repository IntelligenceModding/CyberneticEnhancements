package de.artemis.cyberneticenhancements.client.ui;

/**
 * The join animation as a pure function of time. The center line splits to open onto the world and HUD.
 */
public final class JoinAnimationTimeline {
    public static final long DURATION = 1600;
    static final long LINE_END = 260, OPEN_END = 900, GLITCH_END = 1000;

    /**
     * @param lineWidth how far the opening line has grown across the screen, 0..1
     * @param open      how much of the view has opened around it, 0..1 (0 = only the line, 1 = fully open)
     * @param fade      the backdrop opacity when Screen Effects is off, 1..0
     * @param glitch    strength of the tearing bands, 0..1
     * @param vignette  darkness of the screen edges, 1..0
     */
    public record Frame(float lineWidth, float open, float fade, float glitch, float vignette) {}

    private JoinAnimationTimeline() {}

    /** {@code strength} is the player's Screen Effects setting: at 0 only a gentle fade remains, otherwise it scales the tearing. */
    public static Frame at(long millis, double strength) {
        float t = Math.clamp(millis, 0, DURATION);
        float vignette = 1 - easeOut(window(t, 0, DURATION));
        if (strength <= 0) return new Frame(1, 1, 1 - easeInOut(window(t, 0, 650)), 0, vignette * 0.6F);
        float amount = (float) Math.min(1, strength);
        float glitch = t < LINE_END || t >= GLITCH_END ? 0 : 0.7F * amount * (float) Math.pow(1 - window(t, LINE_END, GLITCH_END), 1.5);
        return new Frame(easeOut(window(t, 0, LINE_END)), easeInOut(window(t, LINE_END, OPEN_END)), 0, glitch, vignette);
    }

    private static float window(float t, float from, float to) { return Math.clamp((t - from) / (to - from), 0, 1); }

    private static float easeOut(float x) { return 1 - (1 - x) * (1 - x) * (1 - x); }

    private static float easeInOut(float x) { return x * x * (3 - 2 * x); }
}
