package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ResponsiveScreen extends Screen implements ResponsiveUi {
    protected ResponsiveScreen(Component title) {
        super(title);
    }

    @Override
    public UiViewport viewport() {
        return ResponsiveUi.referenceViewport();
    }

    @Override
    protected void init() {
        width = viewport().width();
        height = viewport().height();
        super.init();
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.flush();
        GuiGraphics ui = graphics(graphics);
        try {
            renderContent(ui, (int) viewport().toUi(mouseX), (int) viewport().toUi(mouseY), partialTick);
        } finally {
            ui.flush();
        }
    }

    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xB5081018, 0xD0081018);
    }

    @Override
    public final boolean mouseClicked(double x, double y, int button) {
        return mouseClickedContent(viewport().toUi(x), viewport().toUi(y), button);
    }

    protected boolean mouseClickedContent(double x, double y, int button) {
        return super.mouseClicked(x, y, button);
    }

    @Override
    public final boolean mouseReleased(double x, double y, int button) {
        return mouseReleasedContent(viewport().toUi(x), viewport().toUi(y), button);
    }

    protected boolean mouseReleasedContent(double x, double y, int button) {
        return super.mouseReleased(x, y, button);
    }

    @Override
    public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return mouseDraggedContent(viewport().toUi(x), viewport().toUi(y), button, viewport().toUi(dx), viewport().toUi(dy));
    }

    protected boolean mouseDraggedContent(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public final boolean mouseScrolled(double x, double y, double dx, double dy) {
        return mouseScrolledContent(viewport().toUi(x), viewport().toUi(y), dx, dy);
    }

    protected boolean mouseScrolledContent(double x, double y, double dx, double dy) {
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(viewport().toUi(x), viewport().toUi(y));
    }
}
