package de.artemis.cyberneticenhancements.client.render;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;

public final class NpcConversationPoses {
    private static AbstractCityNpcEntity portrait;
    private static NpcConversationMotion.Pose portraitPose;

    private NpcConversationPoses() {}

    public static void renderPortrait(AbstractCityNpcEntity entity, NpcConversationMotion.Pose pose, Runnable render) {
        var previousEntity = portrait;
        var previousPose = portraitPose;
        portrait = entity;
        portraitPose = pose;
        try { render.run(); }
        finally { portrait = previousEntity; portraitPose = previousPose; }
    }

    static NpcConversationMotion.Pose pose(AbstractCityNpcEntity entity, float partialTick) {
        if (entity == portrait) return portraitPose;
        if ((!entity.isOnCall() && !entity.isInConversation()) || entity.hurtTime > 0
                || entity.isAggressive() || entity.getDeltaMovement().horizontalDistanceSqr() > 0.0025) return null;
        double time = (entity.level().getGameTime() + partialTick) / 20.0;
        double replyAge = entity.conversationSpokeAt() < 0 ? -1 : time - entity.conversationSpokeAt() / 20.0;
        return NpcConversationMotion.sample(time, entity.getUUID().hashCode(), replyAge);
    }
}
