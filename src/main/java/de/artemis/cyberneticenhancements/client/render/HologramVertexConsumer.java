package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class HologramVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final Matrix4f inverseProjection;
    private final Vector3f localPosition = new Vector3f();
    private final int phase;
    private int height;

    HologramVertexConsumer(VertexConsumer delegate, Matrix4f projectionPose, float time) {
        this.delegate = delegate;
        inverseProjection = new Matrix4f(projectionPose).invert();
        phase = Math.round((time % 80.0F) * 256.0F);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        inverseProjection.transformPosition(x, y, z, localPosition);
        height = Math.round(localPosition.y * 4096.0F);
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(height, phase);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }
}
