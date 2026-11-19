package de.artemis.cyberneticenhancements.client.render;

import java.util.HashMap;
import java.util.Map;

/** Per-marker transitions continue after the notification itself is cleared. */
final class MarkerVisibility {
    static final double DURATION_TICKS = 8;
    private static final double STALE_TICKS = 40;
    private static final class State {
        float progress;
        double sampledAt;
        State(double time) { sampledAt = time; }
    }
    private final Map<Object, State> states = new HashMap<>();

    float sample(Object id, boolean wanted, double time) {
        State state = states.get(id);
        if (state == null) {
            if (!wanted) return 0;
            state = new State(time);
            states.put(id, state);
        }
        double elapsed = Math.max(0, time - state.sampledAt);
        state.progress = (float) Math.clamp(state.progress + (wanted ? elapsed : -elapsed) / DURATION_TICKS, 0, 1);
        state.sampledAt = time;
        if (!wanted && state.progress == 0) states.remove(id);
        return state.progress;
    }

    void prune(double time) { states.values().removeIf(state -> time < state.sampledAt || time - state.sampledAt > STALE_TICKS); }
    void forget(Object id) { states.remove(id); }
    void clear() { states.clear(); }

    static float opacity(float progress) { return progress * progress * (3 - 2 * progress); }
    static float rowOpacity(float progress, int row) {
        return opacity(Math.clamp(progress * HologramPixels.MARKER_ROWS - row, 0, 1));
    }
}
