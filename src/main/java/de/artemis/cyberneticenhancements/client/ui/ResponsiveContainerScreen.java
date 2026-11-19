package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import de.artemis.cyberneticenhancements.common.menu.StationInventoryLayout;

public abstract class ResponsiveContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements ResponsiveUi {
    protected double preciseRenderMouseX, preciseRenderMouseY;
    protected boolean renderingContent;
    private int stationWidth, stationHeight, firstPlayerInventorySlot = -1;
    private StationInventoryLayout stationInventory;
    private static final net.minecraft.resources.ResourceLocation STATION_INVENTORY_TEXTURE = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "cyberneticenhancements", "textures/gui/player_inventory.png");
    protected ResponsiveContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected final void useMinecraftStationScale(int machineWidth, int machineHeight, int firstInventorySlot) {
        stationWidth = imageWidth = machineWidth;
        stationHeight = imageHeight = machineHeight;
        firstPlayerInventorySlot = firstInventorySlot;
        stationInventory = firstInventorySlot < 0 ? null : new StationInventoryLayout(machineWidth, machineHeight);
    }

    public final boolean usesMinecraftStationScale() { return stationWidth > 0; }
    public final int machineGuiHeight() { return viewport().toScreenCeil(stationHeight); }
    public final boolean isPlayerInventorySlot(Slot slot) {
        return firstPlayerInventorySlot >= 0 && slot.index >= firstPlayerInventorySlot;
    }
    public final NativeStationInventory nativeInventory() {
        return NativeStationInventory.at(stationInventory, leftPos, topPos, viewport().renderScale());
    }
    public final int nativeFloatingX(int x) { return (int) Math.round((leftPos + x + 8) * viewport().renderScale() - 8); }
    public final int nativeFloatingY(int y) { return (int) Math.round((topPos + y + 8) * viewport().renderScale() - 8); }

    private GuiGraphics nativeGraphics(GuiGraphics parent) {
        parent.flush();
        return new GuiGraphics(net.minecraft.client.Minecraft.getInstance(), parent.bufferSource());
    }

    private GuiGraphics nativeInventoryGraphics(GuiGraphics parent) {
        GuiGraphics nativeGui = nativeGraphics(parent);
        var panel = nativeInventory();
        nativeGui.pose().translate(panel.x() - stationInventory.x(), panel.y() - stationInventory.y(), 0);
        return nativeGui;
    }

    @Override
    public UiViewport viewport() {
        if (usesMinecraftStationScale()) {
            var window = net.minecraft.client.Minecraft.getInstance().getWindow();
            return StationGuiScale.fit(window.getGuiScaledWidth(), window.getGuiScaledHeight(), stationWidth, stationHeight, stationInventory != null);
        }
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
        if (stationInventory != null)
            imageHeight = stationInventory.y() + (int) Math.ceil(StationInventoryLayout.HEIGHT / viewport().renderScale());
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
        preciseRenderMouseX = viewport().toUi(mouseX);
        preciseRenderMouseY = viewport().toUi(mouseY);
        renderingContent = true;
        try {
            renderContent(ui, (int) preciseRenderMouseX, (int) preciseRenderMouseY, partialTick);
        } finally {
            renderingContent = false;
            ui.flush();
        }
    }

    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (isPlayerInventorySlot(slot)) {
            GuiGraphics nativeGui = nativeInventoryGraphics(graphics);
            super.renderSlot(nativeGui, slot);
            nativeGui.flush();
        } else super.renderSlot(graphics, slot);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (isPlayerInventorySlot(slot)) {
            if (slot.isHighlightable()) {
                GuiGraphics nativeGui = nativeInventoryGraphics(graphics);
                renderSlotHighlight(nativeGui, slot.x, slot.y, 0, getSlotColor(slot.index));
                nativeGui.flush();
            }
            return;
        }
        if (slot.isHighlightable()) {
            renderSlotHighlight(graphics, slot.x, slot.y, 0, 0x408BDAF8);
            UiRenderer.outline(graphics, slot.x - 1, slot.y - 1, 18, 18, UiRenderer.SELECTED_EDGE);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xB5081018, 0xD0081018);
        UiRenderer.titlePlate(graphics, font, leftPos, Math.max(4, topPos - 22),
                Math.max(1, imageWidth - UiCloseControl.WIDTH - 12), title);
        renderBg(graphics, partialTick, mouseX, mouseY);
        if (stationInventory != null) {
            GuiGraphics nativeGui = nativeGraphics(graphics);
            var panel = nativeInventory();
            nativeGui.blit(STATION_INVENTORY_TEXTURE, panel.x(), panel.y(), 0F, 0F,
                    StationInventoryLayout.WIDTH, StationInventoryLayout.HEIGHT, 256, 256);
            nativeGui.drawString(font, Component.translatable("container.inventory"), panel.x() + 8, panel.y() + 7, UiRenderer.TEXT, false);
            nativeGui.flush();
        }
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (stationInventory != null && width == 16 && height == 16
                && x >= stationInventory.inventoryX() && x <= stationInventory.inventoryX() + 8 * 18
                && y >= stationInventory.inventoryY() && y <= stationInventory.hotbarY())
            return nativeInventory().hovering(x, y, renderingContent ? preciseRenderMouseX : mouseX,
                    renderingContent ? preciseRenderMouseY : mouseY, viewport().renderScale());
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        if (!usesMinecraftStationScale()) return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
        boolean outsideMachine = mouseX < leftPos || mouseX >= leftPos + stationWidth
                || mouseY < topPos || mouseY >= topPos + stationHeight;
        return outsideMachine && (stationInventory == null || !nativeInventory().contains(mouseX, mouseY, viewport().renderScale()));
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot != null && isPlayerInventorySlot(hoveredSlot)) {
            GuiGraphics nativeGui = nativeGraphics(graphics);
            double scale = viewport().renderScale();
            super.renderTooltip(nativeGui, (int) Math.round((renderingContent ? preciseRenderMouseX : mouseX) * scale),
                    (int) Math.round((renderingContent ? preciseRenderMouseY : mouseY) * scale));
            nativeGui.flush();
        } else super.renderTooltip(graphics, mouseX, mouseY);
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
