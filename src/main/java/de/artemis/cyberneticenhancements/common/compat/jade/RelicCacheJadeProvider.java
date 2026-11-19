package de.artemis.cyberneticenhancements.common.compat.jade;

import de.artemis.cyberneticenhancements.common.block.RelicCacheBlock;
import de.artemis.cyberneticenhancements.common.block.RelicCacheTier;
import de.artemis.cyberneticenhancements.common.blockentity.RelicCacheBlockEntity;
import net.minecraft.ChatFormatting;
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

/** The tier of a relic cache, and whether this player has already breached it. */
enum RelicCacheJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String TIER = "CeTier", OPENED = "CeOpened", QUEST = "CeQuest";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        var theme = IThemeHelper.get();
        if (data.contains(TIER)) {
            var tier = RelicCacheTier.values()[Math.clamp(data.getInt(TIER), 0, RelicCacheTier.values().length - 1)];
            var label = Component.translatable("jade.cyberneticenhancements.relic_cache.tier." + tier.name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(switch (tier) {
                        case UNCOMMON -> ChatFormatting.GREEN;
                        case RARE -> ChatFormatting.AQUA;
                        case EPIC -> ChatFormatting.LIGHT_PURPLE;
                        case LEGENDARY -> ChatFormatting.GOLD;
                    });
            tooltip.add(Component.translatable("jade.cyberneticenhancements.relic_cache.tier", label));
        }
        if (data.getBoolean(QUEST)) tooltip.add(theme.info(Component.translatable("jade.cyberneticenhancements.relic_cache.quest")));
        if (data.getBoolean(OPENED)) tooltip.add(theme.warning(Component.translatable("jade.cyberneticenhancements.relic_cache.opened")));
        else if (data.contains(OPENED)) tooltip.add(theme.success(Component.translatable("jade.cyberneticenhancements.relic_cache.sealed")));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlock() instanceof RelicCacheBlock block)) return;
        data.putInt(TIER, block.tier().ordinal());
        if (accessor.getBlockEntity() instanceof RelicCacheBlockEntity cache && cache.isQuestCache()) data.putBoolean(QUEST, true);
        if (accessor.getLevel() instanceof ServerLevel level) data.putBoolean(OPENED, RelicCacheBlock.hasPlayerOpened(accessor.getPlayer(), level, accessor.getPosition()));
    }

    @Override
    public ResourceLocation getUid() { return CyberneticEnhancementsJadePlugin.id("relic_cache"); }
}
