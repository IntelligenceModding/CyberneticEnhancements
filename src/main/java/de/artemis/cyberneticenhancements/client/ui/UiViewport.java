package de.artemis.cyberneticenhancements.client.ui;

public record UiViewport(int width, int height, double renderScale) {
    public static UiViewport fit(int pixelWidth, int pixelHeight, double guiScale, int referenceWidth, int referenceHeight) {
        double physicalScale = Math.min(Math.max(1, pixelWidth) / (double) Math.max(1, referenceWidth),
                Math.max(1, pixelHeight) / (double) Math.max(1, referenceHeight));
        return new UiViewport(Math.max(1, (int) Math.floor(pixelWidth / physicalScale)),
                Math.max(1, (int) Math.floor(pixelHeight / physicalScale)), physicalScale / Math.max(0.01, guiScale));
    }

    public double toUi(double coordinate) {
        return coordinate / renderScale;
    }

    public int toScreenFloor(int coordinate) {
        return (int) Math.floor(coordinate * renderScale);
    }

    public int toScreenCeil(int coordinate) {
        return (int) Math.ceil(coordinate * renderScale);
    }
}
