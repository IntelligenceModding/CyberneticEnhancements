package de.artemis.cyberneticenhancements.client.screen;

import net.minecraft.client.Minecraft;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveUi;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiMotion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class WikiLaunchButton extends AbstractButton {
    private final java.util.function.Consumer<String> onPress;
    private final Component archiveMessage;
    private String entryId;
    private final net.minecraft.client.gui.screens.Screen owner;
    private final UiMotion highlight = new UiMotion();

    public WikiLaunchButton(net.minecraft.client.gui.screens.Screen owner, int x, int y, int width, int height, Component message, java.util.function.Consumer<String> onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.owner = owner;
        this.archiveMessage = message;
        float textScale = owner instanceof ResponsiveUi ? 1 : (float) ResponsiveUi.referenceViewport().renderScale();
        setWidth(Math.max(width, (int) Math.ceil((Minecraft.getInstance().font.width(entryMessage()) + 12) * textScale)));
        updatePlacement();
    }

    public void updatePlacement() {
        var occupied = new java.util.ArrayList<de.artemis.cyberneticenhancements.client.ui.ArchiveButtonPlacement.Bounds>();
        double scale = owner instanceof ResponsiveUi responsive ? responsive.viewport().renderScale() : 1;
        if (owner instanceof de.artemis.cyberneticenhancements.client.ui.ResponsiveContainerScreen<?> station && station.usesMinecraftStationScale()) {
            occupied.add(bounds(station.getGuiLeft(), station.getGuiTop(), station.getXSize(), station.machineGuiHeight(), scale));
            var inventory = station.nativeInventory();
            occupied.add(bounds(inventory.x(), inventory.y(), 176, 100, scale));
        } else if (owner instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container) {
            occupied.add(bounds(container.getGuiLeft(), container.getGuiTop(), container.getXSize(), container.getYSize(), scale));
        }
        // JEI's bookmark/config buttons and search controls ignore ingredient-list exclusion areas.
        if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
            int strip = (int) Math.ceil(28 / scale);
            occupied.add(new de.artemis.cyberneticenhancements.client.ui.ArchiveButtonPlacement.Bounds(0, owner.height - strip, owner.width, strip));
        }
        for (var child : owner.children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                && widget != this && widget.visible)
            occupied.add(new de.artemis.cyberneticenhancements.client.ui.ArchiveButtonPlacement.Bounds(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
        var placement = de.artemis.cyberneticenhancements.client.ui.ArchiveButtonPlacement.find(owner.width, owner.height, width, height, occupied);
        visible = placement.isPresent();
        placement.ifPresent(position -> { setX(position.x()); setY(position.y()); });
        if (!visible) setFocused(false);
    }

    private static de.artemis.cyberneticenhancements.client.ui.ArchiveButtonPlacement.Bounds bounds(int x, int y, int width, int height, double scale) {
        int left = (int) Math.floor(x / scale), top = (int) Math.floor(y / scale);
        return new de.artemis.cyberneticenhancements.client.ui.ArchiveButtonPlacement.Bounds(left, top,
                (int) Math.ceil((x + width) / scale) - left, (int) Math.ceil((y + height) / scale) - top);
    }

    private static Component entryMessage() {
        return Component.translatable("screen.cyberneticenhancements.archive.hover_hint",
                de.artemis.cyberneticenhancements.client.ModKeyMappings.OPEN_ARCHIVE.getTranslatedKeyMessage());
    }

    private void updateContext() {
        String hovered = de.artemis.cyberneticenhancements.client.ClientModEvents.hoveredArchiveEntry(owner);
        var minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        if (owner instanceof ResponsiveUi responsive) {
            mouseX = responsive.viewport().toUi(mouseX);
            mouseY = responsive.viewport().toUi(mouseY);
        }
        // Keep the selected entry while moving onto its button, so clicking opens that entry.
        if (hovered != null || !isMouseOver(mouseX, mouseY)) entryId = hovered;
        setMessage(entryId == null ? archiveMessage : entryMessage());
    }

    public static void updateForScreen(net.minecraft.client.gui.screens.Screen screen) {
        for (var child : screen.children()) if (child instanceof WikiLaunchButton button) {
            button.updateContext();
            button.updatePlacement();
        }
    }

    public static java.util.List<net.minecraft.client.renderer.Rect2i> jeiExclusions(net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null) return java.util.List.of();
        updateForScreen(screen);
        for (var child : screen.children()) if (child instanceof WikiLaunchButton button && button.visible) {
            double scale = screen instanceof ResponsiveUi responsive ? responsive.viewport().renderScale() : 1;
            int x = (int) Math.floor(button.getX() * scale) - 2, y = (int) Math.floor(button.getY() * scale) - 2;
            return java.util.List.of(new net.minecraft.client.renderer.Rect2i(x, y,
                    (int) Math.ceil(button.getWidth() * scale) + 4, (int) Math.ceil(button.getHeight() * scale) + 4));
        }
        return java.util.List.of();
    }

    @Override
    public void onPress() {
        onPress.accept(entryId);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateContext();
        updatePlacement();
        if (!visible) return;
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        float scale = minecraft.screen instanceof ResponsiveUi ? 1 : (float) ResponsiveUi.referenceViewport().renderScale();
        int w = Math.round(width / scale);
        int h = Math.round(height / scale);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX(), getY(), 0);
        guiGraphics.pose().scale(scale, scale, 1);
        UiRenderer.action(guiGraphics, 0, 0, w, h, active, highlight.update(active && UiRenderer.highlighted(this)));
        guiGraphics.drawString(font, getMessage(), (w - font.width(getMessage())) / 2, (h - font.lineHeight) / 2,
                active ? UiRenderer.TEXT : UiRenderer.TEXT_SECONDARY, false);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
