package de.artemis.cyberneticenhancements.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.cyberneticenhancements.client.render.RipperStationHighlightLayer;
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
    private static final int TEXT_PRIMARY = StationScreenStyle.TEXT_PRIMARY;
    private static final int TEXT_SECONDARY = StationScreenStyle.TEXT_SECONDARY;
    private static final int WARNING = StationScreenStyle.WARNING;
    private static final int LEFT_PANEL_X = RipperStationLayout.PANEL_X;
    private static final int CENTER_PANEL_X = RipperStationLayout.CENTER_X;
    private static final int RIGHT_PANEL_X = RipperStationLayout.RIGHT_X;
    private static final int TOP_PANEL_Y = RipperStationLayout.PANEL_Y;
    private static final int LOWER_PANEL_Y = RipperStationLayout.LOWER_Y;
    private static final int PANEL_TEXT_PADDING_X = RipperStationLayout.PANEL_PADDING;
    private static final int PANEL_TEXT_PADDING_Y = RipperStationLayout.PANEL_PADDING;
    private static final long SLOT_UPGRADE_HOLD_MS = 1000L;
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
    private static final int OVERVIEW_X = LEFT_PANEL_X + PANEL_TEXT_PADDING_X, OVERVIEW_Y = 43,
            OVERVIEW_WIDTH = RipperStationLayout.SIDE_WIDTH - 2 * PANEL_TEXT_PADDING_X,
            OVERVIEW_HEIGHT = TOP_PANEL_Y + RipperStationLayout.TOP_HEIGHT - PANEL_TEXT_PADDING_Y - OVERVIEW_Y;
    private final UiScrollbar overviewScrollbar = new UiScrollbar();
    private final UiScrollState overviewScroll = new UiScrollState();
    private int overviewMaxScroll;
    private static final int SUMMARY_X = RIGHT_PANEL_X + PANEL_TEXT_PADDING_X;
    private final UiScrollbar summaryScrollbar = new UiScrollbar();
    private final UiScrollState summaryScroll = new UiScrollState();
    private int summaryMaxScroll;
    private float displayedChrome = -1, displayedIntegrity = -1;
    private long statusFrameMillis;
    private record SummaryRow(Component label, int installed, int total) {}

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

    private record SlotTooltip(List<Component> lines) {
    }

    public RipperStationScreen(RipperStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        useMinecraftStationScale(RipperStationLayout.IMAGE_WIDTH, RipperStationLayout.IMAGE_HEIGHT, menu.playerInventoryStart());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        UiRenderer.panel(guiGraphics, x0, y0, RipperStationLayout.IMAGE_WIDTH, RipperStationLayout.IMAGE_HEIGHT, StationScreenStyle.PANEL_DEEP);
        UiRenderer.panel(guiGraphics, x0 + LEFT_PANEL_X, y0 + TOP_PANEL_Y, RipperStationLayout.SIDE_WIDTH, RipperStationLayout.TOP_HEIGHT, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + CENTER_PANEL_X, y0 + TOP_PANEL_Y, RipperStationLayout.CENTER_WIDTH, RipperStationLayout.TOP_HEIGHT, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + RIGHT_PANEL_X, y0 + TOP_PANEL_Y, RipperStationLayout.SIDE_WIDTH, RipperStationLayout.TOP_HEIGHT, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + LEFT_PANEL_X, y0 + LOWER_PANEL_Y, RipperStationLayout.SIDE_WIDTH, RipperStationLayout.LOWER_HEIGHT, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + CENTER_PANEL_X, y0 + LOWER_PANEL_Y, RipperStationLayout.CENTER_WIDTH, RipperStationLayout.LOWER_HEIGHT, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(guiGraphics, x0 + RIGHT_PANEL_X, y0 + LOWER_PANEL_Y, RipperStationLayout.SIDE_WIDTH, RipperStationLayout.LOWER_HEIGHT, StationScreenStyle.PANEL_BG);

        drawPlayerModel(guiGraphics, mouseX, mouseY);
        drawSlotFrames(guiGraphics, mouseX, mouseY);
        renderStatusBars(guiGraphics);
    }

    private void renderStatusBars(GuiGraphics graphics) {
        float chrome = menu.getChromeCapacity() > 0
                ? Math.clamp(menu.getInstalledChrome() / (float) menu.getChromeCapacity(), 0, 1) : 0;
        float integrity = Math.clamp(menu.getIntegrity() / 100F, 0, 1);
        long now = net.minecraft.Util.getMillis();
        double seconds = statusFrameMillis == 0 ? 0 : Math.clamp((now - statusFrameMillis) / 1000D, 0, .1);
        statusFrameMillis = now;
        float blend = (float) (1 - Math.exp(-seconds / .14));
        displayedChrome = displayedChrome < 0 ? chrome : displayedChrome + (chrome - displayedChrome) * blend;
        displayedIntegrity = displayedIntegrity < 0 ? integrity : displayedIntegrity + (integrity - displayedIntegrity) * blend;
        if (Math.abs(displayedChrome - chrome) < .001F) displayedChrome = chrome;
        if (Math.abs(displayedIntegrity - integrity) < .001F) displayedIntegrity = integrity;
        drawStatusBar(graphics, RipperStationLayout.STATUS_X, "chrome",
                menu.getInstalledChrome() + " / " + menu.getChromeCapacity(), displayedChrome, 0xFF397E9B, UiRenderer.ACCENT);
        drawStatusBar(graphics, RipperStationLayout.STATUS_X + RipperStationLayout.STATUS_BAR_WIDTH + RipperStationLayout.PANEL_GAP,
                "integrity", menu.getIntegrity() + "%", displayedIntegrity, 0xFF398D65, 0xFF91E3B7);
    }

    private void drawStatusBar(GuiGraphics graphics, int localX, String label, String value, float progress, int fill, int accent) {
        int x = leftPos + localX, y = topPos + RipperStationLayout.STATUS_BAR_Y;
        int width = RipperStationLayout.STATUS_BAR_WIDTH;
        graphics.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station." + label),
                x, topPos + RipperStationLayout.STATUS_LABEL_Y, TEXT_SECONDARY, false);
        graphics.drawString(font, value, x + width - font.width(value), topPos + RipperStationLayout.STATUS_LABEL_Y, TEXT_PRIMARY, false);
        UiRenderer.panel(graphics, x, y, width, 10, UiRenderer.FIELD, UiRenderer.FIELD_EDGE);
        int barX = x + 2, barWidth = width - 4;
        graphics.fill(barX, y + 2, barX + barWidth, y + 8, 0xFF202C38);
        int filled = Math.clamp(Math.round(barWidth * progress), 0, barWidth);
        if (filled > 0) {
            graphics.fill(barX, y + 2, barX + filled, y + 8, fill);
            graphics.fill(barX, y + 2, barX + filled, y + 3, accent);
        }
        for (int segment = 1; segment < 10; segment++) {
            int notch = barX + barWidth * segment / 10;
            graphics.fill(notch, y + 3, notch + 1, y + 8, 0xAA111820);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station.body_overview"), LEFT_PANEL_X + PANEL_TEXT_PADDING_X, TOP_PANEL_Y + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.ripper_station.system_summary"), RIGHT_PANEL_X + PANEL_TEXT_PADDING_X, TOP_PANEL_Y + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);


        renderBodyLabels(guiGraphics);
        renderChipSummary(guiGraphics);
        renderModuleSummary(guiGraphics);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateHeldSlotUpgrade(mouseX, mouseY);
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        renderBodyOverviewBonuses(guiGraphics, mouseX, mouseY);
        renderSystemSummary(guiGraphics, mouseX, mouseY);
        renderHeldUpgradeBorder(guiGraphics);
        renderSlotTooltips(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedContent(double mouseX, double mouseY, int button) {
        if (button == 0 && overviewScrollbar.begin(overviewTrack(), mouseX, mouseY)) {
            clearHeldSlotUpgrade();
            overviewScroll.set(overviewScrollbar.value(overviewTrack(), mouseX, mouseY), overviewMaxScroll);
            return true;
        }
        if (button == 0 && summaryScrollbar.begin(summaryTrack(), mouseX, mouseY)) {
            clearHeldSlotUpgrade();
            summaryScroll.set(summaryScrollbar.value(summaryTrack(), mouseX, mouseY), summaryMaxScroll);
            return true;
        }
        if (button == 0 && menu.getCarried().isEmpty()) {
            UpgradeTarget hoveredTarget = getHoveredEmptyUpgradeableSlot((int) mouseX, (int) mouseY);
            if (hoveredTarget != null && canUpgrade(hoveredTarget) && hasRequiredUpgradeComponent(hoveredTarget)) {
                heldUpgradeTarget = hoveredTarget;
                heldUpgradeStartTier = getSupportedTier(hoveredTarget);
                heldUpgradeStartMs = net.minecraft.Util.getMillis();
                upgradeTriggeredThisHold = false;
                return true;
            }
        }

        clearHeldSlotUpgrade();
        return super.mouseClickedContent(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseReleasedContent(double mouseX, double mouseY, int button) {
        boolean consumed = button == 0 && (heldUpgradeTarget != null || overviewScrollbar.dragging() || summaryScrollbar.dragging());
        if (button == 0) { overviewScrollbar.release(); summaryScrollbar.release(); }
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
        if (button == 0 && summaryScrollbar.dragging()) {
            summaryScroll.set(summaryScrollbar.value(summaryTrack(), mouseX, mouseY), summaryMaxScroll);
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
        if (summaryMaxScroll > 0 && isHovering(SUMMARY_X, OVERVIEW_Y, OVERVIEW_WIDTH, OVERVIEW_HEIGHT, mouseX, mouseY)) {
            summaryScroll.add(-dy * UiScrollState.WHEEL_STEP, summaryMaxScroll);
            return true;
        }
        return super.mouseScrolledContent(mouseX, mouseY, dx, dy);
    }

    private List<OverviewRow> bodyOverviewRows() {
        List<OverviewRow> rows = new ArrayList<>();
        int broken = 0, faults = 0;
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            ItemStack stack = menu.getCyberwareStack(slot);
            if (!(stack.getItem() instanceof CyberwareItem implant)) continue;
            double integrity = CyberwareConditionHelper.getIntegrityRatio(stack, implant.getDefinition());
            if (CyberwareConditionHelper.hasNeuralFault(stack)) faults++;
            else if (integrity <= 0) broken++;
        }
        if (faults > 0) rows.add(new OverviewRow(Component.translatable("screen.cyberneticenhancements.ripper_station.overview_faults", faults), WARNING));
        if (broken > 0)
            rows.add(new OverviewRow(Component.translatable("screen.cyberneticenhancements.ripper_station.overview_broken", broken), WARNING));
        var totals = menu.getInstalledEffectTotals();

        for (CyberwareEffectType type : CyberwareEffectType.values()) {
            double amount = type == CyberwareEffectType.CHROME_CAPACITY ? menu.getChromeHeadroomBonus() : totals.getOrDefault(type, 0.0D);
            if (type.isMobEffect() ? !totals.containsKey(type) : Math.abs(amount) <= 0.0001D) continue;
            rows.add(new OverviewRow(new CyberwareEffect(type, amount).describe(), TEXT_SECONDARY));
        }
        if (rows.isEmpty()) rows.add(new OverviewRow(Component.translatable("screen.cyberneticenhancements.ripper_station.overview_no_effects"), TEXT_SECONDARY));
        return rows;
    }

    private int overviewContentHeight(List<OverviewRow> rows, int width) {
        int height = 0;
        for (var row : rows) height += font.split(row.text(), width).size() * 11 + 5;
        return Math.max(0, height - 5);
    }

    private void renderBodyOverviewBonuses(GuiGraphics g, int mouseX, int mouseY) {
        var rows = bodyOverviewRows();
        boolean overflow = overviewContentHeight(rows, OVERVIEW_WIDTH) > OVERVIEW_HEIGHT;
        int textWidth = OVERVIEW_WIDTH - (overflow ? UiScrollbar.WIDTH + 6 : 0);
        overviewMaxScroll = Math.max(0, overviewContentHeight(rows, textWidth) - OVERVIEW_HEIGHT);
        overviewScroll.clamp(overviewMaxScroll);
        overviewScroll.tick();
        if (overviewMaxScroll == 0) overviewScrollbar.release();

        g.enableScissor(leftPos + OVERVIEW_X, topPos + OVERVIEW_Y, leftPos + OVERVIEW_X + textWidth, topPos + OVERVIEW_Y + OVERVIEW_HEIGHT);
        int y = topPos + OVERVIEW_Y - (int) Math.round(overviewScroll.current);
        for (var row : rows) {
            for (var line : font.split(row.text(), textWidth)) {
                g.drawString(font, line, leftPos + OVERVIEW_X, y, row.color(), false);
                y += 11;
            }
            y += 5;
        }
        g.disableScissor();

        UiScrollbar.draw(g, overviewTrack(), mouseX, mouseY, overviewScrollbar.dragging());
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
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_SECONDARY, false);
    }

    private List<SummaryRow> systemSummaryRows() {
        var rows = new ArrayList<SummaryRow>();
        for (var type : CyberwareSlotType.values())
            rows.add(new SummaryRow(Component.translatable(type.translationKey()), menu.getInstalledCount(type), type.getBaseSlotCount()));
        rows.add(new SummaryRow(Component.translatable("screen.cyberneticenhancements.ripper_station.chipware"), menu.getInstalledChipwareCount(), menu.getTotalChipwareSlotCount()));
        rows.add(new SummaryRow(Component.translatable("screen.cyberneticenhancements.ripper_station.arm_modules"), menu.getInstalledArmModuleCount(), menu.getTotalArmModuleSlotCount()));
        rows.add(new SummaryRow(Component.translatable("screen.cyberneticenhancements.ripper_station.leg_modules"), menu.getInstalledLegModuleCount(), menu.getTotalLegModuleSlotCount()));
        return rows;
    }

    private int summaryRowHeight(SummaryRow row, int labelWidth) {
        return Math.max(font.lineHeight, font.split(row.label(), labelWidth).size() * (font.lineHeight + 2) - 2) + 9;
    }

    private void renderSystemSummary(GuiGraphics g, int mouseX, int mouseY) {
        var rows = systemSummaryRows();
        int countWidth = rows.stream().mapToInt(row -> font.width(row.installed() + " / " + row.total())).max().orElse(0);
        int labelWidth = OVERVIEW_WIDTH - countWidth - RipperStationLayout.PANEL_GAP;
        int height = 0;
        for (var row : rows) height += summaryRowHeight(row, labelWidth);
        boolean overflow = height - 9 > OVERVIEW_HEIGHT;
        int contentWidth = OVERVIEW_WIDTH - (overflow ? UiScrollbar.WIDTH + 6 : 0);
        labelWidth = contentWidth - countWidth - RipperStationLayout.PANEL_GAP;
        height = 0;
        for (var row : rows) height += summaryRowHeight(row, labelWidth);
        summaryMaxScroll = Math.max(0, height - 9 - OVERVIEW_HEIGHT);
        summaryScroll.clamp(summaryMaxScroll);
        summaryScroll.tick();
        if (summaryMaxScroll == 0) summaryScrollbar.release();
        int x = leftPos + SUMMARY_X, top = topPos + OVERVIEW_Y;
        int y = top - (int) Math.round(summaryScroll.current);
        g.enableScissor(x, top, x + contentWidth, top + OVERVIEW_HEIGHT);
        for (var row : rows) {
            int lineY = y;
            for (var line : font.split(row.label(), labelWidth)) {
                g.drawString(font, line, x, lineY, TEXT_SECONDARY, false);
                lineY += font.lineHeight + 2;
            }
            String count = row.installed() + " / " + row.total();
            g.drawString(font, count, x + contentWidth - font.width(count), y, row.installed() > 0 ? PANEL_ACCENT : TEXT_SECONDARY, false);
            y += summaryRowHeight(row, labelWidth);
        }
        g.disableScissor();
        UiScrollbar.draw(g, summaryTrack(), mouseX, mouseY, summaryScrollbar.dragging());
    }

    private UiScrollbar.Track summaryTrack() {
        return UiScrollbar.layout(leftPos + SUMMARY_X + OVERVIEW_WIDTH - UiScrollbar.WIDTH, topPos + OVERVIEW_Y,
                UiScrollbar.WIDTH, OVERVIEW_HEIGHT, false, OVERVIEW_HEIGHT, summaryMaxScroll, summaryScroll.current);
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
            guiGraphics.drawString(font, displayHost, hostX, hostY, emptyHost ? TEXT_SECONDARY : TEXT_PRIMARY, false);
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
        guiGraphics.drawString(font, armHostComponent, armCenterX - font.width(armHostComponent) / 2, RipperStationLayout.ARM_MODULE_Y - 12, emptyArmHost ? TEXT_SECONDARY : TEXT_PRIMARY, false);

        Component legHost = menu.getLegModuleHostName();
        boolean emptyLegHost = legHost.getString().isEmpty();
        Component legHostComponent = emptyLegHost
                ? Component.translatable("screen.cyberneticenhancements.ripper_station.no_leg_cyberware")
                : legHost;
        int legCenterX = getExplicitSlotBankCenterX(RipperStationLayout.LEG_MODULE_X);
        guiGraphics.drawString(font, legHostComponent, legCenterX - font.width(legHostComponent) / 2, RipperStationLayout.LEG_MODULE_Y - 12, emptyLegHost ? TEXT_SECONDARY : TEXT_PRIMARY, false);
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

    private void drawStationSlotBack(GuiGraphics guiGraphics, int x, int y, boolean unlocked, boolean installed, CyberwareTier tier, boolean hovered) {
        int fill = unlocked ? SLOT_BACKGROUND : UiRenderer.ROW_DISABLED;
        int outline = unlocked ? StationSlotRenderer.getTierFrameColor(tier) : UiRenderer.FIELD_EDGE;
        UiRenderer.panel(guiGraphics, leftPos + x - 1, topPos + y - 1, 18, 18, fill, outline);
    }



    private boolean isHoveringSlot(int x, int y, int mouseX, int mouseY) {
        return isHovering(x, y, StationSlotRenderer.SLOT_SIZE, StationSlotRenderer.SLOT_SIZE, mouseX, mouseY);
    }

    private void renderSlotTooltip(GuiGraphics graphics, SlotTooltip tooltip, int mouseX, int mouseY) {
        graphics.renderTooltip(font, tooltip.lines(), Optional.empty(), mouseX, mouseY);
    }

    private void renderHeldUpgradeBorder(GuiGraphics graphics) {
        if (heldUpgradeTarget == null || heldUpgradeStartTier == null) return;
        var target = heldUpgradeTarget;
        int slotX = switch (target.kind()) {
            case CYBERWARE -> menu.slots.get(target.primaryIndex()).x;
            case CHIPWARE -> RipperStationLayout.CHIP_CLUSTER_X[target.primaryIndex()] + RipperStationLayout.CHIP_SLOT_OFFSET_X[target.slotIndex()];
            case ARM_MODULE -> RipperStationLayout.ARM_MODULE_X[target.slotIndex()];
            case LEG_MODULE -> RipperStationLayout.LEG_MODULE_X[target.slotIndex()];
        };
        int slotY = switch (target.kind()) {
            case CYBERWARE -> menu.slots.get(target.primaryIndex()).y;
            case CHIPWARE -> RipperStationLayout.CHIP_SLOT_Y;
            case ARM_MODULE -> RipperStationLayout.ARM_MODULE_Y;
            case LEG_MODULE -> RipperStationLayout.LEG_MODULE_Y;
        };
        int x = leftPos + slotX - 1, y = topPos + slotY - 1;
        int currentColor = StationSlotRenderer.getTierFrameColor(heldUpgradeStartTier);
        int nextColor = StationSlotRenderer.getTierFrameColor(getNextSupportedTier(target));
        float progress = getHeldUpgradeProgress(target) * 68;
        // Trace the 18x18 frame clockwise, counting each corner once. Blend the leading pixel smoothly.
        for (int pixel = 0; pixel < 68; pixel++) {
            float coverage = Math.clamp(progress - pixel, 0, 1);
            if (coverage <= 0) break;
            int dx, dy;
            if (pixel < 18) { dx = pixel; dy = 0; }
            else if (pixel < 35) { dx = 17; dy = pixel - 17; }
            else if (pixel < 52) { dx = 51 - pixel; dy = 17; }
            else { dx = 0; dy = 68 - pixel; }
            graphics.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, UiRenderer.blend(currentColor, nextColor, coverage));
        }
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (isPlayerInventorySlot(slot)) {
            super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
            return;
        }
        if (!slot.isHighlightable()) return;
        graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x188BDAF8);
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
            return new SlotTooltip(tooltip);
        }

        CyberwareTier nextTier = getNextSupportedTier(target);
        ItemStack requiredComponent = getRequiredUpgradeComponentStack(target);
        if (!requiredComponent.isEmpty()) {
            tooltip.add(Component.translatable(
                    "screen.cyberneticenhancements.ripper_station.slot_upgrade_cost",
                    Component.translatable("screen.cyberneticenhancements.common.counted_item", requiredComponent.getCount(), requiredComponent.getHoverName())
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
                    requiredComponent.isEmpty() ? Component.empty() : Component.translatable("screen.cyberneticenhancements.common.counted_item", requiredComponent.getCount(), requiredComponent.getHoverName())
            ));
        }

        return new SlotTooltip(tooltip);
    }

    private float getHeldUpgradeProgress(UpgradeTarget target) {
        if (!target.equals(heldUpgradeTarget) || heldUpgradeStartTier == null) {
            return 0.0F;
        }
        if (upgradeTriggeredThisHold) {
            return 1.0F;
        }
        long elapsedMs = net.minecraft.Util.getMillis() - heldUpgradeStartMs;
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
        if (!upgradeTriggeredThisHold && net.minecraft.Util.getMillis() - heldUpgradeStartMs >= SLOT_UPGRADE_HOLD_MS) {
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
