package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.client.ClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

public final class TitleScreenPresentation {
    private static Screen pendingBackground;
    private static float pendingFade;
    private TitleScreenPresentation() {}

    public static boolean enabled(Screen screen) {
        return screen != null && screen.getClass() == TitleScreen.class && backgroundEnabled();
    }

    public static boolean menuButtonsEnabled(Screen screen) {
        return backgroundEnabled() && screen != null && supportsMenuTheme(screen.getClass());
    }

    static boolean supportsMenuTheme(Class<?> screenType) {
        return (screenType.getName().startsWith("net.minecraft.client.gui.screens.")
                    || screenType == net.neoforged.neoforge.client.gui.ModListScreen.class)
                && !net.minecraft.client.gui.screens.ChatScreen.class.isAssignableFrom(screenType)
                && !net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen.class.isAssignableFrom(screenType)
                && !net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.isAssignableFrom(screenType)
                && !net.minecraft.client.gui.screens.DeathScreen.class.isAssignableFrom(screenType)
                && !net.minecraft.client.gui.screens.DeathScreen.TitleConfirmScreen.class.isAssignableFrom(screenType);
    }

    private static boolean backgroundEnabled() {
        return ClientConfig.SPEC.isLoaded() && ClientConfig.CUSTOM_TITLE_SCREEN.get();
    }

    public static void panoramaRendered(GuiGraphics graphics, float fade) {
        pendingBackground = null;
        if (!backgroundEnabled()) return;
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof TitleScreen) {
            // TitleScreen skips Screen.renderBackground and its background event.
            background(graphics, screen, fade);
        } else {
            pendingBackground = screen;
            pendingFade = fade;
        }
    }

    public static void onBackgroundRendered(net.neoforged.neoforge.client.event.ScreenEvent.BackgroundRendered event) {
        if (de.artemis.cyberneticenhancements.client.TitleSlideshow.renderedForCurrentScreen()) {
            pendingBackground = null;
            return;
        }
        if (pendingBackground != event.getScreen()) return;
        pendingBackground = null;
        // Preserve normal menu blur and textures, then draw crisp scan lines above them.
        background(event.getGuiGraphics(), event.getScreen(), pendingFade);
    }

    private static int color(int rgb, float alpha) {
        return Math.round(Math.clamp(alpha, 0, 1) * 255) << 24 | rgb & 0xFFFFFF;
    }

    public static void background(GuiGraphics graphics, Screen screen, float fade) {
        if (!backgroundEnabled() || screen == null || fade <= 0.02F) return;
        graphics = de.artemis.cyberneticenhancements.client.TitleSlideshow.nativeGraphics(graphics);
        backdropEffects(graphics, fade, true);
        graphics.flush();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
    }

    public static void backdropEffects(GuiGraphics graphics, float fade, boolean sweepEnabled) {
        if (!backgroundEnabled() || fade <= 0.02F) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.fillGradient(0, 0, width, height, color(0x090408, fade * 0.28F), color(0x16060E, fade * 0.55F));
        for (int y = 0; y < height; y += 4)
            graphics.fill(0, y, width, y + 1, color(0, fade * 0.08F));

        double motion = sweepEnabled ? Minecraft.getInstance().options.screenEffectScale().get() : 0;
        if (motion > 0) {
            double seconds = Util.getMillis() / 1000.0;
            int sweep = (int) (seconds * 45 * motion) % (height + 18) - 18;
            graphics.fillGradient(0, sweep, width, sweep + 18, 0, color(0xE83E59, fade * 0.06F));
        }
    }

    public static int footerTextColor(int original) {
        return enabled(Minecraft.getInstance().screen) && (original & 0xFFFFFF) == 0xFFFFFF
                ? original & 0xFF000000 | 0xFFB3BC : original;
    }
}
