package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.LaunchIntroGate;
import de.artemis.cyberneticenhancements.client.ui.LaunchIntroOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class LaunchIntro {
    private static final LaunchIntroGate GATE = new LaunchIntroGate();
    private static TitleScreen title;
    private static LaunchIntroOverlay overlay;
    private LaunchIntro() {}

    public static void onOpening(ScreenEvent.Opening event) {
        var mc = Minecraft.getInstance();
        var next = event.getNewScreen();
        boolean enabled = ClientConfig.SPEC.isLoaded() && ClientConfig.SHOW_LAUNCH_INTRO.get();
        if (GATE.claim(enabled, mc.level != null, next != null && next.getClass() == TitleScreen.class)) {
            title = (TitleScreen) next;
            overlay = new LaunchIntroOverlay();
            TitleSlideshow.prepare();
        }
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level != null) GATE.gameplayStarted();
        if (overlay != null && (Minecraft.getInstance().screen != title || !ClientConfig.SHOW_LAUNCH_INTRO.get())) clear();
    }

    public static void onMusic(SelectMusicEvent event) {
        if (active(Minecraft.getInstance().screen)) event.overrideMusic(null);
    }

    private static boolean active(net.minecraft.client.gui.screens.Screen screen) {
        return overlay != null && screen == title;
    }

    public static boolean isPlaying() { return overlay != null; }

    /** LoadingOverlay calls Screen.render directly, bypassing ScreenEvent.Render.Post. */
    public static boolean coverLoadingHandoff(net.minecraft.client.gui.screens.Screen screen,
                                              net.minecraft.client.gui.GuiGraphics graphics) {
        if (!active(screen) || !(Minecraft.getInstance().getOverlay() instanceof net.minecraft.client.gui.screens.LoadingOverlay)) return false;
        graphics.flush();
        // The loader supplies its fade as the global shader alpha; the backing layer must stay opaque.
        float[] color = com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            graphics.fill(net.minecraft.client.renderer.RenderType.guiOverlay(), 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
            graphics.flush();
        } finally {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        }
        return true;
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!active(event.getScreen())) return;
        overlay.render(event.getGuiGraphics());
        if (overlay.finished()) clear();
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!active(event.getScreen())) return;
        overlay.keyPressed(event.getKeyCode());
        event.setCanceled(true);
    }

    public static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (!active(event.getScreen())) return;
        event.setCanceled(true);
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!active(event.getScreen())) return;
        event.setCanceled(true);
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (active(event.getScreen())) event.setCanceled(true);
    }

    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (active(event.getScreen())) event.setCanceled(true);
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (active(event.getScreen())) event.setCanceled(true);
    }

    public static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (active(event.getScreen())) event.setCanceled(true);
    }

    public static void onClosing(ScreenEvent.Closing event) {
        if (active(event.getScreen())) clear();
    }

    private static void clear() {
        if (overlay != null) overlay.close();
        overlay = null;
        title = null;
    }
}
