package de.artemis.cyberneticenhancements.common.tarot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** An immobile wall projection; the supporting blocks are never replaced. */
public final class TarotCardEntity extends Entity {
    public static final float WIDTH = 2F, HEIGHT = 3F;
    public static final int COLLECTION_TICKS = 12;
    private static final EntityDataAccessor<BlockPos> ANCHOR = SynchedEntityData.defineId(TarotCardEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(TarotCardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> CARD = SynchedEntityData.defineId(TarotCardEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DECORATION = SynchedEntityData.defineId(TarotCardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> COLLECTING = SynchedEntityData.defineId(TarotCardEntity.class, EntityDataSerializers.BOOLEAN);
    private int collectionTicks;

    public TarotCardEntity(EntityType<? extends TarotCardEntity> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANCHOR, BlockPos.ZERO); builder.define(FACE, Direction.SOUTH.get3DDataValue()); builder.define(CARD, "the_fool"); builder.define(DECORATION, false); builder.define(COLLECTING, false);
    }
    public String cardId() { return entityData.get(CARD); }
    public Direction facing() { return Direction.from3DDataValue(entityData.get(FACE)); }
    public BlockPos anchor() { return entityData.get(ANCHOR); }
    public boolean isDecoration() { return entityData.get(DECORATION); }
    public boolean isCollecting() { return entityData.get(COLLECTING); }
    public float collectionProgress(float partialTick) {
        return isCollecting() ? Math.clamp((collectionTicks + partialTick) / COLLECTION_TICKS, 0, 1) : 0;
    }
    public void collect() {
        if (level() instanceof net.minecraft.server.level.ServerLevel server && !isDecoration() && !isCollecting()) {
            entityData.set(COLLECTING, true);
            TarotPlacements.get(server).remove(getUUID());
        }
    }
    public void setDecoration(boolean decoration) { entityData.set(DECORATION, decoration); }
    public void attach(BlockPos anchor, Direction face, String id) {
        if (!face.getAxis().isHorizontal() || TarotCatalog.find(id) == null) throw new IllegalArgumentException("Invalid tarot attachment");
        entityData.set(ANCHOR, anchor.immutable()); entityData.set(FACE, face.get3DDataValue()); entityData.set(CARD, id); updateAttachment();
    }
    public void updateAttachment() {
        Vec3 center = attachmentPosition(anchor(), facing());
        setPos(center.x, center.y, center.z);
    }
    public static Vec3 attachmentPosition(BlockPos anchor, Direction face) {
        Direction right = face.getCounterClockWise();
        return Vec3.atBottomCenterOf(anchor).add(right.getStepX() * .5 - face.getStepX() * .47, 0, right.getStepZ() * .5 - face.getStepZ() * .47);
    }
    public static AABB attachmentBounds(BlockPos anchor, Direction face) {
        return boundsAt(attachmentPosition(anchor, face), face);
    }
    private static AABB boundsAt(Vec3 at, Direction face) {
        double dx = face.getAxis() == Direction.Axis.X ? .025 : WIDTH / 2D;
        double dz = face.getAxis() == Direction.Axis.Z ? .025 : WIDTH / 2D;
        return new AABB(at.x - dx, at.y, at.z - dz, at.x + dx, at.y + HEIGHT, at.z + dz);
    }
    @Override protected AABB makeBoundingBox() {
        // Entity.load and client movement packets also rebuild this box.
        Direction face = entityData == null ? Direction.SOUTH : facing();
        return boundsAt(position(), face);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { super.onSyncedDataUpdated(key); if (key == ANCHOR || key == FACE) updateAttachment(); }
    public static boolean canAttach(Level level, BlockPos anchor, Direction face) {
        if (!face.getAxis().isHorizontal()) return false;
        Direction right = face.getCounterClockWise();
        for (int col = 0; col < 2; col++) for (int row = 0; row < 3; row++) {
            BlockPos air = anchor.relative(right, col).above(row), wall = air.relative(face.getOpposite());
            if (!level.hasChunkAt(air) || !level.hasChunkAt(wall) || !level.isInWorldBounds(air)) return false;
            if (!level.getBlockState(air).getCollisionShape(level, air).isEmpty() || !level.getFluidState(air).isEmpty()
                    || !level.getBlockState(wall).isFaceSturdy(level, wall, face)) return false;
        }
        return true;
    }
    @Override public void tick() {
        super.tick(); updateAttachment();
        if (isCollecting()) {
            collectionTicks++;
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                if (collectionTicks % 2 == 0) collectionParticles(server);
                if (collectionTicks >= COLLECTION_TICKS) discard();
            }
            return;
        }
        if (level() instanceof net.minecraft.server.level.ServerLevel server && (tickCount == 1 || tickCount % 40 == 0))
            TarotPlacements.get(server).register(this);
        if (!level().isClientSide && tickCount % 40 == 0
                && level().hasChunksAt(anchor().offset(-2, 0, -2), anchor().offset(2, 3, 2))
                && !canAttach(level(), anchor(), facing())) discard();
    }
    private void collectionParticles(net.minecraft.server.level.ServerLevel server) {
        var dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.35F, .85F, 1F), .65F);
        Direction right = facing().getCounterClockWise();
        double height = HEIGHT * (1 - collectionProgress(0));
        for (int i = 0; i < 8; i++) {
            double across = (random.nextDouble() - .5) * WIDTH;
            Vec3 point = position().add(right.getStepX() * across + facing().getStepX() * .08,
                    height + (random.nextDouble() - .5) * .25, right.getStepZ() * across + facing().getStepZ() * .08);
            server.sendParticles(dust, point.x, point.y, point.z, 1, .025, .06, .025, 0);
            if (i % 4 == 0) server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    point.x, point.y, point.z, 1, .02, .04, .02, .015);
        }
    }
    @Override public boolean isPickable() { return !isCollecting(); }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!isDecoration() || !(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                || !player.mayBuild() || !player.serverLevel().mayInteract(player, anchor())) return false;
        discard();
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.PAINTING_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1);
        return true;
    }
    @Override public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof net.minecraft.server.level.ServerLevel server)
            TarotPlacements.get(server).remove(getUUID());
        super.remove(reason);
    }
    @Override public boolean isPushable() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        Direction face = Direction.from3DDataValue(tag.getInt("Facing"));
        String id = tag.getString("Card");
        attach(BlockPos.of(tag.getLong("Anchor")), face.getAxis().isHorizontal() ? face : Direction.SOUTH,
                TarotCatalog.find(id) == null ? "the_fool" : id);
        setDecoration(tag.getBoolean("Decoration"));
        entityData.set(COLLECTING, tag.getBoolean("Collecting"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putLong("Anchor", anchor().asLong()); tag.putInt("Facing", facing().get3DDataValue()); tag.putString("Card", cardId()); tag.putBoolean("Decoration", isDecoration()); tag.putBoolean("Collecting", isCollecting()); }
}
