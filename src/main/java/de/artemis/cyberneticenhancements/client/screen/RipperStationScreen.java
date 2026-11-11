package de.artemis.cyberneticenhancements.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.client.tooltip.ModTooltipStyle;
import de.artemis.cyberneticenhancements.client.render.RipperStationHighlightLayer;
import de.artemis.cyberneticenhancements.client.tooltip.UpgradeProgressClientTooltip;
import de.artemis.cyberneticenhancements.client.tooltip.UpgradeProgressTooltip;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffect;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareEffectType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlot;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareSlotType;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareTier;
import de.artemis.cyberneticenhancements.common.menu.RipperStationLayout;
import de.artemis.cyberneticenhancements.common.menu.RipperStationMenu;
import de.artemis.cyberneticenhancements.common.network.UpgradeCyberwareSlotPayload;
import de.artemis.cyberneticenhancements.common.network.UpgradeRipperSubSlotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiScrollbar;
import de.artemis.cyberneticenhancements.client.ui.UiScrollState;
import de.artemis.cyberneticenhancements.client.ui.ResponsiveContainerScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareConditionHelper;
import de.artemis.cyberneticenhancements.common.item.CyberwareItem;

import java.util.List;
import java.util.Optional;

public final class RipperStationScreen extends ResponsiveContainerScreen<RipperStationMenu> {
    private static final int PANEL_ACCENT = StationScreenStyle.ACCENT;
    private static final int SLOT_BACKGROUND = StationScreenStyle.SLOT_BG;
    private static final int SLOT_ACTIVE = StationScreenStyle.SLOT_ACTIVE;
    private static final int TEXT_PRIMARY = StationScreenStyle.TEXT_PRIMARY;
    private static final int TEXT_SECONDARY = StationScreenStyle.TEXT_SECONDARY;
    private static final int WARNING = StationScreenStyle.WARNING;
    private static final int SLOT_HOVER_FILL = StationScreenStyle.SLOT_ACTIVE;
    private static final int SLOT_HOVER_OUTLINE = StationScreenStyle.ACCENT;
    private static final int LEFT_PANEL_X = 12;
    private static final int CENTER_PANEL_X = 200;
    private static final int RIGHT_PANEL_X = 610;
    private static final int TOP_PANEL_Y = 12;
    private static final int LOWER_PANEL_Y = 258;
    private static final int PANEL_TEXT_PADDING_X = StationScreenStyle.PANEL_TEXT_PADDING_X;
    private static final int PANEL_TEXT_PADDING_Y = StationScreenStyle.PANEL_TEXT_PADDING_Y;
    private static final int CHROME_TANK_X1 = 14;
    private static final int CHROME_TANK_Y1 = 340;
    private static final int CHROME_TANK_X2 = 187;
    private static final int CHROME_TANK_Y2 = 441;
    private static final int INTEGRITY_TANK_X1 = 612;
    private static final int INTEGRITY_TANK_Y1 = 340;
    private static final int INTEGRITY_TANK_X2 = 785;
    private static final int INTEGRITY_TANK_Y2 = 441;
    private static final long SLOT_UPGRADE_HOLD_MS = 5000L;
    private static final int MATRIX_MODEL_CENTER_X = 400;
    private static final int MATRIX_MODEL_CENTER_Y = 130;
    private static final int MATRIX_MODEL_HALF_WIDTH = 84;
    private static final int MATRIX_MODEL_HALF_HEIGHT = 97;
    private static final int MATRIX_MODEL_X1 = MATRIX_MODEL_CENTER_X - MATRIX_MODEL_HALF_WIDTH;
    private static final int MATRIX_MODEL_Y1 = MATRIX_MODEL_CENTER_Y - MATRIX_MODEL_HALF_HEIGHT;
    private static final int MATRIX_MODEL_X2 = MATRIX_MODEL_CENTER_X + MATRIX_MODEL_HALF_WIDTH;
    private static final int MATRIX_MODEL_Y2 = MATRIX_MODEL_CENTER_Y + MATRIX_MODEL_HALF_HEIGHT;
    private static final int MATRIX_MODEL_SCALE = 58;
    private static final float MATRIX_MODEL_Y_OFFSET = 0.0625F;
    private static final int BODY_OVERVIEW_LINE_SPACING = 16;
    private static final int BODY_OVERVIEW_CONTENT_OFFSET_Y = -3;
    private static final int SYSTEM_SUMMARY_CONTENT_OFFSET_Y = -3;
    private static final int BODY_OVERVIEW_VALUE_OFFSET_X = 7;
    private static final int SYSTEM_SUMMARY_COUNT_OFFSET_X = 3;
    private static final int OVERVIEW_X = 18, OVERVIEW_Y = 130, OVERVIEW_WIDTH = 160, OVERVIEW_HEIGHT = 108;
    private final UiScrollbar overviewScrollbar = new UiScrollbar();
    private final UiScrollState overviewScroll = new UiScrollState();
    private int overviewMaxScroll;

    private record OverviewRow(Component text, int color) {
    }

    private UpgradeTarget heldUpgradeTarget;
    private CyberwareTier heldUpgradeStartTier;
    private long heldUpgradeStartMs;
    private boolean upgradeTriggeredThisHold;

    private enum UpgradeTargetKind {
        CYBERWARE,
        CHIPWARE,
        ARM_MODULE,
        LEG_MODULE
    }

    private record UpgradeTarget(UpgradeTargetKind kind, int primaryIndex, int slotIndex) {
    }

    private record SlotTooltip(List<Component> lines, Optional<TooltipComponent> visualComponent) {
    }

