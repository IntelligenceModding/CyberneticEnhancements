package de.artemis.cyberneticenhancements.common.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DeferredWorkQueue<K, V> {
    private final Map<K, List<V>> pending = new HashMap<>();

    synchronized void add(K key, V value) {
        pending.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
    }

    synchronized List<V> takeBatch(K key) {
        List<V> batch = pending.remove(key);
        return batch == null ? List.of() : batch;
    }

    synchronized List<V> takeBatch(K key, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Batch limit must be positive");
        List<V> values = pending.get(key);
        if (values == null) return List.of();
        if (values.size() <= limit) return takeBatch(key);
        List<V> batch = new ArrayList<>(values.subList(0, limit));
        values.subList(0, limit).clear();
        return batch;
    }

    synchronized void defer(K key, List<V> values) {
        if (!values.isEmpty()) pending.computeIfAbsent(key, ignored -> new ArrayList<>()).addAll(values);
    }

    synchronized void clear(K key) {
        pending.remove(key);
    }
}
