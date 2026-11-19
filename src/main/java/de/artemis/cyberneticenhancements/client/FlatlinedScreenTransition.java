package de.artemis.cyberneticenhancements.client;

import com.mojang.logging.LogUtils;
import de.artemis.cyberneticenhancements.client.ui.FlatlinedPresentation;
import de.artemis.cyberneticenhancements.client.ui.UiFadeLayer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class FlatlinedScreenTransition {
    private static UiFadeLayer snapshot;
    private static Screen destination;
    private static long startedAt;

    private FlatlinedScreenTransition() {
    }

    public static void onOpening(ScreenEvent.Opening event) {
        clear();
        if (!FlatlinedPresentation.enabled(event.getCurrentScreen()) || !FlatlinedPresentation.confirmationEnabled(event.getNewScreen())
                || Minecraft.getInstance().options.screenEffectScale().get() <= 0) return;
        snapshot = new UiFadeLayer();
        try {
            snapshot.capture();
            destination = event.getNewScreen();
            startedAt = Util.getMillis();
        } catch (RuntimeException exception) {
            clear();
            LogUtils.getLogger().warn("Unable to capture death-screen transition", exception);
        }
    }

    public static void render(GuiGraphics graphics, Screen screen) {
        if (snapshot == null) return;
        double strength = Minecraft.getInstance().options.screenEffectScale().get();
        if (screen != destination || !FlatlinedPresentation.confirmationEnabled(screen) || strength <= 0) {
            clear();
            return;
        }
        double progress = Math.clamp((Util.getMillis() - startedAt) / (360.0 * strength), 0, 1);
        if (progress >= 1) {
            clear();
            return;
        }
        float opacity = (float) (1 - progress * progress * (3 - 2 * progress));
        snapshot.drawCaptured(graphics, opacity);
    }

    public static void onClosing(ScreenEvent.Closing event) {
        if (event.getScreen() == destination) clear();
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        if (snapshot != null) {
            snapshot.close();
            snapshot = null;
        }
        destination = null;
    }
}
