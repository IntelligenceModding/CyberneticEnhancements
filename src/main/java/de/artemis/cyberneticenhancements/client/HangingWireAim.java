package de.artemis.cyberneticenhancements.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.cyberneticenhancements.client.render.WireSway;
import de.artemis.cyberneticenhancements.common.entity.HangingWireEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * What a player looks at on a wire, like a block: hung items have hit boxes that move with them in the wind, found against the
 * wind of the very frame being drawn, and are outlined while looked at. The bare wire follows its swaying curve.
 */
public final class HangingWireAim {
    private HangingWireAim() {}

    /** The wire, how far along it (0..1), whether it is a hung item rather than the bare wire, and for an item where it is and what its boxes are. */
    public record Aim(HangingWireEntity wire, double t, boolean item, Matrix4f frame, List<AABB> boxes) {}

    public static float partialTick() { return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true); }

    /** What the player looks at on a wire, if nearer than whatever block they look at. */
    public static Aim find(Player player, float partialTick) {
        double reach = player.blockInteractionRange();
        Vec3 eye = player.getEyePosition(partialTick), look = player.getViewVector(partialTick);
        HitResult block = player.pick(reach, partialTick, false);
        double limit = block.getType() == HitResult.Type.BLOCK ? block.getLocation().distanceTo(eye) : reach;
        Aim bestItem = null, bestWire = null;
        double itemDistance = limit, wireScore = 0;
        for (HangingWireEntity wire : player.level().getEntitiesOfClass(HangingWireEntity.class, player.getBoundingBox().inflate(reach + HangingWireEntity.MAX_LENGTH / 2))) {
            Vec3 start = wire.start(), end = wire.end();
            float time = WireSway.time(wire, partialTick);
            double wind = WireSway.wind(wire, partialTick);
            for (HangingWireEntity.Decoration d : wire.decorations()) {
                Matrix4f frame = WireSway.frame(start, end, d.t(), time, wind);
                Matrix4f inverse = new Matrix4f(frame).invert();
                Vector3f origin = inverse.transformPosition(new Vector3f((float) eye.x, (float) eye.y, (float) eye.z));
                Vector3f direction = inverse.transformDirection(new Vector3f((float) look.x, (float) look.y, (float) look.z));
                List<AABB> boxes = WireSway.boxes(d.stack());
                for (AABB box : boxes) {
                    double distance = entry(origin, direction, box);
                    if (distance >= 0 && distance <= itemDistance) { itemDistance = distance; bestItem = new Aim(wire, d.t(), true, frame, boxes); }
                }
            }
            int steps = (int) Math.ceil(start.distanceTo(end) * 6);
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double score = miss(eye, look, limit, WireSway.swayed(start, end, t, time, wind)) - 0.15;
                if (score < wireScore) { wireScore = score; bestWire = new Aim(wire, t, false, null, List.of()); }
            }
        }
        return bestItem != null ? bestItem : bestWire;
    }

    /** How far along the ray it enters the box, or -1 if it misses. */
    private static double entry(Vector3f origin, Vector3f direction, AABB box) {
        double near = 0, far = Double.MAX_VALUE;
        double[] o = {origin.x, origin.y, origin.z}, d = {direction.x, direction.y, direction.z};
        double[] low = {box.minX, box.minY, box.minZ}, high = {box.maxX, box.maxY, box.maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(d[axis]) < 1.0E-9) {
                if (o[axis] < low[axis] || o[axis] > high[axis]) return -1;
                continue;
            }
            double a = (low[axis] - o[axis]) / d[axis], b = (high[axis] - o[axis]) / d[axis];
            near = Math.max(near, Math.min(a, b));
            far = Math.min(far, Math.max(a, b));
            if (near > far) return -1;
        }
        return near;
    }

    /** How far the ray passes from the point, or far if the point is behind the player or beyond what the ray can reach. */
    private static double miss(Vec3 eye, Vec3 look, double limit, Vec3 at) {
        double along = at.subtract(eye).dot(look);
        return along < 0 || along > limit ? Double.MAX_VALUE : at.distanceTo(eye.add(look.scale(along)));
    }

    /** Outlines the hung item looked at, the way a block is, drawn in the frame's own wind. */
    public static void outline(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player.isSpectator() || minecraft.level == null) return;
        Aim aim = find(player, event.getPartialTick().getGameTimeDeltaPartialTick(true));
        if (aim == null || !aim.item()) return;
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        pose.mulPose(aim.frame());
        for (AABB box : aim.boxes()) LevelRenderer.renderLineBox(pose, lines, box, 0F, 0F, 0F, 0.4F);
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /** A block behind a hung item that is looked at is not outlined too. */
    public static void hideBlockOutline(RenderHighlightEvent.Block event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Aim aim = find(player, event.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (aim != null) event.setCanceled(true);
    }
}
