package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareServicePlan;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareServiceHelper;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeHelper;
import de.artemis.cyberneticenhancements.common.menu.AbstractBaseMenu;
import de.artemis.cyberneticenhancements.common.menu.TechStationLayout;
import de.artemis.cyberneticenhancements.common.menu.TechStationMenu;
import de.artemis.cyberneticenhancements.common.network.UpgradeStationInputSlotPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class TechStationScreen extends AbstractContainerScreen<TechStationMenu> {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CyberneticEnhancements.MOD_ID,
            "textures/gui/container/tech_station.png");
    private static final int SLOT_BG = StationScreenStyle.SLOT_BG;
    private static final int TEXT_PRIMARY = StationScreenStyle.TEXT_PRIMARY;
    private static final int TEXT_SECONDARY = StationScreenStyle.TEXT_SECONDARY;
    private static final int ACCENT = StationScreenStyle.ACCENT;
    private static final int WARNING = StationScreenStyle.WARNING;
    private static final int PANEL_TEXT_PADDING_X = StationScreenStyle.PANEL_TEXT_PADDING_X;
    private static final int PANEL_TEXT_PADDING_Y = StationScreenStyle.PANEL_TEXT_PADDING_Y;
    private static final int SLOT_LABEL_Y = StationScreenStyle.SLOT_LABEL_Y;
    private static final int REPAIR_SUMMARY_Y = 124;
    private static final int UPGRADE_SUMMARY_Y = 124;
    private static final int SUMMARY_LINE_HEIGHT = 18;
    private static final int SUMMARY_MAX_LINES = 3;
    private static final long SLOT_UPGRADE_HOLD_MS = 5000L;

    private UpgradeTarget heldUpgradeTarget;
    private CyberwareTier heldUpgradeStartTier;
    private long heldUpgradeStartMs;
    private boolean upgradeTriggeredThisHold;

    private enum UpgradeTarget {
        REPAIR_INPUT,
        UPGRADE_INPUT
    }

    public TechStationScreen(TechStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TechStationLayout.IMAGE_WIDTH;
        this.imageHeight = TechStationLayout.IMAGE_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        guiGraphics.blit(BACKGROUND_TEXTURE, x0, y0, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        drawUpgradeableSlotBack(guiGraphics, TechStationLayout.REPAIR_INPUT_X, TechStationLayout.REPAIR_INPUT_Y, menu.getRepairSupportedTier());
        drawSlotBack(guiGraphics, TechStationLayout.REPAIR_MATERIAL_X, TechStationLayout.REPAIR_PRIMARY_Y);
        drawSlotBack(guiGraphics, TechStationLayout.REPAIR_MATERIAL_X, TechStationLayout.REPAIR_MATERIAL_Y);
        drawSlotBack(guiGraphics, TechStationLayout.REPAIR_MATERIAL_X, TechStationLayout.REPAIR_TERTIARY_Y);
        drawSlotBack(guiGraphics, TechStationLayout.REPAIR_RESULT_X, TechStationLayout.REPAIR_RESULT_Y);
        drawUpgradeableSlotBack(guiGraphics, TechStationLayout.UPGRADE_INPUT_X, TechStationLayout.UPGRADE_INPUT_Y, menu.getUpgradeSupportedTier());
        drawSlotBack(guiGraphics, TechStationLayout.UPGRADE_PRIMARY_X, TechStationLayout.UPGRADE_PRIMARY_Y);
        drawSlotBack(guiGraphics, TechStationLayout.UPGRADE_SECONDARY_X, TechStationLayout.UPGRADE_SECONDARY_Y);
        drawSlotBack(guiGraphics, TechStationLayout.UPGRADE_RESULT_X, TechStationLayout.UPGRADE_RESULT_Y);
        drawPlayerSlotBacks(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.tech_station.service_bay"), TechStationLayout.REPAIR_PANEL_X1 + PANEL_TEXT_PADDING_X, TechStationLayout.REPAIR_PANEL_Y1 + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.tech_station.upgrade_bay"), TechStationLayout.UPGRADE_PANEL_X1 + PANEL_TEXT_PADDING_X, TechStationLayout.UPGRADE_PANEL_Y1 + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);

        drawCenteredLabel(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.input"), TechStationLayout.REPAIR_INPUT_X + StationSlotRenderer.SLOT_SIZE / 2, SLOT_LABEL_Y, TEXT_SECONDARY);
        drawCenteredLabel(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.materials"), TechStationLayout.REPAIR_MATERIAL_X + StationSlotRenderer.SLOT_SIZE / 2, SLOT_LABEL_Y, TEXT_SECONDARY);
        drawCenteredLabel(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.output"), TechStationLayout.REPAIR_RESULT_X + StationSlotRenderer.SLOT_SIZE / 2, SLOT_LABEL_Y, TEXT_SECONDARY);

        drawCenteredLabel(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.input"), TechStationLayout.UPGRADE_INPUT_X + StationSlotRenderer.SLOT_SIZE / 2, SLOT_LABEL_Y, TEXT_SECONDARY);
        drawCenteredLabel(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.upgrade_parts"), TechStationLayout.UPGRADE_PRIMARY_X + StationSlotRenderer.SLOT_SIZE / 2, SLOT_LABEL_Y, TEXT_SECONDARY);
        drawCenteredLabel(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.output"), TechStationLayout.UPGRADE_RESULT_X + StationSlotRenderer.SLOT_SIZE / 2, SLOT_LABEL_Y, TEXT_SECONDARY);

        renderRepairSummary(guiGraphics);
        renderUpgradeSummary(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        updateHeldSlotUpgrade(mouseX, mouseY);
        renderUpgradeSlotTooltip(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.getCarried().isEmpty()) {
            UpgradeTarget hoveredTarget = getHoveredEmptyUpgradeableSlot((int) mouseX, (int) mouseY);
            if (hoveredTarget != null && canUpgrade(hoveredTarget) && hasRequiredUpgradeComponent(hoveredTarget)) {
                heldUpgradeTarget = hoveredTarget;
                heldUpgradeStartTier = getSupportedTier(hoveredTarget);
                heldUpgradeStartMs = System.currentTimeMillis();
                upgradeTriggeredThisHold = false;
                return true;
            }
        }

        clearHeldSlotUpgrade();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean consumed = button == 0 && heldUpgradeTarget != null;
        if (button == 0) {
            clearHeldSlotUpgrade();
        }
        return consumed || super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderRepairSummary(GuiGraphics guiGraphics) {
        int x = TechStationLayout.REPAIR_PANEL_X1 + PANEL_TEXT_PADDING_X;
        int maxWidth = TechStationLayout.REPAIR_PANEL_X2 - TechStationLayout.REPAIR_PANEL_X1 - PANEL_TEXT_PADDING_X * 2;
        ItemStack input = menu.getRepairInputStack();
        CyberwareServicePlan plan = menu.getRepairPlan();
        CyberwareServicePlan resolvedPlan = menu.getRepairResolvedPlan();

        if (input.isEmpty()) {
            drawWrappedText(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.insert_repair_target"), x, REPAIR_SUMMARY_Y, maxWidth, TEXT_SECONDARY, SUMMARY_MAX_LINES);
            return;
        }

        if (!plan.isAvailable()) {
            drawWrappedText(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.repair_unavailable"), x, REPAIR_SUMMARY_Y, maxWidth, WARNING, 2);
            return;
        }

        var requirements = CyberwareServiceHelper.getRequiredMaterials(plan);
        ItemStack[] repairMaterials = menu.getRepairMaterialStacks();
        for (int index = 0; index < requirements.size(); index++) {
            var requirement = requirements.get(index);
            drawPlanMaterial(guiGraphics, requirement.count(), requirement.stack(), x, REPAIR_SUMMARY_Y + SUMMARY_LINE_HEIGHT * index, maxWidth, hasMatchingCount(repairMaterials, requirement.stack(), requirement.count()));
        }
    }

    private void renderUpgradeSummary(GuiGraphics guiGraphics) {
        int x = TechStationLayout.UPGRADE_PANEL_X1 + PANEL_TEXT_PADDING_X;
        int maxWidth = TechStationLayout.UPGRADE_PANEL_X2 - TechStationLayout.UPGRADE_PANEL_X1 - PANEL_TEXT_PADDING_X * 2;
        ItemStack input = menu.getUpgradeInputStack();
        CyberwareServicePlan plan = menu.getUpgradePlan();

        if (input.isEmpty()) {
            drawWrappedText(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.insert_upgrade_target"), x, UPGRADE_SUMMARY_Y, maxWidth, TEXT_SECONDARY, SUMMARY_MAX_LINES);
            return;
        }

        if (!plan.isAvailable()) {
            drawWrappedText(guiGraphics, Component.translatable("screen.cyberneticenhancements.tech_station.upgrade_unavailable"), x, UPGRADE_SUMMARY_Y, maxWidth, WARNING, 2);
            return;
        }

        if (input.getItem() instanceof de.artemis.cyberneticenhancements.common.item.CyberwareItem cyberwareItem) {
            drawWrappedText(
                    guiGraphics,
                    CyberwareUpgradeHelper.getUpgradePreviewStatusComponent(input, cyberwareItem.getDefinition()),
                    x,
                    UPGRADE_SUMMARY_Y,
                    maxWidth,
                    TEXT_PRIMARY,
                    1
            );
        }

        drawPlanMaterial(guiGraphics, plan.primaryCount(), plan.primaryMaterial(), x, UPGRADE_SUMMARY_Y + SUMMARY_LINE_HEIGHT, maxWidth, hasMatchingCount(menu.getUpgradePrimaryMaterialStack(), plan.primaryMaterial(), plan.primaryCount()) || hasMatchingCount(menu.getUpgradeSecondaryMaterialStack(), plan.primaryMaterial(), plan.primaryCount()));
        if (plan.requiresSecondaryMaterial()) {
            drawPlanMaterial(guiGraphics, plan.secondaryCount(), plan.secondaryMaterial(), x, UPGRADE_SUMMARY_Y + SUMMARY_LINE_HEIGHT * 2, maxWidth, hasMatchingCount(menu.getUpgradePrimaryMaterialStack(), plan.secondaryMaterial(), plan.secondaryCount()) || hasMatchingCount(menu.getUpgradeSecondaryMaterialStack(), plan.secondaryMaterial(), plan.secondaryCount()));
        }
    }

    private boolean hasMatchingCount(ItemStack candidate, ItemStack expected, int requiredCount) {
        return !candidate.isEmpty()
                && !expected.isEmpty()
                && ItemStack.isSameItemSameComponents(candidate.copyWithCount(1), expected.copyWithCount(1))
                && candidate.getCount() >= requiredCount;
    }

    private boolean hasMatchingCount(ItemStack[] candidates, ItemStack expected, int requiredCount) {
        for (ItemStack candidate : candidates) {
            if (hasMatchingCount(candidate, expected, requiredCount)) {
                return true;
            }
        }
        return false;
    }

    private void drawPlanMaterial(GuiGraphics guiGraphics, int count, ItemStack stack, int x, int y, int maxWidth, boolean ready) {
        Component amount = Component.translatable("screen.cyberneticenhancements.common.count_prefix", count);
        guiGraphics.drawString(font, amount, x, y, ready ? ACCENT : WARNING, false);
        int materialX = x + font.width(amount);
        drawTrimmedText(guiGraphics, stack.getHoverName(), materialX, y, Math.max(0, maxWidth - font.width(amount)), ready ? TEXT_PRIMARY : TEXT_SECONDARY);
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int color) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, color, false);
    }

    private void drawTrimmedText(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        guiGraphics.drawString(font, trimStyled(text, maxWidth), x, y, color, false);
    }

    private FormattedCharSequence trimStyled(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return Language.getInstance().getVisualOrder(text);
        }

        int ellipsisWidth = font.width("...");
        FormattedText base = font.substrByWidth(text, Math.max(0, maxWidth - ellipsisWidth));
        FormattedText combined = FormattedText.composite(base, Component.literal("..."));
        return Language.getInstance().getVisualOrder(combined);
    }

    private void drawWrappedText(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        int lineCount = Math.min(lines.size(), maxLines);
        for (int index = 0; index < lineCount; index++) {
            guiGraphics.drawString(font, lines.get(index), x, y + index * SUMMARY_LINE_HEIGHT, color, false);
        }
    }

    private void drawPlayerSlotBacks(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotBack(guiGraphics, TechStationLayout.PLAYER_INVENTORY_X + column * AbstractBaseMenu.SLOT_SPACING, TechStationLayout.PLAYER_INVENTORY_Y + row * AbstractBaseMenu.SLOT_SPACING);
            }
        }
        for (int slot = 0; slot < 9; slot++) {
            drawSlotBack(guiGraphics, TechStationLayout.PLAYER_INVENTORY_X + slot * AbstractBaseMenu.SLOT_SPACING, TechStationLayout.PLAYER_HOTBAR_Y);
        }
    }

    private void drawSlotBack(GuiGraphics guiGraphics, int x, int y) {
        StationSlotRenderer.drawStandardSlot(guiGraphics, leftPos, topPos, x, y, SLOT_BG);
    }

    private void drawUpgradeableSlotBack(GuiGraphics guiGraphics, int x, int y, CyberwareTier tier) {
        StationSlotRenderer.drawUpgradeableSlot(guiGraphics, leftPos, topPos, x, y, SLOT_BG, StationSlotRenderer.getTierFrameColor(tier));
    }

    private void renderUpgradeSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        UpgradeTarget hoveredTarget = getHoveredEmptyUpgradeableSlot(mouseX, mouseY);
        if (hoveredTarget == null) {
            return;
        }
        StationUpgradeTooltipBuilder.Tooltip tooltip = buildUpgradeTooltip(hoveredTarget);
        if (!tooltip.showProgressBar()) {
            guiGraphics.renderTooltip(font, tooltip.lines(), java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
        StationUpgradeTooltipRenderer.renderTooltip(guiGraphics, font, width, height, tooltip.lines(), getHeldUpgradeProgress(hoveredTarget), mouseX, mouseY);
    }

    private StationUpgradeTooltipBuilder.Tooltip buildUpgradeTooltip(UpgradeTarget target) {
        return StationUpgradeTooltipBuilder.build(
                Component.translatable(target == UpgradeTarget.REPAIR_INPUT
                        ? "screen.cyberneticenhancements.tech_station.service_bay"
                        : "screen.cyberneticenhancements.tech_station.upgrade_bay"),
                getSupportedTier(target),
                getNextSupportedTier(target),
                getRequiredUpgradeComponentStack(target),
                canUpgrade(target),
                hasRequiredUpgradeComponent(target),
                target == UpgradeTarget.REPAIR_INPUT
                        ? "screen.cyberneticenhancements.tech_station.repair_slot_hint"
                        : "screen.cyberneticenhancements.tech_station.upgrade_slot_hint"
        );
    }

    private float getHeldUpgradeProgress(UpgradeTarget target) {
        if (target != heldUpgradeTarget || heldUpgradeStartTier == null) {
            return 0.0F;
        }
        if (upgradeTriggeredThisHold) {
            return 1.0F;
        }
        long elapsedMs = System.currentTimeMillis() - heldUpgradeStartMs;
        return Math.max(0.0F, Math.min(1.0F, (float) elapsedMs / (float) SLOT_UPGRADE_HOLD_MS));
    }

    private void updateHeldSlotUpgrade(int mouseX, int mouseY) {
        if (heldUpgradeTarget == null || heldUpgradeStartTier == null) {
            return;
        }
        if (!isHeldUpgradeStillValid(heldUpgradeTarget, mouseX, mouseY)) {
            clearHeldSlotUpgrade();
            return;
        }
        if (getSupportedTier(heldUpgradeTarget) != heldUpgradeStartTier) {
            clearHeldSlotUpgrade();
            return;
        }
        if (!upgradeTriggeredThisHold && System.currentTimeMillis() - heldUpgradeStartMs >= SLOT_UPGRADE_HOLD_MS) {
            sendUpgradePacket(heldUpgradeTarget);
            upgradeTriggeredThisHold = true;
        }
    }

    private boolean isHeldUpgradeStillValid(UpgradeTarget target, int mouseX, int mouseY) {
        return isHoveringUpgradeTarget(target, mouseX, mouseY)
                && isTargetEmpty(target)
                && canUpgrade(target)
                && hasRequiredUpgradeComponent(target);
    }

    private UpgradeTarget getHoveredEmptyUpgradeableSlot(int mouseX, int mouseY) {
        if (isHoveringUpgradeTarget(UpgradeTarget.REPAIR_INPUT, mouseX, mouseY) && menu.getRepairInputStack().isEmpty()) {
            return UpgradeTarget.REPAIR_INPUT;
        }
        if (isHoveringUpgradeTarget(UpgradeTarget.UPGRADE_INPUT, mouseX, mouseY) && menu.getUpgradeInputStack().isEmpty()) {
            return UpgradeTarget.UPGRADE_INPUT;
        }
        return null;
    }

    private void clearHeldSlotUpgrade() {
        heldUpgradeTarget = null;
        heldUpgradeStartTier = null;
        heldUpgradeStartMs = 0L;
        upgradeTriggeredThisHold = false;
    }

    private void sendUpgradePacket(UpgradeTarget target) {
        PacketDistributor.sendToServer(new UpgradeStationInputSlotPayload(target == UpgradeTarget.REPAIR_INPUT ? 0 : 1));
    }

    private boolean isHoveringUpgradeTarget(UpgradeTarget target, int mouseX, int mouseY) {
        return switch (target) {
            case REPAIR_INPUT -> isHoveringSlot(TechStationLayout.REPAIR_INPUT_X, TechStationLayout.REPAIR_INPUT_Y, mouseX, mouseY);
            case UPGRADE_INPUT -> isHoveringSlot(TechStationLayout.UPGRADE_INPUT_X, TechStationLayout.UPGRADE_INPUT_Y, mouseX, mouseY);
        };
    }

    private boolean isHoveringSlot(int x, int y, int mouseX, int mouseY) {
        return isHovering(x, y, StationSlotRenderer.SLOT_SIZE, StationSlotRenderer.SLOT_SIZE, mouseX, mouseY);
    }

    private boolean isTargetEmpty(UpgradeTarget target) {
        return switch (target) {
            case REPAIR_INPUT -> menu.getRepairInputStack().isEmpty();
            case UPGRADE_INPUT -> menu.getUpgradeInputStack().isEmpty();
        };
    }

    private CyberwareTier getSupportedTier(UpgradeTarget target) {
        return switch (target) {
            case REPAIR_INPUT -> menu.getRepairSupportedTier();
            case UPGRADE_INPUT -> menu.getUpgradeSupportedTier();
        };
    }

    private CyberwareTier getNextSupportedTier(UpgradeTarget target) {
        return switch (target) {
            case REPAIR_INPUT -> menu.getNextRepairSupportedTier();
            case UPGRADE_INPUT -> menu.getNextUpgradeSupportedTier();
        };
    }

    private boolean canUpgrade(UpgradeTarget target) {
        return switch (target) {
            case REPAIR_INPUT -> menu.canUpgradeRepairSupportedTier();
            case UPGRADE_INPUT -> menu.canUpgradeUpgradeSupportedTier();
        };
    }

    private boolean hasRequiredUpgradeComponent(UpgradeTarget target) {
        return switch (target) {
            case REPAIR_INPUT -> menu.hasRequiredRepairUpgradeComponent();
            case UPGRADE_INPUT -> menu.hasRequiredUpgradeInputComponent();
        };
    }

    private ItemStack getRequiredUpgradeComponentStack(UpgradeTarget target) {
        return switch (target) {
            case REPAIR_INPUT -> menu.getRequiredRepairUpgradeComponentStack();
            case UPGRADE_INPUT -> menu.getRequiredUpgradeInputComponentStack();
        };
    }
}
