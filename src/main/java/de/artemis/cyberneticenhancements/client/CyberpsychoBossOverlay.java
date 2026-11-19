package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveUi;
import de.artemis.cyberneticenhancements.client.ui.HealingHealthBar;
import de.artemis.cyberneticenhancements.client.ui.GameplayHudStyle;
import de.artemis.cyberneticenhancements.client.render.NpcHealingColors;
import de.artemis.cyberneticenhancements.common.registry.ModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

public final class CyberpsychoBossOverlay {
    private static final java.util.Map<net.minecraft.world.entity.LivingEntity, HealingHealthBar> HEALTH = new java.util.WeakHashMap<>();
    private CyberpsychoBossOverlay() {}

    public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var minecraft = Minecraft.getInstance();
        if (event.isCanceled() || minecraft.level == null) return;
        // Match the server boss UUID to its entity; never restyle unrelated vanilla/modded bosses.
        de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity subject = null;
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity.getType() == ModEntityTypes.CYBERPSYCHO.get()
                    && entity.getUUID().equals(event.getBossEvent().getId())) {
                subject = (de.artemis.cyberneticenhancements.common.entity.CyberpsychoEntity) entity;
                break;
            }
        }
        if (subject == null) return;
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        var font = minecraft.font;
        int width = Math.min(280, g.guiWidth() - 16), x = (g.guiWidth() - width) / 2;
        int statusBottom = (int) Math.ceil(CyberwareHudOverlay.topStatusBottom()
                * ResponsiveUi.referenceViewport().renderScale());
        int y = Math.max(event.getY() - 9, statusBottom == 0 ? 3 : statusBottom + 4);
        event.setIncrement(40 + y - (event.getY() - 9));
        CyberwareHudOverlay.drawCardFrame(g, x, y, width, 34, 0);
        var healing = subject.combatHealing();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var frame = HEALTH.computeIfAbsent(subject, ignored -> new HealingHealthBar()).sample(
                subject.getHealth() / subject.getMaxHealth(), healing.usingItem(), healing.highlighted(),
                subject.tickCount + (double) partialTick,
                de.artemis.cyberneticenhancements.common.entity.NpcCombatHealing.HEAL_FRACTION);
        float healingColor = frame.green();
        int red = NpcHealingColors.blend(0xFFE16D7A, 0xFF83DCAD, healingColor);
        String percent = Math.round(frame.health() * 100) + "%";
        Component status = Component.translatable(healing.usingItem() ? "hud.cyberneticenhancements.npc.using_medicine"
                : frame.filling() ? "hud.cyberneticenhancements.npc.recovering" : "hud.cyberneticenhancements.cyberpsycho");
        int percentX = x + width - 8 - font.width(percent);
        String statusText = font.plainSubstrByWidth(status.getString(), Math.max(1, (width - 32 - font.width(percent)) * 2 / 3));
        int statusX = percentX - 8 - font.width(statusText);
        String name = font.plainSubstrByWidth(event.getBossEvent().getName().getString(), Math.max(1, statusX - x - 16));
        g.drawString(font, name, x + 8, y + 5, UiRenderer.TEXT, true);
        g.drawString(font, statusText, statusX, y + 5, red, true);
        g.drawString(font, percent, percentX, y + 5, red, true);
        // Equal six-pixel gaps below the header divider and above the bottom border.
        int barX = x + 8, barY = y + 22, barWidth = width - 16;
        g.fill(barX, barY, barX + barWidth, barY + 5, NpcHealingColors.blend(0xFF30232D, 0xFF20382D, healingColor));
        int filled = Math.clamp(Math.round(barWidth * frame.health()), 0, barWidth);
        if (filled > 0) {
            g.fill(barX, barY, barX + filled, barY + 5, NpcHealingColors.blend(0xFFB7475B, 0xFF398D65, healingColor));
            g.fill(barX, barY, barX + filled, barY + 1, red);
        }
        if (frame.pendingHealth() > frame.health()) {
            int pending = Math.clamp(Math.round(barWidth * frame.pendingHealth()), filled, barWidth);
            g.fill(barX + filled, barY, barX + pending, barY + 5, 0x805BCB98);
        }
        GameplayHudStyle.meterNotches(g, barX, barY, barWidth, 5, 0xAA111820);
        CompassHudOverlay.reserveBossSpace(y + 37);
    }
}
