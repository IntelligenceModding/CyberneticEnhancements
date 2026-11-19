package de.artemis.cyberneticenhancements.client.ui;

import de.artemis.cyberneticenhancements.client.LaunchIntroAudio;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A presentation layer over the real title menu, with no screen lifecycle of its own. */
public final class LaunchIntroOverlay implements ResponsiveUi, AutoCloseable {
    private final LaunchIntroTimeline timeline = new LaunchIntroTimeline();
    private final UiFadeLayer layer = new UiFadeLayer();
    private LaunchIntroTimeline.Phase announced;
    private final LaunchIntroAudio startupAudio = new LaunchIntroAudio();
    private boolean skipping;
    private boolean skipRequested;

    @Override public UiViewport viewport() {
        var window = Minecraft.getInstance().getWindow();
        return ResponsiveUi.referenceViewport().coveringGui(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    public boolean finished() { return timeline.phase() == LaunchIntroTimeline.Phase.FINISHED; }

    public void render(GuiGraphics source) {
        var mc = Minecraft.getInstance();
        source.flush();
        // The title's glyphs write depth. Start a fresh GUI layer so they cannot
        // occlude the opaque intro backdrop or its logos/text.
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        // Allocate the compositing target early, before the automatic menu handoff.
        layer.prepare();
        GuiGraphics graphics = graphics(source);
        try {
            if (mc.getOverlay() != null) {
                startupAudio.positionMillis(false);
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
                return;
            }
            boolean focused = mc.isWindowActive();
            // Render time, rather than the 20 Hz game tick, keeps fades fluid at high FPS.
            timeline.advance(Util.getMillis(), focused, startupAudio.positionMillis(focused));
            var phase = timeline.phase();
            if (phase != announced) {
                announced = phase;
                mc.getNarrator().sayNow(Component.translatable("screen.cyberneticenhancements.intro.narration."
                        + phase.name().toLowerCase(java.util.Locale.ROOT)));
            }
            if (phase == LaunchIntroTimeline.Phase.FINISHED) return;
            if (phase == LaunchIntroTimeline.Phase.ENTERING) {
                LaunchIntroPresentation.renderMenuTransition(graphics, timeline, layer, skipping);
            } else {
                LaunchIntroPresentation.render(graphics, timeline);
                if (skipRequested) {
                    // Capture just the page; the persistent Escape hint must never
                    // enter the texture that will be faded or torn on the next frame.
                    graphics.flush();
                    layer.capture();
                    timeline.skipPresentation();
                    skipping = true;
                    skipRequested = false;
                }
            }
            LaunchIntroPresentation.renderSkipHint(graphics);
            graphics.flush();
            LaunchIntroPresentation.renderCopyrightNotices(source);
        } finally {
            graphics.flush();
            // Leave depth clean for the native menu's next frame, which is also
            // the source of the incoming glitch strips. Opaque intro glyphs and
            // fills must not occlude that menu before its render event reaches us.
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }
    }

    public void keyPressed(int key) {
        if (Minecraft.getInstance().getOverlay() != null) return;
        if (key == GLFW.GLFW_KEY_ESCAPE && timeline.phase().ordinal() < LaunchIntroTimeline.Phase.ENTERING.ordinal()) {
            skipRequested = true;
            startupAudio.close();
        }
    }

    @Override public void close() {
        startupAudio.close();
        layer.close();
    }
}
