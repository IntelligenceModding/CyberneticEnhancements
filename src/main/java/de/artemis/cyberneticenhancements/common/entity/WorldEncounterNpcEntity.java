package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * An NPC that belongs to a world encounter rather than to the city: never a contact, never recorded, never answered
 * for by MaxTec. The leader hands out the encounter's quest, the bystanders only stand guard, and the patient is
 * what the quest is looking for.
 */
public final class WorldEncounterNpcEntity extends AbstractCityNpcEntity implements de.artemis.cyberneticenhancements.common.dialogue.FaceTalker {
    public enum Role {
        /** SEATED is a bystander who sits where they were put until the build is robbed. */
        LEADER, BYSTANDER, PATIENT, SEATED;

        public static Role of(int id) { return values()[Math.clamp(id, 0, values().length - 1)]; }
    }

    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(WorldEncounterNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(WorldEncounterNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int GUARD_RADIUS = 2, LOOK_RANGE = 8, TRESPASS_CHECK_TICKS = 2, TRESPASS_RANGE = 24;
    private static final String ROLE_TAG = "EncounterRole", HOME_TAG = "EncounterPost";
    private static final int GIVE_UP_RETURN_TICKS = 20 * 20;
    private static final double ARRIVED_DISTANCE_SQR = 0.6 * 0.6, SETTLE_DISTANCE_SQR = 2.5 * 2.5;

    /** Where they were put and which way they faced, so they can go back once a fight is over. Null for the patient. */
    @Nullable private net.minecraft.world.phys.Vec3 post;
    private float postYaw;
    private int returnTicks;

    public WorldEncounterNpcEntity(EntityType<? extends AbstractCityNpcEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // They stand where they were put: no strolling, only turning to whoever speaks and fighting back.
        goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(2, new FixerPlayerFocusGoal(this));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.9D));
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, Role.BYSTANDER.ordinal());
        builder.define(SITTING, false);
    }

    public Role role() {
        if (entityData == null) return Role.BYSTANDER; // The super constructor asks for the name colour before the data exists.
        return Role.of(entityData.get(ROLE));
    }

    /** Whether they are sitting down right now, which is only while they are seated by role and not up and fighting. */
    public boolean isSitting() {
        return entityData != null && entityData.get(SITTING);
    }

    /** The site's people hold a grudge longer than a city NPC does. */
    @Override
    protected long selfDefenseTicks() {
        return 15L * 20L;
    }

    /** Sets the whole group on a player who robbed the site: whoever was sitting gets up. */
    public void provoke(ServerPlayer player) {
        provoke(player, true);
    }

    /** As above; {@code sound} is false for the rest of a group, so only one of them announces the aggression. */
    public void provoke(ServerPlayer player, boolean sound) {
        // The whole group shares the grudge: yellow for the usual five minutes as well as the fight itself.
        de.artemis.cyberneticenhancements.common.world.NpcDisposition.anger(player, getUUID());
        defendAgainst(player, sound);
    }

    /** Fixes what this NPC is, and so how it is named and treated. Call before adding it to the world. */
    public void configure(Role role, BlockPos home) {
        entityData.set(ROLE, role.ordinal());
        if (role == Role.PATIENT) {
            setNoAi(true);
            setInvulnerable(true);
            entityData.set(SITTING, true); // Slumped on the ground against a wall.
        } else {
            restrictTo(home.immutable(), GUARD_RADIUS);
            post = position();
            postYaw = getYRot();
        }
        ensureIdentity();
    }

    @Override
    public NpcType npcType() {
        return NpcType.WORLD_MED_TECH;
    }

    @Override
    public boolean canInteract(Player player) {
        return role() == Role.LEADER;
    }

    @Override
    protected boolean keepsContactRecord() {
        return false;
    }

    @Override
    protected boolean angersOnFirstHit() {
        return role() != Role.PATIENT;
    }

    @Override
    protected String dispositionColor(boolean angry) {
        if (role() == Role.LEADER) return super.dispositionColor(angry);
        return angry ? "yellow" : "gray";
    }

    /** The mark above the leader's head: whether they have something to ask. */
    public boolean offersQuest() {
        return role() == Role.LEADER && isAlive();
    }

    @Override
    public void setHome(BlockPos pos) {
        // Tied to the encounter's site in configure(); a world encounter NPC has no identity record to keep a home in.
    }

    @Override
    public void ensureIdentity() {
        var key = switch (role()) {
            case PATIENT -> "entity.cyberneticenhancements.world_encounter.patient";
            default -> "entity.cyberneticenhancements.med_tech";
        };
        var name = Component.translatable(key).withStyle(role() == Role.LEADER ? ChatFormatting.AQUA : ChatFormatting.GRAY);
        var categories = NpcCategory.values();
        setTransientIdentity(name, categories[Math.floorMod(getUUID().hashCode(), categories.length)]);
    }

    @Override
    public void tick() {
        super.tick();
        // Their AI is only on while they are fighting; otherwise they stand exactly where they were put.
        if (!level().isClientSide() && role() == Role.SEATED && post == null) { // Placed by the structure template, so nothing called configure().
            post = position();
            postYaw = getYRot();
            restrictTo(blockPosition(), GUARD_RADIUS);
        }
        if (!level().isClientSide() && role() != Role.PATIENT && tickCount % TRESPASS_CHECK_TICKS == 0) challengeTrespassers();
        if (!level().isClientSide() && role() != Role.PATIENT) {
            boolean idle = getTarget() == null && post != null && position().distanceToSqr(post) <= ARRIVED_DISTANCE_SQR;
            entityData.set(SITTING, role() == Role.SEATED && idle);
            if (getTarget() != null) {
                returnTicks = 0;
                setNoAi(false);
            } else if (post != null && position().distanceToSqr(post) > ARRIVED_DISTANCE_SQR) returnToPost();
            else {
                returnTicks = 0;
                setNoAi(true);
                if (post != null) setPos(post.x, post.y, post.z);
                if (!faceNearestPlayer()) faceTowards(postYaw);
            }
        }
        if (role() == Role.PATIENT && !level().isClientSide() && !isSitting()) entityData.set(SITTING, true);
    }

    /** Anyone the site is barred to, who walks through its red barrier, is set upon by whoever stands guard. */
    private void challengeTrespassers() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        for (var player : serverLevel.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(TRESPASS_RANGE),
                candidate -> candidate.isAlive() && !candidate.isCreative() && !candidate.isSpectator())) {
            if (getTarget() == player || !de.artemis.cyberneticenhancements.common.world.WorldEncounterOffenses.refuses(player, this)
                    || !de.artemis.cyberneticenhancements.common.block.HoloBarrierBlock.isCrossing(serverLevel, player.getBoundingBox())) continue;
            provoke(player, role() == Role.LEADER); // Each of them reacts on their own, so only the leader announces it.
            return;
        }
    }

    /** After a fight they walk back to where they stood; if the way is blocked for too long they are simply set down there. */
    private void returnToPost() {
        setNoAi(false);
        if (++returnTicks > GIVE_UP_RETURN_TICKS) {
            teleportTo(post.x, post.y, post.z);
            getNavigation().stop();
            return;
        }
        if (!getNavigation().isDone()) return;
        // A path ends at the nearest standable block, so the last stretch is a small step onto the exact spot.
        if (position().distanceToSqr(post) < SETTLE_DISTANCE_SQR) teleportTo(post.x, post.y, post.z);
        else getNavigation().moveTo(post.x, post.y, post.z, 1.0);
    }

    /** Eases the body and head round to a heading, for standing back at the post exactly as they were first placed. */
    private void faceTowards(float yaw) {
        float turned = net.minecraft.util.Mth.rotLerp(0.25F, getYHeadRot(), yaw);
        setYHeadRot(turned);
        setYBodyRot(net.minecraft.util.Mth.rotLerp(0.25F, yBodyRot, yaw));
        setYRot(turned);
        setXRot(net.minecraft.util.Mth.lerp(0.25F, getXRot(), 0));
    }

    /** Without AI nothing turns them, so they follow the nearest player with their gaze by hand, never moving a step. */
    private boolean faceNearestPlayer() {
        var player = level().getNearestPlayer(this, LOOK_RANGE);
        if (player == null || player.isSpectator()) return false;
        double dx = player.getX() - getX(), dz = player.getZ() - getZ(), dy = player.getEyeY() - getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float turned = net.minecraft.util.Mth.rotLerp(0.35F, getYHeadRot(), yaw);
        setYHeadRot(turned);
        setYBodyRot(net.minecraft.util.Mth.rotLerp(0.2F, yBodyRot, yaw));
        setYRot(turned);
        setXRot(net.minecraft.util.Mth.clamp(pitch, -40.0F, 40.0F));
        return true;
    }

    @Override
    public de.artemis.cyberneticenhancements.common.network.FaceDialogueStatePayload.Header header(ServerPlayer player) { return null; }

    @Override
    public boolean willTalk(ServerPlayer player) {
        return role() == Role.LEADER && isAlive() && !"red".equals(nameColorId())
                && !de.artemis.cyberneticenhancements.common.world.WorldEncounterOffenses.refuses(player, this);
    }

    @Override
    public de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.Node greet(ServerPlayer player) {
        if (!willTalk(player)) return null;
        lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.getEyePosition()); // Their AI is off while they stand around, so they have to be turned to the speaker by hand.
        var handIn = handInNode(player);
        return handIn != null ? handIn : new de.artemis.cyberneticenhancements.common.dialogue.LeaderDialogue(this).greet(player);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // The leader only talks once right-clicked; there is no window, and a click also hands over what is held.
        if (role() == Role.LEADER && hand == InteractionHand.MAIN_HAND && !level().isClientSide() && player instanceof ServerPlayer speaker
                && isAlive() && !de.artemis.cyberneticenhancements.common.world.WorldEncounterOffenses.refuses(player, this)) {
            if (de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager.tryHandInHeldItem(speaker, this, player.getItemInHand(hand))) {
                speaker.stopUsingItem();
                return InteractionResult.CONSUME;
            }
            if (!de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.active(speaker.getUUID())) {
                var node = greet(speaker);
                if (node != null) de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.open(speaker, this, node);
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && source.getEntity() instanceof ServerPlayer striker && role() != Role.PATIENT)
            de.artemis.cyberneticenhancements.common.world.WorldEncounterOffenses.record(striker, blockPosition());
        if (hurt && !level().isClientSide() && isAlive() && source.getEntity() instanceof ServerPlayer attacker && role() != Role.PATIENT) {
            // One blow sets the whole group on the attacker.
            for (var other : level().getEntitiesOfClass(WorldEncounterNpcEntity.class, getBoundingBox().inflate(24),
                    npc -> npc != this && npc.isAlive() && npc.role() != Role.PATIENT)) other.provoke(attacker, false);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide() && role() == Role.LEADER && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Their open quests can no longer be handed in; this is how the quest log learns of it.
            de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData.get(serverLevel).markDeceased(getUUID());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(ROLE_TAG, entityData.get(ROLE));
        if (post != null) tag.put(HOME_TAG, newDoubleList(post.x, post.y, post.z, postYaw));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, Math.clamp(tag.getInt(ROLE_TAG), 0, Role.values().length - 1));
        if (tag.contains(HOME_TAG, net.minecraft.nbt.Tag.TAG_LIST)) {
            var saved = tag.getList(HOME_TAG, net.minecraft.nbt.Tag.TAG_DOUBLE);
            post = new net.minecraft.world.phys.Vec3(saved.getDouble(0), saved.getDouble(1), saved.getDouble(2));
            postYaw = (float) saved.getDouble(3);
            restrictTo(BlockPos.containing(post), GUARD_RADIUS);
        }
        if (role() == Role.PATIENT) {
            setNoAi(true);
            setInvulnerable(true);
        }
        ensureIdentity();
    }

    @Nullable
    @Override
    public Player findPriorityPatient() {
        return null;
    }
}
