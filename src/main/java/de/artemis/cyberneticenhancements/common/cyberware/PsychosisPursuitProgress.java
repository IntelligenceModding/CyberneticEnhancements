package de.artemis.cyberneticenhancements.common.cyberware;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/** Progress survives navigation repaths, so repeatedly rebuilding a failed path cannot reset the timeout. */
public final class PsychosisPursuitProgress {
    private UUID target;
    private Vec3 anchor;
    private double bestDistance;
    private long lastMovement, lastApproach;
    private final Map<UUID, Long> skipped = new HashMap<>();

    public boolean skipped(UUID id, long now) {
        skipped.values().removeIf(until -> until <= now);
        return skipped.containsKey(id);
    }

    public boolean stalled(UUID id, Vec3 position, double distance, boolean inMelee, long now) {
        if (!id.equals(target)) {
            target = id; anchor = position; bestDistance = distance;
            lastMovement = lastApproach = now;
        }
        if (inMelee) {
            anchor = position; bestDistance = distance; lastMovement = lastApproach = now;
        }
        if (position.distanceToSqr(anchor) >= .65 * .65) {
            anchor = position; lastMovement = now;
        }
        if (distance < bestDistance - .5) { bestDistance = distance; lastApproach = now; }
        if (now - lastMovement < 60 && now - lastApproach < 160) return false;
        skipped.put(id, now + 200);
        target = null;
        return true;
    }
}
