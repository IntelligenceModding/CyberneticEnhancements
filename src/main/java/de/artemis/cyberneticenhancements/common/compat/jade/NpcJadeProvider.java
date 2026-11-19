package de.artemis.cyberneticenhancements.common.compat.jade;

import de.artemis.cyberneticenhancements.common.entity.AbstractCityNpcEntity;
import de.artemis.cyberneticenhancements.common.entity.WorldEncounterNpcEntity;
import de.artemis.cyberneticenhancements.common.menu.FixerDialogMenu;
import de.artemis.cyberneticenhancements.common.world.NpcDisposition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

/** Trust, discount, mood and aggression of an NPC towards the player looking at it. */
enum NpcJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final String TRUST = "CeTrust", DISCOUNT = "CeDiscount", ROLE = "CeRole", AGGRESSION = "CeAggression",
            ANGRY = "CeAngry", REFUSING = "CeRefusing", SURCHARGE = "CeSurcharge", CONTACT = "CeContact";
    private static final int TRUST_COLOR = 0xFF5CC8E8, AGGRESSION_COLOR = 0xFFD9822B, TEXT_COLOR = 0xFFFFFFFF;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        var theme = IThemeHelper.get();
        var elements = IElementHelper.get();
        if (data.contains(ROLE)) {
            var role = WorldEncounterNpcEntity.Role.of(data.getInt(ROLE));
            tooltip.add(Component.translatable("jade.cyberneticenhancements.npc.role",
                    theme.info(Component.translatable("jade.cyberneticenhancements.npc.role." + role.name().toLowerCase(java.util.Locale.ROOT)))));
        }
        if (data.getBoolean(CONTACT)) {
            int points = data.getInt(TRUST);
            int level = points < 0 ? 0 : AbstractCityNpcEntity.trustLevelForPoints(points);
            tooltip.add(Component.translatable("jade.cyberneticenhancements.npc.trust",
                    theme.info(Component.translatable("screen.cyberneticenhancements.fixer.trust." + level))));
            if (points >= 0) {
                tooltip.add(elements.progress(FixerDialogMenu.trustProgressPercentForPoints(points) / 100.0F, Component.empty(),
                        elements.progressStyle().color(TRUST_COLOR), BoxStyle.getNestedBox(), true));
            }
            if (data.contains(DISCOUNT)) {
                int discount = data.getInt(DISCOUNT);
                tooltip.add(Component.translatable("jade.cyberneticenhancements.npc.discount", discount > 0
                        ? theme.success(Component.translatable("screen.cyberneticenhancements.fixer.summary.discount.value", discount))
                        : theme.info(Component.translatable("screen.cyberneticenhancements.fixer.summary.discount.none"))));
            }
            if (data.getBoolean(SURCHARGE)) tooltip.add(theme.warning(Component.translatable("jade.cyberneticenhancements.npc.surcharge")));
        }
        if (data.contains(AGGRESSION)) {
            tooltip.add(Component.translatable("jade.cyberneticenhancements.npc.mood",
                    theme.danger(Component.translatable("jade.cyberneticenhancements.npc.mood.hostile"))));
            tooltip.add(elements.progress(data.getFloat(AGGRESSION), Component.translatable("jade.cyberneticenhancements.npc.aggression"),
                    elements.progressStyle().color(AGGRESSION_COLOR).textColor(TEXT_COLOR), BoxStyle.getNestedBox(), true));
        } else if (data.getBoolean(REFUSING)) {
            tooltip.add(Component.translatable("jade.cyberneticenhancements.npc.mood",
                    theme.danger(Component.translatable("jade.cyberneticenhancements.npc.mood.refusing"))));
        } else if (data.getBoolean(ANGRY)) {
            tooltip.add(Component.translatable("jade.cyberneticenhancements.npc.mood",
                    theme.warning(Component.translatable("jade.cyberneticenhancements.npc.mood.angry"))));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof AbstractCityNpcEntity npc) || !(accessor.getPlayer() instanceof ServerPlayer player)) return;
        if (npc instanceof WorldEncounterNpcEntity encounter) data.putInt(ROLE, encounter.role().ordinal());
        if (npc.isContact()) {
            data.putBoolean(CONTACT, true);
            int points = npc.knownTrustPoints(player);
            data.putInt(TRUST, points);
            if (npc.npcType().supportsTrustDiscount()) data.putInt(DISCOUNT, points < 0 ? 0 : FixerDialogMenu.discountPercentForTrustPoints(points));
            data.putBoolean(SURCHARGE, npc.hasHeatSurcharge(player));
            data.putBoolean(REFUSING, npc.isRefusing(player));
        }
        if (npc.isDefendingAgainst(player)) data.putFloat(AGGRESSION, npc.aggressionRemaining(0));
        data.putBoolean(ANGRY, NpcDisposition.angry(player, npc.getUUID()));
    }

    @Override
    public ResourceLocation getUid() { return CyberneticEnhancementsJadePlugin.id("npc"); }
}
