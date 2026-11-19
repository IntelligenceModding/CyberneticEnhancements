package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload;
import de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload.Target;
import de.artemis.cyberneticenhancements.common.network.QuestNavigationPayload.TurnIn;
import de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

public final class QuestNavigation {
    private static final Map<ServerPlayer, QuestNavigationPayload> SENT = new WeakHashMap<>();

    private QuestNavigation() {
    }

    public static QuestNavigationPayload capture(ServerPlayer player) {
        var targets = new ArrayList<Target>();
        targets.addAll(de.artemis.cyberneticenhancements.common.world.RelicCacheIntel.navigationTargets(player));
        targets.addAll(de.artemis.cyberneticenhancements.common.world.ChromiumDiscovery.navigationTargets(player));
        var turnIns = new ArrayList<TurnIn>();
        for (var quest : PlayerQuestManager.activeContracts(player)) {
            if (quest.status() == QuestStatus.ACTIVE && quest.type() != QuestType.RECOVERY && quest.targetPos() != null && !quest.dimensionId().isBlank()) {
                String title = quest.type() == QuestType.INVESTIGATION
                        ? net.minecraft.network.chat.Component.translatable(quest.investigation() != null && quest.investigation().located()
                                ? "quest.cyberneticenhancements.marker.investigation.source" : "quest.cyberneticenhancements.marker.investigation.survey", quest.title()).getString()
                        : quest.hudTitle(de.artemis.cyberneticenhancements.common.dialogue.NpcIncomingCalls.playTicks(player)).getString();
                if (quest.type() == QuestType.CYBERPSYCHO) title = net.minecraft.network.chat.Component.translatable(quest.destinationReached()
                        ? "quest.cyberneticenhancements.marker.cyberpsycho.shard" : "quest.cyberneticenhancements.marker.cyberpsycho.area", quest.psychoName()).getString();
                targets.add(new Target(quest.id(), title, quest.dimensionId(), quest.targetPos(), false));
            } else if (quest.status() == QuestStatus.READY && quest.type() == QuestType.LOCATE) {
                // A world encounter's people keep no contact record, so the hand-in point is stored on the quest itself.
                turnIns.add(new TurnIn(quest.id(), quest.issuerId()));
                if (quest.returnPos() != null) targets.add(new Target(quest.id(), quest.issuerName(), quest.dimensionId(), quest.returnPos(), true));
            } else if (quest.status() == QuestStatus.READY) {
                var identities = FixerIdentitySavedData.get(player.serverLevel());
                var issuer = identities.identity(quest.issuerId());
                if (issuer != null && !identities.isDeceased(quest.issuerId())) {
                    turnIns.add(new TurnIn(quest.id(), quest.issuerId()));
                    var location = AbstractCityNpcEntity.archiveContactLocation(player, quest.issuerId(), issuer.dimensionId(), issuer.homePos());
                    if (location.pos() != null && !location.dimensionId().isBlank())
                        targets.add(new Target(quest.id(), issuer.displayName(), location.dimensionId(), location.pos(), true,
                                quest.type() == QuestType.RECOVERY ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(quest.recoveryItem()).toString() : ""));
                }
            }
        }
        targets.sort(Comparator.comparing(Target::questId));
        turnIns.sort(Comparator.comparing(TurnIn::questId));
        return new QuestNavigationPayload(targets, turnIns);
    }

    public static void sync(ServerPlayer player) {
        QuestProgress.sync(player);
        var snapshot = capture(player);
        if (!snapshot.equals(SENT.put(player, snapshot))) PacketDistributor.sendToPlayer(player, snapshot);
    }

    public static void reset(ServerPlayer player) {
        SENT.remove(player);
        QuestProgress.reset(player);
    }

    public static void onDimensionChange(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reset(player);
            sync(player);
        }
    }
}
