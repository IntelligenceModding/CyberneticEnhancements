package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A ripperdoc who walks up to one player, trades from a street stock and leaves again as a hologram.
 * Never a contact: no identity, relation or home is stored, and unlike a permanent ripperdoc it does not stay.
 */
public final class StreetRipperdocEntity extends AbstractCityNpcEntity implements HolographicNpc {
    public static final int WAIT_TICKS = 20 * 60 * 5;
    public static final int AFTER_TALK_TICKS = 20 * 60 * 2;
    private static final EntityDataAccessor<Long> ARRIVAL = SynchedEntityData.defineId(StreetRipperdocEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DEPARTURE = SynchedEntityData.defineId(StreetRipperdocEntity.class, EntityDataSerializers.LONG);
    private static final String CLIENT_TAG = "StreetClient", LEAVES_TAG = "StreetLeavesAt";
    private UUID client;
    /** Game time the doctor gives up and leaves, pushed back each time the client talks to them. */
    private long leavesAt;

    public StreetRipperdocEntity(EntityType<? extends AbstractCityNpcEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation ground) ground.setCanWalkOverFences(true);
    }

    @Override public EntityDataAccessor<Long> hologramArrival() { return ARRIVAL; }
    @Override public EntityDataAccessor<Long> hologramDeparture() { return DEPARTURE; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARRIVAL, -1L);
        builder.define(DEPARTURE, -1L);
    }

    @Override
    public NpcType npcType() {
        return NpcType.STREET_RIPPERDOC;
    }

    /** Sends this doctor to the player, who has {@link #WAIT_TICKS} to be reached before they give up. */
    public void serve(Player player) {
        client = player.getUUID();
        leavesAt = level().getGameTime() + WAIT_TICKS;
    }

    public UUID client() { return client; }

    public long leavesAt() { return leavesAt; }

    @Override
    protected boolean keepsContactRecord() {
        return false;
    }

    @Override
    public void setHome(BlockPos pos) {
        // They walk to the player, not back to where they appeared; and a street doctor has no home to remember.
    }

    @Override
    public void ensureIdentity() {
        // The renderer draws nothing for an NPC without a category, so pick a look per doctor even without a saved identity.
        var categories = de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory.values();
        setTransientIdentity(Component.translatable("entity.cyberneticenhancements.street_ripperdoc").withStyle(ChatFormatting.AQUA),
                categories[Math.floorMod(getUUID().hashCode(), categories.length)]);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return getTarget() == null && !HolographicDeployment.departing(StreetRipperdocEntity.this) && clientPlayer() != null; }
            @Override public boolean canContinueToUse() { return canUse(); }
            @Override public void tick() {
                var player = clientPlayer();
                if (player == null) return;
                getLookControl().setLookAt(player);
                if (distanceToSqr(player) > 6.25) getNavigation().moveTo(player, 1.15);
                else getNavigation().stop();
            }
            @Override public void stop() { getNavigation().stop(); }
        });
    }

    private Player clientPlayer() {
        if (client == null) return null;
        var player = level().getPlayerByUUID(client);
        return player != null && player.isAlive() && !player.isSpectator() ? player : null;
    }

    @Override
    public void tick() {
        if (!isAlive()) { super.tick(); return; }
        if (HolographicDeployment.tick(this)) {
            if (!isRemoved()) super.tick();
            return;
        }
        if (!level().isClientSide && tickCount % 20 == 0 && (clientPlayer() == null || level().getGameTime() >= leavesAt)
                && !isInConversation()) HolographicDeployment.dismiss(this);
        super.tick();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (HolographicDeployment.departing(this)) return InteractionResult.PASS;
        InteractionResult result = super.mobInteract(player, hand);
        if (!level().isClientSide && result.consumesAction()) leavesAt = Math.max(leavesAt, level().getGameTime() + AFTER_TALK_TICKS);
        return result;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (client != null) tag.putUUID(CLIENT_TAG, client);
        tag.putLong(LEAVES_TAG, leavesAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        client = tag.hasUUID(CLIENT_TAG) ? tag.getUUID(CLIENT_TAG) : null;
        leavesAt = tag.getLong(LEAVES_TAG);
    }
}
