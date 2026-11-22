package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.dialogue.NpcIncomingCalls;
import de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PlayerQuestManager {
    private static final java.util.Map<ServerPlayer, QuestItemRecovery> ITEM_RECOVERY = new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerPlayer, SurveyScan> SURVEY_SCANS = new java.util.WeakHashMap<>();
    private record SurveyScan(String questId, net.minecraft.world.phys.Vec3 origin, int started, int lastTick) {}
    private static final String ROOT_TAG = "cyberneticenhancements.quest_state";
    private static final String ISSUER_BOARDS_TAG = "IssuerBoards";
    private static final String LEGACY_FIXER_BOARDS_TAG = "FixerBoards";
    private static final String BOARD_REFRESH_DAY_TAG = "RefreshDay";
    private static final String BOARD_OFFERS_TAG = "Offers";
    private static final String BOARD_ACTIVE_TAG = "Active";
    private static final String[] ACTIVE_TAGS = {BOARD_ACTIVE_TAG, "Active2", "Active3"};
    /** How many contracts one issuer may have open with the player at once. */
    public static final int MAX_ACTIVE_PER_ISSUER = ACTIVE_TAGS.length;
    private static final String BOARD_HISTORY_TAG = "History";
    public static final int DEV_OFFER_COUNT = QuestType.values().length + 1;
    private static final int MAX_OFFERS = 3;
    private static final int MAX_HISTORY = 8;
    private static final String QUEST_ITEM_TYPE_TAG = "QuestItemType";
    private static final String QUEST_CONTRACT_ID_TAG = "QuestContractId";
    private static final String QUEST_ISSUER_ID_TAG = "QuestIssuerId";
    private static final String QUEST_ITEM_TYPE_PACKAGE = "courier_package";
    private static final String QUEST_ITEM_TYPE_REVIVE = "revive_injector", QUEST_ITEM_TYPE_BEACON = "signal_beacon";
    private static final String QUEST_ITEM_TYPE_EXTRACTION = "exfil_shard";
    private static final String QUEST_ITEM_TYPE_PSYCHO = "cyberpsycho_dossier";
    private static final String QUEST_ITEM_TYPE_SALVAGE = "salvage_cargo";
    private static final Set<String> LEGACY_COMPASS_TYPES = Set.of("hidden_stash_compass", "extraction_compass",
            "investigation_compass", "breach_compass", "hunt_compass");
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

    public static void updateCreativeCursor(ServerPlayer player, String contractId, String itemType, long revision) {
        var recovery = ITEM_RECOVERY.computeIfAbsent(player, ignored -> new QuestItemRecovery());
        boolean valid = player.isCreative() && activeContracts(player).stream().anyMatch(contract -> contract.id().equals(contractId));
        if (valid && (LEGACY_COMPASS_TYPES.contains(itemType)
                || !QuestItemVersions.get(player.serverLevel()).current(contractId + "/" + itemType, revision))) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new de.artemis.cyberneticenhancements.common.network.QuestCursorPayload(contractId, itemType, revision));
            valid = false;
        }
        recovery.cursor(valid ? contractId : "", valid ? itemType : "", player.tickCount);
    }

    public static void onQuestItemPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        var stack = event.getItemEntity().getItem();
        if (isLegacyQuestCompass(stack) || !QuestItemVersions.get(player.serverLevel()).current(stack)) {
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            event.getItemEntity().discard();
        }
    }

    static void sanitizeQuestItems(ServerPlayer player) {
        var versions = QuestItemVersions.get(player.serverLevel());
        Set<String> seen = new HashSet<>();
        Set<ItemStack> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        var stacks = new ArrayList<ItemStack>();
        stacks.add(player.containerMenu.getCarried());
        stacks.add(player.inventoryMenu.getCarried());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) stacks.add(player.getInventory().getItem(slot));
        for (var slot : player.containerMenu.slots) stacks.add(slot.getItem());
        boolean changed = false;
        for (var stack : stacks) {
            if (!visited.add(stack)) continue;
            if (isLegacyQuestCompass(stack)) {
                stack.setCount(0);
                changed = true;
                continue;
            }
            String key = QuestItemVersions.key(stack);
            if (key.isEmpty()) continue;
            if (!versions.current(stack) || !seen.add(key)) {
                stack.setCount(0);
                changed = true;
            } else if (stack.getCount() > 1) {
                stack.setCount(1);
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            for (var slot : player.containerMenu.slots) slot.setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        }
    }

    public static void copyState(Player original, Player clone) {
        QuestFailureCalls.copyState(original, clone);
        CompoundTag source = original.getPersistentData().getCompound(ROOT_TAG);
        if (!source.isEmpty()) {
            clone.getPersistentData().put(ROOT_TAG, source.copy());
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getEntity().level().isClientSide()) {
            return;
        }
        sanitizeQuestItems(player);
        if (player.tickCount % 20 == 7) claimFoundCargo(player);
        tickActiveContracts(player);
        if (player.tickCount % 20 == 0) tickLostItemCalls(player);
        if (player.tickCount % 20 == 0) QuestFailureCalls.tick(player);
        if (player.tickCount % 5 == 0) QuestProgress.sync(player);
        if (player.tickCount % 20 == 0) QuestNavigation.sync(player);
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

    public static boolean onRelicCacheBreached(ServerPlayer player, BlockPos pos) {
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            return false;
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            return false;
        }
        boolean dirty = false;
        String dimensionId = player.serverLevel().dimension().location().toString();
        for (var ref : activeSlots(boards)) {
            String issuerKey = ref.key();
            CompoundTag board = boards.getCompound(issuerKey);
            QuestContract contract = QuestContract.fromTag(board.getCompound(ref.slot()));
            boolean investigationSource = contract.type() == QuestType.INVESTIGATION && !contract.chromiumSurvey()
                    && contract.investigation() != null && contract.investigation().located();
            if ((contract.type() != QuestType.BREACH && !investigationSource) || contract.status() != QuestStatus.ACTIVE
                    || contract.targetPos() == null || contract.urgentTimeIsUp(NpcIncomingCalls.playTicks(player))) {
                continue;
            }
            if (dimensionId.equals(contract.dimensionId()) && contract.targetPos().equals(pos)) {
                contract.setProgress(contract.targetCount());
                if (investigationSource) contract.setDestinationReached(true);
                markQuestReady(player, contract);
                writeActive(board, contract);
                boards.put(issuerKey, board);
                dirty = true;
            }
        }
        if (dirty) {
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
        }
        return dirty;
    }

    public static boolean urgentDeadlineExpired(ServerPlayer player, String questId) {
        long playTicks = NpcIncomingCalls.playTicks(player);
        return activeContracts(player).stream().anyMatch(contract -> contract.id().equals(questId) && contract.urgentTimeIsUp(playTicks));
    }

    public static void onQuestCacheAttemptStarted(ServerPlayer player, BlockPos pos) {
        updateQuestCacheAttempt(player, pos, false);
    }

    public static void onQuestCacheFailed(ServerPlayer player, BlockPos pos) {
        updateQuestCacheAttempt(player, pos, true);
    }

    private static boolean isQuestCacheObjective(QuestContract contract) {
        return contract.type() == QuestType.BREACH || (contract.type() == QuestType.INVESTIGATION && !contract.chromiumSurvey()
                && contract.investigation() != null && contract.investigation().located());
    }

    private static void updateQuestCacheAttempt(ServerPlayer player, BlockPos pos, boolean failed) {
        var root = rootTag(player, false);
        if (root == null) return;
        var boards = issuerBoards(root);
        boolean dirty = false;
        for (var ref : activeSlots(boards)) {
            String key = ref.key();
            var board = boards.getCompound(key);
            var contract = QuestContract.fromTag(board.getCompound(ref.slot()));
            if (contract.status() != QuestStatus.ACTIVE || !isQuestCacheObjective(contract)
                    || !pos.equals(contract.targetPos()) || !player.level().dimension().location().toString().equals(contract.dimensionId())) continue;
            if (failed) finishFailedBreach(player, board, contract);
            else {
                contract.startBreachAttempt();
                writeActive(board, contract);
            }
            boards.put(key, board);
            dirty = true;
        }
        if (dirty) {
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
            QuestNavigation.sync(player);
        }
    }

    private static void finishFailedBreach(ServerPlayer player, CompoundTag board, QuestContract contract) {
        contract.setStatus(QuestStatus.FAILED);
        contract.setResolvedDay(currentDay(player.level()));
        cleanupContractArtifacts(player, contract);
        removeActive(board, contract.id());
        appendHistory(player, board, contract);
        QuestFailureCalls.queue(player, contract);
        int loss = de.artemis.cyberneticenhancements.common.skills.SkillEffects.trustLoss(player, Math.max(2, contract.rewardTrust() + 2));
        AbstractCityNpcEntity.lowerTrustFromArchive(player, contract.issuerId(), loss);
        var feedback = Component.translatable("message.cyberneticenhancements.quest.breach_failed", contract.title(), contract.issuerName(), loss)
                .withStyle(net.minecraft.ChatFormatting.RED);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, feedback, PlayerActionFeedback.FAILED);
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.message(player, "quests", feedback);
    }

    /** Both of the patient quest's items matter: the injector until it is used, then the beacon until it is handed in. */
    private static boolean patientQuestItemLost(ServerPlayer player, QuestContract contract) {
        if (contract.type() != QuestType.LOCATE || contract.status() != QuestStatus.ACTIVE && contract.status() != QuestStatus.READY) return false;
        var recovery = ITEM_RECOVERY.computeIfAbsent(player, ignored -> new QuestItemRecovery());
        boolean beaconLost = recovery.missingLongEnough(contract.id(), QUEST_ITEM_TYPE_BEACON, hasTaggedQuestItem(player, contract, QUEST_ITEM_TYPE_BEACON), player.isCreative(), player.tickCount);
        boolean injectorLost = !contract.patientRevived()
                && recovery.missingLongEnough(contract.id(), QUEST_ITEM_TYPE_REVIVE, hasTaggedQuestItem(player, contract, QUEST_ITEM_TYPE_REVIVE), player.isCreative(), player.tickCount);
        return beaconLost || injectorLost;
    }

    /** A failed patient quest: the leader at the site it came from will not deal with the player for a while. */
    private static void lockOutEncounter(ServerPlayer player, QuestContract contract) {
        if (contract.type() == QuestType.LOCATE && contract.returnPos() != null)
            de.artemis.cyberneticenhancements.common.world.WorldEncounterOffenses.lockOut(player, contract.returnPos());
    }

    private static void failPatientQuest(ServerPlayer player, CompoundTag board, QuestContract contract) {
        contract.setStatus(QuestStatus.FAILED);
        lockOutEncounter(player, contract);
        contract.setResolvedDay(currentDay(player.level()));
        QuestFailureCalls.queue(player, contract); // Before the cleanup, which would otherwise close the contact the call needs.
        cleanupContractArtifacts(player, contract);
        removeActive(board, contract.id());
        appendHistory(player, board, contract);
        var feedback = Component.translatable("message.cyberneticenhancements.quest.locate_failed", contract.title(), contract.issuerName())
                .withStyle(net.minecraft.ChatFormatting.RED);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, feedback, PlayerActionFeedback.FAILED);
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.message(player, "quests", feedback);
    }

    /** Right-click on the patient with the injector revives them; the beacon works anywhere, see {@link #useSignalBeacon}. */
    public static boolean usePatientItem(ServerPlayer player, de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity patient, ItemStack held) {
        if (patient.role() != de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity.Role.PATIENT) return false;
        var contract = activeContracts(player).stream().filter(quest -> quest.type() == QuestType.LOCATE && quest.status() != QuestStatus.FAILED
                && patient.getUUID().equals(quest.patientId())).findFirst().orElse(null);
        if (contract == null) return false;
        var level = player.serverLevel();
        if (!contract.patientRevived() && matchesQuestItem(held, contract, QUEST_ITEM_TYPE_REVIVE)) {
            held.shrink(1);
            contract.addPatientStage(QuestContract.PATIENT_REVIVED);
            patient.setGlowingTag(false);
            level.playSound(null, patient.blockPosition(), de.artemis.cyberneticenhancements.common.registry.ModSounds.HEALING.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getY() + 1.0, patient.getZ(), 18, 0.4, 0.5, 0.4, 0.02);
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.quest.locate.revived").withStyle(net.minecraft.ChatFormatting.GREEN));
            de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "quests", "quest_progress", contract.title(), contract.objectiveLine());
            storeActiveContract(player, contract);
            return true;
        }
        if (contract.patientRevived()) return false;
        player.displayClientMessage(Component.translatable("message.cyberneticenhancements.quest.locate.needs_injector"), true);
        return false;
    }

    /** Right-click with the signal beacon, anywhere: it logs where the player stands. Whether that was near the patient is only judged at the hand-in. */
    public static boolean useSignalBeacon(ServerPlayer player, ItemStack held) {
        var contract = activeContracts(player).stream().filter(quest -> quest.type() == QuestType.LOCATE && quest.status() == QuestStatus.ACTIVE
                && matchesQuestItem(held, quest, QUEST_ITEM_TYPE_BEACON)).findFirst().orElse(null);
        if (contract == null) return false;
        if (contract.beaconLogged()) {
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.quest.locate.already_logged"), true);
            return true;
        }
        var spot = player.blockPosition();
        var tag = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong("BeaconPos", spot.asLong());
        held.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        held.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.translatable("tooltip.cyberneticenhancements.signal_beacon.set", spot.getX(), spot.getY(), spot.getZ()),
                Component.translatable("tooltip.cyberneticenhancements.signal_beacon.return", contract.issuerName()),
                Component.translatable("tooltip.cyberneticenhancements.quest_item.lose").withStyle(net.minecraft.ChatFormatting.RED))));
        held.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        contract.setBeaconPos(spot);
        contract.addPatientStage(QuestContract.PATIENT_MARKED);
        contract.setDestinationReached(true);
        contract.setProgress(contract.targetCount());
        if (contract.returnPos() != null) contract.setTargetLocation(contract.dimensionId(), contract.returnPos(), 1);
        player.serverLevel().playSound(null, spot, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        markQuestReady(player, contract);
        storeActiveContract(player, contract);
        return true;
    }

    /** A hand-in whose beacon was not logged beside a revived patient: no pay, and the questgiver goes for the player. */
    private static void rejectPatientReport(ServerPlayer player, AbstractCityNpcEntity issuer, CompoundTag board, QuestContract contract) {
        cleanupContractArtifacts(player, contract);
        contract.setStatus(QuestStatus.FAILED);
        lockOutEncounter(player, contract);
        contract.setResolvedDay(currentDay(player.level()));
        removeActive(board, contract.id());
        appendHistory(player, board, contract);
        storeIssuerBoard(player, issuer, board);
        var feedback = Component.translatable("message.cyberneticenhancements.quest.locate.false_report", contract.issuerName())
                .withStyle(net.minecraft.ChatFormatting.RED);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, feedback, PlayerActionFeedback.FAILED);
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.message(player, "quests", feedback);
        if (issuer instanceof de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity encounter) encounter.provoke(player);
        QuestNavigation.sync(player);
    }

    public static void storeActiveContract(ServerPlayer player, QuestContract contract) {
        CompoundTag root = rootTag(player, false);
        if (root == null) return;
        CompoundTag boards = issuerBoards(root);
        String key = contract.issuerId().toString();
        if (!boards.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag board = boards.getCompound(key);
        writeActive(board, contract);
        boards.put(key, board);
        root.put(ISSUER_BOARDS_TAG, boards);
        player.getPersistentData().put(ROOT_TAG, root);
        QuestNavigation.sync(player);
    }

    private static boolean questCacheAttemptLost(ServerPlayer player, QuestContract contract) {
        if (contract.status() != QuestStatus.ACTIVE || !isQuestCacheObjective(contract) || contract.targetPos() == null) return false;
        var level = resolveContractLevel(player, contract);
        if (level == null || !level.hasChunkAt(contract.targetPos())) return false;
        var blockEntity = level.getBlockEntity(contract.targetPos());
        if (blockEntity instanceof RelicCacheBlockEntity cache && cache.isBoundToQuest(player.getUUID(), contract.id()))
            return cache.questAttemptFailed() || (contract.breachAttemptStarted() && !cache.hasActiveHackSession());
        return contract.breachAttemptStarted();
    }

    public static BoardSnapshot snapshotForIssuer(ServerPlayer player, AbstractCityNpcEntity issuer) {
        if (!issuer.canInteract(player)) return new BoardSnapshot(List.of(), List.of());
        ensureIssuerBoard(player, issuer);
        CompoundTag board = issuerBoardTag(player, issuer, true);
        List<QuestContract> offers = readContracts(board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND));
        List<QuestContract> actives = new ArrayList<>();
        boolean expired = false;
        for (QuestContract active : boardActives(board)) {
            var repaired = CyberpsychoEncounter.repairRepeatedContract(player, active);
            if (repaired != active) {
                removeActive(board, active.id());
                active = repaired;
            }
            active.initializeUrgentDeadline(NpcIncomingCalls.playTicks(player));
            if (active.urgentTimeIsUp(NpcIncomingCalls.playTicks(player))) {
                expireUrgentContract(player, board, active);
                expired = true;
            } else {
                refreshContractProgress(player, active);
                writeActive(board, active);
                actives.add(active);
            }
        }
        if (!actives.isEmpty() || expired) {
            storeIssuerBoard(player, issuer, board);
            if (expired) QuestNavigation.sync(player);
        }
        return new BoardSnapshot(List.copyOf(actives), withholdsOffers(player, issuer) ? List.of() : List.copyOf(offers));
    }

    /** A world encounter's leader gives nothing more to someone who struck their people or broke their site. */
    public static boolean withholdsOffers(ServerPlayer player, AbstractCityNpcEntity issuer) {
        return issuer instanceof de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity leader
                && de.artemis.cyberneticenhancements.common.world.WorldEncounterOffenses.refuses(player, leader);
    }

    public static Result acceptIssuerOffer(ServerPlayer player, AbstractCityNpcEntity issuer, int offerIndex) {
        return acceptIssuerOffer(player, issuer, offerIndex, false);
    }

    public static boolean canReceiveUrgentOffer(ServerPlayer player, AbstractCityNpcEntity issuer) {
        if (!issuer.npcType().supportsContracts()) return false;
        var board = snapshotForIssuer(player, issuer);
        return board.actives().size() < MAX_ACTIVE_PER_ISSUER && !board.offers().isEmpty();
    }

    public static Result acceptUrgentIssuerOffer(ServerPlayer player, AbstractCityNpcEntity issuer, String offerId) {
        var offers = snapshotForIssuer(player, issuer).offers();
        for (int index = 0; index < offers.size(); index++)
            if (offers.get(index).id().equals(offerId)) return acceptIssuerOffer(player, issuer, index, true);
        return Result.INVALID;
    }

    public static boolean declineUrgentIssuerOffer(ServerPlayer player, AbstractCityNpcEntity issuer, String offerId) {
        ensureIssuerBoard(player, issuer);
        var board = issuerBoardTag(player, issuer, true);
        if (activeFull(board)) return false;
        var offers = readContracts(board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND));
        if (!offers.removeIf(offer -> offer.id().equals(offerId))) return false;
        writeOffers(board, offers);
        storeIssuerBoard(player, issuer, board);
        issuer.lowerTrust(player, 1);
        return true;
    }

    private static Result acceptIssuerOffer(ServerPlayer player, AbstractCityNpcEntity issuer, int offerIndex, boolean urgent) {
        if (!issuer.canInteract(player) || !issuer.isAlive() || de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData.get(player.serverLevel()).isDeceased(issuer.getUUID())) return Result.INVALID;
        if (withholdsOffers(player, issuer)) return Result.INVALID;
        ensureIssuerBoard(player, issuer);
        CompoundTag board = issuerBoardTag(player, issuer, true);
        return acceptPreparedOffer(player, issuer, board, offerIndex, urgent);
    }

    /** Prepares only the requested developer contract; INVALID can mean terrain is still loading. */
    public static Result prepareAndAcceptDevQuest(ServerPlayer player,
            de.artemis.cyberneticenhancements.common.entity.DevTestEntity issuer, QuestType type, boolean survey) {
        if (!issuer.canInteract(player) || !issuer.isAlive() || player.level() != issuer.level() || survey && type != QuestType.INVESTIGATION) return Result.INVALID;
        CompoundTag board = issuerBoardTag(player, issuer, true);
        if (activeFull(board)) return Result.HAS_ACTIVE;
        long day = currentDay(player.level());
        int index = survey ? QuestType.values().length : type.ordinal();
        RandomSource random = RandomSource.create(seedFor(player, issuer, day) ^ (index + 1L) * 341873128712L);
        int tier = 1 + random.nextInt(3);
        QuestContract offer = generateOffer(player, issuer, type, tier, index, day, random);
        if (offer == null || offer.targetPos() == null && type != QuestType.RECOVERY) return Result.INVALID;
        board.putLong(BOARD_REFRESH_DAY_TAG, day);
        board.putBoolean("OffersPreparing", false);
        writeOffers(board, List.of(offer));
        return acceptPreparedOffer(player, issuer, board, 0, false);
    }

    public enum DevQuestResult { ACCEPTED, PREPARING, UNAVAILABLE }

    public static DevQuestResult prepareDevQuest(ServerPlayer player, de.artemis.cyberneticenhancements.common.entity.DevTestEntity issuer,
                                                  QuestDimension route, int slot, UUID requestId) {
        if (!issuer.canInteract(player) || !issuer.isAlive() || player.level() != issuer.level()
                || slot < 0 || slot >= DEV_OFFER_COUNT || route != QuestDimension.STANDARD && slot == QuestType.values().length)
            return DevQuestResult.UNAVAILABLE;
        CompoundTag board = issuerBoardTag(player, issuer, true);
        if (activeFull(board)) return DevQuestResult.UNAVAILABLE;
        long day = currentDay(player.server.overworld());
        QuestType type = slot == QuestType.values().length ? QuestType.INVESTIGATION : QuestType.values()[slot];
        var random = RandomSource.create(requestId.getMostSignificantBits() ^ requestId.getLeastSignificantBits());
        int tier = 1 + random.nextInt(3);
        generatingTerrain = false;
        QuestContract offer = generateOffer(player, issuer, type, tier, slot, day, random, route, requestId.toString());
        if (offer == null || offer.targetPos() == null && type != QuestType.RECOVERY)
            return generatingTerrain ? DevQuestResult.PREPARING : DevQuestResult.UNAVAILABLE;
        var offers = readContracts(board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND));
        int selected = offers.size();
        offers.add(offer);
        writeOffers(board, offers);
        if (acceptPreparedOffer(player, issuer, board, selected, false) == Result.OK) return DevQuestResult.ACCEPTED;
        offers.removeLast();
        writeOffers(board, offers);
        storeIssuerBoard(player, issuer, board);
        return DevQuestResult.UNAVAILABLE;
    }

    private static Result acceptPreparedOffer(ServerPlayer player, AbstractCityNpcEntity issuer, CompoundTag board, int offerIndex, boolean urgent) {
        if (activeFull(board)) {
            return Result.HAS_ACTIVE;
        }
        List<QuestContract> offers = readContracts(board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND));
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return Result.INVALID;
        }
        QuestContract offer = offers.remove(offerIndex);
        if (QuestRecovery.immediateIssue(player, offer) != null) return Result.INVALID;
        if (offer.type() == QuestType.CYBERPSYCHO && offer.targetPos() == null) return Result.INVALID;
        if ((offer.type() == QuestType.DELIVERY || offer.type() == QuestType.EXTRACTION) && offer.targetPos() != null) {
            ServerLevel targetLevel = resolveContractLevel(player, offer);
            if (targetLevel != null && QuestDimension.of(offer.dimensionId()) == QuestDimension.STANDARD) {
                BlockPos ground = courierGroundPosition(targetLevel, offer.targetPos().getX(), offer.targetPos().getZ());
                if (isUsableCourierPosition(targetLevel, ground)) {
                    offer.setTargetLocation(offer.dimensionId(), ground, offer.areaRadius());
                }
            }
        }
        ServerLevel siteLevel = resolveContractLevel(player, offer);
        if (!offer.chromiumSurvey() && offer.targetPos() != null && siteLevel != null) {
            if (QuestDimension.of(offer.dimensionId()) != QuestDimension.STANDARD && siteLevel.hasChunkAt(offer.targetPos())
                    && !DimensionalQuestSites.usable(siteLevel, offer.targetPos())) {
                writeOffers(board, offers);
                board.putBoolean("OffersPreparing", true);
                storeIssuerBoard(player, issuer, board);
                return Result.INVALID;
            }
            var sites = QuestSiteReservations.get(siteLevel);
            if (!sites.available(offer.targetPos(), Math.max(32, offer.areaRadius() + 16), offer.id(), siteLevel.getGameTime())) return Result.INVALID;
            sites.reserve(offer, true, siteLevel.getGameTime());
        }
        if (urgent || offer.type() == QuestType.LOCATE) offer.markUrgent(); // The patient will not last: the clock always runs.
        offer.setStatus(QuestStatus.ACTIVE);
        long playTicks = NpcIncomingCalls.playTicks(player);
        offer.setAcceptedPlayTicks(playTicks);
        offer.initializeUrgentDeadline(playTicks);
        offer.setProgress(0);
        offer.setDestinationReached(false);
        switch (offer.type()) {
            case DELIVERY -> {
                issueCourierPackage(player, offer);
                ensureCourierStash(resolveContractLevel(player, offer), offer);
            }
            case EXTRACTION -> {
                ensureExtractionDrop(resolveContractLevel(player, offer), offer);
            }
            case INVESTIGATION -> {
                initializeInvestigation(player, offer);
            }
            case BREACH -> {
                ensureBreachSite(resolveContractLevel(player, offer), offer, player.getUUID());
            }
            case SALVAGE -> {
                var cargo = createSalvageCargo(offer);
                QuestItemVersions.get(player.serverLevel()).issue(cargo);
                boolean dev = issuer instanceof de.artemis.cyberneticenhancements.common.entity.DevTestEntity;
                boolean stashed = !dev && offer.targetPos() != null && SalvageQuests.stash(resolveContractLevel(player, offer), offer.targetPos(), cargo);
                if (dev) giveQuestItem(player, cargo);
                else if (!stashed) {
                    QuestItemVersions.get(player.serverLevel()).retire(offer.id());
                    return Result.INVALID;
                }
            }
            case LOCATE -> {
                giveQuestItem(player, createReviveInjector(offer));
                giveQuestItem(player, createSignalBeacon(offer));
                AbstractCityNpcEntity.openTemporaryContact(player, issuer, offer.issuerName());
            }
            default -> {
            }
        }
        writeOffers(board, offers);
        writeActive(board, offer);
        storeIssuerBoard(player, issuer, board);
        QuestNavigation.sync(player);
        notifyQuestAction(player, Component.translatable("screen.cyberneticenhancements.call.quest.accepted_chat", offer.title(), offer.issuerName()), PlayerActionFeedback.ACCEPTED);
        return Result.OK;
    }

    public static Result abandonIssuerActive(ServerPlayer player, AbstractCityNpcEntity issuer) {
        return abandonIssuerActive(player, issuer, "");
    }

    /** Drops one of the issuer's open contracts; an empty id means the first. */
    public static Result abandonIssuerActive(ServerPlayer player, AbstractCityNpcEntity issuer, String questId) {
        CompoundTag board = issuerBoardTag(player, issuer, false);
        QuestContract active = board == null ? null : boardActive(board, questId);
        if (active == null) {
            return Result.INVALID;
        }
        String issue = QuestRecovery.immediateIssue(player, active);
        if (issue != null) {
            cancelUnfinishableContract(player, board, active, issue);
            storeIssuerBoard(player, issuer, board);
            return Result.OK;
        }
        cleanupContractArtifacts(player, active);
        active.setStatus(QuestStatus.ABANDONED);
        active.setResolvedDay(currentDay(player.level()));
        removeActive(board, active.id());
        appendHistory(player, board, active);
        storeIssuerBoard(player, issuer, board);
        applyAbandonPenalty(player, active);
        return Result.OK;
    }

    public static boolean setAcceptedPlayTicks(ServerPlayer player, AbstractCityNpcEntity issuer, long ticks) {
        return setAcceptedPlayTicks(player, issuer.getUUID(), ticks);
    }

    public static boolean setAcceptedPlayTicks(ServerPlayer player, UUID issuerId, long ticks) {
        CompoundTag root = rootTag(player, false);
        if (root == null) return false;
        CompoundTag boards = issuerBoards(root);
        CompoundTag board = boards.getCompound(issuerId.toString());
        var actives = boardActives(board);
        if (actives.isEmpty()) return false;
        for (QuestContract active : actives) {
            active.setAcceptedPlayTicks(ticks);
            writeActive(board, active);
        }
        boards.put(issuerId.toString(), board);
        root.put(ISSUER_BOARDS_TAG, boards);
        player.getPersistentData().put(ROOT_TAG, root);
        return true;
    }

    public static List<QuestContract> activeContracts(ServerPlayer player) {
        CompoundTag root = rootTag(player, false);
        if (root == null) return List.of();
        List<QuestContract> result = new ArrayList<>();
        CompoundTag boards = issuerBoards(root);
        for (String key : boards.getAllKeys()) {
            CompoundTag board = boards.getCompound(key);
            result.addAll(boardActives(board));
        }
        return List.copyOf(result);
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
        for (var ref : activeSlots(boards)) {
            String issuerKey = ref.key();
            CompoundTag board = boards.getCompound(issuerKey);
            QuestContract active = QuestContract.fromTag(board.getCompound(ref.slot()));
            if (!questId.equals(active.id())) {
                continue;
            }
            String issue = QuestRecovery.immediateIssue(player, active);
            if (issue != null) {
                cancelUnfinishableContract(player, board, active, issue);
                boards.put(issuerKey, board);
                root.put(ISSUER_BOARDS_TAG, boards);
                player.getPersistentData().put(ROOT_TAG, root);
                QuestNavigation.sync(player);
                return Result.OK;
            }
            cleanupContractArtifacts(player, active);
            active.setStatus(QuestStatus.ABANDONED);
            active.setResolvedDay(currentDay(player.level()));
            removeActive(board, active.id());
            appendHistory(player, board, active);
            boards.put(issuerKey, board);
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
            applyAbandonPenalty(player, active);
            QuestNavigation.sync(player);
            return Result.OK;
        }
        return Result.INVALID;
    }

    public static int clearAllQuestData(ServerPlayer player) {
        QuestFailureCalls.clear(player);
        QuestRecovery.retain(player, Set.of());
        ITEM_RECOVERY.remove(player);
        CompoundTag root = rootTag(player, false);
        if (root == null || root.isEmpty()) {
            return 0;
        }
        int removedEntries = countQuestEntries(root);
        for (var contract : activeContracts(player)) cleanupContractArtifacts(player, contract);
        player.getPersistentData().remove(ROOT_TAG);
        QuestNavigation.sync(player);
        return removedEntries;
    }

    public static int removeQuestEntryById(ServerPlayer player, String questId) {
        if (questId == null || questId.isBlank()) {
            return 0;
        }
        QuestFailureCalls.acknowledge(player, questId);
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

            for (QuestContract active : boardActives(board)) {
                if (questId.equals(active.id())) {
                    cleanupContractArtifacts(player, active);
                    removeActive(board, active.id());
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
            boolean hasActive = hasActive(board);
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
        return turnInIssuerActive(player, issuer, false, "");
    }

    /** Hands in one specific open contract of this issuer. */
    public static TurnInResult turnInIssuerActive(ServerPlayer player, AbstractCityNpcEntity issuer, String questId) {
        return turnInIssuerActive(player, issuer, false, questId);
    }

    /** True means this interaction attempted a matching item handover, including an incomplete quantity. */
    public static boolean tryHandInHeldItem(ServerPlayer player, AbstractCityNpcEntity issuer, ItemStack held) {
        var contract = handInContract(player, issuer, held);
        if (contract == null) return false;
        var result = turnInIssuerActive(player, issuer, false, contract.id());
        if (result != TurnInResult.OK) {
            Component feedback = contract.type() == QuestType.RECOVERY
                    && (result == TurnInResult.NOT_READY || result == TurnInResult.MISSING_ITEMS)
                    ? Component.translatable("message.cyberneticenhancements.quest.handover_missing", contract.targetCount(),
                            new ItemStack(contract.recoveryItem()).getHoverName())
                    : Component.translatable("message.cyberneticenhancements.quest.handover_not_ready");
            player.displayClientMessage(feedback, true);
        }
        return true;
    }

    /** Whether what is held is something this issuer's open contract asks for, so that handing it over means something. */
    public static boolean canHandIn(ServerPlayer player, AbstractCityNpcEntity issuer, ItemStack held) {
        return handInContract(player, issuer, held) != null;
    }

    private static QuestContract handInContract(ServerPlayer player, AbstractCityNpcEntity issuer, ItemStack held) {
        if (!issuer.canInteract(player) || held.isEmpty()) return null;
        var board = issuerBoardTag(player, issuer, false);
        if (board == null) return null;
        for (var contract : boardActives(board)) if (handInMatches(player, contract, held)) return contract;
        return null;
    }

    private static boolean handInMatches(ServerPlayer player, QuestContract contract, ItemStack held) {
        return switch (contract.type()) {
            case RECOVERY -> held.is(contract.recoveryItem());
            case EXTRACTION -> matchesQuestItem(held, contract, QUEST_ITEM_TYPE_EXTRACTION)
                    && QuestItemVersions.get(player.serverLevel()).current(held);
            case CYBERPSYCHO -> matchesQuestItem(held, contract, QUEST_ITEM_TYPE_PSYCHO)
                    && QuestItemVersions.get(player.serverLevel()).current(held);
            case LOCATE -> matchesQuestItem(held, contract, QUEST_ITEM_TYPE_BEACON)
                    && QuestItemVersions.get(player.serverLevel()).current(held);
            case SALVAGE -> matchesQuestItem(held, contract, QUEST_ITEM_TYPE_SALVAGE)
                    && QuestItemVersions.get(player.serverLevel()).current(held);
            default -> false;
        };
    }

    public static TurnInResult turnInRemotely(ServerPlayer player, String questId) {
        var contract = activeContracts(player).stream().filter(quest -> quest.id().equals(questId)).findFirst().orElse(null);
        if (contract == null) return TurnInResult.NOT_READY;
        if (contract.type().requiresInPersonTurnIn()) return TurnInResult.IN_PERSON_REQUIRED;
        var issuer = AbstractCityNpcEntity.resolveCallContact(player, contract.issuerId());
        if (issuer == null || issuer.isRefusing(player)) return TurnInResult.UNAVAILABLE;
        return turnInIssuerActive(player, issuer, true, questId);
    }

    private static TurnInResult turnInIssuerActive(ServerPlayer player, AbstractCityNpcEntity issuer, boolean remote, String expectedId) {
        if (!issuer.canInteract(player) || !issuer.isAlive() || de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData.get(player.serverLevel()).isDeceased(issuer.getUUID())) return TurnInResult.UNAVAILABLE;
        CompoundTag board = issuerBoardTag(player, issuer, false);
        QuestContract active = board == null ? null : boardActive(board, expectedId);
        if (active == null) {
            return TurnInResult.NOT_READY;
        }
        if (active.type().requiresInPersonTurnIn()) {
            if (remote) return TurnInResult.IN_PERSON_REQUIRED;
            if (player.level() != issuer.level() || issuer.distanceToSqr(player) > 64) return TurnInResult.UNAVAILABLE;
        }
        if (active.urgentTimeIsUp(NpcIncomingCalls.playTicks(player))) {
            expireUrgentContract(player, board, active);
            storeIssuerBoard(player, issuer, board);
            QuestNavigation.sync(player);
            return TurnInResult.NOT_READY;
        }
        refreshContractProgress(player, active);
        if (active.type() == QuestType.DELIVERY && active.destinationReached()) {
            active.setDestinationReached(true);
            active.setStatus(QuestStatus.READY);
            active.completeUrgentObjective();
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
        if (active.type() == QuestType.CYBERPSYCHO && !consumeTaggedQuestItem(player, active, QUEST_ITEM_TYPE_PSYCHO))
            return TurnInResult.MISSING_ITEMS;
        if (active.type() == QuestType.SALVAGE && !consumeTaggedQuestItem(player, active, QUEST_ITEM_TYPE_SALVAGE))
            return TurnInResult.MISSING_ITEMS;
        if (active.type() == QuestType.LOCATE && !consumeTaggedQuestItem(player, active, QUEST_ITEM_TYPE_BEACON))
            return TurnInResult.MISSING_ITEMS;
        if (active.type() == QuestType.LOCATE && !active.honestReport()) {
            rejectPatientReport(player, issuer, board, active);
            return TurnInResult.REJECTED;
        }
        cleanupContractArtifacts(player, active);
        if (active.rewardItemKind() == QuestContract.REWARD_COVERAGE) {
            player.getPersistentData().putBoolean(de.artemis.cyberneticenhancements.common.world.TraumaCoverage.COVERAGE, true);
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player,
                    Component.translatable("message.cyberneticenhancements.quest.locate.coverage").withStyle(net.minecraft.ChatFormatting.GREEN));
        }

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
        var bonus = active.replacementPenalty() && player.getRandom().nextBoolean() ? ItemStack.EMPTY
                : de.artemis.cyberneticenhancements.common.fabrication.FabricationDiscoveries.rollReward(player, active.tier(), active.type());
        var rewards = List.of(rewardItem, bonus);
        if (remote) QuestRewardDeliveries.get(player.server).dispatch(player, active.id(), rewards);
        else {
            for (var reward : rewards) {
                if (reward.isEmpty()) continue;
                var rewardCopy = reward.copy();
                if (!player.addItem(rewardCopy)) player.drop(rewardCopy, false);
            }
        }
        active.setStatus(QuestStatus.COMPLETED);
        de.artemis.cyberneticenhancements.common.streetcred.StreetCred.gain(player,
                de.artemis.cyberneticenhancements.common.streetcred.StreetCredRewards.contract(active.type(), active.tier()));
        active.setResolvedDay(currentDay(player.level()));
        removeActive(board, active.id());
        appendHistory(player, board, active);
        storeIssuerBoard(player, issuer, board);
        if (active.chromiumSurvey())
            de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.rewardSurvey(player,
                    active.investigation() == null ? active.targetPos() : active.investigation().source());
        player.giveExperiencePoints(active.rewardExperience());
        notifyQuestAction(player, Component.translatable(remote ? "screen.cyberneticenhancements.call.quest.paid_chat"
                : "screen.cyberneticenhancements.call.quest.paid_direct_chat", active.title(), active.rewardSummary()), PlayerActionFeedback.COMPLETED);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return TurnInResult.OK;
    }

    public static List<QuestLogEntry> questLog(ServerPlayer player) {
        List<QuestLogEntry> entries = new ArrayList<>();
        var pending = QuestRewardDeliveries.get(player.server).pendingContractIds(player.getUUID());
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
            for (QuestContract contract : boardActives(board)) {
                refreshContractProgress(player, contract);
                entries.add(toLogEntry(player, contract, pending.contains(contract.id())));
                writeActive(board, contract);
                boards.put(issuerKey, board);
            }
            for (QuestContract contract : readContracts(board.getList(BOARD_HISTORY_TAG, Tag.TAG_COMPOUND))) {
                entries.add(toLogEntry(player, contract, pending.contains(contract.id())));
            }
        }
        player.getPersistentData().put(ROOT_TAG, root);
        entries.sort(Comparator.comparing(QuestLogEntry::sortGroup).thenComparing(QuestLogEntry::title, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
    }

    private static QuestLogEntry toLogEntry(ServerPlayer player, QuestContract contract, boolean pendingDelivery) {
        refreshContractProgress(player, contract);
        pendingDelivery &= contract.status() == QuestStatus.COMPLETED;
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
                contract.hudTitle(NpcIncomingCalls.playTicks(player)).getString(),
                contract.timedObjective(contract.objectiveLine(), NpcIncomingCalls.playTicks(player)).getString(),
                contract.rewardSummary().getString(),
                pendingDelivery ? "pending_delivery" : contract.status().id(),
                pendingDelivery ? Component.translatableWithFallback("quest.cyberneticenhancements.status.pending_delivery", "Pending delivery").getString() : contract.statusLabel().getString(),
                contract.tier(),
                hasLocation,
                location,
                x,
                y,
                z,
                pendingDelivery ? 2 : sortGroup(contract.status())
        );
    }

    private static int sortGroup(QuestStatus status) {
        return switch (status) {
            case READY -> 0;
            case ACTIVE -> 1;
            case OFFERED -> 2;
            case COMPLETED -> 3;
            case FAILED, ABANDONED, CANCELLED -> 4;
        };
    }

    private static void tickActiveContracts(ServerPlayer player) {
        CompoundTag root = rootTag(player, false);
        if (root == null) {
            ITEM_RECOVERY.remove(player);
            return;
        }
        CompoundTag boards = issuerBoards(root);
        if (boards.isEmpty()) {
            ITEM_RECOVERY.remove(player);
            return;
        }
        boolean dirty = false, historyChanged = false;
        long playTicks = NpcIncomingCalls.playTicks(player);
        Set<String> activeIds = new HashSet<>();
        for (var ref : activeSlots(boards)) {
            String issuerKey = ref.key();
            CompoundTag board = boards.getCompound(issuerKey);
            QuestContract contract = QuestContract.fromTag(board.getCompound(ref.slot()));
            QuestContract repaired = CyberpsychoEncounter.repairRepeatedContract(player, contract);
            if (repaired != contract) { removeActive(board, contract.id()); contract = repaired; dirty = true; }
            dirty |= contract.initializeUrgentDeadline(playTicks);
            if (contract.urgentTimeIsUp(playTicks) && QuestRecovery.immediateIssue(player, contract) == null) {
                expireUrgentContract(player, board, contract);
                dirty = true;
                historyChanged = true;
            } else if (questCacheAttemptLost(player, contract)) {
                finishFailedBreach(player, board, contract);
                dirty = true;
            } else if (salvageCargoLost(player, contract)) {
                cancelUnfinishableContract(player, board, contract, "cargo_lost");
                dirty = true;
                historyChanged = true;
            } else if (patientQuestItemLost(player, contract)) {
                failPatientQuest(player, board, contract);
                dirty = true;
                historyChanged = true;
            } else {
                String reason = QuestRecovery.immediateIssue(player, contract);
                if (reason == null) {
                    int previousProgress = contract.progress();
                    dirty |= refreshContractProgress(player, contract);
                    dirty |= updateQuestItemLoss(player, contract);
                    if (contract.progress() != previousProgress) de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "quests", "quest_progress", contract.title(), contract.objectiveLine());
                    reason = QuestRecovery.checkSite(player, contract);
                }
                if (reason != null) {
                    cancelUnfinishableContract(player, board, contract, reason);
                    dirty = true;
                    historyChanged = true;
                } else {
                    activeIds.add(contract.id());
                    writeActive(board, contract);
                }
            }
            boards.put(issuerKey, board);
        }
        var recovery = ITEM_RECOVERY.get(player);
        if (recovery != null) recovery.retainContracts(activeIds);
        QuestRecovery.retain(player, activeIds);
        if (dirty) {
            root.put(ISSUER_BOARDS_TAG, boards);
            player.getPersistentData().put(ROOT_TAG, root);
            QuestNavigation.sync(player);
            if (historyChanged) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.capture(player));
        }
    }

    private static boolean refreshContractProgress(ServerPlayer player, QuestContract contract) {
        if (contract.status() != QuestStatus.ACTIVE && contract.status() != QuestStatus.READY
                || contract.urgentTimeIsUp(NpcIncomingCalls.playTicks(player))) {
            return false;
        }
        return switch (contract.type()) {
            case DELIVERY -> updateDeliveryProgress(player, contract);
            case EXTRACTION -> updateExtractionProgress(player, contract);
            case INVESTIGATION -> updateInvestigationMarker(player, contract);
            case BREACH -> updateBreachProgress(player, contract);
            case RECOVERY -> updateRecoveryProgress(player, contract);
            case ELIMINATION -> updateEliminationRuntime(player, contract);
            case CYBERPSYCHO -> updateCyberpsychoProgress(player, contract);
            case LOCATE -> PatientSearch.update(player, contract);
            case SALVAGE -> updateSalvageProgress(player, contract);
        };
    }

    private static boolean updateSalvageProgress(ServerPlayer player, QuestContract contract) {
        if (!hasTaggedQuestItem(player, contract, QUEST_ITEM_TYPE_SALVAGE)) {
            if (contract.status() != QuestStatus.ACTIVE || player.tickCount % 40 != 3) return false;
            var level = resolveContractLevel(player, contract);
            var versions = QuestItemVersions.get(player.serverLevel());
            if (level == null || contract.targetPos() == null || !level.hasChunkAt(contract.targetPos()) || versions.issued(contract.id(), QUEST_ITEM_TYPE_SALVAGE)) return false;
            var cargo = createSalvageCargo(contract);
            versions.issue(cargo);
            SalvageQuests.stash(level, contract.targetPos(), cargo);
            return false;
        }
        if (contract.destinationReached() && contract.status() == QuestStatus.READY) return false;
        contract.setDestinationReached(true);
        contract.setProgress(contract.targetCount());
        markQuestReady(player, contract);
        return true;
    }

    /** A cargo that is gone once found is simply written off: it was lost before, and nobody is blamed. */
    private static boolean salvageCargoLost(ServerPlayer player, QuestContract contract) {
        if (contract.type() != QuestType.SALVAGE || contract.status() != QuestStatus.READY) return false;
        var recovery = ITEM_RECOVERY.computeIfAbsent(player, ignored -> new QuestItemRecovery());
        return recovery.missingLongEnough(contract.id(), QUEST_ITEM_TYPE_SALVAGE,
                hasTaggedQuestItem(player, contract, QUEST_ITEM_TYPE_SALVAGE), player.isCreative(), player.tickCount);
    }

    /** A cargo picked up from a lost container turns out to belong to one of the player's contacts. */
    private static void claimFoundCargo(ServerPlayer player) {
        var cargos = ModItems.LOST_CARGO.get();
        ItemStack found = ItemStack.EMPTY;
        var stacks = new ArrayList<ItemStack>(player.getInventory().items);
        stacks.addAll(player.getInventory().offhand);
        for (var stack : stacks)
            if (stack.is(cargos) && QuestItemVersions.key(stack).isEmpty()) { found = stack; break; }
        if (found.isEmpty()) return;
        var owners = new ArrayList<AbstractCityNpcEntity>();
        for (var contact : AbstractCityNpcEntity.reachablePermanentContacts(player)) {
            var board = issuerBoardTag(player, contact, false);
            if (board == null || !activeFull(board)) owners.add(contact);
        }
        if (owners.isEmpty()) return; // The cargo stays as it is until somebody can be told about it.
        var issuer = owners.get(player.getRandom().nextInt(owners.size()));
        int tier = 1 + player.getRandom().nextInt(3);
        long day = currentDay(player.level());
        int money = de.artemis.cyberneticenhancements.common.economy.EconomyBalance.tier("quest.salvage.base", tier)
                + player.getRandom().nextInt(de.artemis.cyberneticenhancements.common.economy.EconomyBalance.tier("quest.salvage.spread", tier) + 1);
        money = Math.max(1, Math.round(money * moneyProfileMultiplier(issuer.npcType())));
        money = de.artemis.cyberneticenhancements.common.streetcred.StreetCredEffects.questPay(player,
                de.artemis.cyberneticenhancements.common.origin.Origins.questPay(player, QuestType.SALVAGE, money));
        int trust = de.artemis.cyberneticenhancements.common.economy.EconomyBalance.tier("trust.quest.salvage", tier);
        int rewardKind = QuestContract.rewardItemKindForTier(tier, QuestType.SALVAGE);
        var contract = new QuestContract(UUID.randomUUID().toString(), issuer.getUUID(), issuer.getName().getString(), issuer.npcTypeId(),
                QuestType.SALVAGE, tier, QuestStatus.READY, 0, 1, 1, true, player.level().dimension().location().toString(),
                null, 1, money, trust, rewardKind, QuestContract.rewardItemCountFor(rewardKind), day, 0L);
        contract.setAcceptedPlayTicks(NpcIncomingCalls.playTicks(player));
        found.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_SALVAGE)));
        decorateSalvageCargo(found, contract);
        QuestItemVersions.get(player.serverLevel()).issue(found);
        var board = issuerBoardTag(player, issuer, true);
        writeActive(board, contract);
        storeIssuerBoard(player, issuer, board);
        player.inventoryMenu.broadcastChanges();
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "quests", "quest_ready", contract.title(), contract.issuerName());
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player,
                Component.translatable("message.cyberneticenhancements.quest.salvage_found", contract.issuerName()).withStyle(net.minecraft.ChatFormatting.AQUA),
                PlayerActionFeedback.READY);
    }

    private static ItemStack createSalvageCargo(QuestContract contract) {
        var stack = new ItemStack(ModItems.LOST_CARGO.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_SALVAGE)));
        decorateSalvageCargo(stack, contract);
        return stack;
    }

    private static void decorateSalvageCargo(ItemStack stack, QuestContract contract) {
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.cyberneticenhancements.lost_cargo.owned", contract.issuerName()));
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.translatable("tooltip.cyberneticenhancements.lost_cargo.return", contract.issuerName()),
                Component.translatable("tooltip.cyberneticenhancements.lost_cargo.lost").withStyle(net.minecraft.ChatFormatting.GRAY))));
    }

    private static boolean updateEliminationRuntime(ServerPlayer player, QuestContract contract) {
        boolean dirty = false;
        if (contract.status() == QuestStatus.ACTIVE) {
            dirty |= EliminationEncounter.trySpawn(player, contract);
        }
        if (contract.isProgressComplete() && contract.status() != QuestStatus.READY) {
            markQuestReady(player, contract);
            dirty = true;
        }
        return dirty;
    }

    static void markQuestReady(ServerPlayer player, QuestContract contract) {
        if (contract.status() == QuestStatus.READY || contract.urgentTimeIsUp(NpcIncomingCalls.playTicks(player))) return;
        contract.setStatus(QuestStatus.READY);
        contract.completeUrgentObjective();
        String key = contract.type() == QuestType.DELIVERY ? "delivery_received" : contract.type().id() + "_ready";
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "quests", "quest_ready", contract.title(), contract.issuerName());
        Component feedback = Component.translatable("message.cyberneticenhancements.quest." + key, contract.issuerName())
                .withStyle(net.minecraft.ChatFormatting.AQUA);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, feedback, PlayerActionFeedback.READY);
    }

    private static boolean updateQuestItemLoss(ServerPlayer player, QuestContract contract) {
        String type;
        boolean present;
        boolean dirty = false;
        var level = resolveContractLevel(player, contract);
        if (contract.type() == QuestType.DELIVERY) {
            if (contract.destinationReached()) return false;
            type = QUEST_ITEM_TYPE_PACKAGE;
            present = hasTaggedQuestItem(player, contract, type) || level != null && courierStashContainsPackage(level, contract);
        } else if (contract.type() == QuestType.EXTRACTION || contract.type() == QuestType.CYBERPSYCHO) {
            type = contract.type() == QuestType.EXTRACTION ? QUEST_ITEM_TYPE_EXTRACTION : QUEST_ITEM_TYPE_PSYCHO;
            present = hasTaggedQuestItem(player, contract, type);
            if (!present && !contract.returnItemCollected()) {
                if (contract.type() == QuestType.EXTRACTION) {
                    if (!contract.destinationReached() && !contract.isReady()) {
                        if (level == null || contract.targetPos() == null || !level.hasChunkAt(contract.targetPos())
                                || !QuestItemVersions.get(level).issued(contract.id(), type)) return false;
                        if (level.getBlockEntity(contract.targetPos()) instanceof Container container
                                && containerContainsQuestItem(level, container, contract, type)) return false;
                    }
                } else {
                    var death = CyberpsychoEncounter.get(player.serverLevel()).death(contract.id());
                    if (death == null || level == null || !level.hasChunkAt(death.pos()) || player.level() != level
                            || !player.blockPosition().closerThan(death.pos(), 64)) return false;
                    for (var itemLevel : player.server.getAllLevels())
                        if (itemLevel.getEntity(death.shard()) instanceof net.minecraft.world.entity.item.ItemEntity item && item.isAlive()) return false;
                }
                contract.markReturnItemCollected();
                dirty = true;
            }
        } else return false;
        var recovery = ITEM_RECOVERY.computeIfAbsent(player, ignored -> new QuestItemRecovery());
        if (present) {
            recovery.missingLongEnough(contract.id(), type, true, player.isCreative(), player.tickCount);
            if (contract.type() == QuestType.DELIVERY) {
                dirty |= contract.deliveryItemMissing();
                contract.setDeliveryItemMissing(false);
            } else {
                dirty |= !contract.returnItemCollected() || contract.itemLossPending() || contract.externalItemConfirmed();
                contract.foundReturnItem();
            }
            return dirty;
        }
        if (contract.itemLossPending() || contract.externalItemConfirmed() || contract.deliveryItemMissing()) return dirty;
        if (!recovery.missingLongEnough(contract.id(), type, false, player.isCreative(), player.tickCount)) return dirty;
        if (contract.type() == QuestType.DELIVERY) {
            contract.setDeliveryItemMissing(true);
            notifyQuestAction(player, Component.translatable("message.cyberneticenhancements.quest.replacement_available", contract.issuerName()), PlayerActionFeedback.CANCELLED);
        } else contract.markItemLossPending();
        return true;
    }

    private static void tickLostItemCalls(ServerPlayer player) {
        for (var contract : activeContracts(player)) {
            if (!contract.itemLossPending() || contract.itemLossCallDeferred()) continue;
            var issuer = AbstractCityNpcEntity.resolveCallContact(player, contract.issuerId());
            if (issuer != null && de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager.startIncoming(player, issuer,
                    de.artemis.cyberneticenhancements.common.dialogue.NpcCallReason.LOST_ITEM, true)) return;
        }
    }

    public static boolean canRequestDeliveryReplacement(ServerPlayer player, AbstractCityNpcEntity issuer) {
        if (!issuer.canInteract(player)) return false;
        return replaceableDelivery(player, issuerBoardTag(player, issuer, false)) != null;
    }

    private static @Nullable QuestContract replaceableDelivery(ServerPlayer player, @Nullable CompoundTag board) {
        if (board == null) return null;
        for (var contract : boardActives(board)) {
            var level = resolveContractLevel(player, contract);
            if (contract.type() == QuestType.DELIVERY && !contract.destinationReached() && !contract.isReady()
                    && !hasTaggedQuestItem(player, contract, QUEST_ITEM_TYPE_PACKAGE)
                    && (level == null || !courierStashContainsPackage(level, contract))) return contract;
        }
        return null;
    }

    public static boolean requestDeliveryReplacement(ServerPlayer player, AbstractCityNpcEntity issuer) {
        if (!canRequestDeliveryReplacement(player, issuer)) return false;
        var board = issuerBoardTag(player, issuer, false);
        var contract = replaceableDelivery(player, board);
        issueCourierPackage(player, contract);
        contract.markReplacementPenalty();
        ITEM_RECOVERY.computeIfAbsent(player, ignored -> new QuestItemRecovery()).missingLongEnough(
                contract.id(), QUEST_ITEM_TYPE_PACKAGE, true, player.isCreative(), player.tickCount);
        writeActive(board, contract);
        storeIssuerBoard(player, issuer, board);
        QuestNavigation.sync(player);
        return true;
    }

    public static boolean confirmLostReturnItem(ServerPlayer player, AbstractCityNpcEntity issuer, String questId) {
        if (!issuer.canInteract(player)) return false;
        var board = issuerBoardTag(player, issuer, false);
        var contract = board == null ? null : boardActive(board, questId);
        if (contract == null || !contract.id().equals(questId) || !contract.itemLossPending()) return false;
        contract.confirmExternalItem();
        writeActive(board, contract);
        storeIssuerBoard(player, issuer, board);
        QuestNavigation.sync(player);
        return true;
    }

    public static boolean failLostReturnItem(ServerPlayer player, AbstractCityNpcEntity issuer, String questId) {
        var board = issuerBoardTag(player, issuer, false);
        var contract = board == null ? null : boardActive(board, questId);
        if (contract == null || !contract.id().equals(questId) || !contract.itemLossPending()) return false;
        contract.setStatus(QuestStatus.FAILED);
        lockOutEncounter(player, contract);
        contract.setResolvedDay(currentDay(player.level()));
        cleanupContractArtifacts(player, contract);
        removeActive(board, contract.id());
        appendHistory(player, board, contract);
        storeIssuerBoard(player, issuer, board);
        int loss = de.artemis.cyberneticenhancements.common.skills.SkillEffects.trustLoss(player, Math.max(2, contract.rewardTrust() + 2));
        AbstractCityNpcEntity.lowerTrustFromArchive(player, contract.issuerId(), loss);
        notifyQuestAction(player, Component.translatable("message.cyberneticenhancements.quest.item_lost_failed",
                contract.title(), contract.issuerName(), loss).withStyle(net.minecraft.ChatFormatting.RED), PlayerActionFeedback.CANCELLED);
        QuestNavigation.sync(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload.capture(player));
        return true;
    }

    public static void deferLostReturnItemCall(ServerPlayer player, AbstractCityNpcEntity issuer, String questId) {
        var board = issuerBoardTag(player, issuer, false);
        var contract = board == null ? null : boardActive(board, questId);
        if (contract == null || !contract.id().equals(questId) || !contract.itemLossPending()) return;
        contract.deferItemLossCall();
        writeActive(board, contract);
        storeIssuerBoard(player, issuer, board);
    }

    private static boolean updateCyberpsychoProgress(ServerPlayer player, QuestContract contract) {
        boolean dirty = CyberpsychoEncounter.trySpawn(player, contract);
        var death = CyberpsychoEncounter.get(player.serverLevel()).death(contract.id());
        if (death == null) return dirty;
        if (!contract.destinationReached()) {
            contract.setDestinationReached(true);
            contract.setTargetLocation(death.dimension(), death.pos(), 1);
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.quest.cyberpsycho.killed", contract.psychoName())
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            dirty = true;
        }
        boolean hasShard = hasTaggedQuestItem(player, contract, QUEST_ITEM_TYPE_PSYCHO);
        if (hasShard) {
            if (!contract.psychoShardCollected()) { contract.markPsychoShardCollected(); dirty = true; }
            contract.setProgress(1);
            if (!contract.isReady()) { markQuestReady(player, contract); dirty = true; }
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
            dirty |= ensureCourierStash(level, contract);
        }
        if (!contract.destinationReached() && consumeCourierPackageFromStash(level, contract)) {
            contract.setDestinationReached(true);
            contract.setProgress(contract.targetCount());
            markQuestReady(player, contract);
            cleanupCourierPackage(player, contract);
            if (player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu chestMenu
                    && chestMenu.getContainer() == level.getBlockEntity(contract.targetPos())) player.closeContainer();
            cleanupCourierStash(level, contract);
            return true;
        }
        return dirty;
    }

    private static boolean updateExtractionProgress(ServerPlayer player, QuestContract contract) {
        boolean dirty = false;
        boolean hasExtractionItem = hasTaggedQuestItem(player, contract, QUEST_ITEM_TYPE_EXTRACTION);
        if (hasExtractionItem) {
            dirty = !contract.returnItemCollected();
            contract.markReturnItemCollected();
            if (!contract.destinationReached() || contract.status() != QuestStatus.READY) {
                contract.setDestinationReached(true);
                contract.setProgress(contract.targetCount());
                markQuestReady(player, contract);
                cleanupExtractionMarker(resolveContractLevel(player, contract), contract);
                return true;
            }
            return dirty;
        }
        if (contract.destinationReached() || contract.isReady() || contract.returnItemCollected()) return dirty;
        return dirty | ensureExtractionDrop(resolveContractLevel(player, contract), contract);
    }

    private static boolean updateInvestigationMarker(ServerPlayer player, QuestContract contract) {
        if (contract.status() != QuestStatus.ACTIVE) {
            if (contract.status() == QuestStatus.READY && contract.investigation() != null && contract.investigation().located())
                cleanupBreachMarker(resolveContractLevel(player, contract), contract);
            return false;
        }
        boolean dirty = initializeInvestigation(player, contract);
        var survey = contract.investigation();
        if (survey == null) return dirty;
        if (survey.located() && contract.chromiumSurvey()) {
            contract.setTargetLocation(contract.dimensionId(), survey.triangulatedPosition(), 1);
            contract.setDestinationReached(true);
            markQuestReady(player, contract);
            return true;
        }
        if (survey.located()) return ensureBreachSite(resolveContractLevel(player, contract), contract, player.getUUID()) | dirty;
        if (!player.serverLevel().dimension().location().toString().equals(contract.dimensionId())
                || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()
                || player.isSpectator() || player.isPassenger()) return dirty;
        var closest = activeContracts(player).stream().filter(quest -> quest.type() == QuestType.INVESTIGATION
                && quest.status() == QuestStatus.ACTIVE && quest.targetPos() != null
                && quest.dimensionId().equals(contract.dimensionId())
                && (quest.investigation() == null || !quest.investigation().located()))
                .min(Comparator.comparingDouble(quest -> quest.targetPos().distSqr(player.blockPosition())));
        if (closest.isEmpty() || !closest.get().id().equals(contract.id())) return dirty;
        String rejection = survey.rejection(player.blockPosition(), contract.tier());
        if (!rejection.isEmpty()) {
            SURVEY_SCANS.remove(player);
            if (player.tickCount % 20 == 0 && player.blockPosition().distSqr(survey.origin()) <= 200 * 200)
                player.displayClientMessage(Component.translatable("message.cyberneticenhancements.quest.survey." + rejection), true);
            return dirty;
        }
        var scan = SURVEY_SCANS.get(player);
        if (scan == null || !scan.questId().equals(contract.id()) || scan.lastTick() != player.tickCount - 1
                || scan.origin().distanceToSqr(player.position()) > 0.16) {
            scan = new SurveyScan(contract.id(), player.position(), player.tickCount, player.tickCount);
        } else scan = new SurveyScan(scan.questId(), scan.origin(), scan.started(), player.tickCount);
        SURVEY_SCANS.put(player, scan);
        int elapsed = player.tickCount - scan.started();
        if (player.tickCount % 10 == 0)
            player.displayClientMessage(Component.translatable("message.cyberneticenhancements.quest.survey.scanning",
                    Math.min(100, elapsed * 100 / InvestigationSurvey.SCAN_TICKS)), true);
        if (elapsed < InvestigationSurvey.SCAN_TICKS) return dirty;
        SURVEY_SCANS.remove(player);
        if (!survey.record(player.blockPosition(), contract.tier())) return dirty;
        contract.setProgress(survey.count());
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.quest.survey.reading", survey.count(),
                player.blockPosition().getX(), player.blockPosition().getZ(), Math.round(survey.bearing(player.blockPosition())) % 360));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.2F);
        if (survey.located()) {
            BlockPos source = survey.triangulatedPosition();
            contract.setTargetLocation(contract.dimensionId(), source, 1);
            if (contract.chromiumSurvey()) {
                contract.setDestinationReached(true);
                markQuestReady(player, contract);
            } else ensureBreachSite(resolveContractLevel(player, contract), contract, player.getUUID());
            de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.quest.survey.located", source.getX(), source.getY(), source.getZ()));
        }
        return true;
    }

    private static boolean initializeInvestigation(ServerPlayer player, QuestContract contract) {
        if (contract.investigation() != null || contract.targetPos() == null) return false;
        var level = resolveContractLevel(player, contract);
        if (level == null || !level.hasChunkAt(contract.targetPos())) return false;
        cleanupInvestigationMarker(level, contract);
        boolean dimensional = QuestDimension.of(contract.dimensionId()) != QuestDimension.STANDARD;
        BlockPos origin = contract.targetPos();
        BlockPos source = null;
        if (dimensional) {
            if (!DimensionalQuestSites.usable(level, origin)) return false;
            source = origin;
            origin = DimensionalQuestSites.surveyApproach(level, source);
            contract.setTargetLocation(contract.dimensionId(), origin, contract.areaRadius());
        }
        var random = RandomSource.create(contract.id().hashCode() * 341873128712L);
        for (int attempt = 0; source == null && attempt < 64; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int distance = 48 + random.nextInt(92);
            int x = contract.targetPos().getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = contract.targetPos().getZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) continue;
            if (!level.hasChunkAt(new BlockPos(x, contract.targetPos().getY(), z))) continue;
            BlockPos candidate = surfaceGroundPosition(level, x, z);
            if (isUsableStashPosition(level, candidate)
                    && QuestSiteReservations.get(level).available(candidate, 32, contract.id(), level.getGameTime())) {
                source = candidate;
                break;
            }
        }
        if (source == null) {
            if (!isUsableStashPosition(level, contract.targetPos())) return false;
            source = contract.targetPos();
        }
        contract.setInvestigation(new InvestigationSurvey(origin, source));
        contract.setProgress(0);
        contract.setDestinationReached(false);
        QuestSiteReservations.get(level).reserveAdditional(contract.id(), "signal_source", source, 32);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, Component.translatable("message.cyberneticenhancements.quest.survey.start"));
        return true;
    }

    private static boolean updateBreachProgress(ServerPlayer player, QuestContract contract) {
        boolean dirty = false;
        if (contract.status() == QuestStatus.READY) {
            cleanupBreachMarker(resolveContractLevel(player, contract), contract);
            return false;
        }
        if (contract.isProgressComplete() && contract.status() != QuestStatus.READY) {
            markQuestReady(player, contract);
            return true;
        }
        if (contract.isProgressComplete()) {
            return dirty;
        }
        return dirty | ensureBreachSite(resolveContractLevel(player, contract), contract, player.getUUID());
    }

    private static boolean updateRecoveryProgress(ServerPlayer player, QuestContract contract) {
        int oldProgress = contract.progress();
        QuestStatus oldStatus = contract.status();
        boolean dimensional = QuestDimension.of(contract.dimensionId()) != QuestDimension.STANDARD;
        boolean inCollectionDimension = player.level().dimension().location().toString().equals(contract.dimensionId());
        boolean wasCollected = contract.destinationReached();
        int inventoryCount = countInventoryItems(player, contract.recoveryItem());
        boolean collectionVerified = !dimensional || inCollectionDimension || wasCollected;
        if (dimensional && inCollectionDimension && inventoryCount >= contract.targetCount()) contract.setDestinationReached(true);
        if (!collectionVerified) inventoryCount = 0;
        contract.setProgress(QuestContract.normalizeProgressForRecovery(contract, inventoryCount));
        if (contract.progress() >= contract.targetCount() && contract.status() != QuestStatus.READY) {
            markQuestReady(player, contract);
        } else if (contract.progress() < contract.targetCount() && contract.status() == QuestStatus.READY) {
            contract.setStatus(QuestStatus.ACTIVE);
        }
        return wasCollected != contract.destinationReached() || contract.progress() != oldProgress || contract.status() != oldStatus;
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
        for (var ref : activeSlots(boards)) {
            String issuerKey = ref.key();
            CompoundTag board = boards.getCompound(issuerKey);
            QuestContract contract = QuestContract.fromTag(board.getCompound(ref.slot()));
            if (contract.type() != QuestType.ELIMINATION || contract.status() != QuestStatus.ACTIVE) {
                continue;
            }
            boolean inHuntZone = contract.targetPos() == null
                    || (killedDimensionId.equals(contract.dimensionId())
                    && killedPos.closerThan(contract.targetPos(), Math.max(1.0D, contract.areaRadius() + 0.5D)));
            if (contract.eliminationTarget() == killedType && inHuntZone) {
                contract.setProgress(contract.progress() + 1);
                de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(player, "quests", "quest_progress", contract.title(), contract.objectiveLine());
                if (contract.isProgressComplete()) {
                    markQuestReady(player, contract);
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
        sanitizeQuestItems(player);
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
        ItemStack carried = player.containerMenu.getCarried();
        if (matchesQuestItem(carried, contract, itemType)) {
            carried.shrink(1);
            if (carried.isEmpty()) player.containerMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
            return true;
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

    private static boolean generatingTerrain;

    public static boolean offersPreparing(ServerPlayer player, AbstractCityNpcEntity issuer) {
        var board = issuerBoardTag(player, issuer, false);
        return board != null && board.getBoolean("OffersPreparing");
    }

    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) QuestTerrainPreparation.unload(level);
    }

    private static void ensureIssuerBoard(ServerPlayer player, AbstractCityNpcEntity issuer) {
        if (!issuer.npcType().supportsContracts()) {
            return;
        }
        CompoundTag board = issuerBoardTag(player, issuer, true);
        long currentDay = currentDay(player.level());
        boolean current = board.getLong(BOARD_REFRESH_DAY_TAG) == currentDay && board.contains(BOARD_OFFERS_TAG, Tag.TAG_LIST);
        boolean dev = issuer instanceof de.artemis.cyberneticenhancements.common.entity.DevTestEntity;
        var existing = current ? readContracts(board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND)) : List.<QuestContract>of();
        if (current && !board.getBoolean("OffersPreparing") && (!dev || hasActive(board)
                || existing.size() == DEV_OFFER_COUNT)) return;
        long now = player.server.overworld().getGameTime();
        if (board.getLong("NextOfferPreparation") > now) return;
        generatingTerrain = false;
        List<QuestContract> offers = generateOffers(player, issuer, currentDay, existing);
        board.putLong(BOARD_REFRESH_DAY_TAG, currentDay);
        board.putBoolean("OffersPreparing", generatingTerrain);
        board.putLong("NextOfferPreparation", now + 20);
        writeOffers(board, offers);
        storeIssuerBoard(player, issuer, board);
    }

    private static List<QuestContract> generateOffers(ServerPlayer player, AbstractCityNpcEntity issuer, long day, List<QuestContract> existing) {
        RandomSource random = RandomSource.create(seedFor(player, issuer, day));
        int trust = issuer.trustLevel(player);
        int maxTier = Math.max(trust >= 3 ? 3 : trust >= 1 ? 2 : 1, de.artemis.cyberneticenhancements.common.streetcred.StreetCredEffects.contractTier(player));
        List<QuestContract> offers = new ArrayList<>(existing);
        Set<QuestType> used = new HashSet<>();
        boolean devTest = issuer instanceof de.artemis.cyberneticenhancements.common.entity.DevTestEntity;
        for (int index = 0; index < (devTest ? DEV_OFFER_COUNT : MAX_OFFERS); index++) {
            boolean extraSurvey = devTest && index == QuestType.values().length;
            QuestType type = extraSurvey ? QuestType.INVESTIGATION : devTest ? QuestType.values()[index] : questTypeForSlot(issuer.npcType(), index, random, trust, used);
            used.add(type);
            int tier = 1 + random.nextInt(maxTier);
            boolean geology = type == QuestType.INVESTIGATION && (issuer.npcType() == NpcType.TECHIE || extraSurvey);
            if (offers.stream().anyMatch(offer -> offer.type() == type && offer.chromiumSurvey() == geology)) continue;
            RandomSource offerRandom = RandomSource.create(seedFor(player, issuer, day) ^ (index + 1L) * 341873128712L);
            QuestContract offer = generateOffer(player, issuer, type, tier, index, day, offerRandom);
            if (offer != null && (offer.targetPos() != null || type == QuestType.RECOVERY)) {
                offers.add(offer);
                ServerLevel siteLevel = resolveContractLevel(player, offer);
                if (siteLevel != null) QuestSiteReservations.get(siteLevel).reserve(offer, false, siteLevel.getGameTime());
            }
        }
        return offers;
    }

    private static QuestType questTypeForSlot(NpcType npcType, int index, RandomSource random, int trust, Set<QuestType> used) {
        if (npcType == NpcType.TECHIE && index == 1) return QuestType.INVESTIGATION;
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

    public static QuestType primaryQuestType(NpcType npcType) {
        return switch (npcType) {
            case FIXER -> QuestType.DELIVERY;
            case RIPPERDOC, STREET_RIPPERDOC, MED_TECH -> QuestType.RECOVERY;
            case TECHIE -> QuestType.RECOVERY;
            case NETRUNNER -> QuestType.BREACH;
            case MERC, MAXTEC -> QuestType.ELIMINATION;
            case WORLD_MED_TECH -> QuestType.LOCATE;
        };
    }

    public static List<QuestType> weightedQuestPool(NpcType npcType, int trust) {
        List<QuestType> base = new ArrayList<>();
        switch (npcType) {
            case FIXER -> {
                addWeighted(base, QuestType.DELIVERY, 4);
                addWeighted(base, QuestType.INVESTIGATION, trust >= 1 ? 3 : 2);
                addWeighted(base, QuestType.EXTRACTION, 2);
                addWeighted(base, QuestType.RECOVERY, 2);
                addWeighted(base, QuestType.ELIMINATION, trust >= 2 ? 2 : 1);
                addWeighted(base, QuestType.SALVAGE, 2);
            }
            case RIPPERDOC, STREET_RIPPERDOC -> {
                addWeighted(base, QuestType.RECOVERY, 4);
                addWeighted(base, QuestType.DELIVERY, 3);
                addWeighted(base, QuestType.EXTRACTION, 2);
                addWeighted(base, QuestType.INVESTIGATION, trust >= 2 ? 2 : 1);
                addWeighted(base, QuestType.SALVAGE, 1);
            }
            case TECHIE -> {
                addWeighted(base, QuestType.SALVAGE, 2);
                addWeighted(base, QuestType.CYBERPSYCHO, 1);
                addWeighted(base, QuestType.RECOVERY, 4);
                addWeighted(base, QuestType.EXTRACTION, 3);
                addWeighted(base, QuestType.DELIVERY, 2);
                addWeighted(base, QuestType.INVESTIGATION, 2);
            }
            case NETRUNNER -> {
                addWeighted(base, QuestType.CYBERPSYCHO, 1);
                addWeighted(base, QuestType.BREACH, 4);
                addWeighted(base, QuestType.INVESTIGATION, 4);
                addWeighted(base, QuestType.EXTRACTION, 3);
                addWeighted(base, QuestType.RECOVERY, trust >= 1 ? 2 : 1);
            }
            case WORLD_MED_TECH -> addWeighted(base, QuestType.LOCATE, 1);
            case MERC -> {
                addWeighted(base, QuestType.SALVAGE, 1);
                addWeighted(base, QuestType.CYBERPSYCHO, 2);
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
        return generateOffer(player, issuer, type, tier, index, day, random, null, null);
    }

    private static QuestContract generateOffer(ServerPlayer player, AbstractCityNpcEntity issuer, QuestType type, int tier, int index,
                                                long day, RandomSource random, @Nullable QuestDimension explicitRoute, @Nullable String explicitId) {
        boolean dev = issuer instanceof de.artemis.cyberneticenhancements.common.entity.DevTestEntity;
        String contractId = explicitId != null ? explicitId : dev ? UUID.randomUUID().toString()
                : UUID.nameUUIDFromBytes((player.getUUID() + "|" + issuer.getUUID() + "|" + day + "|" + index + "|" + type.id()).getBytes(StandardCharsets.UTF_8)).toString();
        boolean geology = type == QuestType.INVESTIGATION && (issuer.npcType() == NpcType.TECHIE
                || issuer instanceof de.artemis.cyberneticenhancements.common.entity.DevTestEntity && index == QuestType.values().length);
        int targetIndex = switch (type) {
            case DELIVERY -> 0;
            case EXTRACTION -> 0;
            case INVESTIGATION -> geology ? 1 : 0;
            case BREACH -> 0;
            case ELIMINATION -> random.nextInt(Math.min(6, 2 + tier * 2));
            case RECOVERY -> random.nextInt(Math.min(5, 2 + tier));
            case CYBERPSYCHO -> random.nextInt(de.artemis.cyberneticenhancements.common.entity.CyberpsychoLoadout.values().length);
            case LOCATE, SALVAGE -> 0;
        };
        BlockPos targetPos = null;
        String dimensionId = "";
        int radius = switch (type) {
            case INVESTIGATION -> 2;
            case ELIMINATION -> 24;
            case CYBERPSYCHO -> 48;
            case LOCATE -> PatientSearch.AREA_RADIUS;
            case BREACH -> 1;
            default -> 1;
        };
        if (issuer.level() instanceof ServerLevel serverLevel) {
            DeliveryTarget target = null;
            QuestDimension route = explicitRoute != null ? explicitRoute
                    : !dev && (type == QuestType.DELIVERY || type == QuestType.EXTRACTION || type == QuestType.BREACH)
                        ? QuestDimension.byIndex(random.nextInt(10)) : QuestDimension.STANDARD;
            if (route != QuestDimension.STANDARD) {
                ServerLevel destination = player.server.getLevel(route.levelKey());
                if (destination != null && type == QuestType.RECOVERY) {
                    dimensionId = destination.dimension().location().toString();
                } else if (destination != null) {
                    var origin = issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition();
                    var result = DimensionalQuestSites.find(destination, serverLevel, origin, contractId, random.nextLong(), Math.max(48, radius + 16));
                    if (result.preparing()) {
                        generatingTerrain = true;
                        return null;
                    }
                    if (result.pos() != null) target = new DeliveryTarget(destination, result.pos());
                }
                if (target == null && dimensionId.isEmpty() && explicitRoute != null) return null;
            }
            ServerLevel preferred = explicitRoute == QuestDimension.STANDARD ? player.server.overworld() : serverLevel;
            if (target == null) target = switch (type) {
                case DELIVERY -> generateDeliveryTarget(preferred, issuer, player, tier, random);
                case EXTRACTION -> generateExtractionTarget(preferred, issuer, player, tier, random);
                case INVESTIGATION -> geology ? null : generateInvestigationTarget(preferred, issuer, player, tier, random);
                case BREACH -> generateBreachTarget(preferred, issuer, player, tier, random);
                case ELIMINATION -> generateHuntTarget(preferred, issuer, player, tier, random);
                case CYBERPSYCHO -> generateCyberpsychoTarget(preferred, issuer, player, tier, random);
                case LOCATE -> generateCyberpsychoTarget(preferred, issuer, player, tier, random);
                case SALVAGE -> {
                    var container = generateSalvageTarget(preferred, issuer);
                    // Test contacts have no ocean to search, so their cargo waits at an ordinary site and is handed over directly.
                    yield container != null || !dev ? container : generateExtractionTarget(preferred, issuer, player, tier, random);
                }
                case RECOVERY -> null;
            };
            if (target != null && target.pos() != null) {
                targetPos = target.pos();
                dimensionId = target.level().dimension().location().toString();
            }
        }
        if (type == QuestType.SALVAGE && targetPos == null) return null; // No lost container within reach.
        var rewardDimension = QuestDimension.of(dimensionId);
        if (rewardDimension != QuestDimension.STANDARD) {
            if (type == QuestType.RECOVERY) targetIndex = rewardDimension == QuestDimension.NETHER ? 5 : 6;
            if (type == QuestType.ELIMINATION) targetIndex = rewardDimension == QuestDimension.NETHER ? 6 : 7;
        }
        int targetCount = QuestContract.targetCountFor(type, tier, targetIndex);

        BlockPos prospect = null;
        if (geology) {
            var overworld = player.server.overworld();
            prospect = de.artemis.cyberneticenhancements.common.world.ChromiumGeology.nearest(overworld.getSeed(), issuer.blockPosition(), 160, 760);
            if (prospect == null || !overworld.getWorldBorder().isWithinBounds(prospect)) return null;
            targetPos = new BlockPos(prospect.getX(), issuer.blockPosition().getY(), prospect.getZ());
            dimensionId = overworld.dimension().location().toString();
        }
        int rewardMoney = de.artemis.cyberneticenhancements.common.economy.EconomyBalance.tier("quest." + type.id() + ".base", tier)
                + random.nextInt(de.artemis.cyberneticenhancements.common.economy.EconomyBalance.tier("quest." + type.id() + ".spread", tier) + 1);
        if (type == QuestType.RECOVERY) rewardMoney += targetCount * de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("recovery.unit." + targetIndex);
        rewardMoney = Math.max(1, Math.round(rewardMoney * moneyProfileMultiplier(issuer.npcType())));
        rewardMoney = de.artemis.cyberneticenhancements.common.streetcred.StreetCredEffects.questPay(player,
                de.artemis.cyberneticenhancements.common.origin.Origins.questPay(player, type, rewardMoney));
        rewardMoney = de.artemis.cyberneticenhancements.common.skills.SkillEffects.combatPay(player,
                type == QuestType.ELIMINATION || type == QuestType.CYBERPSYCHO || type == QuestType.BREACH, rewardMoney);
        int rewardTrust = de.artemis.cyberneticenhancements.common.economy.EconomyBalance.tier("trust.quest." + type.id(), tier);
        int rewardItemKind = geology ? QuestContract.REWARD_CHROMIUM : QuestContract.rewardItemKindForTier(tier, type);
        if (type == QuestType.LOCATE && random.nextInt(4) == 0) rewardItemKind = QuestContract.REWARD_COVERAGE; // A quarter of saved patients come with a year of cover.
        int rewardItemCount = geology ? 4 : QuestContract.rewardItemCountFor(rewardItemKind);
        var offer = new QuestContract(
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
        if (geology) offer.setInvestigation(new InvestigationSurvey(targetPos, prospect));
        if (type == QuestType.LOCATE) offer.setReturnPos(issuer.blockPosition());
        return offer;
    }

    public static float moneyProfileMultiplier(NpcType npcType) {
        return switch (npcType) {
            case FIXER -> 1.0F;
            case RIPPERDOC, STREET_RIPPERDOC, TECHIE, MED_TECH, WORLD_MED_TECH -> 0.95F;
            case NETRUNNER -> 1.04F;
            case MERC, MAXTEC -> 1.08F;
        };
    }

    private static @Nullable DeliveryTarget generateSalvageTarget(ServerLevel level, AbstractCityNpcEntity issuer) {
        if (level.dimension() != Level.OVERWORLD) return null;
        BlockPos container = SalvageQuests.nearest(level, issuer.blockPosition());
        return container == null ? null : new DeliveryTarget(level, container);
    }

    private static DeliveryTarget generateDeliveryTarget(ServerLevel preferredLevel, AbstractCityNpcEntity issuer, ServerPlayer player, int tier, RandomSource random) {
        ServerLevel targetLevel = chooseDeliveryLevel(preferredLevel);
        BlockPos origin = targetLevel == preferredLevel
                ? (issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition())
                : player.blockPosition();
        int minDistance = DELIVERY_MIN_DISTANCE + (tier - 1) * 60;
        int maxDistance = Math.min(DELIVERY_MAX_DISTANCE, DELIVERY_MIN_DISTANCE + 260 + tier * 180);
        return new DeliveryTarget(targetLevel, findSurfaceTarget(targetLevel, origin, player, minDistance, maxDistance, random, true));
    }

    private static DeliveryTarget generateExtractionTarget(ServerLevel preferredLevel, AbstractCityNpcEntity issuer, ServerPlayer player, int tier, RandomSource random) {
        ServerLevel targetLevel = chooseDeliveryLevel(preferredLevel);
        BlockPos origin = targetLevel == preferredLevel
                ? (issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition())
                : player.blockPosition();
        int minDistance = EXTRACTION_MIN_DISTANCE + (tier - 1) * 50;
        int maxDistance = Math.min(EXTRACTION_MAX_DISTANCE, EXTRACTION_MIN_DISTANCE + 220 + tier * 160);
        return new DeliveryTarget(targetLevel, findSurfaceTarget(targetLevel, origin, player, minDistance, maxDistance, random, true));
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

    private static DeliveryTarget generateCyberpsychoTarget(ServerLevel preferredLevel, AbstractCityNpcEntity issuer, ServerPlayer player, int tier, RandomSource random) {
        var level = chooseDeliveryLevel(preferredLevel);
        var origin = level == preferredLevel ? (issuer.hasRestriction() ? issuer.getRestrictCenter() : issuer.blockPosition()) : player.blockPosition();
        return new DeliveryTarget(level, findSurfaceTarget(level, origin, player, 220 + (tier - 1) * 60, 600 + tier * 160, random, false, 64));
    }

    private static BlockPos findSurfaceTarget(ServerLevel level, BlockPos origin, ServerPlayer player, int minDistance, int maxDistance, RandomSource random) {
        return findSurfaceTarget(level, origin, player, minDistance, maxDistance, random, false);
    }

    private static BlockPos findSurfaceTarget(ServerLevel level, BlockPos origin, ServerPlayer player, int minDistance, int maxDistance, RandomSource random, boolean underwater) {
        return findSurfaceTarget(level, origin, player, minDistance, maxDistance, random, underwater, 48);
    }

    private static BlockPos findSurfaceTarget(ServerLevel level, BlockPos origin, ServerPlayer player, int minDistance, int maxDistance, RandomSource random, boolean underwater, int clearance) {
        reserveKnownActiveSites(level);
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance));
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) continue;
            if (!QuestTerrainPreparation.ready(level, x, z)) { generatingTerrain = true; return null; }
            BlockPos candidate = underwater ? courierGroundPosition(level, x, z)
                    : surfaceGroundPosition(level, x, z);
            if (candidate.distManhattan(player.blockPosition()) > 96 && (underwater ? isUsableCourierPosition(level, candidate) : isUsableStashPosition(level, candidate))
                    && level.getWorldBorder().isWithinBounds(candidate)
                    && QuestSiteReservations.get(level).available(candidate, clearance, "", level.getGameTime())) {
                return candidate;
            }
        }
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int distance = maxDistance + 64 * (1 + attempt / 8);
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) continue;
            if (!QuestTerrainPreparation.ready(level, x, z)) { generatingTerrain = true; return null; }
            BlockPos candidate = underwater ? courierGroundPosition(level, x, z)
                    : surfaceGroundPosition(level, x, z);
            if ((underwater ? isUsableCourierPosition(level, candidate) : isUsableStashPosition(level, candidate))
                    && level.getWorldBorder().isWithinBounds(candidate)
                    && QuestSiteReservations.get(level).available(candidate, clearance, "", level.getGameTime())) return candidate;
        }
        return null;
    }

    private static void reserveKnownActiveSites(ServerLevel level) {
        for (ServerPlayer online : level.getServer().getPlayerList().getPlayers()) {
            CompoundTag root = rootTag(online, false);
            if (root == null) continue;
            CompoundTag boards = issuerBoards(root);
            for (var ref : activeSlots(boards)) {
                QuestContract active = QuestContract.fromTag(boards.getCompound(ref.key()).getCompound(ref.slot()));
                if (level.dimension().location().toString().equals(active.dimensionId())) {
                    QuestSiteReservations.get(level).reserve(active, true, level.getGameTime());
                }
            }
        }
    }

    private static boolean isUsableStashPosition(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos) || pos.getY() <= level.getMinBuildHeight() + 1) {
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
        ResourceLocation id = ResourceLocation.tryParse(contract.dimensionId());
        if (id == null) return null;
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, id);
        return player.serverLevel().getServer().getLevel(levelKey);
    }

    private static void issueCourierPackage(ServerPlayer player, QuestContract contract) {
        ItemStack packageStack = createCourierPackage(contract);
        giveQuestItem(player, packageStack);
    }

    private static ItemStack createCourierPackage(QuestContract contract) {
        ItemStack stack = new ItemStack(ModItems.COURIER_PACKAGE.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.cyberneticenhancements.courier_package"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_PACKAGE)));
        return stack;
    }

    private static ItemStack createReviveInjector(QuestContract contract) {
        ItemStack stack = new ItemStack(ModItems.REVIVE_INJECTOR.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_REVIVE)));
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.translatable("tooltip.cyberneticenhancements.revive_injector.use"),
                Component.translatable("tooltip.cyberneticenhancements.quest_item.lose").withStyle(net.minecraft.ChatFormatting.RED))));
        return stack;
    }

    private static ItemStack createSignalBeacon(QuestContract contract) {
        ItemStack stack = new ItemStack(ModItems.SIGNAL_BEACON.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_BEACON)));
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.translatable("tooltip.cyberneticenhancements.signal_beacon.use"),
                Component.translatable("tooltip.cyberneticenhancements.quest_item.lose").withStyle(net.minecraft.ChatFormatting.RED))));
        return stack;
    }

    private static ItemStack createExtractionShard(QuestContract contract) {
        ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.cyberneticenhancements.exfil_data_shard"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_EXTRACTION)));
        return stack;
    }

    public static ItemStack createCyberpsychoShard(QuestContract contract) {
        var stack = new ItemStack(Items.AMETHYST_SHARD);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.cyberneticenhancements.cyberpsycho_dossier", contract.psychoName()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(questItemTag(contract, QUEST_ITEM_TYPE_PSYCHO)));
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                Component.translatable("tooltip.cyberneticenhancements.cyberpsycho.subject", contract.psychoName()),
                Component.translatable("tooltip.cyberneticenhancements.cyberpsycho.cyberware", de.artemis.cyberneticenhancements.common.entity.CyberpsychoLoadout.fromIndex(contract.targetIndex()).label()),
                Component.translatable("tooltip.cyberneticenhancements.cyberpsycho.return", contract.issuerName()))));
        return stack;
    }

    private static boolean isLegacyQuestCompass(ItemStack stack) {
        if (!stack.is(Items.COMPASS)) return false;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.getString(QUEST_CONTRACT_ID_TAG).isEmpty() && LEGACY_COMPASS_TYPES.contains(tag.getString(QUEST_ITEM_TYPE_TAG));
    }

    private static CompoundTag questItemTag(QuestContract contract, String itemType) {
        CompoundTag tag = new CompoundTag();
        tag.putString(QUEST_ITEM_TYPE_TAG, itemType);
        tag.putString(QUEST_CONTRACT_ID_TAG, contract.id());
        tag.putUUID(QUEST_ISSUER_ID_TAG, contract.issuerId());
        return tag;
    }

    private static @Nullable UUID giveQuestItem(ServerPlayer player, ItemStack stack) {
        ItemStack copy = stack.copy();
        QuestItemVersions.get(player.serverLevel()).issue(copy);
        if (!player.addItem(copy)) {
            var dropped = player.drop(copy, false);
            if (dropped != null) { dropped.setTarget(player.getUUID()); return dropped.getUUID(); }
        }
        return null;
    }

    private static BlockPos surfaceGroundPosition(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1, z);
    }

    static BlockPos courierGroundPosition(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15) + 1, z);
    }

    static boolean isUsableCourierPosition(ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.NETHER || level.dimension() == Level.END) return DimensionalQuestSites.usable(level, pos);
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos) || !level.isInWorldBounds(pos.above())) return false;
        var fluid = level.getFluidState(pos);
        var aboveFluid = level.getFluidState(pos.above());
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                && level.getBlockState(pos).canBeReplaced()
                && level.getBlockState(pos.above()).canBeReplaced()
                && (fluid.isEmpty() || fluid.is(net.minecraft.tags.FluidTags.WATER))
                && (aboveFluid.isEmpty() || aboveFluid.is(net.minecraft.tags.FluidTags.WATER));
    }

    static boolean ensureCourierStash(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null) return false;
        boolean changed = ensureQuestContainer(level, contract.targetPos(), Blocks.CHEST, contract.id(), contract.allowsLegacyContainerMigration());
        if (level.hasChunkAt(contract.targetPos()) && level.getBlockEntity(contract.targetPos()) instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity crate
                && crate.questId().equals(contract.id())) changed |= contract.markSiteBound();
        return changed;
    }

    private static boolean ensureQuestContainer(ServerLevel level, BlockPos pos, Block legacyBlock, String questId, boolean legacyAllowed) {
        if (!level.hasChunkAt(pos)) return false;
        var previous = level.getBlockState(pos);
        if (previous.is(ModBlocks.IRON_QUEST_CRATE.get()))
            return legacyAllowed && level.getBlockEntity(pos) instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity crate && crate.bindQuest(questId);
        boolean legacy = previous.is(legacyBlock);
        if (legacy && !legacyAllowed) return false;
        if (!legacy && !isUsableCourierPosition(level, pos)) return false;
        var contents = new ArrayList<ItemStack>();
        var oldEntity = level.getBlockEntity(pos);
        Component customName = oldEntity instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity container ? container.getCustomName() : null;
        if (legacy && oldEntity instanceof Container container) {
            for (var player : level.players()) {
                if (player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu menu && menu.getContainer() == container) player.closeContainer();
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) contents.add(container.getItem(slot).copy());
            container.clearContent();
        }
        boolean waterlogged = level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                || legacy && level.getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER);
        BlockState state = ModBlocks.IRON_QUEST_CRATE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.BarrelBlock.FACING, net.minecraft.core.Direction.UP).setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,
                waterlogged);
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            if (oldEntity instanceof Container container) for (int slot = 0; slot < contents.size(); slot++) container.setItem(slot, contents.get(slot));
            return false;
        }
        if (level.getBlockEntity(pos) instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity crate) {
            crate.bindQuest(questId);
            if (customName != null) crate.applyComponents(net.minecraft.core.component.DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, customName).build(), net.minecraft.core.component.DataComponentPatch.EMPTY);
            for (int slot = 0; slot < contents.size(); slot++) crate.setItem(slot, contents.get(slot));
            crate.setChanged();
        }
        return true;
    }

    static boolean ensureExtractionDrop(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null || contract.destinationReached()) {
            return false;
        }
        BlockPos barrelPos = contract.targetPos();
        if (!level.hasChunkAt(barrelPos)) return false;
        boolean dirty = ensureQuestContainer(level, barrelPos, Blocks.BARREL, contract.id(), contract.allowsLegacyContainerMigration());
        if (!level.getBlockState(barrelPos).is(ModBlocks.IRON_QUEST_CRATE.get())) return false;
        if (!(level.getBlockEntity(barrelPos) instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity crate) || !crate.questId().equals(contract.id())) return false;
        dirty |= contract.markSiteBound();
        if (level.getBlockEntity(barrelPos) instanceof Container container) {
            if (!containerContainsQuestItem(level, container, contract, QUEST_ITEM_TYPE_EXTRACTION)
                    && !QuestItemVersions.get(level).issued(contract.id(), QUEST_ITEM_TYPE_EXTRACTION)) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    if (container.getItem(slot).isEmpty()) {
                        var shard = createExtractionShard(contract);
                        QuestItemVersions.get(level).issue(shard);
                        container.setItem(slot, shard);
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

    private static boolean ensureBreachSite(@Nullable ServerLevel level, QuestContract contract, UUID owner) {
        if (level == null || contract.targetPos() == null || contract.isReady() || contract.breachAttemptStarted()) {
            return false;
        }
        BlockPos cachePos = contract.targetPos();
        if (!level.hasChunkAt(cachePos)) return false;
        if (level.getBlockEntity(cachePos) instanceof RelicCacheBlockEntity existing) {
            if (existing.isBoundToQuest(owner, contract.id())) return contract.markSiteBound();
            if (contract.allowsLegacyContainerMigration() && !existing.isQuestCache()
                    && level.getBlockState(cachePos).getBlock() == breachCacheStateFor(contract.tier()).getBlock()) {
                boolean bound = existing.bindQuest(owner, contract.id());
                contract.markSiteBound();
                return bound;
            }
            return false;
        }
        if (!isUsableCourierPosition(level, cachePos)) return false;
        boolean dirty = false;
        BlockState expectedState = breachCacheStateFor(contract.tier());
        if (level.getBlockState(cachePos).getBlock() != expectedState.getBlock()) {
            level.setBlock(cachePos, expectedState, Block.UPDATE_ALL);
            dirty = true;
        }
        if (level.getBlockEntity(cachePos) instanceof RelicCacheBlockEntity cache)
            dirty |= cache.bindQuest(owner, contract.id());
        contract.markSiteBound();
        return dirty;
    }

    private static boolean consumeCourierPackageFromStash(ServerLevel level, QuestContract contract) {
        if (contract.targetPos() == null || !level.hasChunkAt(contract.targetPos())) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(contract.targetPos());
        if (!(blockEntity instanceof de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity container)
                || !container.questId().equals(contract.id())) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isQuestCourierPackage(stack, contract) && QuestItemVersions.get(level).current(stack)) {
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

    private static boolean containerContainsQuestItem(ServerLevel level, Container container, QuestContract contract, String itemType) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (matchesQuestItem(container.getItem(slot), contract, itemType) && QuestItemVersions.get(level).current(container.getItem(slot))) {
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
        QuestItemVersions.get(player.serverLevel()).retire(contract.id());
        ServerLevel siteLevel = resolveContractLevel(player, contract);
        if (siteLevel != null) {
            QuestSiteReservations.get(siteLevel).reserve(contract, true, siteLevel.getGameTime());
            QuestSiteReservations.get(siteLevel).finish(contract.id());
        }
        switch (contract.type()) {
            case DELIVERY -> {
                cleanupCourierPackage(player, contract);
                cleanupCourierStash(resolveContractLevel(player, contract), contract);
            }
            case EXTRACTION -> {
                cleanupExtractionItems(player, contract);
                cleanupExtractionMarker(resolveContractLevel(player, contract), contract);
            }
            case INVESTIGATION -> {
                if (contract.investigation() != null && contract.investigation().located())
                    cleanupBreachMarker(resolveContractLevel(player, contract), contract);
                cleanupInvestigationMarker(resolveContractLevel(player, contract), contract);
                var scan = SURVEY_SCANS.get(player);
                if (scan != null && scan.questId().equals(contract.id())) SURVEY_SCANS.remove(player);
            }
            case BREACH -> {
                cleanupBreachMarker(resolveContractLevel(player, contract), contract);
            }
            case ELIMINATION -> {
                cleanupEliminationMarker(resolveContractLevel(player, contract), contract);
            }
            case CYBERPSYCHO -> {
                CyberpsychoEncounter.cleanup(player, contract);
                removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_PSYCHO);
            }
            case SALVAGE -> removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_SALVAGE);
            case LOCATE -> {
                PatientSearch.cleanup(player, contract);
                removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_REVIVE);
                removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_BEACON);
                if (!QuestFailureCalls.isQueued(player, contract.id())) AbstractCityNpcEntity.closeTemporaryContact(player, contract.issuerId());
            }
            default -> {
            }
        }
    }

    private static void cleanupCourierPackage(ServerPlayer player, QuestContract contract) {
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_PACKAGE);
    }

    private static void cleanupExtractionItems(ServerPlayer player, QuestContract contract) {
        removeTaggedQuestItems(player, contract, QUEST_ITEM_TYPE_EXTRACTION);
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
        if (matchesQuestItem(player.containerMenu.getCarried(), contract, itemType)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
        }
        if (matchesQuestItem(player.inventoryMenu.getCarried(), contract, itemType)) player.inventoryMenu.setCarried(ItemStack.EMPTY);
        for (var slot : player.containerMenu.slots)
            if (matchesQuestItem(slot.getItem(), contract, itemType)) slot.set(ItemStack.EMPTY);
    }

    private static boolean hasTaggedQuestItem(ServerPlayer player, QuestContract contract, String itemType) {
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
        return matchesQuestItem(player.containerMenu.getCarried(), contract, itemType)
                || matchesQuestItem(player.inventoryMenu.getCarried(), contract, itemType);
    }


    private static boolean courierStashContainsPackage(ServerLevel level, QuestContract contract) {
        if (contract.targetPos() == null || !level.hasChunkAt(contract.targetPos())) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(contract.targetPos());
        return blockEntity instanceof Container container && containerContainsQuestItem(level, container, contract, QUEST_ITEM_TYPE_PACKAGE);
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
        if (level != null && contract.targetPos() != null) QuestRecovery.cleanupContainer(level, contract.targetPos(), contract.id());
    }

    private static void cleanupExtractionMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (level != null && contract.targetPos() != null) QuestRecovery.cleanupContainer(level, contract.targetPos(), contract.id());
    }

    private static void cleanupInvestigationMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (contract.chromiumSurvey()) return;
        if (level == null || contract.targetPos() == null || !level.hasChunkAt(contract.targetPos())) {
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
        if (contract.chromiumSurvey()) return;
        if (level == null || contract.targetPos() == null || !level.hasChunkAt(contract.targetPos())) {
            return;
        }
        BlockPos cachePos = contract.targetPos();
        BlockEntity blockEntity = level.getBlockEntity(cachePos);
        if (blockEntity instanceof RelicCacheBlockEntity cacheEntity && (cacheEntity.isAnimating() || cacheEntity.hasActiveHackSession())) {
            return;
        }
        if (blockEntity instanceof RelicCacheBlockEntity cache && cache.isBoundToQuest(contract.id())) {
            level.removeBlock(cachePos, false);
        }
    }

    private static void cleanupEliminationMarker(@Nullable ServerLevel level, QuestContract contract) {
        if (level == null || contract.targetPos() == null || !level.hasChunkAt(contract.targetPos())) {
            return;
        }
        BlockPos lodestonePos = contract.targetPos().below();
        if (level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            level.removeBlock(lodestonePos, false);
        }
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
            count += boardActives(board).size();
            count += board.getList(BOARD_OFFERS_TAG, Tag.TAG_COMPOUND).size();
            count += board.getList(BOARD_HISTORY_TAG, Tag.TAG_COMPOUND).size();
        }
        return count;
    }

    private static void applyAbandonPenalty(ServerPlayer player, QuestContract contract) {
        int trustLoss = de.artemis.cyberneticenhancements.common.skills.SkillEffects.trustLoss(player, Math.max(2, contract.rewardTrust() + 2));
        if (!contract.issuerType().isBlank()) {
            AbstractCityNpcEntity.lowerTrustFromArchive(player, contract.issuerId(), trustLoss);
        }
        notifyQuestAction(player, Component.translatable("message.cyberneticenhancements.quest.abandoned", contract.title(), trustLoss)
                .withStyle(net.minecraft.ChatFormatting.GOLD), PlayerActionFeedback.CANCELLED);
    }

    private static void notifyQuestAction(ServerPlayer player, Component message, PlayerActionFeedback feedback) {
        de.artemis.cyberneticenhancements.common.journal.PlayerJournal.message(player, "quests", message);
        var text = message.copy();
        if (text.getStyle().getColor() == null) text.withStyle(net.minecraft.ChatFormatting.AQUA);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, text, feedback);
    }

    private static void cancelUnfinishableContract(ServerPlayer player, CompoundTag board, QuestContract contract, String reason) {
        cleanupContractArtifacts(player, contract);
        contract.cancel(reason);
        contract.setResolvedDay(currentDay(player.level()));
        removeActive(board, contract.id());
        board.put(BOARD_OFFERS_TAG, new ListTag());
        appendHistory(player, board, contract);
        notifyQuestAction(player, Component.translatable("message.cyberneticenhancements.quest.cancelled", contract.title(), contract.cancellationReason())
                .withStyle(net.minecraft.ChatFormatting.GOLD), PlayerActionFeedback.CANCELLED);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
    }

    private static void expireUrgentContract(ServerPlayer player, CompoundTag board, QuestContract contract) {
        contract.expireUrgent();
        lockOutEncounter(player, contract);
        contract.setResolvedDay(currentDay(player.level()));
        cleanupContractArtifacts(player, contract);
        removeActive(board, contract.id());
        appendHistory(player, board, contract);
        notifyQuestAction(player, Component.translatable("message.cyberneticenhancements.quest.urgent_expired", contract.title())
                .withStyle(net.minecraft.ChatFormatting.RED), PlayerActionFeedback.FAILED);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
    }


    private static void appendHistory(ServerPlayer player, CompoundTag board, QuestContract contract) {
        List<QuestContract> history = readContracts(board.getList(BOARD_HISTORY_TAG, Tag.TAG_COMPOUND));
        history.add(0, contract);
        if (history.size() > MAX_HISTORY) {
            var pending = QuestRewardDeliveries.get(player.server).pendingContractIds(player.getUUID());
            var retained = new ArrayList<QuestContract>();
            int resolved = 0;
            for (var entry : history) {
                if (pending.contains(entry.id()) || resolved++ < MAX_HISTORY) retained.add(entry);
            }
            history = retained;
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

    /** Updates the open contract with the same id, or takes the first free slot. */
    private static void writeActive(CompoundTag board, QuestContract active) {
        String free = null;
        for (String slot : ACTIVE_TAGS) {
            if (!board.contains(slot, Tag.TAG_COMPOUND)) {
                if (free == null) free = slot;
            } else if (board.getCompound(slot).getString("Id").equals(active.id())) {
                board.put(slot, active.toTag());
                return;
            }
        }
        board.put(free == null ? BOARD_ACTIVE_TAG : free, active.toTag());
    }

    private static void removeActive(CompoundTag board, String id) {
        for (String slot : ACTIVE_TAGS)
            if (board.contains(slot, Tag.TAG_COMPOUND) && board.getCompound(slot).getString("Id").equals(id)) board.remove(slot);
    }

    private static List<QuestContract> boardActives(CompoundTag board) {
        List<QuestContract> result = new ArrayList<>();
        for (String slot : ACTIVE_TAGS)
            if (board.contains(slot, Tag.TAG_COMPOUND)) result.add(QuestContract.fromTag(board.getCompound(slot)));
        return result;
    }

    /** The open contract with this id, or the first one when the id is empty. */
    private static @Nullable QuestContract boardActive(CompoundTag board, String id) {
        for (var contract : boardActives(board)) if (id.isEmpty() || id.equals(contract.id())) return contract;
        return null;
    }

    private static boolean hasActive(CompoundTag board) {
        for (String slot : ACTIVE_TAGS) if (board.contains(slot, Tag.TAG_COMPOUND)) return true;
        return false;
    }

    private static boolean activeFull(CompoundTag board) {
        return boardActives(board).size() >= MAX_ACTIVE_PER_ISSUER;
    }

    private record ActiveRef(String key, String slot) {}

    private static List<ActiveRef> activeSlots(CompoundTag boards) {
        List<ActiveRef> result = new ArrayList<>();
        for (String key : boards.getAllKeys()) {
            if (!boards.contains(key, Tag.TAG_COMPOUND)) continue;
            var board = boards.getCompound(key);
            for (String slot : ACTIVE_TAGS) if (board.contains(slot, Tag.TAG_COMPOUND)) result.add(new ActiveRef(key, slot));
        }
        return result;
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
        QuestNavigation.sync(player);
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

    public record BoardSnapshot(List<QuestContract> actives, List<QuestContract> offers) {
        /** The first open contract, for callers that only care whether there is one. */
        public @Nullable QuestContract active() {
            return actives.isEmpty() ? null : actives.get(0);
        }
    }

    public enum Result {
        OK,
        INVALID,
        HAS_ACTIVE
    }

    public enum TurnInResult {
        OK,
        NOT_READY,
        MISSING_ITEMS,
        UNAVAILABLE,
        IN_PERSON_REQUIRED,
        /** The hand-in was a false report; the contract is over and nothing was paid. */
        REJECTED
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
