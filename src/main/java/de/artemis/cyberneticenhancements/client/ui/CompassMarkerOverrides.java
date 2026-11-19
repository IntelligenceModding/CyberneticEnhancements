package de.artemis.cyberneticenhancements.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompassMarkerOverrides {
    private static final Entry DEFAULT = new Entry(false, "", null);
    private final Map<Key, Entry> entries = new LinkedHashMap<>();

    public static CompassMarkerOverrides parse(List<? extends String> values) {
        var result = new CompassMarkerOverrides();
        for (String value : values) {
            try {
                var json = JsonParser.parseString(value).getAsJsonObject();
                String scope = json.get("scope").getAsString(), id = json.get("id").getAsString();
                if (scope.isBlank() || id.isBlank()) continue;
                result.set(scope, id, new Entry(json.has("hidden") && json.get("hidden").getAsBoolean(),
                        json.has("label") ? json.get("label").getAsString() : "",
                        json.has("color") ? 0xFF000000 | json.get("color").getAsInt() & 0xFFFFFF : null,
                        json.has("deleted") && json.get("deleted").getAsBoolean()));
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    public Entry get(String scope, String id) {
        return entries.getOrDefault(new Key(scope, id), DEFAULT);
    }

    public void set(String scope, String id, Entry entry) {
        String label = entry.label().replaceAll("\\p{Cntrl}", "").strip();
        label = label.substring(0, Math.min(label.length(), 80));
        var normalized = entry.deleted() ? new Entry(false, "", null, true)
                : new Entry(entry.hidden(), label, entry.color() == null ? null : 0xFF000000 | entry.color() & 0xFFFFFF);
        var key = new Key(scope, id);
        if (normalized.equals(DEFAULT)) entries.remove(key);
        else entries.put(key, normalized);
    }

    public void clear(String scope) {
        entries.entrySet().removeIf(entry -> entry.getKey().scope.equals(scope) && !entry.getValue().deleted());
    }

    public List<String> serialize() {
        var result = new ArrayList<String>();
        entries.forEach((key, entry) -> {
            var json = new JsonObject();
            json.addProperty("scope", key.scope);
            json.addProperty("id", key.id);
            if (entry.hidden()) json.addProperty("hidden", true);
            if (entry.deleted()) json.addProperty("deleted", true);
            if (!entry.label().isEmpty()) json.addProperty("label", entry.label());
            if (entry.color() != null) json.addProperty("color", entry.color());
            result.add(json.toString());
        });
        return List.copyOf(result);
    }

    public record Entry(boolean hidden, String label, Integer color, boolean deleted) {
        public Entry(boolean hidden, String label, Integer color) { this(hidden, label, color, false); }
    }

    private record Key(String scope, String id) {
    }
}
