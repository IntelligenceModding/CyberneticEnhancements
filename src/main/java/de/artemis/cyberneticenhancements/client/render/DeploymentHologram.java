package de.artemis.cyberneticenhancements.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.cyberneticenhancements.common.entity.HolographicDeployment;
import de.artemis.cyberneticenhancements.common.entity.HolographicNpc;
import de.artemis.cyberneticenhancements.common.entity.TraumaTeamEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Mob;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class DeploymentHologram {
    private DeploymentHologram() {}

    static boolean active(Mob entity) {
        if (DeathBodies.rendering(entity)) return false;
        return entity instanceof HolographicNpc && (HolographicDeployment.departing(entity)
                || HolographicDeployment.visibility(entity, 0) < 1);
    }

    static MultiBufferSource translucentBuffers(float visibility, int tint, float time, PoseStack pose, MultiBufferSource original) {
        float motion = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float flicker = 1 - motion * 0.12F * (0.5F + 0.5F * (float) Math.sin(time * 2.7));
        Matrix4f inverse = new Matrix4f(pose.last().pose()).invert();
        return type -> pixelBands(type, original.getBuffer(CorpseRenderTypes.translucent(type)), inverse, visibility, flicker, tint, time * motion);
    }

    static MultiBufferSource buffers(Mob entity, float partialTick, PoseStack pose, MultiBufferSource original) {
        if (!active(entity)) return original;
        float visibility = HolographicDeployment.visibility(entity, partialTick);
        float motion = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float time = entity.tickCount + partialTick;
        float flicker = 1 - motion * 0.12F * (0.5F + 0.5F * (float) Math.sin(time * 2.7));
        int tint = entity instanceof TraumaTeamEntity || entity instanceof de.artemis.cyberneticenhancements.common.entity.StreetRipperdocEntity ? 0x66FFC1 : 0x55CEFF;
        Matrix4f inverse = new Matrix4f(pose.last().pose()).invert();
        return type -> pixelBands(type, original.getBuffer(type), inverse, visibility, flicker, tint, time * motion);
    }

    private static VertexConsumer pixelBands(net.minecraft.client.renderer.RenderType type, VertexConsumer target,
                                             Matrix4f inverse, float visibility, float flicker, int tint, float time) {
        if (type.mode() == com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS
                && (type.format() == com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY
                || type.format() == com.mojang.blaze3d.vertex.DefaultVertexFormat.BLOCK))
            return new PixelHologramConsumer(target, inverse, visibility, flicker, tint, time);
        return new Pixels(target, inverse, visibility, flicker, tint, time);
    }

    private static final class Pixels implements VertexConsumer {
        private final VertexConsumer target;
        private final Matrix4f inverse;
        private final Vector3f position = new Vector3f();
        private final float visibility, flicker, time;
        private final int tint;
        private float stripe = 1;

        Pixels(VertexConsumer target, Matrix4f inverse, float visibility, float flicker, int tint, float time) {
            this.target = target;
            this.inverse = inverse;
            this.visibility = visibility;
            this.flicker = flicker;
            this.tint = tint;
            this.time = time;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            inverse.transformPosition(x, y, z, position);
            stripe = HologramPixels.stripe((int) Math.floor(position.y / HologramPixels.PLAYER_PIXEL), time);
            target.addVertex(x, y, z);
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            float strength = 1 - visibility;
            target.setColor((int) (r * visibility + (tint >> 16 & 255) * strength),
                    (int) (g * visibility + (tint >> 8 & 255) * strength),
                    (int) (b * visibility + (tint & 255) * strength),
                    (int) (a * visibility * (visibility + (1 - visibility) * stripe) * flicker));
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { target.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { target.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { target.setUv2(240, 240); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { target.setNormal(x, y, z); return this; }
    }
}
