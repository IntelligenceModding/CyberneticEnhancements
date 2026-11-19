package de.artemis.cyberneticenhancements.client.render;

import com.mojang.logging.LogUtils;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class NpcTextureCatalog<T> {
    private final Map<String, List<T>> textures = new HashMap<>();

    List<T> get(String prefix, Supplier<List<T>> loader) {
        return textures.computeIfAbsent(prefix, key -> {
            try {
                return List.copyOf(loader.get());
            } catch (UncheckedIOException exception) {
                LogUtils.getLogger().warn("Could not enumerate NPC skins in {}; using available fallback skins until resources reload", prefix, exception);
                return List.of();
            }
        });
    }

    void clear() {
        textures.clear();
    }
}