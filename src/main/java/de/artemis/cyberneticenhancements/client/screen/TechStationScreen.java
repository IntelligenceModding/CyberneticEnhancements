package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.menu.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import static de.artemis.cyberneticenhancements.client.screen.WorkshopScreenParts.*;

 public final class TechStationScreen extends ResponsiveContainerScreen<TechStationMenu> {
    private UiButton apply, fill, clear, capacity;

    public TechStationScreen(TechStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = TechStationLayout.IMAGE_WIDTH;
        imageHeight = TechStationLayout.IMAGE_HEIGHT;
    }
    protected void init() {
        super.init();
        capacity = button("capacity_button", 386, 18, 130, 3);
        fill = button("fill", 12, 250, 94, 1);
        clear = button("clear", 114, 250, 94, 2);
        apply = button(menu.isUpgrading() ? "upgrade" : "repair", 218, 250, 176, 0);
    }
    private UiButton button(String key, int x, int y, int width, int action) {
        return addRenderableWidget(new UiButton(leftPos + x, topPos + y, width, 20, tr(key), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        }));
    }
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        frame(g, leftPos, topPos);
        for (var slot : menu.slots)
            StationSlotRenderer.drawStandardSlot(g, leftPos, topPos, slot.x, slot.y, UiRenderer.FIELD);
        var plan = menu.plan();
        if (plan.isAvailable()) {
            UiRenderer.panel(g, leftPos + 230, topPos + 82, 26, 26, UiRenderer.FIELD, UiRenderer.FIELD_EDGE);
            g.renderFakeItem(plan.output(), leftPos + 235, topPos + 87);
            for (int i = 0; i < plan.materials().size(); i++) {
                if (!menu.getMaterialStack(i).isEmpty()) continue;
                g.renderFakeItem(plan.materials().get(i).stack(), leftPos + TechStationLayout.MATERIAL_X, topPos + TechStationLayout.MATERIAL_Y[i]);
                g.fill(leftPos + 30, topPos + TechStationLayout.MATERIAL_Y[i], leftPos + 46, topPos + TechStationLayout.MATERIAL_Y[i] + 16, 0x990D141C);
            }
        }
    }
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        line(g, tr("capacity", menu.tier().ordinal() + 1), 22, 24, 352, UiRenderer.ACCENT);
        line(g, tr("implant"), 22, 64, 176, UiRenderer.TEXT);
        line(g, tr("preview"), 230, 64, 286, UiRenderer.TEXT);
        ItemStack input = menu.getInputStack();
        var plan = menu.plan();
        if (input.isEmpty()) {
            wrap(g, font, tr(menu.isUpgrading() ? "insert_upgrade" : "insert_repair"), 56, 86, 140, 3, UiRenderer.TEXT_SECONDARY);
        } else {
            wrap(g, font, input.getHoverName(), 56, 86, 140, 3, UiRenderer.TEXT);
        }
        line(g, tr("materials"), 22, 126, 176, UiRenderer.TEXT_SECONDARY);
        for (int i = 0; i < plan.materials().size(); i++) {
            var requirement = plan.materials().get(i);
            int owned = menu.materialTotal(requirement.stack());
            int y = TechStationLayout.MATERIAL_Y[i];
            line(g, requirement.stack().getHoverName(), 56, y - 2, 140, UiRenderer.TEXT);
            line(g, tr("quantity", owned, requirement.count()), 56, y + 12, 140,
                    owned >= requirement.count() ? UiRenderer.ACCENT : UiRenderer.WARNING);
        }
        if (input.getItem() instanceof CyberwareItem item) {
            int level = CyberwareUpgradeHelper.getUpgradeLevel(input);
            int integrity = (int) Math.round(CyberwareConditionHelper.getIntegrityRatio(input, item.getDefinition()) * 100);
            line(g, input.getHoverName(), 264, 90, 252, UiRenderer.TEXT);
            line(g, tr("integrity_change", integrity, menu.isUpgrading() ? integrity : 100), 230, 116, 286, UiRenderer.ACCENT);
            line(g, tr("level_change", level, plan.isAvailable() && menu.isUpgrading() ? level + 1 : level, CyberwareUpgradeHelper.getMaxUpgrades(input)),
                    230, 132, 286, UiRenderer.TEXT_SECONDARY);
            if (menu.isUpgrading() && plan.isAvailable()) {
                int y = 156;
                for (var benefit : CyberwareUpgradeHelper.getUpgradePreviewLines(input, item.getDefinition())) {
                    var split = font.split(benefit, 286);
                    for (var text : split) {
                        if (y <= 218) g.drawString(font, text, 230, y, UiRenderer.TEXT, false);
                        y += 11;
                    }
                }
            } else wrap(g, font, tr(menu.isUpgrading() ? "max_upgrade" : "repair_note"), 230, 160, 286, 5, UiRenderer.TEXT_SECONDARY);
        } else wrap(g, font, tr("preview_help"), 230, 90, 286, 4, UiRenderer.TEXT_SECONDARY);
        line(g, tr("completed"), 404, 256, 82, menu.hasPendingOutput() ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY);
        wrap(g, font, menu.status(), 22, 286, 496, 2, UiRenderer.TEXT_SECONDARY);
        line(g, playerInventoryTitle, 22, 332, 154, UiRenderer.TEXT);
        wrap(g, font, tr(menu.isUpgrading() ? "upgrade_note" : "repair_short"), 22, 350, 154, 4, UiRenderer.TEXT_SECONDARY);
        wrap(g, font, tr("collection_help"), 364, 332, 152, 5, UiRenderer.TEXT_SECONDARY);
    }
    private void line(GuiGraphics g, Component text, int x, int y, int width, int color) {
        text(g, font, text, x, y, width, color);
    }
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partial) {
        apply.active = menu.canService();
        apply.setSelected(apply.active);
        apply.setTooltip(Tooltip.create(menu.status()));
        fill.active = menu.canFillMaterials();
        clear.active = java.util.stream.IntStream.range(0, menu.materialCount()).anyMatch(i -> !menu.getMaterialStack(i).isEmpty());
        capacity.active = menu.canUpgradeCapacity() && menu.hasCapacityMaterial();
        ItemStack cost = menu.capacityCost();
        capacity.setTooltip(Tooltip.create(cost.isEmpty() ? tr("capacity_max")
                : tr("capacity_cost", cost.getHoverName(), menu.tier().ordinal() + 2)));
        super.renderContent(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        var plan = menu.plan();
        for (int i = 0; i < plan.materials().size(); i++) {
            var required = plan.materials().get(i);
            if (isHovering(30, TechStationLayout.MATERIAL_Y[i] - 4, 168, 28, mouseX, mouseY))
                g.renderTooltip(font, java.util.List.of(required.stack().getHoverName(),
                        tr("quantity", menu.materialTotal(required.stack()), required.count())), java.util.Optional.empty(), mouseX, mouseY);
        }
        if (plan.isAvailable() && isHovering(230, 82, 286, 26, mouseX, mouseY))
            g.renderTooltip(font, plan.output(), mouseX, mouseY);
    }
}
