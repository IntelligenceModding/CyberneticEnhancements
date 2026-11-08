package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlayerQuestManager {
    private static final String ROOT_TAG = "cyberneticenhancements.quest_state";
    private static final String ISSUER_BOARDS_TAG = "IssuerBoards";
    private static final String LEGACY_FIXER_BOARDS_TAG = "FixerBoards";
    private static final String BOARD_REFRESH_DAY_TAG = "RefreshDay";
    private static final String BOARD_OFFERS_TAG = "Offers";
    private static final String BOARD_ACTIVE_TAG = "Active";
    private static final String BOARD_HISTORY_TAG = "History";
    private static final int MAX_OFFERS = 3;
    private static final int MAX_HISTORY = 8;
    private static final int MAX_GLOBAL_ACTIVE = 3;
    private static final String QUEST_ITEM_TYPE_TAG = "QuestItemType";
    private static final String QUEST_CONTRACT_ID_TAG = "QuestContractId";
    private static final String QUEST_ISSUER_ID_TAG = "QuestIssuerId";
    private static final String QUEST_ITEM_TYPE_PACKAGE = "courier_package";
    private static final String QUEST_ITEM_TYPE_COMPASS = "hidden_stash_compass";
    private static final String QUEST_ITEM_TYPE_EXTRACTION = "exfil_shard";
    private static final String QUEST_ITEM_TYPE_EXTRACTION_COMPASS = "extraction_compass";
    private static final String QUEST_ITEM_TYPE_INVESTIGATION_COMPASS = "investigation_compass";
    private static final String QUEST_ITEM_TYPE_BREACH_COMPASS = "breach_compass";
    private static final String QUEST_ITEM_TYPE_HUNT_COMPASS = "hunt_compass";
    private static final int DELIVERY_MIN_DISTANCE = 220;
    private static final int DELIVERY_MAX_DISTANCE = 1000;
    private static final int EXTRACTION_MIN_DISTANCE = 180;
    private static final int EXTRACTION_MAX_DISTANCE = 920;
    private static final int INVESTIGATION_MIN_DISTANCE = 120;
    private static final int INVESTIGATION_MAX_DISTANCE = 760;
    private static final int BREACH_MIN_DISTANCE = 260;
    private static final int BREACH_MAX_DISTANCE = 1000;

    private PlayerQuestManager() {
    }

    public static void copyState(Player original, Player clone) {
        CompoundTag source = original.getPersistentData().getCompound(ROOT_TAG);
        if (!source.isEmpty()) {
            clone.getPersistentData().put(ROOT_TAG, source.copy());
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getEntity().level().isClientSide()) {
            return;
        }
        tickActiveContracts(player);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (entity instanceof Player) {
            return;
        }
        updateEliminationProgress(
                player,
                entity.getType(),
                entity.blockPosition(),
                entity.level().dimension().location().toString()
        );
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()
                || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        updateInvestigationProgress(player, event.getPos());
    }

    public static void onRelicCacheBreached(ServerPlayer player, BlockPos pos) {
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return;
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            return;
        }
        boolean dirty = false;
        String dimensionId = player.serverLevel().dimension().location().toString();
        for (String issuerKey : boards.getAllKeys()) {
            CompoundTag board = boards.getCompound(issuerKey);
            if (!board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            QuestContract contract = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
            if (contract.type() != QuestType.BREACH || contract.status() != QuestStatus.ACTIVE || contract.targetPos() == null) {
                continue;
            }
            if (dimensionId.equals(contract.dimensionId()) && contract.targetPos().equals(pos)) {
                contract.setProgress(contract.targetCount());
                contract.setStatus(QuestStatus.READY);
                cleanupBreachCompass(player, contract);
                writeActive(board, contract);
                boards.put(issuerKey, board);
                dirty = true;
            }
        }
        if (dirty) {
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
        }
    }

    public static BoardSnapshot snapshotForIssuer(ServerPlayer player, AbstractCityNpcEntity issuer) {
        ensureIssuerBoard(player, issuer);
        CompoundTag board = issuerBoardTag(player, issuer, true);
        List<QuestContract> offers = readContracts(board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND));
        QuestContract active = board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)
                ? QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG))
                : null;
        if (active != null) {
            refreshContractProgress(player, active);
            writeActive(board, active);
            storeIssuerBoard(player, issuer, board);
        }
        return new BoardSnapshot(active, List.copyOf(offers));
    }

    public static Result acceptIssuerOffer(ServerPlayer player, AbstractCityNpcEntity issuer, int offerIndex) {
        ensureIssuerBoard(player, issuer);
        if (globalActiveCount(player) >= MAX_GLOBAL_ACTIVE) {
            return Result.MAX_ACTIVE;
        }
        CompoundTag board = issuerBoardTag(player, issuer, true);
        if (board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
            return Result.HAS_ACTIVE;
        }
        List<QuestContract> offers = readContracts(board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND));
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return Result.INVALID;
        }
        QuestContract offer = offers.remove(offerIndex);
        offer.setStatus(QuestStatus.ACTIVE);
        offer.setProgress(0);
        offer.setDestinationReached(false);
        switch (offer.type()) {
            case DELIVERY -> {
                issueCourierDeliveryItems(player, offer);
                ensureCourierStash(resolveContractLevel(player, offer), offer);
            }
            case EXTRACTION -> {
                issueExtractionItems(player, offer);
                ensureExtractionDrop(resolveContractLevel(player, offer), offer);
            }
            case INVESTIGATION -> {
                issueInvestigationItems(player, offer);
                ensureInvestigationMarker(resolveContractLevel(player, offer), offer);
            }
            case BREACH -> {
                issueBreachItems(player, offer);
                ensureBreachSite(resolveContractLevel(player, offer), offer);
            }
            case ELIMINATION -> {
                issueEliminationItems(player, offer);
                ensureEliminationMarker(resolveContractLevel(player, offer), offer);
            }
            default -> {
            }
        }
        writeOffers(board, offers);
        writeActive(board, offer);
        storeIssuerBoard(player, issuer, board);
        return Result.OK;
    }

    public static Result abandonIssuerActive(ServerPlayer player, AbstractCityNpcEntity issuer) {
        CompoundTag board = issuerBoardTag(player, issuer, false);
        if (board == null || !board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
            return Result.INVALID;
        }
        QuestContract active = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
        cleanupContractArtifacts(player, active);
        active.setStatus(QuestStatus.ABANDONED);
        active.setResolvedDay(currentDay(player.level()));
        board.remove(BOARD_ACTIVE_TAG);
        appendHistory(board, active);
        storeIssuerBoard(player, issuer, board);
        applyAbandonPenalty(player, active);
        return Result.OK;
    }

    public static Result abandonQuestById(ServerPlayer player, String questId) {
        if (questId == null || questId.isBlank()) {
            return Result.INVALID;
        }
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return Result.INVALID;
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            return Result.INVALID;
        }
        for (String issuerKey : boards.getAllKeys()) {
            CompoundTag board = boards.getCompound(issuerKey);
            if (!board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            QuestContract active = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
            if (!questId.equals(active.id())) {
                continue;
            }
            cleanupContractArtifacts(player, active);
            board.remove(BOARD_ACTIVE_TAG);
            boards.put(issuerKey, board);
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
            applyAbandonPenalty(player, active);
            return Result.OK;
        }
        return Result.INVALID;
    }

    public static int clearAllQuestData(ServerPlayer player) {
        CompoundTag root = rootTag(player, false);
        if (root == null || root.isEmpty()) {
            return 0;
        }
        int removedEntries = countQuestEntries(root);
        player.getPersistentData().remove(ROOT_TAG);
        return removedEntries;
    }

    public static int removeQuestEntryById(ServerPlayer player, String questId) {
        if (questId == null || questId.isBlank()) {
            return 0;
        }
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return 0;
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            return 0;
        }
        int removedCount = 0;
        List<String> emptyBoards = new ArrayList<>();
        for (String issuerKey : boards.getAllKeys()) {
            CompoundTag board = boards.getCompound(issuerKey);

            if (board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                QuestContract active = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
                if (questId.equals(active.id())) {
                    cleanupContractArtifacts(player, active);
                    board.remove(BOARD_ACTIVE_TAG);
                    removedCount++;
                }
            }

            List<QuestContract> offers = readContracts(board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND));
            int offerCount = offers.size();
            offers.removeIf(contract -> questId.equals(contract.id()));
            if (offers.size() != offerCount) {
                writeOffers(board, offers);
                removedCount += offerCount - offers.size();
            }

            List<QuestContract> history = readContracts(board.getList(BOARD_HISTORY_TAG, Tag.TAG_COMPOUND));
            int historyCount = history.size();
            history.removeIf(contract -> questId.equals(contract.id()));
            if (history.size() != historyCount) {
                ListTag historyTag = new ListTag();
                for (QuestContract entry : history) {
                    historyTag.add(entry.toTag());
                }
                board.put(BOARD_HISTORY_TAG, historyTag);
                removedCount += historyCount - history.size();
            }

            boolean hasOffers = board.contains(BOARD_OFFERS_TAG, Tag.TAG_LIST) && !board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND).isEmpty();
            boolean hasActive = board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND);
            boolean hasHistory = board.contains(BOARD_HISTORY_TAG, Tag.TAG_LIST) && !board.getList(BOARD_HISTORY_TAG, Tag.TAG_COMPOUND).isEmpty();
            if (!hasOffers && !hasActive && !hasHistory) {
                emptyBoards.add(issuerKey);
            } else {
                boards.put(issuerKey, board);
            }
        }

        for (String emptyBoard : emptyBoards) {
            boards.remove(emptyBoard);
        }

        if (removedCount <= 0) {
            return 0;
        }

        if (boards.isEmpty()) {
            player.getPersistentData().remove(ROOT_TAG);
            return removedCount;
        }

        root.put(ISSUER_BOARDS_TAG, boards);
        player.getPersistentData().put(ROOT_TAG, root);
        return removedCount;
    }

    public static TurnInResult turnInIssuerActive(ServerPlayer player, AbstractCityNpcEntity issuer) {
        CompoundTag board = issuerBoardTag(player, issuer, false);
        if (board == null || !board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
            return TurnInResult.NOT_READY;
        }
        QuestContract active = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
        refreshContractProgress(player, active);
        if (active.type() == QuestType.DELIVERY && canForceReadyDeliveryTurnIn(player, active)) {
            active.setDestinationReached(true);
            active.setStatus(QuestStatus.READY);
        }
        if (!active.isReady()) {
            writeActive(board, active);
            storeIssuerBoard(player, issuer, board);
            return TurnInResult.NOT_READY;
        }
        if (active.type() == QuestType.RECOVERY && !consumeRecoveryItems(player, active)) {
            return TurnInResult.MISSING_ITEMS;
        }
        if (active.type() == QuestType.EXTRACTION && !consumeExtractionItem(player, active)) {
            return TurnInResult.MISSING_ITEMS;
        }
        cleanupContractArtifacts(player, active);

        PlayerEurodollarManager.add(
                player,
                active.rewardMoney(),
                "quest_reward",
                "Contract payout",
                active.issuerId(),
                active.issuerName()
        );
        issuer.raiseTrust(player, active.rewardTrust());
        issuer.recordPaidTransaction(player);
        ItemStack rewardItem = active.rewardItemStack();
        if (!rewardItem.isEmpty()) {
            ItemStack rewardCopy = rewardItem.copy();
            if (!player.addItem(rewardCopy)) {
                player.drop(rewardCopy, false);
            }
        }
        active.setStatus(QuestStatus.COMPLETED);
        active.setResolvedDay(currentDay(player.level()));
        board.remove(BOARD_ACTIVE_TAG);
        appendHistory(board, active);
        storeIssuerBoard(player, issuer, board);
        return TurnInResult.OK;
    }

    public static List<QuestLogEntry> questLog(ServerPlayer player) {
        List<QuestLogEntry> entries = new ArrayList<>();
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return List.of();
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            return List.of();
        }
        for (String issuerKey : boards.getAllKeys()) {
            if (!boards.contains(issuerKey, Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag board = boards.getCompound(issuerKey);
            if (board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                QuestContract contract = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
                refreshContractProgress(player, contract);
                entries.add(toLogEntry(player, contract));
                writeActive(board, contract);
                boards.put(issuerKey, board);
            }
            for (QuestContract contract : readContracts(board.getList(BOARD_HISTORY_TAG, Tag.TAG_COMPOUND))) {
                entries.add(toLogEntry(player, contract));
            }
        }
        player.getPersistentData().put(ROOT_TAG, root);
        entries.sort(Comparator.comparing(QuestLogEntry::sortGroup).thenComparing(QuestLogEntry::title, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
    }

    private static QuestLogEntry toLogEntry(ServerPlayer player, QuestContract contract) {
        refreshContractProgress(player, contract);
        String location = "";
        int x = 0;
        int y = 0;
        int z = 0;
        boolean hasLocation = false;
        if (contract.targetPos() != null && !contract.dimensionId().isBlank()) {
            location = contract.dimensionId();
            x = contract.targetPos().getX();
            y = contract.targetPos().getY();
            z = contract.targetPos().getZ();
            hasLocation = true;
        }
        return new QuestLogEntry(
                contract.id(),
                contract.issuerId().toString(),
                contract.issuerName(),
                contract.issuerType(),
                contract.type().id(),
                contract.title().getString(),
                contract.objectiveLine().getString(),
                contract.rewardSummary().getString(),
                contract.status().id(),
                contract.statusLabel().getString(),
                contract.tier(),
                hasLocation,
                location,
                x,
                y,
                z,
                sortGroup(contract.status())
        );
    }

    private static int sortGroup(QuestStatus status) {
        return switch (status) {
            case READY -> 0;
            case ACTIVE -> 1;
            case OFFERED -> 2;
            case COMPLETED -> 3;
            case FAILED, ABANDONED -> 4;
        };
    }

    private static void tickActiveContracts(ServerPlayer player) {
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return;
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            return;
        }
        boolean dirty = false;
        for (String issuerKey : boards.getAllKeys()) {
            CompoundTag board = boards.getCompound(issuerKey);
            if (!board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            QuestContract contract = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
            refreshCourierCompassTargets(player, contract);
            dirty |= refreshContractProgress(player, contract);
            writeActive(board, contract);
            boards.put(issuerKey, board);
        }
        if (dirty) {
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
        }
    }

    private static boolean refreshContractProgress(ServerPlayer player, QuestContract contract) {
        if (contract.status() != QuestStatus.ACTIVE && contract.status() != QuestStatus.READY) {
            return false;
        }
        return switch (contract.type()) {
            case DELIVERY -> updateDeliveryProgress(player, contract);
            case EXTRACTION -> updateExtractionProgress(player, contract);
            case INVESTIGATION -> updateInvestigationMarker(player, contract);
            case BREACH -> updateBreachProgress(player, contract);
            case RECOVERY -> updateRecoveryProgress(player, contract);
            case ELIMINATION -> updateEliminationRuntime(player, contract);
        };
    }

    private static boolean updateEliminationRuntime(ServerPlayer player, QuestContract contract) {
        boolean dirty = false;
        if (contract.status() == QuestStatus.ACTIVE) {
            dirty |= ensureEliminationMarker(resolveContractLevel(player, contract), contract);
            dirty |= EliminationEncounter.trySpawn(player, contract);
            dirty |= ensureQuestGuidanceItem(
                    player,
                    contract,
                    QUEST_ITEM_TYPE_HUNT_COMPASS,
                    () -> createQuestCompass(contract, QUEST_ITEM_TYPE_HUNT_COMPASS, "item.cyberneticenhancements.hunt_compass")
            );
        }
        if (contract.isProgressComplete() && contract.status() != QuestStatus.READY) {
            contract.setStatus(QuestStatus.READY);
            dirty = true;
        }
        return dirty;
    }

    private static boolean updateDeliveryProgress(ServerPlayer player, QuestContract contract) {
        ServerLevel level = resolveContractLevel(player, contract);
        if (level == null || contract.targetPos() == null) {
            return false;
        }
        boolean dirty = false;
        if (!contract.destinationReached()) {
            dirty |= ensureQuestGuidanceItem(
                    player,
                    contract,
                    QUEST_ITEM_TYPE_COMPASS,
                    () -> createHiddenStashCompass(contract)
            );
            dirty |= ensureCourierPackage(player, level, contract);
            dirty |= ensureCourierStash(level, contract);
        }
        if (!contract.destinationReached() && consumeCourierPackageFromStash(level, contract)) {
            contract.setDestinationReached(true);
            contract.setStatus(QuestStatus.READY);
            cleanupCourierGuidanceItems(player, contract);
            cleanupCourierStash(level, contract);
            return true;
        }
        return dirty;
    }

    private static boolean updateExtractionProgress(ServerPlayer player, QuestContract contract) {
        boolean dirty = false;
        boolean hasExtractionItem = hasTaggedQuestItemInInventory(player, contract, QUEST_ITEM_TYPE_EXTRACTION);
        if (contract.status() == QuestStatus.ACTIVE) {
            dirty |= ensureQuestGuidanceItem(
                    player,
                    contract,
                    QUEST_ITEM_TYPE_EXTRACTION_COMPASS,
                    () -> createQuestCompass(contract, QUEST_ITEM_TYPE_EXTRACTION_COMPASS, "item.cyberneticenhancements.extraction_compass")
            );
        }
        if (hasExtractionItem) {
            if (!contract.destinationReached() || contract.status() != QuestStatus.READY) {
                contract.setDestinationReached(true);
                contract.setProgress(contract.targetCount());
                contract.setStatus(QuestStatus.READY);
                cleanupExtractionMarker(resolveContractLevel(player, contract), contract);
                cleanupExtractionCompass(player, contract);
                return true;
            }
            return dirty;
        }
        if (contract.destinationReached() || contract.status() == QuestStatus.READY) {
            contract.setDestinationReached(false);
            contract.setProgress(0);
            contract.setStatus(QuestStatus.ACTIVE);
            dirty = true;
        }
        return dirty | ensureExtractionDrop(resolveContractLevel(player, contract), contract);
    }

    private static boolean updateInvestigationMarker(ServerPlayer player, QuestContract contract) {
        if (contract.status() != QuestStatus.ACTIVE) {
            return false;
        }
        boolean dirty = ensureQuestGuidanceItem(
                player,
                contract,
                QUEST_ITEM_TYPE_INVESTIGATION_COMPASS,
                () -> createQuestCompass(contract, QUEST_ITEM_TYPE_INVESTIGATION_COMPASS, "item.cyberneticenhancements.signal_trace_compass")
        );
        return dirty | ensureInvestigationMarker(resolveContractLevel(player, contract), contract);
    }

    private static boolean updateBreachProgress(ServerPlayer player, QuestContract contract) {
        boolean dirty = false;
        if (contract.status() == QuestStatus.ACTIVE) {
            dirty |= ensureQuestGuidanceItem(
                    player,
                    contract,
                    QUEST_ITEM_TYPE_BREACH_COMPASS,
                    () -> createQuestCompass(contract, QUEST_ITEM_TYPE_BREACH_COMPASS, "item.cyberneticenhancements.breach_vector_compass")
            );
        }
        if (contract.isProgressComplete() && contract.status() != QuestStatus.READY) {
            contract.setStatus(QuestStatus.READY);
            return true;
        }
        if (contract.isProgressComplete()) {
            return dirty;
        }
        return dirty | ensureBreachSite(resolveContractLevel(player, contract), contract);
    }

    private static boolean updateRecoveryProgress(ServerPlayer player, QuestContract contract) {
        int oldProgress = contract.progress();
        QuestStatus oldStatus = contract.status();
        int inventoryCount = countInventoryItems(player, contract.recoveryItem());
        contract.setProgress(QuestContract.normalizeProgressForRecovery(contract, inventoryCount));
        if (contract.progress() >= contract.targetCount() && contract.status() != QuestStatus.READY) {
            contract.setStatus(QuestStatus.READY);
        } else if (contract.progress() < contract.targetCount() && contract.status() == QuestStatus.READY) {
            contract.setStatus(QuestStatus.ACTIVE);
        }
        return contract.progress() != oldProgress || contract.status() != oldStatus;
    }

    private static void updateEliminationProgress(ServerPlayer player, EntityType<?> killedType, BlockPos killedPos, String killedDimensionId) {
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return;
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            return;
        }
        boolean dirty = false;
        for (String issuerKey : boards.getAllKeys()) {
            CompoundTag board = boards.getCompound(issuerKey);
            if (!board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            QuestContract contract = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
            if (contract.type() != QuestType.ELIMINATION || contract.status() != QuestStatus.ACTIVE) {
                continue;
            }
            boolean inHuntZone = contract.targetPos() == null
                    || (killedDimensionId.equals(contract.dimensionId())
                    && killedPos.closerThan(contract.targetPos(), Math.max(1.0D, contract.areaRadius() + 0.5D)));
            if (contract.eliminationTarget() == killedType && inHuntZone) {
                contract.setProgress(contract.progress() + 1);
                if (contract.isProgressComplete()) {
                    contract.setStatus(QuestStatus.READY);
                }
                writeActive(board, contract);
                boards.put(issuerKey, board);
                dirty = true;
            }
        }
        if (dirty) {
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
        }
    }

    private static void updateInvestigationProgress(ServerPlayer player, BlockPos clickedPos) {
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return;
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            return;
        }
        boolean dirty = false;
        String dimensionId = player.serverLevel().dimension().location().toString();
        for (String issuerKey : boards.getAllKeys()) {
            CompoundTag board = boards.getCompound(issuerKey);
            if (!board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            QuestContract contract = QuestContract.fromTag(board.getCompound(BOARD_ACTIVE_TAG));
            if (contract.type() != QuestType.INVESTIGATION || contract.status() != QuestStatus.ACTIVE || contract.targetPos() == null) {
                continue;
            }
            if (!dimensionId.equals(contract.dimensionId())
                    || !clickedPos.closerThan(contract.targetPos(), Math.max(1.0D, contract.areaRadius() + 0.5D))) {
                continue;
            }
            cleanupInvestigationMarker(player.serverLevel(), contract);
            contract.setProgress(contract.progress() + 1);
            if (contract.isProgressComplete()) {
                contract.setStatus(QuestStatus.READY);
                cleanupInvestigationCompass(player, contract);
            } else {
                advanceInvestigationTarget(player, contract);
            }
            writeActive(board, contract);
            boards.put(issuerKey, board);
            dirty = true;
        }
        if (dirty) {
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
        }
    }

    private static boolean consumeRecoveryItems(ServerPlayer player, QuestContract contract) {
        Item item = contract.recoveryItem();
        int needed = contract.targetCount();
        if (countInventoryItems(player, item) < needed) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(item)) {
                continue;
            }
            int remove = Math.min(needed, stack.getCount());
            stack.shrink(remove);
            needed -= remove;
            if (needed <= 0) {
                break;
            }
        }
        if (needed > 0) {
            for (ItemStack stack : player.getInventory().offhand) {
                if (!stack.is(item)) {
                    continue;
                }
                int remove = Math.min(needed, stack.getCount());
                stack.shrink(remove);
                needed -= remove;
                if (needed <= 0) {
                    break;
                }
            }
        }
        return needed <= 0;
    }

    private static boolean consumeExtractionItem(ServerPlayer player, QuestContract contract) {
        return consumeTaggedQuestItem(player, contract, QUEST_ITEM_TYPE_EXTRACTION);
    }

    private static boolean consumeTaggedQuestItem(ServerPlayer player, QuestContract contract, String itemType) {
        for (ItemStack stack : player.getInventory().items) {
            if (matchesQuestItem(stack, contract, itemType)) {
                stack.shrink(1);
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (matchesQuestItem(stack, contract, itemType)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static int countInventoryItems(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void ensureIssuerBoard(ServerPlayer player, AbstractCityNpcEntity issuer) {
        if (!issuer.npcType().supportsContracts()) {
            return;
        }
        CompoundTag board = issuerBoardTag(player, issuer, true);
        long currentDay = currentDay(player.level());
        if (board.getLong(BOARD_REFRESH_DAY_TAG) == currentDay && board.contains(BOARD_OFFERS_TAG, Tag.TAG_LIST)) {
            return;
        }
        List<QuestContract> offers = generateOffers(player, issuer, currentDay);
        board.putLong(BOARD_REFRESH_DAY_TAG, currentDay);
        writeOffers(board, offers);
        storeIssuerBoard(player, issuer, board);
    }

    private static List<QuestContract> generateOffers(ServerPlayer player, AbstractCityNpcEntity issuer, long day) {
        RandomSource random = RandomSource.create(seedFor(player, issuer, day));
        int trust = issuer.trustLevel(player);
        int maxTier = trust >= 3 ? 3 : trust >= 1 ? 2 : 1;
        List<QuestContract> offers = new ArrayList<>(MAX_OFFERS);
        Set<QuestType> used = new HashSet<>();
        for (int index = 0; index < MAX_OFFERS; index++) {
            QuestType type = questTypeForSlot(issuer.npcType(), index, random, trust, used);
            used.add(type);
            int tier = 1 + random.nextInt(maxTier);
            offers.add(generateOffer(player, issuer, type, tier, index, day, random));
        }
        return offers;
    }

    private static QuestType questTypeForSlot(NpcType npcType, int index, RandomSource random, int trust, Set<QuestType> used) {
        if (index == 0) {
            return primaryQuestType(npcType);
        }
        List<QuestType> pool = weightedQuestPool(npcType, trust);
        for (int attempt = 0; attempt < 10; attempt++) {
            QuestType picked = pool.get(random.nextInt(pool.size()));
            if (used.size() >= QuestType.values().length || !used.contains(picked)) {
                return picked;
            }
        }
        for (QuestType fallback : pool) {
            if (!used.contains(fallback)) {
                return fallback;
            }
        }
        return primaryQuestType(npcType);
    }

    private static QuestType primaryQuestType(NpcType npcType) {
        return switch (npcType) {
            case FIXER -> QuestType.DELIVERY;
            case RIPPERDOC -> QuestType.RECOVERY;
            case TECHIE -> QuestType.RECOVERY;
            case NETRUNNER -> QuestType.BREACH;
            case MERC -> QuestType.ELIMINATION;
        };
    }

    private static List<QuestType> weightedQuestPool(NpcType npcType, int trust) {
        List<QuestType> base = new ArrayList<>();
        switch (npcType) {
            case FIXER -> {
                addWeighted(base, QuestType.DELIVERY, 4);
                addWeighted(base, QuestType.INVESTIGATION, trust >= 1 ? 3 : 2);
                addWeighted(base, QuestType.EXTRACTION, 2);
                addWeighted(base, QuestType.RECOVERY, 2);
                addWeighted(base, QuestType.ELIMINATION, trust >= 2 ? 2 : 1);
            }
            case RIPPERDOC -> {
                addWeighted(base, QuestType.RECOVERY, 4);
                addWeighted(base, QuestType.DELIVERY, 3);
                addWeighted(base, QuestType.EXTRACTION, 2);
                addWeighted(base, QuestType.INVESTIGATION, trust >= 2 ? 2 : 1);
            }
            case TECHIE -> {
                addWeighted(base, QuestType.RECOVERY, 4);
                addWeighted(base, QuestType.EXTRACTION, 3);
                addWeighted(base, QuestType.DELIVERY, 2);
                addWeighted(base, QuestType.INVESTIGATION, 2);
            }
            case NETRUNNER -> {
                addWeighted(base, QuestType.BREACH, 4);
                addWeighted(base, QuestType.INVESTIGATION, 4);
                addWeighted(base, QuestType.EXTRACTION, 3);
                addWeighted(base, QuestType.RECOVERY, trust >= 1 ? 2 : 1);
            }
            case MERC -> {
                addWeighted(base, QuestType.ELIMINATION, 5);
                addWeighted(base, QuestType.EXTRACTION, 3);
                addWeighted(base, QuestType.DELIVERY, 2);
                addWeighted(base, QuestType.RECOVERY, 1);
            }
        }
        return base;
    }

    private static void addWeighted(List<QuestType> pool, QuestType type, int weight) {
        for (int count = 0; count < weight; count++) {
            pool.add(type);
        }
    }

    private static QuestContract generateOffer(ServerPlayer player, AbstractCityNpcEntity issuer, QuestType type, int tier, int index, long day, RandomSource random) {
        int targetIndex = switch (type) {
            case DELIVERY -> 0;
            case EXTRACTION -> 0;
            case INVESTIGATION -> 0;
            case BREACH -> 0;
            case ELIMINATION -> random.nextInt(Math.min(6, 2 + tier * 2));
            case RECOVERY -> random.nextInt(Math.min(5, 2 + tier));
        };
        int targetCount = QuestContract.targetCountFor(type, tier, targetIndex);
        BlockPos targetPos = null;
        String dimensionId = "";
        int radius = switch (type) {
            case INVESTIGATION -> 2;
            case ELIMINATION -> 24;
            case BREACH -> 1;
            default -> 1;
        };
        if (issuer.level() instanceof ServerLevel serverLevel) {
            DeliveryTarget target = switch (type) {
                case DELIVERY -> generateDeliveryTarget(serverLevel, issuer, player, tier, random);
                case EXTRACTION -> generateExtractionTarget(serverLevel, issuer, player, tier, random);
                case INVESTIGATION -> generateInvestigationTarget(serverLevel, issuer, player, tier, random);
                case BREACH -> generateBreachTarget(serverLevel, issuer, player, tier, random);
                case ELIMINATION -> generateHuntTarget(serverLevel, issuer, player, tier, random);
                case RECOVERY -> null;
            };
            if (target != null) {
                targetPos = target.pos();
                dimensionId = target.level().dimension().location().toString();
            }
        }

        int rewardMoney = switch (type) {
            case DELIVERY -> 90 + tier * 65 + random.nextInt(30);
            case EXTRACTION -> 120 + tier * 78 + random.nextInt(35);
            case INVESTIGATION -> 110 + tier * 72 + random.nextInt(35);
            case BREACH -> 165 + tier * 95 + random.nextInt(45);
            case ELIMINATION -> 110 + tier * 80 + random.nextInt(40);
            case RECOVERY -> 100 + tier * 70 + random.nextInt(35);
        };
        rewardMoney = Math.max(45, Math.round(rewardMoney * moneyProfileMultiplier(issuer.npcType())));
        int rewardTrust;
        if (tier >= 3 && (type == QuestType.BREACH || type == QuestType.INVESTIGATION)) {
            rewardTrust = 3;
        } else if (tier >= 3) {
            rewardTrust = 2;
        } else {
            rewardTrust = 1;
        }
        int rewardItemKind = QuestContract.rewardItemKindForTier(tier, type);
        int rewardItemCount = QuestContract.rewardItemCountFor(rewardItemKind);
        String contractId = UUID.nameUUIDFromBytes((player.getUUID() + "|" + issuer.getUUID() + "|" + day + "|" + index + "|" + type.id()).getBytes(StandardCharsets.UTF_8)).toString();
        return new QuestContract(
                contractId,
                issuer.getUUID(),
                issuer.getName().getString(),
                issuer.npcTypeId(),
                type,
                tier,
                QuestStatus.OFFERED,
                targetIndex,
                targetCount,
                0,
                false,
                dimensionId,
                targetPos,
                radius,
                rewardMoney,
                rewardTrust,
                rewardItemKind,
                rewardItemCount,
                day,
                0L
        );
    }

    private static float moneyProfileMultiplier(NpcType npcType) {
        return switch (npcType) {
            case FIXER -> 1.0F;
            case RIPPERDOC, TECHIE -> 0.95F;
            case NETRUNNER -> 1.04F;
            case MERC -> 1.08F;
        };
    }

    private static DeliveryTarget generateDeliveryTarget(ServerLevel preferredLevel, AbstractCityNpcEntity issuer, ServerPlayer player, int tier, RandomSource random) {
        ServerLevel targetLevel = chooseDeliveryLevel(preferredLevel);
        BlockPos origin = targetLevel == preferredLevel
                ? (issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition())
                : player.blockPosition();
        int minDistance = DELIVERY_MIN_DISTANCE + (tier - 1) * 60;
        int maxDistance = Math.min(DELIVERY_MAX_DISTANCE, DELIVERY_MIN_DISTANCE + 260 + tier * 180);
        return new DeliveryTarget(targetLevel, findSurfaceTarget(targetLevel, origin, player, minDistance, maxDistance, random));
    }

    private static DeliveryTarget generateExtractionTarget(ServerLevel preferredLevel, AbstractCityNpcEntity issuer, ServerPlayer player, int tier, RandomSource random) {
        ServerLevel targetLevel = chooseDeliveryLevel(preferredLevel);
        BlockPos origin = targetLevel == preferredLevel
                ? (issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition())
                : player.blockPosition();
        int minDistance = EXTRACTION_MIN_DISTANCE + (tier - 1) * 50;
        int maxDistance = Math.min(EXTRACTION_MAX_DISTANCE, EXTRACTION_MIN_DISTANCE + 220 + tier * 160);
        return new DeliveryTarget(targetLevel, findSurfaceTarget(targetLevel, origin, player, minDistance, maxDistance, random));
    }

    private static DeliveryTarget generateInvestigationTarget(ServerLevel preferredLevel, AbstractCityNpcEntity issuer, ServerPlayer player, int tier, RandomSource random) {
        ServerLevel targetLevel = chooseDeliveryLevel(preferredLevel);
        BlockPos origin = targetLevel == preferredLevel
                ? (issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition())
                : player.blockPosition();
        int minDistance = INVESTIGATION_MIN_DISTANCE + (tier - 1) * 35;
        int maxDistance = Math.min(INVESTIGATION_MAX_DISTANCE, INVESTIGATION_MIN_DISTANCE + 180 + tier * 120);
        return new DeliveryTarget(targetLevel, findSurfaceTarget(targetLevel, origin, player, minDistance, maxDistance, random));
    }

    private static DeliveryTarget generateBreachTarget(ServerLevel preferredLevel, AbstractCityNpcEntity issuer, ServerPlayer player, int tier, RandomSource random) {
        ServerLevel targetLevel = chooseDeliveryLevel(preferredLevel);
        BlockPos origin = targetLevel == preferredLevel
                ? (issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition())
                : player.blockPosition();
        int minDistance = BREACH_MIN_DISTANCE + (tier - 1) * 70;
        int maxDistance = Math.min(BREACH_MAX_DISTANCE, BREACH_MIN_DISTANCE + 260 + tier * 180);
        return new DeliveryTarget(targetLevel, findSurfaceTarget(targetLevel, origin, player, minDistance, maxDistance, random));
    }

    private static DeliveryTarget generateHuntTarget(ServerLevel preferredLevel, AbstractCityNpcEntity issuer, ServerPlayer player, int tier, RandomSource random) {
        ServerLevel targetLevel = chooseDeliveryLevel(preferredLevel);
        BlockPos origin = targetLevel == preferredLevel
                ? (issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition())
                : player.blockPosition();
        int minDistance = 140 + (tier - 1) * 45;
        int maxDistance = 520 + tier * 120;
        return new DeliveryTarget(targetLevel, findSurfaceTarget(targetLevel, origin, player, minDistance, maxDistance, random));
    }

    private static ServerLevel chooseDeliveryLevel(ServerLevel preferredLevel) {
        if (supportsSurfaceStash(preferredLevel)) {
            return preferredLevel;
        }
        ServerLevel overworld = preferredLevel.getServer().overworld();
        if (overworld != null && supportsSurfaceStash(overworld)) {
            return overworld;
        }
        return preferredLevel;
    }

    private static BlockPos findSurfaceTarget(ServerLevel level, BlockPos origin, ServerPlayer player, int minDistance, int maxDistance, RandomSource random) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance));
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (candidate.distManhattan(player.blockPosition()) > 96 && isUsableStashPosition(level, candidate)) {
                return candidate;
            }
        }
        int fallbackX = origin.getX() + Math.max(220, minDistance + 80);
        int fallbackZ = origin.getZ() + Math.max(180, minDistance + 40);
        int fallbackY = level.getHeight(Heightmap.Types.WORLD_SURFACE, fallbackX, fallbackZ);
        BlockPos fallback = new BlockPos(fallbackX, fallbackY, fallbackZ);
        return isUsableStashPosition(level, fallback) ? fallback : fallback.above();
    }

    private static boolean isUsableStashPosition(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || pos.getY() <= level.getMinBuildHeight() + 1) {
            return false;
        }
        if (!supportsSurfaceStash(level)) {
            return false;
        }
        return level.canSeeSky(pos)
                && level.canSeeSky(pos.above())
                && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty()
                && level.getBlockState(pos).canBeReplaced()
                && level.getBlockState(pos.above()).canBeReplaced()
                && !level.getBlockState(pos.below()).isAir()
                && level.getBlockState(pos.below()).blocksMotion()
                && !level.getBlockState(pos.below()).is(Blocks.BEDROCK)
                && level.getFluidState(pos.below()).isEmpty();
    }

    private static boolean supportsSurfaceStash(ServerLevel level) {
        return !level.dimensionType().hasCeiling() && level.dimensionType().hasSkyLight();
    }

    private static @Nullable ServerLevel resolveContractLevel(ServerPlayer player, QuestContract contract) {
        if (contract.dimensionId().isBlank()) {
            return null;
        }
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(contract.dimensionId()));
        return player.serverLevel().getServer().getLevel(levelKey);
    }

    private static void issueCourierDeliveryItems(ServerPlayer player, QuestContract contract) {
        ItemStack packageStack = createCourierPackage(contract);
        ItemStack compassStack = createHiddenStashCompass(contract);
        giveQuestItem(player, packageStack);
        giveQuestItem(player, compassStack);
    }

    private static void issueExtractionItems(ServerPlayer player, QuestContract contract) {
        giveQuestItem(player, createQuestCompass(contract, QUEST_ITEM_TYPE_EXTRACTION_COMPASS, "item.cyberneticenhancements.extraction_compass"));
    }

    private static void issueInvestigationItems(ServerPlayer player, QuestContract contract) {
        giveQuestItem(player, createQuestCompass(contract, QUEST_ITEM_TYPE_INVESTIGATION_COMPASS, "item.cyberneticenhancements.signal_trace_compass"));
    }

    private static void issueBreachItems(ServerPlayer player, QuestContract contract) {
        giveQuestItem(player, createQuestCompass(contract, QUEST_ITEM_TYPE_BREACH_COMPASS, "item.cyberneticenhancements.breach_vector_compass"));
    }

    private static void issueEliminationItems(ServerPlayer player, QuestContract contract) {
        giveQuestItem(player, createQuestCompass(contract, QUEST_ITEM_TYPE_HUNT_COMPASS, "item.cyberneticenhancements.hunt_compass"));
    }

    private static ItemStack createCourierPackage(QuestContract contract) {
        ItemStack stack = new ItemStack(ModItems.COURIER_PACKAGE.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.cyberneticenhancements.courier_package"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_PACKAGE)));
        return stack;
    }

    private static ItemStack createHiddenStashCompass(QuestContract contract) {
        return createQuestCompass(contract, QUEST_ITEM_TYPE_COMPASS, "item.cyberneticenhancements.hidden_stash_compass");
    }

    private static ItemStack createQuestCompass(QuestContract contract, String itemType, String nameKey) {
        ItemStack stack = new ItemStack(Items.COMPASS);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(nameKey));
        applyQuestCompassTarget(stack, contract);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, itemType)));
        return stack;
    }

    private static ItemStack createExtractionShard(QuestContract contract) {
        ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.cyberneticenhancements.exfil_data_shard"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_EXTRACTION)));
        return stack;
    }

    private static void applyQuestCompassTarget(ItemStack stack, QuestContract contract) {
        if (contract.targetPos() == null || contract.dimensionId().isBlank()) {
            return;
        }
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(contract.dimensionId()));
        stack.set(
                DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(GlobalPos.of(levelKey, contract.targetPos().below())), false)
        );
    }

    private static CompoundTag questItemTag(QuestContract contract, String itemType) {
        CompoundTag tag = new CompoundTag();
        tag.putString(QUEST_ITEM_TYPE_TAG, itemType);
        tag.putString(QUEST_CONTRACT_ID_TAG, contract.id());
        tag.putUUID(QUEST_ISSUER_ID_TAG, contract.issuerId());
        return tag;
    }

    private static void giveQuestItem(ServerPlayer player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!player.addItem(copy)) {
            player.drop(copy, false);
        }
    }

    private static boolean ensureCourierStash(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null) {
            return false;
        }
        BlockPos chestPos = contract.targetPos();
        BlockPos lodestonePos = chestPos.below();
        boolean dirty = false;
        if (!level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.setBlock(lodestonePos, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
            dirty = true;
        }
        if (!level.getBlockState(chestPos).is(Blocks.CHEST)) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            dirty = true;
        }
        if (dirty && level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestBlockEntity) {
            chestBlockEntity.setChanged();
        }
        return dirty;
    }

    private static boolean ensureExtractionDrop(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null || contract.destinationReached()) {
            return false;
        }
        BlockPos barrelPos = contract.targetPos();
        BlockPos lodestonePos = barrelPos.below();
        boolean dirty = false;
        if (!level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.setBlock(lodestonePos, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
            dirty = true;
        }
        if (!level.getBlockState(barrelPos).is(Blocks.BARREL)) {
            level.setBlock(barrelPos, Blocks.BARREL.defaultBlockState(), Block.UPDATE_ALL);
            dirty = true;
        }
        if (level.getBlockEntity(barrelPos) instanceof Container container) {
            if (!containerContainsQuestItem(container, contract, QUEST_ITEM_TYPE_EXTRACTION)) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    if (container.getItem(slot).isEmpty()) {
                        container.setItem(slot, createExtractionShard(contract));
                        container.setChanged();
                        if (level.getBlockEntity(barrelPos) != null) {
                            level.getBlockEntity(barrelPos).setChanged();
                        }
                        dirty = true;
                        break;
                    }
                }
            }
        }
        return dirty;
    }

    private static boolean ensureInvestigationMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null || contract.isReady()) {
            return false;
        }
        BlockPos markerPos = contract.targetPos();
        BlockPos lodestonePos = markerPos.below();
        boolean dirty = false;
        if (!level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.setBlock(lodestonePos, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
            dirty = true;
        }
        if (!level.getBlockState(markerPos).is(Blocks.END_ROD)) {
            level.setBlock(markerPos, Blocks.END_ROD.defaultBlockState(), Block.UPDATE_ALL);
            dirty = true;
        }
        return dirty;
    }

    private static boolean ensureBreachSite(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null || contract.isReady()) {
            return false;
        }
        BlockPos cachePos = contract.targetPos();
        BlockPos lodestonePos = cachePos.below();
        boolean dirty = false;
        if (!level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.setBlock(lodestonePos, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
            dirty = true;
        }
        BlockState expectedState = breachCacheStateFor(contract.tier());
        if (level.getBlockState(cachePos).getBlock() != expectedState.getBlock()) {
            level.setBlock(cachePos, expectedState, Block.UPDATE_ALL);
            dirty = true;
        }
        return dirty;
    }

    private static boolean ensureEliminationMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null || contract.isReady()) {
            return false;
        }
        BlockPos lodestonePos = contract.targetPos().below();
        if (!level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.setBlock(lodestonePos, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    private static boolean consumeCourierPackageFromStash(ServerLevel level, QuestContract contract) {
        if (contract.targetPos() == null) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(contract.targetPos());
        if (!(blockEntity instanceof Container container)) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isQuestCourierPackage(stack, contract)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    container.setItem(slot, ItemStack.EMPTY);
                } else {
                    container.setItem(slot, stack);
                }
                container.setChanged();
                if (blockEntity != null) {
                    blockEntity.setChanged();
                }
                return true;
            }
        }
        return false;
    }

    private static boolean containerContainsQuestItem(Container container, QuestContract contract, String itemType) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (matchesQuestItem(container.getItem(slot), contract, itemType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isQuestCourierPackage(ItemStack stack, QuestContract contract) {
        if (!stack.is(ModItems.COURIER_PACKAGE.get()) || !stack.has(DataComponents.CUSTOM_DATA)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return QUEST_ITEM_TYPE_PACKAGE.equals(tag.getString(QUEST_ITEM_TYPE_TAG))
                && contract.id().equals(tag.getString(QUEST_CONTRACT_ID_TAG));
    }

    private static BlockState breachCacheStateFor(int tier) {
        return switch (Math.max(1, tier)) {
            case 1 -> ModBlocks.UNCOMMON_RELIC_CACHE.get().defaultBlockState();
            case 2 -> ModBlocks.RARE_RELIC_CACHE.get().defaultBlockState();
            default -> ModBlocks.EPIC_RELIC_CACHE.get().defaultBlockState();
        };
    }

    private static void cleanupContractArtifacts(ServerPlayer player, QuestContract contract) {
        switch (contract.type()) {
            case DELIVERY -> {
                cleanupCourierGuidanceItems(player, contract);
                cleanupCourierStash(resolveContractLevel(player, contract), contract);
            }
            case EXTRACTION -> {
                cleanupExtractionItems(player, contract);
                cleanupExtractionMarker(resolveContractLevel(player, contract), contract);
            }
            case INVESTIGATION -> {
                cleanupInvestigationCompass(player, contract);
                cleanupInvestigationMarker(resolveContractLevel(player, contract), contract);
            }
            case BREACH -> {
                cleanupBreachCompass(player, contract);
                cleanupBreachMarker(resolveContractLevel(player, contract), contract);
            }
            case ELIMINATION -> {
                cleanupEliminationCompass(player, contract);
                cleanupEliminationMarker(resolveContractLevel(player, contract), contract);
            }
            default -> {
            }
        }
    }

    private static void cleanupCourierGuidanceItems(ServerPlayer player, QuestContract contract) {
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_PACKAGE);
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_COMPASS);
    }

    private static void cleanupExtractionItems(ServerPlayer player, QuestContract contract) {
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_EXTRACTION);
        cleanupExtractionCompass(player, contract);
    }

    private static void cleanupExtractionCompass(ServerPlayer player, QuestContract contract) {
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_EXTRACTION_COMPASS);
    }

    private static void cleanupInvestigationCompass(ServerPlayer player, QuestContract contract) {
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_INVESTIGATION_COMPASS);
    }

    private static void cleanupBreachCompass(ServerPlayer player, QuestContract contract) {
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_BREACH_COMPASS);
    }

    private static void cleanupEliminationCompass(ServerPlayer player, QuestContract contract) {
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_HUNT_COMPASS);
    }

    private static void removeTaggedQuestItems(ServerPlayer player, QuestContract contract, String itemType) {
        for (ItemStack stack : player.getInventory().items) {
            if (matchesQuestItem(stack, contract, itemType)) {
                stack.setCount(0);
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (matchesQuestItem(stack, contract, itemType)) {
                stack.setCount(0);
            }
        }
    }

    private static boolean hasTaggedQuestItemInInventory(ServerPlayer player, QuestContract contract, String itemType) {
        for (ItemStack stack : player.getInventory().items) {
            if (matchesQuestItem(stack, contract, itemType)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (matchesQuestItem(stack, contract, itemType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean ensureQuestGuidanceItem(ServerPlayer player, QuestContract contract, String itemType, Supplier<ItemStack> supplier) {
        if (hasTaggedQuestItemInInventory(player, contract, itemType)) {
            return false;
        }
        giveQuestItem(player, supplier.get());
        return true;
    }

    private static boolean ensureCourierPackage(ServerPlayer player, ServerLevel level, QuestContract contract) {
        if (hasTaggedQuestItemInInventory(player, contract, QUEST_ITEM_TYPE_PACKAGE)
                || courierStashContainsPackage(level, contract)) {
            return false;
        }
        giveQuestItem(player, createCourierPackage(contract));
        return true;
    }

    private static boolean courierStashContainsPackage(ServerLevel level, QuestContract contract) {
        if (contract.targetPos() == null) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(contract.targetPos());
        return blockEntity instanceof Container container && containerContainsQuestItem(container, contract, QUEST_ITEM_TYPE_PACKAGE);
    }

    private static boolean canForceReadyDeliveryTurnIn(ServerPlayer player, QuestContract contract) {
        if (contract.type() != QuestType.DELIVERY || contract.targetPos() == null) {
            return false;
        }
        if (contract.destinationReached()) {
            return true;
        }
        ServerLevel level = resolveContractLevel(player, contract);
        if (level == null) {
            return false;
        }
        boolean packageStillHeld = hasTaggedQuestItemInInventory(player, contract, QUEST_ITEM_TYPE_PACKAGE)
                || hasTaggedQuestItemInInventory(player, contract, QUEST_ITEM_TYPE_COMPASS);
        boolean stashStillPresent = level.getBlockState(contract.targetPos()).is(Blocks.CHEST);
        return !packageStillHeld && !stashStillPresent;
    }

    private static void refreshCourierCompassTargets(ServerPlayer player, QuestContract contract) {
        for (ItemStack stack : player.getInventory().items) {
            if (isQuestCompassForContract(stack, contract)) {
                applyQuestCompassTarget(stack, contract);
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isQuestCompassForContract(stack, contract)) {
                applyQuestCompassTarget(stack, contract);
            }
        }
    }

    private static boolean isQuestCompassForContract(ItemStack stack, QuestContract contract) {
        return matchesQuestItem(stack, contract, QUEST_ITEM_TYPE_COMPASS)
                || matchesQuestItem(stack, contract, QUEST_ITEM_TYPE_EXTRACTION_COMPASS)
                || matchesQuestItem(stack, contract, QUEST_ITEM_TYPE_INVESTIGATION_COMPASS)
                || matchesQuestItem(stack, contract, QUEST_ITEM_TYPE_BREACH_COMPASS);
    }

    private static boolean matchesQuestItem(ItemStack stack, QuestContract contract, String itemType) {
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return itemType.equals(tag.getString(QUEST_ITEM_TYPE_TAG))
                && contract.id().equals(tag.getString(QUEST_CONTRACT_ID_TAG));
    }

    private static void cleanupCourierStash(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null) {
            return;
        }
        BlockPos chestPos = contract.targetPos();
        BlockPos lodestonePos = chestPos.below();
        if (level.getBlockState(chestPos).is(Blocks.CHEST)) {
            level.removeBlock(chestPos, false);
        }
        if (level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.removeBlock(lodestonePos, false);
        }
    }

    private static void cleanupExtractionMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null) {
            return;
        }
        BlockPos barrelPos = contract.targetPos();
        BlockPos lodestonePos = barrelPos.below();
        if (level.getBlockState(barrelPos).is(Blocks.BARREL)) {
            level.removeBlock(barrelPos, false);
        }
        if (level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.removeBlock(lodestonePos, false);
        }
    }

    private static void cleanupInvestigationMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null) {
            return;
        }
        BlockPos markerPos = contract.targetPos();
        BlockPos lodestonePos = markerPos.below();
        if (level.getBlockState(markerPos).is(Blocks.END_ROD)) {
            level.removeBlock(markerPos, false);
        }
        if (level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.removeBlock(lodestonePos, false);
        }
    }

    private static void cleanupBreachMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null) {
            return;
        }
        BlockPos cachePos = contract.targetPos();
        BlockPos lodestonePos = cachePos.below();
        BlockEntity blockEntity = level.getBlockEntity(cachePos);
        if (blockEntity instanceof RelicCacheBlockEntity cacheEntity && (cacheEntity.isAnimating() || cacheEntity.hasActiveHackSession())) {
            return;
        }
        if (level.getBlockState(cachePos).getBlock() == ModBlocks.RELIC_CACHE.get()
                || level.getBlockState(cachePos).getBlock() == ModBlocks.UNCOMMON_RELIC_CACHE.get()
                || level.getBlockState(cachePos).getBlock() == ModBlocks.RARE_RELIC_CACHE.get()
                || level.getBlockState(cachePos).getBlock() == ModBlocks.EPIC_RELIC_CACHE.get()
                || level.getBlockState(cachePos).getBlock() == ModBlocks.LEGENDARY_RELIC_CACHE.get()) {
            level.removeBlock(cachePos, false);
        }
        if (level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.removeBlock(lodestonePos, false);
        }
    }

    private static void cleanupEliminationMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null) {
            return;
        }
        BlockPos lodestonePos = contract.targetPos().below();
        if (level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.removeBlock(lodestonePos, false);
        }
    }

    private static void advanceInvestigationTarget(ServerPlayer player, QuestContract contract) {
        ServerLevel currentLevel = resolveContractLevel(player, contract);
        ServerLevel targetLevel = currentLevel != null ? currentLevel : chooseDeliveryLevel(player.serverLevel());
        BlockPos origin = contract.targetPos() == null ? player.blockPosition() : contract.targetPos();
        RandomSource random = RandomSource.create(seedForInvestigationStep(contract, contract.progress()));
        BlockPos nextPos = findSurfaceTarget(
                targetLevel,
                origin,
                player,
                INVESTIGATION_MIN_DISTANCE / 2,
                Math.min(INVESTIGATION_MAX_DISTANCE, INVESTIGATION_MIN_DISTANCE + 140 + contract.progress() * 100),
                random
        );
        contract.setTargetLocation(targetLevel.dimension().location().toString(), nextPos, 2);
        ensureInvestigationMarker(targetLevel, contract);
    }

    private static long seedForInvestigationStep(QuestContract contract, int step) {
        return contract.id().hashCode() * 31L + step * 341873128712L;
    }

    private static long seedFor(ServerPlayer player, AbstractCityNpcEntity fixer, long day) {
        return player.getUUID().getLeastSignificantBits()
                ^ player.getUUID().getMostSignificantBits()
                ^ fixer.getUUID().getLeastSignificantBits()
                ^ fixer.getUUID().getMostSignificantBits()
                ^ (day * 341873128712L);
    }

    private static int countQuestEntries(CompoundTag root) {
        CompoundTag boards = issuerBoards(root);
        int count = 0;
        for (String issuerKey : boards.getAllKeys()) {
            CompoundTag board = boards.getCompound(issuerKey);
            if (board.contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                count++;
            }
            count += board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND).size();
            count += board.getList(BOARD_HISTORY_TAG, Tag.TAG_COMPOUND).size();
        }
        return count;
    }

    private static void applyAbandonPenalty(ServerPlayer player, QuestContract contract) {
        int trustLoss = Math.max(2, contract.rewardTrust() + 2);
        if (!contract.issuerType().isBlank()) {
            AbstractCityNpcEntity.lowerTrustFromArchive(player, contract.issuerId(), trustLoss);
        }
    }

    private static int globalActiveCount(ServerPlayer player) {
        int count = 0;
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return 0;
        }
        CompoundTag boards = issuerBoards(root);
        for (String key : boards.getAllKeys()) {
            if (boards.getCompound(key).contains(BOARD_ACTIVE_TAG, Tag.TAG_COMPOUND)) {
                count++;
            }
        }
        return count;
    }

    private static void appendHistory(CompoundTag board, QuestContract contract) {
        List<QuestContract> history = readContracts(board.getList(BOARD_HISTORY_TAG, Tag.TAG_COMPOUND));
        history.add(0, contract);
        if (history.size() > MAX_HISTORY) {
            history = new ArrayList<>(history.subList(0, MAX_HISTORY));
        }
        ListTag historyTag = new ListTag();
        for (QuestContract entry : history) {
            historyTag.add(entry.toTag());
        }
        board.put(BOARD_HISTORY_TAG, historyTag);
    }

    private static void writeOffers(CompoundTag board, List<QuestContract> offers) {
        ListTag offersTag = new ListTag();
        for (QuestContract offer : offers) {
            offersTag.add(offer.toTag());
        }
        board.put(BOARD_OFFERS_TAG, offersTag);
    }

    private static void writeActive(CompoundTag board, @Nullable QuestContract active) {
        if (active == null) {
            board.remove(BOARD_ACTIVE_TAG);
        } else {
            board.put(BOARD_ACTIVE_TAG, active.toTag());
        }
    }

    private static List<QuestContract> readContracts(ListTag listTag) {
        List<QuestContract> entries = new ArrayList<>(listTag.size());
        for (Tag tag : listTag) {
            if (tag instanceof CompoundTag compoundTag) {
                entries.add(QuestContract.fromTag(compoundTag));
            }
        }
        return entries;
    }

    private static @Nullable CompoundTag issuerBoardTag(ServerPlayer player, AbstractCityNpcEntity fixer, boolean create) {
        CompoundTag root = rootTag(player, create);
        if (root == null) {
            return null;
        }
        CompoundTag boards = issuerBoards(root);
        String key = fixer.getStringUUID();
        if (!boards.contains(key, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }
            boards.put(key, new CompoundTag());
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
        }
        return boards.getCompound(key);
    }

    private static void storeIssuerBoard(ServerPlayer player, AbstractCityNpcEntity fixer, CompoundTag board) {
        CompoundTag root = rootTag(player, true);
        CompoundTag boards = issuerBoards(root);
        boards.put(fixer.getStringUUID(), board);
        root.put(ISSUER_BOARDS_TAG, boards);
        player.getPersistentData().put(ROOT_TAG, root);
    }

    private static @Nullable CompoundTag rootTag(ServerPlayer player, boolean create) {
        if (!player.getPersistentData().contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }
            player.getPersistentData().put(ROOT_TAG, new CompoundTag());
        }
        return player.getPersistentData().getCompound(ROOT_TAG);
    }

    private static CompoundTag issuerBoards(CompoundTag root) {
        if (root.contains(ISSUER_BOARDS_TAG, Tag.TAG_COMPOUND)) {
            return root.getCompound(ISSUER_BOARDS_TAG);
        }
        if (root.contains(LEGACY_FIXER_BOARDS_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag migrated = root.getCompound(LEGACY_FIXER_BOARDS_TAG).copy();
            root.put(ISSUER_BOARDS_TAG, migrated);
            root.remove(LEGACY_FIXER_BOARDS_TAG);
            return migrated;
        }
        return new CompoundTag();
    }

    private static long currentDay(Level level) {
        return level.getDayTime() / 24000L;
    }

    public record BoardSnapshot(@Nullable QuestContract active, List<QuestContract> offers) {
    }

    public enum Result {
        OK,
        INVALID,
        HAS_ACTIVE,
        MAX_ACTIVE
    }

    public enum TurnInResult {
        OK,
        NOT_READY,
        MISSING_ITEMS
    }

    public record QuestLogEntry(
            String questId,
            String issuerId,
            String issuerName,
            String issuerType,
            String typeId,
            String title,
            String objective,
            String reward,
            String statusId,
            String statusLabel,
            int tier,
            boolean hasLocation,
            String dimensionId,
            int x,
            int y,
            int z,
            int sortGroup
    ) {
    }

    private record DeliveryTarget(ServerLevel level, BlockPos pos) {
    }
}
