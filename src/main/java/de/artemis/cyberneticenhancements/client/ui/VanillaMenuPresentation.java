package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Rendering only: native widgets keep their geometry, input and narration. */
public final class VanillaMenuPresentation {
    private static boolean renderingMenuText;
    public static final int TEXT = 0xFFFFD5DC, MUTED = 0xFFD5A6B0;
    // Repeated borders provide quiet structure; text and active controls carry emphasis.
    public static final int EDGE = 0xFF73394A, HIGHLIGHT = 0xFFD38A9A;
    public static final int PANEL_EDGE = 0xFF43252F, ENTRY_EDGE = 0xFF4F2B37;
    public static final int ENTRY_HOVER = 0xFF82505F, ENTRY_SELECTED = 0xFFAC6779;
    /** The red scrollbar of the title-screen menus. */
    public static void scrollbar(GuiGraphics g, UiScrollbar.Track track, boolean highlighted) {
        g.fill(track.x(), track.y(), track.x() + track.width(), track.y() + track.height(), 0xFF200B13);
        UiRenderer.outline(g, track.x(), track.y(), track.width(), track.height(), SCROLL_EDGE);
        g.fill(track.x(), track.thumbStart(), track.x() + track.width(), track.thumbStart() + track.thumbLength(),
                highlighted ? 0xFFB94A63 : 0xFF803047);
        UiRenderer.outline(g, track.x(), track.thumbStart(), track.width(), track.thumbLength(),
                highlighted ? HIGHLIGHT : SCROLL_THUMB_EDGE);
    }

    public static final int SCROLL_EDGE = 0xFF51313F, SCROLL_THUMB_EDGE = 0xFFA56576;
    public static final int FIELD = 0xFF200B13;

    public static boolean enabled() {
        return TitleScreenPresentation.menuButtonsEnabled(Minecraft.getInstance().screen);
    }

    public static boolean enabled(Object widget) {
        // Option sliders are OptionInstance's nested classes in net.minecraft.client,
        // rather than net.minecraft.client.gui alongside the other native widgets.
        return (widget.getClass().getName().startsWith("net.minecraft.client.")
                || widget.getClass() == net.neoforged.neoforge.client.gui.widget.ModListWidget.class)
                && enabled();
    }

    public static boolean beginScreenText(Screen screen) {
        boolean previous = renderingMenuText;
        renderingMenuText = TitleScreenPresentation.menuButtonsEnabled(screen);
        return previous;
    }

    public static void endScreenText(boolean previous) { renderingMenuText = previous; }
    public static boolean textEnabled() { return renderingMenuText && enabled(); }

    public static int textColor(int original) {
        int rgb = original & 0xFFFFFF;
        int replacement = switch (rgb) {
            case 0xFFFFFF, 0xE0E0E0 -> TEXT;
            case 0x808080, 0xA0A0A0, 0x707070, 0xCCCCCC -> MUTED;
            default -> original;
        };
        return original & 0xFF000000 | replacement & 0xFFFFFF;
    }

    public static void menuPanel(GuiGraphics g, int x, int y, int width, int height) {
        g.fillGradient(x, y, x + width, y + height, 0xC51C0810, 0xE016060E);
    }

    public static void promptPanel(GuiGraphics graphics, int x, int y, int width, int height, float opacity) {
        graphics.fillGradient(x, y, x + width, y + height,
                fadeAlpha(0xC51C0810, opacity), fadeAlpha(0xE016060E, opacity));
        UiRenderer.outline(graphics, x, y, width, height, fadeAlpha(EDGE, opacity));
    }

    private static int fadeAlpha(int argb, float opacity) {
        return Math.round((argb >>> 24) * Math.clamp(opacity, 0, 1)) << 24 | argb & 0xFFFFFF;
    }

    public static void menuDivider(GuiGraphics g, int x, int y, int width) {
        g.fill(x, y, Math.min(g.guiWidth(), x + width), y + 1, PANEL_EDGE);
    }

    public static void field(GuiGraphics g, int x, int y, int width, int height, boolean focused, boolean active) {
        g.fill(x, y, x + width, y + height, FIELD);
        UiRenderer.outline(g, x, y, width, height, !active ? 0xFF70424D : focused ? HIGHLIGHT : EDGE);
    }

    public static void scroll(GuiGraphics g, int x, int y, int width, int height, boolean thumb) {
        g.fill(x, y, x + width, y + height, thumb ? 0xFF803047 : FIELD);
        UiRenderer.outline(g, x, y, width, height, thumb ? SCROLL_THUMB_EDGE : SCROLL_EDGE);
    }

    private VanillaMenuPresentation() { }
}