    public RipperStationScreen(RipperStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = RipperStationLayout.IMAGE_WIDTH;
        this.imageHeight = RipperStationLayout.IMAGE_HEIGHT;
        this.inventoryLabelX = RipperStationLayout.PLAYER_INVENTORY_X;
        this.inventoryLabelY = RipperStationLayout.PLAYER_INVENTORY_Y - 12;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        UiRenderer.panel(guiGraphics, x0, y0, imageWidth, imageHeight, StationScreenStyle.PANEL_DEEP);
        UiRenderer.panel(guiGraphics, x0 + 12, y0 + 12, 176, 236, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + 200, y0 + 12, 400, 236, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + 610, y0 + 12, 178, 236, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + 12, y0 + 258, 176, 70, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + 200, y0 + 258, 400, 70, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + 610, y0 + 258, 178, 70, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + 200, y0 + 338, 400, 106, StationScreenStyle.PANEL_BG);

        drawPlayerModel(guiGraphics, mouseX, mouseY);
        drawPlayerSlotBacks(guiGraphics);
        drawSlotFrames(guiGraphics, mouseX, mouseY);
        drawStatusTanks(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station.body_overview"), LEFT_PANEL_X + PANEL_TEXT_PADDING_X, TOP_PANEL_Y + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station.system_summary"), RIGHT_PANEL_X + PANEL_TEXT_PADDING_X, TOP_PANEL_Y + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);

        int overviewY = 46 + BODY_OVERVIEW_CONTENT_OFFSET_Y;
        drawStatLine(guiGraphics, Component.translatable("screen.cyberneticenhancements.ripper_station.stage"), getStageLabel(), 18, overviewY, 178);
        overviewY += BODY_OVERVIEW_LINE_SPACING;
        drawStatLine(guiGraphics, Component.translatable("screen.cyberneticenhancements.ripper_station.chrome"), Component.literal(menu.getInstalledChrome() + " / " + menu.getChromeCapacity()), 18, overviewY, 178);
        overviewY += BODY_OVERVIEW_LINE_SPACING;
        drawStatLine(guiGraphics, Component.translatable("screen.cyberneticenhancements.ripper_station.cyberstrain"), Component.literal(Integer.toString(menu.getCyberstrain())), 18, overviewY, 178);
        overviewY += BODY_OVERVIEW_LINE_SPACING;
        drawStatLine(guiGraphics, Component.translatable("screen.cyberneticenhancements.ripper_station.integrity"), Component.literal(menu.getIntegrity() + "%"), 18, overviewY, 178);
        overviewY += BODY_OVERVIEW_LINE_SPACING;
        renderBodyOverviewBonuses(guiGraphics, mouseX, mouseY);

