package de.artemis.cyberneticenhancements.common.compat.jade;

import de.artemis.cyberneticenhancements.common.blockentity.DropoffPointBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.IronQuestCrateBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/** Whether a quest crate belongs to a contract, and whether a dropoff point is yours and has something waiting. */
enum CrateJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String QUEST = "CeQuest", OWNER = "CeOwner", DELIVERY = "CeDelivery";
    private static final int MINE = 1, OTHER = 2;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        var theme = IThemeHelper.get();
        if (data.contains(OWNER)) {
            tooltip.add(data.getInt(OWNER) == MINE
                    ? theme.success(Component.translatable("jade.cyberneticenhancements.dropoff.mine"))
                    : data.getInt(OWNER) == OTHER ? theme.warning(Component.translatable("jade.cyberneticenhancements.dropoff.other"))
                    : theme.info(Component.translatable("jade.cyberneticenhancements.dropoff.unbound")));
            if (data.getBoolean(DELIVERY)) tooltip.add(theme.success(Component.translatable("jade.cyberneticenhancements.dropoff.delivery")));
        } else if (data.getBoolean(QUEST)) {
            tooltip.add(theme.info(Component.translatable("jade.cyberneticenhancements.crate.quest")));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof DropoffPointBlockEntity dropoff) {
            data.putInt(OWNER, dropoff.owner() == null ? 0 : dropoff.owner().equals(accessor.getPlayer().getUUID()) ? MINE : OTHER);
            if (dropoff.owner() != null && dropoff.owner().equals(accessor.getPlayer().getUUID())) data.putBoolean(DELIVERY, dropoff.hasUnreadDelivery());
        } else if (accessor.getBlockEntity() instanceof IronQuestCrateBlockEntity crate && !crate.questId().isEmpty()) {
            data.putBoolean(QUEST, true);
        }
    }

    @Override
    public ResourceLocation getUid() { return CyberneticEnhancementsJadePlugin.id("crate"); }
}
