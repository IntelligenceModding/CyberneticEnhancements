package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class UiGraphics extends GuiGraphics {
    private final UiViewport viewport;

    public UiGraphics(GuiGraphics source, UiViewport viewport) {
        super(Minecraft.getInstance(), source.bufferSource());
        this.viewport = viewport;
        pose().last().pose().set(source.pose().last().pose());
        pose().last().normal().set(source.pose().last().normal());
        pose().scale((float) viewport.renderScale(), (float) viewport.renderScale(), 1);
    }

    @Override
    public int guiWidth() {
        return viewport.width();
    }

    @Override
    public int guiHeight() {
        return viewport.height();
    }

    @Override
    public void enableScissor(int x1, int y1, int x2, int y2) {
        super.enableScissor(viewport.toScreenFloor(x1), viewport.toScreenFloor(y1),
                viewport.toScreenCeil(x2), viewport.toScreenCeil(y2));
    }

    @Override
    public boolean containsPointInScissor(int x, int y) {
        return super.containsPointInScissor(viewport.toScreenFloor(x), viewport.toScreenFloor(y));
    }
}
