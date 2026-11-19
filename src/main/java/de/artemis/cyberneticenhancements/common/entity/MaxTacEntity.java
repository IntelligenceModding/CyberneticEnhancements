package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.world.MaxTacResponse;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class MaxTacEntity extends CyberpsychoEntity implements HolographicNpc {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> ARRIVAL = net.minecraft.network.syncher.SynchedEntityData.defineId(MaxTacEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> DEPARTURE = net.minecraft.network.syncher.SynchedEntityData.defineId(MaxTacEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
    @Override public net.minecraft.network.syncher.EntityDataAccessor<Long> hologramArrival() { return ARRIVAL; }
    @Override public net.minecraft.network.syncher.EntityDataAccessor<Long> hologramDeparture() { return DEPARTURE; }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARRIVAL, -1L);
        builder.define(DEPARTURE, -1L);
    }
    private UUID offender, deployment;
    public MaxTacEntity(EntityType<? extends MaxTacEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) ground.setCanWalkOverFences(true);
        setCustomName(net.minecraft.network.chat.Component.translatable("entity.cyberneticenhancements.max_tac").withStyle(net.minecraft.ChatFormatting.RED));
    }

    public void deploy(ServerPlayer player, UUID wave, int slot, BlockPos pos) {
        offender = player.getUUID();
        deployment = wave;
        var loadout = switch (slot % 4) {
            case 0 -> CyberpsychoLoadout.LAUNCHER;
            case 1 -> CyberpsychoLoadout.SHOCK;
            case 2 -> CyberpsychoLoadout.MANTIS;
            default -> CyberpsychoLoadout.GORILLA;
        };
        configureCombat(1, loadout, "MaxTec / " + (slot + 1), pos);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(70);
        getAttribute(Attributes.ARMOR).setBaseValue(12);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(slot % 4 == 3 ? 8 : 6);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.31);
        setHealth(getMaxHealth());
        setTarget(player);
    }

    public UUID offender() { return offender; }
    public boolean isCityGuard() { return offender == null && de.artemis.cyberneticenhancements.common.world.NightCityResidents.assigned(this); }

    public void configureCityGuard(net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        if (getPersistentData().getBoolean("ce_city_guard_configured")) return;
        configureCombat(1, CyberpsychoLoadout.GORILLA, "MaxTec", blockPosition());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(70); setHealth(getMaxHealth());
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Math.max(64, Math.max(bounds.getXSpan(), bounds.getZSpan())));
        restrictTo(blockPosition(), Math.max(bounds.getXSpan(), bounds.getZSpan()));
        getPersistentData().putBoolean("ce_city_guard_configured", true);
        setTarget(null);
    }

    private boolean cityThreat(LivingEntity target) {
        if (!(target instanceof net.minecraft.world.entity.Mob mob) || !target.isAlive() || target instanceof AbstractCityNpcEntity
                || target instanceof MaxTacEntity) return false;
        var bounds = de.artemis.cyberneticenhancements.common.world.NightCityResidents.bounds(this);
        if (bounds == null || !de.artemis.cyberneticenhancements.common.world.NightCityResidents.area(bounds).contains(target.position())) return false;
        return target instanceof net.minecraft.world.entity.monster.Enemy || mob.getTarget() instanceof Player
                || mob.getTarget() instanceof AbstractCityNpcEntity || mob.getTarget() instanceof MaxTacEntity guard && guard.isCityGuard();
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.85) {
            @Override public boolean canUse() { return isCityGuard() && super.canUse(); }
            @Override protected net.minecraft.world.phys.Vec3 getPosition() {
                var bounds = de.artemis.cyberneticenhancements.common.world.NightCityResidents.bounds(MaxTacEntity.this);
                if (bounds == null) return null;
                for (int attempt = 0; attempt < 5; attempt++) {
                    var pos = super.getPosition();
                    if (pos != null && de.artemis.cyberneticenhancements.common.world.NightCityResidents.area(bounds).contains(pos)) return pos;
                }
                return null;
            }
        });
    }

    @Override protected boolean acceptsTarget(LivingEntity target) {
        return isCityGuard() ? cityThreat(target) : super.acceptsTarget(target);
    }
    @Override
    protected int getBaseExperienceReward() {
        return de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.MAX_TAC;
    }
    @Override protected boolean withinCombatArea(LivingEntity target) { return !isCityGuard() || cityThreat(target); }
    @Override protected double pursuitRange() {
        var bounds = isCityGuard() ? de.artemis.cyberneticenhancements.common.world.NightCityResidents.bounds(this) : null;
        return bounds == null ? 128 : Math.max(64, Math.hypot(bounds.getXSpan(), bounds.getZSpan()));
    }
    @Override protected boolean showsBossBar() { return false; }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target.level() == level() && super.canAttack(target)
                && (isCityGuard() || offender == null || target.getUUID().equals(offender) || target == getLastHurtByMob());
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel && isCityGuard()
                && de.artemis.cyberneticenhancements.common.world.NightCityResidents.migrateGuard(this)) return;
        if (!isAlive()) { super.tick(); return; }
        if (HolographicDeployment.tick(this)) {
            if (!isRemoved()) super.tick();
            return;
        }
        if (level() instanceof ServerLevel && isCityGuard() && !isNoAi() && !de.artemis.cyberneticenhancements.common.world.CityNpcMedicine.downed(this)) {
            if (getTarget() != null && !cityThreat(getTarget())) setTarget(null);
            if (tickCount % 20 == 0) {
                var bounds = de.artemis.cyberneticenhancements.common.world.NightCityResidents.bounds(this);
                if (getTarget() == null && bounds != null)
                    level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                            de.artemis.cyberneticenhancements.common.world.NightCityResidents.area(bounds), this::canAttack).stream()
                            .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).ifPresent(this::setTarget);
                if (getTarget() == null && bounds != null && !de.artemis.cyberneticenhancements.common.world.NightCityResidents.area(bounds).contains(position())) {
                    var home = getRestrictCenter(); getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1);
                }
            }
        }
        if (level() instanceof ServerLevel level && offender != null) {
            var player = level.getServer().getPlayerList().getPlayer(offender);
            if (!MaxTacResponse.get(level).active(offender, deployment) || player == null || !player.isAlive()) { HolographicDeployment.dismiss(this); return; }
            if (player.level() == level && (getTarget() == null || !getTarget().isAlive()) && canAttack(player)) setTarget(player);
        }
        super.tick();
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        boolean wasDead = dead;
        super.die(source);
        if (!wasDead && dead && offender != null && level() instanceof ServerLevel level)
            MaxTacResponse.officerKilled(level, offender, getUUID());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (offender != null) tag.putUUID("MaxTacOffender", offender);
        if (deployment != null) tag.putUUID("MaxTacDeployment", deployment);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        offender = tag.hasUUID("MaxTacOffender") ? tag.getUUID("MaxTacOffender") : null;
        deployment = tag.hasUUID("MaxTacDeployment") ? tag.getUUID("MaxTacDeployment") : null;
    }
}
