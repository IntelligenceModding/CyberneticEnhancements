package de.artemis.cyberneticenhancements.client.ui;

import java.util.List;
import java.util.Optional;

/** Places the launcher in a free edge margin, never over a machine or another widget. */
public final class ArchiveButtonPlacement {
    private ArchiveButtonPlacement() {}
    public record Bounds(int x, int y, int width, int height) {
        public boolean overlaps(Bounds other, int gap) {
            return x < other.x + other.width + gap && x + width + gap > other.x
                    && y < other.y + other.height + gap && y + height + gap > other.y;
        }
    }
    public static Optional<Bounds> find(int screenWidth, int screenHeight, int width, int height, List<Bounds> occupied) {
        int margin = 8, right = screenWidth - margin - width, bottom = screenHeight - margin - height;
        if (right < margin || bottom < margin) return Optional.empty();
        for (int x = margin; x <= right; x++) {
            var bounds = new Bounds(x, bottom, width, height);
            if (occupied.stream().noneMatch(other -> bounds.overlaps(other, 4))) return Optional.of(bounds);
        }
        for (int y = bottom - 1; y >= margin; y--) {
            var bounds = new Bounds(margin, y, width, height);
            if (occupied.stream().noneMatch(other -> bounds.overlaps(other, 4))) return Optional.of(bounds);
        }
        return Optional.empty();
    }
}
