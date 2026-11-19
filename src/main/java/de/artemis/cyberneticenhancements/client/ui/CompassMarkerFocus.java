package de.artemis.cyberneticenhancements.client.ui;

import java.util.List;

public final class CompassMarkerFocus {
    private CompassMarkerFocus() {}

    public static boolean isQuest(CompassLandmark target) {
        return target.kind() == CompassLandmark.Kind.QUEST || target.kind() == CompassLandmark.Kind.TURN_IN;
    }

    public static int captionAlpha(CompassProjection.Marker marker, int focusAngle) {
        double alignment = Math.abs(marker.angle());
        if (alignment >= focusAngle) return 0;
        return (int) Math.round(Math.clamp((focusAngle - alignment) / Math.max(1, focusAngle * .4), 0, 1) * 235);
    }

    public static CompassProjection.Marker focused(List<CompassProjection.Marker> markers, int focusAngle) {
        return markers.isEmpty() || captionAlpha(markers.getFirst(), focusAngle) < 8 ? null : markers.getFirst();
    }

    public static int x(CompassProjection.Marker marker, int center, int width) {
        return center + (int) Math.round(Math.clamp(marker.angle() * width / 180.0, -width / 2.0 + 5, width / 2.0 - 5));
    }

    public static CompassProjection.Marker hovered(List<CompassProjection.Marker> markers, int center, int top, int width,
                                                   double mouseX, double mouseY, boolean edgeArrows) {
        CompassProjection.Marker best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (var marker : markers) {
            if (Math.abs(marker.angle()) > 85 && !edgeArrows) continue;
            double dx = mouseX - x(marker, center, width), dy = mouseY - (top + 13);
            if (Math.abs(dx) > 6 || Math.abs(dy) > 7) continue;
            double distance = dx * dx + dy * dy;
            if (distance < bestDistance) { best = marker; bestDistance = distance; }
        }
        return best != null && isQuest(best.target()) ? best : null;
    }
}
