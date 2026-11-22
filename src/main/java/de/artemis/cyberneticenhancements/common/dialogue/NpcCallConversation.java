package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.network.ArchiveQuestsPayload;
import de.artemis.cyberneticenhancements.common.network.NpcCallStatePayload.Choice;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import de.artemis.cyberneticenhancements.common.quest.QuestType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.artemis.cyberneticenhancements.common.network.NpcCallRequestPayload.*;

final class NpcCallConversation {
    private static final String PREFIX = "screen.cyberneticenhancements.call.";
    private final AbstractCityNpcEntity npc;
    private final String turnInQuestId;
    private List<Choice> choices = List.of();
    private List<QuestContract> offers = List.of();
    private String selectedOfferId = "";
    private String reportQuestId = "";
    /** Which of this contact's active contracts the conversation is about, when there are several. */
    private String focusedQuestId = "";
    private int offerOffset;
    private boolean questions, ended;
    private Component line = text("ringing");
    private NpcCallReason incomingReason;
    private String reminderQuestId = "";
    private int requestedMoney;
    private String urgentOfferId = "";
    private String lostItemQuestId = "";
    private String failedCacheQuestId = "";
    private Component failedCacheTitle = Component.empty();
    private boolean failedPatient;

    NpcCallConversation(AbstractCityNpcEntity npc) { this(npc, ""); }

    NpcCallConversation(AbstractCityNpcEntity npc, String turnInQuestId) {
        this.npc = npc;
        this.turnInQuestId = turnInQuestId;
        this.focusedQuestId = turnInQuestId;
    }

    Component line() { return line; }
    List<Choice> choices() { return choices; }
    boolean ended() { return ended; }

    void open(ServerPlayer player) {
        var quest = active(player);
        if (!turnInQuestId.isEmpty()) {
            if (canReport(quest, turnInQuestId)) showActive(player, QUEST_STATUS, quest);
            else finish(text("quest.report_unavailable"));
            return;
        }
        if (lossQuest(player) != null) { showItemLoss(player); return; }
        if (prepareCacheFailure(player)) { showCacheFailure(); return; }
        line = NpcCallDialogue.greeting(npc, player);
        rootChoices(player);
    }

    boolean prepareIncoming(ServerPlayer player, NpcCallReason reason) {
        incomingReason = reason;
        if (reason == NpcCallReason.CACHE_FAILED && !prepareCacheFailure(player)) return false;
        if (reason == NpcCallReason.LOST_ITEM) {
            var quest = lossQuest(player);
            if (quest == null) return false;
            lostItemQuestId = quest.id();
        }
        if (reason == NpcCallReason.URGENT_QUEST) {
            if (!PlayerQuestManager.canReceiveUrgentOffer(player, npc)) return false;
            var available = PlayerQuestManager.snapshotForIssuer(player, npc).offers();
            urgentOfferId = available.get(player.getRandom().nextInt(available.size())).id();
        }
        requestedMoney = Math.clamp(de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.balance(player) / 20,
                de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("support.minimum"),
                de.artemis.cyberneticenhancements.common.economy.EconomyBalance.value("support.maximum"));
        if (reason == NpcCallReason.OVERDUE) reminderQuestId = focusedQuestId = Objects.requireNonNull(active(player)).id();
        return true;
    }

