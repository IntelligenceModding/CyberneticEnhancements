package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.Test;

import static org.junit.Assert.*;

public class HologramVertexConsumerTest {
    @Test
    public void scanlinesStayAtTheSameHeightWhenTheCameraAndPlayerScaleChange() {
        for (float scale : new float[]{0.5F, 1.0F, 2.0F}) {
            for (float cameraPitch : new float[]{-1.2F, 0.0F, 0.8F}) {
                Matrix4f projection = new Matrix4f().rotateX(cameraPitch).rotateY(0.7F)
                        .translate(120.5F, -23.0F, 54.5F).scale(scale);
                RecordingConsumer target = new RecordingConsumer();
                HologramVertexConsumer consumer = new HologramVertexConsumer(target, projection, 35.5F);
                Vector3f position = projection.transformPosition(0.2F, 1.25F, -0.1F, new Vector3f());
                consumer.addVertex(position.x, position.y, position.z).setUv1(0, 0);
                assertEquals(1.25F * 4096, target.overlayU, 1.0F);
                assertEquals(Math.round(35.5F * 256), target.overlayV);
                assertEquals(position, target.position);
            }
        }
    }

    @Test
    public void hologramsKeepIndependentAnimationDataWhenSharingABuffer() {
        RecordingConsumer target = new RecordingConsumer();
        HologramVertexConsumer first = new HologramVertexConsumer(target, new Matrix4f().translate(0, 10, 0), 10);
        HologramVertexConsumer second = new HologramVertexConsumer(target, new Matrix4f().translate(0, 20, 0), 70);
        first.addVertex(0, 11, 0).setUv1(0, 0);
        assertEquals(4096, target.overlayU);
        assertEquals(2560, target.overlayV);
        second.addVertex(0, 21.5F, 0).setUv1(0, 0);
        assertEquals(6144, target.overlayU);
        assertEquals(17920, target.overlayV);
        first.addVertex(0, 10.5F, 0).setUv1(0, 0);
        assertEquals(2048, target.overlayU);
        assertEquals(2560, target.overlayV);
    }

    @Test
    public void skinTextureCoordinatesAndColorArePreserved() {
        RecordingConsumer target = new RecordingConsumer();
        HologramVertexConsumer consumer = new HologramVertexConsumer(target, new Matrix4f(), 80);
        consumer.addVertex(0, 1, 0, 0xAFB4EEFF, 0.25F, 0.75F, 0, 15728880, 0, 1, 0);
        assertEquals(0.25F, target.u, 0);
        assertEquals(0.75F, target.v, 0);
        assertEquals(0xAFB4EEFF, target.color);
        assertEquals(0, target.overlayV);
    }

    private static final class RecordingConsumer implements VertexConsumer {
        private final Vector3f position = new Vector3f();
        private int overlayU;
        private int overlayV;
        private int color;
        private float u;
        private float v;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            position.set(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            color = alpha << 24 | red << 16 | green << 8 | blue;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            overlayU = u;
            overlayV = v;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}
