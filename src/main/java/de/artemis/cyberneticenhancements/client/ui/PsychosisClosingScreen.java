package de.artemis.cyberneticenhancements.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Fade a frozen frame: the closed menu cannot receive input or execute rendering code. */
public final class PsychosisClosingScreen extends Screen {
    private final ResourceLocation texture;
    private final long started = net.minecraft.Util.getMillis();
    public PsychosisClosingScreen() {
        super(Component.empty());
        var mc = Minecraft.getInstance();
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        texture = mc.getTextureManager().register("psychosis_closing", new DynamicTexture(image));
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean mouseClicked(double x, double y, int button) { return true; }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) minecraft.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
        return true;
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        float progress = Math.clamp((net.minecraft.Util.getMillis() - started) / 260F, 0, 1);
        if (progress >= 1) { minecraft.setScreen(null); return; }
        float alpha = 1 - progress * progress * (3 - 2 * progress);
        g.flush();
        float[] color = RenderSystem.getShaderColor().clone();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            RenderSystem.setShaderColor(1, 1, 1, alpha);
            g.blit(texture, 0, 0, 0F, 0F, width, height, width, height);
            g.flush();
        } finally {
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.disableBlend();
        }
    }
    @Override public void removed() { Minecraft.getInstance().getTextureManager().release(texture); }
}
