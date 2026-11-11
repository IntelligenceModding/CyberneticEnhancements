package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class UiRenderer {
    static final float[] DISCLOSURE_CHEVRON = {
            -7, -10, 3, 0, 7, 0, -5, -12,
            3, 0, -7, 10, -5, 12, 7, 0
    };
    public static final int PANEL = 0xF0181F28;
    public static final int PANEL_EDGE = 0xFF2C4B63;
    public static final int PANEL_EDGE_ACTIVE = 0xFF4F7793;
    public static final int ROW = 0xE6232C36;
    public static final int ROW_SELECTED = 0xF03B5369;
    public static final int ROW_DISABLED = 0xB01A222B;
    public static final int FIELD = 0xFF111820;
    public static final int FIELD_EDGE = 0xFF314252;
    public static final int TEXT = 0xFFF1F4F8;
    public static final int TEXT_SECONDARY = 0xFF95A4B2;
    public static final int ACCENT = 0xFF8BDAF8;
    public static final int WARNING = 0xFFF0B36A;

    private UiRenderer() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill, int edge) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, ((fill >>> 24) * 0x30 / 255) << 24);
        graphics.fill(x, y, x + width, y + height, fill);
        outline(graphics, x, y, width, height, edge);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        panel(graphics, x, y, width, height, fill, PANEL_EDGE);
    }

    public static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static void tank(GuiGraphics graphics, int x, int y, int width, int height, int percent, int fluidColor) {
        if (width < 3 || height < 3) return;
        graphics.fill(x, y, x + width, y + height, FIELD);
        int innerHeight = height - 2;
        int bottom = y + height - 1;
        int fillHeight = Math.round(innerHeight * Math.max(0, Math.min(100, percent)) / 100.0F);
        if (fillHeight > 0) {
            graphics.fill(x + 1, bottom - fillHeight, x + width - 1, bottom, fluidColor);
        }
        outline(graphics, x, y, width, height, FIELD_EDGE);
    }

    public static void action(GuiGraphics graphics, int x, int y, int width, int height, boolean active, boolean highlighted) {
        action(graphics, x, y, width, height, active, highlighted ? 1 : 0);
    }

    public static void action(GuiGraphics graphics, int x, int y, int width, int height, boolean active, float highlight) {
        control(graphics, x, y, width, height, active, false, highlight, 0xFFFFFFFF);
    }

    public static void control(GuiGraphics graphics, int x, int y, int width, int height,
                               boolean active, boolean selected, boolean hovered, int alphaMask) {
        control(graphics, x, y, width, height, active, selected, hovered ? 1F : 0F, alphaMask);
    }

    public static void control(GuiGraphics graphics, int x, int y, int width, int height,
                               boolean active, boolean selected, float highlight, int alphaMask) {
        highlight = Math.clamp(highlight, 0, 1);
        int fill = !active ? ROW_DISABLED : selected ? ROW_SELECTED : ROW;
        if (active) {
            fill = blend(fill, selected ? ACCENT : ROW_SELECTED, highlight * (selected ? 0.14F : 0.65F));
        }
        int edge = !active ? FIELD_EDGE : selected ? ACCENT : blend(FIELD_EDGE, PANEL_EDGE_ACTIVE, highlight);
        panel(graphics, x, y, width, height, opacity(fill, alphaMask), opacity(edge, alphaMask));
        if (active && selected) {
            graphics.fill(x + 1, y + 1, x + 3, y + height - 1, opacity(ACCENT, alphaMask));
        }
    }

    public static void button(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                              Component label, boolean active, boolean selected, boolean hovered, int alphaMask) {
        control(graphics, x, y, width, height, active, selected, hovered, alphaMask);
        Component fitted = font.width(label) <= width - 12 ? label
                : Component.literal(font.plainSubstrByWidth(label.getString(), Math.max(0, width - 20)) + "…");
        graphics.drawString(font, fitted, x + (width - font.width(fitted)) / 2,
                y + (height - font.lineHeight) / 2,
                opacity(!active ? TEXT_SECONDARY : selected ? ACCENT : TEXT, alphaMask), false);
    }

    public static int titlePlate(GuiGraphics graphics, Font font, int x, int y, int maxWidth, Component title) {
        int width = Math.min(maxWidth, font.width(title) + 20);
        button(graphics, font, x, y, width, UiCloseControl.HEIGHT, title, true, false, false, 0xFFFFFFFF);
        return width;
    }

    private static int opacity(int color, int alphaMask) {
        return (color & 0xFFFFFF) | (((color >>> 24) * (alphaMask >>> 24) / 255) << 24);
    }

    public static void disclosureChevron(GuiGraphics graphics, float x, float y, float expansion, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) (Math.PI * 0.5) * expansion));
        graphics.pose().scale(0.32F, 0.32F, 1);
        var matrix = graphics.pose().last().pose();
        var vertices = graphics.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        for (int index = 0; index < DISCLOSURE_CHEVRON.length; index += 2) {
            vertices.addVertex(matrix, DISCLOSURE_CHEVRON[index], DISCLOSURE_CHEVRON[index + 1], 0).setColor(color);
        }
        graphics.flush();
        graphics.pose().popPose();
    }

    public static int blend(int first, int second, float amount) {
        float value = Math.max(0, Math.min(1, amount));
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int from = (first >>> shift) & 255;
            int to = (second >>> shift) & 255;
            result |= Math.round(from + (to - from) * value) << shift;
        }
        return result;
    }

    public static void arrow(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 3, x + 12, y + 5, color);
        for (int step = 0; step < 5; step++) {
            graphics.fill(x + 8 + step, y + step, x + 9 + step, y + 8 - step, color);
        }
    }
}
