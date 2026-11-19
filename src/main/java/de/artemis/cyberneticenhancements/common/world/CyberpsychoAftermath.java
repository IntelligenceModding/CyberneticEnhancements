package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.entity.EncounterBodyEntity;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.util.DeathFallDirection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CyberpsychoAftermath {
    private static final Map<ServerLevel, Set<EncounterBodyEntity>> LOADED = new IdentityHashMap<>();

    public static void ownerDied(CyberpsychoEntity psycho) {
        if (!(psycho.level() instanceof ServerLevel level)) return;
        var bodies = LOADED.get(level);
        if (bodies != null) for (var body : bodies)
            if (psycho.getUUID().equals(body.owner())) body.releaseWithOwner(level.getGameTime());
    }

    public static void joined(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof EncounterBodyEntity body && event.getLevel() instanceof ServerLevel level)
            LOADED.computeIfAbsent(level, ignored -> new HashSet<>()).add(body);
    }

    public static void left(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof EncounterBodyEntity body && event.getLevel() instanceof ServerLevel level) {
            var bodies = LOADED.get(level);
            // Tracking can end before the entity actually unloads. Keep checking that scenery too.
            if (bodies != null && body.isRemoved()) bodies.remove(body);
        }
    }

    public static void unloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) LOADED.remove(level);
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var bodies = LOADED.get(level);
        if (bodies == null) return;
        // Loaded scenery may be visible outside the simulation distance; it must still clean up.
        for (var body : List.copyOf(bodies)) {
            if (body.isRemoved()) bodies.remove(body);
            else if (body.departure() >= 0 || level.getGameTime() % 10 == 0) body.updateLifecycle();
        }
    }

    public static boolean create(CyberpsychoEntity psycho) {
        if (!(psycho.level() instanceof ServerLevel level)) return false;
        int count = 2 + psycho.getRandom().nextInt(4);
        var positions = new ArrayList<Vec3>();
        for (int attempt = 0; attempt < 100 && positions.size() < count; attempt++) {
            double angle = psycho.getRandom().nextDouble() * Math.PI * 2;
            double radius = 2 + psycho.getRandom().nextDouble() * 3;
            var column = psycho.position().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            if (!level.hasChunkAt(BlockPos.containing(column))) continue;
            var hit = level.clip(new ClipContext(column.add(0, 1, 0), column.add(0, -3, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, psycho));
            if (hit.getType() == HitResult.Type.MISS) continue;
            var pos = hit.getLocation();
            var away = pos.subtract(psycho.position()).multiply(1, 0, 1).normalize();
            var head = pos.add(away.scale(1.7));
            if (!level.hasChunkAt(BlockPos.containing(head))) continue;
            var support = level.clip(new ClipContext(head.add(0, 0.5, 0), head.add(0, -0.5, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, psycho));
            if (!level.hasChunkAt(BlockPos.containing(head)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(pos))
                    || !level.getWorldBorder().isWithinBounds(BlockPos.containing(head))
                    || support.getType() == HitResult.Type.MISS || Math.abs(support.getLocation().y - pos.y) > 0.2
                    || !level.getFluidState(BlockPos.containing(pos.add(0, 0.1, 0))).isEmpty()
                    || positions.stream().anyMatch(other -> other.distanceToSqr(pos) < 2.5)
                    || !level.noCollision(new AABB(pos, head).inflate(0.3, 0, 0.3).expandTowards(0, 0.46, 0).move(0, 0.04, 0))) continue;
            positions.add(pos);
        }
        // Wait for usable ground rather than producing floating or overlapping scenery.
        if (positions.size() < 2) return false;
        for (int i = 0; i < positions.size(); i++) {
            var body = ModEntityTypes.ENCOUNTER_BODY.get().create(level);
            if (body == null) continue;
            var pos = positions.get(i);
            float yaw = psycho.getRandom().nextFloat() * 360;
            body.moveTo(pos.x, pos.y, pos.z, yaw, 0);
            body.configure(psycho, i == 0 ? 0 : i == 1 ? 1 + psycho.getRandom().nextInt(3) : psycho.getRandom().nextInt(4),
                    DeathFallDirection.yaw(pos.x - psycho.getX(), pos.z - psycho.getZ(), yaw));
            level.addFreshEntity(body);
        }
        return true;
    }
    private CyberpsychoAftermath() { }
}