        renderBodyLabels(guiGraphics);
        renderSystemSummary(guiGraphics);
        renderChipSummary(guiGraphics);
        renderModuleSummary(guiGraphics);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        updateHeldSlotUpgrade(mouseX, mouseY);
        renderSlotTooltips(guiGraphics, mouseX, mouseY);
        renderStatusTankTooltip(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedContent(double mouseX, double mouseY, int button) {
        if (button == 0 && overviewScrollbar.begin(overviewTrack(), mouseX, mouseY)) {
            clearHeldSlotUpgrade();
            overviewScroll.set(overviewScrollbar.value(overviewTrack(), mouseX, mouseY), overviewMaxScroll);
            return true;
        }
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
        return super.mouseClickedContent(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseReleasedContent(double mouseX, double mouseY, int button) {
        boolean consumed = button == 0 && (heldUpgradeTarget != null || overviewScrollbar.dragging());
        if (button == 0) overviewScrollbar.release();
        if (button == 0) {
            clearHeldSlotUpgrade();
        }
        return consumed || super.mouseReleasedContent(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedContent(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && overviewScrollbar.dragging()) {
            overviewScroll.set(overviewScrollbar.value(overviewTrack(), mouseX, mouseY), overviewMaxScroll);
            return true;
        }
        return super.mouseDraggedContent(mouseX, mouseY, button, dx, dy);
    }

    @Override
    protected boolean mouseScrolledContent(double mouseX, double mouseY, double dx, double dy) {
        if (overviewMaxScroll > 0 && isHovering(OVERVIEW_X, OVERVIEW_Y, OVERVIEW_WIDTH, OVERVIEW_HEIGHT, mouseX, mouseY)) {
            overviewScroll.add(-dy * UiScrollState.WHEEL_STEP, overviewMaxScroll);
            return true;
        }
        return super.mouseScrolledContent(mouseX, mouseY, dx, dy);
    }

    private void drawStatLine(GuiGraphics guiGraphics, Component label, Component value, int x, int y, int rightX) {
        guiGraphics.drawString(font, label, x, y, TEXT_SECONDARY, false);
        guiGraphics.drawString(font, value, rightX - font.width(value) + BODY_OVERVIEW_VALUE_OFFSET_X, y, TEXT_PRIMARY, false);
    }

    private List<OverviewRow> bodyOverviewRows() {
        List<OverviewRow> rows = new ArrayList<>();
        int damaged = 0, broken = 0;
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            ItemStack stack = menu.getCyberwareStack(slot);
            if (!(stack.getItem() instanceof CyberwareItem implant)) continue;
            double integrity = CyberwareConditionHelper.getIntegrityRatio(stack, implant.getDefinition());
            if (integrity <= 0) broken++;
            else if (integrity < 1) damaged++;
        }
        if (broken > 0)
            rows.add(new OverviewRow(Component.translatable("screen.cyberneticenhancements.ripper_station.overview_broken", broken), WARNING));
        if (damaged > 0)
            rows.add(new OverviewRow(Component.translatable("screen.cyberneticenhancements.ripper_station.overview_damaged", damaged), WARNING));
        var totals = menu.getInstalledEffectTotals();

        for (CyberwareEffectType type : CyberwareEffectType.values()) {
            double amount = type == CyberwareEffectType.CHROME_CAPACITY ? menu.getChromeHeadroomBonus() : totals.getOrDefault(type, 0.0D);
            if (type.isMobEffect() ? !totals.containsKey(type) : Math.abs(amount) <= 0.0001D) continue;
            rows.add(new OverviewRow(new CyberwareEffect(type, amount).describe(), PANEL_ACCENT));
        }
        return rows;
    }

    private int overviewContentHeight(List<OverviewRow> rows, int width) {
        int height = 0;
        for (var row : rows) height += font.split(row.text(), width).size() * 11 + 5;
        return Math.max(0, height - 5);
    }

    private void renderBodyOverviewBonuses(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(OVERVIEW_X, 108, OVERVIEW_X + OVERVIEW_WIDTH, 109, UiRenderer.FIELD_EDGE);
        g.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station.overview_effects"), OVERVIEW_X, 114, TEXT_SECONDARY, false);
        var rows = bodyOverviewRows();
        boolean overflow = overviewContentHeight(rows, OVERVIEW_WIDTH) > OVERVIEW_HEIGHT;
        int textWidth = OVERVIEW_WIDTH - (overflow ? UiScrollbar.WIDTH + 6 : 0);
        overviewMaxScroll = Math.max(0, overviewContentHeight(rows, textWidth) - OVERVIEW_HEIGHT);
        overviewScroll.clamp(overviewMaxScroll);
        overviewScroll.tick();
        if (overviewMaxScroll == 0) overviewScrollbar.release();

        g.enableScissor(leftPos + OVERVIEW_X, topPos + OVERVIEW_Y, leftPos + OVERVIEW_X + textWidth, topPos + OVERVIEW_Y + OVERVIEW_HEIGHT);
        int y = OVERVIEW_Y - (int) Math.round(overviewScroll.current);
        for (var row : rows) {
            for (var line : font.split(row.text(), textWidth)) {
                g.drawString(font, line, OVERVIEW_X, y, row.color(), false);
                y += 11;
            }
            y += 5;
        }
        if (rows.isEmpty()) {
            for (var line : font.split(Component.translatable("screen.cyberneticenhancements.ripper_station.overview_no_effects"), textWidth)) {
                g.drawString(font, line, OVERVIEW_X, y, TEXT_SECONDARY, false);
                y += 11;
            }
        }
        g.disableScissor();

        g.pose().pushPose();
        g.pose().translate(-leftPos, -topPos, 0);
        UiScrollbar.draw(g, overviewTrack(), mouseX, mouseY, overviewScrollbar.dragging());
        g.pose().popPose();
    }

    private UiScrollbar.Track overviewTrack() {
        return UiScrollbar.layout(leftPos + OVERVIEW_X + OVERVIEW_WIDTH - UiScrollbar.WIDTH, topPos + OVERVIEW_Y,
                UiScrollbar.WIDTH, OVERVIEW_HEIGHT, false, OVERVIEW_HEIGHT, overviewMaxScroll, overviewScroll.current);
    }

    private void drawPlayerModel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        CyberwareSlot hoveredSlot = getHoveredCyberwareSlot(mouseX, mouseY);

        renderPlayerModelInMatrixFollowsMouse(
                guiGraphics,
                leftPos + MATRIX_MODEL_X1,
                topPos + MATRIX_MODEL_Y1,
                leftPos + MATRIX_MODEL_X2,
                topPos + MATRIX_MODEL_Y2,
                MATRIX_MODEL_SCALE,
                MATRIX_MODEL_Y_OFFSET,
                mouseX,
                mouseY,
                (AbstractClientPlayer) minecraft.player,
                hoveredSlot
        );
    }

    private void renderPlayerModelInMatrixFollowsMouse(
            GuiGraphics guiGraphics,
            int x1,
            int y1,
            int x2,
            int y2,
            int scale,
            float yOffset,
            float mouseX,
            float mouseY,
            AbstractClientPlayer player,
            CyberwareSlot hoveredSlot
    ) {
        float centerX = (float) (x1 + x2) / 2.0F;
        float centerY = (float) (y1 + y2) / 2.0F;
        guiGraphics.enableScissor(x1, y1, x2, y2);
        float yawOffset = (float) Math.atan((centerX - mouseX) / 40.0F);
        float pitchOffset = (float) Math.atan((centerY - mouseY) / 40.0F);
        Quaternionf poseRotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraRotation = new Quaternionf().rotateX(pitchOffset * 20.0F * (float) (Math.PI / 180.0));
        poseRotation.mul(cameraRotation);

        float bodyRot = player.yBodyRot;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float headRotO = player.yHeadRotO;
        float headRot = player.yHeadRot;
        player.yBodyRot = 180.0F + yawOffset * 20.0F;
        player.setYRot(180.0F + yawOffset * 40.0F);
        player.setXRot(-pitchOffset * 20.0F);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();

        float playerScale = player.getScale();
        Vector3f translation = new Vector3f(0.0F, player.getBbHeight() / 2.0F + yOffset * playerScale, 0.0F);
        float renderScale = (float) scale / playerScale;
        renderPlayerModelInMatrix(guiGraphics, centerX, centerY, renderScale, translation, poseRotation, cameraRotation, player, hoveredSlot);

        player.yBodyRot = bodyRot;
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yHeadRotO = headRotO;
        player.yHeadRot = headRot;
        guiGraphics.disableScissor();
    }

    private void renderPlayerModelInMatrix(
            GuiGraphics guiGraphics,
            float x,
            float y,
            float scale,
            Vector3f translation,
            Quaternionf poseRotation,
            Quaternionf cameraRotation,
            AbstractClientPlayer player,
            CyberwareSlot hoveredSlot
    ) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 50.0F);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().translate(translation.x, translation.y, translation.z);
        guiGraphics.pose().mulPose(poseRotation);
        Lighting.setupForEntityInInventory();

        EntityRenderDispatcher renderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        renderDispatcher.overrideCameraOrientation(cameraRotation.conjugate(new Quaternionf()).rotateY((float) Math.PI));
        renderDispatcher.setRenderShadow(false);

        RenderSystem.runAsFancy(() -> RipperStationHighlightLayer.renderWithSelection(player, hoveredSlot,
                () -> renderDispatcher.render(player, 0.0, 0.0, 0.0, 0.0F, 1.0F,
                        guiGraphics.pose(), guiGraphics.bufferSource(), 15728880)));

        guiGraphics.flush();
        renderDispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    private CyberwareSlot getHoveredCyberwareSlot(int mouseX, int mouseY) {
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            Slot menuSlot = menu.slots.get(slot.ordinal());
            if (isHoveringSlot(menuSlot.x, menuSlot.y, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private void drawSlotFrames(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        CyberwareSlot hoveredSlot = getHoveredCyberwareSlot(mouseX, mouseY);
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            drawStationSlotBack(
                    guiGraphics,
                    RipperStationLayout.CYBERWARE_SLOT_X[slot.ordinal()],
                    RipperStationLayout.CYBERWARE_SLOT_Y[slot.ordinal()],
                    true,
                    !menu.getCyberwareStack(slot).isEmpty(),
                    menu.getSupportedTier(slot),
                    slot == hoveredSlot
            );
        }

        for (int cluster = 0; cluster < RipperStationLayout.CHIP_CLUSTER_X.length; cluster++) {
            for (int slot = 0; slot < RipperStationLayout.CHIP_SLOT_OFFSET_X.length; slot++) {
                drawStationSlotBack(
                        guiGraphics,
                        RipperStationLayout.CHIP_CLUSTER_X[cluster] + RipperStationLayout.CHIP_SLOT_OFFSET_X[slot],
                        RipperStationLayout.CHIP_SLOT_Y,
                        menu.isChipSlotUnlocked(cluster, slot),
                        !menu.getChipwareStack(cluster, slot).isEmpty(),
                        menu.getChipSupportedTier(cluster, slot),
                        false
                );
            }
        }

        for (int slot = 0; slot < RipperStationLayout.ARM_MODULE_X.length; slot++) {
            drawStationSlotBack(guiGraphics, RipperStationLayout.ARM_MODULE_X[slot], RipperStationLayout.ARM_MODULE_Y, menu.isArmModuleSlotUnlocked(slot), !menu.getArmModuleStack(slot).isEmpty(), menu.getArmModuleSupportedTier(slot), false);
            drawStationSlotBack(guiGraphics, RipperStationLayout.LEG_MODULE_X[slot], RipperStationLayout.LEG_MODULE_Y, menu.isLegModuleSlotUnlocked(slot), !menu.getLegModuleStack(slot).isEmpty(), menu.getLegModuleSupportedTier(slot), false);
        }
    }

    private void drawStatusTanks(GuiGraphics guiGraphics) {
        drawTank(guiGraphics, CHROME_TANK_X1, CHROME_TANK_Y1, CHROME_TANK_X2, CHROME_TANK_Y2, menu.getChromePercent());
        drawTank(guiGraphics, INTEGRITY_TANK_X1, INTEGRITY_TANK_Y1, INTEGRITY_TANK_X2, INTEGRITY_TANK_Y2, menu.getIntegrity());
    }

    private void drawTank(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int percent) {
        int x = leftPos + x1;
        int y = topPos + y1;
        int width = x2 - x1;
        int height = y2 - y1;
        boolean chrome = x1 == CHROME_TANK_X1;
        Component label = Component.translatable("screen.cyberneticenhancements.ripper_station." + (chrome ? "chrome" : "integrity"));
        UiRenderer.panel(guiGraphics, x, y, width, height, UiRenderer.PANEL);
        guiGraphics.drawString(font, label, x + 8, y + 9, TEXT_SECONDARY, false);
        String value = percent + "%";
        guiGraphics.drawString(font, value, x + width - 8 - font.width(value), y + 9, TEXT_PRIMARY, false);
        UiRenderer.tank(guiGraphics, x + 8, y + 25, width - 16, height - 33, percent,
                chrome ? 0xFF409DC5 : 0xFF31BDA6);
    }

    private void renderStatusTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHoveringTank(CHROME_TANK_X1, CHROME_TANK_Y1, CHROME_TANK_X2, CHROME_TANK_Y2, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, List.of(
                    Component.translatable("screen.cyberneticenhancements.ripper_station.chrome"),
                    Component.literal(menu.getInstalledChrome() + " / " + menu.getChromeCapacity()),
                    Component.translatable("screen.cyberneticenhancements.ripper_station.cyberstrain")
                            .append(Component.literal(": " + menu.getCyberstrain()))
            ), Optional.empty(), mouseX, mouseY);
            return;
        }

        if (isHoveringTank(INTEGRITY_TANK_X1, INTEGRITY_TANK_Y1, INTEGRITY_TANK_X2, INTEGRITY_TANK_Y2, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, List.of(
                    Component.translatable("screen.cyberneticenhancements.ripper_station.integrity"),
                    Component.literal(menu.getIntegrity() + "%")
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    private boolean isHoveringTank(int x1, int y1, int x2, int y2, int mouseX, int mouseY) {
        return isHovering(x1, y1, x2 - x1, y2 - y1, mouseX, mouseY);
    }

    private void renderBodyLabels(GuiGraphics guiGraphics) {
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.OPERATING_SYSTEM, 400, 30);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.SKELETON, 534, 198);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.FACE, 534, 30);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.ARMS, 266, 92);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.HANDS, 534, 92);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.CIRCULATORY_SYSTEM, 266, 148);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.NERVOUS_SYSTEM, 534, 148);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.INTEGUMENTARY_SYSTEM, 266, 198);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.LEGS, 266, 30);
        drawCenteredBodyLabel(guiGraphics, CyberwareSlotType.FRONTAL_CORTEX, 400, 198);
    }

    private void drawCenteredBodyLabel(GuiGraphics guiGraphics, CyberwareSlotType slotType, int centerX, int y) {
        Component label = Component.translatable(slotType.translationKey());
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, getBodyLabelColor(slotType), false);
    }

    private int getBodyLabelColor(CyberwareSlotType slotType) {
        return menu.getInstalledCount(slotType) > 0 ? PANEL_ACCENT : TEXT_SECONDARY;
    }

    private void renderSystemSummary(GuiGraphics guiGraphics) {
        int labelX = RIGHT_PANEL_X + PANEL_TEXT_PADDING_X;
        int countRightX = 780;
        int startY = 46 + SYSTEM_SUMMARY_CONTENT_OFFSET_Y;
        CyberwareSlotType[] types = CyberwareSlotType.values();
        for (int i = 0; i < types.length; i++) {
            int y = startY + i * 18;
            CyberwareSlotType type = types[i];
            int installed = menu.getInstalledCount(type);
            Component label = Component.translatable(type.translationKey());
            String value = installed + " / " + type.getBaseSlotCount();
            guiGraphics.drawString(font, label, labelX, y, installed > 0 ? PANEL_ACCENT : TEXT_SECONDARY, false);
            guiGraphics.drawString(font, value, countRightX - font.width(value) + SYSTEM_SUMMARY_COUNT_OFFSET_X, y, TEXT_PRIMARY, false);
        }
    }

    private void renderChipSummary(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station.chipware"), CENTER_PANEL_X + PANEL_TEXT_PADDING_X, LOWER_PANEL_Y + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);
        for (int cluster = 0; cluster < RipperStationLayout.CHIP_CLUSTER_X.length; cluster++) {
            Component host = menu.getChipwareHostName(cluster);
            boolean emptyHost = host.getString().isEmpty();
            Component displayHost = emptyHost
                    ? Component.translatable("screen.cyberneticenhancements.ripper_station.no_chip_socket")
                    : host;
            int clusterCenterX = getSlotBankCenterX(RipperStationLayout.CHIP_CLUSTER_X[cluster], RipperStationLayout.CHIP_SLOT_OFFSET_X);
            int hostY = RipperStationLayout.CHIP_SLOT_Y - 12;
            int hostX = clusterCenterX - font.width(displayHost) / 2;
            guiGraphics.drawString(font, displayHost, hostX, hostY, emptyHost ? TEXT_SECONDARY : PANEL_ACCENT, false);
        }
    }

    private void renderModuleSummary(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station.arm_modules"), LEFT_PANEL_X + PANEL_TEXT_PADDING_X, LOWER_PANEL_Y + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station.leg_modules"), RIGHT_PANEL_X + PANEL_TEXT_PADDING_X, LOWER_PANEL_Y + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);

        Component armHost = menu.getArmModuleHostName();
        boolean emptyArmHost = armHost.getString().isEmpty();
        Component armHostComponent = emptyArmHost
                ? Component.translatable("screen.cyberneticenhancements.ripper_station.no_arm_cyberware")
                : armHost;
        int armCenterX = getExplicitSlotBankCenterX(RipperStationLayout.ARM_MODULE_X);
        guiGraphics.drawString(font, armHostComponent, armCenterX - font.width(armHostComponent) / 2, RipperStationLayout.ARM_MODULE_Y - 12, emptyArmHost ? TEXT_SECONDARY : PANEL_ACCENT, false);

        Component legHost = menu.getLegModuleHostName();
        boolean emptyLegHost = legHost.getString().isEmpty();
        Component legHostComponent = emptyLegHost
                ? Component.translatable("screen.cyberneticenhancements.ripper_station.no_leg_cyberware")
                : legHost;
        int legCenterX = getExplicitSlotBankCenterX(RipperStationLayout.LEG_MODULE_X);
        guiGraphics.drawString(font, legHostComponent, legCenterX - font.width(legHostComponent) / 2, RipperStationLayout.LEG_MODULE_Y - 12, emptyLegHost ? TEXT_SECONDARY : PANEL_ACCENT, false);
    }

    private int getSlotBankCenterX(int firstSlotX, int[] slotOffsets) {
        return firstSlotX + slotOffsets[slotOffsets.length - 1] / 2 + StationSlotRenderer.SLOT_SIZE / 2;
    }

    private int getExplicitSlotBankCenterX(int[] slotX) {
        return (slotX[0] + slotX[slotX.length - 1] + StationSlotRenderer.SLOT_SIZE) / 2;
    }

    private void renderSlotTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (CyberwareSlot cyberwareSlot : CyberwareSlot.values()) {
            Slot slot = menu.slots.get(cyberwareSlot.ordinal());
            if (isHoveringSlot(slot.x, slot.y, mouseX, mouseY) && !slot.hasItem()) {
                renderSlotTooltip(guiGraphics, buildCyberwareSlotTooltip(cyberwareSlot), mouseX, mouseY);
                return;
            }
        }

        for (int cluster = 0; cluster < RipperStationLayout.CHIP_CLUSTER_X.length; cluster++) {
            for (int slot = 0; slot < RipperStationLayout.CHIP_SLOT_OFFSET_X.length; slot++) {
                int chipX = RipperStationLayout.CHIP_CLUSTER_X[cluster] + RipperStationLayout.CHIP_SLOT_OFFSET_X[slot];
                if (isHoveringSlot(chipX, RipperStationLayout.CHIP_SLOT_Y, mouseX, mouseY)
                        && menu.getChipwareStack(cluster, slot).isEmpty()) {
                    if (!menu.isChipSlotUnlocked(cluster, slot)) {
                        guiGraphics.renderTooltip(font, List.of(
                                Component.translatable("screen.cyberneticenhancements.ripper_station.chipware"),
                                Component.translatable("screen.cyberneticenhancements.ripper_station.chip_slot_locked")
                        ), Optional.empty(), mouseX, mouseY);
                    } else {
                        renderSlotTooltip(guiGraphics, buildSubSlotTooltip(
                                new UpgradeTarget(UpgradeTargetKind.CHIPWARE, cluster, slot),
                                Component.translatable("screen.cyberneticenhancements.ripper_station.chipware"),
                                "screen.cyberneticenhancements.ripper_station.chip_slot_hint"
                        ), mouseX, mouseY);
                    }
                    return;
                }
            }
        }

        for (int i = 0; i < RipperStationLayout.ARM_MODULE_X.length; i++) {
            if (isHoveringSlot(RipperStationLayout.ARM_MODULE_X[i], RipperStationLayout.ARM_MODULE_Y, mouseX, mouseY)
                    && menu.getArmModuleStack(i).isEmpty()) {
                if (!menu.isArmModuleSlotUnlocked(i)) {
                    guiGraphics.renderTooltip(font, List.of(
                            Component.translatable("screen.cyberneticenhancements.ripper_station.arm_modules"),
                            Component.translatable("screen.cyberneticenhancements.ripper_station.module_slot_locked")
                    ), Optional.empty(), mouseX, mouseY);
                } else {
                    renderSlotTooltip(guiGraphics, buildSubSlotTooltip(
                            new UpgradeTarget(UpgradeTargetKind.ARM_MODULE, 0, i),
                            Component.translatable("screen.cyberneticenhancements.ripper_station.arm_modules"),
                            "screen.cyberneticenhancements.ripper_station.module_slot_hint"
                    ), mouseX, mouseY);
                }
                return;
            }

            if (isHoveringSlot(RipperStationLayout.LEG_MODULE_X[i], RipperStationLayout.LEG_MODULE_Y, mouseX, mouseY)
                    && menu.getLegModuleStack(i).isEmpty()) {
                if (!menu.isLegModuleSlotUnlocked(i)) {
                    guiGraphics.renderTooltip(font, List.of(
                            Component.translatable("screen.cyberneticenhancements.ripper_station.leg_modules"),
                            Component.translatable("screen.cyberneticenhancements.ripper_station.module_slot_locked")
                    ), Optional.empty(), mouseX, mouseY);
                } else {
                    renderSlotTooltip(guiGraphics, buildSubSlotTooltip(
                            new UpgradeTarget(UpgradeTargetKind.LEG_MODULE, 0, i),
                            Component.translatable("screen.cyberneticenhancements.ripper_station.leg_modules"),
                            "screen.cyberneticenhancements.ripper_station.module_slot_hint"
                    ), mouseX, mouseY);
                }
                return;
            }
        }
    }

    private Component getStageLabel() {
        return Component.translatable("screen.cyberneticenhancements.ripper_station.stage." + switch (menu.getStageIndex()) {
            case 0 -> "organic";
            case 1 -> "augmented";
            case 2 -> "cybernetic";
            case 3 -> "synthetic";
            default -> "android";
        });
    }

    private void drawPlayerSlotBacks(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawVanillaSlotBack(guiGraphics,
                        RipperStationLayout.PLAYER_INVENTORY_X + column * RipperStationLayout.PLAYER_SLOT_SPACING,
                        RipperStationLayout.PLAYER_INVENTORY_Y + row * RipperStationLayout.PLAYER_SLOT_SPACING,
                        SLOT_BACKGROUND);
            }
        }
        for (int slot = 0; slot < 9; slot++) {
            drawVanillaSlotBack(guiGraphics,
                    RipperStationLayout.PLAYER_INVENTORY_X + slot * RipperStationLayout.PLAYER_SLOT_SPACING,
                    RipperStationLayout.PLAYER_HOTBAR_Y,
                    SLOT_BACKGROUND);
        }
    }

    private void drawStationSlotBack(GuiGraphics guiGraphics, int x, int y, boolean unlocked, boolean installed, CyberwareTier tier, boolean hovered) {
        int fill = !unlocked ? 0xFF171D24 : hovered ? SLOT_HOVER_FILL : installed ? SLOT_ACTIVE : SLOT_BACKGROUND;
        int outline = !unlocked ? 0xFF28323C : hovered ? SLOT_HOVER_OUTLINE : StationSlotRenderer.getTierFrameColor(tier);
        StationSlotRenderer.drawUpgradeableSlot(guiGraphics, leftPos, topPos, x, y, fill, outline);
    }

    private void drawVanillaSlotBack(GuiGraphics guiGraphics, int x, int y, int fill) {
        StationSlotRenderer.drawStandardSlot(guiGraphics, leftPos, topPos, x, y, fill);
    }

    private boolean isHoveringSlot(int x, int y, int mouseX, int mouseY) {
        return isHovering(x, y, StationSlotRenderer.SLOT_SIZE, StationSlotRenderer.SLOT_SIZE, mouseX, mouseY);
    }

    private void renderSlotTooltip(GuiGraphics guiGraphics, SlotTooltip tooltip, int mouseX, int mouseY) {
        if (tooltip.visualComponent().isPresent() && tooltip.visualComponent().get() instanceof UpgradeProgressTooltip progressTooltip) {
            renderProgressSlotTooltip(guiGraphics, tooltip.lines(), progressTooltip.progress(), mouseX, mouseY);
            return;
        }
        guiGraphics.renderTooltip(font, tooltip.lines(), Optional.empty(), mouseX, mouseY);
    }

    private void renderProgressSlotTooltip(GuiGraphics guiGraphics, List<Component> lines, float progress, int mouseX, int mouseY) {
        int textWidth = 0;
        for (Component line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }

        int barWidth = UpgradeProgressClientTooltip.getBarWidth();
        int barHeight = UpgradeProgressClientTooltip.getBarHeight();
        int contentWidth = Math.max(textWidth, barWidth);
        int lineAdvance = font.lineHeight + ModTooltipStyle.LINE_SPACING;
        int textHeight = lines.size() * lineAdvance - ModTooltipStyle.LINE_SPACING;
        int tooltipWidth = contentWidth + ModTooltipStyle.CONTENT_PADDING * 2;
        int tooltipHeight = ModTooltipStyle.CONTENT_PADDING + textHeight + ModTooltipStyle.BAR_GAP + barHeight + ModTooltipStyle.BAR_BOTTOM_PADDING;

        int tooltipX = mouseX + ModTooltipStyle.TOOLTIP_CURSOR_OFFSET_X;
        if (tooltipX + tooltipWidth > width - ModTooltipStyle.SCREEN_EDGE_MARGIN) {
            tooltipX = mouseX - ModTooltipStyle.TOOLTIP_CURSOR_OFFSET_X - tooltipWidth;
        }
        tooltipX = Math.max(ModTooltipStyle.SCREEN_EDGE_MARGIN, tooltipX);

        int tooltipY = mouseY - ModTooltipStyle.TOOLTIP_CURSOR_OFFSET_Y;
        if (tooltipY + tooltipHeight > height - ModTooltipStyle.SCREEN_EDGE_MARGIN) {
            tooltipY = height - ModTooltipStyle.SCREEN_EDGE_MARGIN - tooltipHeight;
        }
        tooltipY = Math.max(ModTooltipStyle.SCREEN_EDGE_MARGIN, tooltipY);

        int tooltipX2 = tooltipX + tooltipWidth;
        int tooltipY2 = tooltipY + tooltipHeight;
        guiGraphics.flush();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
        RenderSystem.disableDepthTest();

        ModTooltipStyle.drawTooltipFrame(guiGraphics, tooltipX, tooltipY, tooltipX2, tooltipY2);

        int textX = tooltipX + ModTooltipStyle.CONTENT_PADDING;
        int textY = tooltipY + ModTooltipStyle.CONTENT_PADDING;
        for (Component line : lines) {
            guiGraphics.drawString(font, line.getVisualOrderText(), textX, textY, ModTooltipStyle.TEXT_FALLBACK, false);
            textY += lineAdvance;
        }

        int barX = tooltipX + ModTooltipStyle.CONTENT_PADDING;
        int barY = tooltipY + ModTooltipStyle.CONTENT_PADDING + textHeight + ModTooltipStyle.BAR_GAP;
        UpgradeProgressClientTooltip.renderBar(guiGraphics, barX, barY, progress);

        guiGraphics.flush();
        RenderSystem.enableDepthTest();
        guiGraphics.pose().popPose();
    }

    private SlotTooltip buildCyberwareSlotTooltip(CyberwareSlot slot) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(slot.displayKey()));
        return appendUpgradeLines(tooltip, new UpgradeTarget(UpgradeTargetKind.CYBERWARE, slot.ordinal(), 0), "screen.cyberneticenhancements.ripper_station.slot_hint");
    }

    private SlotTooltip buildSubSlotTooltip(UpgradeTarget target, Component title, String defaultHintKey) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(title);
        return appendUpgradeLines(tooltip, target, defaultHintKey);
    }

    private SlotTooltip appendUpgradeLines(List<Component> tooltip, UpgradeTarget target, String defaultHintKey) {
        CyberwareTier supportedTier = getSupportedTier(target);
        tooltip.add(Component.translatable(
                "screen.cyberneticenhancements.ripper_station.slot_supported_tier",
                Component.translatable(supportedTier.translationKey()).withStyle(supportedTier.getColor())
        ));

        if (supportedTier == CyberwareTier.TIER_5) {
            tooltip.add(Component.translatable("screen.cyberneticenhancements.ripper_station.slot_upgrade_maxed"));
            return new SlotTooltip(tooltip, Optional.empty());
        }

        CyberwareTier nextTier = getNextSupportedTier(target);
        ItemStack requiredComponent = getRequiredUpgradeComponentStack(target);
        if (!requiredComponent.isEmpty()) {
            tooltip.add(Component.translatable(
                    "screen.cyberneticenhancements.ripper_station.slot_upgrade_cost",
                    Component.translatable("screen.cyberneticenhancements.common.counted_item", 1, requiredComponent.getHoverName())
            ));
        }

        if (!canUpgrade(target)) {
            tooltip.add(Component.translatable(defaultHintKey));
        } else if (hasRequiredUpgradeComponent(target)) {
            tooltip.add(Component.translatable(
                    "screen.cyberneticenhancements.ripper_station.slot_upgrade_hold",
                    Component.translatable(nextTier.translationKey()).withStyle(nextTier.getColor())
            ));
        } else {
            tooltip.add(Component.translatable(
                    "screen.cyberneticenhancements.ripper_station.slot_upgrade_missing",
                    requiredComponent.isEmpty() ? Component.empty() : requiredComponent.getHoverName()
            ));
        }

        return new SlotTooltip(tooltip, Optional.of(new UpgradeProgressTooltip(getHeldUpgradeProgress(target))));
    }

    private float getHeldUpgradeProgress(UpgradeTarget target) {
        if (!target.equals(heldUpgradeTarget) || heldUpgradeStartTier == null) {
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
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            Slot menuSlot = menu.slots.get(slot.ordinal());
            if (isHoveringSlot(menuSlot.x, menuSlot.y, mouseX, mouseY) && !menuSlot.hasItem()) {
                return new UpgradeTarget(UpgradeTargetKind.CYBERWARE, slot.ordinal(), 0);
            }
        }
        for (int cluster = 0; cluster < RipperStationLayout.CHIP_CLUSTER_X.length; cluster++) {
            for (int slot = 0; slot < RipperStationLayout.CHIP_SLOT_OFFSET_X.length; slot++) {
                int chipX = RipperStationLayout.CHIP_CLUSTER_X[cluster] + RipperStationLayout.CHIP_SLOT_OFFSET_X[slot];
                if (isHoveringSlot(chipX, RipperStationLayout.CHIP_SLOT_Y, mouseX, mouseY) && menu.getChipwareStack(cluster, slot).isEmpty()) {
                    return new UpgradeTarget(UpgradeTargetKind.CHIPWARE, cluster, slot);
                }
            }
        }
        for (int slot = 0; slot < RipperStationLayout.ARM_MODULE_X.length; slot++) {
            if (isHoveringSlot(RipperStationLayout.ARM_MODULE_X[slot], RipperStationLayout.ARM_MODULE_Y, mouseX, mouseY) && menu.getArmModuleStack(slot).isEmpty()) {
                return new UpgradeTarget(UpgradeTargetKind.ARM_MODULE, 0, slot);
            }
            if (isHoveringSlot(RipperStationLayout.LEG_MODULE_X[slot], RipperStationLayout.LEG_MODULE_Y, mouseX, mouseY) && menu.getLegModuleStack(slot).isEmpty()) {
                return new UpgradeTarget(UpgradeTargetKind.LEG_MODULE, 0, slot);
            }
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
        switch (target.kind()) {
            case CYBERWARE -> PacketDistributor.sendToServer(new UpgradeCyberwareSlotPayload(target.primaryIndex()));
            case CHIPWARE ->
                    PacketDistributor.sendToServer(new UpgradeRipperSubSlotPayload(UpgradeRipperSubSlotPayload.KIND_CHIPWARE, target.primaryIndex(), target.slotIndex()));
            case ARM_MODULE ->
                    PacketDistributor.sendToServer(new UpgradeRipperSubSlotPayload(UpgradeRipperSubSlotPayload.KIND_ARM_MODULE, 0, target.slotIndex()));
            case LEG_MODULE ->
                    PacketDistributor.sendToServer(new UpgradeRipperSubSlotPayload(UpgradeRipperSubSlotPayload.KIND_LEG_MODULE, 0, target.slotIndex()));
        }
    }

    private boolean isHoveringUpgradeTarget(UpgradeTarget target, int mouseX, int mouseY) {
        return switch (target.kind()) {
            case CYBERWARE -> {
                Slot menuSlot = menu.slots.get(target.primaryIndex());
                yield isHoveringSlot(menuSlot.x, menuSlot.y, mouseX, mouseY);
            }
            case CHIPWARE -> {
                int x = RipperStationLayout.CHIP_CLUSTER_X[target.primaryIndex()] + RipperStationLayout.CHIP_SLOT_OFFSET_X[target.slotIndex()];
                yield isHoveringSlot(x, RipperStationLayout.CHIP_SLOT_Y, mouseX, mouseY);
            }
            case ARM_MODULE ->
                    isHoveringSlot(RipperStationLayout.ARM_MODULE_X[target.slotIndex()], RipperStationLayout.ARM_MODULE_Y, mouseX, mouseY);
            case LEG_MODULE ->
                    isHoveringSlot(RipperStationLayout.LEG_MODULE_X[target.slotIndex()], RipperStationLayout.LEG_MODULE_Y, mouseX, mouseY);
        };
    }

    private boolean isTargetEmpty(UpgradeTarget target) {
        return switch (target.kind()) {
            case CYBERWARE -> menu.getCyberwareStack(CyberwareSlot.values()[target.primaryIndex()]).isEmpty();
            case CHIPWARE -> menu.getChipwareStack(target.primaryIndex(), target.slotIndex()).isEmpty();
            case ARM_MODULE -> menu.getArmModuleStack(target.slotIndex()).isEmpty();
            case LEG_MODULE -> menu.getLegModuleStack(target.slotIndex()).isEmpty();
        };
    }

    private CyberwareTier getSupportedTier(UpgradeTarget target) {
        return switch (target.kind()) {
            case CYBERWARE -> menu.getSupportedTier(CyberwareSlot.values()[target.primaryIndex()]);
            case CHIPWARE -> menu.getChipSupportedTier(target.primaryIndex(), target.slotIndex());
            case ARM_MODULE -> menu.getArmModuleSupportedTier(target.slotIndex());
            case LEG_MODULE -> menu.getLegModuleSupportedTier(target.slotIndex());
        };
    }

    private CyberwareTier getNextSupportedTier(UpgradeTarget target) {
        return switch (target.kind()) {
            case CYBERWARE -> menu.getNextSupportedTier(CyberwareSlot.values()[target.primaryIndex()]);
            case CHIPWARE -> menu.getNextChipSupportedTier(target.primaryIndex(), target.slotIndex());
            case ARM_MODULE -> menu.getNextArmModuleSupportedTier(target.slotIndex());
            case LEG_MODULE -> menu.getNextLegModuleSupportedTier(target.slotIndex());
        };
    }

    private boolean canUpgrade(UpgradeTarget target) {
        return switch (target.kind()) {
            case CYBERWARE -> menu.canUpgradeSupportedTier(CyberwareSlot.values()[target.primaryIndex()]);
            case CHIPWARE -> menu.canUpgradeChipSupportedTier(target.primaryIndex(), target.slotIndex());
            case ARM_MODULE -> menu.canUpgradeArmModuleSupportedTier(target.slotIndex());
            case LEG_MODULE -> menu.canUpgradeLegModuleSupportedTier(target.slotIndex());
        };
    }

    private boolean hasRequiredUpgradeComponent(UpgradeTarget target) {
        return switch (target.kind()) {
            case CYBERWARE -> menu.hasRequiredUpgradeComponent(CyberwareSlot.values()[target.primaryIndex()]);
            case CHIPWARE -> menu.hasRequiredChipUpgradeComponent(target.primaryIndex(), target.slotIndex());
            case ARM_MODULE -> menu.hasRequiredArmModuleUpgradeComponent(target.slotIndex());
            case LEG_MODULE -> menu.hasRequiredLegModuleUpgradeComponent(target.slotIndex());
        };
    }

    private ItemStack getRequiredUpgradeComponentStack(UpgradeTarget target) {
        return switch (target.kind()) {
            case CYBERWARE -> menu.getRequiredUpgradeComponentStack(CyberwareSlot.values()[target.primaryIndex()]);
            case CHIPWARE -> menu.getRequiredChipUpgradeComponentStack(target.primaryIndex(), target.slotIndex());
            case ARM_MODULE -> menu.getRequiredArmModuleUpgradeComponentStack(target.slotIndex());
            case LEG_MODULE -> menu.getRequiredLegModuleUpgradeComponentStack(target.slotIndex());
        };
    }
}
