package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

public final class HudText {
    private HudText() {}

    public static FormattedCharSequence trim(Font font, FormattedText text, int width) {
        if (font.width(text) <= width) return Language.getInstance().getVisualOrder(text);
        return Language.getInstance().getVisualOrder(FormattedText.composite(
                font.substrByWidth(text, Math.max(1, width - font.width("…"))),
                Component.literal("…").setStyle(text instanceof Component component ? component.getStyle() : Style.EMPTY)));
    }

    private static final double SCROLL_PAUSE = 2, SCROLL_SPEED = 8;

    /** How far text that overflows its window has scrolled: a pause, a slow glide to the end, a pause, then back to the start. */
    public static double scrollOffset(double overflow, double seconds) {
        if (overflow <= 0) return 0;
        double travel = overflow / SCROLL_SPEED, time = seconds % (SCROLL_PAUSE * 2 + travel);
        if (time < SCROLL_PAUSE) return 0;
        return time < SCROLL_PAUSE + travel ? (time - SCROLL_PAUSE) * SCROLL_SPEED : overflow;
    }

    public static List<FormattedCharSequence> wrap(Font font, Component text, int width, int limit) {
        var lines = font.getSplitter().splitLines(text, width, Style.EMPTY);
        var result = new ArrayList<FormattedCharSequence>();
        for (int i = 0; i < Math.min(limit, lines.size()); i++) {
            var line = lines.get(i);
            if (i == limit - 1 && lines.size() > limit) line = FormattedText.composite(
                    font.substrByWidth(line, Math.max(1, width - font.width("…"))), Component.literal("…").setStyle(text.getStyle()));
            result.add(Language.getInstance().getVisualOrder(line));
        }
        return List.copyOf(result);
    }
}
