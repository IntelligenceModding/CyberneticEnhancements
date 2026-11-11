package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.menu.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import static de.artemis.cyberneticenhancements.client.screen.WorkshopScreenParts.*;

 public final class RecyclerStationScreen extends ResponsiveContainerScreen<RecyclerStationMenu> {
    private UiButton recycle, capacity;
    public RecyclerStationScreen(RecyclerStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = RecyclerStationLayout.IMAGE_WIDTH;
        imageHeight = RecyclerStationLayout.IMAGE_HEIGHT;
    }
    protected void init() {
        super.init();
        capacity = addRenderableWidget(new UiButton(leftPos + 386, topPos + 18, 130, 20, tr("capacity_button"), b -> send(1)));
        recycle = addRenderableWidget(new UiButton(leftPos + 12, topPos + 250, 196, 20, tr("recycle"), b -> send(0)));
    }
    private void send(int action) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        frame(g, leftPos, topPos);
        for (var slot : menu.slots)
            StationSlotRenderer.drawStandardSlot(g, leftPos, topPos, slot.x, slot.y, UiRenderer.FIELD);
        var preview = menu.getDisplayedResultStacks();
        for (int i = 0; i < 3; i++) {
            int x = leftPos + 228 + i * 100;
            UiRenderer.panel(g, x, topPos + 88, 90, 112, UiRenderer.ROW, UiRenderer.FIELD_EDGE);
            if (i < preview.size() && !preview.get(i).isEmpty()) {
                g.renderFakeItem(preview.get(i), x + 37, topPos + 102);
                g.renderItemDecorations(font, preview.get(i), x + 37, topPos + 102);
            }
        }
    }
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        line(g, tr("capacity", menu.getSupportedTier().ordinal() + 1), 22, 24, 352, UiRenderer.ACCENT);
        line(g, tr("scrap"), 22, 64, 176, UiRenderer.TEXT);
        line(g, tr(menu.hasPendingResultStacks() ? "salvage_complete" : "salvage_preview"), 230, 64, 286, UiRenderer.TEXT);
        var input = menu.getInputStack();
        wrap(g, font, input.isEmpty() ? tr("insert_scrap") : input.getHoverName(), 56, 86, 140, 3, UiRenderer.TEXT);
        if (input.getItem() instanceof CyberwareItem item) {
            double integrity = CyberwareConditionHelper.getIntegrityRatio(input, item.getDefinition());
            line(g, tr("integrity", Math.round(integrity * 100)), 22, 132, 176, UiRenderer.ACCENT);
            line(g, tr(integrity < 0.45 ? "condition_damaged" : integrity < 0.75 ? "condition_worn" : "condition_good"),
                    22, 150, 176, UiRenderer.TEXT_SECONDARY);
        }
        wrap(g, font, tr("scrap_warning"), 22, 178, 176, 4, UiRenderer.WARNING);
        var preview = menu.getDisplayedResultStacks();
        for (int i = 0; i < preview.size() && i < 3; i++) {
            if (preview.get(i).isEmpty()) continue;
            int x = 234 + i * 100;
            wrap(g, font, preview.get(i).getHoverName(), x, 132, 78, 3, UiRenderer.TEXT);
            line(g, tr("recovery_count", preview.get(i).getCount()), x, 178, 78, UiRenderer.ACCENT);
        }
        line(g, tr("collection_slots"), 230, 218, 286, UiRenderer.TEXT_SECONDARY);
        wrap(g, font, menu.status(), 22, 286, 496, 2, UiRenderer.TEXT_SECONDARY);
        line(g, playerInventoryTitle, 22, 332, 154, UiRenderer.TEXT);
        wrap(g, font, tr("recycle_note"), 22, 350, 154, 4, UiRenderer.TEXT_SECONDARY);
        wrap(g, font, tr("collection_help"), 364, 332, 152, 5, UiRenderer.TEXT_SECONDARY);
    }
    private void line(GuiGraphics g, Component text, int x, int y, int width, int color) {
        text(g, font, text, x, y, width, color);
    }
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partial) {
        recycle.active = menu.canRecycle();
        recycle.setSelected(recycle.active);
        recycle.setTooltip(Tooltip.create(menu.status()));
        capacity.active = menu.canUpgradeSupportedTier() && menu.hasRequiredUpgradeComponent();
        var required = menu.getRequiredUpgradeComponentStack();
        capacity.setTooltip(Tooltip.create(required.isEmpty() ? tr("capacity_max")
                : tr("capacity_cost", required.getHoverName(), menu.getNextSupportedTier().ordinal() + 1)));
        super.renderContent(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        var preview = menu.getDisplayedResultStacks();
        for (int i = 0; i < preview.size() && i < 3; i++)
            if (!preview.get(i).isEmpty() && isHovering(228 + i * 100, 88, 90, 112, mouseX, mouseY))
                g.renderTooltip(font, java.util.List.of(preview.get(i).getHoverName(),
                        tr(menu.hasPendingResultStacks() ? "recovered_count" : "preview_count", preview.get(i).getCount())),
                        java.util.Optional.empty(), mouseX, mouseY);
    }
}
