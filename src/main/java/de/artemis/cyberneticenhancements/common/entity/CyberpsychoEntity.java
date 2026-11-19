package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusManager;
import de.artemis.cyberneticenhancements.common.cyberware.CombatStatusType;
import de.artemis.cyberneticenhancements.common.quest.CyberpsychoEncounter;
import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public class CyberpsychoEntity extends PathfinderMob implements net.minecraft.world.entity.monster.Enemy {
    private static final EntityDataAccessor<Long> COMBAT_MEDICINE = SynchedEntityData.defineId(CyberpsychoEntity.class, EntityDataSerializers.LONG);
    private final NpcCombatHealing combatHealing = new NpcCombatHealing(this, COMBAT_MEDICINE);
    public NpcCombatHealing combatHealing() { return combatHealing; }
    private static final EntityDataAccessor<Integer> LOADOUT = SynchedEntityData.defineId(CyberpsychoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(CyberpsychoEntity.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent boss = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS) {
        @Override public UUID getId() { return CyberpsychoEntity.this.getUUID(); }
    };
    private CompoundTag contract = new CompoundTag();
    private UUID questOwner;
    private BlockPos sightingHome;
    private int tier = 1;
    private int meleeCooldown, abilityCooldown = 60, chargeTicks;
    private String subjectName = "Cyberpsycho";
    private boolean aftermathCreated;

    public CyberpsychoEntity(EntityType<? extends CyberpsychoEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomNameVisible(true);
        refreshName();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 110).add(Attributes.ARMOR, 14)
                .add(Attributes.ATTACK_DAMAGE, 8.5).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.KNOCKBACK_RESISTANCE, 0.65);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COMBAT_MEDICINE, -1L);
        builder.define(LOADOUT, 0);
        builder.define(CHARGING, false);
    }

    public CyberpsychoLoadout loadout() { return CyberpsychoLoadout.fromIndex(entityData.get(LOADOUT)); }
    public boolean charging() { return entityData.get(CHARGING); }
    public int tier() { return tier; }
    public UUID questOwner() { return questOwner; }
    public QuestContract contract() { return contract.isEmpty() ? null : QuestContract.fromTag(contract); }

    @Override
    protected int getBaseExperienceReward() {
        return de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.cyberpsycho(tier);
    }

    public void configure(QuestContract quest, UUID owner, BlockPos home) {
        contract = quest.toTag();
        questOwner = owner;
        configureCombat(quest.tier(), CyberpsychoLoadout.fromIndex(quest.targetIndex()), quest.psychoName(), home);
    }

    public void configureDebugEncounter(int combatTier, CyberpsychoLoadout combatLoadout, BlockPos home) {
        configureCombat(combatTier, combatLoadout, CyberpsychoEncounter.subjectName(getUUID().toString()), home);
    }

    protected void configureCombat(int combatTier, CyberpsychoLoadout combatLoadout, String name, BlockPos home) {
        tier = Math.clamp(combatTier, 1, 3);
        subjectName = name;
        sightingHome = home.immutable();
        entityData.set(LOADOUT, combatLoadout.ordinal());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(80 + tier * 30);
        getAttribute(Attributes.ARMOR).setBaseValue(12 + tier * 2);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((loadout() == CyberpsychoLoadout.GORILLA ? 10 : 7) + tier * 1.5);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(loadout() == CyberpsychoLoadout.GORILLA ? 0.29 : 0.32);
        setHealth(getMaxHealth());
        restrictTo(home, 48);
        refreshName();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, NpcCombatHealing.goal(this));
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CombatGoal());
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> target instanceof Player player && !player.isCreative() && !player.isSpectator() && distanceToSqr(player) <= 144));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return acceptsTarget(target) && super.canAttack(target);
    }

    protected boolean acceptsTarget(LivingEntity target) {
        return target instanceof Player player && !player.isCreative() && !player.isSpectator();
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        if (!level().isClientSide()) refreshName();
    }

    private void refreshName() {
        setCustomName(Component.literal(subjectName).withStyle(ChatFormatting.RED));
        boss.setName(getDisplayName());
    }

    protected boolean withinCombatArea(LivingEntity target) { return isWithinRestriction(target.blockPosition()); }
    protected double pursuitRange() { return 64; }
    protected boolean showsBossBar() { return true; }

    public ServerBossEvent bossEvent() { return boss; }

    private void updateBossBar(ServerLevel level) {
        boss.setName(getDisplayName());
        boss.setProgress(Math.clamp(getHealth() / getMaxHealth(), 0, 1));
        if (!showsBossBar() || !isAlive()) { boss.removeAllPlayers(); return; }
        for (var player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && distanceToSqr(player) <= 48 * 48) boss.addPlayer(player);
            else boss.removePlayer(player);
        }
        boss.getPlayers().stream().filter(player -> !level.players().contains(player)).toList().forEach(boss::removePlayer);
    }

    @Override
    public void tick() {
        super.tick();
        combatHealing.tick();
        if (combatHealing.usingItem()) { entityData.set(CHARGING, false); chargeTicks = 0; }
        if (!(level() instanceof ServerLevel level)) return;
        if (getType() == ModEntityTypes.CYBERPSYCHO.get() && isAlive() && !aftermathCreated && (tickCount == 1 || tickCount % 40 == 0))
            aftermathCreated = de.artemis.cyberneticenhancements.common.world.CyberpsychoAftermath.create(this);
        if (tickCount % 20 == 0) {
            var quest = contract();
            if (quest != null && CyberpsychoEncounter.get(level).retired(quest.id())) { discard(); return; }
        }
        updateBossBar(level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide()) combatHealing.damaged(source);
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = dead;
        super.die(source);
        if (!wasDead && dead && level() instanceof ServerLevel level) {
            CyberpsychoEncounter.onDeath(this);
            de.artemis.cyberneticenhancements.common.world.CyberpsychoAftermath.ownerDied(this);
            // MaxTec shares this base class but is a separate entity type.
            if (getType() == ModEntityTypes.CYBERPSYCHO.get()) {
                for (var player : level.players()) {
                    if (distanceToSqr(player) <= 48 * 48)
                        player.playNotifySound(ModSounds.CYBERPSYCHO_NEUTRALIZED.get(), SoundSource.HOSTILE, 0.35F, 1.0F);
                }
            }
        }
        if (dead) boss.removeAllPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        boss.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        combatHealing.save(tag);
        tag.put("SightingContract", contract);
        if (questOwner != null) tag.putUUID("SightingOwner", questOwner);
        if (sightingHome != null) tag.putLong("SightingHome", sightingHome.asLong());
        tag.putInt("PsychoTier", tier);
        tag.putBoolean("AftermathCreated", aftermathCreated);
        tag.putInt("PsychoLoadout", loadout().ordinal());
        tag.putString("SubjectName", subjectName);
        tag.putInt("AbilityCooldown", Math.max(abilityCooldown, 40));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        combatHealing.load(tag);
        contract = tag.getCompound("SightingContract").copy();
        questOwner = tag.hasUUID("SightingOwner") ? tag.getUUID("SightingOwner") : null;
        sightingHome = tag.contains("SightingHome") ? BlockPos.of(tag.getLong("SightingHome")) : null;
        if (sightingHome != null) restrictTo(sightingHome, 48);
        tier = Math.clamp(tag.getInt("PsychoTier"), 1, 3);
        aftermathCreated = tag.getBoolean("AftermathCreated");
        entityData.set(LOADOUT, Math.floorMod(tag.getInt("PsychoLoadout"), CyberpsychoLoadout.values().length));
        subjectName = tag.getString("SubjectName");
        if (subjectName.isBlank()) subjectName = "Cyberpsycho";
        abilityCooldown = Math.max(40, tag.getInt("AbilityCooldown"));
        chargeTicks = 0;
        entityData.set(CHARGING, false);
        refreshName();
    }

    private final class CombatGoal extends Goal {
        private CombatGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return getTarget() != null && getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { getNavigation().stop(); chargeTicks = 0; entityData.set(CHARGING, false); }

        @Override
        public void tick() {
            if (combatHealing.usingItem()) { getNavigation().stop(); return; }
            var target = getTarget();
            if (target == null || !canAttack(target) || distanceToSqr(target) > pursuitRange() * pursuitRange() || !withinCombatArea(target)) {
                setTarget(null);
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            meleeCooldown = Math.max(0, meleeCooldown - 1);
            abilityCooldown = Math.max(0, abilityCooldown - 1);
            boolean visible = getSensing().hasLineOfSight(target);
            double distance = distanceToSqr(target);
            if (loadout() == CyberpsychoLoadout.LAUNCHER && visible && distance > 36 && distance < 32 * 32) {
                getNavigation().stop();
                if (abilityCooldown == 0) {
                    entityData.set(CHARGING, true);
                    if (++chargeTicks == 1) level().playSound(null, blockPosition(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.HOSTILE, 0.8F, 0.7F);
                    if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.SMOKE, getX(), getEyeY() - 0.3, getZ(), 2, 0.1, 0.1, 0.1, 0);
                    if (chargeTicks >= 24) {
                        var missile = new CyberpsychoMissile(level(), CyberpsychoEntity.this);
                        var aim = target.getBoundingBox().getCenter().subtract(missile.position());
                        missile.shoot(aim.x, aim.y, aim.z, 0.75F, 1.5F);
                        level().addFreshEntity(missile);
                        level().playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 0.9F, 0.8F);
                        abilityCooldown = 100;
                        chargeTicks = 0;
                        entityData.set(CHARGING, false);
                    }
                }
            } else {
                getNavigation().moveTo(target, 1.15);
                if (visible && distance > 9 && distance < 81 && abilityCooldown == 0 && onGround() && loadout() != CyberpsychoLoadout.LAUNCHER) {
                    getNavigation().stop();
                    entityData.set(CHARGING, true);
                    if (++chargeTicks == 1) level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 0.6F, 1.4F);
                    if (chargeTicks >= 12) {
                        var direction = target.position().subtract(position()).normalize();
                        setDeltaMovement(direction.x * 0.85, 0.32, direction.z * 0.85);
                        hasImpulse = true;
                        abilityCooldown = 80;
                        chargeTicks = 0;
                        entityData.set(CHARGING, false);
                    }
                } else {
                    chargeTicks = 0;
                    entityData.set(CHARGING, false);
                }
            }
            if (visible && distance <= 9 && meleeCooldown == 0) {
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                if (doHurtTarget(target)) {
                    if (loadout() == CyberpsychoLoadout.THERMAL) target.igniteForSeconds(3);
                    if (loadout() == CyberpsychoLoadout.SHOCK) CombatStatusManager.applyStatus(target, CombatStatusType.SHOCK, 40, 1, false);
                    if (loadout() == CyberpsychoLoadout.MANTIS) CombatStatusManager.applyStatus(target, CombatStatusType.BLEED, 60, 1, false);
                    if (loadout() == CyberpsychoLoadout.GORILLA) target.knockback(1, getX() - target.getX(), getZ() - target.getZ());
                }
                meleeCooldown = loadout() == CyberpsychoLoadout.GORILLA ? 30 : 22;
            }
        }
    }
}