    void openIncoming(ServerPlayer player) {
        if (incomingReason == NpcCallReason.CACHE_FAILED) {
            if (prepareCacheFailure(player)) showCacheFailure();
            else { line = text("incoming.quest_changed"); rootChoices(player); }
            return;
        }
        if (incomingReason == NpcCallReason.LOST_ITEM) { showItemLoss(player); return; }
        rootChoices(player);
        if (incomingReason == NpcCallReason.URGENT_QUEST) {
            var board = PlayerQuestManager.snapshotForIssuer(player, npc);
            var offer = board.actives().size() < PlayerQuestManager.MAX_ACTIVE_PER_ISSUER ? board.offers().stream().filter(value -> value.id().equals(urgentOfferId)).findFirst().orElse(null) : null;
            if (offer == null) {
                urgentOfferId = "";
                line = text("quest.offer_gone");
                return;
            }
            var preview = QuestContract.fromTag(offer.toTag());
            preview.markUrgent();
            line = NpcCallDialogue.variant(npc, "incoming.urgent." + npc.npcTypeId(), preview.title(), preview.objectiveLine(), preview.rewardSummary())
                    .copy().append(text("incoming.urgent_deadline", preview.urgentLimitMinutes()));
            choices = List.of(choice(ACCEPT_URGENT, "accept_urgent"), choice(DECLINE_URGENT, "decline_urgent"), choice(END, "end"));
        } else if (incomingReason == NpcCallReason.MONEY) {
            line = NpcCallDialogue.variant(npc, "incoming.money." + npc.npcTypeId(), requestedMoney);
            choices = List.of(new Choice(SEND_MONEY, text("choice.send_money", requestedMoney)), choice(DECLINE_MONEY, "decline_money"),
                    choice(BACK, "change_subject"), choice(END, "end"));
        } else if (incomingReason == NpcCallReason.OVERDUE) {
            var quest = active(player);
            if (quest == null || !quest.id().equals(reminderQuestId)) {
                line = text("incoming.quest_changed");
            } else if (quest.isReady()) {
                showActive(player, QUEST_STATUS);
            } else {
                line = NpcCallDialogue.variant(npc, "incoming.overdue", quest.title());
                choices = List.of(choice(QUEST_HELP, "quest_help"), choice(QUEST_LOCATION, "quest_location"),
                        choice(ACKNOWLEDGE_REMINDER, "working_on_it"), choice(BACK, "change_subject"), choice(END, "end"));
            }
        } else {
            line = NpcCallDialogue.variant(npc, "incoming.checkin");
        }
    }

