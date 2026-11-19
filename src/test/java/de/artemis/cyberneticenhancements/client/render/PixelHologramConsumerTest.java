package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class PixelHologramConsumerTest {
    private static PixelHologramConsumer.Point point(float x, float y, float u, float v) {
        return new PixelHologramConsumer.Point(x, y, 0, y, u, v, 255, 255, 255, 255, 0, 0, 240, 240, 0, 0, 1);
    }

    @Test public void splittingAnAngledFacePreservesItsAreaAndInterpolatedTextureCoordinates() {
        float pixel = HologramPixels.PLAYER_PIXEL;
        var face = List.of(point(0, -pixel / 2, 0, 0), point(1, pixel / 2, 1, 0),
                point(1, pixel * 4.5F, 1, 1), point(0, pixel * 3.5F, 0, 1));
        double[] area = {0};
        int[] count = {0};
        PixelHologramConsumer.bands(face, pixel, (row, polygon) -> {
            count[0]++;
            for (var p : polygon) {
                assertTrue(p.height() >= row * pixel - .00001F && p.height() <= (row + 1) * pixel + .00001F);
                assertEquals(p.x(), p.u(), .00001F);
                assertEquals((p.height() / pixel + .5F - p.x()) / 4, p.v(), .00001F);
            }
            for (int i = 0; i < polygon.size(); i++) {
                var a = polygon.get(i); var b = polygon.get((i + 1) % polygon.size());
                area[0] += (a.x() * b.y() - b.x() * a.y()) / 2;
            }
        });
        assertEquals(6, count[0]);
        assertEquals(4 * pixel, area[0], .00001);
    }

    @Test public void completedQuadEmitsSharpBandsAndKeepsUvOverlayAndNormals() {
        var recorded = new ArrayList<float[]>();
        VertexConsumer target = new VertexConsumer() {
            float x, y, u, v, alpha;
            @Override public VertexConsumer addVertex(float x, float y, float z) { this.x=x; this.y=y; return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { alpha=a; return this; }
            @Override public VertexConsumer setUv(float u, float v) { this.u=u; this.v=v; return this; }
            @Override public VertexConsumer setUv1(int u, int v) { assertEquals(16, u); assertEquals(32, v); return this; }
            @Override public VertexConsumer setUv2(int u, int v) { assertEquals(240, u); assertEquals(240, v); return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) {
                assertEquals(1, z, 0); recorded.add(new float[]{this.x,this.y,u,v,alpha}); return this;
            }
        };
        float pixel = HologramPixels.PLAYER_PIXEL;
        var consumer = new PixelHologramConsumer(target, new Matrix4f().translate(0, -10, 0), .5F, 1, 0x55CEFF, 0);
        for (float[] p : new float[][]{{0,0}, {1,0}, {1,2}, {0,2}})
            consumer.addVertex(p[0], 10 + p[1] * pixel, 0).setColor(255,255,255,255).setUv(p[0],p[1]/2)
                    .setUv1(16,32).setUv2(0,0).setNormal(0,0,1);
        assertFalse(recorded.isEmpty());
        assertEquals(0, recorded.size() % 4);
        assertEquals(2, recorded.stream().map(p -> p[4]).distinct().count());
        for (int i = 0; i < recorded.size(); i += 4) {
            for (int vertex = i; vertex < i + 4; vertex++) {
                var p = recorded.get(vertex);
                assertEquals(p[0], p[2], .0001F);
                assertEquals((p[1]-10)/(2*pixel), p[3], .0001F);
                assertEquals(recorded.get(i)[4], p[4], 0);
            }
        }
    }
}
