package de.artemis.cyberneticenhancements.common.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The server's list of {@code key=value} lines, split into settings and readable problems. Never throws, so one typo cannot cost the rest. */
public final class EnforcedEntries {
    public record Parsed(Map<String, String> entries, List<String> problems) {}

    private EnforcedEntries() {}

    public static Parsed parse(Collection<? extends String> lines) {
        var entries = new LinkedHashMap<String, String>();
        var problems = new ArrayList<String>();
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            int split = line.indexOf('=');
            String key = split < 0 ? "" : line.substring(0, split).trim(), value = split < 0 ? "" : line.substring(split + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                problems.add("Expected key=value but found \"" + line + "\"");
                continue;
            }
            if (entries.put(key, value) != null) problems.add("\"" + key + "\" is listed more than once; the last value is used");
        }
        return new Parsed(Collections.unmodifiableMap(entries), List.copyOf(problems));
    }
}
