package de.artemis.cyberneticenhancements.common.dialogue;

public record NpcDialogueChoice(
        int id,
        String translationKey,
        String playerLineKey,
        int nextNodeId,
        NpcDialogueView view,
        int trustDelta,
        int annoyanceDelta,
        boolean closeConversation,
        String rewardMemoryKey
) {
    public boolean opensView() {
        return view == NpcDialogueView.SHOP || view == NpcDialogueView.SERVICES || view == NpcDialogueView.CONTRACTS;
    }
}
