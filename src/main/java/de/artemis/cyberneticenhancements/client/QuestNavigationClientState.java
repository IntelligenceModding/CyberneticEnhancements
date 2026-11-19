package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class QuestNavigationClientState {
    private static QuestNavigationPayload snapshot = new QuestNavigationPayload(List.of());
    private static java.util.Map<String, QuestNavigationPayload.Target> byId = java.util.Map.of();

    private QuestNavigationClientState() {
    }

    public static void update(QuestNavigationPayload payload) {
        snapshot = payload;
        var targets = new java.util.HashMap<String, QuestNavigationPayload.Target>();
        payload.targets().forEach(target -> targets.put(target.questId(), target));
        byId = java.util.Map.copyOf(targets);
    }

    public static QuestNavigationPayload get() {
        return snapshot;
    }
    public static QuestNavigationPayload.Target target(String questId) { return byId.get(questId); }

    public static Set<UUID> visibleTurnInNpcs() {
        return snapshot.turnIns().stream()
                .filter(turnIn -> !CompassPreferences.entry(turnIn.questId()).hidden())
                .map(QuestNavigationPayload.TurnIn::issuerId)
                .collect(Collectors.toSet());
    }

    public static void clear() {
        snapshot = new QuestNavigationPayload(List.of());
        byId = java.util.Map.of();
    }
}