    void respond(ServerPlayer player, int action) {
        switch (action) {
            case CONFIRM_ITEM_KEPT -> {
                boolean confirmed = PlayerQuestManager.confirmLostReturnItem(player, npc, lostItemQuestId);
                lostItemQuestId = "";
                line = text(confirmed ? "incoming.item_still_owned" : "incoming.quest_changed");
                rootChoices(player);
                sync(player);
            }
            case ADMIT_ITEM_LOST -> {
                PlayerQuestManager.failLostReturnItem(player, npc, lostItemQuestId);
                lostItemQuestId = "";
                line = text("incoming.item_lost_ended");
                ended = true;
                choices = List.of();
                sync(player);
            }
            case REQUEST_REPLACEMENT -> {
                boolean replaced = PlayerQuestManager.requestDeliveryReplacement(player, npc);
                line = text(replaced ? "quest.replacement_issued" : "quest.replacement_unavailable");
                rootChoices(player);
                sync(player);
            }
            case LOCATE -> {
                String position = npc.getBlockX() + ", " + npc.getBlockY() + ", " + npc.getBlockZ();
                boolean marked = AbstractCityNpcEntity.locateFromArchive(player, npc.getUUID());
                line = marked ? NpcCallDialogue.variant(npc, "location", position)
                        : text("other_dimension", position, npc.level().dimension().location().toString());
                rootChoices(player);
            }
            case MEET -> {
                boolean meeting = AbstractCityNpcEntity.summonFromArchive(player, npc.getUUID());
                line = meeting ? NpcCallDialogue.meeting(npc, player) : NpcCallDialogue.variant(npc, "cannot_meet");
                ended = meeting;
                rootChoices(player);
            }
            case WORK -> {
                if (npc.npcType().supportsContracts()) openContracts(player, false);
                else services();
            }
            case ADVICE -> {
                if (questions) {
                    line = NpcCallDialogue.variant(npc, "advice." + npc.npcTypeId());
                    questionChoices();
                } else if (npc.npcType().supportsContracts() && active(player) != null) {
                    openActives(player);
                } else {
                    line = NpcCallDialogue.variant(npc, "questions");
                    questionChoices();
                }
            }
            case BACK -> {
                line = NpcCallDialogue.variant(npc, "back");
                rootChoices(player);
            }
            case SMALL_TALK -> {
                line = NpcCallDialogue.smallTalk(npc, player);
                questionChoices();
            }
            case SERVICES -> services();
            case MORE_CONTRACTS -> openContracts(player, true);
            case CONTRACT_FIRST, CONTRACT_FIRST + 1, CONTRACT_FIRST + 2 -> {
                var actives = PlayerQuestManager.snapshotForIssuer(player, npc).actives();
                int index = action - CONTRACT_FIRST;
                if (index < actives.size()) focusedQuestId = actives.get(index).id();
                showActive(player, QUEST_STATUS);
            }
            case OFFER_FIRST, OFFER_SECOND -> showOffer(player, offerOffset + action - OFFER_FIRST);
            case ACCEPT_CONTRACT -> accept(player);
            case ACCEPT_URGENT -> acceptUrgent(player);
            case DECLINE_URGENT -> {
                boolean declined = !urgentOfferId.isEmpty() && PlayerQuestManager.declineUrgentIssuerOffer(player, npc, urgentOfferId);
                urgentOfferId = "";
                line = declined ? NpcCallDialogue.variant(npc, "incoming.urgent_declined") : text("quest.offer_gone");
                if (declined) notifyPlayer(player, text("incoming.urgent_declined_chat", npc.getName()).copy().withStyle(ChatFormatting.RED),
                        de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.CANCELLED);
                if (declined) finish(line);
                else rootChoices(player);
                sync(player);
            }
            case QUEST_STATUS, QUEST_HELP, QUEST_LOCATION -> showActive(player, action);
            case ABANDON_CONTRACT -> {
                var contract = active(player);
                if (contract == null) showActive(player, QUEST_STATUS);
                else {
                    questions = false;
                    line = text("quest.abandon_confirm", contract.title());
                    choices = List.of(choice(CONFIRM_ABANDON, "confirm_abandon"), choice(QUEST_STATUS, "keep_contract"), choice(END, "end"));
                }
            }
            case CONFIRM_ABANDON -> {
                var contract = active(player);
                var result = contract == null ? PlayerQuestManager.Result.INVALID : PlayerQuestManager.abandonIssuerActive(player, npc, contract.id());
                if (result == PlayerQuestManager.Result.OK) {
                    line = NpcCallDialogue.variant(npc, "quest.abandoned", contract.title());
                    rootChoices(player);
                    sync(player);
                } else showActive(player, QUEST_STATUS);
            }
            case REPORT_CONTRACT -> report(player);
            case ACKNOWLEDGE_REMINDER -> {
                if (!failedCacheQuestId.isEmpty()) {
                    de.artemis.cyberneticenhancements.common.quest.QuestFailureCalls.acknowledge(player, failedCacheQuestId);
                    failedCacheQuestId = "";
                    line = text(failedPatient ? "incoming.patient_lost_acknowledged" : "incoming.cache_failed_acknowledged");
                    // A call about a failed contract ends with the answer: there is nothing left to talk about, and for a patient the contact is gone too.
                    finish(line);
                    break;
                } else line = NpcCallDialogue.variant(npc, "incoming.acknowledged");
                rootChoices(player);
            }
            case DECLINE_MONEY -> {
                requestedMoney = 0;
                finish(NpcCallDialogue.variant(npc, "incoming.money_declined"));
            }
            case SEND_MONEY -> {
                boolean paid = requestedMoney > 0 && de.artemis.cyberneticenhancements.common.economy.PlayerEurodollarManager.tryWithdraw(player,
                        requestedMoney, "contact_support", "Helped a contact", npc.getUUID(), npc.getName().getString());
                if (paid) {
                    npc.rewardCommerceTrust(player, 2);
                    notifyPlayer(player, text("incoming.money_sent_chat", requestedMoney, npc.getName()),
                            de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.TRANSFER);
                    finish(NpcCallDialogue.variant(npc, "incoming.money_thanks"));
                } else {
                    line = text("incoming.money_insufficient");
                    de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(player);
                }
                requestedMoney = 0;
                if (!paid) rootChoices(player);
            }
            default -> throw new IllegalArgumentException("Unsupported call action");
        }
    }

