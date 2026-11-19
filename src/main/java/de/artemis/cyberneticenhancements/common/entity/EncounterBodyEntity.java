package de.artemis.cyberneticenhancements.common.entity;

import java.util.UUID;
import java.lang.ref.WeakReference;
import de.artemis.cyberneticenhancements.common.util.DeathBodyLifetime;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Saved encounter scenery, with no inventory, AI, damage, loot or NPC identity. */
public final class EncounterBodyEntity extends Entity {
    public static final double OWNER_RANGE = 32;
    public static final int DISSOLVE_TICKS = DeathBodyLifetime.DISSOLVE_TICKS;
    private static final EntityDataAccessor<Integer> APPEARANCE = SynchedEntityData.defineId(EncounterBodyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FALL_YAW = SynchedEntityData.defineId(EncounterBodyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> DEPARTURE = SynchedEntityData.defineId(EncounterBodyEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> OWNER_DEATH = SynchedEntityData.defineId(EncounterBodyEntity.class, EntityDataSerializers.LONG);
    private UUID owner;
    private BlockPos ownerLastPos = BlockPos.ZERO;
    private WeakReference<CyberpsychoEntity> ownerRef;
    private long missingSince = -1;

    public EncounterBodyEntity(EntityType<? extends EncounterBodyEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(APPEARANCE, 0);
        builder.define(FALL_YAW, 0F);
        builder.define(DEPARTURE, -1L);
        builder.define(OWNER_DEATH, -1L);
    }

    public void configure(CyberpsychoEntity psycho, int appearance, float fallYaw) {
        owner = psycho.getUUID(); ownerLastPos = psycho.blockPosition();
        ownerRef = new WeakReference<>(psycho);
        entityData.set(APPEARANCE, Math.clamp(appearance, 0, 3));
        entityData.set(FALL_YAW, fallYaw);
    }

    public UUID owner() { return owner; }
    public int appearance() { return entityData.get(APPEARANCE); }
    public float fallYaw() { return entityData.get(FALL_YAW); }
    public long departure() { return entityData.get(DEPARTURE); }
    public long ownerDeath() { return entityData.get(OWNER_DEATH); }

    /** A render-only proxy: never added to the world or assigned a saved NPC/contact identity. */
    public net.minecraft.world.entity.Mob createVisual() {
        var type = switch (appearance()) {
            case 0 -> de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.MAX_TAC.get();
            case 1 -> de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.MERC.get();
            case 2 -> de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.TECHIE.get();
            default -> de.artemis.cyberneticenhancements.common.registry.ModEntityTypes.NETRUNNER.get();
        };
        var source = type.create(level());
        if (source == null) return null;
        source.setUUID(getUUID()); source.setPos(position());
        source.yBodyRot = source.yBodyRotO = getYRot();
        source.setYRot(getYRot()); source.setNoAi(true);
        if (source instanceof AbstractCityNpcEntity npc) {
            var category = (getUUID().hashCode() & 1) == 0
                    ? de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory.MALE
                    : de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory.FEMALE;
            npc.applyClientPreviewIdentity("", category, source.getType().getDescription().getString(), "aqua");
        }
        source.setCustomNameVisible(false);
        return source;
    }

    public void releaseWithOwner(long deathTick) {
        if (level().isClientSide() || departure() >= 0) return;
        entityData.set(OWNER_DEATH, deathTick);
        entityData.set(DEPARTURE, deathTick + DeathBodyLifetime.HOLD_TICKS);
    }

    public void updateLifecycle() {
        if (!(level() instanceof ServerLevel level)) return;
        if (departure() < 0) {
            var found = owner == null ? null : level.getEntity(owner);
            var psycho = found instanceof CyberpsychoEntity living ? living : ownerRef == null ? null : ownerRef.get();
            if (psycho != null && !psycho.isRemoved()) {
                ownerRef = new WeakReference<>(psycho); missingSince = -1;
                ownerLastPos = psycho.blockPosition();
                if (!psycho.isAlive() || distanceToSqr(psycho) > OWNER_RANGE * OWNER_RANGE) release();
            } else if (owner == null || psycho != null && psycho.getRemovalReason() != RemovalReason.UNLOADED_TO_CHUNK) release();
            else if (level.hasChunkAt(ownerLastPos)) {
                if (missingSince < 0) missingSince = level.getGameTime();
                if (level.getGameTime() - missingSince > 40) release();
            } else missingSince = -1;
        }
        if (departure() >= 0 && level.getGameTime() - departure() >= DISSOLVE_TICKS) discard();
    }

    public void release() {
        if (!level().isClientSide() && departure() < 0) entityData.set(DEPARTURE, level().getGameTime());
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putLong("OwnerLastPos", ownerLastPos.asLong());
        tag.putInt("Appearance", appearance()); tag.putFloat("FallYaw", fallYaw());
        tag.putLong("Departure", departure());
        tag.putLong("OwnerDeath", ownerDeath());
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerRef = null; missingSince = -1;
        ownerLastPos = BlockPos.of(tag.getLong("OwnerLastPos"));
        entityData.set(APPEARANCE, Math.clamp(tag.getInt("Appearance"), 0, 3));
        float yaw = tag.getFloat("FallYaw");
        entityData.set(FALL_YAW, Float.isFinite(yaw) ? yaw : 0);
        entityData.set(DEPARTURE, tag.contains("Departure") ? tag.getLong("Departure") : -1L);
        entityData.set(OWNER_DEATH, tag.contains("OwnerDeath") ? tag.getLong("OwnerDeath") : -1L);
    }
}
