package de.artemis.cyberneticenhancements.common.dialogue;

import de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.Node;
import de.artemis.cyberneticenhancements.common.dialogue.FaceDialogue.Option;
import de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity;
import de.artemis.cyberneticenhancements.common.quest.PlayerQuestManager;
import de.artemis.cyberneticenhancements.common.quest.QuestContract;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;

/**
 * What the leader of a world-encounter site says when you look at them: their contracts, as a spoken exchange instead of a
 * window. It uses the same board, the same wording and the same rules as a call, so nothing about the work itself differs.
 */
public final class LeaderDialogue {
    private static final String PREFIX = "screen.cyberneticenhancements.call.";
    private static final int MAX_OFFERS = 3;

    private final WorldEncounterNpcEntity npc;

    public LeaderDialogue(WorldEncounterNpcEntity npc) { this.npc = npc; }

    private static Component text(String key, Object... arguments) { return Component.translatable(PREFIX + key, arguments); }

    private Component speaker() { return npc.getName(); }

    private Node say(Component line) { return Node.say(speaker(), line); }

    private Node node(Component line, List<Option> options) { return new Node(speaker(), line, options); }

    private Option leave() { return Option.of(Component.translatable("dialogue.cyberneticenhancements.leave"), player -> null); }

    public Node greet(ServerPlayer player) {
        if (PlayerQuestManager.withholdsOffers(player, npc)) return say(Component.translatable("dialogue.cyberneticenhancements.leader.refuses"));
        return root(player, NpcCallDialogue.greeting(npc, player));
    }

    private Node root(ServerPlayer player, Component line) {
        var board = PlayerQuestManager.snapshotForIssuer(player, npc);
        var options = new ArrayList<Option>();
        var actives = board.actives();
        if (actives.size() == 1) {
            addContractOptions(options, actives.get(0));
        } else {
            // Several jobs with the same contact: each one gets a line of its own, which opens what can be done with it.
            for (var active : actives) options.add(Option.of(text("choice.contract", active.title()), p -> contract(p, active.id())));
        }
        if (actives.isEmpty() && board.offers().isEmpty()) {
            line = NpcCallDialogue.variant(npc, "no_contracts");
        } else if (actives.size() < PlayerQuestManager.MAX_ACTIVE_PER_ISSUER) {
            for (var offer : board.offers().stream().limit(MAX_OFFERS - actives.size()).toList())
                options.add(Option.of(text("choice.offer", offer.title()), p -> brief(p, offer.id())));
        }
        options.add(leave());
        return node(line, options);
    }

    private void addContractOptions(List<Option> options, QuestContract active) {
        options.add(active.isReady() ? Option.of(text("choice.report_contract"), p -> report(p, active.id()))
                : Option.locked(text("choice.report_contract"), Component.translatable("dialogue.cyberneticenhancements.reason.unfinished")));
        options.add(Option.of(text("choice.quest_status"), p -> status(p, active.id())));
        options.add(Option.of(text("choice.abandon_contract"), p -> confirmAbandon(p, active.id())));
    }

    /** What can be done with one of several active contracts. */
    private Node contract(ServerPlayer player, String questId) {
        var active = find(player, questId);
        if (active == null) return root(player, text("quest.no_active"));
        var options = new ArrayList<Option>();
        addContractOptions(options, active);
        options.add(Option.of(text("choice.back"), p -> root(p, NpcCallDialogue.variant(npc, "back"))));
        options.add(leave());
        return node(text("quest.status", active.title(), active.objectiveLine(), active.rewardSummary()), options);
    }

    private QuestContract find(ServerPlayer player, String questId) {
        return PlayerQuestManager.snapshotForIssuer(player, npc).actives().stream().filter(value -> value.id().equals(questId)).findFirst().orElse(null);
    }

    private Node status(ServerPlayer player, String questId) {
        var active = find(player, questId);
        if (active == null) return root(player, text("quest.no_active"));
        return node(text("quest.status", active.title(), active.objectiveLine(), active.rewardSummary()), List.of(
                Option.of(text("choice.back"), p -> root(p, NpcCallDialogue.variant(npc, "back"))), leave()));
    }

    private Node brief(ServerPlayer player, String offerId) {
        var board = PlayerQuestManager.snapshotForIssuer(player, npc);
        if (board.actives().size() >= PlayerQuestManager.MAX_ACTIVE_PER_ISSUER) return root(player, NpcCallDialogue.variant(npc, "back"));
        var offer = board.offers().stream().filter(value -> value.id().equals(offerId)).findFirst().orElse(null);
        if (offer == null) return root(player, text("quest.offer_gone"));
        return node(text("quest.briefing", offer.title(), offer.objectiveLine(), offer.rewardSummary()), List.of(
                Option.of(text("choice.accept_contract"), p -> accept(p, offerId)),
                Option.of(text("choice.other_contracts"), p -> root(p, NpcCallDialogue.variant(npc, "contracts"))), leave()));
    }

    private Node accept(ServerPlayer player, String offerId) {
        var board = PlayerQuestManager.snapshotForIssuer(player, npc);
        int index = -1;
        for (int slot = 0; slot < board.offers().size(); slot++) if (board.offers().get(slot).id().equals(offerId)) { index = slot; break; }
        var result = index < 0 ? PlayerQuestManager.Result.INVALID : PlayerQuestManager.acceptIssuerOffer(player, npc, index);
        if (result == PlayerQuestManager.Result.OK) {
            QuestContract contract = find(player, offerId);
            if (contract != null) return say(text("quest.accepted", contract.title(), contract.objectiveLine()));
        }
        return root(player, text("quest.offer_gone"));
    }

    private Node report(ServerPlayer player, String questId) {
        var contract = find(player, questId);
        if (contract == null || PlayerQuestManager.turnInIssuerActive(player, npc, questId) != PlayerQuestManager.TurnInResult.OK)
            return root(player, text("quest.report_unavailable"));
        return say(NpcCallDialogue.variant(npc, "quest.paid", contract.rewardSummary()));
    }

    private Node confirmAbandon(ServerPlayer player, String questId) {
        var contract = find(player, questId);
        if (contract == null) return root(player, text("quest.no_active"));
        return node(text("quest.abandon_confirm", contract.title()), List.of(
                Option.of(text("choice.confirm_abandon"), p -> {
                    var current = find(p, questId);
                    if (current == null || PlayerQuestManager.abandonIssuerActive(p, npc, questId) != PlayerQuestManager.Result.OK)
                        return root(p, text("quest.no_active"));
                    return say(NpcCallDialogue.variant(npc, "quest.abandoned", current.title()));
                }),
                Option.of(text("choice.keep_contract"), p -> root(p, NpcCallDialogue.variant(npc, "back"))), leave()));
    }
}
