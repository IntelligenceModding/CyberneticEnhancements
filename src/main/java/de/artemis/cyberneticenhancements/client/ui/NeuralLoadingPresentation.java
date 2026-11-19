package de.artemis.cyberneticenhancements.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class NeuralLoadingPresentation {
    public static final int BACKGROUND = 0xFF090408;
    private static final ResourceLocation MOJANG_LOGO = ResourceLocation.withDefaultNamespace("textures/gui/title/mojangstudios.png");
    private NeuralLoadingPresentation() {}

    public static boolean enabled() {
        return ClientConfig.SPEC.isLoaded() && ClientConfig.CUSTOM_LOADING_SCREEN.get();
    }

    public static float opacity(long now, long fadeOutStart, long fadeInStart, boolean fadeIn) {
        if (fadeOutStart >= 0) return Math.clamp(2 - (now - fadeOutStart) / 1000.0F, 0, 1);
        return fadeIn && fadeInStart >= 0 ? Math.clamp((now - fadeInStart) / 500.0F, 0.15F, 1) : 1;
    }

    private static int color(int rgb, float alpha) {
        return Math.clamp(Math.round(alpha * 255), 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    public static void render(GuiGraphics graphics, float progress, float opacity, double seconds) {
        if (opacity <= 0.02F) return;
        var mc = Minecraft.getInstance();
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        int center = width / 2;
        double motion = mc.options.screenEffectScale().get();
        graphics.fill(0, 0, width, height, color(BACKGROUND, opacity));
        graphics.fillGradient(0, 0, width, height, color(0x1E0006, opacity * 0.4F), color(0x39000B, opacity * 0.7F));
        for (int y = 0; y < height; y += 4)
            graphics.fill(0, y, width, y + 1, color(0, opacity * 0.15F));
        if (motion > 0) {
            int sweep = (int) (seconds * 60 * motion) % (height + 20) - 20;
            graphics.fillGradient(0, sweep, width, sweep + 20, 0, color(0xE83E59, opacity * 0.12F));
        }

        // Match Minecraft's original logo proportions and two-part texture mapping.
        double logoHeight = Math.min(width * 0.75, height) * 0.25;
        int halfWidth = (int) (logoHeight * 2);
        int logoTop = height / 2 - (int) (logoHeight * 0.5);
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        graphics.setColor(1.0F, 0.32F, 0.40F, opacity);
        try {
            graphics.blit(MOJANG_LOGO, center - halfWidth, logoTop, halfWidth, (int) logoHeight,
                    -0.0625F, 0.0F, 120, 60, 120, 120);
            graphics.blit(MOJANG_LOGO, center, logoTop, halfWidth, (int) logoHeight,
                    0.0625F, 60.0F, 120, 60, 120, 120);
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }

        // The original continuous bar and rectangular frame, recolored to match the mod.
        int left = center - halfWidth;
        int right = center + halfWidth;
        int top = (int) (height * 0.8325) - 5;
        int bottom = top + 10;
        int accent = color(0xFF5266, opacity);
        int filled = Math.max(0, (int) Math.ceil((right - left - 4) * Math.clamp(progress, 0, 1)));
        if (filled > 0) graphics.fill(left + 2, top + 2, left + 2 + filled, bottom - 2, accent);
        graphics.fill(left + 1, top, right - 1, top + 1, accent);
        graphics.fill(left + 1, bottom - 1, right - 1, bottom, accent);
        graphics.fill(left, top + 1, left + 1, bottom - 1, accent);
        graphics.fill(right - 1, top + 1, right, bottom - 1, accent);
    }
}
