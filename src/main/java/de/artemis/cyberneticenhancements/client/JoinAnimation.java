package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.JoinAnimationTimeline;
import de.artemis.cyberneticenhancements.client.ui.JoinPresentation;
import de.artemis.cyberneticenhancements.client.ui.UiFadeLayer;
import de.artemis.cyberneticenhancements.common.registry.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Opens onto the live world and HUD after loading. It pauses while a menu is open and never blocks play.
 */
public final class JoinAnimation {
    private static final long PENDING_TIMEOUT_MILLIS = 20_000;
    private static boolean pending, running;
    private static long pendingSince, lastTime, elapsed;
    private static UiFadeLayer layer;

    private JoinAnimation() {}

    static boolean enabled() {
        // The tutorial runs its own presentation.
        return ClientConfig.SPEC.isLoaded() && ClientConfig.SHOW_JOIN_ANIMATION.get() && !TutorialClient.inTutorialWorld();
    }

    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        clear();
        pending = enabled();
        pendingSince = Util.getMillis();
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    private static void clear() {
        pending = running = false;
        elapsed = 0;
        lastTime = -1;
        if (layer != null) {
            layer.close();
            layer = null;
        }
    }

    /** Waits out the loading screens, so the animation opens onto a world that is really there. */
    public static void onTick(ClientTickEvent.Post event) {
        if (!pending) return;
        var mc = Minecraft.getInstance();
        if (Util.getMillis() - pendingSince > PENDING_TIMEOUT_MILLIS) {
            pending = false;
            return;
        }
        if (mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
        pending = false;
        running = true;
        lastTime = -1;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.JOIN_LINK.get(), 1.0F, 0.9F));
    }

    public static boolean isWorldLoadingScreen(Screen screen) {
        if (screen == null || !enabled()) return false;
        if (screen.getClass() == ReceivingLevelScreen.class) return pending;
        if (screen.getClass() == ConnectScreen.class || screen.getClass() == LevelLoadingScreen.class) return true;
        return screen.getClass() == GenericMessageScreen.class
                && screen.getTitle().getContents() instanceof TranslatableContents title
                && (title.getKey().equals("selectWorld.data_read") || title.getKey().equals("selectWorld.resource_load"));
    }

    public static void onLoadingRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        // The connect screen already draws the slideshow behind its Cancel button.
        if (screen != Minecraft.getInstance().screen || screen instanceof ConnectScreen
                || !isWorldLoadingScreen(screen) || !TitleSlideshow.transitionReady()) return;
        var graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 4000);
            TitleSlideshow.renderTransition(graphics, 1);
        } finally {
            graphics.pose().popPose();
        }
    }

    public static void onRender(RenderGuiEvent.Post event) {
        if (!running) return;
        var mc = Minecraft.getInstance();
        long now = Util.getMillis();
        // Time stands still behind a menu or while the window is unfocused, so nothing plays out unseen.
        if (lastTime >= 0 && mc.screen == null && mc.isWindowActive()) elapsed += Math.clamp(now - lastTime, 0, 100);
        lastTime = now;
        if (elapsed >= JoinAnimationTimeline.DURATION) {
            clear();
            return;
        }
        if (!GameplayHudVisibility.gameplay()) return;
        var frame = JoinAnimationTimeline.at(elapsed, mc.options.screenEffectScale().get());
        var graphics = event.getGuiGraphics();
        try {
            UiFadeLayer capturedScene = null;
            if (frame.glitch() > 0.01F && !TitleSlideshow.transitionReady()) {
                if (layer == null) layer = new UiFadeLayer();
                graphics.flush();
                layer.capture();
                capturedScene = layer;
            }
            JoinPresentation.render(graphics, frame, capturedScene, elapsed);
        } catch (RuntimeException failure) {
            // A cosmetic effect must never break a join; drop it and play on.
            com.mojang.logging.LogUtils.getLogger().warn("Join animation failed and was skipped", failure);
            clear();
        }
    }
}
