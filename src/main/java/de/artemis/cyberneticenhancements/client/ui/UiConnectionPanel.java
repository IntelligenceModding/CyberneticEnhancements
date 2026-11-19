package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class UiConnectionPanel {
    private UiConnectionPanel() {}

    public static void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, float progress, float animationProgress,
                              Component header, Component title, Component subtitle, List<Component> stages,
                              Component status, Component ready) {
        progress = Math.clamp(progress, 0, 1);
        animationProgress = Math.clamp(animationProgress, 0, 1);
        UiRenderer.panel(graphics, x, y, width, height, UiRenderer.FIELD, UiRenderer.PANEL_EDGE);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 20, UiRenderer.ROW_DISABLED);
        graphics.fill(x + 2, y + 20, x + width - 2, y + 21, alpha(UiRenderer.ACCENT, 0.8F));
        long now = System.currentTimeMillis();
        int sweepY = y + 24 + Math.round((height - 44) * animationProgress);
        graphics.fill(x + 2, Math.max(y + 22, sweepY - 8), x + width - 2, Math.min(y + height - 2, sweepY + 2), alpha(UiRenderer.ROW_SELECTED, 0.12F));
        for (int row = y + 24; row < y + height - 2; row += 3) {
            graphics.fill(x + 2, row, x + width - 2, row + 1, alpha(UiRenderer.PANEL_EDGE, (row + now / 24L) % 9 == 0 ? 0.08F : 0.04F));
        }
        line(graphics, font, header, x + 10, y + 8, width - 20, UiRenderer.ACCENT);
        line(graphics, font, title, x + 10, y + 30, width - 20, UiRenderer.TEXT);
        line(graphics, font, subtitle, x + 10, y + 44, width - 20, UiRenderer.TEXT_SECONDARY);
        int complete = Math.min(stages.size(), Math.max(1, (int) Math.ceil(animationProgress * stages.size())));
        for (int index = 0; index < stages.size(); index++) {
            boolean active = index < complete;
            graphics.drawString(font, active ? "[OK]" : "[  ]", x + 10, y + 66 + index * 14, active ? UiRenderer.ACCENT : alpha(UiRenderer.PANEL_EDGE, 0.55F), false);
            line(graphics, font, stages.get(index), x + 44, y + 66 + index * 14, width - 54,
                    active ? UiRenderer.TEXT : alpha(UiRenderer.TEXT_SECONDARY, 0.45F));
        }
        int barY = y + height - 30, barWidth = width - 20;
        graphics.fill(x + 10, barY, x + width - 10, barY + 8, UiRenderer.ROW_DISABLED);
        graphics.fill(x + 11, barY + 1, x + 11 + Math.round((barWidth - 2) * progress), barY + 7, UiRenderer.ACCENT);
        graphics.fill(x + 11, barY + 1, x + 11 + Math.round((barWidth - 2) * progress * 0.45F), barY + 3, alpha(UiRenderer.TEXT, 0.35F));
        line(graphics, font, status, x + 10, barY - 11, width - 160, UiRenderer.TEXT_SECONDARY);
        if (progress >= 0.995F) line(graphics, font, ready, x + width - 150, barY - 11, 140, UiRenderer.ACCENT);
    }

    private static void line(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int color) {
        var lines = font.split(text, Math.max(1, width));
        if (!lines.isEmpty()) graphics.drawString(font, lines.getFirst(), x, y, color, false);
    }

    private static int alpha(int color, float alpha) {
        return (color & 0xFFFFFF) | (Math.round(alpha * 255) << 24);
    }
}
