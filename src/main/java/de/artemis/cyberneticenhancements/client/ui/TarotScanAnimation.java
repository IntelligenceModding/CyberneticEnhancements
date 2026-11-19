package de.artemis.cyberneticenhancements.client.ui;

/** Frame-time animation, independent of packet timing and rendering frame rate. */
public final class TarotScanAnimation {
    private long previousMillis = -1, completedAt = -1;
    private float opacity, progress, completion;

    public void update(long now, boolean visible, float targetProgress, boolean completed) {
        double seconds = previousMillis < 0 ? 0 : Math.clamp((now - previousMillis) / 1000D, 0, .1);
        previousMillis = now;
        if (completed && completedAt < 0) completedAt = now;
        boolean holdingCompletion = completedAt >= 0 && now - completedAt < 450;
        float desiredOpacity = completedAt >= 0 ? holdingCompletion ? 1 : 0 : visible ? 1 : 0;
        opacity += (desiredOpacity - opacity) * (float) (1 - Math.exp(-seconds / .09));
        float desiredProgress = completedAt >= 0 ? 1 : Math.clamp(targetProgress, 0, 1);
        progress += (desiredProgress - progress) * (float) (1 - Math.exp(-seconds / .065));
        completion += ((completedAt >= 0 ? 1 : 0) - completion) * (float) (1 - Math.exp(-seconds / .12));
    }

    public float opacity() { return opacity; }
    public float progress() { return progress; }
    public float completion() { return completion; }
    public boolean completed() { return completedAt >= 0; }
    public void reset() { previousMillis = -1; completedAt = -1; opacity = progress = completion = 0; }
}
