package de.artemis.cyberneticenhancements.common.compat.jade;

import de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity;
import de.artemis.cyberneticenhancements.common.entity.CyberpsychoLoadout;
import de.artemis.cyberneticenhancements.common.entity.HolographicNpc;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/** The threat tier and installed weapon of a cyberpsycho. */
enum CyberpsychoJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final String TIER = "CeTier", LOADOUT = "CeLoadout";

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        var theme = IThemeHelper.get();
        if (data.contains(TIER)) {
            int tier = data.getInt(TIER);
            var label = Component.translatable("jade.cyberneticenhancements.cyberpsycho.tier." + tier);
            tooltip.add(Component.translatable("jade.cyberneticenhancements.cyberpsycho.tier",
                    tier >= 3 ? theme.danger(label) : tier == 2 ? theme.warning(label) : theme.info(label)));
        }
        if (data.contains(LOADOUT)) {
            tooltip.add(Component.translatable("jade.cyberneticenhancements.cyberpsycho.loadout",
                    theme.info(CyberpsychoLoadout.fromIndex(data.getInt(LOADOUT)).label())));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof CyberpsychoEntity psycho)) return;
        data.putInt(LOADOUT, psycho.loadout().ordinal());
        // MaxTac and gang members are summoned at a fixed strength rather than a contract tier.
        if (!(psycho instanceof HolographicNpc)) data.putInt(TIER, psycho.tier());
    }

    @Override
    public ResourceLocation getUid() { return CyberneticEnhancementsJadePlugin.id("cyberpsycho"); }
}
