package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

 final class WorkshopScreenParts {
    private WorkshopScreenParts() {}
    static Component tr(String key, Object... args) {
        return Component.translatable("screen.cyberneticenhancements.workshop." + key, args);
    }
    static void frame(GuiGraphics g, int x, int y) {
        UiRenderer.panel(g, x, y, 540, 416, StationScreenStyle.PANEL_DEEP);
        panel(g, x + 12, y + 12, 516, 32);
        panel(g, x + 12, y + 54, 196, 184);
        panel(g, x + 218, y + 54, 310, 184);
        panel(g, x + 12, y + 280, 516, 30);
        panel(g, x + 12, y + 320, 516, 84);
    }
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        UiRenderer.panel(g, x, y, w, h, UiRenderer.PANEL);
    }
    static void text(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
        var fitted = font.width(text) <= width ? text
                : Component.literal(font.plainSubstrByWidth(text.getString(), Math.max(1, width - font.width("…"))) + "…");
        g.drawString(font, fitted, x, y, color, false);
    }
    static void wrap(GuiGraphics g, Font font, Component text, int x, int y, int width, int lines, int color) {
        var split = font.split(text, width);
        for (int i = 0; i < Math.min(lines, split.size()); i++) g.drawString(font, split.get(i), x, y + i * 11, color, false);
    }
}