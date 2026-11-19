package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.cyberware.*;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;
import de.artemis.cyberneticenhancements.common.menu.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import de.artemis.cyberneticenhancements.client.render.GhostItemRenderer;
import static de.artemis.cyberneticenhancements.client.screen.WorkshopScreenParts.*;

 public final class TechStationScreen extends ResponsiveContainerScreen<TechStationMenu> {
    private UiButton apply, fill, clear, stop;
    private final WorkshopProgressAnimation serviceAnimation = new WorkshopProgressAnimation();
    private String serviceAnimationKey = "";
    private float displayedGaugeFill = -1;
    private long gaugeFrameMillis;
    private final UiScrollState benefitsScroll = new UiScrollState();
    private final UiScrollbar benefitsScrollbar = new UiScrollbar();
    private double benefitsMaximum;
    private record BenefitLine(net.minecraft.util.FormattedCharSequence text, int y, int color) {}

    public TechStationScreen(TechStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        useMinecraftStationScale(UpgradeStationLayout.IMAGE_WIDTH, UpgradeStationLayout.IMAGE_HEIGHT, menu.machineSlotCount());
    }
    @Override protected void init() {
        super.init();
        int row = UpgradeStationLayout.ACTION_Y, x = UpgradeStationLayout.PROGRESS_X, gap = UpgradeStationLayout.CONTROL_GAP;
        fill = button("fill", x, row, 96, 1);
        clear = button("clear_materials", x + 96 + gap, row, 104, 2);
        apply = button(menu.isUpgrading() ? "upgrade" : "repair", x + 200 + 2 * gap, row, 96, 0);
        stop = button("stop", x + 296 + 3 * gap, row, 62, 4);
    }
    private UiButton button(String key, int x, int y, int width, int action) {
        return addRenderableWidget(new UiButton(leftPos + x, topPos + y, width, 20, tr(key), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        }));
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) { renderServicePanel(g, mouseX, mouseY); }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}
    @Override protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partial) {
        apply.active = menu.canService();
        fill.active = menu.canFillMaterials();
        clear.active = !menu.isWorking() && java.util.stream.IntStream.range(0, menu.materialCount()).anyMatch(i -> !menu.getMaterialStack(i).isEmpty());
        stop.active = menu.isWorking();
        super.renderContent(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
    private void renderServicePanel(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        UiRenderer.panel(g, x, y, UpgradeStationLayout.IMAGE_WIDTH, UpgradeStationLayout.IMAGE_HEIGHT, StationScreenStyle.PANEL_DEEP);
        UiRenderer.panel(g, x + UpgradeStationLayout.PANEL_X, y + UpgradeStationLayout.PANEL_Y,
                UpgradeStationLayout.PANEL_WIDTH, UpgradeStationLayout.PANEL_HEIGHT, StationScreenStyle.PANEL_BG);
        var input = menu.getInputStack();
        var plan = menu.plan();
        Component name = !input.isEmpty() ? input.getHoverName() : tr(menu.isUpgrading() ? "insert_upgrade" : "insert_repair");
        Component time = tr(menu.isUpgrading() ? "upgrade_time" : "repair_time");
        float headingScale = Math.min(1F, (UpgradeStationLayout.PANEL_WIDTH - 16F) / (font.width(name) + 14F + font.width(time)));
        g.pose().pushPose();
        g.pose().translate(x + UpgradeStationLayout.PANEL_X + 8, y + 20 + (20 - font.lineHeight * headingScale) / 2F, 0);
        g.pose().scale(headingScale, headingScale, 1);
        g.drawString(font, name, 0, 0, UiRenderer.TEXT, false);
        g.drawString(font, time.getVisualOrderText(), (UpgradeStationLayout.PANEL_WIDTH - 16) / headingScale - font.width(time), 0F, UiRenderer.TEXT_SECONDARY, false);
        g.pose().popPose();
        drawUpgradeBay(g, UpgradeStationLayout.INPUT_FRAME_X, !input.isEmpty());
        renderServiceGauge(g);
        renderUpgradeBenefits(g, mouseX, mouseY);
        for (int i = 0; i < menu.materialCount(); i++) {
            int slotY = UpgradeStationLayout.MATERIAL_Y[i];
            UiRenderer.panel(g, x + UpgradeStationLayout.MATERIAL_FRAME_X, y + UpgradeStationLayout.MATERIAL_FRAME_Y[i],
                    UpgradeStationLayout.MATERIAL_FRAME_SIZE, UpgradeStationLayout.MATERIAL_FRAME_SIZE, StationScreenStyle.SLOT_BG, UiRenderer.FIELD_EDGE);
            if (i < plan.materials().size()) {
                var required = plan.materials().get(i);
                if (menu.getMaterialStack(i).isEmpty()) {
                    g.pose().pushPose();
                    g.pose().translate(x + UpgradeStationLayout.MATERIAL_X[i] + 8 - UpgradeStationLayout.MATERIAL_ITEM_SIZE / 2,
                            y + slotY + 8 - UpgradeStationLayout.MATERIAL_ITEM_SIZE / 2, 0);
                    g.pose().scale(UpgradeStationLayout.MATERIAL_ITEM_SIZE / 16F, UpgradeStationLayout.MATERIAL_ITEM_SIZE / 16F, 1);
                    GhostItemRenderer.render(g, required.stack(), 0, 0);
                    g.pose().popPose();
                }
                g.drawString(font, tr("quantity", menu.materialTotal(required.stack()), required.count()),
                        x + UpgradeStationLayout.MATERIAL_FRAME_X + UpgradeStationLayout.MATERIAL_FRAME_SIZE + UpgradeStationLayout.CONTROL_GAP,
                        y + slotY + (16 - font.lineHeight) / 2,
                        menu.materialTotal(required.stack()) >= required.count() ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, false);
            }
        }
        renderServiceProgress(g);
    }
    private void renderServiceGauge(GuiGraphics g) {
        var implant = menu.getInputStack();
        boolean faulted = !menu.isUpgrading() && CyberwareConditionHelper.hasNeuralFault(implant);
        float target = implant.getItem() instanceof CyberwareItem
                ? menu.isUpgrading()
                    ? Math.clamp(CyberwareUpgradeHelper.getUpgradeLevel(implant) / (float) Math.max(1, CyberwareUpgradeHelper.getMaxUpgrades(implant)), 0, 1)
                    : (float) CyberwareConditionHelper.getIntegrityRatio(implant, ((CyberwareItem) implant.getItem()).getDefinition()) : 0;
        long now = net.minecraft.Util.getMillis();
        if (displayedGaugeFill < 0) displayedGaugeFill = target;
        double seconds = gaugeFrameMillis == 0 ? 0 : Math.clamp((now - gaugeFrameMillis) / 1000D, 0, .1);
        gaugeFrameMillis = now;
        displayedGaugeFill += (target - displayedGaugeFill) * (float) (1 - Math.exp(-seconds / .14));
        if (Math.abs(target - displayedGaugeFill) < .001F) displayedGaugeFill = target;
        int x = leftPos + UpgradeStationLayout.LEVEL_X - 1, y = topPos + UpgradeStationLayout.FRAME_Y;
        UiRenderer.panel(g, x, y, UpgradeStationLayout.LEVEL_WIDTH, UpgradeStationLayout.FRAME_SIZE,
                StationScreenStyle.SLOT_BG, UiRenderer.FIELD_EDGE);
        if (faulted) {
            g.fill(x + 1, y + 1, x + UpgradeStationLayout.LEVEL_WIDTH - 1,
                    y + UpgradeStationLayout.FRAME_SIZE - 1, 0xFFB83E4B);
            return;
        }
        int filled = Math.round(displayedGaugeFill * (UpgradeStationLayout.FRAME_SIZE - 2));
        int bottom = y + UpgradeStationLayout.FRAME_SIZE - 1;
        if (!menu.isUpgrading() && menu.plan().isAvailable() && implant.getItem() instanceof CyberwareItem item) {
            int future = Math.round((float) CyberwareConditionHelper.getIntegrityRatio(menu.plan().output(), item.getDefinition()) * (UpgradeStationLayout.FRAME_SIZE - 2));
            if (future > filled) g.fill(x + 1, bottom - future, x + UpgradeStationLayout.LEVEL_WIDTH - 1, bottom - filled, 0x55398D65);
        }
        if (filled > 0) {
            g.fill(x + 1, bottom - filled, x + UpgradeStationLayout.LEVEL_WIDTH - 1, bottom, menu.isUpgrading() ? 0xFF397E9B : 0xFF398D65);
            g.fill(x + 1, bottom - filled, x + UpgradeStationLayout.LEVEL_WIDTH - 1, bottom - filled + 1, menu.isUpgrading() ? UiRenderer.ACCENT : 0xFF91E3B7);
        }
    }
    private void renderUpgradeBenefits(GuiGraphics g, int mouseX, int mouseY) {
        var lines = new java.util.ArrayList<BenefitLine>();
        int textWidth = UpgradeStationLayout.BENEFITS_WIDTH - UiScrollbar.WIDTH - 6;
        int y = 0;
        var implant = menu.getInputStack();
        if (implant.getItem() instanceof CyberwareItem item) {
            y = benefitText(lines, tr("current_buffs"), y, textWidth, UiRenderer.ACCENT);
            var current = currentImplantBenefits(implant, item);
            if (current.isEmpty()) current.add(tr("no_buffs"));
            for (var text : current) y = benefitText(lines, text, y, textWidth, UiRenderer.TEXT);
            y = benefitText(lines, tr(menu.isUpgrading() ? "next_upgrade" : "next_repair"), y + 7, textWidth, UiRenderer.ACCENT);
            var changes = menu.isUpgrading() ? CyberwareUpgradeHelper.getUpgradePreviewLines(implant, item.getDefinition())
                    : menu.plan().isAvailable() ? java.util.List.of(tr(CyberwareConditionHelper.hasNeuralFault(implant) ? "repair_fault_gain" : "repair_gain")) : java.util.List.<Component>of();
            if (changes.isEmpty()) y = benefitText(lines, menu.status(), y, textWidth, UiRenderer.TEXT_SECONDARY);
            else for (var text : changes) y = benefitText(lines, text, y, textWidth, UiRenderer.TEXT);
        } else y = benefitText(lines, tr(menu.isUpgrading() ? "insert_upgrade" : "insert_repair"), y, textWidth, UiRenderer.TEXT_SECONDARY);
        benefitsMaximum = Math.max(0, y - 2 - UpgradeStationLayout.BENEFITS_HEIGHT);
        benefitsScroll.clamp(benefitsMaximum);
        benefitsScroll.tick();
        int x = leftPos + UpgradeStationLayout.BENEFITS_X, top = topPos + UpgradeStationLayout.BENEFITS_Y;
        g.enableScissor(x, top, x + textWidth, top + UpgradeStationLayout.BENEFITS_HEIGHT);
        for (var line : lines) g.drawString(font, line.text(), x, top + line.y() - (int) Math.round(benefitsScroll.current), line.color(), false);
        g.disableScissor();
        UiScrollbar.draw(g, benefitsTrack(), mouseX, mouseY, benefitsScrollbar.dragging());
    }
    private int benefitText(java.util.List<BenefitLine> lines, Component text, int y, int width, int color) {
        for (var line : font.split(text, width)) {
            lines.add(new BenefitLine(line, y, color));
            y += font.lineHeight + 2;
        }
        return y;
    }
    private java.util.List<Component> currentImplantBenefits(ItemStack implant, CyberwareItem item) {
        var lines = new java.util.ArrayList<Component>();
        if (CyberwareConditionHelper.hasNeuralFault(implant)) {
            lines.add(Component.translatable("tooltip.cyberneticenhancements.neural_fault"));
            return lines;
        }
        var definition = item.getDefinition();
        double headroom = CyberwareUpgradeHelper.getCapacityBonus(implant, definition);
        for (var effect : CyberwareUpgradeHelper.getEffects(implant, definition)) {
            if (effect.type() == CyberwareEffectType.CHROME_CAPACITY) headroom += effect.amount();
            else if (effect.amount() != 0) lines.add(effect.describe());
        }
        if (headroom > 0) lines.add(Component.translatable("tooltip.cyberneticenhancements.capacity_bonus", Math.round(headroom)));
        int chips = CyberwareUpgradeHelper.getChipSlotCount(implant, definition), modules = CyberwareUpgradeHelper.getModuleSlotCount(implant, definition);
        if (chips > 0) lines.add(Component.translatable("tooltip.cyberneticenhancements.upgrade_chip_slots", chips));
        if (modules > 0) lines.add(Component.translatable("tooltip.cyberneticenhancements.upgrade_module_slots", modules));
        var upgrades = CyberwareUpgradeRegistry.get(definition.id());
        var current = upgrades.at(CyberwareUpgradeHelper.getUpgradeLevel(implant));
        if (CyberwareUpgradeHelper.supportsFunction(definition.id()))
            lines.add(Component.translatable("tooltip.cyberneticenhancements.function." + definition.id()).append(
                    Component.literal(String.format(java.util.Locale.ROOT, ": %.0f%%", current.functionMultiplier() * 100))));
        if (upgrades.levels().stream().anyMatch(level -> Math.abs(level.durationMultiplier() - 1) > .0001))
            lines.add(tr("duration_multiplier", Math.round(current.durationMultiplier() * 100)));
        if (upgrades.levels().stream().anyMatch(level -> Math.abs(level.cooldownMultiplier() - 1) > .0001))
            lines.add(tr("cooldown_multiplier", Math.round(current.cooldownMultiplier() * 100)));
        if (upgrades.levels().stream().anyMatch(level -> Math.abs(level.instabilityMultiplier() - 1) > .0001))
            lines.add(tr("instability_multiplier", Math.round(current.instabilityMultiplier() * 100)));
        return lines;
    }
    private UiScrollbar.Track benefitsTrack() {
        return UiScrollbar.layout(leftPos + UpgradeStationLayout.BENEFITS_X + UpgradeStationLayout.BENEFITS_WIDTH - UiScrollbar.WIDTH,
                topPos + UpgradeStationLayout.BENEFITS_Y, UiScrollbar.WIDTH, UpgradeStationLayout.BENEFITS_HEIGHT, false,
                UpgradeStationLayout.BENEFITS_HEIGHT, benefitsMaximum, benefitsScroll.current);
    }
    private boolean overBenefits(double x, double y) {
        return x >= leftPos + UpgradeStationLayout.BENEFITS_X
                && x < leftPos + UpgradeStationLayout.BENEFITS_X + UpgradeStationLayout.BENEFITS_WIDTH
                && y >= topPos + UpgradeStationLayout.BENEFITS_Y && y < topPos + UpgradeStationLayout.BENEFITS_Y + UpgradeStationLayout.BENEFITS_HEIGHT;
    }
    @Override protected boolean mouseScrolledContent(double x, double y, double dx, double dy) {
        if (overBenefits(x, y) && benefitsMaximum > 0) {
            benefitsScroll.add(-dy * UiScrollState.WHEEL_STEP, benefitsMaximum);
            return true;
        }
        return super.mouseScrolledContent(x, y, dx, dy);
    }
    @Override protected boolean mouseClickedContent(double x, double y, int button) {
        var track = benefitsTrack();
        if (button == 0 && benefitsScrollbar.begin(track, x, y)) {
            benefitsScroll.set(benefitsScrollbar.value(track, x, y), benefitsMaximum);
            return true;
        }
        return super.mouseClickedContent(x, y, button);
    }
    @Override protected boolean mouseDraggedContent(double x, double y, int button, double dx, double dy) {
        if (button == 0 && benefitsScrollbar.dragging()) {
            benefitsScroll.set(benefitsScrollbar.value(benefitsTrack(), x, y), benefitsMaximum);
            return true;
        }
        return super.mouseDraggedContent(x, y, button, dx, dy);
    }
    @Override protected boolean mouseReleasedContent(double x, double y, int button) {
        benefitsScrollbar.release();
        return super.mouseReleasedContent(x, y, button);
    }
    private void drawUpgradeBay(GuiGraphics g, int x, boolean occupied) {
        UiRenderer.panel(g, leftPos + x, topPos + UpgradeStationLayout.FRAME_Y, UpgradeStationLayout.FRAME_SIZE,
                UpgradeStationLayout.FRAME_SIZE, UiRenderer.FIELD, occupied ? UiRenderer.SELECTED_EDGE : UiRenderer.FIELD_EDGE);
    }
    private int serviceCompletionMetric() {
        var implant = menu.getInputStack();
        return menu.isUpgrading() ? CyberwareUpgradeHelper.getUpgradeLevel(implant)
                : implant.getItem() instanceof CyberwareItem item ? CyberwareConditionHelper.getIntegrity(implant, item.getDefinition()) : 0;
    }
    private void renderServiceProgress(GuiGraphics g) {
        if (!menu.getInputStack().isEmpty()) serviceAnimationKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(menu.getInputStack().getItem())
                .toString();
        var frame = serviceAnimation.sample(net.minecraft.Util.getMillis(), serviceAnimationKey, menu.progress(),
                de.artemis.cyberneticenhancements.common.blockentity.TechStationBlockEntity.SERVICE_TICKS, menu.isWorking(), serviceCompletionMetric());
        int x = leftPos + UpgradeStationLayout.PROGRESS_X, y = topPos + UpgradeStationLayout.PROGRESS_Y;
        int accent = UiRenderer.blend(UiRenderer.ACCENT, 0xFF91E3B7, frame.completion());
        int fill = UiRenderer.blend(0xFF397E9B, 0xFF398D65, frame.completion());
        UiRenderer.panel(g, x, y - 1, UpgradeStationLayout.PROGRESS_WIDTH, 10, UiRenderer.FIELD, UiRenderer.FIELD_EDGE);
        int barX = x + 2, barWidth = UpgradeStationLayout.PROGRESS_WIDTH - 4;
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
    @Override protected void renderSlot(GuiGraphics g, Slot slot) {
        if (isPlayerInventorySlot(slot)) { super.renderSlot(g, slot); return; }
        int itemSize = slot.index == 0 ? UpgradeStationLayout.ITEM_SIZE : UpgradeStationLayout.MATERIAL_ITEM_SIZE;
        g.pose().pushPose();
        g.pose().translate(slot.x + 8 - itemSize / 2, slot.y + 8 - itemSize / 2, 0);
        g.pose().scale(itemSize / 16F, itemSize / 16F, 1);
        g.pose().translate(-slot.x, -slot.y, 0);
        super.renderSlot(g, slot);
        g.pose().popPose();
    }
    @Override protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (width == 16 && height == 16) {
            if (x == UpgradeStationLayout.INPUT_X && y == UpgradeStationLayout.INPUT_Y)
                return super.isHovering(UpgradeStationLayout.INPUT_FRAME_X, UpgradeStationLayout.FRAME_Y, UpgradeStationLayout.FRAME_SIZE, UpgradeStationLayout.FRAME_SIZE, mouseX, mouseY);
            for (int i = 0; i < menu.materialCount(); i++)
                if (x == UpgradeStationLayout.MATERIAL_X[i] && y == UpgradeStationLayout.MATERIAL_Y[i])
                    return super.isHovering(UpgradeStationLayout.MATERIAL_FRAME_X, UpgradeStationLayout.MATERIAL_FRAME_Y[i],
                            UpgradeStationLayout.MATERIAL_FRAME_SIZE, UpgradeStationLayout.MATERIAL_FRAME_SIZE, mouseX, mouseY);
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }
    @Override protected void renderSlotHighlight(GuiGraphics g, Slot slot, int mouseX, int mouseY, float partial) {
        if (isPlayerInventorySlot(slot)) { super.renderSlotHighlight(g, slot, mouseX, mouseY, partial); return; }
        int x = slot.index == 0 ? UpgradeStationLayout.INPUT_FRAME_X : UpgradeStationLayout.MATERIAL_FRAME_X;
        int y = slot.index == 0 ? UpgradeStationLayout.FRAME_Y : UpgradeStationLayout.MATERIAL_FRAME_Y[slot.index - 1];
        int size = slot.index == 0 ? UpgradeStationLayout.FRAME_SIZE : UpgradeStationLayout.MATERIAL_FRAME_SIZE;
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x188BDAF8);
        UiRenderer.outline(g, x, y, size, size, UiRenderer.SELECTED_EDGE);
    }
    @Override protected void renderTooltip(GuiGraphics g, int x, int y) {
        if (!menu.getCarried().isEmpty() || hoveredSlot == null || isPlayerInventorySlot(hoveredSlot)) {
            super.renderTooltip(g, x, y);
            return;
        }
        var plan = menu.plan();
        if (hoveredSlot.index == 1 || hoveredSlot.index == 2) {
            int index = hoveredSlot.index - 1;
            if (hoveredSlot.hasItem()) g.renderTooltip(font, hoveredSlot.getItem().getHoverName(), x, y);
            else if (index < plan.materials().size()) g.renderTooltip(font, plan.materials().get(index).stack().getHoverName(), x, y);
            return;
        }
        if (hoveredSlot.index == 0 && hoveredSlot.hasItem() && plan.isAvailable()) {
            var lines = new java.util.ArrayList<Component>();
            lines.add(menu.getInputStack().getHoverName());
            if (CyberwareConditionHelper.hasNeuralFault(menu.getInputStack())) lines.add(Component.translatable("tooltip.cyberneticenhancements.neural_fault"));
            lines.add(tr(menu.isUpgrading() ? "next_upgrade" : "next_repair"));
            if (menu.isUpgrading() && menu.getInputStack().getItem() instanceof CyberwareItem item)
                lines.addAll(CyberwareUpgradeHelper.getUpgradePreviewLines(menu.getInputStack(), item.getDefinition()));
            else lines.add(tr(CyberwareConditionHelper.hasNeuralFault(menu.getInputStack()) ? "repair_fault_gain" : "repair_gain"));
            g.renderTooltip(font, lines, java.util.Optional.empty(), x, y);
            return;
        }
        super.renderTooltip(g, x, y);
    }

}
