package de.artemis.cyberneticenhancements.common.compat.jade;

import de.artemis.cyberneticenhancements.common.blockentity.CrateBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/** Points out the shipping container crate that holds a contact's sealed cargo; the crate already tells the client, so nothing is sent. */
enum ContainerCrateJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof CrateBlockEntity crate && crate.marked()) {
            tooltip.add(IThemeHelper.get().info(Component.translatable("jade.cyberneticenhancements.container_crate.cargo")));
        }
    }

    @Override
    public ResourceLocation getUid() { return CyberneticEnhancementsJadePlugin.id("container_crate"); }
}
