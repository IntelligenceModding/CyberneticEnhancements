package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.common.menu.RelicCacheHackLayout;
import de.artemis.cyberneticenhancements.common.menu.RelicCacheHackMenu;
import de.artemis.cyberneticenhancements.common.network.RelicCacheHackSelectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class RelicCacheHackScreen extends AbstractContainerScreen<RelicCacheHackMenu> {
    private static final int OUTER_BG = 0xF40A1015;
    private static final int PANEL_BG = StationScreenStyle.PANEL_BG;
    private static final int PANEL_ALT = StationScreenStyle.PANEL_ALT;
    private static final int PANEL_EDGE = StationScreenStyle.PANEL_EDGE;
    private static final int PANEL_DEEP = StationScreenStyle.PANEL_DEEP;
    private static final int TEXT_PRIMARY = StationScreenStyle.TEXT_PRIMARY;
    private static final int TEXT_SECONDARY = StationScreenStyle.TEXT_SECONDARY;
    private static final int ACCENT = StationScreenStyle.ACCENT;
    private static final int WARNING = 0xFFFFB454;
    private static final int CRITICAL = StationScreenStyle.WARNING;
    private static final int PANEL_TEXT_PADDING_X = StationScreenStyle.PANEL_TEXT_PADDING_X;
    private static final int PANEL_TEXT_PADDING_Y = StationScreenStyle.PANEL_TEXT_PADDING_Y;
    private static final int CONTENT_PADDING = 12;
    private static final int HEADER_GAP = 26;
    private static final int TIMER_BAR_HEIGHT = 8;
    private static final int TIMER_BAR_GAP = 10;
    private static final int TARGET_CARD_GAP = 6;
    private static final int TARGET_TOKEN_GAP = 2;
    private static final int SELECTION_BADGE_SIZE = 9;

    private int lastSelectedCount;
    private int lastCompletedMask;
    private int lastSecondBucket = -1;

    public RelicCacheHackScreen(RelicCacheHackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = RelicCacheHackLayout.IMAGE_WIDTH;
        this.imageHeight = RelicCacheHackLayout.IMAGE_HEIGHT;
        this.inventoryLabelX = 10000;
        this.inventoryLabelY = 10000;
        this.titleLabelX = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;

        guiGraphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, OUTER_BG);
        drawPanel(guiGraphics, x0, y0, imageWidth, imageHeight, PANEL_BG);
        drawPanel(guiGraphics, x0 + RelicCacheHackLayout.GRID_PANEL_X1, y0 + RelicCacheHackLayout.GRID_PANEL_Y1, gridPanelWidth(), gridPanelHeight(), PANEL_ALT);
        drawPanel(guiGraphics, x0 + RelicCacheHackLayout.TARGET_PANEL_X1, y0 + RelicCacheHackLayout.TARGET_PANEL_Y1, targetPanelWidth(), targetPanelHeight(), PANEL_ALT);
        drawPanel(guiGraphics, x0 + RelicCacheHackLayout.FOOTER_PANEL_X1, y0 + RelicCacheHackLayout.FOOTER_PANEL_Y1, footerPanelWidth(), footerPanelHeight(), PANEL_ALT);

        drawTimerBar(guiGraphics, x0 + targetPanelInnerX(), y0 + targetTimerBarY(), targetPanelInnerWidth(), TIMER_BAR_HEIGHT);
        drawGrid(guiGraphics, mouseX, mouseY);
        drawTargets(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.relic_cache_hack.header"), gridPanelInnerX(), RelicCacheHackLayout.GRID_PANEL_Y1 + PANEL_TEXT_PADDING_Y, ACCENT, false);
        String moveKey = menu.selectedCount() == 0 ? "top_row" : (menu.selectedCount() & 1) == 1 ? "column" : "row";
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.relic_cache_hack.next." + moveKey),
                gridPanelInnerX(), RelicCacheHackLayout.GRID_PANEL_Y1 + PANEL_TEXT_PADDING_Y + 12, TEXT_PRIMARY, false);

        Component difficulty = Component.translatable("screen.cyberneticenhancements.relic_cache_hack.difficulty", menu.difficulty());
        Component timer = Component.translatable("screen.cyberneticenhancements.relic_cache_hack.timer", formatSeconds(menu.timeRemainingTicks()));
        guiGraphics.drawString(font, Component.translatable("screen.cyberneticenhancements.relic_cache_hack.targets"), targetPanelInnerX(), RelicCacheHackLayout.TARGET_PANEL_Y1 + PANEL_TEXT_PADDING_Y, TEXT_PRIMARY, false);
        drawRightAlignedString(guiGraphics, difficulty, RelicCacheHackLayout.TARGET_PANEL_X2 - PANEL_TEXT_PADDING_X, RelicCacheHackLayout.TARGET_PANEL_Y1 + PANEL_TEXT_PADDING_Y, TEXT_SECONDARY);
        guiGraphics.drawString(font, timer, targetPanelInnerX(), RelicCacheHackLayout.TARGET_PANEL_Y1 + PANEL_TEXT_PADDING_Y + 12, colorForRemainingTime(), false);

        drawWrappedText(guiGraphics, Component.translatable("screen.cyberneticenhancements.relic_cache_hack.rule"), footerPanelInnerX(), footerPanelInnerY(), footerPanelInnerWidth(), TEXT_SECONDARY, footerMaxLines());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderCellTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.selectedCount() > lastSelectedCount) {
            playLocalSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.22F, 1.35F);
        }
        if (menu.completedMask() != lastCompletedMask) {
            playLocalSound(SoundEvents.BEACON_POWER_SELECT, 0.35F, 1.55F);
        }

        int secondBucket = menu.timeRemainingTicks() / 20;
        if (secondBucket != lastSecondBucket && secondBucket <= 4 && secondBucket > 0) {
            playLocalSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.18F, 1.8F - 0.08F * secondBucket);
        }

        lastSelectedCount = menu.selectedCount();
        lastCompletedMask = menu.completedMask();
        lastSecondBucket = secondBucket;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cellIndex = hoveredCell((int) mouseX, (int) mouseY);
            if (cellIndex >= 0) {
                if (menu.isCellSelectable(cellIndex)) {
                    PacketDistributor.sendToServer(new RelicCacheHackSelectPayload(cellIndex));
                    playLocalSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.15F, 1.35F);
                    return true;
                }
                playLocalSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.20F, 0.65F);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int gridSize = menu.gridSize();
        int cellGap = cellGap(gridSize);
        int cellSize = cellSize(gridSize, cellGap);
        int startX = leftPos + gridStartX(gridSize, cellSize, cellGap);
        int startY = topPos + gridStartY(gridSize, cellSize, cellGap);
        int hovered = hoveredCell(mouseX, mouseY);

        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                int cellIndex = row * gridSize + column;
                int x = startX + column * (cellSize + cellGap);
                int y = startY + row * (cellSize + cellGap);
                boolean selected = menu.isCellAlreadySelected(cellIndex);
                boolean selectable = menu.isCellSelectable(cellIndex);
                boolean hoveredCell = hovered == cellIndex;

                int fillColor = selected
                        ? 0xFF173C39
                        : selectable
                        ? 0xFF133138
                        : 0xFF121A22;
                if (hoveredCell && selectable) {
                    fillColor = 0xFF1A4148;
                } else if (hoveredCell) {
                    fillColor = 0xFF1B2731;
                }

                guiGraphics.fill(x, y, x + cellSize, y + cellSize, fillColor);
                guiGraphics.fill(x, y, x + cellSize, y + 1, selectable ? ACCENT : PANEL_EDGE);
                guiGraphics.fill(x, y + cellSize - 1, x + cellSize, y + cellSize, PANEL_DEEP);
                guiGraphics.fill(x, y, x + 1, y + cellSize, PANEL_EDGE);
                guiGraphics.fill(x + cellSize - 1, y, x + cellSize, y + cellSize, PANEL_DEEP);

                String token = menu.tokenLabel(menu.tokenAtCell(cellIndex));
                int tokenColor = selected ? 0xFFE9FFF6 : selectable ? 0xFFBFEFE1 : TEXT_SECONDARY;
                guiGraphics.drawString(font, token, x + (cellSize - font.width(token)) / 2, y + (cellSize - 8) / 2, tokenColor, false);

                if (selected) {
                    int order = selectionIndex(cellIndex) + 1;
                    String orderLabel = Integer.toString(order);
                    drawSelectionBadge(guiGraphics, x, y, orderLabel);
                }
            }
        }
    }

    private void drawTargets(GuiGraphics guiGraphics) {
        int baseX = leftPos + targetPanelInnerX();
        int baseY = topPos + targetCardsStartY();
        int cardWidth = targetPanelInnerWidth();
        int cardHeight = targetCardHeight();

        for (int targetIndex = 0; targetIndex < menu.targetCount(); targetIndex++) {
            int y = baseY + targetIndex * (cardHeight + TARGET_CARD_GAP);
            boolean completed = menu.isTargetCompleted(targetIndex);
            drawPanel(guiGraphics, baseX, y, cardWidth, cardHeight, completed ? 0xFF143833 : PANEL_BG);
            guiGraphics.drawString(
                    font,
                    Component.translatable(completed ? "screen.cyberneticenhancements.relic_cache_hack.completed" : "screen.cyberneticenhancements.relic_cache_hack.daemon", targetIndex + 1),
                    baseX + 6,
                    y + 4,
                    completed ? ACCENT : TEXT_SECONDARY,
                    false
            );

            int matchedPrefix = menu.targetPrefixLength(targetIndex);
            int tokenBox = targetTokenBoxSize(targetIndex, cardWidth);
            int tokenRowWidth = menu.targetLength(targetIndex) * tokenBox + Math.max(0, menu.targetLength(targetIndex) - 1) * TARGET_TOKEN_GAP;
            int tokenX = baseX + (cardWidth - tokenRowWidth) / 2;
            int tokenY = y + cardHeight - 16;
            for (int tokenIndex = 0; tokenIndex < menu.targetLength(targetIndex); tokenIndex++) {
                int x = tokenX + tokenIndex * (tokenBox + TARGET_TOKEN_GAP);
                boolean matched = completed || tokenIndex < matchedPrefix;
                guiGraphics.fill(x, tokenY - 2, x + tokenBox, tokenY + 10, matched ? 0xFF1A5248 : PANEL_DEEP);
                if (!completed && tokenIndex == matchedPrefix) {
                    guiGraphics.fill(x, tokenY + 10, x + tokenBox, tokenY + 11, ACCENT);
                }
                String token = menu.tokenLabel(menu.targetToken(targetIndex, tokenIndex));
                guiGraphics.drawString(font, token, x + (tokenBox - font.width(token)) / 2, tokenY, matched ? TEXT_PRIMARY : TEXT_SECONDARY, false);
            }
        }
    }

    private void drawTimerBar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        float progress = (float) menu.timeRemainingTicks() / (float) menu.timeLimitTicks();
        int fillWidth = Mth.clamp(Math.round(progress * width), 0, width);
        guiGraphics.fill(x, y, x + width, y + height, PANEL_DEEP);
        guiGraphics.fill(x, y, x + fillWidth, y + height, colorForRemainingTime());
    }

    private int hoveredCell(int mouseX, int mouseY) {
        int gridSize = menu.gridSize();
        int cellGap = cellGap(gridSize);
        int cellSize = cellSize(gridSize, cellGap);
        int startX = leftPos + gridStartX(gridSize, cellSize, cellGap);
        int startY = topPos + gridStartY(gridSize, cellSize, cellGap);

        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                int x = startX + column * (cellSize + cellGap);
                int y = startY + row * (cellSize + cellGap);
                if (mouseX >= x && mouseX < x + cellSize && mouseY >= y && mouseY < y + cellSize) {
                    return row * gridSize + column;
                }
            }
        }
        return -1;
    }

    private int selectionIndex(int cellIndex) {
        for (int index = 0; index < menu.selectedCount(); index++) {
            if (menu.selectedCell(index) == cellIndex) {
                return index;
            }
        }
        return -1;
    }

    private int colorForRemainingTime() {
        float progress = (float) menu.timeRemainingTicks() / (float) menu.timeLimitTicks();
        if (progress < 0.25F) {
            return CRITICAL;
        }
        if (progress < 0.5F) {
            return WARNING;
        }
        return ACCENT;
    }

    private String formatSeconds(int ticks) {
        return Integer.toString(Math.max(0, Mth.ceil(ticks / 20.0F)));
    }

    private int cellGap(int gridSize) {
        return gridSize >= 6 ? 2 : 4;
    }

    private int cellSize(int gridSize, int cellGap) {
        int availableWidth = gridContentWidth();
        int availableHeight = gridContentHeight();
        int widthLimited = (availableWidth - cellGap * (gridSize - 1)) / gridSize;
        int heightLimited = (availableHeight - cellGap * (gridSize - 1)) / gridSize;
        return Math.max(18, Math.min(widthLimited, heightLimited));
    }

    private void renderCellTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int hoveredCell = hoveredCell(mouseX, mouseY);
        if (hoveredCell < 0) {
            return;
        }
        Component hint = menu.isCellAlreadySelected(hoveredCell)
                ? Component.translatable("screen.cyberneticenhancements.relic_cache_hack.used")
                : menu.isCellSelectable(hoveredCell)
                ? Component.translatable("screen.cyberneticenhancements.relic_cache_hack.pick")
                : Component.translatable("screen.cyberneticenhancements.relic_cache_hack.locked");
        guiGraphics.renderTooltip(font, hint, mouseX, mouseY);
    }

    private void drawWrappedText(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int lineCount = Math.min(lines.size(), maxLines);
        for (int index = 0; index < lineCount; index++) {
            guiGraphics.drawString(font, lines.get(index), x, y + index * wrappedLineHeight(), color, false);
        }
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fill) {
        guiGraphics.fill(x, y, x + width, y + height, fill);
        guiGraphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, PANEL_DEEP);
        guiGraphics.fill(x, y, x + 1, y + height, PANEL_EDGE);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, PANEL_DEEP);
    }

    private void playLocalSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(sound, volume, pitch);
        }
    }

    private void drawRightAlignedString(GuiGraphics guiGraphics, Component text, int rightX, int y, int color) {
        guiGraphics.drawString(font, text, rightX - font.width(text), y, color, false);
    }

    private void drawSelectionBadge(GuiGraphics guiGraphics, int x, int y, String orderLabel) {
        int badgeWidth = Math.max(SELECTION_BADGE_SIZE, font.width(orderLabel) + 4);
        guiGraphics.fill(x + 2, y + 2, x + 2 + badgeWidth, y + 2 + SELECTION_BADGE_SIZE, PANEL_DEEP);
        guiGraphics.fill(x + 2, y + 2, x + 2 + badgeWidth, y + 3, ACCENT);
        guiGraphics.drawString(font, orderLabel, x + 4, y + 3, ACCENT, false);
    }

    private int gridPanelWidth() {
        return RelicCacheHackLayout.GRID_PANEL_X2 - RelicCacheHackLayout.GRID_PANEL_X1;
    }

    private int gridPanelHeight() {
        return RelicCacheHackLayout.GRID_PANEL_Y2 - RelicCacheHackLayout.GRID_PANEL_Y1;
    }

    private int targetPanelWidth() {
        return RelicCacheHackLayout.TARGET_PANEL_X2 - RelicCacheHackLayout.TARGET_PANEL_X1;
    }

    private int targetPanelHeight() {
        return RelicCacheHackLayout.TARGET_PANEL_Y2 - RelicCacheHackLayout.TARGET_PANEL_Y1;
    }

    private int footerPanelWidth() {
        return RelicCacheHackLayout.FOOTER_PANEL_X2 - RelicCacheHackLayout.FOOTER_PANEL_X1;
    }

    private int footerPanelHeight() {
        return RelicCacheHackLayout.FOOTER_PANEL_Y2 - RelicCacheHackLayout.FOOTER_PANEL_Y1;
    }

    private int gridPanelInnerX() {
        return RelicCacheHackLayout.GRID_PANEL_X1 + PANEL_TEXT_PADDING_X;
    }

    private int gridPanelInnerY() {
        return RelicCacheHackLayout.GRID_PANEL_Y1 + PANEL_TEXT_PADDING_Y + HEADER_GAP;
    }

    private int gridContentX() {
        return RelicCacheHackLayout.GRID_PANEL_X1 + CONTENT_PADDING;
    }

    private int gridContentY() {
        return gridPanelInnerY();
    }

    private int gridContentWidth() {
        return gridPanelWidth() - CONTENT_PADDING * 2;
    }

    private int gridContentHeight() {
        return RelicCacheHackLayout.GRID_PANEL_Y2 - gridContentY() - CONTENT_PADDING;
    }

    private int gridStartX(int gridSize, int cellSize, int cellGap) {
        int usedWidth = gridSize * cellSize + Math.max(0, gridSize - 1) * cellGap;
        return gridContentX() + Math.max(0, (gridContentWidth() - usedWidth) / 2);
    }

    private int gridStartY(int gridSize, int cellSize, int cellGap) {
        int usedHeight = gridSize * cellSize + Math.max(0, gridSize - 1) * cellGap;
        return gridContentY() + Math.max(0, (gridContentHeight() - usedHeight) / 2);
    }

    private int targetPanelInnerX() {
        return RelicCacheHackLayout.TARGET_PANEL_X1 + PANEL_TEXT_PADDING_X;
    }

    private int targetPanelInnerWidth() {
        return targetPanelWidth() - PANEL_TEXT_PADDING_X * 2;
    }

    private int targetTimerBarY() {
        return RelicCacheHackLayout.TARGET_PANEL_Y1 + PANEL_TEXT_PADDING_Y + 24;
    }

    private int targetCardsStartY() {
        return targetTimerBarY() + TIMER_BAR_HEIGHT + TIMER_BAR_GAP;
    }

    private int targetCardHeight() {
        int count = Math.max(1, menu.targetCount());
        int availableHeight = RelicCacheHackLayout.TARGET_PANEL_Y2 - targetCardsStartY() - CONTENT_PADDING;
        return Math.max(34, Math.min(40, (availableHeight - TARGET_CARD_GAP * Math.max(0, count - 1)) / count));
    }

    private int targetTokenBoxSize(int targetIndex, int cardWidth) {
        int length = Math.max(1, menu.targetLength(targetIndex));
        int availableWidth = cardWidth - 12;
        return Math.max(14, Math.min(18, (availableWidth - TARGET_TOKEN_GAP * Math.max(0, length - 1)) / length));
    }

    private int footerPanelInnerX() {
        return RelicCacheHackLayout.FOOTER_PANEL_X1 + PANEL_TEXT_PADDING_X;
    }

    private int footerPanelInnerY() {
        return RelicCacheHackLayout.FOOTER_PANEL_Y1 + PANEL_TEXT_PADDING_Y;
    }

    private int footerPanelInnerWidth() {
        return footerPanelWidth() - PANEL_TEXT_PADDING_X * 2;
    }

    private int footerMaxLines() {
        return Math.max(1, (footerPanelHeight() - PANEL_TEXT_PADDING_Y * 2) / wrappedLineHeight());
    }

    private int wrappedLineHeight() {
        return font.lineHeight + 2;
    }
}
