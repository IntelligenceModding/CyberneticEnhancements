package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class TraumaTeamEntity extends MedTechEntity implements HolographicNpc {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> ARRIVAL = net.minecraft.network.syncher.SynchedEntityData.defineId(TraumaTeamEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> DEPARTURE = net.minecraft.network.syncher.SynchedEntityData.defineId(TraumaTeamEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
    @Override public net.minecraft.network.syncher.EntityDataAccessor<Long> hologramArrival() { return ARRIVAL; }
    @Override public net.minecraft.network.syncher.EntityDataAccessor<Long> hologramDeparture() { return DEPARTURE; }
    private static final String AFTERCARE_UNTIL = "cyberneticenhancements.aftercare_until";
    private static final String PATIENT = "cyberneticenhancements.guarded_patient";
    private static final net.minecraft.network.syncher.EntityDataAccessor<java.util.Optional<java.util.UUID>> GUARDED_PATIENT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(TraumaTeamEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_UUID);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GUARDED_PATIENT, java.util.Optional.empty());
        builder.define(ARRIVAL, -1L);
        builder.define(DEPARTURE, -1L);
    }

    public boolean protects(java.util.UUID playerId) {
        return isAlive() && !isRemoved() && !HolographicDeployment.departing(this) && entityData.get(GUARDED_PATIENT).filter(playerId::equals).isPresent()
                && (level().isClientSide || aftercare());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(GUARDED_PATIENT, getPersistentData().hasUUID(PATIENT)
                ? java.util.Optional.of(getPersistentData().getUUID(PATIENT)) : java.util.Optional.empty());
    }

    public void beginAftercare(net.minecraft.server.level.ServerPlayer patient) {
        getPersistentData().putLong(AFTERCARE_UNTIL, level().getGameTime() + 400);
        getPersistentData().putUUID(PATIENT, patient.getUUID());
        entityData.set(GUARDED_PATIENT, java.util.Optional.of(patient.getUUID()));
        getNavigation().stop();
        setTarget(null);
        ensureIdentity();
    }

    private boolean aftercare() {
        return getPersistentData().getLong(AFTERCARE_UNTIL) > level().getGameTime();
    }

    private net.minecraft.world.entity.player.Player patient() {
        return getPersistentData().hasUUID(PATIENT)
                ? level().getPlayerByUUID(getPersistentData().getUUID(PATIENT)) : null;
    }

    private boolean threatensPatient(net.minecraft.world.entity.LivingEntity threat,
                                     net.minecraft.world.entity.player.Player patient) {
        if (threat == patient || threat instanceof TraumaTeamEntity || !threat.isAlive() || !canAttack(threat)) return false;
        if (threat instanceof net.minecraft.world.entity.Mob mob) {
            var victim = mob.getTarget();
            if (victim == patient) return true;
            if (victim instanceof TraumaTeamEntity medic && medic.aftercare()
                    && medic.getPersistentData().hasUUID(PATIENT)
                    && medic.getPersistentData().getUUID(PATIENT).equals(patient.getUUID())) return true;
        }
        return patient.getLastHurtByMob() == threat
                && patient.tickCount - patient.getLastHurtByMobTimestamp() < 100;
    }

    @Override
    protected void updateSelfDefense() {
        if (HolographicDeployment.departing(this)) { setTarget(null); return; }
        if (!aftercare()) {
            super.updateSelfDefense();
            return;
        }
        // The patient's position, rather than the deployment location, is the guard area.
        clearRestriction();
        var patient = patient();
        if (patient == null || !patient.isAlive()) {
            setTarget(null);
            return;
        }
        if (getTarget() != null && (!threatensPatient(getTarget(), patient) || getTarget().distanceToSqr(patient) > 32 * 32))
            setTarget(null);
        if (tickCount % 5 != 0) return;
        if (getTarget() == null) {
            var threats = level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    patient.getBoundingBox().inflate(24), entity -> threatensPatient(entity, patient));
            threats.stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).ifPresent(this::setTarget);
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.Goal() {
            { setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override
            public boolean canUse() {
                var patient = patient();
                return aftercare() && patient != null && patient.isAlive() && getTarget() == null;
            }
            @Override
            public void tick() {
                var patient = patient();
                if (patient == null) return;
                getLookControl().setLookAt(patient);
                if (distanceToSqr(patient) > 9) getNavigation().moveTo(patient, 1.15);
                else getNavigation().stop();
            }
            @Override
            public void stop() { getNavigation().stop(); }
        });
    }

    public TraumaTeamEntity(EntityType<? extends AbstractCityNpcEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground)
            ground.setCanWalkOverFences(true);
    }

    @Override
    public void tick() {
        if (!isAlive()) { super.tick(); return; }
        if (HolographicDeployment.tick(this)) {
            if (!isRemoved()) super.tick();
            return;
        }
        if (!level().isClientSide && getTags().contains("cyberneticenhancements.trauma_dispatch") && !de.artemis.cyberneticenhancements.common.world.TraumaCoverage.assigned(this)) {
            if (!aftercare()) {
                HolographicDeployment.dismiss(this);
                return;
            }
        }
        super.tick();
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (HolographicDeployment.departing(this)) return net.minecraft.world.InteractionResult.PASS;
        if (getTags().contains("cyberneticenhancements.trauma_dispatch") && !level().isClientSide && !aftercare())
            return net.minecraft.world.InteractionResult.PASS;
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
