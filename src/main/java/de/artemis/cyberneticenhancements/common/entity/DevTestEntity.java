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
