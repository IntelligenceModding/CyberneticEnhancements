package de.artemis.cyberneticenhancements.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class DevTestEntity extends AbstractCityNpcEntity {
    public DevTestEntity(EntityType<? extends AbstractCityNpcEntity> type, Level level) {
        super(type, level);
        setCustomName(net.minecraft.network.chat.Component.literal("Dev-Test").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        setInvulnerable(true);
    }

    public static de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueTree dialogueTree() {
        return new de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueTree(300, java.util.Map.of(300,
                new de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueNode(300, "screen.cyberneticenhancements.dev_test.greeting", java.util.List.of(
                        choice(1310, "contracts", de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueView.CONTRACTS),
                        choice(1302, "shop", de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueView.SHOP),
                        choice(1321, "services", de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueView.SERVICES),
                        choice(1301, "cache", de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueView.CACHE_INTEL)))));
    }

    private static de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueChoice choice(int id, String key,
            de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueView view) {
        String label = "screen.cyberneticenhancements.npc.dialogue.choice." + key;
        return new de.artemis.cyberneticenhancements.common.dialogue.NpcDialogueChoice(id, label, label, 300, view, 0, 0, false, "");
    }

    @Override
    public NpcType npcType() {
        return NpcType.NETRUNNER;
    }

    @Override
    protected String dispositionColor(boolean angry) {
        return "light_purple";
    }

    @Override
    public int trustPoints(Player player) {
        return 300;
    }

    @Override
    public boolean canInteract(Player player) {
        return player.hasPermissions(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS);
    }

    @Override
    public void markMet(Player player) {
        if (canInteract(player)) {
            super.markMet(player);
            super.raiseTrust(player, 300);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Older developer NPCs were saved with their AI disabled.
        setNoAi(false);
    }

    @Override
    public boolean isRefusing(Player player) {
        return false;
    }

    @Override
    public boolean hasHeatSurcharge(Player player) {
        return false;
    }

    @Override
    public void applyDialoguePenalty(ServerPlayer player, int trustLoss, int annoyance, boolean closeConversation) {
    }
}
