package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import net.minecraft.client.gui.GuiGraphics;

final class WikiFrameRenderer {
    private WikiFrameRenderer() {
    }

    static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        UiRenderer.panel(graphics, x, y, width, height, fill);
    }

    static void drawPanelWithAccentTop(GuiGraphics graphics, int x, int y, int width, int height, int fill, int accent) {
        UiRenderer.panel(graphics, x, y, width, height, fill, accent);
    }
}
