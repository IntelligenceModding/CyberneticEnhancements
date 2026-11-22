package de.artemis.cyberneticenhancements.client.render;

import de.artemis.cyberneticenhancements.common.entity.HangingWireEntity;
import de.artemis.cyberneticenhancements.common.entity.WireSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * How wires and what hangs on them move in the wind. Both what is drawn and what the crosshair can hit work from these, so a hung
 * item's hit box is exactly where it is drawn on every frame.
 */
public final class WireSway {
    private WireSway() {}

    /** The time the wind blows by: whole ticks and the fraction of the current one. */
    public static float time(HangingWireEntity entity, float partialTick) { return entity.level().getGameTime() + partialTick; }

    /** How hard the wind pulls at the wire: a faint breeze outdoors in fair weather, more in rain and most in thunder, little under a roof. */
    public static double wind(HangingWireEntity entity, float partialTick) {
        var level = entity.level();
        double weather = 0.03 + 0.08 * level.getRainLevel(partialTick) + 0.1 * level.getThunderLevel(partialTick);
        return level.canSeeSky(BlockPos.containing(entity.position())) ? weather : weather * 0.25;
    }

    /** The one wind everything hangs in: it depends on the time alone, so wires and what hangs on them always move together. */
    public static double gust(float time) { return Math.sin(time * 0.045) + 0.5 * Math.sin(time * 0.11); }

    /** How much of the wind blows across a wire running along (dx, dz), as the component along its sideways direction. */
    public static double across(float time, double dx, double dz, double horizontal) {
        double angle = 0.6 + 0.3 * Math.sin(0.003 * time);
        return (-dz * Math.cos(angle) + dx * Math.sin(angle)) / horizontal;
    }

    /** A point on the wire, pushed sideways by the wind: slow, uneven swells that vanish at the anchors. */
    public static Vec3 swayed(Vec3 start, Vec3 end, double t, float time, double wind) {
        Vec3 at = HangingWireEntity.point(start, end, t);
        double dx = end.x - start.x, dz = end.z - start.z, horizontal = Math.hypot(dx, dz);
        double reach = Math.min(1, start.distanceTo(end) / 6) * 4 * t * (1 - t);
        double lift = wind * 0.3 * reach * Math.sin(time * 0.07);
        if (horizontal < 1.0E-4) return at.add(0, lift, 0);
        double push = wind * reach * gust(time) * across(time, dx, dz, horizontal);
        return at.add(-dz / horizontal * push, lift, dx / horizontal * push);
    }

    /** The turn that puts a hung item's width along the wire. */
    public static float yaw(Vec3 start, Vec3 end) { return (float) Math.atan2(start.z - end.z, end.x - start.x); }

    /** How far, in degrees, a hung item swings across the wire about the point it hangs from. */
    public static float swing(Vec3 start, Vec3 end, float time, double wind) {
        double dx = end.x - start.x, dz = end.z - start.z, horizontal = Math.hypot(dx, dz);
        double across = horizontal < 1.0E-4 ? 0 : across(time, dx, dz, horizontal);
        return (float) (-(3 + wind * 60) * gust(time) * (0.3 + 0.7 * Math.abs(across)) * Math.signum(across == 0 ? 1 : across));
    }

    /** Where a hung item is and how it is turned, from the world to the item: its point on the wire, then yaw, then swing. */
    public static Matrix4f frame(Vec3 start, Vec3 end, double t, float time, double wind) {
        Vec3 at = swayed(start, end, t, time, wind);
        return new Matrix4f().translation((float) at.x, (float) at.y, (float) at.z).rotateY(yaw(start, end))
                .rotateX((float) Math.toRadians(swing(start, end, time, wind)));
    }

    /** What can be hit of a hung item, as boxes in its own frame, with the point it hangs from at the origin and down being negative. */
    public static List<AABB> boxes(ItemStack stack) {
        if (stack.is(ItemTags.BANNERS)) return List.of(new AABB(-0.3125, -1.25, -0.06, 0.3125, 0, 0.06));
        if (WireSigns.isSign(stack)) return List.of(new AABB(-0.4375, -1.0, -0.0625, 0.4375, -0.375, 0.0625), new AABB(-0.375, -0.375, -0.04, 0.375, 0, 0.04));
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof LanternBlock)
            return List.of(new AABB(-0.1875, -0.78, -0.1875, 0.1875, -0.34, 0.1875), new AABB(-0.125, -0.34, -0.125, 0.125, -0.215, 0.125), new AABB(-0.07, -0.215, -0.07, 0.07, 0, 0.07));
        return List.of(new AABB(-0.25, -0.8, -0.25, 0.25, 0, 0.25));
    }
}
