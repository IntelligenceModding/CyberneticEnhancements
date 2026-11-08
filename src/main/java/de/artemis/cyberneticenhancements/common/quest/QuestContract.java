package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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

    public static final int REWARD_NONE = 0;
    public static final int REWARD_MAXDOC = 1;
    public static final int REWARD_RAM_JOLT = 2;
    public static final int REWARD_CHROME_SUPPRESSANT = 3;

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

    public void setTargetLocation(String dimensionId, @Nullable BlockPos targetPos, int areaRadius) {
        this.dimensionId = dimensionId == null ? "" : dimensionId;
        this.targetPos = targetPos;
        this.areaRadius = Math.max(1, areaRadius);
    }

    public int rewardMoney() {
        return rewardMoney;
    }

    public int rewardTrust() {
        return rewardTrust;
    }

    public int rewardItemKind() {
        return rewardItemKind;
    }

    public int rewardItemCount() {
        return rewardItemCount;
    }

    public long generatedDay() {
        return generatedDay;
    }

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
            case DELIVERY, EXTRACTION -> destinationReached;
            case ELIMINATION, RECOVERY, INVESTIGATION, BREACH -> progress >= targetCount;
        };
    }

    public ItemStack rewardItemStack() {
        if (rewardItemCount <= 0) {
            return ItemStack.EMPTY;
        }
        return switch (rewardItemKind) {
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
        };
    }

    public Component title() {
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
        };
    }

    public Component targetLabel() {
        return switch (type) {
            case DELIVERY -> Component.translatable("quest.cyberneticenhancements.target.hidden_stash");
            case EXTRACTION -> Component.translatable("quest.cyberneticenhancements.target.exfil_shard");
            case INVESTIGATION -> Component.translatable("quest.cyberneticenhancements.target.signal_site");
            case BREACH -> Component.translatable("quest.cyberneticenhancements.target.breach_cache");
            case ELIMINATION -> eliminationTarget().getDescription();
            case RECOVERY -> recoveryItem().getDefaultInstance().getHoverName();
        };
    }

    public Component objectiveLine() {
        if (isReady()) {
            return Component.translatable("quest.cyberneticenhancements.objective.return", issuerName);
        }
        return switch (type) {
            case DELIVERY -> Component.translatable("quest.cyberneticenhancements.objective.delivery.stash");
            case EXTRACTION -> Component.translatable("quest.cyberneticenhancements.objective.extraction", targetLabel());
            case INVESTIGATION -> Component.translatable("quest.cyberneticenhancements.objective.investigation", progress, targetCount);
            case BREACH -> Component.translatable("quest.cyberneticenhancements.objective.breach", targetLabel());
            case ELIMINATION -> Component.translatable("quest.cyberneticenhancements.objective.elimination", progress, targetCount, targetLabel());
            case RECOVERY -> Component.translatable("quest.cyberneticenhancements.objective.recovery", progress, targetCount, targetLabel());
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
        };
    }

    public Component rewardSummary() {
        ItemStack rewardStack = rewardItemStack();
        if (!rewardStack.isEmpty()) {
            return Component.translatable("quest.cyberneticenhancements.reward.bundle.item", rewardMoney, rewardTrust, rewardStack.getHoverName());
        }
        return Component.translatable("quest.cyberneticenhancements.reward.bundle", rewardMoney, rewardTrust);
    }

    public static int rewardItemKindForTier(int tier, QuestType type) {
        if (tier >= 3) {
            return switch (type) {
                case DELIVERY, EXTRACTION -> REWARD_CHROME_SUPPRESSANT;
                case BREACH, INVESTIGATION, ELIMINATION, RECOVERY -> REWARD_RAM_JOLT;
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
            case INVESTIGATION -> 2 + Math.max(0, tier - 1);
            case BREACH -> 1;
            case ELIMINATION -> 2 + tier * 2;
            case RECOVERY -> switch (targetIndex) {
                case 1 -> 6 + tier * 3;
                case 2 -> 4 + tier * 2;
                case 3 -> 1 + tier;
                case 4 -> 2 + tier;
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
