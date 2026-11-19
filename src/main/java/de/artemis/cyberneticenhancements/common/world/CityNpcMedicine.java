package de.artemis.cyberneticenhancements.common.world;

import de.artemis.cyberneticenhancements.common.entity.*;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** A recoverable downed state preserves the actual resident instead of cloning dead NPCs. */
public final class CityNpcMedicine {
    private static final String DOWNED = "ce_city_downed", SINCE = "ce_city_downed_since", NO_AI = "ce_city_previous_no_ai";
    private static final java.util.Map<ServerLevel, java.util.Set<Mob>> PATIENTS = new java.util.IdentityHashMap<>();
    public static boolean downed(LivingEntity npc) { return npc.getPersistentData().getBoolean(DOWNED); }

    private static boolean patient(LivingEntity entity) {
        return entity instanceof AbstractCityNpcEntity || entity instanceof MaxTacEntity guard && guard.isCityGuard();
    }

    private static boolean available(MedTechEntity medic) {
        return medic.isAlive() && !medic.isRemoved() && !medic.isNoAi() && !downed(medic) && NightCityResidents.assigned(medic)
                && !medic.getTags().contains("cyberneticenhancements.trauma_dispatch") && !HolographicDeployment.departing(medic);
    }

    public static void dying(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob npc) || !patient(npc) || !(npc.level() instanceof ServerLevel level)) return;
        if (event.getSource().is(DamageTypes.GENERIC_KILL) || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            if (downed(npc)) clear(npc);
            return;
        }
        var bounds = NightCityResidents.bounds(npc);
        if (bounds == null) return;
        boolean reachableMedic = !level.getEntitiesOfClass(MedTechEntity.class, NightCityResidents.area(bounds),
                medic -> medic != npc && available(medic)).isEmpty();
        if (!downed(npc) && !reachableMedic) return;
        if (!downed(npc)) {
            npc.getPersistentData().putBoolean(DOWNED, true);
            npc.getPersistentData().putLong(SINCE, level.getGameTime());
            npc.getPersistentData().putBoolean(NO_AI, npc.isNoAi());
            PATIENTS.computeIfAbsent(level, ignored -> new java.util.HashSet<>()).add(npc);
            npc.setNoAi(true); npc.getNavigation().stop(); npc.setTarget(null); npc.clearFire();
            if (npc instanceof AbstractCityNpcEntity contact)
                de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.interruptForNpc(contact, false);
            for (var player : level.players())
                if (player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu menu && menu.entityId() == npc.getId()) player.closeContainer();
        }
        npc.setHealth(1); event.setCanceled(true); DeathBodySync.downed(npc, true);
    }

    public static void damage(LivingIncomingDamageEvent event) {
        if (downed(event.getEntity()) && !event.getSource().is(DamageTypes.GENERIC_KILL)
                && !event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) event.setCanceled(true);
    }

    private static void clear(Mob npc) {
        npc.setNoAi(npc.getPersistentData().getBoolean(NO_AI));
        npc.getPersistentData().remove(DOWNED); npc.getPersistentData().remove(SINCE); npc.getPersistentData().remove(NO_AI);
        var patients = PATIENTS.get(npc.level()); if (patients != null) patients.remove(npc);
    }

    public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob npc && event.getLevel() instanceof ServerLevel level && downed(npc)) {
            PATIENTS.computeIfAbsent(level, ignored -> new java.util.HashSet<>()).add(npc);
            npc.getPersistentData().putLong(SINCE, level.getGameTime()); npc.setNoAi(true);
            DeathBodySync.downed(npc, true);
        }
    }
    public static void unloaded(net.neoforged.neoforge.event.level.LevelEvent.Unload event) { PATIENTS.remove(event.getLevel()); }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var patients = PATIENTS.get(level); if (patients == null) return;
        for (var npc : java.util.List.copyOf(patients)) {
            if (npc.isRemoved() || !npc.isAlive() || !downed(npc)) { patients.remove(npc); continue; }
            npc.setDeltaMovement(Vec3.ZERO); npc.fallDistance = 0; npc.clearFire();
            if (level.getGameTime() % 20 == 0) DeathBodySync.downed(npc, true);
            if (level.getGameTime() - npc.getPersistentData().getLong(SINCE) > 1200) {
                clear(npc); npc.hurt(npc.damageSources().genericKill(), Float.MAX_VALUE);
            }
        }
    }

    public static Goal treatmentGoal(MedTechEntity medic) { return new TreatmentGoal(medic); }

    private static final class TreatmentGoal extends Goal {
        private final MedTechEntity medic;
        private Mob target;
        private int treating;
        private int nextSearch;
        TreatmentGoal(MedTechEntity medic) { this.medic = medic; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private boolean needsHelp(Mob npc) {
            var bounds = NightCityResidents.bounds(medic);
            return bounds != null && npc != medic && patient(npc) && npc.isAlive() && !npc.isRemoved()
                    && NightCityResidents.area(bounds).contains(npc.position()) && (downed(npc) || npc.getHealth() < npc.getMaxHealth());
        }
        @Override public boolean canUse() {
            if (!available(medic) || medic.getTarget() != null || medic.isOnCall() || medic.isInConversation() || medic.tickCount < nextSearch) return false;
            nextSearch = medic.tickCount + 20;
            var bounds = NightCityResidents.bounds(medic);
            if (bounds == null) return false;
            target = medic.level().getEntitiesOfClass(Mob.class, NightCityResidents.area(bounds), this::needsHelp).stream()
                    .min(Comparator.<Mob>comparingInt(npc -> downed(npc) ? 0 : 1).thenComparingDouble(medic::distanceToSqr)).orElse(null);
            return target != null;
        }
        @Override public boolean canContinueToUse() {
            return available(medic) && medic.getTarget() == null && !medic.isOnCall() && !medic.isInConversation() && target != null && needsHelp(target);
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { treating = 0; }
        @Override public void stop() { target = null; treating = 0; medic.getNavigation().stop(); }
        @Override public void tick() {
            if (target == null) return;
            medic.getLookControl().setLookAt(target, 30, 30);
            if (medic.distanceToSqr(target) > 6.25 || !medic.hasLineOfSight(target)) {
                medic.getNavigation().moveTo(target, 1.15); treating = 0; return;
            }
            medic.getNavigation().stop();
            if (++treating % 20 == 0 && medic.level() instanceof ServerLevel level)
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 0.5, target.getZ(), 3, 0.2, 0.15, 0.2, 0);
            if (downed(target)) {
                if (treating < 60) return;
                clear(target); target.setHealth(Math.max(2, target.getMaxHealth() * 0.5F)); target.clearFire();
                DeathBodySync.downed(target, false);
            } else if (treating % 20 == 0) target.heal(4);
        }
    }
    private CityNpcMedicine() { }
}
