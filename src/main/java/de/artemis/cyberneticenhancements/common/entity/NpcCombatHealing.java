package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.world.CityNpcMedicine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Limited combat supplies, with an interruptible use action synchronized to both health displays. */
public final class NpcCombatHealing {
    public static final int GAP = 120, USE_TIME = 40, COOLDOWN = 300, DOSES = 3;
    public static final float HEAL_FRACTION = 0.25F;
    private final Mob owner;
    private final EntityDataAccessor<Long> phase;
    private long lastDamage = -1, readyAt;
    private int doses = DOSES;

    public NpcCombatHealing(Mob owner, EntityDataAccessor<Long> phase) { this.owner = owner; this.phase = phase; }

    public static NpcCombatHealing of(LivingEntity entity) {
        if (entity instanceof AbstractCityNpcEntity npc) return npc.combatHealing();
        if (entity instanceof CyberpsychoEntity psycho) return psycho.combatHealing();
        return null;
    }

    public boolean usingItem() {
        return owner.getEntityData().get(phase) >= 0 && owner.isAlive()
                && !CityNpcMedicine.downed(owner) && !HolographicDeployment.departing(owner);
    }
    public boolean highlighted() {
        long value = owner.getEntityData().get(phase);
        return usingItem() || value < -1 && owner.level().getGameTime() - (-value - 2) < 40;
    }
    public float progress(float partialTick) {
        return usingItem() ? Math.clamp((owner.level().getGameTime() - owner.getEntityData().get(phase) + partialTick) / USE_TIME, 0, 1) : 0;
    }
    public int remainingDoses() { return doses; }

    public void damaged(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity || lastDamage >= 0) lastDamage = owner.level().getGameTime();
        if (usingItem()) owner.getEntityData().set(phase, -1L);
    }

    public void tick() {
        if (!(owner.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (!owner.isAlive() || owner.isRemoved() || CityNpcMedicine.downed(owner)
                || owner instanceof HolographicNpc && HolographicDeployment.departing(owner)) {
            if (owner.getEntityData().get(phase) >= 0) owner.getEntityData().set(phase, -1L);
            return;
        }
        if (usingItem()) {
            owner.getNavigation().stop();
            if (now - owner.getEntityData().get(phase) >= USE_TIME) {
                owner.heal(owner.getMaxHealth() * HEAL_FRACTION);
                doses--;
                readyAt = now + COOLDOWN;
                owner.getEntityData().set(phase, -now - 2);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        owner.getX(), owner.getEyeY(), owner.getZ(), 5, 0.2, 0.2, 0.2, 0);
            }
            return;
        }
        if (lastDamage < 0 || now - lastDamage < GAP || now - lastDamage > 1200
                || now < readyAt || doses <= 0 || owner.getHealth() > owner.getMaxHealth() * 0.75F) return;
        owner.getNavigation().stop();
        owner.getEntityData().set(phase, now);
        level.playSound(null, owner.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.3F, 1.15F);
    }

    public void save(CompoundTag tag) {
        var state = new CompoundTag();
        state.putInt("Doses", doses);
        state.putLong("LastDamage", lastDamage);
        state.putLong("ReadyAt", readyAt);
        tag.put("CombatMedicine", state);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("CombatMedicine")) return;
        var state = tag.getCompound("CombatMedicine");
        doses = Math.clamp(state.getInt("Doses"), 0, DOSES);
        lastDamage = state.getLong("LastDamage");
        readyAt = state.getLong("ReadyAt");
        owner.getEntityData().set(phase, -1L);
    }

    public static Goal goal(Mob owner) {
        return new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return of(owner).usingItem(); }
            @Override public void tick() { owner.getNavigation().stop(); }
        };
    }
}
