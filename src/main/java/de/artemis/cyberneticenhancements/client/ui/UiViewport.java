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

    /** Cover the entire native GUI, including its own rounded-up projection dimensions. */
    public UiViewport coveringGui(int guiWidth, int guiHeight) {
        // UiGraphics submits a float scale to the GPU. Account for that rounding
        // too, and keep exact boundaries just outside the visible rectangle.
        double actualScale = (float) renderScale;
        return new UiViewport(Math.max(1, (int) Math.ceil(Math.nextUp(guiWidth / actualScale))),
                Math.max(1, (int) Math.ceil(Math.nextUp(guiHeight / actualScale))), renderScale);
    }

    public int toScreenFloor(int coordinate) {
        return (int) Math.floor(coordinate * renderScale);
    }

    public int toScreenCeil(int coordinate) {
        return (int) Math.ceil(coordinate * renderScale);
    }
}
