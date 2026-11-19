package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Split model faces at texel rows so vanilla's vertex-color interpolation cannot blur scanlines. */
final class PixelHologramConsumer implements VertexConsumer {
    record Point(float x, float y, float z, float height, float u, float v,
                 float r, float g, float b, float a, float overlayU, float overlayV,
                 float lightU, float lightV, float nx, float ny, float nz) {
        Point mix(Point other, float t) {
            return new Point(lerp(x, other.x, t), lerp(y, other.y, t), lerp(z, other.z, t), lerp(height, other.height, t),
                    lerp(u, other.u, t), lerp(v, other.v, t), lerp(r, other.r, t), lerp(g, other.g, t),
                    lerp(b, other.b, t), lerp(a, other.a, t), lerp(overlayU, other.overlayU, t), lerp(overlayV, other.overlayV, t),
                    lerp(lightU, other.lightU, t), lerp(lightV, other.lightV, t),
                    lerp(nx, other.nx, t), lerp(ny, other.ny, t), lerp(nz, other.nz, t));
        }
        private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    }

    private final VertexConsumer target;
    private final Matrix4f inverse;
    private final Vector3f local = new Vector3f();
    private final float visibility, flicker, time;
    private final int tint;
    private final List<Point> quad = new ArrayList<>(4);
    private float x, y, z, height, u, v;
    private int r, g, b, a, overlayU, overlayV;

    PixelHologramConsumer(VertexConsumer target, Matrix4f inverse, float visibility, float flicker, int tint, float time) {
        this.target = target;
        this.inverse = inverse;
        this.visibility = visibility;
        this.flicker = flicker;
        this.tint = tint;
        this.time = time;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        inverse.transformPosition(x, y, z, local);
        height = local.y;
        return this;
    }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) {
        this.r = r; this.g = g; this.b = b; this.a = a;
        return this;
    }
    @Override public VertexConsumer setUv(float u, float v) { this.u = u; this.v = v; return this; }
    @Override public VertexConsumer setUv1(int u, int v) { overlayU = u; overlayV = v; return this; }
    @Override public VertexConsumer setUv2(int u, int v) { return this; } // Holograms remain self-lit.
    @Override public VertexConsumer setNormal(float x, float y, float z) {
        quad.add(new Point(this.x, this.y, this.z, height, u, v, r, g, b, a, overlayU, overlayV, 240, 240, x, y, z));
        if (quad.size() == 4) {
            bands(quad, HologramPixels.PLAYER_PIXEL, this::emit);
            quad.clear();
        }
        return this;
    }

    private void emit(int row, List<Point> polygon) {
        // A degenerate fourth corner lets the QUADS buffer carry each clipped triangle.
        for (int i = 1; i < polygon.size() - 1; i++) {
            vertex(polygon.getFirst(), row);
            vertex(polygon.get(i), row);
            vertex(polygon.get(i + 1), row);
            vertex(polygon.get(i + 1), row);
        }
    }

    private void vertex(Point point, int row) {
        float strength = 1 - visibility;
        float stripe = HologramPixels.stripe(row, time);
        target.addVertex(point.x, point.y, point.z)
                .setColor((int) (point.r * visibility + (tint >> 16 & 255) * strength),
                        (int) (point.g * visibility + (tint >> 8 & 255) * strength),
                        (int) (point.b * visibility + (tint & 255) * strength),
                        (int) (point.a * visibility * (visibility + strength * stripe) * flicker))
                .setUv(point.u, point.v).setUv1(Math.round(point.overlayU), Math.round(point.overlayV))
                .setUv2(240, 240).setNormal(point.nx, point.ny, point.nz);
    }

    static void bands(List<Point> polygon, float pixel, BiConsumer<Integer, List<Point>> output) {
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (Point point : polygon) { min = Math.min(min, point.height); max = Math.max(max, point.height); }
        int first = (int) Math.floor(min / pixel + 0.0001F);
        int last = Math.max(first, (int) Math.ceil(max / pixel - 0.0001F) - 1);
        for (int row = first; row <= last; row++) {
            var clipped = clip(clip(polygon, row * pixel, true), (row + 1) * pixel, false);
            if (clipped.size() >= 3) output.accept(row, clipped);
        }
    }

    private static List<Point> clip(List<Point> polygon, float height, boolean above) {
        if (polygon.isEmpty()) return List.of();
        List<Point> result = new ArrayList<>(6);
        Point previous = polygon.getLast();
        boolean previousInside = above ? previous.height >= height : previous.height <= height;
        for (Point point : polygon) {
            boolean inside = above ? point.height >= height : point.height <= height;
            if (inside != previousInside) result.add(previous.mix(point, (height - previous.height) / (point.height - previous.height)));
            if (inside) result.add(point);
            previous = point;
            previousInside = inside;
        }
        return result;
    }
}
