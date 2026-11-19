package de.artemis.cyberneticenhancements.client.ui;

public final class CompassHeading {
    private CompassHeading() {
    }

    public static double bearingTo(double deltaX, double deltaZ) {
        return Math.toDegrees(Math.atan2(deltaX, -deltaZ));
    }

    public static double offsetDegrees(double playerYaw, double bearing) {
        double offset = (bearing - playerYaw - 180) % 360;
        if (offset >= 180) offset -= 360;
        if (offset < -180) offset += 360;
        return offset;
    }
}