    private void rootChoices(ServerPlayer player) {
        questions = false;
        boolean contracts = npc.npcType().supportsContracts();
        var all = new java.util.ArrayList<>(List.of(choice(LOCATE, "locate"), choice(MEET, "meet"), choice(WORK, contracts ? "contracts" : "work"),
                PlayerQuestManager.canRequestDeliveryReplacement(player, npc) ? choice(REQUEST_REPLACEMENT, "replacement")
                        : choice(ADVICE, contracts && active(player) != null ? "active_contract" : "questions"), choice(END, "end")));
        if (npc instanceof de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity) all.remove(1); // A quest contact does not come to meet you.
        choices = List.copyOf(all);
    }

    private void showItemLoss(ServerPlayer player) {
        if (!lostItemQuestId.isEmpty()) focusedQuestId = lostItemQuestId;
        var quest = lostItemQuestId.isEmpty() ? lossQuest(player) : active(player);
        if (quest == null || !quest.itemLossPending() || !lostItemQuestId.isEmpty() && !quest.id().equals(lostItemQuestId)) {
            line = text("incoming.quest_changed");
            rootChoices(player);
            return;
        }
        lostItemQuestId = quest.id();
        line = text("incoming.item_lost", quest.title()).copy().withStyle(ChatFormatting.RED);
        choices = List.of(choice(CONFIRM_ITEM_KEPT, "still_have_item"), choice(ADMIT_ITEM_LOST, "lost_item"), choice(END, "end"));
    }

    void closed(ServerPlayer player) {
        if (!failedCacheQuestId.isEmpty()) de.artemis.cyberneticenhancements.common.quest.QuestFailureCalls.acknowledge(player, failedCacheQuestId);
        if (!lostItemQuestId.isEmpty() && npc.isAlive()
                && !de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData.get(player.serverLevel()).isDeceased(npc.getUUID()))
            PlayerQuestManager.failLostReturnItem(player, npc, lostItemQuestId);
    }

    void idleClosed(ServerPlayer player) {
        if (!failedCacheQuestId.isEmpty()) de.artemis.cyberneticenhancements.common.quest.QuestFailureCalls.acknowledge(player, failedCacheQuestId);
        if (!lostItemQuestId.isEmpty()) PlayerQuestManager.deferLostReturnItemCall(player, npc, lostItemQuestId);
    }

    private boolean prepareCacheFailure(ServerPlayer player) {
        var failed = de.artemis.cyberneticenhancements.common.quest.QuestFailureCalls.pending(player, npc.getUUID());
        if (failed == null) return false;
        failedCacheQuestId = failed.id();
        failedCacheTitle = failed.title();
        failedPatient = failed.type() == de.artemis.cyberneticenhancements.common.quest.QuestType.LOCATE;
        return true;
    }

    private void showCacheFailure() {
        line = text(failedPatient ? "incoming.patient_lost" : "incoming.cache_failed", failedCacheTitle).copy().withStyle(ChatFormatting.RED);
        choices = List.of(choice(ACKNOWLEDGE_REMINDER, "cache_failed_acknowledge"), choice(END, "end"));
    }

    private void questionChoices() {
        questions = true;
        choices = List.of(choice(SMALL_TALK, "small_talk"), choice(SERVICES, "services"), choice(ADVICE, "advice"),
                choice(BACK, "back"), choice(END, "end"));
    }

    private void services() {
        questions = false;
        line = NpcCallDialogue.variant(npc, "services." + npc.npcTypeId());
        choices = List.of(choice(BACK, "back"), choice(END, "end"));
    }

    /** The contract the conversation is about: the chosen one, or the first when none was chosen (or it is gone). */
    private QuestContract active(ServerPlayer player) {
        var actives = PlayerQuestManager.snapshotForIssuer(player, npc).actives();
        return actives.stream().filter(quest -> quest.id().equals(focusedQuestId)).findFirst().orElse(actives.isEmpty() ? null : actives.get(0));
    }

    private QuestContract lossQuest(ServerPlayer player) {
        return PlayerQuestManager.snapshotForIssuer(player, npc).actives().stream().filter(QuestContract::itemLossPending).findFirst().orElse(null);
    }

