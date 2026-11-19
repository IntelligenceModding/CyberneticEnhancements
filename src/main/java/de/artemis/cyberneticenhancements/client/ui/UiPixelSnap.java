package de.artemis.cyberneticenhancements.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class UiPixelSnap {
    private UiPixelSnap() {
    }

    public static void push(GuiGraphics graphics, int x, int y) {
        graphics.pose().pushPose();
        var pose = graphics.pose().last().pose();
        var projection = RenderSystem.getProjectionMatrix();
        var window = Minecraft.getInstance().getWindow();
        double scaleX = window.getWidth() * projection.m00() / 2.0;
        double scaleY = -window.getHeight() * projection.m11() / 2.0;
        double pixelX = (pose.m00() * x + pose.m30()) * scaleX + (projection.m30() + 1) * window.getWidth() / 2.0;
        double pixelY = (pose.m11() * y + pose.m31()) * scaleY + (1 - projection.m31()) * window.getHeight() / 2.0;
        graphics.pose().translate(offset(pixelX, scaleX * pose.m00()), offset(pixelY, scaleY * pose.m11()), 0);
    }

    public static double offset(double pixelOrigin, double pixelsPerUnit) {
        return Math.abs(pixelsPerUnit) < 0.000001 ? 0 : (Math.rint(pixelOrigin) - pixelOrigin) / pixelsPerUnit;
    }
}
