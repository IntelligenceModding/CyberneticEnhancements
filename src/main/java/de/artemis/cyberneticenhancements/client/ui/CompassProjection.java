package de.artemis.cyberneticenhancements.client.ui;

import java.util.Comparator;
import java.util.List;

public final class CompassProjection {
    private CompassProjection() {
    }

    public static List<Marker> project(List<CompassLandmark> targets, String dimension, double x, double y, double z, double yaw) {
        return targets.stream().filter(target -> target.dimension().equals(dimension)).map(target -> {
            double dx = target.pos().getX() + 0.5 - x, dy = target.pos().getY() + 0.5 - y, dz = target.pos().getZ() + 0.5 - z;
            double angle = dx * dx + dz * dz < 0.25 ? 0 : CompassHeading.offsetDegrees(yaw, CompassHeading.bearingTo(dx, dz));
            return new Marker(target, angle, (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)),
                    Math.abs(dy) > 8 ? (int) Math.round(dy) : 0);
        }).sorted(Comparator.comparingDouble((Marker marker) -> Math.abs(marker.angle()))
                .thenComparingInt(Marker::distance).thenComparing(marker -> marker.target().id())).toList();
    }

    public record Marker(CompassLandmark target, double angle, int distance, int verticalDistance) {
    }
}