    /** With several active contracts the player says which one they mean; with one it goes straight to it. */
    private void openActives(ServerPlayer player) {
        var actives = PlayerQuestManager.snapshotForIssuer(player, npc).actives();
        if (actives.size() < 2) {
            focusedQuestId = actives.isEmpty() ? "" : actives.get(0).id();
            showActive(player, QUEST_STATUS);
            return;
        }
        questions = false;
        line = text("quest.which");
        var options = new ArrayList<Choice>();
        for (int index = 0; index < actives.size(); index++) options.add(new Choice(CONTRACT_FIRST + index, text("choice.contract", actives.get(index).title())));
        options.add(choice(BACK, "back"));
        options.add(choice(END, "end"));
        choices = List.copyOf(options);
    }

    private void openContracts(ServerPlayer player, boolean nextPage) {
        questions = false;
        var board = PlayerQuestManager.snapshotForIssuer(player, npc);
        if (board.actives().size() >= PlayerQuestManager.MAX_ACTIVE_PER_ISSUER || !board.actives().isEmpty() && board.offers().isEmpty()) {
            openActives(player);
            return;
        }
        offers = board.offers();
        selectedOfferId = "";
        if (offers.isEmpty()) {
            line = NpcCallDialogue.variant(npc, "no_contracts");
            choices = List.of(choice(BACK, "back"), choice(END, "end"));
            return;
        }
        offerOffset = nextPage && offerOffset + 2 < offers.size() ? offerOffset + 2 : 0;
        line = NpcCallDialogue.variant(npc, "contracts");
        var options = new ArrayList<Choice>();
        for (int index = offerOffset; index < Math.min(offers.size(), offerOffset + 2); index++) {
            options.add(new Choice(OFFER_FIRST + index - offerOffset, text("choice.offer", offers.get(index).title())));
        }
        if (offers.size() > 2) options.add(choice(MORE_CONTRACTS, "more_contracts"));
        options.add(choice(BACK, "back"));
        options.add(choice(END, "end"));
        choices = List.copyOf(options);
    }

    private void showOffer(ServerPlayer player, int index) {
        var shown = offers.get(index);
        var board = PlayerQuestManager.snapshotForIssuer(player, npc);
        if (board.actives().size() >= PlayerQuestManager.MAX_ACTIVE_PER_ISSUER) {
            openActives(player);
            return;
        }
        var offer = board.offers().stream().filter(value -> value.id().equals(shown.id())).findFirst().orElse(null);
        if (offer == null) {
            openContracts(player, false);
            line = text("quest.offer_gone");
            return;
        }
        selectedOfferId = offer.id();
        line = text("quest.briefing", offer.title(), offer.objectiveLine(), offer.rewardSummary());
        choices = List.of(choice(ACCEPT_CONTRACT, "accept_contract"), choice(WORK, "other_contracts"), choice(BACK, "back"), choice(END, "end"));
    }

    private void accept(ServerPlayer player) {
        var board = PlayerQuestManager.snapshotForIssuer(player, npc);
        int index = -1;
        for (int slot = 0; slot < board.offers().size(); slot++) {
            if (board.offers().get(slot).id().equals(selectedOfferId)) { index = slot; break; }
        }
        var result = index < 0 ? PlayerQuestManager.Result.INVALID : PlayerQuestManager.acceptIssuerOffer(player, npc, index);
        if (result == PlayerQuestManager.Result.OK) {
            focusedQuestId = selectedOfferId;
            var contract = Objects.requireNonNull(active(player));
            showActive(player, QUEST_STATUS);
            line = text("quest.accepted", contract.title(), contract.objectiveLine());
            sync(player);
        } else if (result == PlayerQuestManager.Result.HAS_ACTIVE) {
            openActives(player);
        } else {
            openContracts(player, false);
            line = text("quest.offer_gone");
        }
    }

    private void acceptUrgent(ServerPlayer player) {
        var result = urgentOfferId.isEmpty() ? PlayerQuestManager.Result.INVALID
                : PlayerQuestManager.acceptUrgentIssuerOffer(player, npc, urgentOfferId);
        if (result == PlayerQuestManager.Result.OK) {
            focusedQuestId = urgentOfferId;
            urgentOfferId = "";
            var contract = Objects.requireNonNull(active(player));
            finish(text("incoming.urgent_accepted", contract.title(), contract.objectiveLine(), contract.rewardSummary())
                    .copy().append(text("incoming.urgent_deadline", contract.urgentLimitMinutes())));
            sync(player);
        } else {
            urgentOfferId = "";
            line = text("quest.offer_gone");
            rootChoices(player);
        }
    }

