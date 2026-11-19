package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.client.render.GhostItemRenderer;
import de.artemis.cyberneticenhancements.common.menu.RecyclerStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import java.util.HashMap;
import java.util.Map;
import static de.artemis.cyberneticenhancements.common.menu.RecyclerStationLayout.*;
import static de.artemis.cyberneticenhancements.client.screen.WorkshopScreenParts.tr;

public final class RecyclerStationScreen extends ResponsiveContainerScreen<RecyclerStationMenu> {
    private final Map<Integer, UiButton> actions = new HashMap<>();
    private final WorkshopProgressAnimation recyclingAnimation = new WorkshopProgressAnimation();
    private String animationKey = "";
    public RecyclerStationScreen(RecyclerStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        useMinecraftStationScale(IMAGE_WIDTH, IMAGE_HEIGHT, MACHINE_SLOT_COUNT);
    }
    @Override protected void init() {
        super.init();
        actions.clear();
        action("recycle", PROGRESS_X, ACTION_WIDTH, 1);
        action("stop", PROGRESS_X + ACTION_WIDTH + 12, ACTION_WIDTH, 2);
    }
    private void action(String label, int x, int width, int id) {
        var button = new UiButton(leftPos + x, topPos + ACTION_Y, width, 20, tr(label), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        });
        button.setTooltip(null);
        actions.put(id, button);
        addRenderableWidget(button);
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        UiRenderer.panel(g, x, y, IMAGE_WIDTH, IMAGE_HEIGHT, StationScreenStyle.PANEL_DEEP);
        UiRenderer.panel(g, x + PANEL_X, y + PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT, StationScreenStyle.PANEL_BG);
        Component name = menu.getInputStack().isEmpty() ? tr("insert_scrap") : menu.getInputStack().getHoverName();
        Component time = tr("recycle_time");
        float headingScale = Math.min(1F, (PANEL_WIDTH - 16F) / (font.width(name) + 14F + font.width(time)));
        g.pose().pushPose();
        g.pose().translate(x + PANEL_X + 8, y + 20 + (20 - font.lineHeight * headingScale) / 2F, 0);
        g.pose().scale(headingScale, headingScale, 1);
        g.drawString(font, name, 0, 0, UiRenderer.TEXT, false);
        g.drawString(font, time.getVisualOrderText(), (PANEL_WIDTH - 16) / headingScale - font.width(time), 0F, UiRenderer.TEXT_SECONDARY, false);
        g.pose().popPose();
        centered(g, tr("recycler_input"), x + INPUT_FRAME_X + INPUT_FRAME_SIZE / 2, y + LABEL_Y, UiRenderer.TEXT_SECONDARY);
        centered(g, tr("recovered_materials"), x + GRID_X + 2 * GRID_STEP + 8, y + LABEL_Y, UiRenderer.TEXT_SECONDARY);
        UiRenderer.panel(g, x + INPUT_FRAME_X, y + INPUT_FRAME_Y, INPUT_FRAME_SIZE, INPUT_FRAME_SIZE,
                UiRenderer.FIELD, menu.getInputStack().isEmpty() ? UiRenderer.FIELD_EDGE : UiRenderer.SELECTED_EDGE);
        for (int i = 0; i < RESULT_SLOT_COUNT; i++)
            StationSlotRenderer.drawStandardSlot(g, x, y, resultX(i), resultY(i), StationScreenStyle.SLOT_BG);
        if (!menu.hasPendingResultStacks()) {
            var preview = menu.getRecyclePlan().outputs();
            for (int i = 0; i < Math.min(preview.size(), RESULT_SLOT_COUNT); i++) {
                GhostItemRenderer.render(g, preview.get(i), x + resultX(i), y + resultY(i));
                if (preview.get(i).getCount() > 1) {
                    String count = Integer.toString(preview.get(i).getCount());
                    g.drawString(font, count, x + resultX(i) + 17 - font.width(count), y + resultY(i) + 9,
                            UiRenderer.TEXT_SECONDARY, false);
                }
            }
        }
        renderProgress(g);
    }
    private void renderProgress(GuiGraphics g) {
        if (!menu.getInputStack().isEmpty()) animationKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(menu.getInputStack().getItem()).toString();
        var frame = recyclingAnimation.sample(net.minecraft.Util.getMillis(), animationKey, menu.progress(),
                de.artemis.cyberneticenhancements.common.blockentity.RecyclerStationBlockEntity.RECYCLE_TICKS, menu.isWorking(), menu.outputCount());
        int x = leftPos + PROGRESS_X, y = topPos + PROGRESS_Y;
        int accent = UiRenderer.blend(UiRenderer.ACCENT, 0xFF91E3B7, frame.completion());
        int fill = UiRenderer.blend(0xFF397E9B, 0xFF398D65, frame.completion());
        UiRenderer.panel(g, x, y - 1, PROGRESS_WIDTH, 10, UiRenderer.FIELD, UiRenderer.FIELD_EDGE);
        int barX = x + 2, barWidth = PROGRESS_WIDTH - 4;
        g.fill(barX, y + 1, barX + barWidth, y + 7, 0xFF202C38);
        int filled = Math.clamp(Math.round(barWidth * frame.progress()), 0, barWidth);
        if (filled > 0) {
            g.fill(barX, y + 1, barX + filled, y + 7, fill);
            g.fill(barX, y + 1, barX + filled, y + 2, accent);
        }
        for (int segment = 1; segment < 10; segment++) {
            int notch = barX + barWidth * segment / 10;
            g.fill(notch, y + 2, notch + 1, y + 7, 0xAA111820);
        }
    }
    private void centered(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}
    @Override protected void renderSlot(GuiGraphics g, Slot slot) {
        if (slot.index != 0) { super.renderSlot(g, slot); return; }
        g.pose().pushPose();
        g.pose().translate(INPUT_X + 8 - INPUT_ITEM_SIZE / 2, INPUT_Y + 8 - INPUT_ITEM_SIZE / 2, 0);
        g.pose().scale(INPUT_ITEM_SIZE / 16F, INPUT_ITEM_SIZE / 16F, 1);
        g.pose().translate(-slot.x, -slot.y, 0);
        super.renderSlot(g, slot);
        g.pose().popPose();
    }
    @Override protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (x == INPUT_X && y == INPUT_Y && width == 16 && height == 16)
            return super.isHovering(INPUT_FRAME_X, INPUT_FRAME_Y, INPUT_FRAME_SIZE, INPUT_FRAME_SIZE, mouseX, mouseY);
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }
    @Override protected void renderSlotHighlight(GuiGraphics g, Slot slot, int mouseX, int mouseY, float partial) {
        if (slot.index != 0) { super.renderSlotHighlight(g, slot, mouseX, mouseY, partial); return; }
        g.fill(INPUT_FRAME_X + 1, INPUT_FRAME_Y + 1, INPUT_FRAME_X + INPUT_FRAME_SIZE - 1,
                INPUT_FRAME_Y + INPUT_FRAME_SIZE - 1, 0x188BDAF8);
        UiRenderer.outline(g, INPUT_FRAME_X, INPUT_FRAME_Y, INPUT_FRAME_SIZE, INPUT_FRAME_SIZE, UiRenderer.SELECTED_EDGE);
    }
    @Override protected void renderTooltip(GuiGraphics g, int x, int y) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.index > 0
                && !isPlayerInventorySlot(hoveredSlot)) {
            if (hoveredSlot.hasItem()) {
                g.renderTooltip(font, hoveredSlot.getItem().getHoverName(), x, y);
                return;
            }
            if (!menu.hasPendingResultStacks()) {
                var preview = menu.getRecyclePlan().outputs();
                int index = hoveredSlot.index - 1;
                if (index < preview.size()) g.renderTooltip(font, preview.get(index).getHoverName(), x, y);
                return;
            }
        }
        super.renderTooltip(g, x, y);
    }
    @Override protected void renderContent(GuiGraphics g, int x, int y, float partial) {
        actions.get(1).active = menu.canRecycle();
        actions.get(2).active = menu.isWorking();
        super.renderContent(g, x, y, partial);
        renderTooltip(g, x, y);

    }
}
