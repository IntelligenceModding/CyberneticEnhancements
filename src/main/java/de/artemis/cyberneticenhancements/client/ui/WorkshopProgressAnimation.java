package de.artemis.cyberneticenhancements.client.ui;

/** Smooths server ticks without predicting more than one tick beyond confirmed progress. */
public final class WorkshopProgressAnimation {
    public record Frame(float progress, float completion, float labelOpacity, boolean completed) {}
    private String recipe = "";
    private long previousMillis = -1, updatedAt, awaitingOutputUntil = -1, completedUntil = -1;
    private int previousTicks = -1, outputBaseline;
    private boolean wasWorking;
    private float progress, completion, labelOpacity;

    public Frame sample(long now, String recipeId, int ticks, int duration, boolean working, int outputCount) {
        if (!recipe.equals(recipeId)) {
            recipe = recipeId;
            previousMillis = -1;
            previousTicks = -1;
            awaitingOutputUntil = completedUntil = -1;
            wasWorking = false;
            progress = completion = labelOpacity = 0;
        }
        double seconds = previousMillis < 0 ? 0 : Math.clamp((now - previousMillis) / 1000D, 0, .1);
        previousMillis = now;
        if (working && !wasWorking) {
            outputBaseline = outputCount;
            awaitingOutputUntil = completedUntil = -1;
            progress = Math.clamp(ticks / (float) Math.max(1, duration), 0, 1);
        }
        // Taking an earlier batch out while the next one runs must not hide its completion.
        if (working && outputCount < outputBaseline) outputBaseline = outputCount;
        if (!working && wasWorking) awaitingOutputUntil = now + 200;
        if (!working && awaitingOutputUntil >= now && outputCount > outputBaseline) {
            completedUntil = now + 550;
            awaitingOutputUntil = -1;
        }
        if (previousTicks != ticks) { previousTicks = ticks; updatedAt = now; }
        boolean completed = completedUntil >= now;
        float partialTick = Math.clamp((now - updatedAt) / 50F, 0, 1);
        float target = working ? Math.min(.995F, (ticks + partialTick) / Math.max(1, duration)) : completed ? 1 : 0;
        progress += (target - progress) * (float) (1 - Math.exp(-seconds / .075));
        completion += ((completed ? 1 : 0) - completion) * (float) (1 - Math.exp(-seconds / .12));
        labelOpacity += ((working || completed ? 1 : 0) - labelOpacity) * (float) (1 - Math.exp(-seconds / .1));
        wasWorking = working;
        return new Frame(progress, completion, labelOpacity, completed);
    }
}
