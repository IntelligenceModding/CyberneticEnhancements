package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.DisconnectPresentation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Bridges the pause menu, vanilla's synchronous save, and the menu Minecraft opens after quitting. */
public final class DisconnectAnimation {
    private static final long FINAL_FRAME_GRACE_MILLIS = 250;
    private record Closing(Screen screen, ClientLevel level, Runnable disconnect, long start) {}
    private record Opening(Screen screen, long start) {}

    private static Closing closing;
    private static boolean closingCovered;
    private static boolean disconnecting;
    private static Opening opening;

    private DisconnectAnimation() {}

    public static void begin(Runnable disconnect) {
        if (closing != null || disconnecting) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.screen == null || mc.getOverlay() != null || !JoinAnimation.enabled()) {
            disconnect.run();
            return;
        }
        closingCovered = false;
        closing = new Closing(mc.screen, mc.level, disconnect, Util.getMillis());
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (closing == null && opening == null) return;
        var mc = Minecraft.getInstance();
        long now = Util.getMillis();
        if (opening != null) {
            if (mc.screen != opening.screen()) opening = null;
            else if (mc.getOverlay() != null && opening.start() >= 0) opening = new Opening(opening.screen(), -1);
        }
        if (closing == null) return;
        if (mc.level != closing.level()) {
            closing = null;
            return;
        }
        // Let the final covered frame draw first, with a time limit if rendering is suspended.
        if (mc.screen == closing.screen() && !closingCovered
                && now - closing.start() < DisconnectPresentation.CLOSE_MILLIS + FINAL_FRAME_GRACE_MILLIS) return;

        Runnable disconnect = closing.disconnect();
        closing = null;
        // Vanilla renders the saving screen and waits for the integrated server inside this call.
        disconnecting = true;
        try {
            disconnect.run();
        } finally {
            disconnecting = false;
        }
        if (mc.screen != null && mc.level == null) opening = new Opening(mc.screen, -1);
    }

    public static boolean isReturningToMenu() {
        return disconnecting || opening != null && Minecraft.getInstance().screen == opening.screen();
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen != Minecraft.getInstance().screen) return;
        if (disconnecting) {
            DisconnectPresentation.saving(event.getGuiGraphics(), screen instanceof GenericMessageScreen ? screen.getTitle() : null);
        } else if (closing != null && screen == closing.screen()) {
            long elapsed = Util.getMillis() - closing.start();
            DisconnectPresentation.close(event.getGuiGraphics(), elapsed);
            closingCovered = elapsed >= DisconnectPresentation.CLOSE_MILLIS;
        } else if (opening != null && screen == opening.screen()) {
            long now = Util.getMillis();
            if (opening.start() < 0) opening = new Opening(screen, now);
            long elapsed = now - opening.start();
            if (elapsed >= DisconnectPresentation.REVEAL_MILLIS) opening = null;
            else DisconnectPresentation.reveal(event.getGuiGraphics(), elapsed);
        }
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (closing != null && event.getScreen() == closing.screen()) event.setCanceled(true);
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (closing != null && event.getScreen() == closing.screen()) event.setCanceled(true);
    }
}
