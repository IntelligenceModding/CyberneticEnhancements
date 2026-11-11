package de.artemis.cyberneticenhancements.client.jei;

import java.util.*;

 final class RecipeVisibilityTracker<K, V> {
    record Changes<V>(List<V> hide, List<V> add, List<V> show) {}
    private final Map<K, V> registered = new HashMap<>();
    private Map<K, V> visible = Map.of();

    Changes<V> update(Map<K, V> allowed) {
        List<V> hide = new ArrayList<>(), add = new ArrayList<>(), show = new ArrayList<>();
        visible.forEach((id, value) -> {
            if (!Objects.equals(value, allowed.get(id))) hide.add(value);
        });
        allowed.forEach((id, value) -> {
            if (!Objects.equals(value, registered.get(id))) add.add(value);
            else if (!Objects.equals(value, visible.get(id))) show.add(value);
        });
        registered.putAll(allowed);
        visible = Map.copyOf(allowed);
        return new Changes<>(List.copyOf(hide), List.copyOf(add), List.copyOf(show));
    }

    void clear() {
        registered.clear();
        visible = Map.of();
    }
}