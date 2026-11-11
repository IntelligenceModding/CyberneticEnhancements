package de.artemis.cyberneticenhancements.client.ui;

public record HudLayout(Band left, Band right) {
    private static final int MARGIN = 6, GAP = 6;

    public static HudLayout fit(UiViewport viewport, int leftCards, int rightCards, int vanillaHeight) {
        int available = Math.max(1, viewport.width() / 2 - (int) Math.ceil(viewport.toUi(125)) - MARGIN - GAP);
        int bandWidth = Math.min(320, available);
        int bottom = viewport.height() - MARGIN;
        return new HudLayout(new Band(MARGIN, bandWidth, bottom, columns(leftCards, bandWidth)),
                new Band(viewport.width() - MARGIN - bandWidth, bandWidth, bottom, columns(rightCards, bandWidth)));
    }

    private static int columns(int cards, int width) {
        return cards > 1 && width < cards * 112 + (cards - 1) * 4 ? 1 : Math.max(1, cards);
    }

    public record Band(int x, int width, int bottom, int columns) {}
}
