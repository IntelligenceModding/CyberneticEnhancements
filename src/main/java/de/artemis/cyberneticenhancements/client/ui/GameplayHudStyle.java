package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Quiet surfaces for gameplay overlays; warning and state colors remain semantic. */
public final class GameplayHudStyle {
    public static final int BACKGROUND = 0x181F28;
    public static final int SURFACE = 0x111820;
    public static final int TRACK = UiRenderer.FIELD_EDGE;

    private GameplayHudStyle() {}

    public static int color(int rgb, float opacity) {
        return Math.round(255 * Math.clamp(opacity, 0, 1)) << 24 | rgb & 0xFFFFFF;
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int accent, float opacity) {
        graphics.fill(x, y, x + width, y + height, color(BACKGROUND, .65F * opacity));
        if (accent >>> 24 != 0) {
            graphics.fill(x, y, x + 2, y + height, color(accent, .55F * opacity));
        }
    }

    public static void surface(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, color(SURFACE, .3F));
    }

    public static void separator(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, color(TRACK, .65F));
    }

    public static void option(GuiGraphics graphics, int x, int y, int width, int height, boolean enabled) {
        option(graphics, x, y, width, height, enabled, 1);
    }

    public static void option(GuiGraphics graphics, int x, int y, int width, int height, boolean enabled, float opacity) {
        graphics.fill(x, y, x + width, y + height, color(UiRenderer.ROW, (enabled ? .32F : .14F) * opacity));
        graphics.fill(x, y + 4, x + 1, y + height - 4, color(UiRenderer.ACCENT, (enabled ? .45F : .15F) * opacity));
    }

    public static void shortcutKey(GuiGraphics graphics, Font font, Component key, int x, int y, int height,
                                   int keyWidth, boolean enabled, float opacity) {
        graphics.drawString(font, HudText.trim(font, key, keyWidth - 8), x + 5, y + (height - font.lineHeight) / 2,
                color(enabled ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, opacity), false);
        graphics.fill(x + keyWidth, y + 5, x + keyWidth + 1, y + height - 5, color(UiRenderer.FIELD_EDGE, .6F * opacity));
    }

    public static void shortcut(GuiGraphics graphics, Font font, int x, int y, int width, Component key,
                                Component label, Component trailing, boolean enabled, float opacity) {
        int keyWidth = CallHudLayout.keyWidth(font.width(key), width);
        int trailingWidth = font.width(trailing);
        int labelX = x + keyWidth + CallHudLayout.GAP;
        option(graphics, x, y, width, 20, enabled, opacity);
        shortcutKey(graphics, font, key, x, y, 20, keyWidth, enabled, opacity);
        graphics.drawString(font, HudText.trim(font, label, width - keyWidth - CallHudLayout.GAP - 8
                        - (trailingWidth == 0 ? 0 : trailingWidth + 8)), labelX, y + (20 - font.lineHeight) / 2,
                color(enabled ? UiRenderer.TEXT : UiRenderer.TEXT_SECONDARY, opacity), false);
        if (trailingWidth > 0) graphics.drawString(font, trailing, x + width - trailingWidth - 5,
                y + (20 - font.lineHeight) / 2, color(UiRenderer.TEXT_SECONDARY, opacity), false);
    }

    public static void meter(GuiGraphics graphics, int x, int y, int width, int height, float ratio,
                             int idle, int fill, boolean warning) {
        graphics.fill(x, y, x + width, y + height, color(idle, .65F));
        int filled = Math.round(width * Math.clamp(ratio, 0, 1));
        if (filled > 0) graphics.fill(x, y, x + filled, y + height, color(fill, warning ? 1 : .85F));
    }

    public static void meterNotches(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        for (int segment = 1; segment < 10; segment++) {
            int notch = x + width * segment / 10;
            graphics.fill(notch, y + 1, notch + 1, y + height, color);
        }
    }
}
