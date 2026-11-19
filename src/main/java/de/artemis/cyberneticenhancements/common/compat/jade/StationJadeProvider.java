package de.artemis.cyberneticenhancements.common.compat.jade;

import de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.TechStationBlockEntity;
import de.artemis.cyberneticenhancements.common.blockentity.UpgradeableStationBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

/** The hardware tier each slot of a workshop station supports, and how far along its current job is. */
enum StationJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String TIERS = "CeSlotTiers", PROGRESS = "CeProgress";
    private static final int PROGRESS_COLOR = 0xFF5CC8E8, TEXT_COLOR = 0xFFFFFFFF;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(TIERS)) {
            int[] tiers = data.getIntArray(TIERS);
            MutableComponent list = Component.empty();
            for (int i = 0; i < tiers.length; i++) {
                if (i > 0) list.append(", ");
                var tier = UpgradeableStationBlockEntity.getTierByOrdinal(tiers[i]);
                list.append(Component.translatable(tier.translationKey()).withStyle(tier.getColor()));
            }
            tooltip.add(Component.translatable("jade.cyberneticenhancements.station.tiers", list));
        }
        if (data.contains(PROGRESS)) {
            var elements = IElementHelper.get();
            tooltip.add(elements.progress(data.getFloat(PROGRESS), Component.translatable("jade.cyberneticenhancements.station.working"),
                    elements.progressStyle().color(PROGRESS_COLOR).textColor(TEXT_COLOR), BoxStyle.getNestedBox(), true));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof UpgradeableStationBlockEntity station)) return;
        int[] tiers = new int[station.getSlotCount()];
        for (int i = 0; i < tiers.length; i++) tiers[i] = station.getSupportedTierOrdinal(i);
        data.putIntArray(TIERS, tiers);
        if (station instanceof TechStationBlockEntity tech && tech.working()) data.putFloat(PROGRESS, tech.progress / (float) TechStationBlockEntity.SERVICE_TICKS);
        if (station instanceof RecyclerStationBlockEntity recycler && recycler.working()) data.putFloat(PROGRESS, recycler.progress / (float) RecyclerStationBlockEntity.RECYCLE_TICKS);
    }

    @Override
    public ResourceLocation getUid() { return CyberneticEnhancementsJadePlugin.id("station"); }
}
