package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public abstract class ResponsiveContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements ResponsiveUi {
    protected ResponsiveContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public UiViewport viewport() {
        return ResponsiveUi.referenceViewport();
    }

    @Override
    public int getGuiLeft() {
        return viewport().toScreenFloor(leftPos);
    }

    @Override
    public int getGuiTop() {
        return viewport().toScreenFloor(topPos);
    }

    @Override
    public int getXSize() {
        return viewport().toScreenCeil(imageWidth);
    }

    @Override
    public int getYSize() {
        return viewport().toScreenCeil(imageHeight);
    }

    @Override
    protected void init() {
        width = viewport().width();
        height = viewport().height();
        super.init();
        titleLabelX = 10000;
        inventoryLabelX = 10000;
        addRenderableWidget(new UiButton(leftPos + imageWidth - UiCloseControl.WIDTH,
                Math.max(4, topPos - 22), UiCloseControl.WIDTH, UiCloseControl.HEIGHT,
                UiCloseControl.label(), button -> onClose()));
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
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (slot.isHighlightable()) {
            renderSlotHighlight(graphics, slot.x, slot.y, 0, 0x408BDAF8);
            UiRenderer.outline(graphics, slot.x - 1, slot.y - 1, 18, 18, UiRenderer.ACCENT);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xB5081018, 0xD0081018);
        UiRenderer.titlePlate(graphics, font, leftPos, Math.max(4, topPos - 22),
                Math.max(1, imageWidth - UiCloseControl.WIDTH - 12), title);
        renderBg(graphics, partialTick, mouseX, mouseY);
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
