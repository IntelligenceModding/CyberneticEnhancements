package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.feedback.GameplayMessages;
import de.artemis.cyberneticenhancements.common.world.StreetEncounters;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

/** A street thug in a shakedown: it closes in and waits for an answer, and only fights once the player is provoked. */
public final class GangMemberEntity extends CyberpsychoEntity implements HolographicNpc {
    private static final EntityDataAccessor<Long> ARRIVAL = SynchedEntityData.defineId(GangMemberEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DEPARTURE = SynchedEntityData.defineId(GangMemberEntity.class, EntityDataSerializers.LONG);
    private static final int MAX_AGE = 20 * 60 * 8;
    private UUID victim;
    private boolean hostile;
    private int demand;

    public GangMemberEntity(EntityType<? extends GangMemberEntity> type, Level level) {
        super(type, level);
    }

    /** {@code power} scales how much they take and deal: a gang that comes for a name is better armed than one that comes for a nobody. */
    public void deploy(ServerPlayer player, int slot, BlockPos pos, float power) {
        victim = player.getUUID();
        boolean leader = slot == 0;
        configureCombat(1, leader ? CyberpsychoLoadout.GORILLA : CyberpsychoLoadout.MANTIS, leader ? "Gang Boss" : "Gang Member", pos);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue((leader ? 60 : 40) * power);
        getAttribute(Attributes.ARMOR).setBaseValue((leader ? 6 : 3) * power);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((leader ? 7 : 5) * power);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.29);
        setHealth(getMaxHealth());
    }

    @Override public EntityDataAccessor<Long> hologramArrival() { return ARRIVAL; }
    @Override public EntityDataAccessor<Long> hologramDeparture() { return DEPARTURE; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARRIVAL, -1L);
        builder.define(DEPARTURE, -1L);
    }

    public UUID victim() { return victim; }
    public boolean hostile() { return hostile; }
    public void demand(int demand) { this.demand = demand; }

    /** Whoever they came to rob and finished off is relieved of the toll they asked for, as if their pockets were turned out. */
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && player.getUUID().equals(victim) && demand > 0) {
            int taken = Math.min(demand, PlayerEurodollarManager.balance(player));
            if (taken > 0) {
                PlayerEurodollarManager.remove(player, taken, "shakedown", "Pockets emptied by a street gang", null, "Street gang");
                GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.encounter.shakedown.looted", taken).withStyle(ChatFormatting.RED));
            }
            demand = 0;
        }
        return super.killedEntity(level, entity);
    }

    public void provoke() {
        if (hostile || HolographicDeployment.departing(this)) return;
        hostile = true;
        if (level() instanceof ServerLevel level && victim != null && level.getServer().getPlayerList().getPlayer(victim) instanceof ServerPlayer player
                && player.level() == level && canAttack(player)) setTarget(player);
    }

    /** Leaves as a hologram, as when paid off or backed down, or when the player is gone. */
    public void withdraw() {
        HolographicDeployment.dismiss(this);
    }

    @Override protected boolean acceptsTarget(LivingEntity target) {
        return hostile && super.acceptsTarget(target) && (target.getUUID().equals(victim) || target == getLastHurtByMob());
    }
    @Override protected boolean showsBossBar() { return false; }
    @Override protected int getBaseExperienceReward() { return 5; }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            private Player victim() { return GangMemberEntity.this.victim == null ? null : level().getPlayerByUUID(GangMemberEntity.this.victim); }
            @Override public boolean canUse() { var player = victim(); return !hostile && player != null && player.isAlive() && distanceToSqr(player) > 9; }
            @Override public boolean canContinueToUse() { return canUse(); }
            @Override public void tick() { var player = victim(); if (player != null) { getLookControl().setLookAt(player); getNavigation().moveTo(player, 1.0); } }
            @Override public void stop() { getNavigation().stop(); }
        });
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && source.getEntity() instanceof Player && level() instanceof ServerLevel level && victim != null)
            StreetEncounters.provoked(level.getServer(), victim);
        return hurt;
    }

    @Override
    public void tick() {
        if (isAlive() && HolographicDeployment.tick(this)) {
            if (!isRemoved()) super.tick();
            return;
        }
        super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive() || tickCount % 20 != 0) return;
        var player = victim == null ? null : level.getServer().getPlayerList().getPlayer(victim);
        // A gang with no player to answer to, no pending demand left, or no end in sight just melts away.
        if (player == null || !player.isAlive() || player.level() != level || distanceToSqr(player) > 96 * 96 || tickCount > MAX_AGE
                || !hostile && !StreetEncounters.pending(victim)) withdraw();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (victim != null) tag.putUUID("GangVictim", victim);
        tag.putBoolean("GangHostile", hostile);
        tag.putInt("GangDemand", demand);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        victim = tag.hasUUID("GangVictim") ? tag.getUUID("GangVictim") : null;
        hostile = tag.getBoolean("GangHostile");
        demand = tag.getInt("GangDemand");
    }
}
