package de.artemis.cyberneticenhancements.common.quest;

import de.artemis.cyberneticenhancements.common.dialogue.NpcCallManager;
import de.artemis.cyberneticenhancements.common.dialogue.NpcCallReason;
import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.world.FixerIdentitySavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/** Failure is final immediately; the follow-up call waits until the player and issuer are available. */
public final class QuestFailureCalls {
    private static final String TAG = "cyberneticenhancements.quest_failure_calls";
    private QuestFailureCalls() {}

    static void queue(ServerPlayer player, QuestContract failed) {
        var queued = player.getPersistentData().getCompound(TAG);
        queued.put(failed.id(), failed.toTag());
        player.getPersistentData().put(TAG, queued);
    }

    public static QuestContract pending(ServerPlayer player, UUID issuer) {
        var queued = player.getPersistentData().getCompound(TAG);
        for (var id : queued.getAllKeys().stream().sorted().toList()) {
            var failed = QuestContract.fromTag(queued.getCompound(id));
            if (failed.issuerId().equals(issuer)) return failed;
        }
        return null;
    }

    static boolean isQueued(ServerPlayer player, String questId) {
        return player.getPersistentData().getCompound(TAG).contains(questId);
    }

    public static void acknowledge(ServerPlayer player, String questId) {
        var queued = player.getPersistentData().getCompound(TAG);
        if (queued.contains(questId)) { // A patient quest's contact only lasts until its failure has been spoken about.
            var failed = QuestContract.fromTag(queued.getCompound(questId));
            if (failed.type() == QuestType.LOCATE) AbstractCityNpcEntity.closeTemporaryContact(player, failed.issuerId());
        }
        queued.remove(questId);
        if (queued.isEmpty()) player.getPersistentData().remove(TAG);
        else player.getPersistentData().put(TAG, queued);
    }

    static void tick(ServerPlayer player) {
        if (NpcCallManager.hasCall(player.getUUID())) return;
        var queued = player.getPersistentData().getCompound(TAG);
        for (var id : queued.getAllKeys().stream().sorted().toList()) {
            var failed = QuestContract.fromTag(queued.getCompound(id));
            if (FixerIdentitySavedData.get(player.serverLevel()).isDeceased(failed.issuerId())) {
                acknowledge(player, id);
                continue;
            }
            var issuer = AbstractCityNpcEntity.resolveCallContact(player, failed.issuerId());
            if (issuer != null && NpcCallManager.startIncoming(player, issuer, NpcCallReason.CACHE_FAILED, true)) return;
        }
    }

    static void copyState(Player original, Player clone) {
        if (original.getPersistentData().contains(TAG)) clone.getPersistentData().put(TAG, original.getPersistentData().getCompound(TAG).copy());
    }

    static void clear(ServerPlayer player) {
        var queued = player.getPersistentData().getCompound(TAG);
        for (var id : queued.getAllKeys()) {
            var failed = QuestContract.fromTag(queued.getCompound(id));
            if (failed.type() == QuestType.LOCATE) AbstractCityNpcEntity.closeTemporaryContact(player, failed.issuerId());
        }
        player.getPersistentData().remove(TAG);
    }
}
