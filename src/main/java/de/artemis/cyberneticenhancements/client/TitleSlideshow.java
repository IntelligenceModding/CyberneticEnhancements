package de.artemis.cyberneticenhancements.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.client.ui.SlideshowFrame;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import de.artemis.cyberneticenhancements.client.ui.TitleScreenPresentation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public final class TitleSlideshow {
    private static final ResourceLocation[] IMAGES = IntStream.rangeClosed(1, 9)
            .mapToObj(index -> ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID,
                    "textures/title/slideshow/" + index + ".png")).toArray(ResourceLocation[]::new);
    private static final SlideshowFrame FRAME = new SlideshowFrame(IMAGES.length, new Random());
    private static CompletableFuture<Void> ready;
    private static long generation;
    private static Screen renderedScreen;

    private TitleSlideshow() {}

    public static boolean enabled() {
        return ClientConfig.SPEC.isLoaded() && ClientConfig.CUSTOM_TITLE_BACKGROUND.get();
    }

    public static void prepare() {
        if (!enabled() || ready != null) return;
        // Handles enabling the slideshow after the last resource reload.
        var mc = Minecraft.getInstance();
        long attempt = ++generation;
        var completion = new CompletableFuture<Void>();
        ready = completion;
        CompletableFuture.supplyAsync(() -> readSlides(mc.getResourceManager()), Util.backgroundExecutor())
                .thenAcceptAsync(slides -> {
                    if (attempt != generation) {
                        slides.close();
                        completion.complete(null);
                    } else install(slides, completion);
                }, mc).exceptionally(error -> {
                    completion.completeExceptionally(error);
                    LogUtils.getLogger().warn("Unable to prepare title slideshow; using the native panorama until the next resource reload", error);
                    return null;
                });
    }

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<PreparedSlides>() {
            @Override protected PreparedSlides prepare(ResourceManager resources, ProfilerFiller profiler) {
                return enabled() ? readSlides(resources) : new PreparedSlides(new NativeImage[0], null);
            }

            @Override protected void apply(PreparedSlides slides, ResourceManager resources, ProfilerFiller profiler) {
                // A new resource generation always gets a fresh attempt, including
                // after failure. Old async preparations cannot replace this batch.
                generation++;
                if (!enabled() || slides.images.length == 0) {
                    slides.close();
                    releaseSlides();
                    ready = null;
                    return;
                }
                ready = new CompletableFuture<>();
                install(slides, ready);
            }
        });
    }

    static PreparedSlides readSlides(ResourceManager resources) {
        var images = new NativeImage[IMAGES.length];
        try {
            for (int index = 0; index < IMAGES.length; index++) {
                try (var stream = resources.open(IMAGES[index])) {
                    images[index] = NativeImage.read(stream);
                }
            }
            return new PreparedSlides(images, null);
        } catch (java.io.IOException | RuntimeException error) {
            var failed = new PreparedSlides(images, error);
            failed.close();
            return failed;
        }
    }

    private static void install(PreparedSlides slides, CompletableFuture<Void> completion) {
        try (slides) {
            if (slides.error != null) {
                releaseSlides();
                completion.completeExceptionally(slides.error);
                LogUtils.getLogger().warn("Unable to load title slideshow; using the native panorama until the next resource reload", slides.error);
                return;
            }
            var textures = Minecraft.getInstance().getTextureManager();
            for (int index = 0; index < IMAGES.length; index++) {
                var texture = new DynamicTexture(slides.images[index]);
                slides.images[index] = null; // TextureManager now owns the image.
                textures.register(IMAGES[index], texture);
            }
            completion.complete(null);
        } catch (RuntimeException error) {
            releaseSlides();
            completion.completeExceptionally(error);
            LogUtils.getLogger().warn("Unable to upload title slideshow; using the native panorama until the next resource reload", error);
        }
    }

    private static void releaseSlides() {
        var textures = Minecraft.getInstance().getTextureManager();
        for (var image : IMAGES) textures.release(image);
    }

    record PreparedSlides(NativeImage[] images, Throwable error) implements AutoCloseable {
        @Override public void close() {
            for (int index = 0; index < images.length; index++) {
                if (images[index] != null) images[index].close();
                images[index] = null;
            }
        }
    }

    public static boolean render(GuiGraphics graphics, float fade) {
        if (!transitionReady()) return false;
        // Menu foregrounds use different responsive scales. The backdrop always uses
        // Minecraft's native full-window projection, independent of those transforms.
        graphics = nativeGraphics(graphics);
        renderedScreen = Minecraft.getInstance().screen;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
        drawSlides(graphics, fade);
        RenderSystem.disableDepthTest();
        TitleScreenPresentation.background(graphics, Minecraft.getInstance().screen, fade);
        return true;
    }

    public static boolean transitionReady() {
        prepare();
        return artworkCreditVisible();
    }

    /** Uses the current menu artwork and clock without changing which screen owns the panorama. */
    public static boolean renderTransition(GuiGraphics graphics, float opacity) {
        if (!transitionReady()) return false;
        if (opacity > 0) {
            drawSlides(graphics, opacity);
            TitleScreenPresentation.backdropEffects(graphics, opacity, false);
        }
        return true;
    }

    private static void drawSlides(GuiGraphics graphics, float opacity) {
        FRAME.advance(Util.getMillis());
        var crop = SlideshowFrame.cover(2560, 1440, graphics.guiWidth(), graphics.guiHeight());
        float blend = Minecraft.getInstance().options.screenEffectScale().get() <= 0 ? 0 : FRAME.blend();
        float nextOpacity = blend * opacity;
        // Draw the two images as a single dissolve over the live scene.
        float currentOpacity = nextOpacity < 1 ? (opacity - nextOpacity) / (1 - nextOpacity) : 0;
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            if (currentOpacity > 0) draw(graphics, IMAGES[FRAME.current()], crop, currentOpacity);
            if (nextOpacity > 0) draw(graphics, IMAGES[FRAME.next()], crop, nextOpacity);
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }

    public static GuiGraphics nativeGraphics(GuiGraphics source) {
        source.flush();
        return new GuiGraphics(Minecraft.getInstance(), source.bufferSource());
    }

    public static void beginScreenFrame() { renderedScreen = null; }

    /** Only suppress native background filters after our backdrop was actually drawn this frame. */
    public static boolean renderedForCurrentScreen() {
        var mc = Minecraft.getInstance();
        return mc.level == null && renderedScreen != null && renderedScreen == mc.screen && artworkCreditVisible();
    }

    private static void draw(GuiGraphics graphics, ResourceLocation image, SlideshowFrame.Crop crop, float alpha) {
        graphics.setColor(1, 1, 1, Math.clamp(alpha, 0, 1));
        graphics.blit(image, 0, 0, graphics.guiWidth(), graphics.guiHeight(),
                crop.x(), crop.y(), crop.width(), crop.height(), 2560, 1440);
    }

    public static boolean artworkCreditVisible() {
        return enabled() && ready != null && ready.isDone() && !ready.isCompletedExceptionally();
    }

    record AudioFrame(int slide, float blend) {}

    /** Keep the shared image/audio clock running even when a child menu has no panorama. */
    static AudioFrame advanceAudio(long now) {
        FRAME.advance(now);
        return new AudioFrame(FRAME.current(), FRAME.blend());
    }
}
