package de.artemis.cyberneticenhancements.common.entity;

import de.artemis.cyberneticenhancements.common.cyberware.CyberstrainManager;
import de.artemis.cyberneticenhancements.common.cyberware.PlayerCyberwareInventory;
import de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu;
import de.artemis.cyberneticenhancements.common.network.FaceHazardHighlightPayload;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData;
import de.artemis.cyberneticenhancements.common.world.NpcIdentityConfig.NpcCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public abstract class AbstractCityNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<Long> COMBAT_MEDICINE = SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.LONG);
    private final NpcCombatHealing combatHealing = new NpcCombatHealing(this, COMBAT_MEDICINE);
    public NpcCombatHealing combatHealing() { return combatHealing; }
    private static final String HOME_POS_X_TAG = "HomePosX";
    private static final String HOME_POS_Y_TAG = "HomePosY";
    private static final String HOME_POS_Z_TAG = "HomePosZ";
    private static final String HAS_HOME_TAG = "HasHome";
    private static final String CURRENT_CUSTOMER_TAG = "CurrentCustomer";
    private static final String NPC_CATEGORY_TAG = "NpcCategory";
    private static final String NPC_APPEARANCE_TAG = "NpcAppearance";
    private static final String NPC_NAME_COLOR_TAG = "NpcNameColor";
    private static final String NPC_BASE_NAME_TAG = "NpcBaseName";
    private static final String NPC_NICKNAME_TAG = "NpcNickname";
    private static final String MEETUP_CUSTOMER_TAG = "MeetupCustomer";
    private static final String MEETUP_EXPIRES_AT_TAG = "MeetupExpiresAt";
    private static final String MEETUP_RETURN_AT_TAG = "MeetupReturnAt";
    private static final String MEETUP_SPENT_TAG = "MeetupSpent";
    private static final String PLAYER_NPC_RELATIONS_TAG = "cyberneticenhancements.npc_relations";
    private static final String LEGACY_PLAYER_FIXER_RELATIONS_TAG = "cyberneticenhancements.fixer_relations";
    private static final String PLAYER_RELATION_SEEN_TAG = "Seen";
    private static final String PLAYER_RELATION_TEMPORARY_TAG = "Temporary";
    private static final String PLAYER_RELATION_TRUST_TAG = "Trust";
    private static final String PLAYER_RELATION_TRUST_SCALE_TAG = "TrustScale";
    private static final String PLAYER_RELATION_PINNED_TAG = "Pinned";
    private static final String PLAYER_RELATION_HIDDEN_TAG = "Hidden";
    private static final String PLAYER_RELATION_DECEASED_ACKNOWLEDGED_TAG = "DeceasedAcknowledged";
    private static final String PLAYER_RELATION_LAST_NO_FUNDS_TICK_TAG = "LastNoFundsTick";
    private static final String PLAYER_RELATION_LAST_ASSAULT_TICK_TAG = "LastAssaultTick";
    private static final String PLAYER_RELATION_ANNOYANCE_TAG = "Annoyance";
    private static final String PLAYER_RELATION_LAST_ANNOYANCE_TICK_TAG = "LastAnnoyanceTick";
    private static final String PLAYER_RELATION_SURCHARGE_UNTIL_TAG = "SurchargeUntil";
    private static final String PLAYER_RELATION_REFUSAL_UNTIL_TAG = "RefusalUntil";
    private static final String PLAYER_RELATION_PURGE_DAY_TAG = "PurgeDay";
    private static final String PLAYER_RELATION_DIALOGUE_REWARDS_TAG = "DialogueRewards";
    private static final String LEGACY_PLAYER_SEEN_TAG = "cyberneticenhancements.fixer_seen";
    private static final String LEGACY_PLAYER_TRUST_TAG = "cyberneticenhancements.fixer_trust";
    private static final String LEGACY_PLAYER_PURGE_DAY_TAG = "cyberneticenhancements.fixer_purge_day";
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_ALERT = 1;
    private static final int STATUS_TRIAGE = 2;
    private static final int STATUS_ENGAGED = 3;
    private static final int TRUST_MAX = 300;
    private static final int TRUST_RECOGNIZED = 60;
    private static final int TRUST_TRUSTED = 135;
    private static final int TRUST_INNER_CIRCLE = 210;
    private static final int TRUST_HOUSE_NAME = 270;
    private static final int TRUST_LOSS_NO_FUNDS = 1;
    private static final int TRUST_LOSS_MISSED_MEETUP = 3;
    private static final int TRUST_LOSS_ASSAULT = 20;
    private static final int ANNOYANCE_NO_FUNDS = 1;
    private static final int ANNOYANCE_MISSED_MEETUP = 2;
    private static final int ANNOYANCE_ASSAULT = 4;
    private static final int ANNOYANCE_SURCHARGE_THRESHOLD = 4;
    private static final int ANNOYANCE_REFUSAL_THRESHOLD = 7;
    private static final EntityDataAccessor<Integer> STATUS =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ON_CALL =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IN_CONVERSATION =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> CONVERSATION_SPOKE_AT =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<String> NPC_CATEGORY =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_APPEARANCE =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_NAME_COLOR =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_BASE_NAME =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<java.util.Optional<UUID>> DEFENDING_AGAINST =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> SELF_DEFENSE_UNTIL =
            SynchedEntityData.defineId(AbstractCityNpcEntity.class, EntityDataSerializers.LONG);
    private static final long MEETUP_WINDOW_TICKS = 30L * 20L;
    private static final long ANNOYANCE_WINDOW_TICKS = 2L * 60L * 20L;
    private static final long NO_FUNDS_PENALTY_COOLDOWN_TICKS = 30L * 20L;
    private static final long SURCHARGE_TICKS = 2L * 60L * 20L;
    private static final long REFUSAL_TICKS = 4L * 60L * 20L;
    private static final long ASSAULT_PENALTY_COOLDOWN_TICKS = 20L;
    public static final long SELF_DEFENSE_TICKS = 5L * 20L;
    private static final float HEAT_SURCHARGE_MULTIPLIER = 3.0F;
    private static final int LOCATE_OUTLINE_TICKS = 10 * 20;

    private @Nullable UUID currentCustomer;
    private @Nullable UUID meetupCustomer;
    private long meetupExpiresAt;
    private long meetupReturnAt;
    private boolean meetupSpent;
    private String savedNicknameOverride = "";
    private String savedAppearanceOverride = "";
    private String savedNameColorOverride = "aqua";
    private @Nullable UUID selfDefenseAttacker;
    private @Nullable BlockPos selfDefenseOrigin;
    private long lastDamageAt;

    protected AbstractCityNpcEntity(EntityType<? extends AbstractCityNpcEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.CITY_NPC;
        setPersistenceRequired();
    }

    public abstract NpcType npcType();

    public boolean canInteract(Player player) {
        return true;
    }

    public String npcTypeId() {
        return npcType().id();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData finalized = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (spawnType == MobSpawnType.SPAWN_EGG && !hasRestriction()) {
            setHome(blockPosition());
        }
        return finalized;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, NpcCombatHealing.goal(this));
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(2, new FixerPlayerFocusGoal(this));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.9D));
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 0.95D, 1.15D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COMBAT_MEDICINE, -1L);
        builder.define(STATUS, STATUS_IDLE);
        builder.define(ON_CALL, false);
        builder.define(IN_CONVERSATION, false);
        builder.define(CONVERSATION_SPOKE_AT, -1L);
        builder.define(NPC_CATEGORY, "");
        builder.define(NPC_APPEARANCE, "");
        builder.define(NPC_NAME_COLOR, dispositionColor(false));
        builder.define(NPC_BASE_NAME, "");
        builder.define(DEFENDING_AGAINST, java.util.Optional.empty());
        builder.define(SELF_DEFENSE_UNTIL, 0L);
    }

    @Override
    public void aiStep() {
        combatHealing.tick();
        if (!level().isClientSide() && de.artemis.cyberneticenhancements.common.world.CityNpcMedicine.downed(this)) {
            setTarget(null); getNavigation().stop(); setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); return;
        }
        if (!level().isClientSide()) updateSelfDefense();
        super.aiStep();
        if (!level().isClientSide() && tickCount == 1) {
            if (!hasRestriction()) {
                setHome(blockPosition());
            }
            ensureIdentity();
        }
        if (!level().isClientSide()) {
            tickMeetupState();
            entityData.set(IN_CONVERSATION, currentCustomer != null);
            if (isAlive() && !combatHealing.usingItem() && getTarget() == null && getHealth() < getMaxHealth()
                    && gameTime() - lastDamageAt >= 20L * 20L && tickCount % 100 == 0) heal(1);
        }
        if (!level().isClientSide() && tickCount % 10 == 0) {
            updateStatus();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!canInteract(player)) return InteractionResult.FAIL;
        if (de.artemis.cyberneticenhancements.common.world.CityNpcMedicine.downed(this)) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            player.stopUsingItem();
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return super.mobInteract(player, hand);
        if (isRefusing(serverPlayer)) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.fixer.cool_off").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        if ((currentCustomer != null && !currentCustomer.equals(player.getUUID()))
                || de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.isOccupiedByOther(serverPlayer, this)) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.fixer.busy").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        if (!hasMet(serverPlayer)) {
            markMet(serverPlayer);
        }
        if (de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager.tryHandInHeldItem(serverPlayer, this, player.getItemInHand(hand))) {
            serverPlayer.stopUsingItem();
            return InteractionResult.CONSUME;
        }
        setCurrentCustomer(player.getUUID());
        if (meetupCustomer != null && meetupCustomer.equals(player.getUUID())) {
            meetupReturnAt = 0L;
            meetupExpiresAt = gameTime() + MEETUP_WINDOW_TICKS;
        }
        setStatus(STATUS_ENGAGED);
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new FixerDialogMenu(containerId, inventory, this),
                getDisplayName()
        );
        serverPlayer.stopUsingItem();
        de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.endForInteraction(serverPlayer, this);
        serverPlayer.openMenu(provider, buffer -> buffer.writeVarInt(getId()));
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.recordAt(serverPlayer, blockPosition(), "contacts", "talked", getDisplayName());
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    public int getBehaviorStatus() {
        return entityData.get(STATUS);
    }

    public void setHome(BlockPos pos) {
        restrictTo(pos.immutable(), 12);
        if (level() instanceof ServerLevel serverLevel) {
            FixerIdentitySavedData.get(serverLevel).updateHome(getUUID(), serverLevel.dimension().location().toString(), pos);
        }
    }

    public void ensureIdentity() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        FixerIdentitySavedData identities = FixerIdentitySavedData.get(serverLevel);
        savedNameColorOverride = dispositionColor(false);
        if (de.artemis.cyberneticenhancements.common.world.NpcAppearanceRules.reserved(savedAppearanceOverride)) savedAppearanceOverride = "";
        if (!savedNicknameOverride.isBlank() || !savedAppearanceOverride.isBlank() || !savedNameColorOverride.isBlank()) {
            identities.getOrAssignIdentity(getUUID(), npcTypeId(), serverLevel.random);
            identities.renameIdentity(getUUID(), savedNicknameOverride);
            identities.setAppearance(getUUID(), savedAppearanceOverride);
            identities.setNameColor(getUUID(), savedNameColorOverride);
        }

        FixerIdentitySavedData.IdentityAssignment identity = identities.getOrAssignIdentity(getUUID(), npcTypeId(), serverLevel.random);
        String color = dispositionColor(false);
        setCustomName(styledName(identity.displayName(), color));
        entityData.set(NPC_CATEGORY, identity.category().id());
        entityData.set(NPC_APPEARANCE, identity.appearance());
        entityData.set(NPC_NAME_COLOR, color);
        entityData.set(NPC_BASE_NAME, identity.name());
        setCustomNameVisible(nameVisibleFor(color));
        if (hasRestriction()) {
            identities.updateHome(getUUID(), serverLevel.dimension().location().toString(), getRestrictCenter());
        }
    }

    /** Gives the NPC a look and name without saving an identity for it, for NPCs that are never contacts. */
    protected void setTransientIdentity(Component name, NpcCategory category) {
        entityData.set(NPC_CATEGORY, category.id());
        entityData.set(NPC_NAME_COLOR, dispositionColor(false));
        entityData.set(NPC_BASE_NAME, name.getString());
        setCustomName(name);
        setCustomNameVisible(true);
    }

    public NpcCategory npcCategory() {
        return NpcCategory.fromId(entityData.get(NPC_CATEGORY));
    }

    public String appearanceOverride() {
        return entityData.get(NPC_APPEARANCE);
    }

    public String nameColorId() {
        return entityData.get(NPC_NAME_COLOR);
    }

    public String nameColorId(ServerPlayer viewer) {
        if (isDefendingAgainst(viewer)) return "red";
        return dispositionColor(de.artemis.cyberneticenhancements.common.world.NpcDisposition.angry(viewer, getUUID()));
    }

    public boolean isDefendingAgainst(Player player) {
        return isAlive() && entityData.get(DEFENDING_AGAINST).filter(player.getUUID()::equals).isPresent();
    }

    public float aggressionRemaining(float partialTick) {
        if (!isAlive() || entityData.get(DEFENDING_AGAINST).isEmpty()) return 0;
        return Math.clamp((entityData.get(SELF_DEFENSE_UNTIL) - gameTime() - partialTick) / selfDefenseTicks(), 0, 1);
    }

    /** How long an NPC stays hostile after the last provocation before it settles back to yellow. */
    protected long selfDefenseTicks() {
        return SELF_DEFENSE_TICKS;
    }

    protected String dispositionColor(boolean angry) {
        return angry ? "yellow" : "aqua";
    }

    public void applyClientDisposition(boolean angry, Player viewer) {
        if (!level().isClientSide() || !hasCustomName()) return;
        String color = isDefendingAgainst(viewer) ? "red" : dispositionColor(angry);
        if (!color.equals(entityData.get(NPC_NAME_COLOR)) || !java.util.Objects.equals(getName().getStyle().getColor(),
                net.minecraft.network.chat.TextColor.fromLegacyFormat(resolveNameColor(color)))) {
            entityData.set(NPC_NAME_COLOR, color);
            setCustomName(styledName(getName().getString(), color));
            setCustomNameVisible(true);
        }
    }

    public String baseName() {
        return entityData.get(NPC_BASE_NAME);
    }

    public boolean hasResolvedIdentity() {
        return hasCustomName() && !entityData.get(NPC_CATEGORY).isBlank();
    }

    public void applyClientPreviewIdentity(String appearanceId, NpcCategory category, String displayName, String nameColorId) {
        entityData.set(NPC_CATEGORY, category.id());
        entityData.set(NPC_APPEARANCE, appearanceId == null ? "" : appearanceId);
        entityData.set(NPC_NAME_COLOR, normalizeNameColorId(nameColorId));
        entityData.set(NPC_BASE_NAME, displayName == null ? "" : displayName);
        setCustomName(styledName(displayName, nameColorId));
        setCustomNameVisible(nameVisibleFor(nameColorId));
    }

    public boolean hasMaxTrust(Player player) {
        return trustPoints(player) >= TRUST_MAX;
    }

    public boolean applyNickname(ServerPlayer player, String nickname) {
        if (!hasMaxTrust(player) || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        String sanitized = nickname == null ? "" : nickname.trim().replaceAll("\\p{Cntrl}", "");
        if (sanitized.length() > 32) {
            sanitized = sanitized.substring(0, 32).trim();
        }
        FixerIdentitySavedData.IdentityAssignment updated = FixerIdentitySavedData.get(serverLevel).renameIdentity(getUUID(), sanitized);
        if (updated == null) {
            return false;
        }
        savedNicknameOverride = sanitized;
        setCustomName(styledName(updated.displayName(), dispositionColor(false)));
        setCustomNameVisible(nameVisibleFor(dispositionColor(false)));
        return true;
    }

    public boolean applyNameColor(ServerPlayer player, String nameColorId) {
        if (!"aqua".equals(nameColorId)) return false;
        if (!hasMaxTrust(player) || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        String normalized = normalizeNameColorId(nameColorId);
        FixerIdentitySavedData.IdentityAssignment updated = FixerIdentitySavedData.get(serverLevel).setNameColor(getUUID(), normalized);
        if (updated == null) {
            return false;
        }
        savedNameColorOverride = updated.nameColorId();
        entityData.set(NPC_NAME_COLOR, dispositionColor(false));
        setCustomName(styledName(updated.displayName(), dispositionColor(false)));
        setCustomNameVisible(nameVisibleFor(dispositionColor(false)));
        return true;
    }

    public boolean applyAppearance(ServerPlayer player, String appearanceId) {
        if (!hasMaxTrust(player) || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        String sanitized = appearanceId == null ? "" : appearanceId.trim();
        if (de.artemis.cyberneticenhancements.common.world.NpcAppearanceRules.reserved(sanitized)) return false;
        FixerIdentitySavedData.IdentityAssignment updated = FixerIdentitySavedData.get(serverLevel).setAppearance(getUUID(), sanitized);
        if (updated == null) {
            return false;
        }
        savedAppearanceOverride = updated.appearance();
        entityData.set(NPC_APPEARANCE, updated.appearance());
        return true;
    }

    public void setCurrentCustomer(@Nullable UUID customerId) {
        this.currentCustomer = customerId;
        if (!level().isClientSide()) entityData.set(IN_CONVERSATION, customerId != null);
    }

    public void clearCustomer(Player player) {
        if (currentCustomer != null && currentCustomer.equals(player.getUUID())) {
            setCurrentCustomer(null);
            if (meetupCustomer != null && meetupCustomer.equals(player.getUUID())) {
                meetupReturnAt = gameTime() + MEETUP_WINDOW_TICKS;
            }
            updateStatus();
        }
    }

    public void beginMeetup(ServerPlayer player) {
        if (level() != player.level()) {
            return;
        }
        meetupCustomer = player.getUUID();
        meetupExpiresAt = gameTime() + MEETUP_WINDOW_TICKS;
        meetupReturnAt = 0L;
        meetupSpent = false;
        currentCustomer = null;
        teleportTo(player.getX() + 1.25D, player.getY(), player.getZ() + 1.25D);
        getNavigation().stop();
        setStatus(STATUS_ENGAGED);
    }

    public int trustLevel(Player player) {
        return trustLevelForPoints(trustPoints(player));
    }

    public static int trustLevelForPoints(int trustPoints) {
        if (trustPoints >= TRUST_HOUSE_NAME) {
            return 4;
        }
        if (trustPoints >= TRUST_INNER_CIRCLE) {
            return 3;
        }
        if (trustPoints >= TRUST_TRUSTED) {
            return 2;
        }
        if (trustPoints >= TRUST_RECOGNIZED) {
            return 1;
        }
        return 0;
    }

    public int trustPoints(Player player) {
        CompoundTag relation = relationTag(player, true);
        int trust = decodedTrustPoints(relation);
        if (relation.getInt(PLAYER_RELATION_TRUST_SCALE_TAG) != TRUST_MAX || relation.getInt(PLAYER_RELATION_TRUST_TAG) != trust) {
            relation.putInt(PLAYER_RELATION_TRUST_TAG, trust);
            relation.putInt(PLAYER_RELATION_TRUST_SCALE_TAG, TRUST_MAX);
            storeRelation(player, relation);
        }
        return trust;
    }

    /** Their trust points as far as the player has earned any, or -1 if they never met; reads only, so looking at someone creates no record. */
    public int knownTrustPoints(Player player) {
        CompoundTag relation = relationTag(player, false);
        return relation.getBoolean(PLAYER_RELATION_SEEN_TAG) ? decodedTrustPoints(relation) : -1;
    }

    public boolean hasMet(Player player) {
        return relationTag(player, true).getBoolean(PLAYER_RELATION_SEEN_TAG);
    }

    public void markMet(Player player) {
        CompoundTag relation = relationTag(player, true);
        relation.putBoolean(PLAYER_RELATION_SEEN_TAG, true);
        storeRelation(player, relation);
    }

    public void raiseTrust(Player player, int amount) {
        int previous = trustPoints(player);
        int trust = previous + Mth.clamp(amount, 0, TRUST_MAX - previous);
        CompoundTag relation = relationTag(player, true);
        relation.putInt(PLAYER_RELATION_TRUST_TAG, trust);
        relation.putInt(PLAYER_RELATION_TRUST_SCALE_TAG, TRUST_MAX);
        storeRelation(player, relation);
        if (player instanceof ServerPlayer serverPlayer && trustLevelForPoints(trust) > trustLevelForPoints(previous)) {
            serverPlayer.playNotifySound(ModSounds.SKILL_ADVANCE.get(), SoundSource.PLAYERS, 0.35F, 1.0F);
            if (level() == serverPlayer.level() && isAlive() && !isInvisibleTo(serverPlayer)) {
                var level = serverPlayer.serverLevel();
                double y = getY() + getBbHeight() + 0.25;
                var dust = new DustParticleOptions(new Vector3f(0.34F, 0.88F, 1.0F), 0.7F);
                level.sendParticles(serverPlayer, dust, false, getX(), y, getZ(), 6, 0.23, 0.13, 0.23, 0.01);
                level.sendParticles(serverPlayer, ParticleTypes.ELECTRIC_SPARK, false,
                        getX(), y, getZ(), 3, 0.19, 0.10, 0.19, 0.025);
            }
        }
    }

    public void lowerTrust(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        int trust = Mth.clamp(trustPoints(player) - amount, 0, TRUST_MAX);
        CompoundTag relation = relationTag(player, true);
        relation.putInt(PLAYER_RELATION_TRUST_TAG, trust);
        relation.putInt(PLAYER_RELATION_TRUST_SCALE_TAG, TRUST_MAX);
        storeRelation(player, relation);
    }

    public void recordPaidTransaction(Player player) {
        if (meetupCustomer != null && meetupCustomer.equals(player.getUUID())) {
            meetupSpent = true;
        }
    }

    public void rewardCommerceTrust(Player player, int amount) {
        CompoundTag relation = relationTag(player, true);
        long day = level().getDayTime() / 24000L;
        int used = relation.getLong("CommerceTrustDay") == day ? relation.getInt("CommerceTrustEarned") : 0;
        int gain = Math.min(Math.max(0, amount), Math.max(0,
                de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("trust.commerce_daily_cap") - used));
        relation.putLong("CommerceTrustDay", day);
        relation.putInt("CommerceTrustEarned", used + gain);
        storeRelation(player, relation);
        if (gain > 0) raiseTrust(player, gain);
    }

    public static void copyRelations(Player original, Player clone) {
        for (String key : java.util.List.of(PLAYER_NPC_RELATIONS_TAG, LEGACY_PLAYER_FIXER_RELATIONS_TAG,
                LEGACY_PLAYER_SEEN_TAG, LEGACY_PLAYER_TRUST_TAG, LEGACY_PLAYER_PURGE_DAY_TAG)) {
            if (original.getPersistentData().contains(key)) clone.getPersistentData().put(key, original.getPersistentData().get(key).copy());
        }
    }

    public void penalizeNoFundsAttempt(Player player) {
        applyTrustLossWithCooldown(player, PLAYER_RELATION_LAST_NO_FUNDS_TICK_TAG, NO_FUNDS_PENALTY_COOLDOWN_TICKS, TRUST_LOSS_NO_FUNDS);
        registerAnnoyance(player, ANNOYANCE_NO_FUNDS);
    }

    public boolean hasHeatSurcharge(Player player) {
        return relationTag(player, false).getLong(PLAYER_RELATION_SURCHARGE_UNTIL_TAG) > gameTime();
    }

    public boolean isRefusing(Player player) {
        return isDefendingAgainst(player) || relationTag(player, false).getLong(PLAYER_RELATION_REFUSAL_UNTIL_TAG) > gameTime();
    }

    public boolean isAnnoyed(Player player) {
        CompoundTag relation = relationTag(player, false);
        return isRefusing(player) || hasHeatSurcharge(player) || (relation.getInt(PLAYER_RELATION_ANNOYANCE_TAG) > 0
                && gameTime() - relation.getLong(PLAYER_RELATION_LAST_ANNOYANCE_TICK_TAG) <= ANNOYANCE_WINDOW_TICKS);
    }

    public boolean isOnCall() { return entityData.get(ON_CALL); }

    public void setOnCall(boolean onCall) { entityData.set(ON_CALL, onCall); }

    public boolean isInConversation() { return entityData.get(IN_CONVERSATION); }

    public long conversationSpokeAt() { return entityData.get(CONVERSATION_SPOKE_AT); }

    public void noteConversationReply() {
        if (!level().isClientSide()) entityData.set(CONVERSATION_SPOKE_AT, level().getGameTime());
    }

    public float priceMultiplier(Player player) {
        return hasHeatSurcharge(player) ? HEAT_SURCHARGE_MULTIPLIER : 1.0F;
    }

    private static int decodedTrustPoints(CompoundTag relation) {
        int stored = relation.getInt(PLAYER_RELATION_TRUST_TAG);
        if (relation.getInt(PLAYER_RELATION_TRUST_SCALE_TAG) == TRUST_MAX) {
            return Mth.clamp(stored, 0, TRUST_MAX);
        }
        if (stored <= 4) {
            return switch (stored) {
                case 4 -> TRUST_MAX;
                case 3 -> 225;
                case 2 -> 150;
                case 1 -> 75;
                default -> 0;
            };
        }
        if (stored <= 100) {
            return Mth.clamp(Math.round(stored * (TRUST_MAX / 100.0F)), 0, TRUST_MAX);
        }
        return Mth.clamp(stored, 0, TRUST_MAX);
    }

    public long lastPurgeDay(Player player) {
        CompoundTag relation = relationTag(player, true);
        return relation.contains(PLAYER_RELATION_PURGE_DAY_TAG) ? relation.getLong(PLAYER_RELATION_PURGE_DAY_TAG) : -1L;
    }

    public boolean isPinned(Player player) {
        return relationTag(player, true).getBoolean(PLAYER_RELATION_PINNED_TAG);
    }

    public boolean isHidden(Player player) {
        return relationTag(player, true).getBoolean(PLAYER_RELATION_HIDDEN_TAG);
    }

    public void setPinned(Player player, boolean pinned) {
        CompoundTag relation = relationTag(player, true);
        relation.putBoolean(PLAYER_RELATION_PINNED_TAG, pinned);
        storeRelation(player, relation);
    }

    public void setHidden(Player player, boolean hidden) {
        CompoundTag relation = relationTag(player, true);
        relation.putBoolean(PLAYER_RELATION_HIDDEN_TAG, hidden);
        storeRelation(player, relation);
    }

    public void setLastPurgeDay(Player player, long day) {
        CompoundTag relation = relationTag(player, true);
        relation.putLong(PLAYER_RELATION_PURGE_DAY_TAG, day);
        storeRelation(player, relation);
    }

    public boolean grantDialogueTrustOnce(Player player, String rewardKey, int amount) {
        if (amount <= 0 || rewardKey == null || rewardKey.isBlank()) {
            return false;
        }
        CompoundTag relation = relationTag(player, true);
        CompoundTag rewards = relation.contains(PLAYER_RELATION_DIALOGUE_REWARDS_TAG, Tag.TAG_COMPOUND)
                ? relation.getCompound(PLAYER_RELATION_DIALOGUE_REWARDS_TAG)
                : new CompoundTag();
        if (rewards.getBoolean(rewardKey)) {
            return false;
        }
        rewards.putBoolean(rewardKey, true);
        relation.put(PLAYER_RELATION_DIALOGUE_REWARDS_TAG, rewards);
        storeRelation(player, relation);
        raiseTrust(player, amount);
        return true;
    }

    public void applyDialoguePenalty(ServerPlayer player, int trustLoss, int annoyance, boolean closeConversation) {
        if (trustLoss > 0) {
            lowerTrust(player, trustLoss);
        }
        if (annoyance > 0) {
            registerAnnoyance(player, annoyance);
        }
        if (closeConversation) {
            terminateConversation(player);
        }
    }

    public void terminateConversation(ServerPlayer player) {
        if (currentCustomer != null && currentCustomer.equals(player.getUUID())) {
            currentCustomer = null;
        }
        if (meetupCustomer != null && meetupCustomer.equals(player.getUUID())) {
            meetupReturnAt = gameTime() + MEETUP_WINDOW_TICKS;
        }
        if (player.containerMenu instanceof de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu menu
                && menu.entityId() == getId()) {
            player.closeContainer();
        }
        updateStatus();
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        boolean hurt = super.hurt(damageSource, amount);
        if (!hurt || level().isClientSide()) {
            return hurt;
        }
        lastDamageAt = gameTime();
        combatHealing.damaged(damageSource);
        de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.interruptForNpc(this, !isAlive());
        if (damageSource.getEntity() instanceof Player player) {
            applyTrustLossWithCooldown(player, PLAYER_RELATION_LAST_ASSAULT_TICK_TAG, ASSAULT_PENALTY_COOLDOWN_TICKS, TRUST_LOSS_ASSAULT);
            registerAnnoyance(player, ANNOYANCE_ASSAULT);
            if (player instanceof ServerPlayer attacker) {
                de.artemis.cyberneticenhancements.common.world.NpcDisposition.anger(attacker, getUUID());
                if (isAlive() && (angersOnFirstHit() || getHealth() < getMaxHealth() * 0.25F)) defendAgainst(attacker);
            }
        }
        return hurt;
    }

    /** True for NPCs that fight back from the very first blow instead of waiting until they are nearly dead. */
    protected boolean angersOnFirstHit() {
        return false;
    }

    protected void defendAgainst(ServerPlayer attacker) {
        defendAgainst(attacker, true);
    }

    /** {@code sound} is whether turning hostile is announced; a group set on someone announces it once, not per member. */
    protected void defendAgainst(ServerPlayer attacker, boolean sound) {
        if (attacker.isCreative() || attacker.isSpectator()) return;
        UUID previous = selfDefenseAttacker;
        boolean turningHostile = "yellow".equals(nameColorId(attacker));
        setTarget(attacker);
        if (getTarget() == attacker) {
            selfDefenseAttacker = attacker.getUUID();
            selfDefenseOrigin = blockPosition();
            entityData.set(SELF_DEFENSE_UNTIL, gameTime() + selfDefenseTicks());
            entityData.set(DEFENDING_AGAINST, java.util.Optional.of(attacker.getUUID()));
            if (turningHostile && sound) attacker.playNotifySound(
                    de.artemis.cyberneticenhancements.common.registry.ModSounds.NPC_AGGRESSION.get(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.08F, 1.0F);
            if (attacker.containerMenu instanceof FixerDialogMenu menu && menu.entityId() == getId()) attacker.closeContainer();
            syncDefenseContact(previous);
            syncDefenseContact(attacker.getUUID());
        }
    }

    protected void updateSelfDefense() {
        var target = getTarget();
        if (target == null && selfDefenseAttacker == null) return;
        if (target != null && isAlive() && (angersOnFirstHit() || getHealth() < getMaxHealth() * 0.25F) && gameTime() < entityData.get(SELF_DEFENSE_UNTIL)
                && target.isAlive() && target.level() == level() && target.getUUID().equals(selfDefenseAttacker)
                && selfDefenseOrigin != null && target.blockPosition().closerThan(selfDefenseOrigin, 16)
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))) return;
        UUID previous = selfDefenseAttacker;
        setTarget(null);
        setAggressive(false);
        getNavigation().stop();
        selfDefenseAttacker = null;
        selfDefenseOrigin = null;
        entityData.set(SELF_DEFENSE_UNTIL, 0L);
        entityData.set(DEFENDING_AGAINST, java.util.Optional.empty());
        syncDefenseContact(previous);
        // Calming down is announced the way turning hostile is, quietly and only to the one it concerned.
        if (previous != null && level() instanceof ServerLevel serverLevel) {
            var player = serverLevel.getServer().getPlayerList().getPlayer(previous);
            if (player != null && player.isAlive() && "yellow".equals(nameColorId(player))) player.playNotifySound(
                    de.artemis.cyberneticenhancements.common.registry.ModSounds.NPC_AGGRESSION_LOST.get(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.08F, 1.0F);
        }
    }

    private void syncDefenseContact(@Nullable UUID playerId) {
        if (playerId == null || !(level() instanceof ServerLevel serverLevel)) return;
        var player = serverLevel.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) PacketDistributor.sendToPlayer(player, de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.capture(player));
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        boolean wasDead = dead;
        super.die(source);
        if (!wasDead && dead && !level().isClientSide()) {
            // Passing street NPCs are fair game: no MaxTec response, and no identity record to mark.
            if (keepsContactRecord()) {
                de.artemis.cyberneticenhancements.common.world.MaxTacResponse.onNpcKilled(this, source);
                FixerIdentitySavedData.get((ServerLevel) level()).markDeceased(getUUID());
            }
            de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.interruptForNpc(this, true);
            for (var player : ((ServerLevel) level()).getServer().getPlayerList().getPlayers()) {
                if (archiveContacts(player).stream().anyMatch(contact -> contact.npcId().equals(getUUID()))) {
                    PacketDistributor.sendToPlayer(player, de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.capture(player));
                }
            }
        }
    }

    @Nullable
    public Player findPriorityPatient() {
        if (!npcType().usesPatientFocus()) {
            return null;
        }
        List<Player> players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(10.0D, 4.0D, 10.0D), Player::isAlive);
        Player best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Player candidate : players) {
            if (isAnnoyed(candidate)) {
                continue;
            }
            PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(candidate);
            int effectiveCyberstrain = CyberstrainManager.getEffectiveCyberstrain(candidate, inventory);
            int score = effectiveCyberstrain;
            if (CyberstrainManager.isPsychosisActive(candidate)) {
                score += 200;
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return bestScore >= 70 ? best : null;
    }

    @Nullable
    public Player findFocusTarget() {
        if (currentCustomer != null) {
            Player customer = level().getPlayerByUUID(currentCustomer);
            if (customer != null && customer.isAlive() && distanceToSqr(customer) <= 144.0D) {
                return customer;
            }
        }

        Player priorityPatient = findPriorityPatient();
        if (priorityPatient != null) {
            return priorityPatient;
        }

        List<Player> players = level().getEntitiesOfClass(
                Player.class,
                getBoundingBox().inflate(12.0D, 4.0D, 12.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator() && !isAnnoyed(candidate)
        );
        Player best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Player candidate : players) {
            int score = scoreFocusTarget(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return bestScore >= 24 ? best : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        combatHealing.save(tag);
        if (hasRestriction()) {
            BlockPos center = getRestrictCenter();
            tag.putBoolean(HAS_HOME_TAG, true);
            tag.putInt(HOME_POS_X_TAG, center.getX());
            tag.putInt(HOME_POS_Y_TAG, center.getY());
            tag.putInt(HOME_POS_Z_TAG, center.getZ());
        }
        if (currentCustomer != null) {
            tag.putUUID(CURRENT_CUSTOMER_TAG, currentCustomer);
        }
        if (meetupCustomer != null) {
            tag.putUUID(MEETUP_CUSTOMER_TAG, meetupCustomer);
        }
        tag.putLong(MEETUP_EXPIRES_AT_TAG, meetupExpiresAt);
        tag.putLong(MEETUP_RETURN_AT_TAG, meetupReturnAt);
        tag.putBoolean(MEETUP_SPENT_TAG, meetupSpent);
        tag.putString(NPC_CATEGORY_TAG, entityData.get(NPC_CATEGORY));
        tag.putString(NPC_APPEARANCE_TAG, entityData.get(NPC_APPEARANCE));
        tag.putString(NPC_NAME_COLOR_TAG, entityData.get(NPC_NAME_COLOR));
        tag.putString(NPC_BASE_NAME_TAG, entityData.get(NPC_BASE_NAME));
        if (!savedNicknameOverride.isBlank()) {
            tag.putString(NPC_NICKNAME_TAG, savedNicknameOverride);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        combatHealing.load(tag);
        selfDefenseAttacker = null;
        selfDefenseOrigin = null;
        entityData.set(SELF_DEFENSE_UNTIL, 0L);
        lastDamageAt = gameTime();
        entityData.set(DEFENDING_AGAINST, java.util.Optional.empty());
        setTarget(null);
        if (tag.getBoolean(HAS_HOME_TAG)) {
            restrictTo(new BlockPos(tag.getInt(HOME_POS_X_TAG), tag.getInt(HOME_POS_Y_TAG), tag.getInt(HOME_POS_Z_TAG)), 12);
        }
        currentCustomer = tag.hasUUID(CURRENT_CUSTOMER_TAG) ? tag.getUUID(CURRENT_CUSTOMER_TAG) : null;
        meetupCustomer = tag.hasUUID(MEETUP_CUSTOMER_TAG) ? tag.getUUID(MEETUP_CUSTOMER_TAG) : null;
        meetupExpiresAt = tag.getLong(MEETUP_EXPIRES_AT_TAG);
        meetupReturnAt = tag.getLong(MEETUP_RETURN_AT_TAG);
        meetupSpent = tag.getBoolean(MEETUP_SPENT_TAG);
        entityData.set(NPC_CATEGORY, NpcCategory.fromId(tag.getString(NPC_CATEGORY_TAG)).id());
        entityData.set(NPC_APPEARANCE, tag.getString(NPC_APPEARANCE_TAG));
        entityData.set(NPC_NAME_COLOR, dispositionColor(false));
        if (hasCustomName()) setCustomName(styledName(getName().getString(), dispositionColor(false)));
        entityData.set(NPC_BASE_NAME, tag.getString(NPC_BASE_NAME_TAG));
        savedNicknameOverride = tag.contains(NPC_NICKNAME_TAG, Tag.TAG_STRING) ? tag.getString(NPC_NICKNAME_TAG) : "";
        savedAppearanceOverride = entityData.get(NPC_APPEARANCE);
        savedNameColorOverride = entityData.get(NPC_NAME_COLOR);
        if (!savedNicknameOverride.isBlank()) {
            setCustomName(styledName(savedNicknameOverride, entityData.get(NPC_NAME_COLOR)));
        }
        refreshDisplayedName();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (NPC_NAME_COLOR.equals(key) || NPC_BASE_NAME.equals(key)) {
            refreshDisplayedName();
        }
    }

    private void refreshDisplayedName() {
        String displayName = currentDisplayNameText();
        if (!displayName.isBlank()) {
            setCustomName(styledName(displayName, entityData.get(NPC_NAME_COLOR)));
        }
        setCustomNameVisible(nameVisibleFor(entityData.get(NPC_NAME_COLOR)));
    }

    private String currentDisplayNameText() {
        Component customName = getCustomName();
        if (customName != null && !customName.getString().isBlank()) {
            return customName.getString();
        }
        return entityData.get(NPC_BASE_NAME);
    }

    private static Component styledName(String displayName, String nameColorId) {
        return Component.literal(displayName).withStyle(resolveNameColor(nameColorId));
    }

    private static ChatFormatting resolveNameColor(String nameColorId) {
        return switch (normalizeNameColorId(nameColorId)) {
            case "yellow" -> ChatFormatting.YELLOW;
            case "blue" -> ChatFormatting.BLUE;
            case "gold" -> ChatFormatting.GOLD;
            case "green" -> ChatFormatting.GREEN;
            case "red" -> ChatFormatting.RED;
            case "gray" -> ChatFormatting.GRAY;
            case "white" -> ChatFormatting.WHITE;
            case "light_purple" -> ChatFormatting.LIGHT_PURPLE;
            case "hidden" -> ChatFormatting.WHITE;
            default -> ChatFormatting.AQUA;
        };
    }

    private static String normalizeNameColorId(String nameColorId) {
        return switch (nameColorId == null ? "" : nameColorId.trim().toLowerCase()) {
            case "blue", "gold", "green", "red", "yellow", "white", "light_purple", "gray", "hidden" -> nameColorId.trim().toLowerCase();
            default -> "aqua";
        };
    }

    private static boolean nameVisibleFor(String nameColorId) {
        return !"hidden".equals(normalizeNameColorId(nameColorId));
    }

    @Override
    public boolean shouldShowName() {
        return nameVisibleFor(entityData.get(NPC_NAME_COLOR)) && super.shouldShowName();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState blockState) {
    }

    private void updateStatus() {
        if (currentCustomer != null) {
            Player customer = level().getPlayerByUUID(currentCustomer);
            if (customer != null && customer.isAlive() && customer.distanceToSqr(this) <= 64.0D) {
                setStatus(STATUS_ENGAGED);
                return;
            }
            currentCustomer = null;
        }

        if (!level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(8.0D), Monster::isAlive).isEmpty()) {
            setStatus(STATUS_ALERT);
            return;
        }

        if (findPriorityPatient() != null) {
            setStatus(STATUS_TRIAGE);
            return;
        }

        if (findFocusTarget() != null) {
            setStatus(STATUS_ENGAGED);
            return;
        }

        setStatus(STATUS_IDLE);
    }

    private void tickMeetupState() {
        if (meetupCustomer == null) {
            return;
        }
        long now = gameTime();
        if (currentCustomer != null && currentCustomer.equals(meetupCustomer)) {
            meetupExpiresAt = now + MEETUP_WINDOW_TICKS;
            meetupReturnAt = 0L;
            return;
        }
        if (meetupReturnAt > 0L && now >= meetupReturnAt) {
            returnHomeFromMeetup();
            return;
        }
        if (meetupReturnAt == 0L && meetupExpiresAt > 0L && now >= meetupExpiresAt) {
            returnHomeFromMeetup();
        }
    }

    private void returnHomeFromMeetup() {
        Player meetupPlayer = meetupCustomer == null ? null : level().getPlayerByUUID(meetupCustomer);
        if (meetupPlayer != null && !meetupSpent) {
            lowerTrust(meetupPlayer, TRUST_LOSS_MISSED_MEETUP);
            registerAnnoyance(meetupPlayer, ANNOYANCE_MISSED_MEETUP);
        }
        meetupCustomer = null;
        meetupExpiresAt = 0L;
        meetupReturnAt = 0L;
        meetupSpent = false;
        if (hasRestriction()) {
            BlockPos home = getRestrictCenter();
            teleportTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        }
        getNavigation().stop();
        currentCustomer = null;
        updateStatus();
    }

    private void setStatus(int status) {
        entityData.set(STATUS, status);
    }

    private long gameTime() {
        return level().getGameTime();
    }

    private void registerAnnoyance(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        CompoundTag relation = relationTag(player, true);
        long now = gameTime();
        long lastAnnoyanceAt = relation.getLong(PLAYER_RELATION_LAST_ANNOYANCE_TICK_TAG);
        int annoyance = now - lastAnnoyanceAt > ANNOYANCE_WINDOW_TICKS ? 0 : relation.getInt(PLAYER_RELATION_ANNOYANCE_TAG);
        annoyance = Mth.clamp(annoyance + amount, 0, 32);
        relation.putInt(PLAYER_RELATION_ANNOYANCE_TAG, annoyance);
        relation.putLong(PLAYER_RELATION_LAST_ANNOYANCE_TICK_TAG, now);
        if (annoyance >= ANNOYANCE_REFUSAL_THRESHOLD) {
            relation.putLong(PLAYER_RELATION_REFUSAL_UNTIL_TAG, now + REFUSAL_TICKS);
            relation.putLong(PLAYER_RELATION_SURCHARGE_UNTIL_TAG, Math.max(relation.getLong(PLAYER_RELATION_SURCHARGE_UNTIL_TAG), now + SURCHARGE_TICKS));
        } else if (annoyance >= ANNOYANCE_SURCHARGE_THRESHOLD) {
            relation.putLong(PLAYER_RELATION_SURCHARGE_UNTIL_TAG, now + SURCHARGE_TICKS);
        }
        storeRelation(player, relation);

        UUID playerId = player.getUUID();
        if (currentCustomer != null && currentCustomer.equals(playerId) && isRefusing(player)) {
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof FixerDialogMenu menu
                    && menu.entityId() == getId()) {
                serverPlayer.closeContainer();
            }
            currentCustomer = null;
            if (meetupCustomer != null && meetupCustomer.equals(playerId)) {
                meetupReturnAt = gameTime();
            }
            updateStatus();
        }
    }

    private void applyTrustLossWithCooldown(Player player, String tickTag, long cooldownTicks, int amount) {
        CompoundTag relation = relationTag(player, true);
        long now = gameTime();
        long lastPenaltyAt = relation.getLong(tickTag);
        if (cooldownTicks > 0L && now - lastPenaltyAt < cooldownTicks) {
            return;
        }
        relation.putLong(tickTag, now);
        storeRelation(player, relation);
        lowerTrust(player, amount);
    }

    /** Temporary NPCs return false: nothing about them (met, trust, purge day) is written to the player, so they never become contacts. */
    protected boolean keepsContactRecord() {
        return true;
    }

    /** Whether this NPC is a real contact the player builds trust with, rather than a passing or encounter NPC. */
    public boolean isContact() {
        return keepsContactRecord() && npcType() != NpcType.MAXTEC;
    }

    private CompoundTag relationTag(Player player, boolean create) {
        if (!keepsContactRecord()) return new CompoundTag();
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag allRelations = persistentData.contains(PLAYER_NPC_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(PLAYER_NPC_RELATIONS_TAG)
                : persistentData.contains(LEGACY_PLAYER_FIXER_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(LEGACY_PLAYER_FIXER_RELATIONS_TAG)
                : new CompoundTag();
        String fixerKey = getStringUUID();
        CompoundTag relation = allRelations.contains(fixerKey, Tag.TAG_COMPOUND)
                ? allRelations.getCompound(fixerKey)
                : new CompoundTag();

        if (relation.isEmpty() && usesLegacyRelationData(persistentData)) {
            relation.putBoolean(PLAYER_RELATION_SEEN_TAG, persistentData.getBoolean(LEGACY_PLAYER_SEEN_TAG));
            relation.putInt(PLAYER_RELATION_TRUST_TAG, persistentData.getInt(LEGACY_PLAYER_TRUST_TAG));
            relation.putLong(PLAYER_RELATION_PURGE_DAY_TAG, persistentData.getLong(LEGACY_PLAYER_PURGE_DAY_TAG));
            if (create) {
                allRelations.put(fixerKey, relation);
                persistentData.put(PLAYER_NPC_RELATIONS_TAG, allRelations);
            }
        } else if (create && !allRelations.contains(fixerKey, Tag.TAG_COMPOUND)) {
            allRelations.put(fixerKey, relation);
            persistentData.put(PLAYER_NPC_RELATIONS_TAG, allRelations);
        }
        return relation;
    }

    private void storeRelation(Player player, CompoundTag relation) {
        if (!keepsContactRecord()) return;
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag allRelations = persistentData.contains(PLAYER_NPC_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(PLAYER_NPC_RELATIONS_TAG)
                : persistentData.contains(LEGACY_PLAYER_FIXER_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(LEGACY_PLAYER_FIXER_RELATIONS_TAG)
                : new CompoundTag();
        allRelations.put(getStringUUID(), relation);
        persistentData.put(PLAYER_NPC_RELATIONS_TAG, allRelations);
    }

    private boolean usesLegacyRelationData(CompoundTag persistentData) {
        return persistentData.contains(LEGACY_PLAYER_SEEN_TAG)
                || persistentData.contains(LEGACY_PLAYER_TRUST_TAG)
                || persistentData.contains(LEGACY_PLAYER_PURGE_DAY_TAG);
    }

    public static List<ArchiveContact> archiveContacts(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(PLAYER_NPC_RELATIONS_TAG, Tag.TAG_COMPOUND)
                && !persistentData.contains(LEGACY_PLAYER_FIXER_RELATIONS_TAG, Tag.TAG_COMPOUND)) {
            return List.of();
        }

        CompoundTag allRelations = persistentData.contains(PLAYER_NPC_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(PLAYER_NPC_RELATIONS_TAG)
                : persistentData.getCompound(LEGACY_PLAYER_FIXER_RELATIONS_TAG);
        FixerIdentitySavedData identityData = FixerIdentitySavedData.get(player.serverLevel());
        long currentDay = player.level().getDayTime() / 24000L;
        long now = player.level().getGameTime();
        List<ArchiveContact> contacts = new ArrayList<>();

        for (String fixerKey : allRelations.getAllKeys()) {
            if (!allRelations.contains(fixerKey, Tag.TAG_COMPOUND)) {
                continue;
            }

            CompoundTag relation = allRelations.getCompound(fixerKey);
            if (!relation.getBoolean(PLAYER_RELATION_SEEN_TAG)) {
                continue;
            }

            UUID fixerId;
            try {
                fixerId = UUID.fromString(fixerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            FixerIdentitySavedData.IdentityAssignment identity = identityData.identity(fixerId);
            if (identity == null || identity.name().isBlank()) {
                continue;
            }

            contacts.add(new ArchiveContact(
                    fixerId,
                    identity.npcTypeId().isBlank() ? "fixer" : identity.npcTypeId(),
                    identity.displayName(),
                    identity.category(),
                    Mth.clamp(new TrustAdapter(relation).trustLevel(), 0, 4),
                    relation.getBoolean(PLAYER_RELATION_PINNED_TAG),
                    relation.getBoolean(PLAYER_RELATION_HIDDEN_TAG),
                    (!relation.contains(PLAYER_RELATION_PURGE_DAY_TAG) || relation.getLong(PLAYER_RELATION_PURGE_DAY_TAG) < currentDay),
                    relation.getLong(PLAYER_RELATION_SURCHARGE_UNTIL_TAG) > now,
                    relation.getLong(PLAYER_RELATION_REFUSAL_UNTIL_TAG) > now,
                    identity.dimensionId(),
                    identity.homePos(),
                    identityData.isDeceased(fixerId)
            ));
        }

        contacts.sort(Comparator
                .comparing(ArchiveContact::npcTypeId)
                .thenComparing(ArchiveContact::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(contacts);
    }

    public static ArchiveContactLocation archiveContactLocation(ServerPlayer player, UUID npcId, String fallbackDimensionId, @Nullable BlockPos fallbackHomePos) {
        AbstractCityNpcEntity npc = resolveArchiveNpc(player, npcId);
        if (npc != null) {
            BlockPos currentPos = npc.blockPosition();
            return new ArchiveContactLocation(npc.level().dimension().location().toString(), currentPos);
        }
        return new ArchiveContactLocation(fallbackDimensionId == null ? "" : fallbackDimensionId, fallbackHomePos);
    }

    /** Puts an NPC in the player's contacts only for as long as a quest needs them to be reachable. */
    public static void openTemporaryContact(ServerPlayer player, AbstractCityNpcEntity npc, String name) {
        var identities = FixerIdentitySavedData.get(player.serverLevel());
        identities.getOrAssignIdentity(npc.getUUID(), npc.npcTypeId(), player.serverLevel().random);
        identities.updateHome(npc.getUUID(), npc.level().dimension().location().toString(), npc.blockPosition());
        identities.renameIdentity(npc.getUUID(), name);
        CompoundTag relation = archiveRelationTag(player, npc.getUUID(), true);
        relation.putBoolean(PLAYER_RELATION_SEEN_TAG, true);
        relation.putBoolean(PLAYER_RELATION_TEMPORARY_TAG, true);
        storeArchiveRelation(player, npc.getUUID(), relation);
        PacketDistributor.sendToPlayer(player, de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.capture(player));
    }

    /** Takes a temporary contact away again; a contact the player has otherwise made is never touched. */
    public static void closeTemporaryContact(ServerPlayer player, UUID npcId) {
        CompoundTag relation = archiveRelationTag(player, npcId, false);
        if (relation == null || !relation.getBoolean(PLAYER_RELATION_TEMPORARY_TAG)) return;
        CompoundTag all = player.getPersistentData().getCompound(PLAYER_NPC_RELATIONS_TAG);
        all.remove(npcId.toString());
        player.getPersistentData().put(PLAYER_NPC_RELATIONS_TAG, all);
        PacketDistributor.sendToPlayer(player, de.artemis.cyberneticenhancements.common.network.ArchiveContactsPayload.capture(player));
    }

    public static AbstractCityNpcEntity resolveCallContact(ServerPlayer player, UUID npcId) {
        if (FixerIdentitySavedData.get(player.serverLevel()).isDeceased(npcId)) return null;
        CompoundTag relation = archiveRelationTag(player, npcId, false);
        if (relation == null || !relation.getBoolean(PLAYER_RELATION_SEEN_TAG)) return null;
        AbstractCityNpcEntity npc = resolveArchiveNpc(player, npcId);
        if (npc == null || npc.isRemoved() || !npc.isAlive() || !npc.canInteract(player)) return null;
        npc.ensureIdentity();
        return npc;
    }

    public boolean canTakeCall(Player player) {
        if (de.artemis.cyberneticenhancements.common.world.CityNpcMedicine.downed(this)) return false;
        return canInteract(player) && isAlive() && !isRemoved() && !isRefusing(player)
                && (currentCustomer == null || currentCustomer.equals(player.getUUID()))
                && (meetupCustomer == null || meetupCustomer.equals(player.getUUID()));
    }

    public static boolean locateFromArchive(ServerPlayer player, UUID npcId) {
        AbstractCityNpcEntity npc = resolveArchiveNpc(player, npcId);
        if (npc == null || npc.level() != player.level()) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, new FaceHazardHighlightPayload(List.of(), List.of(npc.getUUID()), LOCATE_OUTLINE_TICKS, true));
        return true;
    }

    public static boolean summonFromArchive(ServerPlayer player, UUID npcId) {
        AbstractCityNpcEntity npc = resolveArchiveNpc(player, npcId);
        if (npc == null || npc.level() != player.level()) {
            return false;
        }
        if (npc.isRefusing(player)) {
            return false;
        }
        if (npc.currentCustomer != null && !npc.currentCustomer.equals(player.getUUID())) {
            return false;
        }
        npc.beginMeetup(player);
        return true;
    }

    public static boolean togglePinnedFromArchive(ServerPlayer player, UUID fixerId) {
        CompoundTag relation = archiveRelationTag(player, fixerId, false);
        if (relation == null || !relation.getBoolean(PLAYER_RELATION_SEEN_TAG)) {
            return false;
        }
        relation.putBoolean(PLAYER_RELATION_PINNED_TAG, !relation.getBoolean(PLAYER_RELATION_PINNED_TAG));
        storeArchiveRelation(player, fixerId, relation);
        return true;
    }

    public static boolean toggleHiddenFromArchive(ServerPlayer player, UUID fixerId) {
        CompoundTag relation = archiveRelationTag(player, fixerId, false);
        if (relation == null || !relation.getBoolean(PLAYER_RELATION_SEEN_TAG)) {
            return false;
        }
        boolean hidden = !relation.getBoolean(PLAYER_RELATION_HIDDEN_TAG);
        if (FixerIdentitySavedData.get(player.serverLevel()).isDeceased(fixerId)) {
            relation.putBoolean(PLAYER_RELATION_DECEASED_ACKNOWLEDGED_TAG, true);
        }
        relation.putBoolean(PLAYER_RELATION_HIDDEN_TAG, hidden);
        if (hidden) {
            relation.putBoolean(PLAYER_RELATION_PINNED_TAG, false);
        }
        storeArchiveRelation(player, fixerId, relation);
        return true;
    }

    public static boolean acknowledgeDeceasedFromArchive(ServerPlayer player, UUID npcId) {
        CompoundTag relation = archiveRelationTag(player, npcId, false);
        if (relation == null || !relation.getBoolean(PLAYER_RELATION_SEEN_TAG)
                || relation.getBoolean(PLAYER_RELATION_DECEASED_ACKNOWLEDGED_TAG)
                || !FixerIdentitySavedData.get(player.serverLevel()).isDeceased(npcId)) return false;
        relation.putBoolean(PLAYER_RELATION_DECEASED_ACKNOWLEDGED_TAG, true);
        relation.putBoolean(PLAYER_RELATION_HIDDEN_TAG, true);
        relation.putBoolean(PLAYER_RELATION_PINNED_TAG, false);
        storeArchiveRelation(player, npcId, relation);
        return true;
    }

    public static boolean lowerTrustFromArchive(ServerPlayer player, UUID fixerId, int amount) {
        if (amount <= 0) {
            return false;
        }
        CompoundTag relation = archiveRelationTag(player, fixerId, false);
        if (relation == null) {
            return false;
        }
        int trust = Mth.clamp(decodedTrustPoints(relation) - amount, 0, TRUST_MAX);
        relation.putInt(PLAYER_RELATION_TRUST_TAG, trust);
        relation.putInt(PLAYER_RELATION_TRUST_SCALE_TAG, TRUST_MAX);
        storeArchiveRelation(player, fixerId, relation);
        return true;
    }

    @Nullable
    private static AbstractCityNpcEntity resolveArchiveNpc(ServerPlayer player, UUID fixerId) {
        for (ServerLevel level : player.server.getAllLevels()) {
            if (level.getEntity(fixerId) instanceof AbstractCityNpcEntity npc) {
                return npc;
            }
        }

        FixerIdentitySavedData.IdentityAssignment identity = FixerIdentitySavedData.get(player.serverLevel()).identity(fixerId);
        if (identity == null || identity.homePos() == null || identity.dimensionId().isBlank()) {
            return null;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(identity.dimensionId()));
        ServerLevel level = player.server.getLevel(dimensionKey);
        if (level == null) {
            return null;
        }

        BlockPos homePos = identity.homePos();
        level.getChunkAt(homePos);
        List<AbstractCityNpcEntity> candidates = level.getEntitiesOfClass(AbstractCityNpcEntity.class, new AABB(homePos).inflate(64.0D), candidate -> candidate.getUUID().equals(fixerId));
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    private static @Nullable CompoundTag archiveRelationTag(Player player, UUID fixerId, boolean create) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag allRelations = persistentData.contains(PLAYER_NPC_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(PLAYER_NPC_RELATIONS_TAG)
                : persistentData.contains(LEGACY_PLAYER_FIXER_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(LEGACY_PLAYER_FIXER_RELATIONS_TAG)
                : new CompoundTag();
        String fixerKey = fixerId.toString();
        if (!allRelations.contains(fixerKey, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }
            CompoundTag created = new CompoundTag();
            allRelations.put(fixerKey, created);
            persistentData.put(PLAYER_NPC_RELATIONS_TAG, allRelations);
            return created;
        }
        return allRelations.getCompound(fixerKey);
    }

    private static void storeArchiveRelation(Player player, UUID fixerId, CompoundTag relation) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag allRelations = persistentData.contains(PLAYER_NPC_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(PLAYER_NPC_RELATIONS_TAG)
                : persistentData.contains(LEGACY_PLAYER_FIXER_RELATIONS_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(LEGACY_PLAYER_FIXER_RELATIONS_TAG)
                : new CompoundTag();
        allRelations.put(fixerId.toString(), relation);
        persistentData.put(PLAYER_NPC_RELATIONS_TAG, allRelations);
    }

    private int scoreFocusTarget(Player candidate) {
        int score = 0;
        if (!hasMet(candidate)) {
            score += 80;
        } else {
            score += 10;
        }
        score += trustLevel(candidate) * 15;

        if (npcType().usesPatientFocus()) {
            PlayerCyberwareInventory inventory = new PlayerCyberwareInventory(candidate);
            int effectiveCyberstrain = CyberstrainManager.getEffectiveCyberstrain(candidate, inventory);
            score += effectiveCyberstrain;
            if (CyberstrainManager.isPsychosisActive(candidate)) {
                score += 200;
            }
        }

        score -= Mth.floor(distanceToSqr(candidate) * 0.75D);
        return score;
    }

    protected static final class FixerPlayerFocusGoal extends Goal {
        private final AbstractCityNpcEntity fixer;
        private @Nullable Player target;

        protected FixerPlayerFocusGoal(AbstractCityNpcEntity fixer) {
            this.fixer = fixer;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = fixer.findFocusTarget();
            return target != null && fixer.level().getEntitiesOfClass(Monster.class, fixer.getBoundingBox().inflate(8.0D), Monster::isAlive).isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return target != null
                    && target.isAlive()
                    && fixer.distanceToSqr(target) < 196.0D
                    && fixer.level().getEntitiesOfClass(Monster.class, fixer.getBoundingBox().inflate(8.0D), Monster::isAlive).isEmpty()
                    && fixer.findFocusTarget() == target;
        }

        @Override
        public void stop() {
            fixer.getNavigation().stop();
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            fixer.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double preferredDistance = fixer.currentCustomer != null && fixer.currentCustomer.equals(target.getUUID()) ? 2.0D : 3.0D;
            if (fixer.distanceToSqr(target) > preferredDistance * preferredDistance) {
                fixer.getNavigation().moveTo(target, 0.85D);
            } else {
                fixer.getNavigation().stop();
            }
        }
    }

    public record ArchiveContact(
            UUID npcId,
            String npcTypeId,
            String name,
            NpcCategory category,
            int trustLevel,
            boolean pinned,
            boolean hidden,
            boolean purgeReady,
            boolean surchargeActive,
            boolean refusing,
            String dimensionId,
            BlockPos homePos,
            boolean deceased
    ) {
    }

    public record ArchiveContactLocation(String dimensionId, @Nullable BlockPos pos) {
    }

    private record TrustAdapter(CompoundTag relation) {
        private int trustPoints() {
            return decodedTrustPoints(relation);
        }

        private int trustLevel() {
            return trustLevelForPoints(trustPoints());
        }
    }
}
