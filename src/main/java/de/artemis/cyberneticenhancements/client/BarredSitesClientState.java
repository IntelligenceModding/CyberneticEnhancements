package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.BarredSitesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Where this player is no longer welcome, as told by the server. */
public final class BarredSitesClientState {
    /** Offences this close to a site are held against it; matches the server's rule. */
    private static final int SITE_RADIUS = 64;
    private static long[] sites = new long[0];
    /** Pairs of (site position, client game time the lockout ends). */
    private static long[] lockouts = new long[0];

    private BarredSitesClientState() {}

    public static void update(BarredSitesPayload payload) {
        sites = payload.sites();
        var level = Minecraft.getInstance().level;
        long now = level == null ? 0 : level.getGameTime();
        long[] deadlines = payload.lockouts().clone();
        for (int i = 1; i < deadlines.length; i += 2) deadlines[i] += now; // The server sends how long is left, which is the same on both clocks.
        lockouts = deadlines;
    }

    public static void clear() {
        sites = new long[0];
        lockouts = new long[0];
    }

    public static boolean holdsAgainst(BlockPos site) {
        for (long packed : sites) if (near(packed, site)) return true;
        return false;
    }

    /** How many ticks are left before the leader at this site deals with the player again after a failed quest; 0 if none. */
    public static long lockoutTicks(BlockPos site, long now) {
        long longest = 0;
        for (int i = 0; i + 1 < lockouts.length; i += 2) if (near(lockouts[i], site)) longest = Math.max(longest, lockouts[i + 1] - now);
        return longest;
    }

    private static boolean near(long packed, BlockPos site) {
        var at = BlockPos.of(packed);
        long dx = at.getX() - site.getX(), dz = at.getZ() - site.getZ();
        return dx * dx + dz * dz <= (long) SITE_RADIUS * SITE_RADIUS;
    }
}
