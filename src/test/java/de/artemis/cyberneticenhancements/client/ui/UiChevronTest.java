package de.artemis.cyberneticenhancements.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiChevronTest {
    @Test
    public void bothChevronStrokesFaceTheSameDirectionAsGuiRectangles() {
        float[] vertices = UiRenderer.DISCLOSURE_CHEVRON;
        assertEquals(16, vertices.length);
        for (int offset = 0; offset < vertices.length; offset += 8) {
            assertTrue("Chevron stroke would be culled by the GUI render type", signedArea(vertices, offset, 0, 1) < 0);
        }
    }

    @Test
    public void expansionRotationAndResponsiveScalingKeepBothStrokesVisible() {
        for (int frame = 0; frame <= 20; frame++) {
            double angle = Math.PI * 0.5 * frame / 20.0;
            for (double scale : new double[]{0.15, 0.32, 0.8, 1.5, 3.0}) {
                for (int offset = 0; offset < UiRenderer.DISCLOSURE_CHEVRON.length; offset += 8) {
                    assertTrue(signedArea(UiRenderer.DISCLOSURE_CHEVRON, offset, angle, scale) < 0);
                }
            }
        }
    }

    private static double signedArea(float[] vertices, int offset, double angle, double scale) {
        double area = 0;
        for (int corner = 0; corner < 4; corner++) {
            int current = offset + corner * 2;
            int next = offset + ((corner + 1) % 4) * 2;
            double x1 = (vertices[current] * Math.cos(angle) - vertices[current + 1] * Math.sin(angle)) * scale;
            double y1 = (vertices[current] * Math.sin(angle) + vertices[current + 1] * Math.cos(angle)) * scale;
            double x2 = (vertices[next] * Math.cos(angle) - vertices[next + 1] * Math.sin(angle)) * scale;
            double y2 = (vertices[next] * Math.sin(angle) + vertices[next + 1] * Math.cos(angle)) * scale;
            area += x1 * y2 - x2 * y1;
        }
        return area / 2;
    }
}