    private void showActive(ServerPlayer player, int action) {
        showActive(player, action, active(player));
    }

    private void showActive(ServerPlayer player, int action, QuestContract contract) {
        questions = false;
        reportQuestId = "";
        if (contract != null && contract.itemLossPending()) { showItemLoss(player); return; }
        if (contract == null) {
            line = text("quest.no_active");
            choices = List.of(choice(WORK, "contracts"), choice(BACK, "back"), choice(END, "end"));
            return;
        }
        if (action == QUEST_HELP) {
            line = contract.isReady() ? readyInstructions(contract)
                    : NpcCallDialogue.variant(npc, "quest.help." + contract.type().id(), contract.targetCount(), contract.targetLabel());
        } else if (action == QUEST_LOCATION) {
            if (contract.isReady()) {
                line = readyInstructions(contract);
            } else line = contract.targetPos() == null ? text("quest.no_site", contract.targetCount(), contract.targetLabel())
                    : text("quest.location", contract.targetPos().getX(), contract.targetPos().getY(), contract.targetPos().getZ(),
                            contract.dimensionId(), contract.type() == QuestType.INVESTIGATION ? text("quest.location_investigation") : text("quest.location_navigation"));
        } else {
            line = text("quest.status", contract.title(), contract.objectiveLine(),
                    contract.isReady() ? readyInstructions(contract) : text("quest.in_progress"));
        }
        boolean remoteReady = contract.isReady() && !contract.type().requiresInPersonTurnIn();
        if (remoteReady) reportQuestId = contract.id();
        choices = List.of(choice(remoteReady ? REPORT_CONTRACT : QUEST_STATUS,
                        remoteReady ? "report_contract" : "quest_status"),
                choice(ABANDON_CONTRACT, "abandon_contract"), PlayerQuestManager.canRequestDeliveryReplacement(player, npc)
                        ? choice(REQUEST_REPLACEMENT, "replacement") : choice(QUEST_LOCATION, "quest_location"), choice(BACK, "back"), choice(END, "end"));
    }

    private void report(ServerPlayer player) {
        var contract = active(player);
        if (!canReport(contract, reportQuestId)) {
            showActive(player, QUEST_STATUS);
            return;
        }
        if (PlayerQuestManager.turnInRemotely(player, contract.id()) != PlayerQuestManager.TurnInResult.OK) {
            de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback.DENIED.play(player);
            showActive(player, QUEST_STATUS);
            line = text("quest.report_unavailable");
            return;
        }
        finish(NpcCallDialogue.variant(npc, "quest.paid", contract.rewardSummary()));
        sync(player);
    }

    private static boolean canReport(QuestContract contract, String questId) {
        return contract != null && contract.id().equals(questId) && contract.isReady()
                && !contract.itemLossPending() && !contract.type().requiresInPersonTurnIn();
    }

    private void finish(Component finalLine) {
        line = finalLine;
        choices = List.of();
        ended = true;
    }

    private Component readyInstructions(QuestContract contract) {
        return contract.type().returnsItems() ? text("quest.ready_in_person") : contract.type().requiresInPersonTurnIn() ? text("quest.ready_report") : text("quest.ready_remote");
    }

    private static void sync(ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, ArchiveQuestsPayload.capture(player));
    }

    private static void notifyPlayer(ServerPlayer player, Component message, de.artemis.cyberneticenhancements.common.feedback.PlayerActionFeedback action) {
        var feedback = message.copy();
        if (feedback.getStyle().getColor() == null) feedback.withStyle(ChatFormatting.AQUA);
        de.artemis.cyberneticenhancements.common.feedback.GameplayMessages.chat(player, feedback, action);
    }

    private static Component text(String key, Object... arguments) { return Component.translatable(PREFIX + key, arguments); }
    private static Choice choice(int action, String key) { return new Choice(action, text("choice." + key)); }
}
