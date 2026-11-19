package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Carries the menu slideshow across disconnecting and saving. */
public final class DisconnectPresentation {
    public static final long CLOSE_MILLIS = 800;
    public static final long REVEAL_MILLIS = 450;
    private DisconnectPresentation() {}

    public static void close(GuiGraphics graphics, long elapsed) {
        drawAboveScreen(graphics, () -> {
            if (Minecraft.getInstance().options.screenEffectScale().get() <= 0) {
                drawBackdrop(graphics, ease(elapsed, 0, CLOSE_MILLIS));
            } else {
                float open = 1 - ease(elapsed, 0, 620);
                float line = 1 - ease(elapsed, 620, CLOSE_MILLIS);
                JoinPresentation.drawOpening(graphics, open, line, elapsed);
            }
        });
    }

    public static void saving(GuiGraphics graphics, Component message) {
        drawAboveScreen(graphics, () -> {
            if (!TitleSlideshow.renderTransition(graphics, 1)) {
                LaunchIntroPresentation.transitionBackdrop(graphics, 1);
                if (message != null) graphics.drawCenteredString(Minecraft.getInstance().font, message,
                        graphics.guiWidth() / 2, graphics.guiHeight() / 2 - 4, 0xFFFFD5DC);
            }
        });
    }

    public static void reveal(GuiGraphics graphics, long elapsed) {
        drawAboveScreen(graphics, () -> drawBackdrop(graphics, 1 - ease(elapsed, 0, REVEAL_MILLIS)));
    }

    private static void drawBackdrop(GuiGraphics graphics, float opacity) {
        if (!TitleSlideshow.renderTransition(graphics, opacity)) LaunchIntroPresentation.transitionBackdrop(graphics, opacity);
    }

    private static void drawAboveScreen(GuiGraphics graphics, Runnable draw) {
        graphics.flush();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 4000);
            draw.run();
        } finally {
            graphics.pose().popPose();
        }
    }

    private static float ease(long elapsed, long start, long end) {
        float t = Math.clamp((elapsed - start) / (float) (end - start), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
