package de.artemis.cyberneticenhancements.common.compat.jade;

import de.artemis.cyberneticenhancements.common.config.FastTravelConfig;
import de.artemis.cyberneticenhancements.common.fasttravel.FastTravelPoints;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/** A terminal's name, whether this player has linked it yet, and what a trip costs. */
enum FastTravelJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String NAME = "CeName", LINKED = "CeLinked", COST = "CeCost", DISABLED = "CeDisabled";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        var theme = IThemeHelper.get();
        if (data.getBoolean(DISABLED)) {
            tooltip.add(theme.warning(Component.translatable("jade.cyberneticenhancements.fast_travel.disabled")));
            return;
        }
        if (data.contains(NAME)) tooltip.add(Component.translatable("jade.cyberneticenhancements.fast_travel.name", theme.info(Component.literal(data.getString(NAME)))));
        tooltip.add(data.getBoolean(LINKED)
                ? theme.success(Component.translatable("jade.cyberneticenhancements.fast_travel.linked"))
                : theme.warning(Component.translatable("jade.cyberneticenhancements.fast_travel.unlinked")));
        if (data.contains(COST)) tooltip.add(Component.translatable("jade.cyberneticenhancements.fast_travel.cost", data.getInt(COST)));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getLevel() instanceof ServerLevel level)) return;
        if (!FastTravelConfig.enabled()) {
            data.putBoolean(DISABLED, true);
            return;
        }
        var points = FastTravelPoints.get(level.getServer());
        var point = points.find(level.dimension().location().toString(), accessor.getPosition());
        boolean linked = point != null && points.discovered(accessor.getPlayer().getUUID(), point);
        data.putBoolean(LINKED, linked);
        // A terminal nobody has linked yet is only a number the player has not earned.
        if (linked) data.putString(NAME, point.name());
        data.putInt(COST, FastTravelConfig.cost());
    }

    @Override
    public ResourceLocation getUid() { return CyberneticEnhancementsJadePlugin.id("fast_travel"); }
}
