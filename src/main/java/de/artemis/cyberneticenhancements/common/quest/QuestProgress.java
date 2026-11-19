package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.network.QuestProgressPayload;
import de.artemis.cyberneticenhancements.common.network.QuestProgressPayload.Objective;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

/** Every ongoing contract appears together, with counters for measurable objectives. */
public final class QuestProgress {
    private static final Map<ServerPlayer, QuestProgressPayload> SENT = new WeakHashMap<>();
    private QuestProgress() {}

    public static QuestProgressPayload capture(ServerPlayer player) {
        var objectives = new ArrayList<Objective>();
        for (var quest : PlayerQuestManager.activeContracts(player)) {
            if (quest.status() != QuestStatus.ACTIVE && quest.status() != QuestStatus.READY) continue;
            String key = null;
            Object target = Component.empty();
            int priority = 2;
            int current = quest.progress(), required = quest.targetCount();
            if (!quest.isReady()) switch (quest.type()) {
                case RECOVERY -> {
                    if (QuestDimension.of(quest.dimensionId()) == QuestDimension.STANDARD || quest.destinationReached()) key = "collection";
                    target = quest.recoveryItem().getDescription();
                    priority = 1;
                }
                case ELIMINATION -> {
                    key = "elimination";
                    target = quest.eliminationTarget().getDescription();
                    priority = nearObjective(player, quest, 96) ? 0 : 1;
                }
                case INVESTIGATION -> {
                    if (quest.investigation() == null || quest.investigation().located()) break;
                    key = "survey";
                    target = Component.empty();
                    priority = nearObjective(player, quest, InvestigationSurvey.RANGE) ? 0 : 1;
                    current = quest.investigation().count();
                    required = InvestigationSurvey.READINGS;
                }
                default -> { }
            }
            Component detail = key == null ? quest.objectiveLine() : Component.translatable(
                    "hud.cyberneticenhancements.quest_progress." + key, current, required, target);
            if (!quest.isReady() && quest.type() != QuestType.RECOVERY && quest.targetPos() != null && QuestDimension.of(quest.dimensionId()) != QuestDimension.STANDARD
                    && !player.level().dimension().location().toString().equals(quest.dimensionId())) {
                detail = Component.translatable("quest.cyberneticenhancements.objective.travel", QuestDimension.name(quest.dimensionId()), detail);
            }
            var title = quest.hudTitle(de.artemis.cyberneticenhancements.common.dialogue.NpcIncomingCalls.playTicks(player));
            QuestProgressPayload.CallTarget callTarget = null;
            if (quest.isReady() && !quest.type().requiresInPersonTurnIn() && !quest.itemLossPending()) {
                var color = net.minecraft.ChatFormatting.getByName(de.artemis.cyberneticenhancements.common.world.NpcDisposition.color(player, quest.issuerId()));
                callTarget = new QuestProgressPayload.CallTarget(quest.issuerId(), Component.literal(quest.issuerName())
                        .withStyle(color == null ? net.minecraft.ChatFormatting.AQUA : color));
            }
            objectives.add(new Objective(quest.id(), title, detail, current, required, priority, key != null, callTarget));
        }
        objectives.sort(Comparator.comparingInt(Objective::priority)
                .thenComparingDouble(objective -> -(double) objective.current() / objective.required()).thenComparing(Objective::id));
        return new QuestProgressPayload(objectives);
    }

    private static boolean nearObjective(ServerPlayer player, QuestContract quest, int range) {
        if (quest.targetPos() == null || !player.level().dimension().location().toString().equals(quest.dimensionId())) return false;
        double x = player.getX() - quest.targetPos().getX() - .5;
        double z = player.getZ() - quest.targetPos().getZ() - .5;
        return x * x + z * z <= range * range;
    }

    public static void sync(ServerPlayer player) {
        var snapshot = capture(player);
        if (!snapshot.equals(SENT.put(player, snapshot))) PacketDistributor.sendToPlayer(player, snapshot);
    }
    public static void reset(ServerPlayer player) { SENT.remove(player); }
}
