package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class QuestContract {
    private static final String ID_TAG = "Id";
    private static final String ISSUER_ID_TAG = "IssuerId";
    private static final String ISSUER_NAME_TAG = "IssuerName";
    private static final String ISSUER_TYPE_TAG = "IssuerType";
    private static final String LEGACY_FIXER_ID_TAG = "FixerId";
    private static final String LEGACY_FIXER_NAME_TAG = "FixerName";
    private static final String TYPE_TAG = "Type";
    private static final String TIER_TAG = "Tier";
    private static final String STATUS_TAG = "Status";
    private static final String TARGET_INDEX_TAG = "TargetIndex";
    private static final String TARGET_COUNT_TAG = "TargetCount";
    private static final String PROGRESS_TAG = "Progress";
    private static final String DESTINATION_REACHED_TAG = "DestinationReached";
    private static final String DIMENSION_ID_TAG = "DimensionId";
    private static final String TARGET_X_TAG = "TargetX";
    private static final String TARGET_Y_TAG = "TargetY";
    private static final String TARGET_Z_TAG = "TargetZ";
    private static final String AREA_RADIUS_TAG = "AreaRadius";
    private static final String REWARD_MONEY_TAG = "RewardMoney";
    private static final String REWARD_TRUST_TAG = "RewardTrust";
    private static final String REWARD_ITEM_KIND_TAG = "RewardItemKind";
    private static final String REWARD_ITEM_COUNT_TAG = "RewardItemCount";
    private static final String GENERATED_DAY_TAG = "GeneratedDay";
    private static final String RESOLVED_DAY_TAG = "ResolvedDay";

    public static final int REWARD_CHROMIUM = 4;
    public boolean chromiumSurvey() { return type == QuestType.INVESTIGATION && targetIndex == 1; }
    public static final int REWARD_NONE = 0;
    public static final int REWARD_MAXDOC = 1;
    public static final int REWARD_RAM_JOLT = 2;
    public static final int REWARD_CHROME_SUPPRESSANT = 3;
    /** A year of free Trauma Team coverage, as a patient quest's bonus; there is no item behind it. */
    public static final int REWARD_COVERAGE = 5;
    /** How far a patient quest has come: still looking, patient revived (beacon in hand), beacon planted. */
    public static final int PATIENT_SEARCHING = 0, PATIENT_REVIVED = 1, PATIENT_MARKED = 2;
    private int patientStage;
    private @Nullable BlockPos beaconPos;
    private @Nullable BlockPos patientPos;
    public int patientStage() { return patientStage; }
    public void setPatientStage(int stage) { patientStage = stage; }
    /** Where the beacon was planted beside the patient. */
    public @Nullable BlockPos beaconPos() { return beaconPos; }
    public void setBeaconPos(@Nullable BlockPos pos) { beaconPos = pos; }
    /** Where the patient actually lies, known once they have been spawned in. */
    public @Nullable BlockPos patientPos() { return patientPos; }
    public void setPatientPos(@Nullable BlockPos pos) { patientPos = pos; }
    public boolean patientRevived() { return (patientStage & PATIENT_REVIVED) != 0; }
    public boolean beaconLogged() { return (patientStage & PATIENT_MARKED) != 0; }
    public void addPatientStage(int flag) { patientStage |= flag; }
    /** A beacon logged within ten blocks of a revived patient: anything else is a false report. */
    public boolean honestReport() {
        return patientRevived() && beaconPos != null && patientPos != null && beaconPos.distSqr(patientPos) <= 100;
    }

    private final String id;
    private final UUID issuerId;
    private final String issuerName;
    private final String issuerType;
    private final QuestType type;
    private final int tier;
    private QuestStatus status;
    private final int targetIndex;
    private final int targetCount;
    private int progress;
    private int huntSpawnedCount;
    private boolean destinationReached;
    private String dimensionId;
    private @Nullable BlockPos targetPos;
    private int areaRadius;
    private final int rewardMoney;
    private final int rewardTrust;
    private final int rewardItemKind;
    private final int rewardItemCount;
    private final long generatedDay;
    private long resolvedDay;
    private long acceptedPlayTicks = Long.MIN_VALUE;
    private @Nullable InvestigationSurvey investigation;
    private boolean breachAttemptStarted;
    private boolean urgent;
    private long urgentDeadline;
    private boolean urgentExpired;
    private boolean returnItemCollected, itemLossPending, externalItemConfirmed, replacementPenalty, deliveryItemMissing;
    private boolean itemLossCallDeferred;
    public boolean itemLossCallDeferred() { return itemLossCallDeferred; }
    public void deferItemLossCall() { itemLossCallDeferred = true; }

    public boolean returnItemCollected() { return returnItemCollected || psychoShardCollected; }
    public void markReturnItemCollected() { returnItemCollected = true; }
    public boolean itemLossPending() { return itemLossPending; }
    public boolean externalItemConfirmed() { return externalItemConfirmed; }
    public void markItemLossPending() { itemLossPending = true; itemLossCallDeferred = false; }
    public void confirmExternalItem() { itemLossPending = false; externalItemConfirmed = true; itemLossCallDeferred = false; }
    public void foundReturnItem() { returnItemCollected = true; itemLossPending = false; externalItemConfirmed = false; itemLossCallDeferred = false; }
    public boolean replacementPenalty() { return replacementPenalty; }
    public void markReplacementPenalty() { replacementPenalty = true; deliveryItemMissing = false; }
    public boolean deliveryItemMissing() { return deliveryItemMissing; }
    public void setDeliveryItemMissing(boolean missing) { deliveryItemMissing = missing; }
    private int adjustedReward(int value) { return replacementPenalty ? value / 2 : value; }
    private boolean psychoSpawned, psychoShardCollected;
    private @Nullable BlockPos returnPos;
    private String cancellationReason = "";
    private int siteBindingVersion = 1;
    public boolean allowsLegacyContainerMigration() { return siteBindingVersion == 0; }
    public boolean markSiteBound() {
        if (siteBindingVersion != 0) return false;
        siteBindingVersion = 1;
        return true;
    }

    public void cancel(String reason) {
        status = QuestStatus.CANCELLED;
        cancellationReason = reason;
    }

    public Component cancellationReason() {
        return Component.translatable("quest.cyberneticenhancements.cancelled." + cancellationReason);
    }

    public boolean psychoSpawned() { return psychoSpawned; }
    public void markPsychoSpawned() { psychoSpawned = true; }
    public boolean psychoShardCollected() { return psychoShardCollected; }
    public void markPsychoShardCollected() { psychoShardCollected = true; }
    public UUID psychoId() { return UUID.nameUUIDFromBytes((id + "/cyberpsycho").getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    public String psychoName() { return CyberpsychoEncounter.subjectName(id); }

    public boolean isUrgent() { return urgent; }
    public void markUrgent() { urgent = true; }

    public int urgentLimitMinutes() {
        return switch (QuestDimension.of(dimensionId)) {
            case STANDARD -> 20;
            case NETHER -> 30;
            case END -> 40;
        };
    }

    boolean initializeUrgentDeadline(long playTicks) {
        if (!urgent || urgentDeadline != 0) return false;
        urgentDeadline = isReady() ? -1 : playTicks + urgentLimitMinutes() * 60L * 20L;
        return true;
    }

    void completeUrgentObjective() {
        if (urgent) urgentDeadline = -1;
    }

    boolean urgentTimeIsUp(long playTicks) {
        return urgent && status == QuestStatus.ACTIVE && urgentDeadline > 0 && playTicks >= urgentDeadline;
    }

    void expireUrgent() {
        urgentExpired = true;
        status = QuestStatus.FAILED;
    }

    /** The title for the quest HUD: a running urgent timer replaces the "Urgent" prefix with the time left. */
    Component hudTitle(long playTicks) {
        return timedLabel(title(), QuestDimension.of(dimensionId).title(baseTitle()), playTicks);
    }

    /** A label for this contract, in the compass markers too, so every urgent contract shows its time left the same way: "m:ss: label". */
    Component timedLabel(Component plain, long playTicks) {
        return timedLabel(plain, plain, playTicks);
    }

    private Component timedLabel(Component plain, Component timed, long playTicks) {
        if (!urgent || status != QuestStatus.ACTIVE || urgentDeadline <= 0) return plain;
        long seconds = Math.ceilDiv(Math.max(0, urgentDeadline - playTicks), 20);
        String remaining = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        return Component.translatable("quest.cyberneticenhancements.title.timed", remaining, timed);
    }

    Component timedObjective(Component objective, long playTicks) {
        if (!urgent || status != QuestStatus.ACTIVE || urgentDeadline <= 0) return objective;
        long seconds = Math.ceilDiv(Math.max(0, urgentDeadline - playTicks), 20);
        String remaining = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        return Component.translatable("quest.cyberneticenhancements.objective.time_remaining", remaining, objective);
    }

    public boolean breachAttemptStarted() { return breachAttemptStarted; }
    public void startBreachAttempt() { breachAttemptStarted = true; }

    public @Nullable InvestigationSurvey investigation() { return investigation; }
    public void setInvestigation(InvestigationSurvey investigation) { this.investigation = investigation; }

    public QuestContract(
            String id,
            UUID issuerId,
            String issuerName,
            String issuerType,
            QuestType type,
            int tier,
            QuestStatus status,
            int targetIndex,
            int targetCount,
            int progress,
            boolean destinationReached,
            String dimensionId,
            @Nullable BlockPos targetPos,
            int areaRadius,
            int rewardMoney,
            int rewardTrust,
            int rewardItemKind,
            int rewardItemCount,
            long generatedDay,
            long resolvedDay
    ) {
        this.id = id;
        this.issuerId = issuerId;
        this.issuerName = issuerName;
        this.issuerType = issuerType;
        this.type = type;
        this.tier = tier;
        this.status = status;
        this.targetIndex = targetIndex;
        this.targetCount = targetCount;
        this.progress = progress;
        this.destinationReached = destinationReached;
        this.dimensionId = dimensionId;
        this.targetPos = targetPos;
        this.areaRadius = areaRadius;
        this.rewardMoney = rewardMoney;
        this.rewardTrust = rewardTrust;
        this.rewardItemKind = rewardItemKind;
        this.rewardItemCount = rewardItemCount;
        this.generatedDay = generatedDay;
        this.resolvedDay = resolvedDay;
    }

    public String id() {
        return id;
    }

    public UUID issuerId() {
        return issuerId;
    }

    public String issuerName() {
        return issuerName;
    }

    public String issuerType() {
        return issuerType;
    }

    public QuestType type() {
        return type;
    }

    public int tier() {
        return tier;
    }

    public QuestStatus status() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    public int targetIndex() {
        return targetIndex;
    }

    public int targetCount() {
        return targetCount;
    }

    public int progress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Mth.clamp(progress, 0, targetCount);
    }

    public boolean destinationReached() {
        return destinationReached;
    }

    public void setDestinationReached(boolean destinationReached) {
        this.destinationReached = destinationReached;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public @Nullable BlockPos targetPos() {
        return targetPos;
    }

    public int areaRadius() {
        return areaRadius;
    }

    /** Where a world-encounter quest is handed in; null for ordinary contracts, which use their issuer's contact record. */
    public @Nullable BlockPos returnPos() { return returnPos; }
    public void setReturnPos(@Nullable BlockPos pos) { returnPos = pos; }
    public UUID patientId() { return UUID.nameUUIDFromBytes((id + "/patient").getBytes(java.nio.charset.StandardCharsets.UTF_8)); }

    public void setTargetLocation(String dimensionId, @Nullable BlockPos targetPos, int areaRadius) {
        this.dimensionId = dimensionId == null ? "" : dimensionId;
        this.targetPos = targetPos;
        this.areaRadius = Math.max(1, areaRadius);
    }

    public int rewardMoney() {
        int payout = QuestDimension.of(dimensionId).reward(rewardMoney);
        return adjustedReward(urgent ? (int) Math.min(Integer.MAX_VALUE, ((long) payout * 125 + 99) / 100) : payout);
    }

    public int rewardTrust() {
        return adjustedReward(urgent ? (int) Math.min(Integer.MAX_VALUE, (long) rewardTrust + 1) : rewardTrust);
    }

    public int rewardItemKind() {
        return rewardItemKind;
    }

    public int rewardItemCount() {
        return adjustedReward(rewardItemCount);
    }

    public long generatedDay() {
        return generatedDay;
    }

    public long acceptedPlayTicks() { return acceptedPlayTicks; }

    public void setAcceptedPlayTicks(long ticks) { acceptedPlayTicks = ticks; }

    public long resolvedDay() {
        return resolvedDay;
    }

    public void setResolvedDay(long resolvedDay) {
        this.resolvedDay = resolvedDay;
    }

    public boolean isReady() {
        return status == QuestStatus.READY;
    }

    public boolean isResolved() {
        return status == QuestStatus.COMPLETED || status == QuestStatus.FAILED || status == QuestStatus.ABANDONED;
    }

    public boolean isProgressComplete() {
        return switch (type) {
            case DELIVERY, EXTRACTION, SALVAGE -> destinationReached;
            case INVESTIGATION -> destinationReached;
            case LOCATE -> beaconLogged();
            case CYBERPSYCHO -> destinationReached && progress >= targetCount;
            case ELIMINATION, RECOVERY, BREACH -> progress >= targetCount;
        };
    }

    public ItemStack rewardItemStack() {
        return rewardItemStack(rewardItemKind, rewardItemCount());
    }

    public static ItemStack rewardItemStack(int rewardItemKind, int rewardItemCount) {
        if (rewardItemCount <= 0) {
            return ItemStack.EMPTY;
        }
        return switch (rewardItemKind) {
            case REWARD_CHROMIUM -> new ItemStack(ModItems.CHROMIUM_ALLOY.get(), rewardItemCount);
            case REWARD_MAXDOC -> new ItemStack(ModItems.consumable("maxdoc_mk2").get(), rewardItemCount);
            case REWARD_RAM_JOLT -> new ItemStack(ModItems.consumable("ram_jolt").get(), rewardItemCount);
            case REWARD_CHROME_SUPPRESSANT -> new ItemStack(ModItems.consumable("chrome_suppressant").get(), rewardItemCount);
            default -> ItemStack.EMPTY;
        };
    }

    public Item recoveryItem() {
        ResourceLocation id = switch (targetIndex) {
            case 1 -> ResourceLocation.withDefaultNamespace("iron_ingot");
            case 2 -> ResourceLocation.withDefaultNamespace("gold_ingot");
            case 3 -> ResourceLocation.withDefaultNamespace("diamond");
            case 4 -> ResourceLocation.withDefaultNamespace("ender_pearl");
            case 5 -> ResourceLocation.withDefaultNamespace("quartz");
            case 6 -> ResourceLocation.withDefaultNamespace("chorus_fruit");
            default -> ResourceLocation.withDefaultNamespace("redstone");
        };
        return BuiltInRegistries.ITEM.get(id);
    }

    public EntityType<?> eliminationTarget() {
        ResourceLocation id = switch (targetIndex) {
            case 1 -> ResourceLocation.withDefaultNamespace("skeleton");
            case 2 -> ResourceLocation.withDefaultNamespace("spider");
            case 3 -> ResourceLocation.withDefaultNamespace("creeper");
            case 4 -> ResourceLocation.withDefaultNamespace("husk");
            case 5 -> ResourceLocation.withDefaultNamespace("drowned");
            case 6 -> ResourceLocation.withDefaultNamespace("wither_skeleton");
            case 7 -> ResourceLocation.withDefaultNamespace("enderman");
            default -> ResourceLocation.withDefaultNamespace("zombie");
        };
        return BuiltInRegistries.ENTITY_TYPE.get(id);
    }

    public int huntSpawnedCount() {
        return huntSpawnedCount;
    }

    public void recordHuntSpawn() {
        huntSpawnedCount++;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ID_TAG, id);
        tag.putUUID(ISSUER_ID_TAG, issuerId);
        tag.putString(ISSUER_NAME_TAG, issuerName);
        tag.putString(ISSUER_TYPE_TAG, issuerType);
        tag.putString(TYPE_TAG, type.id());
        tag.putInt(TIER_TAG, tier);
        tag.putString(STATUS_TAG, status.id());
        tag.putInt(TARGET_INDEX_TAG, targetIndex);
        tag.putInt(TARGET_COUNT_TAG, targetCount);
        tag.putInt(PROGRESS_TAG, progress);
        tag.putInt("HuntSpawnedCount", huntSpawnedCount);
        tag.putBoolean(DESTINATION_REACHED_TAG, destinationReached);
        tag.putString(DIMENSION_ID_TAG, dimensionId);
        if (targetPos != null) {
            tag.putInt(TARGET_X_TAG, targetPos.getX());
            tag.putInt(TARGET_Y_TAG, targetPos.getY());
            tag.putInt(TARGET_Z_TAG, targetPos.getZ());
        }
        tag.putInt(AREA_RADIUS_TAG, areaRadius);
        tag.putInt(REWARD_MONEY_TAG, rewardMoney);
        tag.putInt(REWARD_TRUST_TAG, rewardTrust);
        tag.putInt(REWARD_ITEM_KIND_TAG, rewardItemKind);
        tag.putInt(REWARD_ITEM_COUNT_TAG, rewardItemCount);
        tag.putLong(GENERATED_DAY_TAG, generatedDay);
        tag.putLong(RESOLVED_DAY_TAG, resolvedDay);
        if (acceptedPlayTicks != Long.MIN_VALUE) tag.putLong("AcceptedPlayTicks", acceptedPlayTicks);
        if (investigation != null) tag.put("Investigation", investigation.toTag());
        tag.putBoolean("BreachAttemptStarted", breachAttemptStarted);
        tag.putBoolean("Urgent", urgent);
        if (urgentDeadline != 0) tag.putLong("UrgentDeadline", urgentDeadline);
        if (urgentExpired) tag.putBoolean("UrgentExpired", true);
        tag.putBoolean("ReturnItemCollected", returnItemCollected);
        tag.putBoolean("ItemLossPending", itemLossPending);
        tag.putBoolean("ItemLossCallDeferred", itemLossCallDeferred);
        tag.putBoolean("ExternalItemConfirmed", externalItemConfirmed);
        tag.putBoolean("ReplacementPenalty", replacementPenalty);
        tag.putBoolean("DeliveryItemMissing", deliveryItemMissing);
        tag.putBoolean("PsychoSpawned", psychoSpawned);
        tag.putBoolean("PsychoShardCollected", psychoShardCollected);
        tag.putString("CancellationReason", cancellationReason);
        if (returnPos != null) tag.putLong("ReturnPos", returnPos.asLong());
        if (patientStage != 0) tag.putInt("PatientStage", patientStage);
        if (beaconPos != null) tag.putLong("BeaconPos", beaconPos.asLong());
        if (patientPos != null) tag.putLong("PatientPos", patientPos.asLong());
        tag.putInt("SiteBindingVersion", siteBindingVersion);
        return tag;
    }

    public static QuestContract fromTag(CompoundTag tag) {
        BlockPos targetPos = tag.contains(TARGET_X_TAG)
                ? new BlockPos(tag.getInt(TARGET_X_TAG), tag.getInt(TARGET_Y_TAG), tag.getInt(TARGET_Z_TAG))
                : null;
        String issuerType = tag.getString(ISSUER_TYPE_TAG);
        if (issuerType.isBlank()) {
            issuerType = "fixer";
        }
        QuestContract contract = new QuestContract(
                tag.getString(ID_TAG),
                readIssuerUuid(tag),
                readIssuerName(tag),
                issuerType,
                QuestType.fromId(tag.getString(TYPE_TAG)),
                Math.max(1, tag.getInt(TIER_TAG)),
                QuestStatus.fromId(tag.getString(STATUS_TAG)),
                tag.getInt(TARGET_INDEX_TAG),
                Math.max(1, tag.getInt(TARGET_COUNT_TAG)),
                tag.getInt(PROGRESS_TAG),
                tag.getBoolean(DESTINATION_REACHED_TAG),
                tag.getString(DIMENSION_ID_TAG),
                targetPos,
                Math.max(8, tag.getInt(AREA_RADIUS_TAG)),
                Math.max(0, tag.getInt(REWARD_MONEY_TAG)),
                Math.max(0, tag.getInt(REWARD_TRUST_TAG)),
                Math.max(0, tag.getInt(REWARD_ITEM_KIND_TAG)),
                Math.max(0, tag.getInt(REWARD_ITEM_COUNT_TAG)),
                tag.getLong(GENERATED_DAY_TAG),
                tag.getLong(RESOLVED_DAY_TAG)
        );
        contract.huntSpawnedCount = Math.max(0, tag.getInt("HuntSpawnedCount"));
        if (tag.contains("AcceptedPlayTicks", Tag.TAG_LONG)) contract.acceptedPlayTicks = tag.getLong("AcceptedPlayTicks");
        if (tag.contains("Investigation", Tag.TAG_COMPOUND)) contract.investigation = InvestigationSurvey.fromTag(tag.getCompound("Investigation"));
        contract.breachAttemptStarted = tag.getBoolean("BreachAttemptStarted");
        contract.urgent = tag.getBoolean("Urgent");
        contract.urgentDeadline = tag.getLong("UrgentDeadline");
        contract.urgentExpired = tag.getBoolean("UrgentExpired");
        contract.returnItemCollected = tag.getBoolean("ReturnItemCollected");
        contract.itemLossPending = tag.getBoolean("ItemLossPending");
        contract.itemLossCallDeferred = tag.getBoolean("ItemLossCallDeferred");
        contract.externalItemConfirmed = tag.getBoolean("ExternalItemConfirmed");
        contract.replacementPenalty = tag.getBoolean("ReplacementPenalty");
        contract.deliveryItemMissing = tag.getBoolean("DeliveryItemMissing");
        contract.psychoSpawned = tag.getBoolean("PsychoSpawned");
        contract.psychoShardCollected = tag.getBoolean("PsychoShardCollected");
        contract.cancellationReason = tag.getString("CancellationReason");
        if (tag.contains("ReturnPos", Tag.TAG_LONG)) contract.returnPos = BlockPos.of(tag.getLong("ReturnPos"));
        contract.patientStage = tag.getInt("PatientStage");
        if (tag.contains("BeaconPos", Tag.TAG_LONG)) contract.beaconPos = BlockPos.of(tag.getLong("BeaconPos"));
        if (tag.contains("PatientPos", Tag.TAG_LONG)) contract.patientPos = BlockPos.of(tag.getLong("PatientPos"));
        contract.siteBindingVersion = tag.getInt("SiteBindingVersion");
        return contract;
    }

    public static Component typeLabel(QuestType type) {
        return switch (type) {
            case DELIVERY -> Component.translatable("quest.cyberneticenhancements.type.delivery");
            case EXTRACTION -> Component.translatable("quest.cyberneticenhancements.type.extraction");
            case INVESTIGATION -> Component.translatable("quest.cyberneticenhancements.type.investigation");
            case BREACH -> Component.translatable("quest.cyberneticenhancements.type.breach");
            case ELIMINATION -> Component.translatable("quest.cyberneticenhancements.type.elimination");
            case RECOVERY -> Component.translatable("quest.cyberneticenhancements.type.recovery");
            case CYBERPSYCHO -> Component.translatable("quest.cyberneticenhancements.type.cyberpsycho");
            case LOCATE -> Component.translatable("quest.cyberneticenhancements.type.locate");
            case SALVAGE -> Component.translatable("quest.cyberneticenhancements.type.salvage");
        };
    }

    public Component title() {
        var title = QuestDimension.of(dimensionId).title(baseTitle());
        return urgent ? Component.translatable("quest.cyberneticenhancements.title.urgent", title) : title;
    }

    private Component baseTitle() {
        if (chromiumSurvey()) return Component.translatable("quest.cyberneticenhancements.title.chromium_survey");
        if (!issuerType.isBlank()) {
            return Component.translatable("quest.cyberneticenhancements.title." + issuerType + "." + type.id());
        }
        return titleForType(type);
    }

    public static Component titleForType(QuestType type) {
        return switch (type) {
            case DELIVERY -> Component.translatable("quest.cyberneticenhancements.title.delivery");
            case EXTRACTION -> Component.translatable("quest.cyberneticenhancements.title.extraction");
            case INVESTIGATION -> Component.translatable("quest.cyberneticenhancements.title.investigation");
            case BREACH -> Component.translatable("quest.cyberneticenhancements.title.breach");
            case ELIMINATION -> Component.translatable("quest.cyberneticenhancements.title.elimination");
            case RECOVERY -> Component.translatable("quest.cyberneticenhancements.title.recovery");
            case CYBERPSYCHO -> Component.translatable("quest.cyberneticenhancements.title.cyberpsycho");
            case LOCATE -> Component.translatable("quest.cyberneticenhancements.title.locate");
            case SALVAGE -> Component.translatable("quest.cyberneticenhancements.title.salvage");
        };
    }

    public Component targetLabel() {
        if (chromiumSurvey()) return Component.translatable("quest.cyberneticenhancements.target.chromium_deposit");
        return switch (type) {
            case DELIVERY -> Component.translatable("quest.cyberneticenhancements.target.hidden_stash");
            case EXTRACTION -> Component.translatable("quest.cyberneticenhancements.target.exfil_shard");
            case INVESTIGATION -> Component.translatable(investigation != null && investigation.located()
                    ? "quest.cyberneticenhancements.target.signal_source" : "quest.cyberneticenhancements.target.signal_site");
            case BREACH -> Component.translatable("quest.cyberneticenhancements.target.breach_cache");
            case ELIMINATION -> eliminationTarget().getDescription();
            case RECOVERY -> recoveryItem().getDefaultInstance().getHoverName();
            case CYBERPSYCHO -> Component.literal(psychoName());
            case LOCATE -> Component.translatable("quest.cyberneticenhancements.target.patient");
            case SALVAGE -> Component.translatable("quest.cyberneticenhancements.target.cargo");
        };
    }

    public Component objectiveLine() {
        if (urgentExpired) return Component.translatable("quest.cyberneticenhancements.objective.urgent_expired");
        if (status == QuestStatus.CANCELLED) return cancellationReason();
        if (status == QuestStatus.ACTIVE || status == QuestStatus.READY) {
            if (itemLossPending) return Component.translatable("quest.cyberneticenhancements.objective.item_missing", issuerName);
            if (deliveryItemMissing) return Component.translatable("quest.cyberneticenhancements.objective.replacement", issuerName);
        }
        if (isReady()) {
            return Component.translatable(type.returnsItems() ? "quest.cyberneticenhancements.objective.return.in_person"
                    : type.requiresInPersonTurnIn() ? "quest.cyberneticenhancements.objective.report.in_person" : "quest.cyberneticenhancements.objective.return", issuerName);
        }
        if (chromiumSurvey()) return Component.translatable("quest.cyberneticenhancements.objective.chromium_survey", progress, InvestigationSurvey.READINGS);
        return switch (type) {
            case DELIVERY -> Component.translatable("quest.cyberneticenhancements.objective.delivery.stash");
            case EXTRACTION -> Component.translatable("quest.cyberneticenhancements.objective.extraction", targetLabel());
            case INVESTIGATION -> investigation != null && investigation.located()
                    ? Component.translatable("quest.cyberneticenhancements.objective.investigation.source")
                    : Component.translatable("quest.cyberneticenhancements.objective.investigation", investigation == null ? 0 : investigation.count(), InvestigationSurvey.READINGS);
            case BREACH -> Component.translatable("quest.cyberneticenhancements.objective.breach", targetLabel());
            case ELIMINATION -> Component.translatable("quest.cyberneticenhancements.objective.elimination", progress, targetCount, targetLabel());
            case RECOVERY -> QuestDimension.of(dimensionId) != QuestDimension.STANDARD && !destinationReached
                    ? Component.translatable("quest.cyberneticenhancements.objective.recovery_dimension", targetCount, targetLabel(), QuestDimension.name(dimensionId))
                    : Component.translatable("quest.cyberneticenhancements.objective.recovery", progress, targetCount, targetLabel());
            case CYBERPSYCHO -> Component.translatable(destinationReached ? "quest.cyberneticenhancements.objective.cyberpsycho.shard"
                    : "quest.cyberneticenhancements.objective.cyberpsycho", psychoName());
            case LOCATE -> Component.translatable(patientStage == PATIENT_REVIVED ? "quest.cyberneticenhancements.objective.locate.beacon"
                    : "quest.cyberneticenhancements.objective.locate");
            case SALVAGE -> Component.translatable("quest.cyberneticenhancements.objective.salvage");
        };
    }

    public Component statusLabel() {
        return switch (status) {
            case OFFERED -> Component.translatable("quest.cyberneticenhancements.status.offered");
            case ACTIVE -> Component.translatable("quest.cyberneticenhancements.status.active");
            case READY -> Component.translatable("quest.cyberneticenhancements.status.ready");
            case COMPLETED -> Component.translatable("quest.cyberneticenhancements.status.completed");
            case FAILED -> Component.translatable("quest.cyberneticenhancements.status.failed");
            case ABANDONED -> Component.translatable("quest.cyberneticenhancements.status.abandoned");
            case CANCELLED -> Component.translatable("quest.cyberneticenhancements.status.cancelled");
        };
    }

    public Component rewardSummary() {
        ItemStack rewardStack = rewardItemStack();
        var summary = rewardItemKind == REWARD_COVERAGE
                ? Component.translatable("quest.cyberneticenhancements.reward.bundle.item", rewardMoney(), rewardTrust(),
                        Component.translatable("quest.cyberneticenhancements.reward.coverage"))
                : rewardStack.isEmpty()
                ? Component.translatable("quest.cyberneticenhancements.reward.bundle", rewardMoney(), rewardTrust())
                : Component.translatable("quest.cyberneticenhancements.reward.bundle.item", rewardMoney(), rewardTrust(), rewardStack.getHoverName());
        summary.append(Component.translatable("quest.cyberneticenhancements.reward.xp", rewardExperience()));
        if (replacementPenalty) summary.append(Component.translatable("quest.cyberneticenhancements.reward.replacement_penalty"));
        if (chromiumSurvey()) summary.append(Component.translatable("quest.cyberneticenhancements.reward.chromium_marker"));
        return summary;
    }

    public int rewardExperience() {
        return adjustedReward(QuestDimension.of(dimensionId).reward(de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.quest(type, tier)));
    }

    public static int rewardItemKindForTier(int tier, QuestType type) {
        if (tier >= 3) {
            return switch (type) {
                case DELIVERY, EXTRACTION -> REWARD_CHROME_SUPPRESSANT;
                case BREACH, INVESTIGATION, ELIMINATION, RECOVERY, CYBERPSYCHO, LOCATE, SALVAGE -> REWARD_RAM_JOLT;
            };
        }
        return switch (type) {
            case RECOVERY, EXTRACTION -> REWARD_MAXDOC;
            default -> REWARD_NONE;
        };
    }

    public static int rewardItemCountFor(int rewardItemKind) {
        return rewardItemKind == REWARD_NONE ? 0 : 1;
    }

    public static int targetCountFor(QuestType type, int tier, int targetIndex) {
        return switch (type) {
            case DELIVERY -> 1;
            case EXTRACTION -> 1;
            case INVESTIGATION -> InvestigationSurvey.READINGS;
            case BREACH -> 1;
            case ELIMINATION -> 2 + tier * 2;
            case CYBERPSYCHO, LOCATE, SALVAGE -> 1;
            case RECOVERY -> switch (targetIndex) {
                case 1 -> 6 + tier * 3;
                case 2 -> 4 + tier * 2;
                case 3 -> 1 + tier;
                case 4 -> 2 + tier;
                case 5 -> 6 + tier * 3;
                case 6 -> 4 + tier * 2;
                default -> 8 + tier * 4;
            };
        };
    }

    public static int normalizeProgressForRecovery(QuestContract contract, int inventoryCount) {
        return Math.min(contract.targetCount(), Math.max(0, inventoryCount));
    }

    public static Item safeRecoveryItem(int targetIndex) {
        Item item = new QuestContract(
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                "",
                "fixer",
                QuestType.RECOVERY,
                1,
                QuestStatus.OFFERED,
                targetIndex,
                1,
                0,
                false,
                "",
                null,
                8,
                0,
                0,
                REWARD_NONE,
                0,
                0L,
                0L
        ).recoveryItem();
        return item == Items.AIR ? Items.REDSTONE : item;
    }

    public static EntityType<?> safeEliminationTarget(int targetIndex) {
        EntityType<?> type = new QuestContract(
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                "",
                "fixer",
                QuestType.ELIMINATION,
                1,
                QuestStatus.OFFERED,
                targetIndex,
                1,
                0,
                false,
                "",
                null,
                8,
                0,
                0,
                REWARD_NONE,
                0,
                0L,
                0L
        ).eliminationTarget();
        return type == EntityType.PIG ? EntityType.ZOMBIE : type;
    }

    private static UUID readIssuerUuid(CompoundTag tag) {
        if (tag.hasUUID(ISSUER_ID_TAG)) {
            return tag.getUUID(ISSUER_ID_TAG);
        }
        if (tag.hasUUID(LEGACY_FIXER_ID_TAG)) {
            return tag.getUUID(LEGACY_FIXER_ID_TAG);
        }
        return UUID.randomUUID();
    }

    private static String readIssuerName(CompoundTag tag) {
        String issuerName = tag.getString(ISSUER_NAME_TAG);
        if (!issuerName.isBlank()) {
            return issuerName;
        }
        issuerName = tag.getString(LEGACY_FIXER_NAME_TAG);
        return issuerName.isBlank() ? "Unknown Issuer" : issuerName;
    }
}
