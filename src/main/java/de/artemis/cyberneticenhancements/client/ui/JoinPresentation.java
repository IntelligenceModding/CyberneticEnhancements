package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.client.TitleSlideshow;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws one frame of the join animation over the live world and HUD; see {@link JoinAnimationTimeline} for the choreography.
 */
public final class JoinPresentation {
    private static final int ACCENT = 0xFF5266;
    /** The overlay sits above all HUD layers so the opening can cover them. */
    private static final float ABOVE_HUD = 4000;

    private JoinPresentation() {}

    private static int color(int rgb, float alpha) {
        return Math.round(Math.clamp(alpha, 0, 1) * 255) << 24 | rgb & 0xFFFFFF;
    }

    /** {@code capturedScene} is the world and HUD together when the fallback backdrop uses tearing. */
    public static void render(GuiGraphics graphics, JoinAnimationTimeline.Frame frame, UiFadeLayer capturedScene, long millis) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, ABOVE_HUD);
            draw(graphics, frame, capturedScene, millis);
        } finally { graphics.pose().popPose(); }
    }

    private static void draw(GuiGraphics graphics, JoinAnimationTimeline.Frame frame, UiFadeLayer capturedScene, long millis) {
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.flush();
        vignette(graphics, width, height, frame.vignette());
        if (frame.fade() > 0) drawBackdrop(graphics, 0, height, frame.fade(), TitleSlideshow.transitionReady());
        else drawOpening(graphics, frame.open(), frame.lineWidth(), millis);
        if (capturedScene != null) {
            int visible = Math.round(height * frame.open());
            int top = (height - visible) / 2;
            bands(graphics, capturedScene, frame.glitch(), top, top + visible, width, millis);
        }
    }

    static void drawOpening(GuiGraphics graphics, float open, float lineWidth, long millis) {
        if (open >= 1) return;
        int height = graphics.guiHeight();
        int visible = Math.round(height * open), top = (height - visible) / 2, bottom = top + visible;
        boolean slideshow = TitleSlideshow.transitionReady();
        if (visible == 0) drawBackdrop(graphics, 0, height, 1, slideshow);
        else {
            drawBackdrop(graphics, 0, top, 1, slideshow);
            drawBackdrop(graphics, bottom, height, 1, slideshow);
        }
        if (lineWidth > 0 && visible < height) edgeLight(graphics, graphics.guiWidth(), top, bottom, open, lineWidth, millis);
    }

    private static void drawBackdrop(GuiGraphics graphics, int top, int bottom, float opacity, boolean slideshow) {
        if (top >= bottom) return;
        if (!slideshow) {
            LaunchIntroPresentation.transitionBackdrop(graphics, top, bottom, opacity);
            return;
        }
        // Clip the full-size slide so the artwork keeps its menu crop while the view opens.
        graphics.enableScissor(0, top, graphics.guiWidth(), bottom);
        try {
            TitleSlideshow.renderTransition(graphics, opacity);
        } finally {
            graphics.disableScissor();
        }
    }

    /** The bright line that opens the view: it splits into the two edges of the opening, with a soft glow bleeding inward. */
    private static void edgeLight(GuiGraphics graphics, int width, int top, int bottom, float open, float lineWidth, long millis) {
        // A faint electrical flicker while the line is still thin.
        float flicker = open > 0 ? 1 : 0.8F + 0.2F * (float) Math.abs(Math.sin(millis / 23.0));
        int half = Math.round(width * lineWidth / 2);
        int left = width / 2 - half, right = width / 2 + half;
        float strength = Math.min(1, (1 - open) * 12) * (1 - open * 0.7F) * flicker;
        graphics.fill(left, top - 1, right, top + 1, color(0xFFFFFF, strength));
        graphics.fill(left, bottom - 1, right, bottom + 1, color(0xFFFFFF, strength));
        graphics.fillGradient(left, top - 12, right, top - 1, 0, color(ACCENT, 0.5F * strength));
        graphics.fillGradient(left, bottom + 1, right, bottom + 12, color(ACCENT, 0.5F * strength), 0);
        if (bottom - top > 2) {
            graphics.fillGradient(left, top + 1, right, top + 14, color(ACCENT, 0.3F * strength), 0);
            graphics.fillGradient(left, bottom - 14, right, bottom - 1, 0, color(ACCENT, 0.3F * strength));
        }
    }

    /** A few slices of the real picture knocked sideways, the launch intro's tearing at a gentler strength, settling as the view opens. */
    private static void bands(GuiGraphics graphics, UiFadeLayer layer, float glitch, int top, int bottom, int width, long millis) {
        int span = bottom - top;
        if (span < 24) return;
        long frame = millis / 50;
        for (int band = 0; band < 5; band++) {
            int hash = (int) (frame * 0x9E3779B1L + band * 0x85EBCA6BL);
            hash ^= hash >>> 15;
            int bandHeight = 3 + Math.floorMod(hash, 7), y = top + Math.floorMod(hash >>> 4, Math.max(1, span - bandHeight));
            int shift = Math.round((Math.floorMod(hash >>> 9, 2) == 0 ? -1 : 1) * (8 + Math.floorMod(hash >>> 11, 18)) * glitch);
            graphics.enableScissor(0, y, width, y + bandHeight);
            try {
                LaunchIntroPresentation.transitionBackdrop(graphics, y, y + bandHeight, 1);
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(shift, 0, 0);
                    layer.drawCaptured(graphics, 1);
                } finally { graphics.pose().popPose(); }
            } finally { graphics.disableScissor(); }
        }
    }

    private static void vignette(GuiGraphics graphics, int width, int height, float amount) {
        if (amount <= 0.01F) return;
        int edgeX = width / 4, edgeY = height / 4;
        graphics.fillGradient(0, 0, width, edgeY, color(0x090408, amount * 0.6F), 0);
        graphics.fillGradient(0, height - edgeY, width, height, 0, color(0x090408, amount * 0.6F));
        // Side shading: a gradient across each edge strip, drawn as a few thin columns.
        int steps = 12;
        for (int step = 0; step < steps; step++) {
            float falloff = 1 - step / (float) steps;
            int alpha = Math.round(amount * 0.4F * falloff * falloff * 255);
            if (alpha <= 0) continue;
            int x = step * edgeX / steps, next = (step + 1) * edgeX / steps;
            graphics.fill(x, 0, next, height, alpha << 24 | 0x090408);
            graphics.fill(width - next, 0, width - x, height, alpha << 24 | 0x090408);
        }
    }
}
