package de.artemis.cyberneticenhancements.client.render;

/** World effects use block texels; player projections include vanilla's model render scale. */
public final class HologramPixels {
    public static final int PER_BLOCK = 16;
    public static final float SIZE = 1F / PER_BLOCK;
    public static final float PLAYER_SCALE = 0.9375F;
    public static final float PLAYER_PIXEL = SIZE * PLAYER_SCALE;
    public static final int MARKER_ROWS = 8;
    private HologramPixels() {}

    public static boolean markerRow(int row) { return row >= 0 && row < MARKER_ROWS && row != 2; }

    public static int sweepRow(float time, int rows) {
        float phase = time % 80;
        if (phase < 0) phase += 80;
        return Math.min(rows - 1, (int) Math.floor(phase * rows / 80F));
    }

    public static float stripe(int row, float time) {
        return Math.floorMod(row - (int) Math.floor(time / 8F), 3) == 0 ? 0.45F : 1F;
    }

    public static boolean ringCell(int x, int z, int outer, int inner) {
        float distance = (x + .5F) * (x + .5F) + (z + .5F) * (z + .5F);
        return distance <= outer * outer && distance >= inner * inner;
    }
}
