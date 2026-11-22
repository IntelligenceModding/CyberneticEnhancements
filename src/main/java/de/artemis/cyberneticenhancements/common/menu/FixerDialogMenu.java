package de.artemis.cyberneticenhancements.common.menu;

import de.artemis.cyberneticenhancements.common.entity.DevTestEntity;
import de.artemis.cyberneticenhancements.common.quest.QuestDimension;
import java.util.UUID;
import de.artemis.cyberneticenhancements.common.dialogue.NpcService;
import de.artemis.cyberneticenhancements.common.dialogue.NpcShopCatalog;
import de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.NpcType;
import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.registry.ModItems;
import de.artemis.cyberneticenhancements.common.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public final class FixerDialogMenu extends AbstractBaseMenu {
    public static final int OPTION_INTRO = 0;
    public static final int OPTION_STABILIZE = 2;
    public static final int OPTION_PURGE = 3;
    public static final int OPTION_BUY_MAXDOC_MK2 = 4;
    public static final int OPTION_BUY_RAM_JOLT = 5;
    public static final int OPTION_BUY_CHROME_SUPPRESSANT = 6;
    public static final int OPTION_BUY_IMMUNOBLOCKERS = 7;
    public static final int OPTION_ACCEPT_CONTRACT_1 = 8;
    public static final int OPTION_ACCEPT_CONTRACT_2 = 9;
    public static final int OPTION_ACCEPT_CONTRACT_3 = 10;
    public static final int OPTION_TURN_IN_CONTRACT = 11;
    public static final int OPTION_ABANDON_CONTRACT = 12;
    public static final int OPTION_TURN_IN_CONTRACT_2 = 13;
    public static final int OPTION_ABANDON_CONTRACT_2 = 14;
    public static final int OPTION_TURN_IN_CONTRACT_3 = 15;
    public static final int OPTION_ABANDON_CONTRACT_3 = 16;

    public static final int RESPONSE_GREETING = 0;
    public static final int RESPONSE_INTRO = 1;
    public static final int RESPONSE_STABILIZE_OK = 3;
    public static final int RESPONSE_NEED_FUNDS = 4;
    public static final int RESPONSE_PURGE_LOCKED = 5;
    public static final int RESPONSE_PURGE_COOLDOWN = 6;
    public static final int RESPONSE_PURGE_OK = 7;
    public static final int RESPONSE_MAXDOC_OK = 8;
    public static final int RESPONSE_RAM_JOLT_OK = 9;
    public static final int RESPONSE_CHROME_SUPPRESSANT_OK = 10;
    public static final int RESPONSE_IMMUNOBLOCKERS_LOCKED = 11;
    public static final int RESPONSE_IMMUNOBLOCKERS_OK = 12;
    public static final int RESPONSE_BUSY_STATE = 13;
    public static final int RESPONSE_COOL_OFF = 14;
    public static final int RESPONSE_CONTRACTS = 15;
    public static final int RESPONSE_CONTRACT_ACCEPTED = 16;
    public static final int RESPONSE_CONTRACT_TURNED_IN = 17;
    public static final int RESPONSE_CONTRACT_NOT_READY = 18;
    public static final int RESPONSE_CONTRACT_MISSING_ITEMS = 19;
    public static final int RESPONSE_CONTRACT_ABANDONED = 20;
    public static final int RESPONSE_CONTRACT_MAX_ACTIVE = 21;
    public static final int RESPONSE_CONTRACT_ALREADY_ACTIVE = 22;
    public static final int RESPONSE_IDENTITY_LOCKED = 23;
    public static final int RESPONSE_IDENTITY_NICKNAME_OK = 24;
    public static final int RESPONSE_IDENTITY_APPEARANCE_OK = 25;
    public static final int RESPONSE_IDENTITY_INVALID = 26;
    public static final int RESPONSE_IDENTITY_COLOR_OK = 27;
    public static final int RESPONSE_DEV_QUEST_UNAVAILABLE = 29;
    private static final int DEV_QUEST_OPTION_BASE = 100;
    private static final int TRUST_MAX_POINTS = 300;

    private static final int DATA_TRUST = 0;
    private static final int DATA_TRUST_POINTS = 1;
    private static final int DATA_RESPONSE = 2;
    private static final int DATA_OPTION_MASK = 3;
    private static final int DATA_STATUS = 4;
    private static final int DATA_PURGE_READY = 5;
    private static final int DATA_BALANCE = 6;
    private static final int DATA_STABILIZE_PRICE = 7;
    private static final int DATA_PURGE_PRICE = 8;
    private static final int DATA_ACTIVE_BASE = 9;
    private static final int ACTIVE_SLOTS = PlayerQuestManager.MAX_ACTIVE_PER_ISSUER;
    private static final int ACTIVE_PRESENT = 0, ACTIVE_TYPE = 1, ACTIVE_TIER = 2, ACTIVE_TARGET_INDEX = 3, ACTIVE_TARGET_COUNT = 4,
            ACTIVE_PROGRESS = 5, ACTIVE_READY = 6, ACTIVE_REWARD_MONEY = 7, ACTIVE_REWARD_TRUST = 8, ACTIVE_REWARD_ITEM_KIND = 9,
            ACTIVE_REWARD_ITEM_COUNT = 10, ACTIVE_HAS_LOCATION = 11, ACTIVE_LOCATION_X = 12, ACTIVE_LOCATION_Y = 13,
            ACTIVE_LOCATION_Z = 14, ACTIVE_ROUTE = 15;
    private static final int DATA_PER_ACTIVE = 16;
    private static final int DATA_OFFER_BASE = DATA_ACTIVE_BASE + DATA_PER_ACTIVE * ACTIVE_SLOTS;
    private static final int DATA_PER_OFFER = 10;
    private static final int OFFER_SLOTS = PlayerQuestManager.DEV_OFFER_COUNT;
    private static final int DATA_SHOP_PRICE_MULTIPLIER = DATA_OFFER_BASE + DATA_PER_OFFER * OFFER_SLOTS;
    private static final int DATA_OFFERS_PREPARING = DATA_SHOP_PRICE_MULTIPLIER + 1;
    private static final int DATA_COUNT = DATA_SHOP_PRICE_MULTIPLIER + 2;

    private final Player player;
    private final AbstractCityNpcEntity npc;
    private final int[] syncedData = new int[DATA_COUNT];
    private int responseId;
    private boolean synchronizingBoard;
    private PlayerQuestManager.BoardSnapshot synchronizationBoard;
    private DevQuestRequest devQuestRequest;
    private long nextDevQuestCheck;

    private record DevQuestRequest(QuestDimension dimension, int slot, UUID id, long expires) {}

    private void withBoardSnapshot(Runnable synchronize) {
        boolean previous = synchronizingBoard;
        var previousBoard = synchronizationBoard;
        synchronizingBoard = true; synchronizationBoard = null;
        try { synchronize.run(); }
        finally { synchronizingBoard = previous; synchronizationBoard = previousBoard; }
    }

    @Override public void broadcastChanges() {
        if (!advanceDevQuest()) withBoardSnapshot(super::broadcastChanges);
    }
    @Override public void broadcastFullState() { withBoardSnapshot(super::broadcastFullState); }
    @Override public void sendAllDataToRemote() { withBoardSnapshot(super::sendAllDataToRemote); }

    private PlayerQuestManager.BoardSnapshot contractBoard() {
        if (!(player instanceof ServerPlayer serverPlayer) || npc == null || !npc.npcType().supportsContracts()) return null;
        if (synchronizingBoard && synchronizationBoard != null) return synchronizationBoard;
        var board = isDevTest() ? new PlayerQuestManager.BoardSnapshot(PlayerQuestManager.activeContracts(serverPlayer).stream()
                .filter(contract -> contract.issuerId().equals(npc.getUUID())).toList(), java.util.List.of())
                : PlayerQuestManager.snapshotForIssuer(serverPlayer, npc);
        if (synchronizingBoard) synchronizationBoard = board;
        return board;
    }

    private int initialView;

    /** The window it opens in, as numbered by {@code FaceDialogueWindowPayload}, or 0 for the conversation. */
    public int initialView() { return initialView; }

    public FixerDialogMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, (AbstractCityNpcEntity) inventory.player.level().getEntity(extraData.readVarInt()));
        this.initialView = extraData.readVarInt();
    }

    public FixerDialogMenu(int containerId, Inventory inventory, AbstractCityNpcEntity npc) {
        super(ModMenuTypes.FIXER_DIALOG.get(), containerId);
        this.player = inventory.player;
        this.npc = npc;
        this.responseId = npc == null ? RESPONSE_BUSY_STATE : RESPONSE_GREETING;
        if (npc != null) npc.noteConversationReply();
        addStateSlots();
    }

    private void addStateSlots() {
        for (int dataIndex = 0; dataIndex < DATA_COUNT; dataIndex++) {
            final int slotIndex = dataIndex;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return serverValue(slotIndex);
                }

                @Override
                public void set(int value) {
                    syncedData[slotIndex] = value;
                }
            });
        }
    }

    private int serverValue(int dataIndex) {
        if (dataIndex == DATA_SHOP_PRICE_MULTIPLIER) return npc == null ? 1000 : Math.round(npc.priceMultiplier(player) * 1000.0F);
        if (npc == null) {
            return 0;
        }
        PlayerQuestManager.BoardSnapshot board = contractBoard();
        if (dataIndex >= DATA_ACTIVE_BASE && dataIndex < DATA_OFFER_BASE)
            return activeServerValue((dataIndex - DATA_ACTIVE_BASE) / DATA_PER_ACTIVE, (dataIndex - DATA_ACTIVE_BASE) % DATA_PER_ACTIVE, board);
        if (dataIndex == DATA_OFFERS_PREPARING)
            return (isDevTest() ? devQuestRequest != null
                    : player instanceof ServerPlayer serverPlayer && PlayerQuestManager.offersPreparing(serverPlayer, npc)) ? 1 : 0;
        return switch (dataIndex) {
            case DATA_TRUST -> npc.trustLevel(player);
            case DATA_TRUST_POINTS -> npc.trustPoints(player);
            case DATA_RESPONSE -> responseId;
            case DATA_OPTION_MASK -> optionMask();
            case DATA_STATUS -> npc.getBehaviorStatus();
            case DATA_PURGE_READY -> purgeReady() ? 1 : 0;
            case DATA_BALANCE -> PlayerEurodollarManager.balance(player);
            case DATA_STABILIZE_PRICE -> priceForServerOption(OPTION_STABILIZE);
            case DATA_PURGE_PRICE -> priceForServerOption(OPTION_PURGE);
            default -> offerServerValue(dataIndex, board);
        };
    }

    private int activeServerValue(int slot, int field, PlayerQuestManager.BoardSnapshot board) {
        QuestContract contract = board == null || slot >= board.actives().size() ? null : board.actives().get(slot);
        if (contract == null) return field == ACTIVE_TYPE ? -1 : 0;
        boolean located = contract.targetPos() != null;
        return switch (field) {
            case ACTIVE_PRESENT -> 1;
            case ACTIVE_TYPE -> contract.type().ordinal();
            case ACTIVE_TIER -> contract.tier();
            case ACTIVE_TARGET_INDEX -> contract.targetIndex();
            case ACTIVE_TARGET_COUNT -> contract.targetCount();
            case ACTIVE_PROGRESS -> contract.progress();
            case ACTIVE_READY -> contract.isReady() ? 1 : 0;
            case ACTIVE_REWARD_MONEY -> contract.rewardMoney();
            case ACTIVE_REWARD_TRUST -> contract.rewardTrust();
            case ACTIVE_REWARD_ITEM_KIND -> contract.rewardItemKind();
            case ACTIVE_REWARD_ITEM_COUNT -> contract.rewardItemCount();
            case ACTIVE_HAS_LOCATION -> located && !contract.dimensionId().isBlank() ? 1 : 0;
            case ACTIVE_LOCATION_X -> located ? contract.targetPos().getX() : 0;
            case ACTIVE_LOCATION_Y -> located ? contract.targetPos().getY() : 0;
            case ACTIVE_LOCATION_Z -> located ? contract.targetPos().getZ() : 0;
            case ACTIVE_ROUTE -> de.artemis.cyberneticenhancements.common.quest.QuestDimension.of(contract.dimensionId()).ordinal();
            default -> 0;
        };
    }

    private int offerServerValue(int dataIndex, PlayerQuestManager.BoardSnapshot board) {
        if (dataIndex < DATA_OFFER_BASE || board == null) {
            return 0;
        }
        int relative = dataIndex - DATA_OFFER_BASE;
        int slot = relative / DATA_PER_OFFER;
        int field = relative % DATA_PER_OFFER;
        if (slot < 0 || slot >= board.offers().size()) {
            return field == 0 ? 0 : 0;
        }
        QuestContract offer = board.offers().get(slot);
        return switch (field) {
            case 0 -> 1;
            case 1 -> offer.type().ordinal();
            case 2 -> offer.tier();
            case 3 -> offer.targetIndex();
            case 4 -> offer.targetCount();
            case 5 -> offer.rewardMoney();
            case 6 -> offer.rewardTrust();
            case 7 -> offer.rewardItemKind();
            case 8 -> offer.rewardItemCount();
            case 9 -> de.artemis.cyberneticenhancements.common.quest.QuestDimension.of(offer.dimensionId()).ordinal();
            default -> 0;
        };
    }

    public void handleChoice(ServerPlayer serverPlayer, int optionId) {
        if (npc == null || !npc.canInteract(serverPlayer)) {
            responseId = RESPONSE_BUSY_STATE;
            broadcastChanges();
            return;
        }
        if (npc.isRefusing(serverPlayer)) {
            responseId = RESPONSE_COOL_OFF;
            broadcastChanges();
            return;
        }
        npc.noteConversationReply();
        int devOption = optionId - DEV_QUEST_OPTION_BASE;
        if (devOption >= 0 && devOption < PlayerQuestManager.DEV_OFFER_COUNT * QuestDimension.values().length) {
            if (isDevTest() && serverPlayer.containerMenu == this && stillValid(serverPlayer) && devQuestRequest == null) {
                long now = serverPlayer.level().getGameTime();
                devQuestRequest = new DevQuestRequest(QuestDimension.byIndex(devOption / PlayerQuestManager.DEV_OFFER_COUNT),
                        devOption % PlayerQuestManager.DEV_OFFER_COUNT, UUID.randomUUID(), now + 2400);
                nextDevQuestCheck = now;
                responseId = RESPONSE_CONTRACTS;
                broadcastChanges();
            }
            return;
        }
        if (optionId < 0) {
            purchaseShopOffer(serverPlayer, -(optionId + 1));
            return;
        }
        switch (optionId) {
            case OPTION_INTRO -> {
                if (!npc.hasMet(serverPlayer)) {
                    npc.markMet(serverPlayer);
                    npc.raiseTrust(serverPlayer, 3);
                }
                responseId = RESPONSE_INTRO;
            }
            case OPTION_STABILIZE, OPTION_PURGE -> {
                if (NpcService.forNpc(npcType()).isEmpty()) return;
                NpcService service = NpcService.forOption(npcType(), optionId);
                if (service == NpcService.TRAUMA_COVERAGE && serverPlayer.getPersistentData().getBoolean("cyberneticenhancements.trauma_coverage")) { responseId = RESPONSE_PURGE_COOLDOWN; } else if (!isServiceUnlocked(optionId)) {
                    responseId = RESPONSE_PURGE_LOCKED;
                } else if (service.daily() && !purgeReady()) {
                    responseId = RESPONSE_PURGE_COOLDOWN;
                } else if (!chargeFunds(serverPlayer, priceForServerOption(optionId), "Paid for " + service.id())) {
                    responseId = npc.isRefusing(serverPlayer) ? RESPONSE_COOL_OFF : RESPONSE_NEED_FUNDS;
                } else {
                    service.apply(serverPlayer);
                    de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(serverPlayer, "commerce", "service", service.id(), npc.getDisplayName(), priceForServerOption(optionId));
                    if (service.daily()) npc.setLastPurgeDay(serverPlayer, currentDay());
                    npc.rewardCommerceTrust(serverPlayer, trustGainForOption(optionId));
                    npc.recordPaidTransaction(serverPlayer);
                    de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.SERVICE.play(serverPlayer);
                    responseId = optionId == OPTION_STABILIZE ? RESPONSE_STABILIZE_OK : RESPONSE_PURGE_OK;
                }
            }
            case OPTION_BUY_MAXDOC_MK2, OPTION_BUY_RAM_JOLT, OPTION_BUY_CHROME_SUPPRESSANT, OPTION_BUY_IMMUNOBLOCKERS -> {
                purchaseShopOffer(serverPlayer, optionId - OPTION_BUY_MAXDOC_MK2);
                return;
            }
            case OPTION_ACCEPT_CONTRACT_1, OPTION_ACCEPT_CONTRACT_2, OPTION_ACCEPT_CONTRACT_3, 20, 21, 22, 23, 24 -> {
                if (optionId >= 20 && !isDevTest()) return;
                int offerIndex = optionId >= 20 ? optionId - 20 + 3 : optionId - OPTION_ACCEPT_CONTRACT_1;
                PlayerQuestManager.Result result = PlayerQuestManager.acceptIssuerOffer(serverPlayer, npc, offerIndex);
                if (result == PlayerQuestManager.Result.OK) {
                    serverPlayer.closeContainer();
                    return;
                }
                responseId = switch (result) {
                    case OK -> RESPONSE_CONTRACT_ACCEPTED;
                    case HAS_ACTIVE -> RESPONSE_CONTRACT_ALREADY_ACTIVE;
                    case INVALID -> RESPONSE_BUSY_STATE;
                };
            }
            case OPTION_TURN_IN_CONTRACT, OPTION_TURN_IN_CONTRACT_2, OPTION_TURN_IN_CONTRACT_3 -> {
                PlayerQuestManager.TurnInResult result = PlayerQuestManager.turnInIssuerActive(serverPlayer, npc,
                        activeContractId(optionId == OPTION_TURN_IN_CONTRACT ? 0 : optionId == OPTION_TURN_IN_CONTRACT_2 ? 1 : 2));
                responseId = switch (result) {
                    case OK -> RESPONSE_CONTRACT_TURNED_IN;
                    case MISSING_ITEMS -> RESPONSE_CONTRACT_MISSING_ITEMS;
                    case NOT_READY, UNAVAILABLE, IN_PERSON_REQUIRED, REJECTED -> RESPONSE_CONTRACT_NOT_READY;
                };
            }
            case OPTION_ABANDON_CONTRACT, OPTION_ABANDON_CONTRACT_2, OPTION_ABANDON_CONTRACT_3 -> {
                PlayerQuestManager.Result result = PlayerQuestManager.abandonIssuerActive(serverPlayer, npc,
                        activeContractId(optionId == OPTION_ABANDON_CONTRACT ? 0 : optionId == OPTION_ABANDON_CONTRACT_2 ? 1 : 2));
                responseId = result == PlayerQuestManager.Result.OK ? RESPONSE_CONTRACT_ABANDONED : RESPONSE_BUSY_STATE;
            }
            default -> {
            }
        }
        if (switch (responseId) {
            case RESPONSE_PURGE_LOCKED, RESPONSE_PURGE_COOLDOWN, RESPONSE_NEED_FUNDS, RESPONSE_COOL_OFF,
                    RESPONSE_CONTRACT_MAX_ACTIVE, RESPONSE_CONTRACT_ALREADY_ACTIVE, RESPONSE_CONTRACT_MISSING_ITEMS,
                    RESPONSE_CONTRACT_NOT_READY -> true;
            default -> false;
        }) de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(serverPlayer);
        broadcastChanges();
    }

    /** The id of the contract shown in this slot of the window, or a value that matches none, so a stale click does nothing. */
    private String activeContractId(int slot) {
        var board = contractBoard();
        return board == null || slot >= board.actives().size() ? "-" : board.actives().get(slot).id();
    }

    private boolean chargeFunds(ServerPlayer player, int count, String note) {
        if (isDevTest()) return true;
        boolean paid = PlayerEurodollarManager.tryWithdraw(
                player,
                count,
                "npc_purchase",
                note,
                npc == null ? null : npc.getUUID(),
                npc == null ? null : npc.getName().getString()
        );
        if (!paid && npc != null) {
            npc.penalizeNoFundsAttempt(player);
        }
        return paid;
    }

    public java.util.List<NpcShopCatalog.Offer> shopOffers() {
        return isDevTest() ? NpcShopCatalog.testOffers() : NpcShopCatalog.offers(npcType());
    }

    public boolean shopOfferAvailable(int index) {
        return index >= 0 && index < shopOffers().size() && (isDevTest() || (player instanceof ServerPlayer ? npc.trustLevel(player) : trustLevel()) >= shopOffers().get(index).requiredTrust());
    }

    public int shopPrice(int index) {
        if (index < 0 || index >= shopOffers().size() || isDevTest()) return 0;
        int basePrice = shopOffers().get(index).basePrice();
        if (player instanceof ServerPlayer) return priceForBase(basePrice, null);
        int discounted = Math.max(1, Math.round(basePrice * (100 - trustDiscountPercent()) / 100.0F * de.artemis.cyberneticenhancements.common.origin.Origins.priceMultiplier(player, npcType(), null)
                * de.artemis.cyberneticenhancements.common.streetcred.StreetCredEffects.priceMultiplier(player)
                * de.artemis.cyberneticenhancements.common.skills.SkillEffects.vendorMultiplier(player, npcType(), false)));
        return Math.max(1, Math.round(discounted * Math.max(1.0F, syncedData[DATA_SHOP_PRICE_MULTIPLIER] / 1000.0F)));
    }

    public Component shopTrustLabel(int index) {
        return Component.translatable("screen.cyberneticenhancements.fixer.trust." + shopOffers().get(index).requiredTrust());
    }

    public void purchaseShopOffer(ServerPlayer serverPlayer, int index) {
        if (serverPlayer != player || npc == null || !stillValid(serverPlayer) || !supportsShop()
                || index < 0 || index >= shopOffers().size()) return;
        var offer = shopOffers().get(index);
        if (npc.isRefusing(serverPlayer)) responseId = RESPONSE_COOL_OFF;
        else if (npc.trustLevel(serverPlayer) < offer.requiredTrust()) responseId = RESPONSE_IMMUNOBLOCKERS_LOCKED;
        else if (!chargeFunds(serverPlayer, shopPrice(index), "Purchased NPC stock #" + (index + 1))) {
            responseId = npc.isRefusing(serverPlayer) ? RESPONSE_COOL_OFF : RESPONSE_NEED_FUNDS;
        } else {
            ItemStack stack = offer.stack();
            de.artemis.cyberneticenhancements.common.journal.PlayerJournal.record(serverPlayer, "commerce", "purchased", stack.getCount(), stack.getHoverName(), npc.getDisplayName(), shopPrice(index));
            serverPlayer.addItem(stack);
            if (!stack.isEmpty()) serverPlayer.drop(stack, false);
            serverPlayer.inventoryMenu.sendAllDataToRemote();
            npc.rewardCommerceTrust(serverPlayer, index == 3 ? 2 : 1);
            npc.recordPaidTransaction(serverPlayer);
            responseId = RESPONSE_MAXDOC_OK;
            de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.PURCHASE.play(serverPlayer);
        }
        if (responseId != RESPONSE_MAXDOC_OK) de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(serverPlayer);
        broadcastChanges();
    }

    public static ItemStack shopItem(NpcType type, int optionId) {
        int index = optionId - OPTION_BUY_MAXDOC_MK2;
        var offers = NpcShopCatalog.offers(type);
        return index < 0 || index >= offers.size() ? ItemStack.EMPTY : offers.get(index).stack();
    }

    private long currentDay() {
        return player.level().getDayTime() / 24000L;
    }

    private boolean purgeReady() {
        if (isDevTest()) return true;
        return npc != null && npc.lastPurgeDay(player) < currentDay();
    }

    private int optionMask() {
        int mask = 0;
        mask |= 1 << OPTION_INTRO;
        if (supportsServices()) {
            mask |= 1 << OPTION_STABILIZE;
        }
        if (npcType().supportsShop()) {
            mask |= 1 << OPTION_BUY_MAXDOC_MK2;
            mask |= 1 << OPTION_BUY_RAM_JOLT;
        }
        if (npcType().supportsShop() && npc.trustLevel(player) >= unlockLevelForOption(OPTION_BUY_CHROME_SUPPRESSANT)) {
            mask |= 1 << OPTION_BUY_CHROME_SUPPRESSANT;
        }
        if (supportsServices() && isServiceUnlocked(OPTION_PURGE)) {
            mask |= 1 << OPTION_PURGE;
        }
        if (npcType().supportsShop() && npc.trustLevel(player) >= unlockLevelForOption(OPTION_BUY_IMMUNOBLOCKERS)) {
            mask |= 1 << OPTION_BUY_IMMUNOBLOCKERS;
        }
        return mask;
    }

    public int entityId() {
        return npc == null ? -1 : npc.getId();
    }

    public int trustLevel() {
        return syncedData[DATA_TRUST];
    }

    public int trustPoints() {
        return syncedData[DATA_TRUST_POINTS];
    }

    public boolean isDevTest() {
        return npc instanceof de.artemis.cyberneticenhancements.common.entity.DevTestEntity;
    }

    public int acceptOfferOption(int slot) {
        if (slot < 0 || slot >= offerSlots()) throw new IllegalArgumentException("Invalid offer slot");
        return slot < 3 ? OPTION_ACCEPT_CONTRACT_1 + slot : 20 + slot - 3;
    }

    public static int devQuestOption(QuestDimension dimension, int slot) {
        return DEV_QUEST_OPTION_BASE + dimension.ordinal() * PlayerQuestManager.DEV_OFFER_COUNT + slot;
    }

    private boolean advanceDevQuest() {
        if (devQuestRequest == null || !(player instanceof ServerPlayer serverPlayer) || !(npc instanceof DevTestEntity dev)
                || serverPlayer.containerMenu != this) return false;
        long now = serverPlayer.level().getGameTime();
        if (now < nextDevQuestCheck) return false;
        var request = devQuestRequest;
        var result = now < request.expires() && stillValid(player)
                ? PlayerQuestManager.prepareDevQuest(serverPlayer, dev, request.dimension(), request.slot(), request.id())
                : PlayerQuestManager.DevQuestResult.UNAVAILABLE;
        if (result == PlayerQuestManager.DevQuestResult.PREPARING) {
            nextDevQuestCheck = now + 10;
        } else {
            devQuestRequest = null;
            if (result == PlayerQuestManager.DevQuestResult.ACCEPTED) {
                serverPlayer.closeContainer();
                return true;
            }
            responseId = RESPONSE_DEV_QUEST_UNAVAILABLE;
        }
        return false;
    }

    public boolean offersPreparing() { return syncedData[DATA_OFFERS_PREPARING] != 0; }

    public int offerSlots() {
        return isDevTest() ? OFFER_SLOTS : 3;
    }

    public NpcType npcType() {
        return npc == null ? NpcType.FIXER : npc.npcType();
    }

    public String npcTypeId() {
        return npcType().id();
    }

    public Component npcTypeLabel() {
        return isDevTest() ? Component.literal("Dev-Test") : npcType().displayName();
    }

    public boolean supportsContracts() {
        return npcType().supportsContracts();
    }

    public boolean supportsServices() {
        return true;
    }

    public boolean supportsShop() {
        return npcType().supportsShop();
    }

    /** A passing street NPC: no trust, discount or renaming, since it is never kept as a contact. */
    public boolean temporary() {
        return npcType() == NpcType.STREET_RIPPERDOC || npcType() == NpcType.WORLD_MED_TECH;
    }

    public boolean identityUnlocked() {
        if (temporary()) return false;
        if (npc != null && player instanceof ServerPlayer serverPlayer) {
            return npc.hasMaxTrust(serverPlayer);
        }
        return trustPoints() >= TRUST_MAX_POINTS;
    }

    public int responseId() {
        return syncedData[DATA_RESPONSE];
    }

    public void handleNicknameUpdate(ServerPlayer serverPlayer, String nickname) {
        if (npc == null) {
            responseId = RESPONSE_BUSY_STATE;
        } else if (!identityUnlocked()) {
            responseId = RESPONSE_IDENTITY_LOCKED;
        } else if (!npc.applyNickname(serverPlayer, nickname)) {
            responseId = RESPONSE_IDENTITY_INVALID;
        } else {
            responseId = RESPONSE_IDENTITY_NICKNAME_OK;
        }
        broadcastChanges();
    }

    public void handleAppearanceUpdate(ServerPlayer serverPlayer, String appearanceId) {
        if (npc == null) {
            responseId = RESPONSE_BUSY_STATE;
        } else if (!identityUnlocked()) {
            responseId = RESPONSE_IDENTITY_LOCKED;
        } else if (!npc.applyAppearance(serverPlayer, appearanceId)) {
            responseId = RESPONSE_IDENTITY_INVALID;
        } else {
            responseId = RESPONSE_IDENTITY_APPEARANCE_OK;
        }
        broadcastChanges();
    }

    public void handleNameColorUpdate(ServerPlayer serverPlayer, String colorId) {
        if (npc == null) {
            responseId = RESPONSE_BUSY_STATE;
        } else if (!identityUnlocked()) {
            responseId = RESPONSE_IDENTITY_LOCKED;
        } else if (!npc.applyNameColor(serverPlayer, colorId)) {
            responseId = RESPONSE_IDENTITY_INVALID;
        } else {
            responseId = RESPONSE_IDENTITY_COLOR_OK;
        }
        broadcastChanges();
    }

    public int status() {
        return syncedData[DATA_STATUS];
    }

    public boolean purgeReadyClient() {
        return syncedData[DATA_PURGE_READY] == 1;
    }

    public int priceForOption(int optionId) {
        if (optionId >= OPTION_BUY_MAXDOC_MK2 && optionId <= OPTION_BUY_IMMUNOBLOCKERS) return shopPrice(optionId - OPTION_BUY_MAXDOC_MK2);
        return switch (optionId) {
            case OPTION_STABILIZE -> syncedData[DATA_STABILIZE_PRICE];
            case OPTION_PURGE -> syncedData[DATA_PURGE_PRICE];
            default -> 0;
        };
    }

    public int balance() {
        return syncedData[DATA_BALANCE];
    }

    public int trustDiscountPercent() {
        return npcType().supportsTrustDiscount() ? de.artemis.cyberneticenhancements.common.origin.Origins.discount(player, discountPercentForTrustPoints(trustPoints())) : 0;
    }

    public int unlockLevelForOption(int optionId) {
        return requiredTrust(npcType(), optionId);
    }

    public static int requiredTrust(NpcType type, int optionId) {
        if (optionId == OPTION_STABILIZE || optionId == OPTION_PURGE) return NpcService.forNpc(type).isEmpty() ? Integer.MAX_VALUE : NpcService.forOption(type, optionId).requiredTrust();
        int index = optionId - OPTION_BUY_MAXDOC_MK2;
        var offers = NpcShopCatalog.offers(type);
        return index < 0 || index >= offers.size() ? 0 : offers.get(index).requiredTrust();
    }

    public Component trustGateLabelForOption(int optionId) {
        return Component.translatable("screen.cyberneticenhancements.fixer.trust." + Math.max(0, Math.min(4, unlockLevelForOption(optionId))));
    }

    public int trustProgressPercent() {
        return trustProgressPercentForPoints(trustPoints());
    }

    public boolean isOptionAvailable(int optionId) {
        return (syncedData[DATA_OPTION_MASK] & (1 << optionId)) != 0;
    }

    public boolean hasActiveContract(int slot) {
        return activeValue(slot, ACTIVE_PRESENT) == 1;
    }

    public int activeContractCount() {
        int count = 0;
        for (int slot = 0; slot < ACTIVE_SLOTS; slot++) if (hasActiveContract(slot)) count++;
        return count;
    }

    public boolean activeContractReady(int slot) {
        return activeValue(slot, ACTIVE_READY) == 1;
    }

    public int activeContractTier(int slot) {
        return activeValue(slot, ACTIVE_TIER);
    }

    public int activeContractTargetIndex(int slot) {
        return activeValue(slot, ACTIVE_TARGET_INDEX);
    }

    public int activeContractTargetCount(int slot) {
        return activeValue(slot, ACTIVE_TARGET_COUNT);
    }

    public int activeContractProgress(int slot) {
        return activeValue(slot, ACTIVE_PROGRESS);
    }

    public QuestType activeContractType(int slot) {
        return questTypeFromOrdinal(activeValue(slot, ACTIVE_TYPE));
    }

    private QuestDimension activeRoute(int slot) {
        return QuestDimension.byIndex(activeValue(slot, ACTIVE_ROUTE));
    }

    private int activeValue(int slot, int field) {
        if (slot < 0 || slot >= ACTIVE_SLOTS) return 0;
        return syncedData[DATA_ACTIVE_BASE + slot * DATA_PER_ACTIVE + field];
    }

    public boolean offerPresent(int slot) {
        return offerValue(slot, 0) == 1;
    }

    public QuestType offerType(int slot) {
        return questTypeFromOrdinal(offerValue(slot, 1));
    }

    public int offerTier(int slot) {
        return offerValue(slot, 2);
    }

    public int offerTargetIndex(int slot) {
        return offerValue(slot, 3);
    }

    public int offerTargetCount(int slot) {
        return offerValue(slot, 4);
    }

    public int offerRewardMoney(int slot) {
        return offerValue(slot, 5);
    }

    public int offerRewardTrust(int slot) {
        return offerValue(slot, 6);
    }

    public int offerRewardItemKind(int slot) {
        return offerValue(slot, 7);
    }

    public int offerRewardItemCount(int slot) {
        return offerValue(slot, 8);
    }

    private int priceForServerOption(int optionId) {
        if (isDevTest()) return 0;
        boolean service = optionId == OPTION_STABILIZE || optionId == OPTION_PURGE;
        return priceForBase(basePriceForOption(optionId), service && !NpcService.forNpc(npcType()).isEmpty() ? NpcService.forOption(npcType(), optionId) : null);
    }

    private int priceForBase(int basePrice, NpcService service) {
        int trustDiscountPercent = npc == null || !npc.npcType().supportsTrustDiscount() ? 0
                : de.artemis.cyberneticenhancements.common.origin.Origins.discount(player, discountPercentForTrustPoints(npc.trustPoints(player)));
        float lifepath = npc == null ? 1.0F : de.artemis.cyberneticenhancements.common.origin.Origins.priceMultiplier(player, npc.npcType(), service)
                * de.artemis.cyberneticenhancements.common.streetcred.StreetCredEffects.priceMultiplier(player)
                * de.artemis.cyberneticenhancements.common.skills.SkillEffects.vendorMultiplier(player, npc.npcType(), service != null);
        int discountedPrice = Math.max(1, Math.round(basePrice * (100 - trustDiscountPercent) / 100.0F * lifepath));
        float priceMultiplier = npc == null ? 1.0F : npc.priceMultiplier(player);
        return Math.max(1, Math.round(discountedPrice * priceMultiplier));
    }

    private int basePriceForOption(int optionId) {
        if (optionId == OPTION_STABILIZE || optionId == OPTION_PURGE) return NpcService.forNpc(npcType()).isEmpty() ? 0 : NpcService.forOption(npcType(), optionId).basePrice();
        int index = optionId - OPTION_BUY_MAXDOC_MK2;
        var offers = shopOffers();
        return index < 0 || index >= offers.size() ? 0 : offers.get(index).basePrice();
    }

    public static int discountPercentForTrustPoints(int trustPoints) {
        return Math.min(25, (trustProgressPercentForPoints(trustPoints) + 3) / 4);
    }

    public static int trustProgressPercentForPoints(int trustPoints) {
        return Math.max(0, Math.min(100, Math.round(trustPoints * 100.0F / TRUST_MAX_POINTS)));
    }

    public Component activeContractTitle(int slot) {
        var title = activeContractType(slot) == QuestType.INVESTIGATION && activeContractTargetIndex(slot) == 1
                ? Component.translatable("quest.cyberneticenhancements.title.chromium_survey") : questTitleFor(activeContractType(slot));
        return activeRoute(slot).title(title);
    }

    public Component activeContractObjective(int slot) {
        return objectiveFor(activeContractType(slot), activeContractTargetIndex(slot), activeContractProgress(slot), activeContractTargetCount(slot),
                activeContractReady(slot), activeRoute(slot));
    }

    public Component activeContractRewardSummary(int slot) {
        return rewardSummary(activeValue(slot, ACTIVE_REWARD_MONEY), activeValue(slot, ACTIVE_REWARD_TRUST),
                activeValue(slot, ACTIVE_REWARD_ITEM_KIND), activeValue(slot, ACTIVE_REWARD_ITEM_COUNT)).copy()
                .append(Component.translatable("quest.cyberneticenhancements.reward.xp", activeRoute(slot)
                        .reward(de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.quest(activeContractType(slot), activeContractTier(slot)))))
                .append(activeContractType(slot) == QuestType.INVESTIGATION && activeContractTargetIndex(slot) == 1
                        ? Component.translatable("quest.cyberneticenhancements.reward.chromium_marker") : Component.empty());
    }

    public Component activeContractLocationSummary(int slot) {
        if (activeValue(slot, ACTIVE_HAS_LOCATION) != 1) {
            return Component.empty();
        }
        return Component.translatable(
                "screen.cyberneticenhancements.fixer.contract.location",
                activeValue(slot, ACTIVE_LOCATION_X),
                activeValue(slot, ACTIVE_LOCATION_Y),
                activeValue(slot, ACTIVE_LOCATION_Z)
        );
    }

    public Component offerTitle(int slot) {
        var title = offerType(slot) == QuestType.INVESTIGATION && offerTargetIndex(slot) == 1
                ? Component.translatable("quest.cyberneticenhancements.title.chromium_survey") : questTitleFor(offerType(slot));
        return offerRoute(slot).title(title);
    }

    private de.artemis.cyberneticenhancements.common.quest.QuestDimension offerRoute(int slot) {
        return de.artemis.cyberneticenhancements.common.quest.QuestDimension.byIndex(syncedData[DATA_OFFER_BASE + slot * DATA_PER_OFFER + 9]);
    }

    public Component contractBoardTitle() {
        return Component.translatable("screen.cyberneticenhancements.npc.contract." + npcTypeId() + ".board");
    }

    public Component contractBoardNote() {
        return Component.translatable("screen.cyberneticenhancements.npc.contract." + npcTypeId() + ".board_note");
    }

    private Component questTitleFor(QuestType type) {
        return Component.translatable("quest.cyberneticenhancements.title." + npcTypeId() + "." + type.id());
    }

    public Component offerSummary(int slot) {
        return objectiveFor(offerType(slot), offerTargetIndex(slot), 0, offerTargetCount(slot), false, offerRoute(slot));
    }

    public Component offerRewardSummary(int slot) {
        return rewardSummary(offerRewardMoney(slot), offerRewardTrust(slot), offerRewardItemKind(slot), offerRewardItemCount(slot)).copy()
                .append(Component.translatable("quest.cyberneticenhancements.reward.xp", offerRoute(slot)
                        .reward(de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.quest(offerType(slot), offerTier(slot)))))
                .append(offerType(slot) == QuestType.INVESTIGATION && offerTargetIndex(slot) == 1
                        ? Component.translatable("quest.cyberneticenhancements.reward.chromium_marker") : Component.empty());
    }

    private Component objectiveFor(QuestType type, int targetIndex, int progress, int targetCount, boolean ready, QuestDimension route) {
        if (ready) {
            return Component.translatable(type.returnsItems() ? "quest.cyberneticenhancements.objective.return.in_person"
                    : type.requiresInPersonTurnIn() ? "quest.cyberneticenhancements.objective.report.in_person" : "quest.cyberneticenhancements.objective.return", npc == null ? "contact" : npc.getName());
        }
        return switch (type) {
            case DELIVERY -> Component.translatable("quest.cyberneticenhancements.objective.delivery.stash");
            case EXTRACTION -> Component.translatable("quest.cyberneticenhancements.objective.extraction", targetLabel(type, targetIndex));
            case INVESTIGATION -> Component.translatable(targetIndex == 1 ? "quest.cyberneticenhancements.objective.chromium_survey" : "quest.cyberneticenhancements.objective.investigation", progress, targetCount);
            case BREACH -> Component.translatable("quest.cyberneticenhancements.objective.breach", targetLabel(type, targetIndex));
            case ELIMINATION -> Component.translatable("quest.cyberneticenhancements.objective.elimination", progress, targetCount, targetLabel(type, targetIndex));
            case RECOVERY -> route == QuestDimension.STANDARD
                    ? Component.translatable("quest.cyberneticenhancements.objective.recovery", progress, targetCount, targetLabel(type, targetIndex))
                    : Component.translatable("quest.cyberneticenhancements.objective.recovery_dimension", targetCount, targetLabel(type, targetIndex), route.displayName());
            case CYBERPSYCHO -> Component.translatable("quest.cyberneticenhancements.objective.cyberpsycho.offer");
            case LOCATE -> Component.translatable("quest.cyberneticenhancements.objective.locate");
            case SALVAGE -> Component.translatable("quest.cyberneticenhancements.objective.salvage");
        };
    }

    private Component targetLabel(QuestType type, int targetIndex) {
        return switch (type) {
            case DELIVERY -> Component.translatable("quest.cyberneticenhancements.target.hidden_stash");
            case EXTRACTION -> Component.translatable("quest.cyberneticenhancements.target.exfil_shard");
            case INVESTIGATION -> Component.translatable("quest.cyberneticenhancements.target.signal_site");
            case BREACH -> Component.translatable("quest.cyberneticenhancements.target.breach_cache");
            case ELIMINATION -> QuestContract.safeEliminationTarget(targetIndex).getDescription();
            case RECOVERY -> QuestContract.safeRecoveryItem(targetIndex).getDefaultInstance().getHoverName();
            case CYBERPSYCHO -> Component.translatable("entity.cyberneticenhancements.cyberpsycho");
            case LOCATE -> Component.translatable("quest.cyberneticenhancements.target.patient");
            case SALVAGE -> Component.translatable("quest.cyberneticenhancements.target.cargo");
        };
    }

    /** One thing a contract pays out, with the item that stands for it. */
    public record RewardLine(ItemStack icon, Component text) {}

    public java.util.List<RewardLine> activeContractRewards(int slot) {
        return rewardLines(activeValue(slot, ACTIVE_REWARD_MONEY), activeValue(slot, ACTIVE_REWARD_TRUST),
                activeValue(slot, ACTIVE_REWARD_ITEM_KIND), activeValue(slot, ACTIVE_REWARD_ITEM_COUNT),
                activeRoute(slot).reward(de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.quest(activeContractType(slot), activeContractTier(slot))),
                activeContractType(slot) == QuestType.INVESTIGATION && activeContractTargetIndex(slot) == 1);
    }

    public java.util.List<RewardLine> offerRewards(int slot) {
        return rewardLines(offerRewardMoney(slot), offerRewardTrust(slot), offerRewardItemKind(slot), offerRewardItemCount(slot),
                offerRoute(slot).reward(de.artemis.cyberneticenhancements.common.progression.ExperienceRewards.quest(offerType(slot), offerTier(slot))),
                offerType(slot) == QuestType.INVESTIGATION && offerTargetIndex(slot) == 1);
    }

    /** One line per kind of reward: the money, the trust, an item if there is one, the experience, and the compass marker of a survey. */
    private java.util.List<RewardLine> rewardLines(int money, int trust, int rewardItemKind, int rewardItemCount, int experience, boolean marker) {
        var lines = new java.util.ArrayList<RewardLine>();
        if (money > 0) lines.add(new RewardLine(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT), Component.translatable("quest.cyberneticenhancements.reward.line.money", money)));
        if (trust > 0) lines.add(new RewardLine(new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD), Component.translatable("quest.cyberneticenhancements.reward.line.trust", trust)));
        ItemStack rewardStack = switch (rewardItemKind) {
            case QuestContract.REWARD_CHROMIUM -> new ItemStack(ModItems.CHROMIUM_ALLOY.get(), rewardItemCount);
            case QuestContract.REWARD_MAXDOC -> new ItemStack(ModItems.consumable("maxdoc_mk2").get(), rewardItemCount);
            case QuestContract.REWARD_RAM_JOLT -> new ItemStack(ModItems.consumable("ram_jolt").get(), rewardItemCount);
            case QuestContract.REWARD_CHROME_SUPPRESSANT -> new ItemStack(ModItems.consumable("chrome_suppressant").get(), rewardItemCount);
            default -> ItemStack.EMPTY;
        };
        if (!rewardStack.isEmpty()) lines.add(new RewardLine(rewardStack.copyWithCount(1),
                Component.translatable("quest.cyberneticenhancements.reward.line.item", rewardStack.getCount(), rewardStack.getHoverName())));
        if (experience > 0) lines.add(new RewardLine(new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE), Component.translatable("quest.cyberneticenhancements.reward.line.xp", experience)));
        if (marker) lines.add(new RewardLine(new ItemStack(net.minecraft.world.item.Items.COMPASS), Component.translatable("quest.cyberneticenhancements.reward.line.chromium_marker")));
        return lines;
    }

    private Component rewardSummary(int money, int trust, int rewardItemKind, int rewardItemCount) {
        ItemStack rewardStack = switch (rewardItemKind) {
            case QuestContract.REWARD_CHROMIUM -> new ItemStack(ModItems.CHROMIUM_ALLOY.get(), rewardItemCount);
            case QuestContract.REWARD_MAXDOC -> new ItemStack(ModItems.consumable("maxdoc_mk2").get(), rewardItemCount);
            case QuestContract.REWARD_RAM_JOLT -> new ItemStack(ModItems.consumable("ram_jolt").get(), rewardItemCount);
            case QuestContract.REWARD_CHROME_SUPPRESSANT -> new ItemStack(ModItems.consumable("chrome_suppressant").get(), rewardItemCount);
            default -> ItemStack.EMPTY;
        };
        return rewardStack.isEmpty()
                ? Component.translatable("quest.cyberneticenhancements.reward.bundle", money, trust)
                : Component.translatable("quest.cyberneticenhancements.reward.bundle.item", money, trust, rewardStack.getHoverName());
    }

    private boolean isServiceUnlocked(int optionId) {
        return npc != null && npc.trustLevel(player) >= unlockLevelForOption(optionId);
    }

    private int trustGainForOption(int optionId) {
        return switch (optionId) {
            case OPTION_STABILIZE -> switch (npcType()) {
                case FIXER, TECHIE, NETRUNNER, MERC, MED_TECH, WORLD_MED_TECH, MAXTEC -> 1;
                case RIPPERDOC, STREET_RIPPERDOC -> 2;
            };
            case OPTION_PURGE -> 2;
            case OPTION_BUY_IMMUNOBLOCKERS -> 2;
            default -> 1;
        };
    }

    private int offerValue(int slot, int offset) {
        if (slot < 0 || slot >= OFFER_SLOTS) {
            return 0;
        }
        return syncedData[DATA_OFFER_BASE + slot * DATA_PER_OFFER + offset];
    }

    private static QuestType questTypeFromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= QuestType.values().length) {
            return QuestType.DELIVERY;
        }
        return QuestType.values()[ordinal];
    }

    @Override
    public boolean stillValid(Player player) {
        return npc != null
                && npc.canInteract(player)
                && npc.isAlive()
                && npc.distanceToSqr(player) <= 64.0D
                && !npc.isRefusing(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (npc != null) {
            npc.clearCustomer(player);
        }
        if (player instanceof ServerPlayer serverPlayer) de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.resume(serverPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
