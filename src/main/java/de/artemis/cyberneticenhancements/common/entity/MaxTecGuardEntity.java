package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.world.CityNpcMedicine;
import de.artemis.cyberneticenhancements.common.world.NightCityResidents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Resident officer: a normal contact with city patrol and immediate assault retaliation. */
public final class MaxTecGuardEntity extends AbstractCityNpcEntity implements HolographicNpc {
    private static final EntityDataAccessor<Optional<UUID>> AGGRESSOR = SynchedEntityData.defineId(MaxTecGuardEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> ARRIVAL = SynchedEntityData.defineId(MaxTecGuardEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DEPARTURE = SynchedEntityData.defineId(MaxTecGuardEntity.class, EntityDataSerializers.LONG);
    private int nextThreat;

    public MaxTecGuardEntity(EntityType<? extends AbstractCityNpcEntity> type, Level level) { super(type, level); }
    @Override public NpcType npcType() { return NpcType.MAXTEC; }
    @Override public String appearanceOverride() { return "cyberneticenhancements:textures/entity/npc/generic/max_tac.png"; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(AGGRESSOR, Optional.empty()); builder.define(ARRIVAL, -1L); builder.define(DEPARTURE, -1L);
    }
    @Override public EntityDataAccessor<Long> hologramArrival() { return ARRIVAL; }
    @Override public EntityDataAccessor<Long> hologramDeparture() { return DEPARTURE; }
    @Override public void tick() {
        if (isAlive() && HolographicDeployment.tick(this)) { if (!isRemoved()) super.tick(); return; }
        super.tick();
    }

    public void configureCityGuard(BoundingBox bounds) {
        setHome(blockPosition());
        restrictTo(getRestrictCenter(), Math.max(bounds.getXSpan(), bounds.getZSpan()));
        getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(Math.max(64, Math.hypot(bounds.getXSpan(), bounds.getZSpan())));
        ensureIdentity();
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> goal instanceof AvoidEntityGoal<?> || goal instanceof WaterAvoidingRandomStrollGoal);
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85) {
            @Override protected net.minecraft.world.phys.Vec3 getPosition() {
                var bounds = NightCityResidents.bounds(MaxTecGuardEntity.this);
                if (bounds == null) return null;
                for (int attempt = 0; attempt < 8; attempt++) {
                    var pos = super.getPosition();
                    if (pos != null && NightCityResidents.area(bounds).contains(pos)) return pos;
                }
                return null;
            }
        });
    }

    private boolean insideCity(LivingEntity target) {
        var bounds = NightCityResidents.bounds(this);
        return bounds != null && NightCityResidents.area(bounds).contains(target.position());
    }

    private boolean cityThreat(LivingEntity target) {
        if (!target.isAlive() || target.isRemoved() || target.level() != level() || !insideCity(target)) return false;
        if (target instanceof Player player)
            return isDefendingAgainst(player) && !player.isCreative() && !player.isSpectator();
        if (!(target instanceof Mob mob) || target instanceof AbstractCityNpcEntity || target instanceof MaxTacEntity) return false;
        return target instanceof Enemy || mob.getTarget() instanceof Player || mob.getTarget() instanceof AbstractCityNpcEntity;
    }

    @Override public boolean canAttack(LivingEntity target) { return super.canAttack(target) && cityThreat(target); }
    @Override public boolean isDefendingAgainst(Player player) { return isAlive() && entityData.get(AGGRESSOR).filter(player.getUUID()::equals).isPresent(); }
    @Override public float aggressionRemaining(float partialTick) { return entityData.get(AGGRESSOR).isPresent() && isAlive() ? 1 : 0; }
    @Override public boolean isRefusing(Player player) { return isDefendingAgainst(player) || super.isRefusing(player); }
    @Override public boolean canTakeCall(Player player) { return getTarget() == null && super.canTakeCall(player); }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && !level().isClientSide() && source.getEntity() instanceof ServerPlayer player
                && !player.isCreative() && !player.isSpectator()) {
            boolean newlyHostile = !isDefendingAgainst(player);
            entityData.set(AGGRESSOR, Optional.of(player.getUUID()));
            setTarget(player); setAggressive(true);
            terminateConversation(player);
            if (newlyHostile && !super.isDefendingAgainst(player)) player.playNotifySound(
                    de.artemis.cyberneticenhancements.common.registry.ModSounds.NPC_AGGRESSION.get(), net.minecraft.sounds.SoundSource.HOSTILE, 0.08F, 1);
            de.artemis.cyberneticenhancements.common.world.NpcDisposition.sync(player);
        }
        return hurt;
    }

    /** Keep guard combat independent of civilians' short, low-health self-defense timer. */
    @Override protected void updateSelfDefense() {
        if (!(level() instanceof ServerLevel level) || isNoAi() || CityNpcMedicine.downed(this)) return;
        var aggressor = entityData.get(AGGRESSOR).map(id -> level.getServer().getPlayerList().getPlayer(id)).orElse(null);
        if (aggressor != null && !aggressor.isAlive()) entityData.set(AGGRESSOR, Optional.empty());
        if (aggressor != null && canAttack(aggressor)) setTarget(aggressor);
        if (getTarget() != null && !canAttack(getTarget())) { setTarget(null); getNavigation().stop(); }
        if (tickCount % 20 != 0) return;
        var bounds = NightCityResidents.bounds(this);
        if (bounds == null) return;
        if (getTarget() == null) {
            // Test paths in distance order; an unreachable nearer mob must not starve reachable threats.
            var threats = level.getEntitiesOfClass(Mob.class, NightCityResidents.area(bounds), this::canAttack);
            threats.sort(Comparator.comparingDouble(this::distanceToSqr));
            int checked = 0;
            for (; checked < Math.min(8, threats.size()); checked++) {
                var threat = threats.get((nextThreat + checked) % threats.size());
                var path = getNavigation().createPath(threat, 1);
                if (path != null && path.canReach()) { setTarget(threat); getNavigation().moveTo(path, 1.1); break; }
            }
            nextThreat = getTarget() != null || threats.isEmpty() ? 0 : (nextThreat + checked) % threats.size();
        } else {
            var path = getNavigation().createPath(getTarget(), 1);
            if (path == null || !path.canReach()) { setTarget(null); getNavigation().stop(); }
        }
        setAggressive(getTarget() != null);
        if (getTarget() == null && !insideCity(this)) {
            var home = getRestrictCenter(); getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1);
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); entityData.get(AGGRESSOR).ifPresent(id -> tag.putUUID("CityGuardAggressor", id));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); entityData.set(AGGRESSOR, tag.hasUUID("CityGuardAggressor") ? Optional.of(tag.getUUID("CityGuardAggressor")) : Optional.empty());
    }
}
