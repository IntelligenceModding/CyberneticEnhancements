package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.TitleStartFlow;
import de.artemis.cyberneticenhancements.client.ui.VanillaMenuPresentation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ScreenEvent;

import static de.artemis.cyberneticenhancements.client.ui.TitleStartFlow.Stage.*;

/** The once-per-launch tutorial offer, drawn over the real title screen's background before the menu fades in. */
public final class TitleStartPrompt {
    private static final TitleStartFlow FLOW = new TitleStartFlow();
    private static final long ENTER_MILLIS = 600, LEAVE_MILLIS = 380, MENU_FADE_MILLIS = 500;
    private static final int ACCENT = 0xFF5266, BODY = 0xE6A3AE, VEIL = 0x090408;
    private static final float OFFER_DIM = 0.6F;
    // The offer fades in, then out before the menu; the backdrop dim glides with it instead of cutting.
    private static long enteredAt = -1, leavingAt = -1, menuAt = -1;
    private TitleStartPrompt() {}

    public record OfferLayout(int x, int y, int width, int height, int buttonsY, java.util.List<FormattedCharSequence> lines) {}

    public static TitleStartFlow.Stage stage() {
        return FLOW.stage(TutorialProgress.open(), Minecraft.getInstance().level != null);
    }

    public static boolean hidesMenu() { return stage() != MENU; }

    /** From here on the title is always the normal menu, whichever way the world was reached. */
    public static void onLogin(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        FLOW.worldEntered();
        leavingAt = enteredAt = menuAt = -1;
    }

    /** Ends the offer at once, such as when the tutorial is launched from it; it never returns this launch. */
    public static void settle() {
        FLOW.advance();
        leavingAt = enteredAt = menuAt = -1;
    }

    /** Fades the menu's widgets in, for a title screen that is shown again after something else covered it. */
    public static void fadeMenuIn() {
        if (stage() == MENU && menuAt < 0) menuAt = Util.getMillis();
    }

    /** Starts fading out the offer; the title switches to the menu once it is gone. */
    public static void leave() {
        if (leavingAt >= 0 || stage() == MENU) return;
        leavingAt = Util.getMillis();
    }

    private static void switchToMenu(Screen title, long now) {
        var mc = Minecraft.getInstance();
        FLOW.advance();
        leavingAt = enteredAt = -1;
        menuAt = now;
        // Called before the title renders its widgets, so the rebuilt ones start this frame faded out.
        title.resize(mc, title.width, title.height);
    }

    private static Component text(String key) {
        return Component.translatable("screen.cyberneticenhancements.title." + key);
    }

    private static int color(int rgb, float alpha) {
        // Font rendering treats alpha below 4 as opaque.
        return Math.clamp(Math.round(alpha * 255), 4, 255) << 24 | rgb & 0xFFFFFF;
    }

    private static float ease(long start, long now, long duration) {
        float progress = Math.clamp((now - start) / (float) duration, 0, 1);
        return progress * progress * (3 - 2 * progress);
    }

    public static OfferLayout offerLayout(int width, int height) {
        var font = Minecraft.getInstance().font;
        int panelWidth = Math.min(320, width - 40);
        var lines = font.split(text("tutorial.message"), panelWidth - 32);
        int buttonsOffset = 46 + lines.size() * 12;
        int panelHeight = buttonsOffset + 44 + 16;
        int lowest = height - panelHeight - 24;
        // Keep clear of the logo where there is room, and of the footer always.
        int y = Math.clamp((height - panelHeight) / 2 + 20, Math.min(84, lowest), Math.max(84, lowest));
        return new OfferLayout((width - panelWidth) / 2, y, panelWidth, panelHeight, y + buttonsOffset, lines);
    }

    /** Drawn after the title background and before its widgets, so the offer's buttons stay on top. */
    public static void renderBehindMenu(GuiGraphics graphics, Screen title, boolean titleFading) {
        long now = Util.getMillis();
        if (stage() != MENU && !LaunchIntro.isPlaying()) {
            if (leavingAt >= 0 && now - leavingAt >= LEAVE_MILLIS) switchToMenu(title, now);
            if (stage() != MENU) {
                offerFrame(graphics, title, now);
                return;
            }
        }
        if (menuAt < 0) return;
        float alpha = ease(menuAt, now, MENU_FADE_MILLIS);
        widgetAlpha(title, alpha);
        // The title's own first-open fade rewrites widget alpha each frame; outlast it.
        if (alpha >= 1 && !titleFading) menuAt = -1;
    }

    private static void offerFrame(GuiGraphics graphics, Screen title, long now) {
        if (enteredAt < 0) {
            enteredAt = now;
            Minecraft.getInstance().getNarrator().sayNow(text("tutorial.heading").copy().append(". ").append(text("tutorial.message")));
        }
        float enter = ease(enteredAt, now, ENTER_MILLIS);
        float leave = leavingAt < 0 ? 0 : ease(leavingAt, now, LEAVE_MILLIS);
        float dim = OFFER_DIM * enter * (1 - leave);
        if (dim > 0.01F) graphics.fill(0, 0, title.width, title.height, color(VEIL, dim));
        offer(graphics, title, enter * (1 - leave));
    }

    private static void widgetAlpha(Screen title, float alpha) {
        for (var child : title.children())
            if (child instanceof AbstractWidget widget && !(child instanceof PlainTextButton)) widget.setAlpha(alpha);
    }

    private static void offer(GuiGraphics graphics, Screen title, float opacity) {
        widgetAlpha(title, opacity);
        if (opacity <= 0.01F) return;
        var font = Minecraft.getInstance().font;
        var layout = offerLayout(title.width, title.height);
        int center = title.width / 2;
        VanillaMenuPresentation.promptPanel(graphics, layout.x(), layout.y(), layout.width(), layout.height(), opacity);
        Component heading = text("tutorial.heading");
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(center - font.width(heading) * 0.75F, layout.y() + 16, 0);
            graphics.pose().scale(1.5F, 1.5F, 1);
            graphics.drawString(font, heading, 0, 0, color(ACCENT, opacity), true);
        } finally { graphics.pose().popPose(); }
        int y = layout.y() + 42;
        for (var line : layout.lines()) {
            graphics.drawString(font, line, center - font.width(line) / 2, y, color(BODY, opacity), true);
            y += 12;
        }
    }

    /** Input waits for each fade, so nothing is pressed half-visible or mid-change. */
    private static boolean blocking(Screen screen) {
        if (screen == null || screen.getClass() != TitleScreen.class || LaunchIntro.isPlaying()) return false;
        if (stage() == MENU) return menuAt >= 0;
        return leavingAt >= 0 || enteredAt < 0 || Util.getMillis() - enteredAt < ENTER_MILLIS;
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (blocking(event.getScreen())) event.setCanceled(true);
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (blocking(event.getScreen())) event.setCanceled(true);
    }

    public static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (blocking(event.getScreen())) event.setCanceled(true);
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (blocking(event.getScreen())) event.setCanceled(true);
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (blocking(event.getScreen())) event.setCanceled(true);
    }

    public static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (blocking(event.getScreen())) event.setCanceled(true);
    }
}
