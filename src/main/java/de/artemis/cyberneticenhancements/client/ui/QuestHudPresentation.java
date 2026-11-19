package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.common.network.QuestProgressPayload.Objective;
import java.util.List;
import java.util.function.Predicate;

public final class QuestHudPresentation {
    public static final int PAGE_SIZE = 3;
    private static final double FADE_SECONDS = .18;
    private List<Objective> shown = List.of();
    private float opacity;
    private double previousTime = Double.NaN;
    private int page;

    private static int pageCount(int count) { return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE); }

    public void nextPage(int count) {
        int pages = pageCount(count);
        page = (Math.min(page, pages - 1) + 1) % pages;
    }

    public Frame sample(List<Objective> objectives, double seconds) {
        return sample(objectives, seconds, id -> false);
    }

    public Frame sample(List<Objective> objectives, double seconds, Predicate<String> hidden) {
        double delta = Double.isNaN(previousTime) ? 0 : Math.clamp(seconds - previousTime, 0, .25);
        previousTime = seconds;
        var updated = new java.util.LinkedHashMap<String, Objective>();
        for (var objective : objectives) {
            if (!hidden.test(objective.id())) updated.put(objective.id(), objective);
        }
        boolean hasVisibleObjectives = !updated.isEmpty();
        if (hasVisibleObjectives) {
            var stable = new java.util.ArrayList<Objective>();
            for (var previous : shown) {
                var current = updated.remove(previous.id());
                if (current != null) stable.add(current);
            }
            stable.addAll(updated.values());
            shown = List.copyOf(stable);
        } else {
            shown = shown.stream().filter(objective -> !hidden.test(objective.id())).toList();
        }
        opacity = (float) Math.clamp(opacity + (hasVisibleObjectives ? delta : -delta) / FADE_SECONDS, 0, 1);
        if (opacity == 0 && !hasVisibleObjectives) shown = List.of();
        int pages = pageCount(shown.size());
        page = Math.min(page, pages - 1);
        int start = page * PAGE_SIZE;
        return new Frame(shown.subList(start, Math.min(start + PAGE_SIZE, shown.size())), opacity, page, pages);
    }

    public void clear() { shown = List.of(); opacity = 0; previousTime = Double.NaN; page = 0; }
    public record Frame(List<Objective> objectives, float opacity, int page, int pages) {
        public boolean visible() { return !objectives.isEmpty() && opacity > 0; }
    }
}
