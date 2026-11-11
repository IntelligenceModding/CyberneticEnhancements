package de.artemis.cyberneticenhancements.common.cyberware.sandevistan;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SandevistanSession {
    public final SandevistanProfile profile;
    public final long started;
    public final long cooldownTicks;
    public final double condition;
    public final int recentUses;
    public final long hardStop;
    private final Set<UUID> rewardedVictims = new HashSet<>();
    private long until;

    public SandevistanSession(SandevistanProfile profile, long started, long durationTicks,
                              long cooldownTicks, double condition, int recentUses) {
        this.profile = profile;
        this.started = started;
        this.cooldownTicks = Math.max(20, cooldownTicks);
        this.condition = Math.clamp(condition, 0, 1);
        this.recentUses = Math.clamp(recentUses, 1, 8);
        long duration = Math.clamp(durationTicks, 20, 1200);
        until = started + duration;
        hardStop = until + duration / 2;
    }

    public long until() { return until; }

    public boolean rewardKill(UUID victim, long extensionTicks) {
        if (rewardedVictims.size() >= 6 || !rewardedVictims.add(victim)) return false;
        until = Math.min(hardStop, until + Math.clamp(extensionTicks, 0, 40));
        return true;
    }

    public double effectiveTimeScale() {

        double strength = SandevistanProfile.EXPERIMENTAL.equals(profile.id())
                ? 0.85 + 0.15 * condition : 0.55 + 0.45 * condition;
        return 1 - (1 - profile.timeScale()) * strength;
    }

    public double activationLoad() {
        return profile.activationStrain() * (1 + (recentUses - 1) * 0.35 + (1 - condition) * 0.75);
    }

    public boolean shouldOverload(double instability) {
        return recentUses >= 3 && instability >= 95 || recentUses >= 3 && condition < 0.15
                || SandevistanProfile.EXPERIMENTAL.equals(profile.id()) && recentUses >= 4 && instability >= 75;
    }

    public static int recentUseCount(int previous, long previousTick, long now, long recoveryWindow) {
        long recovered = Math.max(0, now - previousTick) / Math.max(20, recoveryWindow);
        return (int) Math.clamp(previous - recovered + 1, 1, 8);
    }
}
